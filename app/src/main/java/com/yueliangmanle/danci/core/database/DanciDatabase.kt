package com.yueliangmanle.danci.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.yueliangmanle.danci.core.database.dao.BookDao
import com.yueliangmanle.danci.core.database.dao.StudyDao
import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.ConfusionEdgeEntity
import com.yueliangmanle.danci.core.database.entity.DailySummaryEntity
import com.yueliangmanle.danci.core.database.entity.LearnerProfileEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PlanHistoryEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.WeeklySummaryEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import java.time.Instant

@Database(
    entities = [
        WordEntity::class,
        BookEntity::class,
        BookWordEntity::class,
        LearningRecordEntity::class,
        StudySessionEntity::class,
        StudyEventEntity::class,
        DailySummaryEntity::class,
        WeeklySummaryEntity::class,
        LearnerProfileEntity::class,
        PlanHistoryEntity::class,
        ConfusionEdgeEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DanciTypeConverters::class)
abstract class DanciDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun bookDao(): BookDao
    abstract fun studyDao(): StudyDao
}

private const val DANCI_DB_NAME = "danci.db"

private object DanciDatabaseHolder {
    @Volatile
    var instance: DanciDatabase? = null
}

fun buildDanciDatabase(context: Context): DanciDatabase {
    DanciDatabaseHolder.instance?.let { return it }
    return synchronized(DanciDatabaseHolder) {
        DanciDatabaseHolder.instance ?: Room.databaseBuilder(
            context.applicationContext,
            DanciDatabase::class.java,
            DANCI_DB_NAME,
        ).build().also { database ->
            DanciDatabaseHolder.instance = database
        }
    }
}

class DanciTypeConverters {
    private val separator = '\u001F'

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value.orEmpty().joinToString(separator.toString())

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value
            ?.takeIf { it.isNotBlank() }
            ?.split(separator)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
