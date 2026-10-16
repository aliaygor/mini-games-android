package com.thirtyseconds.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thirtyseconds.domain.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:GameViewModel
    private fun profile() {
        compose.runOnUiThread {
            vm=ViewModelProvider(compose.activity)[GameViewModel::class.java]
            if(!vm.player.value.profileCreated)vm.profile("")
            vm.settings(language="en")
        }
        compose.waitUntil(8000){vm.player.value.profileCreated && vm.player.value.language=="en"}
    }
    @Test fun profileAndChallengesSurviveRecreation() {
        profile()
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Your competitive profile").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Your competitive profile").assertIsDisplayed()
        compose.onNodeWithText("Challenges").performClick()
        compose.onNodeWithText("Weekly cup").assertIsDisplayed()
    }
    @Test fun allFifteenGamesRenderAndPause() {
        profile()
        GameKind.entries.forEach {kind->
            compose.runOnUiThread{vm.prepare(kind)}
            compose.waitUntil(5000){vm.ui.value.game?.kind==kind && !vm.ui.value.saving}
            compose.runOnUiThread{vm.start();vm.pause()}
            compose.onNodeWithContentDescription("Pause").assertExists()
            compose.onNodeWithText("Take a breath").assertIsDisplayed()
            compose.runOnUiThread{vm.resume()}
            android.os.SystemClock.sleep(180)
            val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(compose.activity.externalCacheDir,"competitive-${kind.name.lowercase()}.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            compose.runOnUiThread{vm.pause()}
            compose.onNodeWithText("Leave round").performClick()
        }
    }
    @Test fun realSudokuCompletionRecordsProgressAndResult() {
        profile()
        compose.runOnUiThread{vm.prepare(GameKind.SUDOKU)}
        compose.waitUntil(5000){vm.ui.value.game?.kind==GameKind.SUDOKU}
        val before=vm.player.value.games
        compose.runOnUiThread {
            vm.start()
            val puzzle=vm.ui.value.game!!
            puzzle.board.indices.filter{puzzle.board[it]==0}.forEach{i->vm.answer(i);vm.answer(1000+puzzle.solution[i])}
        }
        compose.waitUntil(8000){vm.ui.value.screen=="result" && !vm.ui.value.saving}
        compose.onNodeWithText("SESSION SCORE").assertIsDisplayed()
        org.junit.Assert.assertTrue(vm.ui.value.result?.completed==true)
        compose.waitUntil(5000){vm.player.value.games==before+1 && vm.player.value.gameLevel(GameKind.SUDOKU)>=2}
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("SESSION SCORE").assertIsDisplayed()
    }
}
