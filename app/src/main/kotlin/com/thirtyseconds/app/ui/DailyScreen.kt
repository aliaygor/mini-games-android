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

@Composable internal fun Daily(p:Player,vm:GameViewModel) {
    Heading(label("Daily reward","Günlük ödül"),label("A small reward for coming back. No pressure.","Geri dönüşün için küçük bir ödül. Zorunluluk yok."))
    Panel {GameText("${p.streak}",size=48,color=Yellow);Text(label("day streak","günlük seri"),color=Muted);Text("${50+(p.streak.coerceIn(1,7)-1)*10} coin",color=Color.White)}
    Action(label("CLAIM REWARD","ÖDÜLÜ AL"),p.lastRewardDay<LocalDate.now(ZoneOffset.UTC).toString(),vm::daily)
    TextButton(onClick=vm::leave){Text(label("Back","Geri"))}
}
