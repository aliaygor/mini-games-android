package com.thirtyseconds.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtyseconds.domain.*
import com.thirtyseconds.platform.Telemetry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

data class UiState(val screen: String="home",val game: GameState?=null,val sessionId: String="",val result: GameResult?=null,val paused: Boolean=false,
    val saving: Boolean=false,val rewardPending: Boolean=false,val error: Boolean=false,val feedback: Int=0,val success: Boolean=true,val rewardBusy: Boolean=false,
    val ranked: Boolean=false,val rankingPending:Boolean=false,val rankingVerified:Boolean=false,val loaded: Boolean=false,val challenge: Boolean=false,val personalBest: Boolean=false)
@dagger.hilt.android.lifecycle.HiltViewModel
class GameViewModel @javax.inject.Inject constructor(private val repo: PlayerRepository,private val telemetry: Telemetry,private val cloud:com.thirtyseconds.platform.FirebaseServices): ViewModel() {
    private val state=MutableStateFlow(UiState());val ui=state.asStateFlow()
    val player=repo.player.onEach{state.update{s->s.copy(loaded=true)}}.catch{telemetry.error(it);state.update{s->s.copy(error=true)};emit(Player())}.stateIn(viewModelScope,SharingStarted.Eagerly,Player())
    private val commands=mutableListOf<SessionCommand>()
    private var ticker: Job?=null;private var preparation: Job?=null;private var lastKind: GameKind?=null
    init {viewModelScope.launch{player.collect{telemetry.consent(it.telemetry)}}}
    fun navigate(screen: String){state.update{it.copy(screen=screen)}}
    fun profile(name: String)=action {repo.createProfile(name);telemetry.event(if(name.isBlank())"guest_created" else "login","profile")}
    fun prepare(kind: GameKind?=null,daily: Boolean=false,retryGame:GameState?=null) {
        ticker?.cancel();preparation?.cancel()
        val p=player.value;val challenge=Challenges.daily(LocalDate.now(ZoneOffset.UTC))
        val selected=if(daily)challenge.kind else retryGame?.kind?:kind?:GameKind.entries.filter{it!=lastKind}.random()
        lastKind=selected
        commands.clear()
        state.value=UiState(screen="ready",saving=true,loaded=true,challenge=daily,sessionId=UUID.randomUUID().toString())
        preparation=viewModelScope.launch {
            val language=p.language.ifBlank{java.util.Locale.getDefault().language}
            val online=try{cloud.startCompetition(selected,language,daily,retryGame?.level)}catch(e:Exception){if(e is CancellationException)throw e;null}
            val game=withContext(Dispatchers.Default){GameEngine.start(online?.kind?:selected,online?.seed?:retryGame?.seed?:if(daily)challenge.seed else Random.nextInt(),online?.level?:retryGame?.level?:if(daily)challenge.level else p.gameLevel(selected),language)}
            if(online!=null)state.update{it.copy(sessionId=online.id,ranked=true)}
            state.update{it.copy(game=game,saving=false)}
        }
    }
    fun retry(){val game=state.value.game?:return;prepare(game.kind,state.value.challenge,game)}
    fun start() {
        if(state.value.game==null || state.value.saving)return
        state.update{it.copy(screen="game",paused=false)}
        telemetry.event(if(state.value.challenge)"daily_challenge_started" else "game_started",state.value.game!!.kind.name)
        runTimer()
    }
    private fun runTimer() {
        ticker?.cancel()
        ticker=viewModelScope.launch {
            var previous=SystemClock.elapsedRealtime()
            while(isActive && state.value.screen=="game" && !state.value.paused) {
                delay(if(state.value.game?.kind==GameKind.PRECISION)16 else 32)
                val now=SystemClock.elapsedRealtime();val game=state.value.game?:break
                val delta=now-previous
                if(state.value.ranked)commands+=SessionCommand.Tick(delta)
                val next=GameEngine.tick(game,delta);previous=now
                state.update{it.copy(game=next)}
                if(next.finished){finish(next);break}
            }
        }
    }
    fun answer(choice: Int) {if(state.value.ranked && !state.value.paused && state.value.screen=="game")commands+=SessionCommand.Answer(choice);input {GameEngine.act(it,choice)}}
    fun pointer(x: Float,y: Float) {if(state.value.ranked && !state.value.paused && state.value.screen=="game")commands+=SessionCommand.Pointer(x,y);input {GameEngine.pointer(it,x,y)}}
    private fun input(reducer:(GameState)->GameState) {
        val s=state.value;if(s.paused || s.screen!="game")return
        val game=s.game?:return;val next=reducer(game)
        val feedback=next.hits!=game.hits || next.mistakes!=game.mistakes
        state.update{it.copy(game=next,feedback=it.feedback+if(feedback)1 else 0,success=next.mistakes==game.mistakes)}
        if(next.finished){ticker?.cancel();viewModelScope.launch{finish(next)}}
    }
    fun hint() {state.update{it.copy(game=it.game?.let(GameEngine::hint))}}
    private suspend fun finish(game: GameState) {
        if(state.value.screen!="game")return
        val ranked=state.value.ranked;val trace=commands.toList()
        val times=game.reactionTimes
        val r=GameResult(state.value.sessionId,game.kind,ScoreManager.breakdown(game).total,game.hits,game.mistakes,game.level,game.completed,game.elapsedMs,game.moves,game.optimalMoves,game.maxCombo,times.minOrNull()?:0,if(times.isEmpty())0 else times.average().toLong(),ScoreManager.breakdown(game),dailyChallenge=state.value.challenge)
        state.update{it.copy(screen="result",result=r,saving=true,personalBest=r.score>(player.value.records[r.kind]?:0))};save(r)
        if(ranked) {
            state.update{if(it.result?.id==r.id)it.copy(rankingPending=true) else it}
            try{cloud.submitCompetition(r.id,trace,player.value.nickname);state.update{if(it.result?.id==r.id)it.copy(rankingPending=false,rankingVerified=true) else it}}
            catch(e:Exception){if(e is CancellationException)throw e;telemetry.error(e)}
        }
    }
    private suspend fun save(r: GameResult) {
        try {
            val before=player.value;repo.record(r)
            telemetry.event(if(r.completed)"level_completed" else "game_completed",r.kind.name,r.score)
            if(state.value.personalBest)telemetry.event("personal_best",r.kind.name,r.score)
            if(r.dailyChallenge)telemetry.event("daily_challenge_completed",r.kind.name,r.score)
            val after=Economy.record(before,r)
            (after.achievements-before.achievements).forEach{telemetry.event("achievement_unlocked",it)}
            if(after.level>before.level)telemetry.event("player_level_up",r.kind.name,after.level)
            state.update{it.copy(saving=false,error=false)}
        }catch(e: Exception){if(e is CancellationException)throw e;telemetry.error(e);state.update{it.copy(saving=false,error=true)}}
    }
    fun retrySave(){if(state.value.rewardPending){doubleCoins();return};val r=state.value.result?:return;state.update{it.copy(saving=true)};viewModelScope.launch{save(r)}}
    fun pause(){if(state.value.screen=="game"){ticker?.cancel();state.update{it.copy(paused=true,ranked=false)}}}
    fun resume(){if(state.value.screen!="game")return;state.update{it.copy(paused=false)};runTimer()}
    fun leave(){ticker?.cancel();preparation?.cancel();state.value=UiState(loaded=state.value.loaded)}
    private fun action(block:suspend()->Unit){viewModelScope.launch{try{block();state.update{it.copy(error=false)}}catch(e:Exception){if(e is CancellationException)throw e;telemetry.error(e);state.update{it.copy(error=true)}}}}
    fun daily()=action{repo.claimDaily()}
    fun mission(index:Int)=action{repo.claimMission(index)}
    fun buy(theme:String)=action{repo.buyTheme(theme)}
    fun settings(language:String=player.value.language,sound:Boolean=player.value.sound,haptics:Boolean=player.value.haptics,telemetryEnabled:Boolean=player.value.telemetry,music:Boolean=player.value.music)=action{repo.settings(language,sound,haptics,telemetryEnabled,music)}
    fun rewardBusy(busy:Boolean){state.update{it.copy(rewardBusy=busy)}}
    fun doubleCoins(){val r=state.value.result?:return;state.update{it.copy(saving=true,rewardPending=true)};action{repo.doubleCoins(r);telemetry.event("rewarded_ad",r.kind.name);state.update{it.copy(saving=false,rewardPending=false)}}}
}
