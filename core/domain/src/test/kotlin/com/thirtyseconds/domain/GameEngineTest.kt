package com.thirtyseconds.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GameEngineTest {
    @Test fun fifteenDistinctGamesFinishAndRejectLateInput() {
        assertEquals(15,GameKind.entries.size)
        GameKind.entries.forEach {kind->val s=GameEngine.start(kind,42);val end=GameEngine.tick(s,s.durationMs+1);assertTrue("$kind",end.finished);assertEquals(end,GameEngine.act(end,0))}
    }
    @Test fun difficultyHasFiveTiersAndChangesGridToleranceAndSpeed() {
        assertEquals(Tier.EASY,DifficultyManager.forLevel(5).tier)
        assertEquals(Tier.NORMAL,DifficultyManager.forLevel(6).tier)
        assertEquals(Tier.HARD,DifficultyManager.forLevel(16).tier)
        assertEquals(Tier.EXPERT,DifficultyManager.forLevel(31).tier)
        assertEquals(Tier.MASTER,DifficultyManager.forLevel(51).tier)
        assertTrue(DifficultyManager.forLevel(60).grid>DifficultyManager.forLevel(1).grid)
        assertTrue(DifficultyManager.forLevel(60).windowMs<DifficultyManager.forLevel(1).windowMs)
    }
    @Test fun scoreHasSpeedAccuracyComboAndNoOldThousandPointCeiling() {
        var s=GameEngine.start(GameKind.MATH,42)
        repeat(30){s=GameEngine.act(s,s.target)}
        assertTrue(s.score>1000);assertTrue(s.maxCombo>=30)
        val b=ScoreManager.breakdown(s);assertTrue(b.combo>0);assertTrue(b.perfect>0);assertEquals(b.total,s.score)
        assertTrue(ScoreManager.hit(s.copy(responseMs=50)).score>ScoreManager.hit(s.copy(responseMs=3000)).score)
    }
    @Test fun trafficGeneratorProducesNonOverlappingReachablePuzzles() {
        for(seed in 0..60) {
            val s=GameEngine.start(GameKind.TRAFFIC,seed,1+seed)
            assertEquals(s.vehicles.sumOf{it.length},TrafficGame.occupied(s.vehicles).size)
            assertFalse("seed $seed starts solved",TrafficGame.solved(s.vehicles))
            assertNotNull("seed $seed",TrafficGame.solve(s.vehicles));assertTrue(s.optimalMoves>0)
        }
    }
    @Test fun sudokuHasExactlyOneValidSolutionAtAllTiers() {
        for(level in listOf(1,6,16,31,60)) for(seed in 0..15) {
            val s=GameEngine.start(GameKind.SUDOKU,seed,level)
            assertEquals(1,SudokuGame.solutionCount(s.board,s.size))
            repeat(s.size){row->assertEquals(s.size,s.solution.subList(row*s.size,(row+1)*s.size).toSet().size)}
            repeat(s.size){col->assertEquals(s.size,(0 until s.size).map{s.solution[it*s.size+col]}.toSet().size)}
            assertTrue(s.board.any{it==0})
        }
    }
    @Test fun pathHasValidKeyBeforeLockedGateAndReachableExit() {
        for(seed in 0..60) {
            var s=GameEngine.start(GameKind.DOORS,seed,40)
            val path=PathGame.path(s.board,s.size,s.selected,s.target)
            assertTrue(path.isNotEmpty())
            path.drop(1).forEach{s=GameEngine.act(s,it)}
            assertTrue(s.completed);assertEquals(0,s.mistakes)
        }
    }
    @Test fun memoryUsesHiddenPatternsAndOrderedInputAtHigherLevel() {
        var s=GameEngine.start(GameKind.MEMORY,1,31)
        assertEquals(6,s.size);assertEquals(s,GameEngine.act(s,s.sequence[0]))
        s=GameEngine.tick(s,s.phaseMs)
        val wrong=GameEngine.act(s,s.sequence[1]);assertEquals(1,wrong.mistakes)
        s.sequence.forEach {s=GameEngine.act(s,it)}
        assertTrue(s.round>1)
    }
    @Test fun earlyReactionIsPenalizedAndEightRoundsReportMilliseconds() {
        var s=GameEngine.start(GameKind.REFLEX,1)
        assertEquals(1,GameEngine.act(s,s.target).mistakes)
        repeat(8){s=GameEngine.tick(s,s.phaseMs);s=GameEngine.tick(s,183);s=GameEngine.act(s,s.target)}
        assertTrue(s.completed);assertEquals(8,s.reactionTimes.size);assertTrue(s.reactionTimes.all{it==183L})
    }
    @Test fun blastIsConnectedGroupPuzzleWithMoveLimitAndSpecials() {
        val s=GameEngine.start(GameKind.BLAST,1,20)
        val index=s.board.indices.first {BlastGame.group(s,it).size>=2}
        val next=GameEngine.act(s,index);assertEquals(1,next.moves);assertTrue(next.score>0)
        assertEquals(s.board.size,next.board.size)
    }
    @Test fun precisionUsesActualTouchDistance() {
        val s=GameEngine.start(GameKind.PRECISION,1,30)
        val perfect=GameEngine.pointer(s,s.targetX,s.targetY)
        assertEquals(500,perfect.baseScore)
        assertEquals(1,GameEngine.pointer(s,0f,0f).mistakes)
    }
    @Test fun resultRewardsAndProgressionAreIdempotentAcrossOlderSessions() {
        val first=GameResult("a",GameKind.COLOR,5000,18,0,completed=true,maxCombo=18)
        val second=first.copy(id="b")
        val p=Economy.record(Economy.record(Player(),first),second)
        assertEquals(p,Economy.record(p,first));assertEquals(2,p.gameLevel(GameKind.COLOR))
        val doubled=Economy.double(p,first);assertEquals(p.coins+first.coins,doubled.coins);assertEquals(doubled,Economy.double(doubled,first))
    }
    @Test fun cloudMergeUnionsSessionsWithoutRepeatingXp() {
        val a=GameResult("a",GameKind.MATH,2500,8,0);val b=a.copy(id="b")
        val local=Economy.record(Player(),a);val remote=Economy.record(local,b)
        val merged=CloudMerge.merge(local,remote)
        assertEquals(2,merged.games);assertEquals(a.xp+b.xp,merged.xp);assertEquals(merged,CloudMerge.merge(merged,remote))
    }
    @Test fun dailyIsUtcSeededAndSameForEveryPlayer() {
        val date=LocalDate.parse("2026-10-05")
        assertEquals(Challenges.daily(date),Challenges.daily(date));assertNotEquals(Challenges.daily(date).seed,Challenges.daily(date.plusDays(1)).seed)
        val p=Economy.daily(Player(),date.toString(),date.minusDays(1).toString());assertEquals(p,Economy.daily(p,date.toString(),date.minusDays(1).toString()))
    }
    @Test fun dictionariesSupportAnagramsAndTurkishLetters() {
        assertTrue(Dictionary.valid("en","TEAR","RATE"));assertFalse(Dictionary.valid("en","AAAA","RATE"))
        assertTrue(Dictionary.words("tr").any{'İ' in it})
    }
}
