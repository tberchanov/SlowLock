# Contract: App List Screen (UI)

**Feature**: `001-installed-apps-list`

This is an application, not a library or service, so the contracts that matter are the UI
surface the user meets and the seam this screen exposes to the rest of the app. This file
covers the screen; `selection-handoff.md` covers the seam.

---

## Composable signature

```kotlin
@Composable
fun AppListScreen(
    onAppSelected: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(),
)
```

The screen owns no navigation. It reports a selection and nothing else — everything about
what happens next belongs to the caller.

---

## Observable behaviour

| # | Given | When | Then | Traces to |
|---|---|---|---|---|
| C1 | Screen entered | Enumeration in flight | A progress indicator is shown; the screen is not blank or frozen | FR-006, US1-AS2 |
| C2 | Enumeration completed with apps | List rendered | One row per package, each with icon and localized label, ordered by collated label | FR-001, FR-002, FR-005 |
| C3 | Any state | List rendered | No row is the SlowLock package | FR-003 |
| C4 | An app exposes several launcher activities | List rendered | Exactly one row for it | FR-004 |
| C5 | Enumeration returned nothing | List rendered | Empty-state text, not a blank area | FR-006 |
| C6 | Query typed | Filter applied | Only rows whose label contains the query (case-insensitive) remain | FR-007 |
| C7 | Query matches nothing | Filter applied | No-results text naming the query | FR-006 |
| C8 | Query cleared | Filter removed | Full list, original collated order | FR-008 |
| C9 | Row tapped, package resolves | Selection made | `onAppSelected(packageName)` invoked; the target app opens in the foreground, immediately and with no countdown | FR-009, FR-010, FR-018 |
| C10 | Row tapped, package uninstalled | Selection attempted | No crash; "no longer available" message; row removed; user stays on the list | FR-014 |
| C11 | Icon fails to load | Row rendered | Neutral placeholder icon; row still selectable | FR-016 |
| C12 | Screen recreated (rotation) or returned to | Screen recomposed | Scroll position and query preserved; no reload flash | FR-017 |
| C13 | Screen reopened after an install/uninstall | `ON_START` | List reflects the change | FR-013 |
| C14 | Any interaction | Always | Main thread never performs enumeration, icon rasterization, or disk I/O | FR-011, Constitution IV |
| C15 | Screen displayed | Ever | Zero permission dialogs | FR-015, SC-006 |

---

## Row contract

```kotlin
@Composable
private fun AppRow(
    app: InstalledApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- Fixed height, single-line label with `TextOverflow.Ellipsis` (spec edge case: very long
  names must not reflow or distort the row).
- Icon slot is a fixed 48dp box; it renders the placeholder until the real icon resolves,
  so row height never changes and scrolling cannot jump (SC-003).
- Keyed in the `LazyColumn` by `packageName`, so recycling cannot show a wrong icon
  (US1-AS3).

---

## State contract

`AppListViewModel` exposes exactly one stream:

```kotlin
val uiState: StateFlow<AppListUiState>
```

and accepts exactly these events:

```kotlin
fun onQueryChanged(query: String)
fun onAppTapped(packageName: String, onResolved: (String) -> Unit)
fun onUnavailableMessageShown()
fun refresh()   // called on ON_START (FR-013)
```

Invariants:

- `uiState` never emits a state where `isLoading` is true and `apps` is non-empty at the
  same time on a refresh — a refresh keeps the previous list visible rather than flashing
  a spinner (FR-017, "without a full reload flash").
- `apps` is always sorted and deduplicated; the screen never re-sorts.
- `onAppTapped` invokes `onResolved` only when
  `getLaunchIntentForPackage(packageName) != null`.

---

## Non-contract (explicitly out of scope)

The screen does **not**: persist a selection, apply any delay or countdown, evaluate
schedules, configure delay settings, pin shortcuts, show already-configured state, support
multi-select, list work-profile or dual-app clones, or update live while open. Each is
recorded as out of scope in `spec.md` Assumptions.

The screen itself does not launch anything either — it reports the selection through
`onAppSelected` and `MainActivity` starts the target (see `selection-handoff.md`). Keeping
the launch outside the screen is what makes replacing it with navigation to the
configuration screen a one-line change.
