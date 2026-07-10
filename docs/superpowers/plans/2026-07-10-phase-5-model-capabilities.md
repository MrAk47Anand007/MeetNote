# Phase 5 Model and Capability Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users install, remove, select, and safely run primary or smaller local models.

**Architecture:** A capability profiler and model registry are Android services. They select immutable installed-model metadata before ASR/LLM execution and return an actionable local outcome when no compatible model is available.

**Tech Stack:** Kotlin, Android storage APIs, WorkManager, SQLDelight, JUnit4

## Global Constraints

- Local-only remains a first-class mode.
- Downloads are explicit user actions and checksum-verified.
- Selection considers free storage, ABI, RAM tier, thermal state, and battery.

---

### Task 1: Installed model registry

**Files:**
- Create: `shared/domain/src/commonMain/kotlin/com/meetnote/shared/domain/model/InstalledModel.kt`
- Create: `android-ai-local/src/main/java/com/meetnote/android/ailocal/InstalledModelRegistry.kt`
- Create: `android-ai-local/src/test/java/com/meetnote/android/ailocal/InstalledModelRegistryTest.kt`

**Interfaces:**

```kotlin
data class InstalledModel(val id: String, val kind: ModelKind, val tier: ProcessingTier, val filePath: String, val sha256: String)
interface InstalledModelRegistry { fun available(kind: ModelKind): List<InstalledModel> }
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals("llm-small", registry.available(ModelKind.SUMMARY).single().id)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest --tests com.meetnote.android.ailocal.InstalledModelRegistryTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
fun available(kind: ModelKind) = metadata.filter { it.kind == kind && File(it.filePath).isFile }
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest --tests com.meetnote.android.ailocal.InstalledModelRegistryTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/domain android-ai-local && git commit -m "feat: register installed local models"
```

### Task 2: Capability profiling and tier selection

**Files:**
- Create: `android-core/src/main/java/com/meetnote/android/core/DeviceCapabilityProfiler.kt`
- Create: `android-core/src/test/java/com/meetnote/android/core/ModelTierSelectorTest.kt`
- Modify: `android-core/src/main/java/com/meetnote/android/core/AppModules.kt`

**Interfaces:**

```kotlin
data class DeviceCapabilities(val ramGb: Int, val freeStorageMb: Long, val thermalStatus: Int, val batteryPercent: Int)
fun selectSummaryModel(capabilities: DeviceCapabilities, models: List<InstalledModel>): InstalledModel?
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(ProcessingTier.SMALLER_LOCAL, selector.select(lowRamCapabilities, models)?.tier)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-core:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
return models.firstOrNull { it.tier == ProcessingTier.PRIMARY_LOCAL && capabilities.ramGb >= 12 } ?: models.firstOrNull { it.tier == ProcessingTier.SMALLER_LOCAL }
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :android-core:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-core && git commit -m "feat: select local models by device capability"
```

### Task 3: Explicit install/remove workflow

**Files:**
- Create: `android-ai-local/src/main/java/com/meetnote/android/ailocal/ModelInstallWorker.kt`
- Create: `androidApp/src/main/java/com/meetnote/android/ui/models/ModelViewModel.kt`
- Create: `androidApp/src/test/java/com/meetnote/android/ui/models/ModelViewModelTest.kt`

**Interfaces:**

```kotlin
suspend fun install(descriptor: ModelDescriptor): ModelInstallResult
suspend fun remove(modelId: String): Boolean
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(ModelUiState.Ready("llm-small"), viewModel.uiState.value)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest :androidApp:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
if (sha256(file) != descriptor.sha256) return ModelInstallResult.Failed("Model checksum mismatch"); registry.add(installedModel)
```

- [ ] **Step 4: Run focused verification**

Run: `./gradlew.bat :android-ai-local:testDebugUnitTest :androidApp:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-ai-local androidApp && git commit -m "feat: manage local model installation"
```
