package com.thirtyseconds.platform

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import com.thirtyseconds.data.PlayerCodec
import com.thirtyseconds.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class FirebaseServices(private val context: Context,private val repository: PlayerRepository,private val competitionUrl:String="") {
    val configured=FirebaseApp.getApps(context).isNotEmpty()
    private val auth=if(configured)FirebaseAuth.getInstance() else null
    private val db=if(configured)FirebaseFirestore.getInstance() else null
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutex=Mutex()
    private val message=MutableStateFlow(if(configured)"not_connected" else "not_configured")
    val status=message.asStateFlow()
    private val account=MutableStateFlow(auth?.currentUser?.displayName)
    val accountName=account.asStateFlow()
    private var restoredUid: String?=null
    private val uploaded=mutableSetOf<String>()
    private var lastPayload=""
    private val outbox=context.getSharedPreferences("competition_outbox",Context.MODE_PRIVATE)
    val rankingConfigured get()=competitionUrl.startsWith("https://")
    val googleConfigured: Boolean get()=configured && context.resources.getIdentifier("default_web_client_id","string",context.packageName)!=0
    val userId get()=auth?.currentUser?.uid
    fun observe() {
        if(!configured)return
        // Persistent local history is the outbox. Retry after offline/auth failures even
        // when no new game is played; process restart automatically resumes this loop.
        scope.launch {
            while(isActive) {
                val p=repository.player.first()
                if(p.profileCreated && auth?.currentUser!=null) {
                    try {mutex.withLock{sync(p);flushCompetition()};message.value="synced"}
                    catch(e:Exception){if(e is CancellationException)throw e;restoredUid=null;uploaded.clear();message.value="sync_pending"}
                }
                delay(20_000)
            }
        }
    }
    suspend fun connectGoogle(activity: Activity) {
        check(googleConfigured){"Enable Google sign-in and download the updated google-services.json"}
        val id=context.resources.getIdentifier("default_web_client_id","string",context.packageName)
        val option=GetGoogleIdOption.Builder().setServerClientId(context.getString(id)).setFilterByAuthorizedAccounts(false).build()
        val response=CredentialManager.create(activity).getCredential(activity,GetCredentialRequest.Builder().addCredentialOption(option).build())
        val token=GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        val credential=GoogleAuthProvider.getCredential(token,null)
        val user=auth!!.currentUser
        if(user?.isAnonymous==true) {
            try {user.linkWithCredential(credential).await()}
            catch(e:FirebaseAuthUserCollisionException){auth.signInWithCredential(credential).await()}
        } else auth.signInWithCredential(credential).await()
        val p=repository.player.first()
        if(!p.profileCreated)repository.createProfile(auth.currentUser?.displayName?.filter{it.isLetterOrDigit()}?.take(20)?.takeIf{it.length>=3}?:"")
        account.value=auth.currentUser?.displayName
        restoredUid=null;uploaded.clear();lastPayload=""
        mutex.withLock {sync(repository.player.first())}
        message.value="synced"
    }
    suspend fun connectGuest() {
        check(configured)
        if(auth!!.currentUser==null)auth.signInAnonymously().await()
        mutex.withLock{sync(repository.player.first())};message.value="synced"
    }
    private fun eventId(id: String)=MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString(""){"%02x".format(it)}
    private suspend fun documents(query: Query): List<DocumentSnapshot> {
        val result=mutableListOf<DocumentSnapshot>();var cursor:DocumentSnapshot?=null
        do {
            var q=query.orderBy(FieldPath.documentId()).limit(200)
            if(cursor!=null)q=q.startAfter(cursor)
            val batch=q.get(Source.SERVER).await().documents;result+=batch;cursor=batch.lastOrNull()
        }while(batch.size==200)
        return result
    }
    private suspend fun sync(local: Player) {
        val uid=auth?.currentUser?.uid?:return
        val ref=db!!.collection("players").document(uid)
        var p=local
        val header=ref.get(Source.SERVER).await().getString("payload")
        if(restoredUid!=uid || header!=lastPayload) {
            var remote=PlayerCodec.decode(header)
            val sessions=documents(ref.collection("sessions"))
            sessions.forEach{doc->val record=PlayerCodec.decode(doc.getString("payload")).results;remote=remote.copy(results=remote.results+record);uploaded+=doc.id}
            val wallets=documents(ref.collection("wallet"));val ledger=wallets.associate {it.getString("eventId")!! to it.getLong("amount")!!.toInt()};wallets.forEach{uploaded+=it.id}
            remote=remote.copy(wallet=ledger)
            p=CloudMerge.merge(local,remote)
            if(p!=local)repository.mergeCloud(remote)
            restoredUid=uid
        }
        val writes=p.results.values.filter {eventId("session:${it.id}") !in uploaded}
        writes.chunked(200).forEach{batch->val b=db.batch();batch.forEach{r->val id=eventId("session:${r.id}");b.set(ref.collection("sessions").document(id),mapOf("payload" to PlayerCodec.encode(Player(results=mapOf(r.id to r))),"createdAt" to FieldValue.serverTimestamp()))};b.commit().await();batch.forEach{uploaded+=eventId("session:${it.id}")}}
        val wallet=if(p.wallet.isEmpty())mapOf("legacy" to p.coins) else p.wallet
        wallet.entries.filter{eventId("wallet:${it.key}") !in uploaded}.chunked(200).forEach{batch->val b=db.batch();batch.forEach{(event,amount)->b.set(ref.collection("wallet").document(eventId("wallet:$event")),mapOf("eventId" to event,"amount" to amount))};b.commit().await();batch.forEach{uploaded+=eventId("wallet:${it.key}")}}
        val payload=PlayerCodec.encode(p.copy(results=emptyMap(),wallet=mapOf("legacy" to (p.wallet["legacy"]?:p.coins))))
        if(payload!=lastPayload) {
            // Transaction protects header metadata from simultaneous device snapshots.
            db.runTransaction {tx->val prior=PlayerCodec.decode(tx.get(ref).getString("payload"));val merged=CloudMerge.merge(p,prior).copy(results=emptyMap(),wallet=mapOf("legacy" to (p.wallet["legacy"]?:p.coins)));tx.set(ref,mapOf("payload" to PlayerCodec.encode(merged),"nickname" to p.nickname,"updatedAt" to FieldValue.serverTimestamp()));null}.await()
            lastPayload=payload
        }
    }
    data class OnlineSession(val id:String,val seed:Int,val level:Int,val kind:GameKind)
    private class CompetitionRejected(val code:Int,message:String):Exception(message)
    private suspend fun post(path:String,payload:org.json.JSONObject):org.json.JSONObject=withContext(Dispatchers.IO) {
        check(rankingConfigured)
        val token=auth?.currentUser?.getIdToken(false)?.await()?.token?:error("Account required")
        val connection=java.net.URI(competitionUrl.trimEnd('/')+path).toURL().openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod="POST";connection.connectTimeout=3000;connection.readTimeout=5000;connection.doOutput=true
            connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Authorization","Bearer $token")
            connection.outputStream.use{it.write(payload.toString().toByteArray())}
            val code=connection.responseCode
            val body=(if(code in 200..299)connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}?:"{}"
            val result=org.json.JSONObject(body)
            if(code !in 200..299)throw CompetitionRejected(code,result.optString("error","Competition request failed"))
            result
        }finally{connection.disconnect()}
    }
    suspend fun startCompetition(kind:GameKind,language:String,daily:Boolean,level:Int?=null):OnlineSession? {
        if(!rankingConfigured || userId==null)return null
        val response=post("/v1/start",org.json.JSONObject().put("game",kind.name).put("language",language).put("daily",daily).apply{if(level!=null)put("level",level)})
        return OnlineSession(response.getString("sessionId"),response.getInt("seed"),response.getInt("level"),GameKind.valueOf(response.getString("game")))
    }
    suspend fun submitCompetition(id:String,commands:List<SessionCommand>,nickname:String) {
        if(!rankingConfigured)return
        val events=org.json.JSONArray()
        commands.forEach {c->events.put(when(c){
            is SessionCommand.Tick->org.json.JSONArray(listOf("tick",c.deltaMs))
            is SessionCommand.Answer->org.json.JSONArray(listOf("answer",c.value))
            is SessionCommand.Pointer->org.json.JSONArray(listOf("pointer",c.x,c.y))
        })}
        val payload=org.json.JSONObject().put("sessionId",id).put("commands",events).put("nickname",nickname)
        withContext(Dispatchers.IO){check(outbox.edit().putString(id,payload.toString()).commit())}
        mutex.withLock {flushCompetition()}
        check(!outbox.contains("rejected:$id")){outbox.getString("rejected:$id","Ranking rejected")!!}
    }
    private suspend fun flushCompetition() {
        if(!rankingConfigured || userId==null)return
        outbox.all.entries.filter{!it.key.startsWith("rejected:")}.take(10).forEach{(id,value)->
            try {
                post("/v1/finish",org.json.JSONObject(value as String))
                check(outbox.edit().remove(id).commit())
            }catch(e:CompetitionRejected) {
                if(e.code in 400..499)check(outbox.edit().remove(id).putString("rejected:$id",e.message).commit()) else throw e
            }
        }
    }
    val leaderboard: LeaderboardRepository=object:LeaderboardRepository {
        private val cache=mutableMapOf<String,Pair<Long,RankingPage>>()
        override suspend fun page(boardId:String,cursor:String?):RankingPage {
            check(configured && userId!=null){"Connect an account first"}
            val key="$boardId:${cursor.orEmpty()}";cache[key]?.takeIf{System.currentTimeMillis()-it.first<60_000}?.let{return it.second}
            var q:Query=db!!.collection("leaderboards").document(boardId).collection("entries").orderBy("score",Query.Direction.DESCENDING).orderBy(FieldPath.documentId()).limit(25)
            if(cursor!=null){val parts=cursor.split('|');require(parts.size==2);q=q.startAfter(parts[0].toLong(),parts[1])}
            var cached=false
            val snapshot=try{q.get(Source.SERVER).await()}catch(e:Exception){if(e is CancellationException)throw e;cached=true;q.get(Source.CACHE).await()}
            val entries=snapshot.documents.map{LeaderboardEntry(it.id,it.getString("nickname")?:"Player",it.getLong("score")?:0,null)}
            val next=entries.lastOrNull()?.takeIf{entries.size==25}?.let{"${it.score}|${it.userId}"}
            return RankingPage(entries,next,cached).also{cache[key]=System.currentTimeMillis() to it}
        }
        override suspend fun rank(boardId:String,userId:String):LeaderboardEntry? {
            check(configured)
            val collection=db!!.collection("leaderboards").document(boardId).collection("entries")
            val doc=collection.document(userId).get(Source.SERVER).await();val score=doc.getLong("score")?:return null
            val count=collection.whereGreaterThan("score",score).count().get(AggregateSource.SERVER).await().count
            return LeaderboardEntry(userId,doc.getString("nickname")?:"Player",score,count+1)
        }
    }
}
