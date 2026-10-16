package com.thirtyseconds.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.FieldValue
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.thirtyseconds.domain.*
import org.json.JSONObject
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.random.Random

private fun JSONObject.commands():List<SessionCommand> {
    val a=getJSONArray("commands");require(a.length()<=8000)
    return (0 until a.length()).map{i->val c=a.getJSONArray(i);when(c.getString(0)){
        "tick"->SessionCommand.Tick(c.getLong(1))
        "answer"->SessionCommand.Answer(c.getInt(1))
        "pointer"->SessionCommand.Pointer(c.getDouble(1).toFloat(),c.getDouble(2).toFloat())
        else->error("Unknown command")
    }}
}
private fun HttpExchange.reply(code:Int,j:JSONObject){val bytes=j.toString().toByteArray();responseHeaders.add("Content-Type","application/json; charset=utf-8");sendResponseHeaders(code,bytes.size.toLong());responseBody.use{it.write(bytes)}}
fun main() {
    FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build())
    val db=FirestoreClient.getFirestore();val auth=FirebaseAuth.getInstance()
    val server=HttpServer.create(InetSocketAddress((System.getenv("PORT")?:"8080").toInt()),0)
    server.executor=Executors.newFixedThreadPool(8)
    server.createContext("/health"){it.reply(200,JSONObject().put("status","ok"))}
    server.createContext("/v1/"){exchange->
        try {
            require(exchange.requestMethod=="POST")
            val bearer=exchange.requestHeaders.getFirst("Authorization")?:error("Authentication required")
            require(bearer.startsWith("Bearer "))
            val uid=auth.verifyIdToken(bearer.removePrefix("Bearer "),true).uid
            val bytes=exchange.requestBody.readNBytes(1_000_001);require(bytes.size<=1_000_000)
            val input=JSONObject(String(bytes));val now=System.currentTimeMillis();val date=LocalDate.now(ZoneOffset.UTC)
            val competition=db.collection("competitionProfiles").document(uid)
            when(exchange.requestURI.path) {
                "/v1/start"->{
                    val challenge=input.optBoolean("daily");val daily=Challenges.daily(date)
                    val kind=if(challenge)daily.kind else GameKind.valueOf(input.getString("game"))
                    val language=input.optString("language","en").let{if(it=="tr")"tr" else "en"}
                    val sessionId=UUID.randomUUID().toString();val ref=db.collection("competitionSessions").document(sessionId)
                    val session=db.runTransaction {tx->
                        val profile=tx.get(competition).get()
                        require(now-(profile.getLong("lastStartMs")?:0)>=3000){"Start rate limit"}
                        val maximum=((profile.get("levels") as? Map<*,*>)?.get(kind.name) as? Number)?.toInt()?:1
                        val level=if(challenge)daily.level else input.optInt("level",maximum).coerceIn(1,maximum)
                        val seed=if(challenge)daily.seed else Random.nextInt()
                        val data=mapOf("uid" to uid,"game" to kind.name,"seed" to seed,"level" to level,"language" to language,"daily" to challenge,"day" to date.toString(),"startedMs" to now,"used" to false)
                        tx.set(ref,data);tx.set(competition,mapOf("lastStartMs" to now,"activeSession" to sessionId),com.google.cloud.firestore.SetOptions.merge());data
                    }.get()
                    exchange.reply(200,JSONObject(session).put("sessionId",sessionId))
                }
                "/v1/finish"->{
                    val id=input.getString("sessionId");require(id.length==36)
                    val ref=db.collection("competitionSessions").document(id);val snap=ref.get().get()
                    require(snap.getString("uid")==uid){"Wrong session owner"}
                    val kind=GameKind.valueOf(snap.getString("game")!!);val seed=snap.getLong("seed")!!.toInt();val level=snap.getLong("level")!!.toInt()
                    val commands=input.commands();val replay=SessionReplay(kind,seed,level,snap.getString("language")!!,commands)
                    val game=replay.verify();require(game.finished){"Session is incomplete"}
                    require(now-snap.getLong("startedMs")!! in game.elapsedMs-250..900_000){"Invalid timing"}
                    require(game.hits+game.mistakes<=600)
                    if(kind==GameKind.REFLEX)require(game.reactionTimes.all{it>=80}){"Implausible reaction time"}
                    val score=ScoreManager.breakdown(game).total
                    val daily=snap.getBoolean("daily")==true
                    val sessionDate=LocalDate.parse(snap.getString("day"))
                    val periods=if(daily)RankingPeriod.entries else listOf(RankingPeriod.GLOBAL,RankingPeriod.WEEKLY)
                    val boards=periods.flatMap{period->listOf(RankingBoards.id(period,kind,sessionDate),RankingBoards.id(period,null,sessionDate))}
                    val refs=boards.associateWith{board->db.collection("leaderboards").document(board).collection("entries").document(uid)}
                    val account=auth.getUser(uid);val nickname=(account.displayName?:input.optString("nickname","Player")).take(20).ifBlank{"Player"}
                    db.runTransaction {tx->
                        val session=tx.get(ref).get();val profile=tx.get(competition).get()
                        if(session.getBoolean("used")==true)return@runTransaction null
                        require(profile.getString("activeSession")==id){"A newer session superseded this one"}
                        val previous=refs.mapValues{tx.get(it.value).get()}
                        refs.forEach{(board,entry)->
                            val old=previous.getValue(board)
                            if(board.endsWith("_all")) {
                                val best=(old.get("records") as? Map<*,*>)?.entries?.associate{it.key.toString() to (it.value as Number).toLong()}?.toMutableMap()?:mutableMapOf()
                                best[kind.name]=maxOf(best[kind.name]?:0,score.toLong())
                                tx.set(entry,mapOf("nickname" to nickname,"score" to best.values.sum(),"records" to best,"updatedAt" to FieldValue.serverTimestamp()))
                            } else tx.set(entry,mapOf("nickname" to nickname,"score" to maxOf(old.getLong("score")?:0,score.toLong()),"updatedAt" to FieldValue.serverTimestamp()))
                        }
                        val levels=(profile.get("levels") as? Map<*,*>)?.entries?.associate{it.key.toString() to (it.value as Number).toInt()}?.toMutableMap()?:mutableMapOf()
                        if(!daily && ChallengeRules.qualifies(kind,game.hits,game.mistakes,game.maxCombo,game.completed))levels[kind.name]=maxOf(levels[kind.name]?:1,level+1)
                        tx.set(competition,mapOf("levels" to levels),com.google.cloud.firestore.SetOptions.merge())
                        tx.update(ref,mapOf("used" to true,"verifiedScore" to score,"verifiedAt" to FieldValue.serverTimestamp()));null
                    }.get()
                    exchange.reply(200,JSONObject().put("score",score).put("verified",true))
                }
                else->exchange.reply(404,JSONObject().put("error","Unknown endpoint"))
            }
        }catch(e:Exception){exchange.reply(400,JSONObject().put("error",e.cause?.message?:e.message?:"Request rejected"))}
    }
    server.start()
}
