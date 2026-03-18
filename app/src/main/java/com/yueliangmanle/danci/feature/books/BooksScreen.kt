package com.yueliangmanle.danci.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BooksScreen(
    state: BooksUiState,
    onImportClick: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "选择今天要学的词书",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "先用内置词书起步，也可以导入自己的表。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AssistChip(
                    onClick = onImportClick,
                    label = { Text("导入词书") },
                )
            }
        }
        item {
            SectionTitle(title = "内置词书")
        }
        items(state.builtInBooks, key = BookListItem::id) { book ->
            BookCard(book = book, onBookClick = onBookClick)
        }
        item {
            SectionTitle(title = "导入词书")
        }
        if (state.importedBooks.isEmpty()) {
            item {
                EmptyState(text = "还没有导入词书，先从内置词书开始也可以。")
            }
        } else {
            items(state.importedBooks, key = BookListItem::id) { book ->
                BookCard(book = book, onBookClick = onBookClick)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun BookCard(
    book: BookListItem,
    onBookClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clickable { onBookClick(book.id) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = book.sourceType,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${book.wordCount} 词",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
