package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.importer.JsonBookImporter
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_IMPORTED
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_COMPLETE
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_EMPTY
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_PARTIAL
import com.yueliangmanle.danci.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WordRepository {
    fun observeWords(query: String = ""): Flow<List<Word>>
    suspend fun getWord(wordId: Long): Word?
    suspend fun getWords(wordIds: List<Long>): List<Word>
    suspend fun getAllWords(): List<Word>
    suspend fun insertWord(word: Word): Long
    suspend fun updateWord(word: Word)
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

    override suspend fun getWords(wordIds: List<Long>): List<Word> =
        if (wordIds.isEmpty()) {
            emptyList()
        } else {
            wordDao.getWordsByIds(wordIds).map(WordEntity::asExternalModel)
        }

    override suspend fun getAllWords(): List<Word> = wordDao.getAllWords().map(WordEntity::asExternalModel)

    override suspend fun insertWord(word: Word): Long {
        val insertedId = wordDao.insertWord(word.asEntity())
        if (insertedId != -1L) {
            return insertedId
        }
        return requireNotNull(wordDao.getWordByLemma(word.lemma)) {
            "Expected canonical word to exist for lemma=${word.lemma}"
        }.id
    }

    override suspend fun updateWord(word: Word) {
        wordDao.updateWord(word.asEntity())
    }

    override suspend fun importWords(words: List<ImportedWord>): List<Long> {
        if (words.isEmpty()) {
            return emptyList()
        }

        val normalizedWords = words.map(ImportedWord::asWord)
        val insertedIds = wordDao.insertWords(normalizedWords.map(Word::asEntity))
        val idsByLemma = linkedMapOf<String, Long>()
        val missingLemmas = linkedSetOf<String>()

        normalizedWords.forEachIndexed { index, word ->
            val insertedId = insertedIds[index]
            if (insertedId != -1L) {
                idsByLemma[word.lemma] = insertedId
            } else if (word.lemma !in idsByLemma) {
                missingLemmas += word.lemma
            }
        }

        if (missingLemmas.isNotEmpty()) {
            wordDao.getWordsByLemmas(missingLemmas.toList()).forEach { entity ->
                idsByLemma[entity.lemma] = entity.id
            }
        }

        return normalizedWords.map { word ->
            requireNotNull(idsByLemma[word.lemma]) {
                "Expected canonical word to exist for lemma=${word.lemma}"
            }
        }
    }
}

internal fun WordEntity.asExternalModel(): Word =
    Word(
        id = id,
        lemma = lemma,
        phonetic = phonetic,
        phoneticUk = phoneticUk,
        phoneticUs = phoneticUs,
        phoneticSource = phoneticSource,
        phoneticStatus = phoneticStatus,
        phoneticUpdatedAt = phoneticUpdatedAt,
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
        phoneticUk = phoneticUk,
        phoneticUs = phoneticUs,
        phoneticSource = phoneticSource,
        phoneticStatus = phoneticStatus,
        phoneticUpdatedAt = phoneticUpdatedAt,
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
        phonetic = phonetic ?: phoneticUk ?: phoneticUs,
        phoneticUk = phoneticUk,
        phoneticUs = phoneticUs,
        phoneticSource = PHONETIC_SOURCE_IMPORTED,
        phoneticStatus = when {
            !phoneticUk.isNullOrBlank() && !phoneticUs.isNullOrBlank() -> PHONETIC_STATUS_COMPLETE
            !phonetic.isNullOrBlank() || !phoneticUk.isNullOrBlank() || !phoneticUs.isNullOrBlank() -> PHONETIC_STATUS_PARTIAL
            else -> PHONETIC_STATUS_EMPTY
        },
        meanings = meanings,
        synonyms = synonyms,
        antonyms = antonyms,
        similarWords = similarWords,
        confusingWords = confusingWords,
        wordForms = wordForms,
        root = root,
        exampleSentence = exampleSentence,
        exampleTranslation = exampleTranslation,
    )

fun buildWordRepository(context: Context): WordRepository =
    RoomWordRepository(buildDanciDatabase(context.applicationContext).wordDao())

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
