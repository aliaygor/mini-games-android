package com.thirtyseconds.domain

import org.junit.Assert.*
import org.junit.Test

class ChallengeQualityTest {
    @Test fun parkingCatalogueRequiresRearrangingAtLeastThreeOtherCars() {
        ParkingCatalogue.boards.forEach {layout->
            assertEquals(layout.optimal,TrafficGame.solve(layout.cars))
            assertTrue(layout.optimal>=10)
            val red=layout.cars.first {it.id==0}
            val blockers=layout.cars.count {!it.horizontal && it.x>red.x+1 && red.y in it.y until it.y+it.length}
            assertTrue(blockers>=2)
            var game=GameEngine.start(GameKind.TRAFFIC,ParkingCatalogue.boards.indexOf(layout))
            repeat(10){game=GameEngine.act(game,1)}
            assertFalse(game.completed)
        }
    }
    @Test fun trafficDifficultyHasRealMinimumSolutionLengths() {
        for(level in listOf(1,6,16,31,51))for(seed in 0..10){
            val s=GameEngine.start(GameKind.TRAFFIC,seed,level)
            assertTrue(s.optimalMoves>=when(level){1->10;6->12;16->15;31->20;else->25})
            assertEquals(s,GameEngine.start(GameKind.TRAFFIC,seed,level))
        }
    }
    @Test fun bubbleCompletionDependsOnColorsNotScoreBonuses() {
        val s=GameEngine.start(GameKind.BLAST,1).copy(score=100000)
        val next=GameEngine.act(s,0)
        assertFalse(next.completed)
        assertTrue(next.collected.values.sum()>=2)
        assertEquals(3,next.colorGoals.size)
    }
    @Test fun mathUsesFourPlausibleChoicesInsteadOfBinaryGuessing() {
        repeat(50){seed->val s=GameEngine.start(GameKind.MATH,seed,16);assertEquals(4,s.options.distinct().size);assertTrue(s.target in s.options);assertTrue(s.expression.endsWith("= ?"))}
    }
    @Test fun repeatedPrecisionInputCannotFarmOneRenderedFrame() {
        val s=GameEngine.start(GameKind.PRECISION,11)
        val first=GameEngine.pointer(s,s.targetX,s.targetY)
        assertEquals(first,GameEngine.pointer(first,s.targetX,s.targetY))
        val elapsed=GameEngine.tick(first,200)
        assertEquals(2,GameEngine.pointer(elapsed,elapsed.targetX,elapsed.targetY).hits)
    }
    @Test fun timedGamesRequireAccuracyAndAnActualRunToAdvance() {
        assertFalse(ChallengeRules.qualifies(GameKind.MATH,6,0,6,false))
        assertFalse(ChallengeRules.qualifies(GameKind.MATH,12,8,5,false))
        assertTrue(ChallengeRules.qualifies(GameKind.MATH,12,2,5,false))
        assertFalse(ChallengeRules.qualifies(GameKind.TRAFFIC,100,0,100,false))
    }
}
