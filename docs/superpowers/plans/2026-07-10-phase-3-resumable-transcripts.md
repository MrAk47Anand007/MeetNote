# Phase 3 Resumable Transcripts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Process long recordings in checkpointed chunks and preserve an ordered, resumable transcript.

**Architecture:** Transcript segments persist separately from the final artifact. WorkManager transcribes missing chunks only, then merges ordered text.

**Tech Stack:** Kotlin, SQLDelight, WorkManager, JUnit4

## Global Constraints

- Chunks are capped at five minutes.
- Retries cannot duplicate completed segments.
- Partial transcript remains readable after failure.

---

### Task 1: Transcript segment persistence

**Files:**
- Modify: `shared/storage/src/commonMain/sqldelight/com/meetnote/storage/MeetNoteDatabase.sq`
- Create: `shared/domain/src/commonMain/kotlin/com/meetnote/shared/domain/model/TranscriptSegment.kt`
- Modify: `shared/domain/src/commonMain/kotlin/com/meetnote/shared/domain/repository/SessionRepository.kt`

**Interfaces:**

```kotlin
data class TranscriptSegment(val sessionId: SessionId, val index: Int, val startMs: Long, val endMs: Long, val text: String)
suspend fun upsertTranscriptSegment(segment: TranscriptSegment)
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(listOf(0, 1), repository.transcriptSegments(id).map { it.index })
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :shared:storage:allTests`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
CREATE TABLE transcript_segment (session_id TEXT NOT NULL, segment_index INTEGER NOT NULL, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, text TEXT NOT NULL, PRIMARY KEY(session_id, segment_index));
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :shared:storage:allTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/domain shared/storage && git commit -m "feat: persist ordered transcript segments"
```

### Task 2: WAV chunk planner

**Files:**
- Create: `android-asr/src/main/java/com/meetnote/android/asr/WavChunkPlanner.kt`
- Create: `android-asr/src/test/java/com/meetnote/android/asr/WavChunkPlannerTest.kt`

**Interfaces:**

```kotlin
data class AudioChunk(val index: Int, val startMs: Long, val endMs: Long, val file: File)
fun plan(wavFile: File): List<AudioChunk>
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(listOf(0L, 300000L), planner.plan(wav).map { it.startMs })
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.WavChunkPlannerTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
return (0L until durationMs step 300000L).mapIndexed { index, start -> extractChunk(index, start, minOf(start + 300000L, durationMs)) }
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.WavChunkPlannerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-asr && git commit -m "feat: split long recordings into ASR chunks"
```

### Task 3: Resume worker from first incomplete segment

**Files:**
- Modify: `android-background/src/main/java/com/meetnote/android/background/PostMeetingProcessingScheduler.kt`
- Modify: `android-background/src/test/java/com/meetnote/android/background/DefaultPostMeetingProcessingExecutorTest.kt`

**Interfaces:**

```kotlin
suspend fun transcriptSegments(sessionId: SessionId): List<TranscriptSegment>
```

- [ ] **Step 1: Write the failing test**

```kotlin
repository.upsertTranscriptSegment(segment0); executor.process("s1", wav.path); verify { engine.transcribe(chunk1Request) }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-background:testDebugUnitTest :android-asr:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
planner.plan(audio).drop(existingSegments.size).forEach { chunk -> repository.upsertTranscriptSegment(transcribeChunk(chunk)) }
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-background:testDebugUnitTest :android-asr:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-background android-asr && git commit -m "feat: resume transcript processing by chunk"
```
