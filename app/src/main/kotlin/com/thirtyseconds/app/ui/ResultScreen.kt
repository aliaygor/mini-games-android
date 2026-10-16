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

@Composable internal fun Result(ui:UiState,p:Player,adReady:Boolean,vm:GameViewModel,onReward:()->Unit,onAgain:()->Unit,onShuffle:()->Unit,onHome:()->Unit) {
    val r=ui.result!!;val b=r.breakdown
    Heading(stringResource(name(r.kind)),"${label("Level","Seviye")} ${r.level} · ${if(r.completed)label("COMPLETE","TAMAMLANDI") else label("SESSION OVER","TUR BİTTİ")}")
    ArenaCard(Modifier.fillMaxWidth(),if(ui.personalBest)Violet else Blue){Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(label("SESSION SCORE","TUR SKORU"),color=Muted,fontSize=11.sp,letterSpacing=2.sp)
        GameText("%,d".format(r.score),size=48,color=Color.White)
        if(ui.personalBest)Pill(label("NEW PERSONAL BEST","YENİ KİŞİSEL REKOR"),Yellow)
        Text("+${r.xp} XP · +${r.coins} ${label("coins","coin")}",color=Lime,fontSize=13.sp)
    }}
    Panel {
        listOf(label("Base","Temel") to b.base,label("Speed","Hız") to b.speed,label("Accuracy","İsabet") to b.accuracy,label("Combo","Kombo") to b.combo,label("Perfect","Hatasız") to b.perfect).forEach{(text,value)->Row(Modifier.fillMaxWidth()){Text(text,Modifier.weight(1f),color=Muted,fontSize=13.sp);Text("%,d".format(value),color=Color.White,fontSize=13.sp)}}
        Text("${label("Difficulty multiplier","Zorluk çarpanı")} × %.2f".format(b.multiplier),color=Violet,fontSize=12.sp)
    }
    Row(Modifier.fillMaxWidth()){Stat("${r.durationMs/1000}s",label("Time","Süre"),Modifier.weight(1f));Stat("${r.moves}",label("Moves","Hamle"),Modifier.weight(1f));Stat("×${r.maxCombo}",label("Combo","Kombo"),Modifier.weight(1f))}
    if(r.kind==GameKind.REFLEX)Panel{Text("${label("Average","Ortalama")}: ${r.averageReactionMs} ms · ${label("Best","En iyi")}: ${r.bestReactionMs} ms",color=Blue)}
    if(r.optimalMoves>0)Text("${label("Shortest solution","En kısa çözüm")}: ${r.optimalMoves} ${label("moves","hamle")}",color=Muted,fontSize=13.sp)
    if(ui.saving)Text(stringResource(R.string.saving),color=Muted)
    if(ui.error){Text(stringResource(R.string.error),color=Coral);Action(stringResource(R.string.retry),onClick=vm::retrySave)}
    Text(if(ui.rankingVerified)label("Verified for global ranking","Dünya sıralaması için doğrulandı") else if(ui.rankingPending)label("Ranking submission pending","Sıralama gönderimi bekliyor") else label("Local session · cloud ranking not submitted","Yerel tur · dünya sıralamasına gönderilmedi"),color=Muted,fontSize=12.sp)
    val enabled=!ui.saving && !ui.error && !ui.rewardBusy
    Action(label("PLAY NEXT","SONRAKİ TUR"),enabled,onAgain)
    OutlinedButton(onClick=vm::retry,enabled=enabled,modifier=Modifier.fillMaxWidth()){Text(label("Retry this level","Bu seviyeyi tekrar oyna"))}
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedButton(onClick={vm.navigate("leaderboard")},enabled=enabled,modifier=Modifier.weight(1f)){Text(label("Rankings","Sıralama"))}
        OutlinedButton(onClick=onHome,enabled=enabled,modifier=Modifier.weight(1f)){Text(label("Home","Ana sayfa"))}
    }
    val doubled=r.id in p.doubledIds
    TextButton(onClick=onReward,enabled=enabled && adReady && !doubled){Text(label(if(doubled)"Reward received" else "Watch ad · double coins",if(doubled)"Ödül alındı" else "Reklam izle · coin ikiye katla"))}
}
