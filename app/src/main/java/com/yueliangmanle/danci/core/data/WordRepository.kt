package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WordRepository {
    fun observeWords(query: String = ""): Flow<List<Word>>
    suspend fun getWord(wordId: Long): Word?
    suspend fun insertWord(word: Word): Long
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
