package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_words",
    primaryKeys = ["bookId", "wordId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wordId")],
)
data class BookWordEntity(
    val bookId: String,
    val wordId: Long,
    val chapter: String? = null,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList(),
    val note: String? = null,
)
