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

@Composable internal fun Shop(p:Player,vm:GameViewModel) {
    Heading(label("Appearance","Görünüm"),label("Cosmetic changes. Your skill sets the score.","Görsel tercihler. Skoru becerin belirler."))
    Pill("${p.coins} coin",Yellow)
    listOf("lime" to Blue,"violet" to Violet,"coral" to Coral).forEach{(id,c)->ArenaCard(Modifier.fillMaxWidth(),c){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){GameText(when(id){"lime"->"Ion Blue";"violet"->"Ultraviolet";else->"Infrared"},color=c,align=TextAlign.Start);Action(if(id==p.theme)label("EQUIPPED","SEÇİLİ") else if(id in p.owned)label("EQUIP","SEÇ") else "300 coin",id!=p.theme && (id in p.owned || p.coins>=300)){vm.buy(id)}}}}
    TextButton(onClick=vm::leave){Text(label("Back","Geri"))}
}
