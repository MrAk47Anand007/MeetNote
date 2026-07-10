# Phase 4 LiteRT Local LLM and MOM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace deterministic summary extraction with local LiteRT-backed structured meeting minutes.

**Architecture:** A LiteRT adapter implements SummaryEngine. It generates JSON matching a strict schema; validation happens before markdown rendering. Long transcripts use segment summaries followed by a final reducer.

**Tech Stack:** Kotlin, Google AI Edge MediaPipe LlmInference, LiteRT-compatible model assets, kotlinx.serialization, JUnit4

## Global Constraints

- No transcript or model leaves the device.
- A malformed model response is Deferred, never silently rendered.
- All results preserve the actual ProcessingTier.

---

### Task 1: Structured meeting-note contract

**Files:**
- Create: `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/StructuredMeetingNote.kt`
- Create: `shared/ai-contracts/src/commonTest/kotlin/com/meetnote/shared/ai/StructuredMeetingNoteTest.kt`
- Modify: `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/SummaryEngine.kt`

**Interfaces:**

```kotlin
@Serializable data class StructuredMeetingNote(val summary: String, val minutes: List<String>, val actionItems: List<String>, val decisions: List<String>, val risks: List<String>, val followUps: List<String>)
typealias SummaryResult = AiProcessingResult<StructuredMeetingNote>
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals("Ship Friday", StructuredMeetingNote.fromJson(validJson).actionItems.single())
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :shared:ai-contracts:allTests`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
fun fromJson(raw: String): StructuredMeetingNote = json.decodeFromString(raw).also { require(it.summary.isNotBlank()) }
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :shared:ai-contracts:allTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/ai-contracts && git commit -m "feat: add structured meeting note contract"
```

### Task 2: LiteRT summary adapter

**Files:**
- Modify: `android-ai-local/build.gradle.kts`
- Create: `android-ai-local/src/main/java/com/meetnote/android/ailocal/LiteRtSummaryEngine.kt`
- Create: `android-ai-local/src/test/java/com/meetnote/android/ailocal/LiteRtSummaryEngineTest.kt`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**

```kotlin
interface LocalLlmRuntime { suspend fun generate(prompt: String): String }
class LiteRtSummaryEngine(private val runtime: LocalLlmRuntime) : SummaryEngine
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals("Launch summary", completedResult.value.summary); assertTrue(malformedResult is AiProcessingResult.Deferred)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
val prompt = MeetingMomPrompt.render(transcript); return parse(runtime.generate(prompt)).fold(::completed, ::deferred)
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-ai-local gradle/libs.versions.toml && git commit -m "feat: generate meeting minutes with LiteRT"
```

### Task 3: Hierarchical MOM and markdown rendering

**Files:**
- Create: `android-ai-local/src/main/java/com/meetnote/android/ailocal/HierarchicalMeetingSummarizer.kt`
- Modify: `shared/export/src/commonMain/kotlin/com/meetnote/shared/export/MeetingNoteMarkdownFormatter.kt`
- Modify: `android-background/src/test/java/com/meetnote/android/background/DefaultPostMeetingProcessingExecutorTest.kt`

**Interfaces:**

```kotlin
suspend fun summarizeSegments(segments: List<TranscriptSegment>): SummaryResult
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertTrue(noteFile.readText().contains("## Decisions")); assertTrue(noteFile.readText().contains("## Action Items"))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest :android-background:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
val reducedInput = segments.map { runtime.generate(MeetingMomPrompt.render(it.text)) }.joinToString("\n"); return engine.summarize(SummaryRequest(reducedInput))
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest :android-background:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-ai-local shared/export android-background && git commit -m "feat: render hierarchical local meeting minutes"
```
