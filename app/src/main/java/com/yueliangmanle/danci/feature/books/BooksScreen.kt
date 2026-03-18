package com.yueliangmanle.danci.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BooksScreen(
    state: BooksUiState,
    onImportClick: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("正在整理词书列表")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroHeader(onImportClick = onImportClick)
        }
        state.statusMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            SectionTitle(
                title = "内置词书",
                modifier = Modifier.testTag("books_built_in_section"),
            )
        }
        items(state.builtInBooks, key = BookListItem::id) { book ->
            BookCard(book = book, onBookClick = onBookClick)
        }
        item {
            SectionTitle(
                title = "导入词书",
                modifier = Modifier.testTag("books_imported_section"),
            )
        }
        if (state.importedBooks.isEmpty()) {
            item {
                EmptyState(text = "还没有导入词书。支持 Excel 首列单词、第二列中文义，并可用 AI 适配不规整表格。")
            }
        } else {
            items(state.importedBooks, key = BookListItem::id) { book ->
                BookCard(book = book, onBookClick = onBookClick)
            }
        }
    }
}

@Composable
private fun HeroHeader(onImportClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF6EEE4),
                            Color(0xFFE3F0FF),
                            Color(0xFFECE7D9),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "词书与词库",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "内置考试向词库，支持 Excel 导入、AI 适配表格结构，以及导入后批量补全双音标。",
                style = MaterialTheme.typography.bodyMedium,
            )
            AssistChip(
                onClick = onImportClick,
                label = { Text("导入 Excel 词书") },
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.padding(horizontal = 20.dp),
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = book.sourceType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (book.isActive) {
                    Text(
                        text = "当前词书",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (book.description.isNotBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            book.sourceMeta?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
