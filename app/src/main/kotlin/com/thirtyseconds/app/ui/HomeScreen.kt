package com.thirtyseconds.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.R
import com.thirtyseconds.app.GameViewModel
import com.thirtyseconds.app.UiState
import com.thirtyseconds.domain.*
import java.time.LocalDate
import java.time.ZoneOffset

@Composable internal fun Home(p: Player,main: Color,vm: GameViewModel,globalRank:Long?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(main.copy(alpha=.16f)).clickable{vm.navigate("profile")},contentAlignment=Alignment.Center){GameText(p.nickname.take(2).uppercase(),size=16,color=main)}
        Column(Modifier.weight(1f)){GameText(p.nickname.uppercase(),size=18,align=TextAlign.Start);Text("${label("Level","Seviye")} ${p.level} · ${p.xp} XP",color=Muted,fontSize=11.sp,lineHeight=15.sp)}
        Pill("${p.coins} ${label("coins","coin")}",Yellow)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text("${label("Personal score","Toplam skor")}  ${"%,d".format(p.totalScore)}",color=Muted,fontSize=12.sp)
        Text("${p.streak} ${label("day streak","günlük seri")}",color=Yellow,fontSize=12.sp)
    }
    val lower=100*((p.level+1)*(p.level+1)-4)
    val upper=100*((p.level+2)*(p.level+2)-4)
    LinearProgressIndicator(progress={((p.xp-lower).toFloat()/(upper-lower)).coerceIn(0f,1f)},Modifier.fillMaxWidth().height(3.dp),color=main,trackColor=Panel)
    val challenge=Challenges.daily(LocalDate.now(ZoneOffset.UTC))
    ArenaCard(Modifier.fillMaxWidth(),Violet) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                Text(label("DAILY CHALLENGE","GÜNLÜK MÜCADELE"),color=Yellow,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                GameText(stringResource(name(challenge.kind)),Modifier.padding(top=6.dp),size=22,align=TextAlign.Start)
                Text("${label("Level","Seviye")} ${challenge.level} · ${label("One shared challenge","Herkese aynı mücadele")}",color=Muted,fontSize=11.sp,lineHeight=15.sp)
            };GameArt(challenge.kind,Modifier.size(64.dp))}
            Action(label("PLAY CHALLENGE","MÜCADELEYE BAŞLA")){vm.prepare(daily=true)}
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        Text(label("Find your next challenge","Sıradaki mücadeleni seç"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text("15 ${label("games","oyun")}",color=Muted,fontSize=11.sp)
    }
    var category by remember{mutableStateOf<Category?>(null)}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        CategoryTab(label("All games","Tüm oyunlar"),category==null){category=null}
        Category.entries.forEach {c->CategoryTab(when(c){Category.QUICK->label("Quick","Hız");Category.PUZZLE->label("Puzzle","Bulmaca");Category.LOGIC->label("Logic","Mantık");Category.SKILL->label("Skill","Beceri");Category.MEMORY->label("Memory","Hafıza")},category==c){category=c}}
    }
    GameKind.entries.filter{category==null || it.category==category}.chunked(2).forEach {games->
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {games.forEach{kind->
            ArenaCard(Modifier.weight(1f),gameColor(kind),{vm.prepare(kind)}) {
                Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    GameArt(kind,Modifier.fillMaxWidth().height(120.dp))
                    Column(Modifier.padding(start=4.dp,end=4.dp,bottom=6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        GameText(stringResource(name(kind)),size=16,align=TextAlign.Start)
                        Text(when(kind){GameKind.BLAST->label("Connect. Pop. Chain.","Birleştir. Patlat. Zincirle.");GameKind.TRAFFIC->label("Clear the way out.","Çıkışa giden yolu aç.");else->label("Beat your personal best.","Kendi rekorunu aş.")},fontSize=11.sp,lineHeight=15.sp,color=Muted)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Lv ${p.gameLevel(kind)}",fontSize=11.sp,color=gameColor(kind));Text("${label("Best","Rekor")} ${p.records[kind]?:0}",fontSize=10.sp,color=Muted)}
                    }
                }
            }
        };if(games.size==1)Spacer(Modifier.weight(1f))}
    }
}

@Composable private fun CategoryTab(text:String,selected:Boolean,onClick:()->Unit) {
    Text(text,Modifier.clip(RoundedCornerShape(12.dp)).background(if(selected)Blue.copy(alpha=.18f) else Color.White.copy(alpha=.04f)).clickable(onClick=onClick).padding(horizontal=14.dp,vertical=10.dp),color=if(selected)Blue else Muted,fontSize=12.sp,lineHeight=16.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)
}
