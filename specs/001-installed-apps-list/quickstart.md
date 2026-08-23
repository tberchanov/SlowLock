# Quickstart: Installed Applications List

**Feature**: `001-installed-apps-list`

---

## Prerequisites

- JDK 11+ (project targets Java 11)
- Android SDK with API 37 installed; `local.properties` already points at the SDK
- A device or emulator on API 33+ (`minSdk 33`)

No git repository — this project was initialized with `--no-git`.

---

## Build and test

```bash
./gradlew assembleDebug     # constitution build gate
./gradlew test              # six JVM unit assertions — dedup, collation, filter, cache key, null-intent path
./gradlew installDebug      # push to a connected device
```

There is no `connectedAndroidTest` suite. This feature is verified manually — see
`manual-test-plan.md`.

Both `assembleDebug` and `test` MUST pass before this feature is reported complete
(Constitution, Build gate).

---

## Where the code goes

```text
app/src/main/java/com/slowlock/
├── MainActivity.kt                    # hosts AppListScreen, supplies onAppSelected
└── apps/
    ├── InstalledApp.kt                # data class + pure sort/dedup/filter functions
    ├── InstalledAppsSource.kt         # LauncherApps enumeration (suspend, IO)
    ├── AppIconCache.kt                # LruCache + cacheDir/app-icons WebP tier
    ├── AppListUiState.kt              # state + derived display states
    ├── AppListViewModel.kt            # StateFlow, SavedStateHandle for the query
    └── AppListScreen.kt               # Scaffold + search field + LazyColumn + AppRow

app/src/test/java/com/slowlock/apps/
    ├── InstalledAppTest.kt             # dedup, collation, filter, cache key
    └── AppListViewModelTest.kt         # null launch-intent path
```

---

## Manifest change required

Add to `app/src/main/AndroidManifest.xml`, as a direct child of `<manifest>` (sibling of
`<application>`):

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

Do **not** add `QUERY_ALL_PACKAGES` (Constitution III). No `<uses-permission>` element is
needed for this feature at all.

---

## Dependency change required

In `gradle/libs.versions.toml`:

```toml
[versions]
lifecycleRuntimeKtx = "2.6.1"   # existing — reused for the version ref

[libraries]
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
```

Then in `app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
```

Versions go in `libs.versions.toml` only — hardcoded coordinates in `build.gradle.kts` are
prohibited (Constitution, Technology Standards).

---

## Manual verification

**See [`manual-test-plan.md`](./manual-test-plan.md)** — it is the primary verification
artifact for this feature, with device prep, tiered cases, and the adb commands for the
timing and jank measurements.

Quick reference:

- **§1** — device prep (do this once; several cases are impossible without it)
- **§2 Tier 1** — must pass, ~20 min, no tooling needed
- **§3 Tier 2** — edge cases and performance, ~25 min, needs adb
- **§4 Tier 3** — non-Pixel OEM device, once before release
- **§6** — five-minute smoke test for re-checking after a change
