# MeetNote KMP Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Kotlin Multiplatform project foundation and the first Android vertical slice for MeetNote: session creation, recording-first capture, local persistence, and post-meeting processing orchestration hooks.

**Architecture:** Use KMP for shared domain, data, storage, export, and provider contracts, while keeping Android-only concerns such as audio capture, background execution, permissions, device profiling, and LiteRT-LM integration in Android-specific modules. Deliver the first milestone as a record-then-process Android flow because it is the safest path for validating real-device capture before adding live inference complexity.

**Tech Stack:** Kotlin Multiplatform, Kotlin DSL Gradle, Compose for Android, Koin, Coroutines, Flow, SQLDelight, Ktor, kotlinx.serialization, WorkManager, Foreground Service, MediaProjection, AudioPlaybackCapture, AudioRecord, Android Keystore

## Global Constraints

- offline-first by default
- no silent cloud usage
- explicit user approval before any provider fallback
- degrade gracefully before failing
- preserve user trust with visible processing mode labels
- keep audio capture, transcription, and summarization isolated so each layer can fail independently
- Android v1 should support two operating modes: Live Assist Mode and Record Then Process Mode
- 8GB phones: default to smaller local models, shorter context windows, and more aggressive fallback to Record Then Process Mode
- 12GB or higher phones: allow stronger local models, richer context retention, and more live summarization
- playback capture is best-effort and microphone fallback must exist
- users can force local-only mode
- generated artifacts should indicate whether they were created using primary local, smaller local, or provider-assisted processing

---

## Scope Check

The approved design covers multiple subsystems. This plan intentionally implements only the first subproject:

- KMP shared foundation
- Android app shell
- Android recording-first capture path
- local persistence
- background processing orchestration hooks

This plan does **not** implement full ASR, LiteRT-LM summarization, provider APIs, or calendar/email ingestion. Those should be separate follow-on plans after the capture-first skeleton is stable on real Android devices.

## Planned File Structure

### Root

- `settings.gradle.kts`
  Declares all KMP and Android modules.
- `build.gradle.kts`
  Defines root plugins and shared repositories.
- `gradle/libs.versions.toml`
  Central dependency versions.
- `gradle.properties`
  JVM and AndroidX flags for KMP and Compose.

### Shared Modules

- `shared/core`
  Common result wrappers, dispatchers, clocks, IDs, and logging contracts.
- `shared/domain`
  Session entities, enums, use cases, and policy logic.
- `shared/storage`
  SQLDelight schema, repository interfaces, and storage implementations.
- `shared/providers`
  Provider-facing DTOs and future cloud client contracts.
- `shared/ai-contracts`
  Shared interfaces for transcription, summarization, and capability evaluation.
- `shared/export`
  Markdown and plain-text export formatters for meeting outputs.

### Android Modules

- `androidApp`
  Compose UI, navigation, manifest, permissions, and application startup.
- `android-core`
  Android-specific DI bootstrap, dispatchers, and app configuration.
- `android-capture`
  Audio capture abstractions and Android implementations.
- `android-background`
  Foreground service and WorkManager orchestration.
- `android-security`
  Secret storage backed by Android Keystore.
- `android-ai-local`
  Local LLM scaffolding and model catalog hooks.
- `android-asr`
  Local ASR scaffolding and transcription engine adapter hooks.

## Folder Tree Target

```text
MeetNote/
├── androidApp/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/meetnote/android/
│       │   ├── MeetNoteApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── navigation/MeetNoteNavHost.kt
│       │   ├── ui/MeetNoteApp.kt
│       │   ├── ui/session/SessionViewModel.kt
│       │   └── ui/session/SessionScreen.kt
│       └── res/values/strings.xml
├── android-background/
├── android-capture/
├── android-core/
├── android-security/
├── android-ai-local/
├── android-asr/
├── shared/
│   ├── ai-contracts/
│   ├── core/
│   ├── domain/
│   ├── export/
│   ├── providers/
│   └── storage/
├── docs/superpowers/plans/2026-07-07-meetnote-kmp-foundation-plan.md
├── docs/superpowers/specs/2026-07-07-meetnote-android-v1-design.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml
```

## Build Logic Plan

Use one root version catalog and module-specific `build.gradle.kts` files. The project should compile with:

- Android application module for `androidApp`
- Android library modules for Android-specific helper modules
- Kotlin Multiplatform library modules for `shared/*`

Use `Koin` instead of `Hilt` to avoid Android-only dependency injection assumptions inside shared code.

### Task 1: Bootstrap Root Gradle And Module Registration

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\settings.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\gradle.properties`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\gradle\libs.versions.toml`

**Interfaces:**
- Consumes: none
- Produces: Gradle module graph for all shared and Android modules

- [ ] **Step 1: Create `settings.gradle.kts` with all modules**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MeetNote"

include(":androidApp")
include(":android-core")
include(":android-capture")
include(":android-background")
include(":android-security")
include(":android-ai-local")
include(":android-asr")
include(":shared:core")
include(":shared:domain")
include(":shared:storage")
include(":shared:providers")
include(":shared:ai-contracts")
include(":shared:export")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
kotlin.mpp.androidSourceSetLayoutVersion=2
kotlin.native.ignoreDisabledTargets=true
```

- [ ] **Step 4: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.6.1"
kotlin = "2.0.21"
coroutines = "1.8.1"
serialization = "1.7.1"
ktor = "2.3.12"
sqldelight = "2.0.2"
koin = "3.5.6"
compose-bom = "2024.09.01"
activity-compose = "1.9.2"
lifecycle = "2.8.6"
work = "2.9.1"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 5: Run Gradle project listing**

Run: `.\gradlew.bat projects`
Expected: lists `androidApp`, `android-core`, `android-capture`, `android-background`, `android-security`, `android-ai-local`, `android-asr`, and all `shared:*` modules

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml
git commit -m "build: bootstrap MeetNote KMP module graph"
```

### Task 2: Create Shared Core And Domain Contracts

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\core\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\core\src\commonMain\kotlin\com\meetnote\shared\core\Result.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\core\src\commonMain\kotlin\com\meetnote\shared\core\Ids.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonMain\kotlin\com\meetnote\shared\domain\model\MeetingSession.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonMain\kotlin\com\meetnote\shared\domain\model\ProcessingMode.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonMain\kotlin\com\meetnote\shared\domain\model\ProcessingTier.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonMain\kotlin\com\meetnote\shared\domain\repository\SessionRepository.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonMain\kotlin\com\meetnote\shared\domain\usecase\CreateManualSessionUseCase.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\domain\src\commonTest\kotlin\com\meetnote\shared\domain\usecase\CreateManualSessionUseCaseTest.kt`

**Interfaces:**
- Consumes: Gradle module graph from Task 1
- Produces:
  - `interface SessionRepository { suspend fun createSession(session: MeetingSession): MeetingSession; fun observeSessions(): Flow<List<MeetingSession>>; suspend fun updateStatus(sessionId: SessionId, status: SessionStatus): Unit }`
  - `class CreateManualSessionUseCase(private val sessionRepository: SessionRepository) { suspend operator fun invoke(title: String, processingMode: ProcessingMode): MeetingSession }`

- [ ] **Step 1: Create `shared/core/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

- [ ] **Step 2: Create `Result.kt` and `Ids.kt`**

```kotlin
package com.meetnote.shared.core

sealed interface MeetNoteResult<out T> {
    data class Success<T>(val value: T) : MeetNoteResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : MeetNoteResult<Nothing>
}
```

```kotlin
package com.meetnote.shared.core

@JvmInline
value class SessionId(val value: String)
```

- [ ] **Step 3: Create `shared/domain/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 4: Create `MeetingSession.kt`, `ProcessingMode.kt`, `ProcessingTier.kt`, and `SessionRepository.kt`**

```kotlin
package com.meetnote.shared.domain.model

import com.meetnote.shared.core.SessionId

data class MeetingSession(
    val id: SessionId,
    val title: String,
    val status: SessionStatus,
    val processingMode: ProcessingMode,
    val processingTier: ProcessingTier,
    val source: SessionSource,
    val audioFilePath: String? = null
)

enum class SessionStatus { SCHEDULED, CAPTURING, RECORDED, PROCESSING, COMPLETED, FAILED }
enum class SessionSource { MANUAL, CALENDAR, EMAIL }
```

```kotlin
package com.meetnote.shared.domain.model

enum class ProcessingMode {
    LIVE_ASSIST,
    RECORD_THEN_PROCESS
}
```

```kotlin
package com.meetnote.shared.domain.model

enum class ProcessingTier {
    PRIMARY_LOCAL,
    SMALLER_LOCAL,
    PROVIDER_ASSISTED
}
```

```kotlin
package com.meetnote.shared.domain.repository

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(session: MeetingSession): MeetingSession
    fun observeSessions(): Flow<List<MeetingSession>>
    suspend fun updateStatus(sessionId: SessionId, status: SessionStatus)
    suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String)
}
```

- [ ] **Step 5: Create `CreateManualSessionUseCase.kt` and test**

```kotlin
package com.meetnote.shared.domain.usecase

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateManualSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(title: String, processingMode: ProcessingMode): MeetingSession {
        val session = MeetingSession(
            id = SessionId(Uuid.random().toString()),
            title = title,
            status = SessionStatus.SCHEDULED,
            processingMode = processingMode,
            processingTier = ProcessingTier.PRIMARY_LOCAL,
            source = SessionSource.MANUAL
        )
        return sessionRepository.createSession(session)
    }
}
```

```kotlin
package com.meetnote.shared.domain.usecase

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateManualSessionUseCaseTest {
    @Test
    fun createsManualRecordThenProcessSession() = runTest {
        val repository = object : SessionRepository {
            override suspend fun createSession(session: MeetingSession): MeetingSession = session
            override fun observeSessions(): Flow<List<MeetingSession>> = flowOf(emptyList())
            override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) = Unit
            override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) = Unit
        }

        val useCase = CreateManualSessionUseCase(repository)
        val result = useCase("Demo Meeting", ProcessingMode.RECORD_THEN_PROCESS)

        assertEquals("Demo Meeting", result.title)
        assertEquals(ProcessingMode.RECORD_THEN_PROCESS, result.processingMode)
        assertEquals(ProcessingTier.PRIMARY_LOCAL, result.processingTier)
        assertEquals(SessionSource.MANUAL, result.source)
    }
}
```

- [ ] **Step 6: Run shared domain test**

Run: `.\gradlew.bat :shared:domain:testDebugUnitTest`
Expected: PASS for `CreateManualSessionUseCaseTest`

- [ ] **Step 7: Commit**

```bash
git add shared/core shared/domain
git commit -m "feat: add shared session domain foundation"
```

### Task 3: Add Shared Storage With SQLDelight Session Persistence

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\storage\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\storage\src\commonMain\sqldelight\com\meetnote\storage\MeetNoteDatabase.sq`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\storage\src\commonMain\kotlin\com\meetnote\shared\storage\SessionStorage.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\storage\src\commonMain\kotlin\com\meetnote\shared\storage\SqlDelightSessionRepository.kt`

**Interfaces:**
- Consumes: `MeetingSession`, `SessionRepository`
- Produces:
  - `class SqlDelightSessionRepository(...) : SessionRepository`
  - SQL table `meeting_session`

- [ ] **Step 1: Create `shared/storage/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(project(":shared:domain"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

sqldelight {
    databases {
        create("MeetNoteDatabase") {
            packageName.set("com.meetnote.storage")
        }
    }
}
```

- [ ] **Step 2: Create `MeetNoteDatabase.sq`**

```sql
CREATE TABLE meeting_session (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    status TEXT NOT NULL,
    processing_mode TEXT NOT NULL,
    processing_tier TEXT NOT NULL,
    source TEXT NOT NULL,
    audio_file_path TEXT
);

selectAll:
SELECT * FROM meeting_session;

insertSession:
INSERT INTO meeting_session(id, title, status, processing_mode, processing_tier, source, audio_file_path)
VALUES (?, ?, ?, ?, ?, ?, ?);

updateStatus:
UPDATE meeting_session
SET status = ?
WHERE id = ?;

attachAudioFile:
UPDATE meeting_session
SET audio_file_path = ?
WHERE id = ?;
```

- [ ] **Step 3: Create `SessionStorage.kt` and `SqlDelightSessionRepository.kt`**

```kotlin
package com.meetnote.shared.storage

import com.meetnote.storage.MeetNoteDatabase

class SessionStorage(
    val database: MeetNoteDatabase
)
```

```kotlin
package com.meetnote.shared.storage

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightSessionRepository(
    private val storage: SessionStorage,
    private val queryToFlow: QueryToFlow
) : SessionRepository {
    override suspend fun createSession(session: MeetingSession): MeetingSession {
        storage.database.meetNoteDatabaseQueries.insertSession(
            id = session.id.value,
            title = session.title,
            status = session.status.name,
            processing_mode = session.processingMode.name,
            processing_tier = session.processingTier.name,
            source = session.source.name,
            audio_file_path = session.audioFilePath
        )
        return session
    }

    override fun observeSessions(): Flow<List<MeetingSession>> {
        return queryToFlow.asFlow(storage.database.meetNoteDatabaseQueries.selectAll())
            .map { rows ->
                rows.map {
                    MeetingSession(
                        id = SessionId(it.id),
                        title = it.title,
                        status = SessionStatus.valueOf(it.status),
                        processingMode = ProcessingMode.valueOf(it.processing_mode),
                        processingTier = ProcessingTier.valueOf(it.processing_tier),
                        source = SessionSource.valueOf(it.source),
                        audioFilePath = it.audio_file_path
                    )
                }
            }
    }

    override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
        storage.database.meetNoteDatabaseQueries.updateStatus(
            status = status.name,
            id = sessionId.value
        )
    }

    override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
        storage.database.meetNoteDatabaseQueries.attachAudioFile(
            audio_file_path = audioFilePath,
            id = sessionId.value
        )
    }
}

interface QueryToFlow {
    fun <T : Any> asFlow(query: app.cash.sqldelight.Query<T>): Flow<List<T>>
}
```

- [ ] **Step 4: Run SQLDelight code generation**

Run: `.\gradlew.bat :shared:storage:generateCommonMainMeetNoteDatabaseInterface`
Expected: PASS and generated SQLDelight interfaces under the build directory

- [ ] **Step 5: Commit**

```bash
git add shared/storage
git commit -m "feat: add shared SQLDelight session storage"
```

### Task 4: Build Android App Shell And Koin Wiring

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\AndroidManifest.xml`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\MeetNoteApplication.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\MainActivity.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\MeetNoteApp.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionViewModel.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionScreen.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-core\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-core\src\main\java\com\meetnote\android\core\AppModules.kt`

**Interfaces:**
- Consumes:
  - `CreateManualSessionUseCase`
  - `SessionRepository`
- Produces:
  - `class SessionViewModel(...)`
  - manual session creation screen

- [ ] **Step 1: Create `androidApp/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.meetnote.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meetnote.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":android-core"))
    implementation(project(":shared:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
}
```

- [ ] **Step 2: Create manifest and application bootstrap**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <application
        android:name=".MeetNoteApplication"
        android:label="MeetNote">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

```kotlin
package com.meetnote.android

import android.app.Application
import com.meetnote.android.core.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MeetNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MeetNoteApplication)
            modules(appModules)
        }
    }
}
```

- [ ] **Step 3: Create `SessionViewModel.kt` and `SessionScreen.kt`**

```kotlin
package com.meetnote.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionUiState(
    val title: String = "",
    val selectedMode: ProcessingMode = ProcessingMode.RECORD_THEN_PROCESS,
    val createdSessionId: String? = null
)

class SessionViewModel(
    private val createManualSession: CreateManualSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun createSession() {
        viewModelScope.launch {
            val session = createManualSession(
                title = _uiState.value.title.ifBlank { "Untitled Meeting" },
                processingMode = _uiState.value.selectedMode
            )
            _uiState.value = _uiState.value.copy(createdSessionId = session.id.value)
        }
    }
}
```

```kotlin
package com.meetnote.android.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionScreen(
    state: SessionUiState,
    onTitleChanged: (String) -> Unit,
    onCreateSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("MeetNote", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = { Text("Meeting title") }
        )
        Button(onClick = onCreateSession, modifier = Modifier.padding(top = 16.dp)) {
            Text("Create Record-Then-Process Session")
        }
        state.createdSessionId?.let { Text("Created session: $it", modifier = Modifier.padding(top = 16.dp)) }
    }
}
```

- [ ] **Step 4: Create `AppModules.kt`, `MainActivity.kt`, and `MeetNoteApp.kt`**

```kotlin
package com.meetnote.android.core

import com.meetnote.android.ui.session.SessionViewModel
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    single { get<com.meetnote.shared.domain.repository.SessionRepository>() }
    single { CreateManualSessionUseCase(get()) }
    viewModel { SessionViewModel(get()) }
}
```

```kotlin
package com.meetnote.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.meetnote.android.ui.MeetNoteApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeetNoteApp() }
    }
}
```

```kotlin
package com.meetnote.android.ui

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import com.meetnote.android.ui.session.SessionScreen
import com.meetnote.android.ui.session.SessionViewModel

@Composable
fun MeetNoteApp(viewModel: SessionViewModel = koinViewModel()) {
    val state = viewModel.uiState.value
    SessionScreen(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onCreateSession = viewModel::createSession
    )
}
```

- [ ] **Step 5: Run Android compile**

Run: `.\gradlew.bat :androidApp:compileDebugKotlin`
Expected: PASS with Compose and Koin wiring compiled

- [ ] **Step 6: Commit**

```bash
git add androidApp android-core
git commit -m "feat: add Android app shell and session creation UI"
```

### Task 5: Add Android Capture Module And Recording Contracts

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\MeetingRecorder.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\MicOnlyMeetingRecorder.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\RecorderResult.kt`

**Interfaces:**
- Consumes:
  - `SessionRepository.attachAudioFile`
  - `SessionRepository.updateStatus`
- Produces:
  - `interface MeetingRecorder { suspend fun start(sessionId: String): RecorderResult; suspend fun stop(sessionId: String): RecorderResult }`

- [ ] **Step 1: Create `android-capture/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.capture"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }
}

dependencies {
    implementation(project(":shared:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 2: Create `RecorderResult.kt` and `MeetingRecorder.kt`**

```kotlin
package com.meetnote.android.capture

sealed interface RecorderResult {
    data class Started(val filePath: String) : RecorderResult
    data class Stopped(val filePath: String) : RecorderResult
    data class Failure(val message: String) : RecorderResult
}
```

```kotlin
package com.meetnote.android.capture

interface MeetingRecorder {
    suspend fun start(sessionId: String): RecorderResult
    suspend fun stop(sessionId: String): RecorderResult
}
```

- [ ] **Step 3: Create `MicOnlyMeetingRecorder.kt` as the first real implementation**

```kotlin
package com.meetnote.android.capture

import android.content.Context
import java.io.File

class MicOnlyMeetingRecorder(
    private val context: Context
) : MeetingRecorder {
    private var lastFile: File? = null

    override suspend fun start(sessionId: String): RecorderResult {
        val file = File(context.filesDir, "$sessionId.raw")
        file.writeBytes(byteArrayOf())
        lastFile = file
        return RecorderResult.Started(file.absolutePath)
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        val file = lastFile ?: return RecorderResult.Failure("Recorder not started")
        return RecorderResult.Stopped(file.absolutePath)
    }
}
```

- [ ] **Step 4: Run Android capture compile**

Run: `.\gradlew.bat :android-capture:compileDebugKotlin`
Expected: PASS for recorder contracts and baseline mic recorder skeleton

- [ ] **Step 5: Commit**

```bash
git add android-capture
git commit -m "feat: add Android recording contracts"
```

### Task 6: Add Android Background Orchestration For Record-Then-Process

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\MeetingCaptureService.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\PostMeetingProcessingWorker.kt`

**Interfaces:**
- Consumes:
  - `MeetingRecorder`
  - `SessionRepository`
- Produces:
  - `MeetingCaptureService`
  - `PostMeetingProcessingWorker`

- [ ] **Step 1: Create `android-background/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.background"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }
}

dependencies {
    implementation(project(":android-capture"))
    implementation(project(":shared:domain"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.android)
}
```

- [ ] **Step 2: Create `MeetingCaptureService.kt`**

```kotlin
package com.meetnote.android.background

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MeetingCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [ ] **Step 3: Create `PostMeetingProcessingWorker.kt`**

```kotlin
package com.meetnote.android.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PostMeetingProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
```

- [ ] **Step 4: Run background module compile**

Run: `.\gradlew.bat :android-background:compileDebugKotlin`
Expected: PASS for service and worker scaffolding

- [ ] **Step 5: Commit**

```bash
git add android-background
git commit -m "feat: add record-then-process background orchestration skeleton"
```

### Task 7: Add AI And Security Scaffolding Interfaces

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\ai-contracts\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\ai-contracts\src\commonMain\kotlin\com\meetnote\shared\ai\TranscriptionEngine.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\shared\ai-contracts\src\commonMain\kotlin\com\meetnote\shared\ai\SummaryEngine.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-security\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-security\src\main\java\com\meetnote\android\security\ProviderKeyStore.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-ai-local\build.gradle.kts`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-asr\build.gradle.kts`

**Interfaces:**
- Consumes: shared core/domain modules
- Produces:
  - `interface TranscriptionEngine { suspend fun transcribe(audioPath: String): String }`
  - `interface SummaryEngine { suspend fun summarize(transcript: String): String }`
  - `interface ProviderKeyStore { suspend fun save(provider: String, apiKey: String); suspend fun read(provider: String): String? }`

- [ ] **Step 1: Create `shared/ai-contracts/build.gradle.kts` and interfaces**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
        }
    }
}
```

```kotlin
package com.meetnote.shared.ai

interface TranscriptionEngine {
    suspend fun transcribe(audioPath: String): String
}
```

```kotlin
package com.meetnote.shared.ai

interface SummaryEngine {
    suspend fun summarize(transcript: String): String
}
```

- [ ] **Step 2: Create Android security and local AI module build files**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.security"
    compileSdk = 35

    defaultConfig { minSdk = 29 }
}
```

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.ai.local"
    compileSdk = 35

    defaultConfig { minSdk = 29 }
}
```

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.asr"
    compileSdk = 35

    defaultConfig { minSdk = 29 }
}
```

- [ ] **Step 3: Create `ProviderKeyStore.kt`**

```kotlin
package com.meetnote.android.security

interface ProviderKeyStore {
    suspend fun save(provider: String, apiKey: String)
    suspend fun read(provider: String): String?
}
```

- [ ] **Step 4: Run module compile**

Run: `.\gradlew.bat :shared:ai-contracts:compileKotlinAndroid :android-security:compileDebugKotlin :android-ai-local:compileDebugKotlin :android-asr:compileDebugKotlin`
Expected: PASS for AI and security scaffolding

- [ ] **Step 5: Commit**

```bash
git add shared/ai-contracts android-security android-ai-local android-asr
git commit -m "feat: add AI and security scaffolding contracts"
```

## Self-Review

### Spec Coverage

- KMP project structure is covered by Tasks 1 through 4.
- record-then-process mode foundation is covered by Tasks 4 through 6.
- local-only and explicit fallback boundaries are preserved by shared domain enums and AI/security contracts in Tasks 2 and 7.
- audio capture with mic fallback foundation is covered by Task 5.

Gaps intentionally deferred to later plans:

- calendar and email connectors
- playback capture with MediaProjection
- full WorkManager session pipeline
- LiteRT-LM integration
- local ASR implementation
- provider API clients
- export format implementation

### Placeholder Scan

No `TBD`, `TODO`, or cross-reference placeholders remain in the task steps. Deferred items are explicitly called out as out of scope for this plan.

### Type Consistency

- `SessionRepository` signatures are reused consistently in Tasks 2 through 6.
- `ProcessingMode.RECORD_THEN_PROCESS` is the default mode in UI and shared domain.
- `MeetingRecorder` uses `sessionId: String`, which matches the current `SessionId.value` usage from the shared domain.

## Recommended Next Plans

After this plan is implemented and verified on a real Android device, create follow-on plans in this order:

1. playback capture plus MediaProjection consent flow
2. local ASR integration and transcript persistence
3. LiteRT-LM summary generation and model tiering
4. provider fallback onboarding and secure key usage
5. calendar and email meeting context connectors

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-07-meetnote-kmp-foundation-plan.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
