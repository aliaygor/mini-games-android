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

@Composable internal fun Missions(p:Player,vm:GameViewModel) {
    Heading(label("Challenges","Mücadeleler"),label("Build mastery. Set your next benchmark.","Ustalığını geliştir. Sıradaki hedefini belirle."))
    val date=LocalDate.now(ZoneOffset.UTC);val challenge=Challenges.daily(date)
    Panel {
        Pill(label("DAILY CHALLENGE · UTC","GÜNLÜK MÜCADELE · UTC"),Yellow)
        GameText(stringResource(name(challenge.kind)),size=24,align=TextAlign.Start)
        Text("${label("Level","Seviye")} ${challenge.level} · $date",color=Muted,fontSize=12.sp)
        val best=p.results.values.filter{it.dailyChallenge && it.day==date.toString()}.maxOfOrNull{it.score}?:0
        Text("${label("Your best today","Bugünkü rekorun")}: $best",color=Blue)
        Action(label("PLAY","OYNA")){vm.prepare(daily=true)}
    }
    Panel {GameText(label("Weekly cup","Haftalık kupa"),size=22,align=TextAlign.Start);Text("${p.weeklyScore(date)} ${label("points this week","bu haftanın puanı")}",color=Yellow);Text(label("Your best score in each game contributes once. Online ranking requires a connected account.","Her oyundaki en iyi skorun bir kez katkı sağlar. Dünya sıralaması bağlı hesap gerektirir."),fontSize=12.sp,color=Muted)}
    (0..2).forEach{i->Panel {
        val target=Economy.missionTarget(i);val progress=Economy.missionProgress(p,i).coerceAtMost(target)
        Text(listOf(label("Complete 3 sessions","3 tur tamamla"),label("Earn 8,000 points","8.000 puan kazan"),label("Play 3 different games","3 farklı oyun oyna"))[i],color=Color.White,fontSize=14.sp)
        LinearProgressIndicator(progress={progress.toFloat()/target},Modifier.fillMaxWidth(),color=Blue)
        Row(verticalAlignment=Alignment.CenterVertically){Text("$progress / $target",Modifier.weight(1f),color=Muted);TextButton(onClick={vm.mission(i)},enabled=progress>=target && i !in p.claimed){Text(if(i in p.claimed)label("Claimed","Alındı") else "+60 coin")}}
    }}
    Action(label("DAILY REWARD","GÜNLÜK ÖDÜL")){vm.navigate("daily")}
}
