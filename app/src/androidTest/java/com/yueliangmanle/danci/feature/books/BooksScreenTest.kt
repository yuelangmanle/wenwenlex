package com.yueliangmanle.danci.feature.books

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class BooksScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun booksScreenShowsBuiltInAndImportedSections() {
        composeRule.setContent {
            BooksScreen(
                state = BooksUiState(),
                onImportClick = {},
                onBookClick = {},
            )
        }

        composeRule.onNodeWithText("内置词书").assertIsDisplayed()
        composeRule.onNodeWithText("导入词书").assertIsDisplayed()
    }
}
