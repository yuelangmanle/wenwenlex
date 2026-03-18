package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.importer.JsonBookImporter
import com.yueliangmanle.danci.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WordRepository {
    fun observeWords(query: String = ""): Flow<List<Word>>
    suspend fun getWord(wordId: Long): Word?
    suspend fun insertWord(word: Word): Long
    suspend fun importWords(words: List<ImportedWord>): List<Long>
}

class RoomWordRepository(
    private val wordDao: WordDao,
) : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> {
        val source = if (query.isBlank()) wordDao.observeWords() else wordDao.searchWords(query)
        return source.map { words -> words.map(WordEntity::asExternalModel) }
    }

    override suspend fun getWord(wordId: Long): Word? = wordDao.getWordById(wordId)?.asExternalModel()

    override suspend fun insertWord(word: Word): Long {
        val insertedId = wordDao.insertWord(word.asEntity())
        if (insertedId != -1L) {
            return insertedId
        }
        return requireNotNull(wordDao.getWordByLemma(word.lemma)) {
            "Expected canonical word to exist for lemma=${word.lemma}"
        }.id
    }

    override suspend fun importWords(words: List<ImportedWord>): List<Long> =
        words.map { importedWord -> insertWord(importedWord.asWord()) }
}

internal fun WordEntity.asExternalModel(): Word =
    Word(
        id = id,
        lemma = lemma,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        meanings = meanings,
        exampleSentence = exampleSentence,
        exampleTranslation = exampleTranslation,
        synonyms = synonyms,
        antonyms = antonyms,
        similarWords = similarWords,
        confusingWords = confusingWords,
        wordForms = wordForms,
        root = root,
        tags = tags,
        frequencyRank = frequencyRank,
        pronunciationUrl = pronunciationUrl,
    )

internal fun Word.asEntity(): WordEntity =
    WordEntity(
        id = id,
        lemma = lemma,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        meanings = meanings,
        exampleSentence = exampleSentence,
        exampleTranslation = exampleTranslation,
        synonyms = synonyms,
        antonyms = antonyms,
        similarWords = similarWords,
        confusingWords = confusingWords,
        wordForms = wordForms,
        root = root,
        tags = tags,
        frequencyRank = frequencyRank,
        pronunciationUrl = pronunciationUrl,
    )

internal fun ImportedWord.asWord(): Word =
    Word(
        lemma = text,
        phonetic = phonetic,
        meanings = meanings,
        synonyms = synonyms,
        antonyms = antonyms,
        similarWords = similarWords,
        wordForms = wordForms,
        root = root,
        exampleSentence = exampleSentence,
        exampleTranslation = exampleTranslation,
    )

fun loadBuiltInWords(
    context: Context,
    assetName: String = "books/cet4.json",
): List<Word> =
    context.assets.open(assetName).use(JsonBookImporter()::parse).words.mapIndexed { index, importedWord ->
        importedWord.asWord().copy(id = index.toLong() + 1L)
    }

fun loadBuiltInWord(
    context: Context,
    wordId: Long,
    assetName: String = "books/cet4.json",
): Word? = loadBuiltInWords(context, assetName).firstOrNull { it.id == wordId }

fun findRelatedWords(
    target: Word,
    candidates: List<Word>,
): List<Word> {
    val relatedLemmas = (target.similarWords + target.confusingWords).map(String::lowercase).toSet()
    return candidates.filter { candidate ->
        candidate.id != target.id && relatedLemmas.contains(candidate.lemma.lowercase())
    }
}
