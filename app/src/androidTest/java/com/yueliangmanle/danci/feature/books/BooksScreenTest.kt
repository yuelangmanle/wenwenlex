package com.yueliangmanle.danci.feature.books

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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

        composeRule.onNodeWithTag("books_built_in_section").assertIsDisplayed()
        composeRule.onNodeWithTag("books_imported_section").assertIsDisplayed()
    }
}
