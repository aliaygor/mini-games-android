package com.thirtyseconds.domain

import org.junit.Assert.*
import org.junit.Test

class CompetitiveRulesTest {
    @Test fun mergingCloudCannotUndoAPurchaseOrDuplicateDailyCoins() {
        val earned=Economy.record(Player(),GameResult("a",GameKind.MATH,10000,8,0))
        val rich=Economy.wallet(earned,"bonus:test",500)
        val local=Economy.buy(rich,"violet")
        val remote=Economy.daily(rich,"2026-10-05","2026-10-04")
        val merged=CloudMerge.merge(local,remote)
        assertEquals(rich.coins-300+50,merged.coins)
        assertEquals(merged,CloudMerge.merge(merged,remote))
        assertTrue("violet" in merged.owned)
    }
    @Test fun replayRecalculatesScoreInsteadOfTrustingClientTotals() {
        val commands=mutableListOf<SessionCommand>();var s=GameEngine.start(GameKind.MATH,7,16)
        repeat(10){commands+=SessionCommand.Tick(120);s=GameEngine.tick(s,120);commands+=SessionCommand.Answer(s.target);s=GameEngine.act(s,s.target)}
        commands+=SessionCommand.Tick(10_000);s=GameEngine.tick(s,10_000)
        val replay=SessionReplay(GameKind.MATH,7,16,"en",commands).verify()
        assertEquals(s,replay)
    }
    @Test fun logicMatrixHasUniqueAnswerOptionsAtEveryTier() {
        for(level in listOf(1,6,16,31,60))for(seed in 0..50){val s=GameEngine.start(GameKind.LOGIC,seed,level);assertEquals(8,s.board.size);assertEquals(4,s.options.distinct().size);assertTrue(s.target in s.options);assertEquals(1,GameEngine.act(s,s.target).hits)}
    }
    @Test fun numberMergeHasLegalOpeningAndAcceptsMovesIntoEmptyCells() {
        val s=GameEngine.start(GameKind.MERGE,1)
        val merged=GameEngine.act(GameEngine.act(s,0),1)
        assertEquals(1,merged.hits);assertTrue(merged.score>0)
        val index=merged.board.indices.first{merged.board[it]>0 && neighbors(it,4).any{n->merged.board[n]==0}}
        val empty=neighbors(index,4).first{merged.board[it]==0}
        assertEquals(2,GameEngine.act(GameEngine.act(merged,index),empty).moves)
    }
    @Test fun invalidSudokuInputAndTrafficCollisionsCannotAdvancePuzzle() {
        val s=GameEngine.start(GameKind.SUDOKU,3)
        val blank=s.board.indices.first{s.board[it]==0};val selected=GameEngine.act(s,blank)
        val wrong=(s.solution[blank]%s.size)+1
        val failed=GameEngine.act(selected,1000+wrong)
        assertEquals(0,failed.board[blank]);assertEquals(1,failed.mistakes)
        val t=GameEngine.start(GameKind.TRAFFIC,5)
        assertNull(TrafficGame.move(t.vehicles,0,2))
    }
}
