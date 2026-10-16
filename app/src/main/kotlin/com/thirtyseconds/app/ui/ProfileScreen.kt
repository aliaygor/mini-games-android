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

@Composable internal fun Profile(p: Player,vm: GameViewModel,privacy:()->Unit,showPrivacy: Boolean) {
    Heading(p.nickname,label("Your competitive profile","Rekabet profilin"))
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Pill("${label("Level","Seviye")} ${p.level}",Blue);Pill("${p.xp} XP");Pill(p.joined,Muted)}
    Panel {
        Row(Modifier.fillMaxWidth()){Stat("${p.games}",label("Sessions","Tur"),Modifier.weight(1f));Stat("${p.perfectGames}",label("Perfect","Hatasız"),Modifier.weight(1f));Stat("%,d".format(p.totalScore),label("Total score","Toplam skor"),Modifier.weight(1f))}
        val best=p.records.maxByOrNull{it.value}
        if(best!=null && best.value>0)Text("${label("Best game","En iyi oyun")}: ${stringResource(name(best.key))}",color=Blue,fontSize=13.sp)
    }
    Panel {
        GameText(label("Achievements","Başarımlar"),size=18,align=TextAlign.Start)
        if(p.achievements.isEmpty())Text(label("Your first achievement is waiting. Complete a perfect run or improve your best.","İlk başarımın seni bekliyor. Hatasız bir tur tamamla veya rekorunu geliştir."),color=Muted,fontSize=13.sp)
        p.achievements.forEach {Pill(it.replace('_',' '),Yellow)}
    }
    Panel {
        GameText(label("Game mastery","Oyun ustalığı"),size=18,align=TextAlign.Start)
        GameKind.entries.forEach{kind->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(stringResource(name(kind)),Modifier.weight(1f),color=Color.White,fontSize=13.sp);Text("Lv ${p.gameLevel(kind)} · ${p.records[kind]?:0}",color=Muted,fontSize=12.sp)}}
    }
    Panel {
        GameText(label("Settings","Ayarlar"),size=18,align=TextAlign.Start)
        Setting(label("Music","Müzik"),p.music){vm.settings(music=it)}
        Setting(stringResource(R.string.sound),p.sound){vm.settings(sound=it)}
        Setting(stringResource(R.string.haptics),p.haptics){vm.settings(haptics=it)}
        Setting(stringResource(R.string.analytics),p.telemetry){vm.settings(telemetryEnabled=it)}
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){TextButton(onClick={vm.settings(language="tr")}){Text("Türkçe")};TextButton(onClick={vm.settings(language="en")}){Text("English")}}
        TextButton(onClick={vm.navigate("shop")}){Text(label("Themes","Temalar"))}
        if(showPrivacy)TextButton(onClick=privacy){Text(stringResource(R.string.privacy))}
    }
}
