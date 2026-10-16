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

@Composable internal fun Account(vm:GameViewModel,configured:Boolean,onGoogle:()->Unit) {
    Spacer(Modifier.height(32.dp))
    Text("MINI GAMES / 30 SECONDS",color=Blue,fontSize=12.sp,letterSpacing=2.sp)
    Heading(label("PLAY. IMPROVE.\nBEAT YOUR BEST.","OYNA. GELİŞ.\nREKORUNU GEÇ."),label("15 skill and puzzle games. One competitive profile.","15 beceri ve bulmaca oyunu. Tek rekabet profili."))
    Spacer(Modifier.height(12.dp))
    Action(label("CONTINUE WITH GOOGLE","GOOGLE İLE DEVAM ET"),configured,onGoogle)
    if(!configured)Text(label("Google login will be available after Firebase is configured.","Firebase yapılandırıldıktan sonra Google girişi kullanılabilir."),color=Muted,fontSize=12.sp)
    OutlinedButton(onClick={vm.profile("")},modifier=Modifier.fillMaxWidth().height(52.dp)){Text(label("PLAY AS GUEST","MİSAFİR OLARAK OYNA"))}
    var nickname by remember{mutableStateOf("")}
    OutlinedTextField(nickname,{nickname=it.take(20)},label={Text(label("Local nickname","Yerel oyuncu adı"))},singleLine=true,modifier=Modifier.fillMaxWidth())
    Action(label("CREATE LOCAL PROFILE","YEREL PROFİL OLUŞTUR"),nickname.length>=3 && nickname.all{it.isLetterOrDigit() || it=='_'}){vm.profile(nickname)}
    Text(label("Progress stays on this device until you connect a cloud account.","Bulut hesabını bağlayana kadar ilerlemen bu cihazda saklanır."),color=Muted,fontSize=12.sp)
}
