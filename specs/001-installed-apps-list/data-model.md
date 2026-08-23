# Phase 1 Data Model: Installed Applications List

**Feature**: `001-installed-apps-list` | **Date**: 2026-08-22

Three entities from the spec, plus the in-memory shapes they take. Nothing here is a
database schema — the feature has no persistence engine (Constitution II); the only thing
that outlives the process is the icon file cache described in §3.

---

## 1. InstalledApp

The spec's **Installed Application**. A launchable app on the current user profile,
already deduplicated and ready to display.

```kotlin
data class InstalledApp(
    val packageName: String,   // stable identity — the ONLY persisted/passed value
    val label: String,         // localized, display only
    val versionCode: Long,     // icon-cache staleness marker
)
```

| Field | Source | Rules |
|---|---|---|
| `packageName` | `LauncherActivityInfo.applicationInfo.packageName` | Non-empty. Unique across the list (FR-004). The only identifier used for matching, persistence, or hand-off (FR-010, Constitution V). |
| `label` | `LauncherActivityInfo.label.toString()` | May be empty, may duplicate another app's label (both rows still appear — spec edge case). Display only; never a key, never used in matching (Constitution V). |
| `versionCode` | `PackageInfo.longVersionCode` | Used only as part of the icon cache key. Not identity. |

**Deliberately absent**: the icon. Icons are loaded lazily per visible row and cached
separately (§3), so they never travel inside the list state. Also absent: `ComponentName`
and the launcher activity name — Constitution V forbids persisting or matching on them.

### Construction rules (the boundary mapping)

Applied in order when turning `LauncherApps` output into `List<InstalledApp>`:

1. **Enumerate** — `getActivityList(null, Process.myUserHandle())`.
2. **Exclude self** — drop entries whose `packageName` equals the app's own (FR-003).
3. **Deduplicate** — group by `packageName`, keep one entry per group (FR-004).
4. **Sort** — `Collator.getInstance(primaryLocale)` at `SECONDARY` strength over `label`
   (FR-005, R3).

Steps 2–4 are pure functions over `List<InstalledApp>` and are unit-tested directly (R10).

---

## 2. AppListUiState

The spec's **App List State**: what the screen is currently showing, plus the query.

```kotlin
data class AppListUiState(
    val isLoading: Boolean,
    val apps: List<InstalledApp>,        // full, sorted, deduplicated
    val query: String,
    val unavailableAppMessage: String?,  // set when a tapped app is gone (FR-014)
)
```

Derived, not stored (single source of truth — `apps` and `query`):

```kotlin
val visibleApps: List<InstalledApp>
    get() = if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
```

### Display states (FR-006)

The four states the spec names are read off the state rather than stored as an enum, so
they cannot contradict the data:

| State | Condition | Screen shows |
|---|---|---|
| Loading | `isLoading` | Progress indicator |
| Populated | `visibleApps.isNotEmpty()` | The list |
| Empty | `!isLoading && apps.isEmpty()` | "No apps found" explanation |
| No results | `!isLoading && apps.isNotEmpty() && visibleApps.isEmpty()` | "No apps match '<query>'" |

### Transitions

```text
                  enumeration completes
Loading ─────────────────────────────────► Populated / Empty
   ▲                                             │
   │ screen reopened (ON_START, FR-013)          │ query typed / cleared
   └─────────────────────────────────────────────┤
                                                 ▼
                                          Populated ⇄ No results
```

Tapping a row is not a state transition of the list: it either hands the package name
forward (FR-009) or, if the package no longer resolves, sets `unavailableAppMessage` and
removes that entry from `apps` (FR-014).

### Persistence across recreation (FR-017)

| Value | Survives rotation via | Survives process death via |
|---|---|---|
| `apps` | `ViewModel` | Re-enumerated (acceptable — this is a cold start) |
| `query` | `ViewModel` | `SavedStateHandle` |
| Scroll position | `rememberLazyListState()` (saveable) | `rememberLazyListState()` |

---

## 3. CachedIcon

The spec's **Cached Icon**. Not a Kotlin entity so much as a keying rule, applied at two
tiers (R5).

**Key**: `"$packageName:$versionCode"` — mandated by Constitution V.

| Tier | Shape | Lifetime |
|---|---|---|
| Memory | `LruCache<String, ImageBitmap>`, ~40 entries | Process |
| Disk | `cacheDir/app-icons/<packageName>-<versionCode>.webp` | Until swept or cache cleared |

**Invalidation**: implicit. An app update changes `versionCode`, so the new key misses
both tiers and the icon is re-rasterized. Stale files are never read; they are swept on
cache open by deleting any file whose `<packageName>-<versionCode>` stem is absent from
the freshly enumerated set.

**Miss path**: `LauncherActivityInfo.getIcon(densityDpi)` → `toBitmap()` → memory tier →
WebP write. All off the main thread (FR-011).

**Failure path**: if the icon is missing or fails to load, the row renders a neutral
placeholder and stays selectable (FR-016). A failure is not cached — a later visit
retries.

---

## Entity relationships

```text
InstalledApp ──1:1── CachedIcon        (by packageName + versionCode)
     │
     └──0..n──> AppListUiState.apps    (sorted, deduplicated, self excluded)

AppListUiState.query ──filters──> visibleApps ──renders──> rows
```
