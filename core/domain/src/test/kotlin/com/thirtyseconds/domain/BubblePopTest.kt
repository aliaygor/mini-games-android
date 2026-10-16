package com.thirtyseconds.domain

import org.junit.Assert.*
import org.junit.Test

class BubblePopTest {
    @Test fun specialChainsPreserveStoneAnchors() {
        val board=listOf(0,1,2,3,0, 1,10,-1,22,2, 2,0,1,2,3, 3,1,2,3,0, 0,2,3,0,1)
        val state=GameState(GameKind.BLAST,81,size=5,board=board,goal=100000,moveLimit=20)
        val next=BlastGame.act(state,6)
        assertEquals(-1,next.board[7])
        assertTrue(8 in next.cleared)
        assertTrue(2 in next.cleared) // Cross triggers the color special in its row.
        assertFalse(7 in next.cleared)
        assertEquals(next.board.size,next.fallOrigins.size)
        next.board.indices.filter {next.board[it]>=0 && next.fallOrigins[it]>=0}.forEach {i->
            assertEquals(i%5,next.fallOrigins[i]%5)
            assertTrue(next.fallOrigins[i]/5<=i/5)
        }
    }

    @Test fun refillsAlwaysRemainPlayableAcrossDifficultyTiers() {
        listOf(1,6,16,31,51).forEach {level->repeat(10) {seed->
            var state=GameEngine.start(GameKind.BLAST,seed,level)
            repeat(12) {
                if(!state.finished) {
                    val index=state.board.indices.firstOrNull {state.board[it]>=10 || BlastGame.group(state,it).size>=2}
                    assertNotNull(index)
                    state=GameEngine.act(state,index!!)
                    assertFalse(state.board.any {it < -1})
                }
            }
        }}
    }
}
