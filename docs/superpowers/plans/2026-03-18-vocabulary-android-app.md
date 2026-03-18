# Vocabulary Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android-first vocabulary learning app with offline study/review loops, importable word books, structured word detail, local backup/restore, and a local-first AI coaching layer.

**Architecture:** Use a single `app` module in Kotlin + Jetpack Compose for the MVP, with feature packages inside the app module and clear internal boundaries between UI, domain logic, storage, backup, and AI strategy. Store canonical content and study state in Room, keep user preferences and reminder settings in DataStore, and route every study decision through a local task engine so AI can only suggest strategy changes instead of directly mutating the database.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Room, DataStore, WorkManager, Kotlinx Serialization, OkHttp/Ktor-style HTTP client, Android Keystore-backed encrypted storage, JUnit, Turbine, Robolectric/Compose UI tests.

---

## File Structure

### Project and Build Files

- `settings.gradle.kts`
  - Includes the `app` module and centralizes plugin repositories.
- `build.gradle.kts`
  - Root Gradle configuration and shared plugin versions.
- `gradle.properties`
  - JVM/AndroidX/Compose build flags.
- `gradle/libs.versions.toml`
  - Single source of truth for library versions.
- `app/build.gradle.kts`
  - Android application plugin, Compose, Room, WorkManager, serialization, and test dependencies.
- `app/proguard-rules.pro`
  - Release keep rules for serialization and Room.

### App Entry and Core Wiring

- `app/src/main/AndroidManifest.xml`
  - App declaration, permissions, backup opt-out, and activity/worker registration.
- `app/src/main/java/com/yueliangmanle/danci/DanciApp.kt`
  - `Application` class that initializes DI/service locators and background workers.
- `app/src/main/java/com/yueliangmanle/danci/MainActivity.kt`
  - Compose host activity.
- `app/src/main/java/com/yueliangmanle/danci/app/DanciAppState.kt`
  - App-level state holder for top-level navigation.
- `app/src/main/java/com/yueliangmanle/danci/app/DanciNavHost.kt`
  - Navigation graph.
- `app/src/main/java/com/yueliangmanle/danci/app/TopLevelDestination.kt`
  - Bottom-navigation destinations.

### Design System

- `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Color.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Type.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Theme.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/designsystem/component/DanciScaffold.kt`
  - Shared styling and shell components.

### Data Models and Storage

- `app/src/main/java/com/yueliangmanle/danci/core/model/Word.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/model/Book.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/model/LearningRecord.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/model/StudySession.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/model/StudyEvent.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/model/AiMemorySummary.kt`
  - App-facing domain models.
- `app/src/main/java/com/yueliangmanle/danci/core/database/DanciDatabase.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/WordEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/BookEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/BookWordEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/LearningRecordEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/StudySessionEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/StudyEventEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/DailySummaryEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/WeeklySummaryEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/LearnerProfileEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/PlanHistoryEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/entity/ConfusionEdgeEntity.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/database/dao/*.kt`
  - Storage schema and data access.

### Repositories and Use Cases

- `app/src/main/java/com/yueliangmanle/danci/core/data/WordRepository.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/data/BookRepository.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/data/StudyRepository.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/data/SettingsRepository.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/data/AiMemoryRepository.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/data/BackupRepository.kt`
  - Boundary between storage and features.

### Import, Backup, and Settings

- `app/src/main/java/com/yueliangmanle/danci/core/importer/BookImportParser.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/importer/CsvBookImporter.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/importer/JsonBookImporter.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupManifest.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupExporter.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupImporter.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/security/AiCredentialStore.kt`
  - External content flow and secure settings.

### Study Engine and AI Strategy

- `app/src/main/java/com/yueliangmanle/danci/core/study/TodayTaskEngine.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/study/ReviewScheduler.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/study/StudyQueueBuilder.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/study/QuizGenerator.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/study/FeedbackMapper.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/analytics/StudyAnalyticsAggregator.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/analytics/SummaryBuilder.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/ai/AiClient.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/ai/AiPromptFactory.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/ai/AiContextBuilder.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/ai/AiSuggestionParser.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/ai/AiStrategyCoordinator.kt`
  - Runtime decision-making and AI orchestration.

### Features

- `app/src/main/java/com/yueliangmanle/danci/feature/home/*`
  - Today dashboard and “analyze plan” entry point.
- `app/src/main/java/com/yueliangmanle/danci/feature/study/*`
  - Card study flow and session coordination.
- `app/src/main/java/com/yueliangmanle/danci/feature/quiz/*`
  - Multiple-choice review flow.
- `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/*`
  - Full word detail, relation tabs, and AI actions.
- `app/src/main/java/com/yueliangmanle/danci/feature/books/*`
  - Built-in/imported/custom book management.
- `app/src/main/java/com/yueliangmanle/danci/feature/me/*`
  - Settings, reminders, backup/restore, AI config, and stats.

### Background Work

- `app/src/main/java/com/yueliangmanle/danci/core/worker/DailyReminderWorker.kt`
- `app/src/main/java/com/yueliangmanle/danci/core/worker/AiSummaryRefreshWorker.kt`
  - Reminder scheduling and deferred summary updates.

### Assets

- `app/src/main/assets/books/*.json`
  - Built-in word books.
- `app/src/main/assets/books/manifest.json`
  - Built-in catalog metadata.

### Tests

- `app/src/test/java/com/yueliangmanle/danci/core/...`
  - Unit tests for scheduling, repositories, parsing, analytics, backup, and AI context shaping.
- `app/src/test/java/com/yueliangmanle/danci/feature/...`
  - ViewModel and state tests.
- `app/src/androidTest/java/com/yueliangmanle/danci/...`
  - Compose UI tests for top-level flows.

## Task 1: Scaffold The Android Project And App Shell

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/yueliangmanle/danci/DanciApp.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/MainActivity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/app/DanciAppState.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/app/DanciNavHost.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/app/TopLevelDestination.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Color.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Type.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/designsystem/theme/Theme.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/designsystem/component/DanciScaffold.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/AppShellTest.kt`

- [ ] **Step 1: Write the failing Compose shell test**

```kotlin
@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsOnHomeAndShowsFourTabs() {
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("学习").assertIsDisplayed()
        composeRule.onNodeWithText("词书").assertIsDisplayed()
        composeRule.onNodeWithText("我的").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the instrumentation test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.AppShellTest`

Expected: FAIL because the Android project, `MainActivity`, and app shell do not exist yet.

- [ ] **Step 3: Create the Gradle project and Compose app shell**

```kotlin
enum class TopLevelDestination(val label: String) {
    HOME("首页"),
    STUDY("学习"),
    BOOKS("词书"),
    ME("我的"),
}

@Composable
fun DanciApp() {
    val destinations = TopLevelDestination.entries
    DanciScaffold(destinations = destinations) {
        DanciNavHost(startDestination = TopLevelDestination.HOME)
    }
}
```

- [ ] **Step 4: Re-run the instrumentation test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.AppShellTest`

Expected: PASS with the four top-level tabs rendered in the root scaffold.

- [ ] **Step 5: Verify the project assembles**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL and a debug APK under `app/build/outputs/apk/debug/`.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml app
git commit -m "feat: scaffold android app shell"
```

## Task 2: Define Domain Models, Room Schema, And Repositories

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/Word.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/Book.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/LearningRecord.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/StudySession.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/StudyEvent.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/model/AiMemorySummary.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/DanciDatabase.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/WordEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/BookEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/BookWordEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/LearningRecordEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/StudySessionEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/StudyEventEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/DailySummaryEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/WeeklySummaryEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/LearnerProfileEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/PlanHistoryEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/entity/ConfusionEdgeEntity.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/dao/WordDao.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/dao/BookDao.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/database/dao/StudyDao.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/WordRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/BookRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/StudyRepository.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/database/DanciDatabaseTest.kt`

- [ ] **Step 1: Write the failing Room relationship test**

```kotlin
class DanciDatabaseTest {
    @Test
    fun wordAppearingInTwoBooksSharesOneLearningRecord() = runTest {
        val db = buildTestDatabase()
        val wordId = db.wordDao().insertWord(sampleWordEntity())
        db.bookDao().insertBook(sampleBookEntity(id = "cet4"))
        db.bookDao().insertBook(sampleBookEntity(id = "kaoyan"))
        db.bookDao().insertBookWordCrossRef(BookWordEntity("cet4", wordId))
        db.bookDao().insertBookWordCrossRef(BookWordEntity("kaoyan", wordId))
        db.studyDao().upsertLearningRecord(LearningRecordEntity(wordId = wordId, mastery = 0.4f))

        val records = db.studyDao().getLearningRecordsForWord(wordId)
        assertThat(records).hasSize(1)
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.database.DanciDatabaseTest`

Expected: FAIL because the Room schema, DAOs, and repositories do not exist.

- [ ] **Step 3: Implement canonical entities, DAOs, and repository mappers**

```kotlin
@Entity(tableName = "book_words", primaryKeys = ["bookId", "wordId"])
data class BookWordEntity(
    val bookId: String,
    val wordId: String,
    val chapter: String? = null,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList(),
    val note: String? = null,
)

@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey val wordId: String,
    val mastery: Float,
    val familiarityState: String,
    val nextReviewAt: Instant?,
)
```

- [ ] **Step 4: Re-run the unit test to verify the schema behavior passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.database.DanciDatabaseTest`

Expected: PASS with one shared `LearningRecordEntity` for the canonical word.

- [ ] **Step 5: Smoke-test all local unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL with database tests green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/model app/src/main/java/com/yueliangmanle/danci/core/database app/src/main/java/com/yueliangmanle/danci/core/data app/src/test/java/com/yueliangmanle/danci/core/database
git commit -m "feat: add core models and room schema"
```

## Task 3: Add Built-In Catalogs And Importable Word Books

**Files:**
- Create: `app/src/main/assets/books/manifest.json`
- Create: `app/src/main/assets/books/cet4.json`
- Create: `app/src/main/assets/books/cet6.json`
- Create: `app/src/main/assets/books/kaoyan.json`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/importer/BookImportParser.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/importer/CsvBookImporter.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/importer/JsonBookImporter.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/BookRepository.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/WordRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/books/BooksViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/books/BooksRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/books/BooksScreen.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/importer/CsvBookImporterTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/books/BooksScreenTest.kt`

- [ ] **Step 1: Write the failing importer test**

```kotlin
class CsvBookImporterTest {
    @Test
    fun parsesRelationsAndWordFormsFromCsv() {
        val csv = """
            word,phonetic,meaning,synonyms,antonyms,similar_words,word_forms
            abandon,/əˈbændən/,"放弃","give up|quit","continue","abundant|absorb","abandoned|abandoning|abandonment"
        """.trimIndent()

        val result = CsvBookImporter().parse(csv.byteInputStream())

        assertThat(result.words.single().synonyms).containsExactly("give up", "quit")
        assertThat(result.words.single().similarWords).contains("abundant")
        assertThat(result.words.single().wordForms).contains("abandonment")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.importer.CsvBookImporterTest`

Expected: FAIL because the import parser and CSV normalization rules do not exist.

- [ ] **Step 3: Implement built-in manifest loading, CSV/JSON import, and books UI**

```kotlin
data class ImportedWord(
    val text: String,
    val meanings: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val similarWords: List<String>,
    val wordForms: List<String>,
)

fun String.splitPipedValues(): List<String> =
    split("|").map(String::trim).filter(String::isNotEmpty)
```

- [ ] **Step 4: Add a failing Compose test for the books list**

```kotlin
@Test
fun booksScreenShowsBuiltInAndImportedSections() {
    composeRule.onNodeWithText("内置词书").assertIsDisplayed()
    composeRule.onNodeWithText("导入词书").assertIsDisplayed()
}
```

- [ ] **Step 5: Run the UI test to verify it fails, then implement the screen**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.books.BooksScreenTest`

Expected: FAIL first, then PASS once `BooksScreen` renders built-in and imported sections from the repository.

- [ ] **Step 6: Run targeted verification**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.importer.CsvBookImporterTest`

Expected: PASS with normalized relation and word-form fields.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets/books app/src/main/java/com/yueliangmanle/danci/core/importer app/src/main/java/com/yueliangmanle/danci/feature/books app/src/test/java/com/yueliangmanle/danci/core/importer app/src/androidTest/java/com/yueliangmanle/danci/feature/books
git commit -m "feat: add word book catalog and import flow"
```

## Task 4: Build The Today Task Engine And Home Dashboard

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/study/TodayTaskEngine.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/study/ReviewScheduler.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/home/HomeRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/home/HomeScreen.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/study/TodayTaskEngineTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the failing task-engine test**

```kotlin
class TodayTaskEngineTest {
    @Test
    fun splitsTodayQueueIntoNewReviewAndMistakeBuckets() {
        val plan = TodayTaskEngine().build(
            dailyGoal = 20,
            overdueWords = 12,
            unseenWords = 50,
            recentMistakeWords = 4,
        )

        assertThat(plan.newWordCount).isEqualTo(8)
        assertThat(plan.reviewCount).isEqualTo(12)
        assertThat(plan.mistakeCount).isEqualTo(4)
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.TodayTaskEngineTest`

Expected: FAIL because the planning engine and Home feature do not exist.

- [ ] **Step 3: Implement the task engine and Home ViewModel**

```kotlin
data class TodayPlan(
    val newWordCount: Int,
    val reviewCount: Int,
    val mistakeCount: Int,
    val estimatedMinutes: Int,
)

fun build(dailyGoal: Int, overdueWords: Int, unseenWords: Int, recentMistakeWords: Int): TodayPlan {
    val review = min(overdueWords, dailyGoal)
    val remaining = (dailyGoal - review).coerceAtLeast(0)
    val newWords = min(unseenWords, remaining)
    return TodayPlan(
        newWordCount = newWords,
        reviewCount = review,
        mistakeCount = recentMistakeWords,
        estimatedMinutes = ((review + newWords + recentMistakeWords) * 0.7f).roundToInt(),
    )
}
```

- [ ] **Step 4: Add the failing Home screen test**

```kotlin
@Test
fun homeScreenShowsTodayStatsAndPrimaryActions() {
    composeRule.onNodeWithText("今天还要学 20 个词").assertIsDisplayed()
    composeRule.onNodeWithText("开始新词学习").assertIsDisplayed()
    composeRule.onNodeWithText("开始复习").assertIsDisplayed()
}
```

- [ ] **Step 5: Run the UI test to verify it fails, then implement `HomeScreen`**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.home.HomeScreenTest`

Expected: FAIL first, then PASS with today counts and quick actions visible.

- [ ] **Step 6: Run focused verification**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.TodayTaskEngineTest`

Expected: PASS with stable task partitioning logic.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/study app/src/main/java/com/yueliangmanle/danci/core/data/SettingsRepository.kt app/src/main/java/com/yueliangmanle/danci/feature/home app/src/test/java/com/yueliangmanle/danci/core/study app/src/androidTest/java/com/yueliangmanle/danci/feature/home
git commit -m "feat: add home dashboard and task engine"
```

## Task 5: Implement Card Study Sessions And Feedback Mapping

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/study/StudyQueueBuilder.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/study/FeedbackMapper.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/study/StudyViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/study/StudyRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/study/StudyScreen.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/StudyRepository.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/study/FeedbackMapperTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/study/StudyScreenTest.kt`

- [ ] **Step 1: Write the failing feedback-mapping test**

```kotlin
class FeedbackMapperTest {
    @Test
    fun notKnownFeedbackSchedulesSameDayRetry() {
        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(mastery = 0.3f),
            feedback = CardFeedback.NOT_KNOWN,
            answeredAt = Instant.parse("2026-03-18T09:00:00Z"),
        )

        assertThat(updated.nextReviewAt).isEqualTo(Instant.parse("2026-03-18T09:20:00Z"))
        assertThat(updated.familiarityState).isEqualTo("生疏")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.FeedbackMapperTest`

Expected: FAIL because the feedback mapper and session flow do not exist.

- [ ] **Step 3: Implement the local feedback mapper and study queue**

```kotlin
fun applyCardFeedback(current: LearningRecord, feedback: CardFeedback, answeredAt: Instant): LearningRecord {
    return when (feedback) {
        CardFeedback.NOT_KNOWN -> current.copy(
            mastery = (current.mastery - 0.2f).coerceAtLeast(0f),
            familiarityState = "生疏",
            nextReviewAt = answeredAt.plus(20, ChronoUnit.MINUTES),
        )
        CardFeedback.FUZZY -> current.copy(nextReviewAt = answeredAt.plus(1, ChronoUnit.DAYS))
        CardFeedback.KNOWN -> current.copy(nextReviewAt = answeredAt.plus(2, ChronoUnit.DAYS))
    }
}
```

- [ ] **Step 4: Add the failing Compose test for card actions**

```kotlin
@Test
fun studyScreenShowsWordAndThreeFeedbackButtons() {
    composeRule.onNodeWithText("不认识").assertIsDisplayed()
    composeRule.onNodeWithText("模糊").assertIsDisplayed()
    composeRule.onNodeWithText("认识").assertIsDisplayed()
}
```

- [ ] **Step 5: Run the UI test to verify it fails, then implement `StudyScreen`**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.study.StudyScreenTest`

Expected: FAIL first, then PASS with the card session UI and feedback buttons wired to the ViewModel.

- [ ] **Step 6: Verify unit coverage**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.FeedbackMapperTest`

Expected: PASS with deterministic interval mapping.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/study/StudyQueueBuilder.kt app/src/main/java/com/yueliangmanle/danci/core/study/FeedbackMapper.kt app/src/main/java/com/yueliangmanle/danci/feature/study app/src/test/java/com/yueliangmanle/danci/core/study app/src/androidTest/java/com/yueliangmanle/danci/feature/study
git commit -m "feat: add card study session flow"
```

## Task 6: Implement Quiz Review And Full Word Detail

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/study/QuizGenerator.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/quiz/QuizViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/quiz/QuizRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/quiz/QuizScreen.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailScreen.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/WordRepository.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/StudyRepository.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/study/QuizGeneratorTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/worddetail/WordDetailScreenTest.kt`

- [ ] **Step 1: Write the failing quiz-priority test**

```kotlin
class QuizGeneratorTest {
    @Test
    fun prefersConfusionPairsOverGenericDistractors() {
        val question = QuizGenerator().createQuestion(
            target = preciseWord(),
            confusionWords = listOf(preciseVsAccurate(), preciseVsExact()),
            fallbackWords = listOf(randomDistractor()),
        )

        assertThat(question.options).contains("精确的")
        assertThat(question.options).contains("准确的")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.QuizGeneratorTest`

Expected: FAIL because quiz generation and confusion-priority logic do not exist.

- [ ] **Step 3: Implement quiz generation and Word detail state**

```kotlin
fun createQuestion(target: Word, confusionWords: List<Word>, fallbackWords: List<Word>): QuizQuestion {
    val distractors = (confusionWords + fallbackWords)
        .filterNot { it.id == target.id }
        .take(3)
        .map { it.primaryMeaning }
    return QuizQuestion(
        prompt = target.text,
        correctAnswer = target.primaryMeaning,
        options = (distractors + target.primaryMeaning).shuffled(),
    )
}
```

- [ ] **Step 4: Add the failing Word detail Compose test**

```kotlin
@Test
fun wordDetailShowsRelationsFormsAndAiActions() {
    composeRule.onNodeWithText("近义词").assertIsDisplayed()
    composeRule.onNodeWithText("反义词").assertIsDisplayed()
    composeRule.onNodeWithText("拼写相近词").assertIsDisplayed()
    composeRule.onNodeWithText("单词变形").assertIsDisplayed()
    composeRule.onNodeWithText("AI 助记").assertIsDisplayed()
}
```

- [ ] **Step 5: Run the UI test to verify it fails, then implement detail tabs**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.worddetail.WordDetailScreenTest`

Expected: FAIL first, then PASS with relation, form, and AI action sections visible.

- [ ] **Step 6: Run focused verification**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.study.QuizGeneratorTest`

Expected: PASS with confusion-heavy distractor selection.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/study/QuizGenerator.kt app/src/main/java/com/yueliangmanle/danci/feature/quiz app/src/main/java/com/yueliangmanle/danci/feature/worddetail app/src/test/java/com/yueliangmanle/danci/core/study/QuizGeneratorTest.kt app/src/androidTest/java/com/yueliangmanle/danci/feature/worddetail
git commit -m "feat: add quiz review and word detail"
```

## Task 7: Record Fine-Grained Study Events And Build AI Memory Summaries

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/analytics/StudyAnalyticsAggregator.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/analytics/SummaryBuilder.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/AiMemoryRepository.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/StudyRepository.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/study/StudyViewModel.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/quiz/QuizViewModel.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailViewModel.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/analytics/StudyAnalyticsAggregatorTest.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/analytics/SummaryBuilderTest.kt`

- [ ] **Step 1: Write the failing analytics aggregation test**

```kotlin
class StudyAnalyticsAggregatorTest {
    @Test
    fun buildsConfusionGraphFromRepeatedMistakes() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                studyEvent(wordId = "precise", confusedWith = "accurate", correct = false),
                studyEvent(wordId = "precise", confusedWith = "accurate", correct = false),
            )
        )

        assertThat(result.confusionEdges.single().weight).isEqualTo(2)
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.analytics.StudyAnalyticsAggregatorTest`

Expected: FAIL because event aggregation and AI memory summary builders do not exist.

- [ ] **Step 3: Implement event tracking, confusion graph aggregation, and daily summaries**

```kotlin
data class AggregatedAnalytics(
    val dailySummary: DailySummary,
    val learnerProfile: LearnerProfile,
    val confusionEdges: List<ConfusionEdge>,
)

fun aggregate(events: List<StudyEvent>): AggregatedAnalytics {
    val confusionEdges = events
        .filter { !it.correct && it.confusedWithWordId != null }
        .groupBy { it.wordId to it.confusedWithWordId }
        .map { (pair, grouped) -> ConfusionEdge(pair.first, pair.second!!, grouped.size) }
    return AggregatedAnalytics(
        dailySummary = DailySummary(totalEvents = events.size, incorrectAnswers = events.count { !it.correct }),
        learnerProfile = LearnerProfile(
            weakWordIds = confusionEdges.map { it.fromWordId }.distinct(),
            preferredModes = listOf("card", "quiz"),
        ),
        confusionEdges = confusionEdges,
    )
}
```

- [ ] **Step 4: Write the failing summary payload test**

```kotlin
class SummaryBuilderTest {
    @Test
    fun buildsSevenDayAndThirtyDaySlicesWithoutRawLogFlooding() {
        val payload = SummaryBuilder().buildContext(
            sevenDay = sampleDailySummaries(count = 7),
            thirtyDay = sampleWeeklySummaries(count = 4),
            rawEvents = sampleEvents(count = 500),
        )

        assertThat(payload.rawSamples.size).isAtMost(20)
        assertThat(payload.dailyTrend).isNotEmpty()
    }
}
```

- [ ] **Step 5: Run the unit test to verify it fails, then implement payload compression**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.analytics.SummaryBuilderTest`

Expected: FAIL first, then PASS with bounded raw samples and retained trend summaries.

- [ ] **Step 6: Verify focused analytics tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.analytics.StudyAnalyticsAggregatorTest --tests com.yueliangmanle.danci.core.analytics.SummaryBuilderTest`

Expected: PASS with deterministic summaries and capped raw sample counts.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/analytics app/src/main/java/com/yueliangmanle/danci/core/data/AiMemoryRepository.kt app/src/main/java/com/yueliangmanle/danci/feature/study app/src/main/java/com/yueliangmanle/danci/feature/quiz app/src/main/java/com/yueliangmanle/danci/feature/worddetail app/src/test/java/com/yueliangmanle/danci/core/analytics
git commit -m "feat: add study analytics and ai memory summaries"
```

## Task 8: Add AI Settings, Client, And Strategy Coordination

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/security/AiCredentialStore.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiClient.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiPromptFactory.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiContextBuilder.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiSuggestionParser.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiStrategyCoordinator.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/AiSettingsViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/AiSettingsRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/AiSettingsScreen.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/home/HomeViewModel.kt`
- Modify: `app/src/androidTest/java/com/yueliangmanle/danci/feature/home/HomeScreenTest.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/ai/AiContextBuilderTest.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/ai/AiStrategyCoordinatorTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/me/AiSettingsScreenTest.kt`

- [ ] **Step 1: Write the failing AI context test**

```kotlin
class AiContextBuilderTest {
    @Test
    fun contextIncludesSummariesButExcludesApiKey() {
        val payload = AiContextBuilder().buildPlanAdjustmentContext(
            settings = aiSettings(baseUrl = "https://api.example.com", apiKey = "sk-secret", model = "gpt-5.4-mini"),
            memory = sampleAiMemory(),
        )

        assertThat(payload).contains("learner_profile")
        assertThat(payload).doesNotContain("sk-secret")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.ai.AiContextBuilderTest`

Expected: FAIL because AI context shaping and credential storage do not exist.

- [ ] **Step 3: Implement secure AI settings, prompt factories, and HTTP client**

```kotlin
interface AiCredentialStore {
    suspend fun saveApiKey(key: String)
    suspend fun readApiKey(): String?
}

data class PlanAdjustmentPayload(
    val learnerProfile: LearnerProfile,
    val sevenDaySummary: List<DailySummary>,
    val thirtyDaySummary: List<WeeklySummary>,
    val rawSamples: List<StudyEventSample>,
)
```

- [ ] **Step 4: Write the failing strategy-coordinator test**

```kotlin
class AiStrategyCoordinatorTest {
    @Test
    fun ignoresInvalidSuggestionAndKeepsLocalPlan() = runTest {
        val coordinator = AiStrategyCoordinator(client = fakeAiClient(response = "{bad json"))

        val result = coordinator.adjustPlan(sampleCurrentPlan())

        assertThat(result.source).isEqualTo(PlanSource.LOCAL_FALLBACK)
    }
}
```

- [ ] **Step 5: Run the unit test to verify it fails, then implement parser and fallback rules**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.ai.AiStrategyCoordinatorTest`

Expected: FAIL first, then PASS with invalid AI output safely ignored.

- [ ] **Step 6: Add AI settings UI and Home “Analyze Plan” trigger**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.home.HomeScreenTest --tests com.yueliangmanle.danci.feature.me.AiSettingsScreenTest`

Expected: PASS with `HomeScreenTest` covering the manual “分析并调整计划” action and `AiSettingsScreenTest` covering Base URL, Model, and API key entry.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/security app/src/main/java/com/yueliangmanle/danci/core/ai app/src/main/java/com/yueliangmanle/danci/core/data/SettingsRepository.kt app/src/main/java/com/yueliangmanle/danci/feature/me app/src/main/java/com/yueliangmanle/danci/feature/home app/src/test/java/com/yueliangmanle/danci/core/ai app/src/androidTest/java/com/yueliangmanle/danci/feature/home/HomeScreenTest.kt app/src/androidTest/java/com/yueliangmanle/danci/feature/me/AiSettingsScreenTest.kt
git commit -m "feat: add ai settings and strategy coordinator"
```

## Task 9: Integrate AI Into Word Help, Error Review, And Session Checkpoints

**Files:**
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailViewModel.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/worddetail/WordDetailScreen.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/study/StudyViewModel.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/quiz/QuizViewModel.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiPromptFactory.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/core/ai/AiStrategyCoordinator.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/ai/AiPromptFactoryTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/worddetail/WordDetailAiActionsTest.kt`

- [ ] **Step 1: Write the failing AI prompt test**

```kotlin
class AiPromptFactoryTest {
    @Test
    fun buildsRelationAwarePromptForWordHelp() {
        val prompt = AiPromptFactory().wordHelpPrompt(
            word = abandonWord(),
            request = AiWordHelpRequest.RELATION_DIFFERENCE,
        )

        assertThat(prompt).contains("近义词")
        assertThat(prompt).contains("反义词")
        assertThat(prompt).contains("拼写相近词")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.ai.AiPromptFactoryTest`

Expected: FAIL because word-help prompt generation is not specialized yet.

- [ ] **Step 3: Implement AI actions for mnemonic help, relation analysis, and mistake explanation**

```kotlin
sealed interface AiWordHelpRequest {
    data object MNEMONIC : AiWordHelpRequest
    data object RELATION_DIFFERENCE : AiWordHelpRequest
    data object WORD_FORM_EXPLANATION : AiWordHelpRequest
    data object EXAMPLE_EXPANSION : AiWordHelpRequest
}
```

- [ ] **Step 4: Add the failing Word detail AI action test**

```kotlin
@Test
fun wordDetailAiActionsShowResultCards() {
    composeRule.onNodeWithText("AI 助记").performClick()
    composeRule.onNodeWithText("记忆提示").assertIsDisplayed()
}
```

- [ ] **Step 5: Run the UI test to verify it fails, then implement the result panels**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.worddetail.WordDetailAiActionsTest`

Expected: FAIL first, then PASS with AI result cards rendered in the detail flow.

- [ ] **Step 6: Add session checkpoint triggers**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.ai.AiPromptFactoryTest`

Expected: PASS after wiring session checkpoints to request plan adjustments every 15 completed words and on abnormal error spikes.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/feature/worddetail app/src/main/java/com/yueliangmanle/danci/feature/study app/src/main/java/com/yueliangmanle/danci/feature/quiz app/src/main/java/com/yueliangmanle/danci/core/ai app/src/test/java/com/yueliangmanle/danci/core/ai/AiPromptFactoryTest.kt app/src/androidTest/java/com/yueliangmanle/danci/feature/worddetail/WordDetailAiActionsTest.kt
git commit -m "feat: integrate ai help and session checkpoints"
```

## Task 10: Add Backup, Restore, Reminders, And Settings Screens

**Files:**
- Create: `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupManifest.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupExporter.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/backup/BackupImporter.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/data/BackupRepository.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/worker/DailyReminderWorker.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/core/worker/AiSummaryRefreshWorker.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/DanciApp.kt`
- Modify: `app/src/main/java/com/yueliangmanle/danci/feature/me/AiSettingsViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/MeViewModel.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/MeRoute.kt`
- Create: `app/src/main/java/com/yueliangmanle/danci/feature/me/MeScreen.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/backup/BackupExporterTest.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/backup/BackupImporterTest.kt`
- Test: `app/src/test/java/com/yueliangmanle/danci/core/worker/DailyReminderSchedulerTest.kt`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/feature/me/MeScreenTest.kt`

- [ ] **Step 1: Write the failing backup exporter test**

```kotlin
class BackupExporterTest {
    @Test
    fun backupIncludesAiMemoryButExcludesApiKey() = runTest {
        val backup = BackupExporter(
            studyRepository = fakeStudyRepository(),
            aiMemoryRepository = fakeAiMemoryRepository(),
            settingsRepository = fakeSettingsRepository(),
        ).export()

        assertThat(backup.manifest.sections).contains("learner_profile")
        assertThat(backup.serializedJson).doesNotContain("sk-secret")
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.backup.BackupExporterTest`

Expected: FAIL because export/import models and manifest validation do not exist.

- [ ] **Step 3: Implement backup manifest, zip export, and versioned restore validation**

```kotlin
@Serializable
data class BackupManifest(
    val version: Int,
    val createdAt: String,
    val sections: List<String>,
)
```

- [ ] **Step 4: Write the failing reminder scheduling test**

```kotlin
class DailyReminderSchedulerTest {
    @Test
    fun schedulesReminderAtUserConfiguredTime() {
        val request = buildReminderWorkRequest(hour = 21, minute = 30)
        assertThat(request.initialDelay).isGreaterThan(Duration.ZERO)
    }
}
```

- [ ] **Step 5: Run the unit test to verify it fails, then implement worker scheduling**

Run: `./gradlew :app:testDebugUnitTest --tests com.yueliangmanle.danci.core.worker.DailyReminderSchedulerTest`

Expected: FAIL first, then PASS with reminder requests generated from settings.

- [ ] **Step 6: Add the settings screen for reminders, backup/restore, and AI config**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.feature.me.MeScreenTest`

Expected: PASS with “我的” exposing backup/restore, reminder configuration, and AI settings entry points.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yueliangmanle/danci/core/backup app/src/main/java/com/yueliangmanle/danci/core/data/BackupRepository.kt app/src/main/java/com/yueliangmanle/danci/core/worker app/src/main/java/com/yueliangmanle/danci/feature/me app/src/test/java/com/yueliangmanle/danci/core/backup app/src/test/java/com/yueliangmanle/danci/core/worker app/src/androidTest/java/com/yueliangmanle/danci/feature/me/MeScreenTest.kt
git commit -m "feat: add backup restore and reminder settings"
```

## Task 11: End-To-End Verification, Sample Data Checks, And Developer Docs

**Files:**
- Create: `README.md`
- Create: `docs/development.md`
- Create: `app/src/androidTest/java/com/yueliangmanle/danci/SmokeJourneyTest.kt`
- Modify: `app/src/main/assets/books/manifest.json`
- Test: `app/src/androidTest/java/com/yueliangmanle/danci/SmokeJourneyTest.kt`

- [ ] **Step 1: Write the failing smoke-journey test**

```kotlin
@RunWith(AndroidJUnit4::class)
class SmokeJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun userCanGoFromHomeToStudyToWordDetailToMe() {
        composeRule.onNodeWithText("开始新词学习").performClick()
        composeRule.onNodeWithText("不认识").assertIsDisplayed()
        composeRule.onNodeWithText("单词详情").performClick()
        composeRule.onNodeWithText("近义词").assertIsDisplayed()
        composeRule.onNodeWithText("我的").performClick()
        composeRule.onNodeWithText("备份与恢复").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the smoke test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.yueliangmanle.danci.SmokeJourneyTest`

Expected: FAIL until all top-level flows are connected end-to-end.

- [ ] **Step 3: Fix any integration gaps and document the developer setup**

```md
# Development

1. Install Android Studio and an SDK matching `compileSdk`.
2. Run `./gradlew :app:assembleDebug`.
3. Run `./gradlew :app:testDebugUnitTest`.
4. Run `./gradlew :app:connectedDebugAndroidTest`.
```

- [ ] **Step 4: Run the full verification suite**

Run: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL with unit tests, UI tests, and APK assembly all green.

- [ ] **Step 5: Commit**

```bash
git add README.md docs/development.md app/src/androidTest/java/com/yueliangmanle/danci/SmokeJourneyTest.kt app/src/main/assets/books/manifest.json
git commit -m "docs: add verification and developer setup"
```

## Notes For Execution

- Keep the package name `com.yueliangmanle.danci` unless the user explicitly requests a different application ID before implementation starts.
- Prefer constructor injection or a minimal service locator; do not introduce a heavyweight DI framework unless the codebase is already struggling without one.
- Keep the MVP in a single `app` module; do not split into multiple Gradle modules until the feature packages are proven unwieldy.
- AI requests must be opt-in, bounded in payload size, and safe to ignore when the network, key, or model fails.
- Backup files must include AI summaries and plan history, but never the stored API key.
- Every feature task above should leave the app runnable and verifiable on its own.
