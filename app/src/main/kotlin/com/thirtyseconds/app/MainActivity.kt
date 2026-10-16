package com.thirtyseconds.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.thirtyseconds.app.ui.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thirtyseconds.domain.*
import com.thirtyseconds.platform.SoundFeedback
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val app = application as MiniGamesApp
        app.ads.requestConsent(this)
        setContent {
            val vm: GameViewModel = viewModel()
            val player by vm.player.collectAsStateWithLifecycle()
            val ui by vm.ui.collectAsStateWithLifecycle()
            val rewardReady by app.ads.rewardReady.collectAsStateWithLifecycle()
            val cloudStatus by app.cloud.status.collectAsStateWithLifecycle()
            val accountName by app.cloud.accountName.collectAsStateWithLifecycle()
            val loginScope=rememberCoroutineScope()
            var loginError by remember { mutableStateOf<String?>(null) }
            var loginBusy by remember { mutableStateOf(false) }
            fun connectGoogle() {
                loginScope.launch {
                    loginBusy=true;loginError=null
                    try { app.cloud.connectGoogle(this@MainActivity) }
                    catch(e:Exception) { if(e is kotlinx.coroutines.CancellationException)throw e;loginError=e.message }
                    finally {loginBusy=false}
                }
            }
            val config = Configuration(LocalConfiguration.current)
            val language = player.language.ifEmpty { Locale.getDefault().language }
            config.setLocale(Locale.forLanguageTag(language))
            val localized = remember(language, LocalConfiguration.current) { createConfigurationContext(config) }
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                val main = accent(player.theme)
                MaterialTheme(colorScheme = darkColorScheme(primary = main, onPrimary = Ink, background = Ink,
                    surface = Panel, onSurface = Color.White, secondary = Violet, onBackground = Color.White)) {
                    val owner = LocalLifecycleOwner.current
                    val haptic = LocalHapticFeedback.current
                    val sound = remember { SoundFeedback(this@MainActivity) }
                    DisposableEffect(owner,player.music) {
                        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) {vm.pause();sound.music(false)} else if(event == Lifecycle.Event.ON_START) sound.music(player.music) }
                        owner.lifecycle.addObserver(observer)
                        onDispose { owner.lifecycle.removeObserver(observer) }
                    }
                    DisposableEffect(sound) { onDispose { sound.close() } }
                    LaunchedEffect(player.music) { sound.music(player.music) }
                    LaunchedEffect(ui.screen) { if(ui.screen=="result" && player.sound) sound.complete() }
                    LaunchedEffect(ui.feedback) {
                        if (ui.feedback > 0) {
                            if (player.sound) {
                                if(ui.success && ui.game?.kind==GameKind.BLAST)sound.pop(ui.game?.combo?:0)
                                else sound.play(ui.success,ui.game?.combo?:0)
                            }
                            if (player.haptics) haptic.performHapticFeedback(if(ui.success) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                        }
                    }
                    BackHandler(enabled = ui.screen != "home") {
                        when (ui.screen) { "game" -> vm.pause(); "result" -> if (!ui.saving && !ui.error && !ui.rewardBusy) vm.leave(); else -> vm.leave() }
                    }
                    fun depart(next: () -> Unit) { app.ads.transition(this@MainActivity, player.games, next) }
                    LaunchedEffect(player.profileCreated) {
                        if(player.profileCreated && app.cloud.configured && app.cloud.userId==null) {
                            try{app.cloud.connectGuest()}catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e}
                        }
                    }
                    var globalRank by remember {mutableStateOf<Long?>(null)}
                    LaunchedEffect(cloudStatus,player.results.size) {
                        val uid=app.cloud.userId
                        if(uid!=null)try{globalRank=app.cloud.leaderboard.rank(RankingBoards.id(RankingPeriod.GLOBAL,null,LocalDate.now(java.time.ZoneOffset.UTC)),uid)?.rank}catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e}
                    }
                    val scroll = rememberScrollState()
                    LaunchedEffect(ui.screen) { scroll.scrollTo(0); if (ui.screen == "result") app.ads.preload(this@MainActivity) }
                    Box(Modifier.fillMaxSize()) {
                    ArenaBackground(menu = ui.screen != "game", theme = player.theme)
                    Scaffold(containerColor = Color.Transparent, bottomBar = {
                        if (player.profileCreated && ui.screen in listOf("home", "missions", "profile", "leaderboard")) BottomNav(ui.screen, vm::navigate)
                    }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                            if (ui.screen == "game") {
                                Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) { Game(ui.game!!, vm,
                                    onHint={vm.pause();app.ads.reward(this@MainActivity,vm::hint){vm.resume()}},hintReady=rewardReady && !ui.challenge && !ui.ranked) }
                            } else Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when {
                                    !ui.loaded -> CircularProgressIndicator()
                                    !player.profileCreated -> Account(vm, app.cloud.googleConfigured && !loginBusy, ::connectGoogle)
                                    ui.screen == "home" -> Home(player, main, vm,globalRank)
                                    ui.screen == "missions" -> Missions(player, vm)
                                    ui.screen == "daily" -> Daily(player, vm)
                                    ui.screen == "shop" -> Shop(player, vm)
                                    ui.screen == "profile" -> {
                                        Profile(player, vm, { app.ads.privacy(this@MainActivity) }, app.ads.privacyRequired())
                                        Panel {
                                            Text(label("Cloud account","Bulut hesabı"),color=Blue)
                                            Text(accountName ?: label("Local profile","Yerel profil"),color=Muted)
                                            Text(cloudStatus,color=Muted,fontSize=12.sp)
                                            Action(label("CONNECT GOOGLE ACCOUNT","GOOGLE HESABINI BAĞLA"),app.cloud.googleConfigured && !loginBusy,::connectGoogle)
                                        }
                                    }
                                    ui.screen == "ready" -> if (ui.game != null) Ready(ui.game!!, vm) else CircularProgressIndicator()
                                    ui.screen == "game" -> Game(ui.game!!, vm)
                                    ui.screen == "leaderboard" -> Leaderboard(app.cloud, player)
                                    ui.screen == "result" -> Result(ui, player, rewardReady, vm,
                                        onReward = {
                                            vm.rewardBusy(true)
                                            app.ads.reward(this@MainActivity, vm::doubleCoins) { vm.rewardBusy(false) }
                                        }, onAgain = { depart { vm.prepare(ui.result!!.kind) } },
                                        onShuffle = { depart { vm.prepare() } }, onHome = { depart(vm::leave) })
                                }
                                if(loginError!=null) Text(loginError!!,color=Coral)
                                if (ui.error && ui.screen != "result") Text(stringResource(R.string.error), color = Coral)
                            }
                        }
                    }
                    }
                    if (ui.paused && ui.screen == "game") AlertDialog(onDismissRequest = {},
                        title = { Text(stringResource(R.string.paused)) }, text = { Text(label("Resume when ready. Pausing a ranked round switches it to local practice.","Hazır olduğunda devam et. Sıralama turunu duraklatmak, turu yerel antrenmana çevirir.")) },
                        confirmButton = { TextButton(onClick = vm::resume) { Text(stringResource(R.string.resume)) } },
                        dismissButton = { TextButton(onClick = vm::leave) { Text(stringResource(R.string.exit)) } })
                }
            }
        }
    }
}

