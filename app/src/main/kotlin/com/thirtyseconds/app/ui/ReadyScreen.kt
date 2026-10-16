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

@Composable internal fun Ready(s: GameState,vm: GameViewModel) {
    TextButton(onClick=vm::leave){Text(label("Back","Geri"),color=Muted)}
    GameArt(s.kind,Modifier.fillMaxWidth().height(190.dp))
    Heading(stringResource(name(s.kind)),stringResource(rule(s.kind)))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Pill("${label("Level","Seviye")} ${s.level}");Pill(s.difficulty.tier.name,Blue);Pill("${s.durationMs/1000}s",Muted)}
    Panel {Text(label("Score rewards speed, accuracy and sustained combos. Perfect rounds earn a bonus.","Skor; hız, isabet ve kesintisiz komboyla büyür. Hatasız turlar ek bonus kazandırır."),color=Muted,fontSize=13.sp)}
    Action(label("START SESSION","TURU BAŞLAT"),onClick=vm::start)
}
