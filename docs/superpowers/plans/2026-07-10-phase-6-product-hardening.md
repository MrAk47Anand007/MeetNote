# Phase 6 Product Hardening and Controlled Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the trustworthy user workflow: review, export, retention, and explicit provider approval without weakening local-first behavior.

**Architecture:** The session screen reads persisted structured notes. Exporters operate on local artifacts; provider approval is a stored one-time decision passed through AiProcessingContext, never an automatic fallback.

**Tech Stack:** Kotlin, Compose, Android share APIs, SQLDelight, JUnit4

## Global Constraints

- No cloud provider is invoked without an explicit session-level approval.
- Exports are generated from persisted local artifacts.
- Live Assist is evaluated only after device-matrix evidence supports it.

---

### Task 1: Review and export structured meeting artifacts

**Files:**
- Create: `androidApp/src/main/java/com/meetnote/android/ui/session/MeetingNoteScreen.kt`
- Create: `androidApp/src/main/java/com/meetnote/android/export/MeetingNoteShareController.kt`
- Create: `androidApp/src/test/java/com/meetnote/android/export/MeetingNoteShareControllerTest.kt`

**Interfaces:**

```kotlin
interface MeetingNoteShareController { fun shareMarkdown(file: File): Intent }
fun exportPlainText(note: StructuredMeetingNote): String
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(Intent.ACTION_SEND, controller.shareMarkdown(markdown).action)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.export.MeetingNoteShareControllerTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
return Intent(Intent.ACTION_SEND).setType("text/markdown").putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, authority, file))
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.export.MeetingNoteShareControllerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add androidApp shared/export && git commit -m "feat: review and share local meeting notes"
```

### Task 2: Retention controls and local-only privacy state

**Files:**
- Modify: `shared/domain/src/commonMain/kotlin/com/meetnote/shared/domain/model/ProcessingPolicy.kt`
- Modify: `shared/storage/src/commonMain/kotlin/com/meetnote/shared/storage/SqlDelightSessionRepository.kt`
- Create: `androidApp/src/main/java/com/meetnote/android/ui/settings/PrivacyViewModel.kt`

**Interfaces:**

```kotlin
data class RetentionPolicy(val deleteAudioAfterDays: Int?, val deleteArtifactsAfterDays: Int?)
suspend fun updateRetentionPolicy(policy: RetentionPolicy)
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(ProcessingPolicy.LOCAL_ONLY, viewModel.uiState.value.processingPolicy)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :shared:storage:allTests :androidApp:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
if (policy == ProcessingPolicy.LOCAL_ONLY) context.copy(providerProcessingApproved = false) else context
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :shared:storage:allTests :androidApp:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/domain shared/storage androidApp && git commit -m "feat: add local retention and privacy controls"
```

### Task 3: Provider approval contract and Live Assist gate

**Files:**
- Modify: `android-security/src/main/java/com/meetnote/android/security/ProviderKeyStore.kt`
- Create: `shared/providers/src/commonMain/kotlin/com/meetnote/shared/providers/ApprovedProviderGateway.kt`
- Create: `shared/providers/src/commonTest/kotlin/com/meetnote/shared/providers/ApprovedProviderGatewayTest.kt`
- Create: `docs/validation/phase-6-live-assist-gate.md`

**Interfaces:**

```kotlin
suspend fun requestApprovedProcessing(sessionId: SessionId): ProviderApproval
interface ApprovedProviderGateway { suspend fun process(request: ProviderRequest, approval: ProviderApproval): ProviderResult }
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertFailsWith<SecurityException> { gateway.process(request, ProviderApproval.Denied) }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :shared:providers:allTests`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
require(approval is ProviderApproval.Granted) { "Provider processing requires explicit approval" }
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :shared:providers:allTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-security shared/providers docs/validation/phase-6-live-assist-gate.md && git commit -m "feat: enforce explicit provider approval"
```
