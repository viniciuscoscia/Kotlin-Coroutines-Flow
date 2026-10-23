package com.viniciuscoscia.kotlincoroutinesflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

class LessonSelectorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonList_isInitialDestination() {
        assertLessonList()
        composeRule.onNodeWithText("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Pigeon").assertDoesNotExist()
        composeRule.onNodeWithTag("rotating_box").assertDoesNotExist()
    }

    @Test
    fun everyLesson_opensAndReturnsUsingUiBack() {
        listOf("Bird simulator", "Counter", "Profile snackbar", "Rotating box").forEach { title ->
            openLesson(title)
            when (title) {
                "Bird simulator" -> composeRule.onNodeWithText("Pigeon").assertIsDisplayed()
                "Counter" -> composeRule.onNodeWithText("Count:", substring = true).assertIsDisplayed()
                "Profile snackbar" -> composeRule.onNodeWithText("Snackbar not showing!").assertIsDisplayed()
                "Rotating box" -> composeRule.onNodeWithTag("rotating_box").assertIsDisplayed()
            }
            composeRule.onNodeWithText("Back").performClick()
            assertLessonList()
        }
    }

    @Test
    fun systemBack_returnsToLessonList() {
        openLesson("Bird simulator")
        composeRule.onNodeWithText("Crow").performClick()

        Espresso.pressBack()

        assertLessonList()
        composeRule.onNodeWithText("Selected bird: Crow").assertDoesNotExist()
        composeRule.onNodeWithText("Back").assertDoesNotExist()
    }

    @Test
    fun activityRecreation_restoresSelectedLessonAndBird() {
        openLesson("Bird simulator")
        composeRule.onNodeWithText("Sparrow").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Selected bird: Sparrow").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertLessonList()
    }

    @Test
    fun leavingBirdLesson_startsFreshWhenReopened() {
        openLesson("Bird simulator")
        composeRule.onNodeWithText("Pigeon").performClick()
        composeRule.onNodeWithText("Selected bird: Pigeon").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        openLesson("Bird simulator")

        composeRule.onNodeWithText("Choose a bird to hear its sound").assertIsDisplayed()
        composeRule.onNodeWithText("Selected bird: Pigeon").assertDoesNotExist()
    }

    @Test
    fun leavingCounterLesson_startsANewProducerWhenReopened() {
        openLesson("Counter")
        composeRule.onNodeWithText("Count: 0").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_100)
        composeRule.onNodeWithText("Count: 0").assertDoesNotExist()
        composeRule.mainClock.autoAdvance = true

        composeRule.onNodeWithText("Back").performClick()
        openLesson("Counter")

        composeRule.onNodeWithText("Count: 0").assertIsDisplayed()
    }

    @Test
    fun leavingProfileLesson_removesPendingSnackbar() {
        openLesson("Profile snackbar")
        composeRule.onNodeWithText("Snackbar not showing!").performClick()
        composeRule.onNodeWithText("Hello world!").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        openLesson("Profile snackbar")

        composeRule.onNodeWithText("Snackbar not showing!").assertIsDisplayed()
        composeRule.onNodeWithText("Hello world!").assertDoesNotExist()
    }

    private fun openLesson(title: String) {
        composeRule.onNodeWithText(title).performScrollTo().performClick()
    }

    private fun assertLessonList() {
        composeRule.onNodeWithText("Coroutine lessons").assertIsDisplayed()
        composeRule.onNodeWithText("Bird simulator").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Counter").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Profile snackbar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rotating box").performScrollTo().assertIsDisplayed()
    }
}
