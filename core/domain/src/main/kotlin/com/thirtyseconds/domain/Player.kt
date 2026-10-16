package com.thirtyseconds.domain

import kotlinx.coroutines.flow.Flow
import kotlin.math.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.IsoFields

data class GameResult(
    val id: String,val kind: GameKind,val score: Int,val hits: Int,val mistakes: Int,
    val level: Int=1,val completed: Boolean=false,val durationMs: Long=0,val moves: Int=0,
    val optimalMoves: Int=0,val maxCombo: Int=0,val bestReactionMs: Long=0,val averageReactionMs: Long=0,
    val breakdown: ScoreBreakdown=ScoreBreakdown(score,0,0,0,0,1.0),
    val day: String=LocalDate.now(ZoneOffset.UTC).toString(),val dailyChallenge: Boolean=false,
) {
    val coins get()=10+score.coerceIn(0,2_000_000)/250
    val xp get()=25+score.coerceIn(0,2_000_000)/25+if(completed)100 else 0
    val perfect get()=hits>0 && mistakes==0
}
data class Player(
    val id: String="",val nickname: String="",val profileCreated: Boolean=false,val joined: String="",
    val wallet: Map<String,Int> = emptyMap(),val coins: Int=0,val xp: Int=0,val games: Int=0,val totalScore: Int=0,
    val records: Map<GameKind,Int> = emptyMap(),val gameLevels: Map<GameKind,Int> = emptyMap(),
    val legacyRecords: Map<String,Int> = emptyMap(),val results: Map<String,GameResult> = emptyMap(),
    val achievements: Set<String> = emptySet(),val perfectGames: Int=0,
    val day: String="",val dailyGames: Int=0,val dailyScore: Int=0,val dailyKinds: Set<GameKind> = emptySet(),
    val claimed: Set<Int> = emptySet(),val rewardClaims: Set<String> = emptySet(),val doubledIds: Set<String> = emptySet(),
    val lastRewardDay: String="",val streak: Int=0,val lastSession: String="",val doubledSession: String="",
    val owned: Set<String> = setOf("lime"),val theme: String="lime",val language: String="",
    val sound: Boolean=true,val music: Boolean=false,val haptics: Boolean=true,val telemetry: Boolean=false,
) {
    val level get()=1+(sqrt(xp.toDouble()/100+4)-2).toInt()
    fun gameLevel(kind: GameKind)=gameLevels[kind]?:1
    fun weeklyScore(day: LocalDate)=results.values.filter {LocalDate.parse(it.day).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)==day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) && LocalDate.parse(it.day).get(IsoFields.WEEK_BASED_YEAR)==day.get(IsoFields.WEEK_BASED_YEAR)}.groupBy {it.kind}.values.sumOf {r->r.maxOf {it.score}}
}
interface PlayerRepository {
    val player: Flow<Player>
    suspend fun createProfile(nickname: String)
    suspend fun record(result: GameResult)
    suspend fun doubleCoins(result: GameResult)
    suspend fun claimDaily()
    suspend fun claimMission(index: Int)
    suspend fun buyTheme(theme: String)
    suspend fun mergeCloud(player: Player)
    suspend fun settings(language: String,sound: Boolean,haptics: Boolean,telemetry: Boolean,music: Boolean=false)
}
object AchievementManager {
    fun unlock(p: Player)=buildSet {
        addAll(p.achievements)
        if(p.totalScore>=100_000)add("100K_CLUB")
        if(p.totalScore>=1_000_000)add("1M_CLUB")
        if(p.gameLevel(GameKind.TRAFFIC)>=50)add("TRAFFIC_MASTER")
        if(p.gameLevel(GameKind.DIFFERENCE)>=50)add("SHARP_EYE")
        if(p.results.values.any {it.kind==GameKind.REFLEX && it.bestReactionMs in 1..179})add("SPEED_DEMON")
        if(p.results.values.any {it.kind==GameKind.MEMORY && it.level>=31 && it.perfect})add("PERFECT_MEMORY")
        if(p.perfectGames>=10)add("PERFECT_TEN")
    }
}
object Economy {
    fun wallet(p: Player,id: String,amount: Int): Player {
        val ledger=(if(p.wallet.isEmpty())mapOf("legacy" to p.coins) else p.wallet)+(id to amount)
        return p.copy(wallet=ledger,coins=ledger.values.sum().coerceAtLeast(0))
    }
    fun today(p: Player,day: String)=if(p.day==day)p else p.copy(day=day,dailyGames=0,dailyScore=0,dailyKinds=emptySet(),claimed=emptySet())
    fun record(p: Player,r: GameResult): Player {
        if(r.id in p.results || p.lastSession==r.id)return p
        val advance=ChallengeRules.qualifies(r.kind,r.hits,r.mistakes,r.maxCombo,r.completed)
        val n=p.copy(coins=p.coins+r.coins,xp=p.xp+r.xp,games=p.games+1,totalScore=p.totalScore+r.score,
            records=p.records+(r.kind to max(p.records[r.kind]?:0,r.score)),
            gameLevels=p.gameLevels+(r.kind to max(p.gameLevel(r.kind),r.level+if(advance)1 else 0)),
            results=p.results+(r.id to r),perfectGames=p.perfectGames+if(r.perfect)1 else 0,
            dailyGames=p.dailyGames+1,dailyScore=p.dailyScore+r.score,dailyKinds=p.dailyKinds+r.kind,lastSession=r.id)
        return wallet(n.copy(wallet=if(p.wallet.isEmpty())mapOf("legacy" to p.coins) else p.wallet),"session:${r.id}",r.coins).copy(achievements=AchievementManager.unlock(n))
    }
    fun double(p: Player,r: GameResult)=if(r.id in p.results && r.id !in p.doubledIds)wallet(p,"double:${r.id}",r.coins).copy(doubledSession=r.id,doubledIds=p.doubledIds+r.id) else p
    fun missionProgress(p: Player,i: Int)=when(i){0->p.dailyGames;1->p.dailyScore;2->p.dailyKinds.size;else->0}
    fun missionTarget(i: Int)=when(i){0->3;1->8000;2->3;else->Int.MAX_VALUE}
    fun claim(p: Player,i: Int): Player {
        val id="mission:${p.day}:$i"
        return if(i in 0..2 && id !in p.rewardClaims && missionProgress(p,i)>=missionTarget(i))wallet(p,id,60).copy(claimed=p.claimed+i,rewardClaims=p.rewardClaims+id) else p
    }
    fun daily(p: Player,day: String,yesterday: String): Player {
        if(p.lastRewardDay>=day || "daily:$day" in p.rewardClaims)return p
        val streak=if(p.lastRewardDay==yesterday)p.streak+1 else 1
        return wallet(p,"daily:$day",50+min(streak-1,6)*10).copy(streak=streak,lastRewardDay=day,rewardClaims=p.rewardClaims+"daily:$day")
    }
    fun buy(p: Player,theme: String): Player {
        if(theme !in setOf("lime","violet","coral"))return p
        if(theme in p.owned)return p.copy(theme=theme)
        return if(p.coins>=300)wallet(p,"purchase:$theme",-300).copy(owned=p.owned+theme,theme=theme) else p
    }
}
object ChallengeRules {
    fun minimumHits(kind:GameKind)=when(kind){GameKind.COLOR->18;GameKind.MATH->10;GameKind.MEMORY->12;GameKind.PRECISION->10;GameKind.SEQUENCE->14;GameKind.WORD->3;GameKind.PATTERN->10;GameKind.LOGIC->5;else->0}
    fun qualifies(kind:GameKind,hits:Int,mistakes:Int,maxCombo:Int,completed:Boolean):Boolean {
        if(completed)return true
        val minimum=minimumHits(kind)
        return minimum>0 && hits>=minimum && mistakes*4<=hits && maxCombo>=minOf(4,minimum)
    }
}
object CloudMerge {
    private fun mergeWallet(a: Player,b: Player): Map<String,Int> {
        val left=if(a.wallet.isEmpty())mapOf("legacy" to a.coins) else a.wallet
        val right=if(b.wallet.isEmpty())mapOf("legacy" to b.coins) else b.wallet
        return (left.keys+right.keys).associateWith {key->maxOf(left[key]?:Int.MIN_VALUE,right[key]?:Int.MIN_VALUE)}
    }
    // Union immutable session IDs; shared history contributes exactly once. Currency claims
    // are likewise identified. Never sum two snapshots of the same account.
    fun merge(local: Player,remote: Player): Player {
        var merged=local
        remote.results.values.filter {it.id !in local.results}.forEach {merged=Economy.record(merged,it)}
        val newDoubles=remote.doubledIds-local.doubledIds
        merged=merged.copy(coins=merged.coins+newDoubles.sumOf {merged.results[it]?.coins?:0},doubledIds=local.doubledIds+remote.doubledIds,
            owned=local.owned+remote.owned,achievements=local.achievements+remote.achievements,
            gameLevels=GameKind.entries.associateWith {max(merged.gameLevel(it),remote.gameLevel(it))},
            records=GameKind.entries.associateWith {max(merged.records[it]?:0,remote.records[it]?:0)},
            rewardClaims=local.rewardClaims+remote.rewardClaims,lastRewardDay=maxOf(local.lastRewardDay,remote.lastRewardDay),
            streak=if(remote.lastRewardDay>local.lastRewardDay)remote.streak else local.streak,
            joined=listOf(local.joined,remote.joined).filter {it.isNotBlank()}.minOrNull()?:local.joined)
        // Snapshot-only pre-v2 progress has no event IDs. Preserve maxima on migration.
        return merged.copy(xp=max(merged.xp,remote.xp),games=max(merged.games,remote.games),totalScore=max(merged.totalScore,remote.totalScore),wallet=mergeWallet(local,remote),coins=mergeWallet(local,remote).values.sum().coerceAtLeast(0),perfectGames=max(merged.perfectGames,remote.perfectGames))
    }
}
data class DailyChallenge(val id: String,val kind: GameKind,val seed: Int,val level: Int)
object Challenges {
    fun daily(date: LocalDate): DailyChallenge {
        val epoch=date.toEpochDay();val kinds=listOf(GameKind.BLAST,GameKind.TRAFFIC,GameKind.COLOR,GameKind.MATH,GameKind.MEMORY,GameKind.SUDOKU,GameKind.DOORS)
        return DailyChallenge(date.toString(),kinds[Math.floorMod(epoch,kinds.size.toLong()).toInt()],(epoch*104729).toInt(),10+Math.floorMod(epoch,20).toInt())
    }
}
data class LeaderboardEntry(val userId: String,val nickname: String,val score: Long,val rank: Long?)
enum class RankingPeriod { GLOBAL,WEEKLY,DAILY }
