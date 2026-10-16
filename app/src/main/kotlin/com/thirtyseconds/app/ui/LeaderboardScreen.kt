package com.thirtyseconds.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.thirtyseconds.domain.*
import com.thirtyseconds.platform.FirebaseServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

@Composable internal fun Leaderboard(cloud:FirebaseServices,p:Player) {
    Heading(label("Global leaderboard","Dünya sıralaması"),label("Verified scores. Real competition.","Doğrulanmış skorlar. Gerçek rekabet."))
    var period by remember{mutableStateOf(RankingPeriod.GLOBAL)}
    var game by remember{mutableStateOf<GameKind?>(null)}
    var entries by remember{mutableStateOf<List<LeaderboardEntry>>(emptyList())}
    var own by remember{mutableStateOf<LeaderboardEntry?>(null)}
    var cursor by remember{mutableStateOf<String?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    var loading by remember{mutableStateOf(false)}
    var cached by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val board=RankingBoards.id(period,game,LocalDate.now(ZoneOffset.UTC))
    fun load(reset:Boolean) {
        if(loading || !cloud.configured || cloud.userId==null)return
        loading=true;error=null
        scope.launch {
            try {
                val page=cloud.leaderboard.page(board,if(reset)null else cursor)
                entries=if(reset)page.entries else entries+page.entries
                cursor=page.cursor;cached=page.cached
                if(reset)own=cloud.leaderboard.rank(board,cloud.userId!!)
            }catch(e:Exception){if(e is CancellationException)throw e;error=e.message}
            finally{loading=false}
        }
    }
    LaunchedEffect(board){entries=emptyList();cursor=null;own=null;load(true)}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){RankingPeriod.entries.forEach{r->FilterChip(period==r,{period=r},label={Text(when(r){RankingPeriod.GLOBAL->label("Global","Genel");RankingPeriod.WEEKLY->label("Weekly","Haftalık");RankingPeriod.DAILY->label("Daily","Günlük")})})}}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(game==null,{game=null},label={Text(label("All games","Tüm oyunlar"))});GameKind.entries.forEach{k->FilterChip(game==k,{game=k},label={Text(androidx.compose.ui.res.stringResource(name(k)))})}}
    if(cloud.userId==null)Panel {
        Text(label("Connect your account to load the real rankings. Local records remain available offline.","Gerçek sıralamayı görmek için hesabını bağla. Yerel rekorların çevrimdışı kullanılabilir."),color=Muted,fontSize=13.sp)
        Text(label("Leaderboard service must be deployed before scores can be ranked.","Skorların sıralanması için sıralama servisinin kurulması gerekiyor."),color=Muted,fontSize=12.sp)
    }
    if(loading)CircularProgressIndicator()
    if(error!=null) {Text(error!!,color=Coral,fontSize=12.sp);OutlinedButton(onClick={load(true)}){Text(label("Retry","Yeniden dene"))}}
    if(cloud.userId!=null && !loading && error==null && entries.isEmpty())Panel{Text(label("No verified scores for this board yet.","Bu sıralamada henüz doğrulanmış skor yok."),color=Muted)}
    if(cached)Text(label("Cached standings · offline","Önbellekteki sıralama · çevrimdışı"),color=Yellow,fontSize=11.sp)
    entries.forEachIndexed{i,e->ArenaCard(Modifier.fillMaxWidth(),if(e.userId==cloud.userId)Blue else Muted){Row(Modifier.padding(14.dp).fillMaxWidth()){
        Text("${i+1}",Modifier.width(32.dp),color=Yellow,fontSize=13.sp);Text(e.nickname,Modifier.weight(1f),color=Color.White,fontSize=13.sp);Text("%,d".format(e.score),color=Color.White,fontSize=13.sp)
    }}}
    own?.let{e->Panel{Text("${label("YOU","SEN")} #${e.rank} · %,d".format(e.score),color=Blue)}}
    if(cursor!=null)OutlinedButton(onClick={load(false)},enabled=!loading){Text(label("Load more","Daha fazla"))}
}
