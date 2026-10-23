package com.viniciuscoscia.kotlincoroutinesflow
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class LaboratoryUiTest {
 @get:Rule val composeRule=createAndroidComposeRule<MainActivity>()
 @Test fun opensSharedLaboratoryAndReturnsToPreservedLessons() {
  composeRule.waitUntil(timeoutMillis=5000) {
   runCatching { composeRule.onAllNodesWithText("Open multiplatform laboratory").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
  }
  composeRule.onNodeWithText("Open multiplatform laboratory").performClick()
  composeRule.onNodeWithText("Coroutine & Flow Laboratory").assertIsDisplayed()
  composeRule.onNodeWithText("Import text file").assertIsDisplayed()
  composeRule.onNodeWithText("Open existing lessons").performClick()
  composeRule.onNodeWithText("Open multiplatform laboratory").assertIsDisplayed()
 }
 @Test fun invalidBackendEndpointReportsErrorAndAllowsRetry() {
  composeRule.onNodeWithText("Open multiplatform laboratory").performClick()
  composeRule.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("not a url")
  composeRule.onNodeWithText("Connect backend").performScrollTo().performClick()
  composeRule.onNodeWithText("Connection failed. Check the ws:// or wss:// URL and try again.", substring = true).performScrollTo().assertIsDisplayed()
  composeRule.onNodeWithText("Connect backend").performScrollTo().assertIsEnabled().performClick()
 }
}
