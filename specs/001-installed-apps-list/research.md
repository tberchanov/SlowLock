# Phase 0 Research: Installed Applications List

**Feature**: `001-installed-apps-list` | **Date**: 2026-08-22

All Technical Context unknowns raised by the plan are resolved below. No
`NEEDS CLARIFICATION` markers remain.

---

## R1. Enumeration API — how the launchable app list is read

**Decision**: `LauncherApps.getActivityList(null, Process.myUserHandle())`, with the
manifest `<queries>` declaration for `ACTION_MAIN` + `CATEGORY_LAUNCHER` retained.

**Rationale**:

- `highlevel_spec.md` §7 step 1 names the picker as "`LauncherApps` + off-thread icon
  loading + disk cache", and §3.1 records `LauncherApps` as the preferred alternative
  because it is profile-aware.
- `LauncherActivityInfo` carries `applicationInfo.packageName`, `label`, and
  `getIcon(density)` in one object — no second `PackageManager` round trip per row.
- Passing `Process.myUserHandle()` (rather than `null` for all profiles) matches the
  spec assumption "Current user profile only", and leaves work-profile support as a
  clean later change: one argument.
- Requires no permission and triggers no dialog, satisfying FR-015 and SC-006.

**Alternatives considered**:

| Alternative | Rejected because |
|---|---|
| `PackageManager.queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)` | Equivalent data but not profile-aware; returns `ResolveInfo` requiring extra lookups for icon/label; contradicts `highlevel_spec.md` §3.1 preference. |
| `PackageManager.getInstalledApplications()` | Returns non-launchable packages, violating FR-001; would need `QUERY_ALL_PACKAGES`, forbidden by Constitution III. |

**Constitution note**: the `<queries>` element stays in the manifest even though
`LauncherApps` is not subject to package-visibility filtering. Constitution III states
the declaration as a binding rule, and it keeps visibility correct if any code later
falls back to `PackageManager`.

---

## R2. Deduplication and identity

**Decision**: group the returned `LauncherActivityInfo` list by
`applicationInfo.packageName` and keep one entry per package (the first, after sorting
the group by label for determinism). `packageName` is the only value persisted or passed
across the selection hand-off.

**Rationale**: FR-004 requires each application to appear exactly once even when it
exposes several launcher entry points (common: Settings, some OEM suites). Constitution V
forbids persisting `ComponentName` or launcher activity names, so identity collapses to
the package. FR-010 ("survives updates and renames") is satisfied precisely because the
package name is what the platform guarantees stable.

**Alternatives considered**: one row per launcher activity (matches the app drawer on
some OEMs) — rejected, FR-004 is explicit, and per-activity identity is exactly the
silent-breakage failure Constitution V exists to prevent.

---

## R3. Locale-correct sorting

**Decision**: sort with `java.text.Collator.getInstance(locale)` at
`Collator.SECONDARY` strength, where `locale` comes from the configuration's primary
locale, applied to the displayed label.

**Rationale**: FR-005 requires case-insensitive alphabetical ordering "using the device's
current language rules". Naive `String.lowercase()` + `compareTo` orders by UTF-16 code
unit and misplaces accented and non-Latin labels (e.g. `Ä` after `Z` in German, wrong
order entirely for Cyrillic/Greek mixes). `Collator` at SECONDARY strength ignores case
differences while respecting accents — the ordering a user of that language expects.

Collator is `java.text`, part of the platform. No dependency added.

**Alternatives considered**: `String.CASE_INSENSITIVE_ORDER` — rejected, locale-blind.
`compareBy { it.label.lowercase(locale) }` — rejected, still code-unit ordering after the
case fold.

---

## R4. Filtering (search)

**Decision**: pure in-memory `contains` against the label, case-insensitively via
`lowercase(locale)` on both sides, applied to the already-sorted list; no debounce.

**Rationale**: FR-007 specifies substring matching against any part of the displayed
name, case-insensitive. With ~150 entries already in memory, filtering is sub-millisecond
— a debounce would add latency and state for no gain, and Constitution II (YAGNI) rules
out the machinery. Filtering preserves the sorted order, satisfying FR-008's "restored in
its original order" for free (the full list is never re-sorted, only re-filtered).

Case folding here uses `lowercase(locale)` rather than Collator: substring search with a
Collator requires `StringSearch`/ICU iteration, which is real complexity for a matching
rule the spec defines only as "ignoring case".

**Alternatives considered**: `Collator`-based `StringSearch` for accent-insensitive
search — rejected as beyond FR-007 and beyond YAGNI; revisit only if users report it.

---

## R5. Icon loading and caching

**Decision**: two-tier cache.

- **Tier 1 (memory)**: `LruCache<String, ImageBitmap>` keyed by `"$packageName:$versionCode"`,
  sized to ~40 entries, held by the icon cache object for the process lifetime.
- **Tier 2 (disk)**: WebP files under `context.cacheDir/app-icons/`, named
  `<packageName>-<versionCode>.webp`, written after the first rasterization.
- Icons are loaded lazily per visible row, off the main thread, and a stale entry is
  never read because the `versionCode` is part of the key — an app update simply misses
  the cache. Orphaned files from old versions are swept on cache open (delete any file
  whose `<packageName>-<versionCode>` no longer matches the installed set).

**Rationale**: FR-012 requires caching invalidated on app update, and Constitution V
mandates the `packageName` + `versionCode` key explicitly; `highlevel_spec.md` §3.3 says
the same. SC-005 (second open at least twice as fast) needs persistence across process
death, so memory alone is insufficient. Rasterizing an adaptive icon is the expensive
part — a decode from WebP is far cheaper than re-rendering the drawable.

`versionCode` is read as `PackageInfo.longVersionCode`.

**Alternatives considered**:

| Alternative | Rejected because |
|---|---|
| Coil / Glide | New third-party dependency (Constitution II) for a case they do not fit — the source is a `Drawable` from the system, not a URL. |
| Memory cache only | Fails SC-005 across process restarts. |
| Cache keyed by package only | Directly violates Constitution V; serves a stale icon after update, contradicting the spec's edge case. |
| Preload all icons up front | Rasterizing 150 adaptive icons before first frame breaks SC-001. |

---

## R6. Rendering a `Drawable` in Compose without a new dependency

**Decision**: convert to bitmap off-thread with `androidx.core.graphics.drawable.toBitmap()`
(already available via `androidx-core-ktx`) and render with `Image(bitmap = …)`.

**Rationale**: `Accompanist`'s `rememberDrawablePainter` is the usual answer and is a
third-party dependency the constitution defaults to refusing. `toBitmap()` is one call in
a library the project already ships, and the conversion must happen anyway to write the
disk cache. Adaptive icons rasterize correctly through `toBitmap()` because
`AdaptiveIconDrawable` implements `draw()`.

Target size: 48dp converted to px at the device density, requested from
`getIcon(displayMetrics.densityDpi)`.

---

## R7. State holder and rotation survival

**Decision**: a single `AppListViewModel` (`androidx.lifecycle.ViewModel`) exposing one
`StateFlow<AppListUiState>`, obtained in Compose via `viewModel()` from
`androidx-lifecycle-viewmodel-compose`. Search query lives in `SavedStateHandle`; scroll
position lives in `rememberLazyListState()`.

**Rationale**: FR-017 requires scroll position and query to survive screen recreation, and
the spec's edge case adds "without a full reload flash" — which rules out re-reading the
package list on every configuration change. A `ViewModel` survives rotation and keeps the
loaded list in memory; `SavedStateHandle` additionally survives process death for the
query. `rememberLazyListState()` is already `rememberSaveable`-backed.

This adds `androidx-lifecycle-viewmodel-compose` — see Complexity Tracking in `plan.md`.

**Alternatives considered**: `rememberSaveable` holding the list — rejected, the parcelled
list would cross the `TransactionTooLarge` risk zone and cannot hold bitmaps. Reloading
from `LauncherApps` on every recreation — rejected, causes the reload flash the spec
forbids.

---

## R8. Threading

**Decision**: `viewModelScope` + `Dispatchers.IO` for enumeration and icon work; results
published to the UI through `StateFlow`. `kotlinx-coroutines` arrives transitively with
`lifecycle-runtime-ktx`, already a dependency.

**Rationale**: FR-011 and Constitution IV both require package enumeration, icon
rasterization, and disk I/O off the main thread. Coroutines are the idiomatic Jetpack
answer and add no new coordinate. Icon loads are launched per row and cancelled with the
row's composition scope, so scrolling past an item stops its work — this is what keeps
SC-003 (no stutter, no mismatched rows) achievable.

---

## R9. Refresh-on-open and the uninstalled-app path

**Decision**: re-enumerate in `ViewModel.init` and on `ON_START` after the first, with no
`BroadcastReceiver`. At tap time, re-resolve the package via
`packageManager.getLaunchIntentForPackage(packageName)`; a `null` result surfaces a
"no longer available" message and drops the row from the list.

**Rationale**: FR-013 requires the list re-read each time the screen is opened; the spec's
assumptions explicitly reject live updates. FR-014 and Constitution IV both require the
`null` return from `getLaunchIntentForPackage()` to be handled at the call site rather
than assumed — this is the exact null path the constitution names.

**Alternatives considered**: registering a `PACKAGE_ADDED`/`PACKAGE_REMOVED` receiver or
`LauncherApps.Callback` — rejected by the spec's own assumption and by Constitution II.

---

## R10. Testing approach

**Decision**: keep sorting, deduplication, and filtering as pure Kotlin functions over a
plain `InstalledApp` data class, unit-tested with JUnit4 in `app/src/test`. Cover the
Compose screen's three empty-ish states (loading / empty / no-results) and selection with
an instrumented Compose test in `app/src/androidTest`.

**Rationale**: the constitution mandates automated coverage for target resolution and the
null `getLaunchIntentForPackage()` path, and permits pure-Compose presentation without
branching to ship untested. The branching here (dedup, collation, filter, three states) is
exactly what is worth testing, and it is all testable without a device once the pure
functions do not touch `Context`. This shapes the design: `LauncherApps` results are
mapped to `InstalledApp` at the boundary, and everything after that is pure.

No `kotlinx-coroutines-test` is required for these tests, because the tested functions are
synchronous.
