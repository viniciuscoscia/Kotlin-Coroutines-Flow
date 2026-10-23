package com.viniciuscoscia.kotlincoroutinesflow

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics.ProfileScreen
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun queuedSnackbars_keepShowingLabelUntilLastDismissed() {
        composeRule.setContent {
            MaterialTheme {
                ProfileScreen()
            }
        }

        composeRule.onNodeWithText("Snackbar not showing!").performClick()
        composeRule.onNodeWithText("Snackbar showing!").performClick()

        dismissCurrentSnackbar()

        composeRule.onNodeWithText("Hello world!").assertIsDisplayed()
        composeRule.onNodeWithText("Snackbar showing!").assertIsDisplayed()

        dismissCurrentSnackbar()

        composeRule.onNodeWithText("Snackbar not showing!").assertIsDisplayed()
    }

    private fun dismissCurrentSnackbar() {
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss) { dismiss -> dismiss() }
    }
}
