# Implementation Plan: Pinned Shortcut Creation

**Branch**: `002-shortcut-pinning` | **Date**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-shortcut-pinning/spec.md`

## Summary

Tapping an app in feature 001's list now opens a configuration screen — a centred preview of the
shortcut, a scrollable row of icon treatments (Original, Invert, Gray), back at the top, "Create
shortcut" at the bottom — which pins a home-screen shortcut carrying the previewed icon and
closes. Tapping that shortcut opens the target app immediately.

This is a feasibility draft, and the plan treats it as one: almost everything is disposable, and
one thing is not. **The pinned shortcut's shape is frozen** (`contracts/pinned-shortcut.md`)
because shortcuts pinned during this draft stay on the user's home screen forever. The decision
that carries the whole feature is that the shortcut's intent points at a SlowLock activity —
`ShortcutLaunchActivity`, carrying only the target's package name — and never at the target app's
own launch intent. That is what makes FR-011 true: when the delay feature ships, it changes that
activity's body and every already-pinned shortcut gains the countdown with nothing asked of the
user. A shortcut pointing straight at the target would freeze its behaviour at pin time and
require re-pinning every icon, which is not something a user can be asked to do.

Technical approach: the shortcut ID **is** the target's package name, so re-pinning is idempotent
with no bookkeeping (FR-027) — `updateShortcuts()` refreshes an existing shortcut in place,
`requestPinShortcut()` pins a new one, both called unconditionally, each a no-op in the case it
does not apply. One 4×5 colour matrix per treatment feeds both the live preview (a Compose
`ColorFilter`, so switching costs a recomposition and no allocation) and the pinned bitmap (baked
once on `Dispatchers.IO` at create time), which makes "the icon that lands matches the preview"
structural rather than a thing to verify by eye. `isRequestPinShortcutSupported()` is checked on
every `ON_START` and gates the root: unsupported launchers get an explanation screen instead of
the app list. Navigation is a `when` over root state — no navigation library — with a
`SaveableStateHolder` so the list's scroll offset survives the round trip. No new permissions, no
new dependencies, no persistence.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java/JVM target 11

**Primary Dependencies**: **No new dependencies.** Existing set only — Jetpack Compose (BOM
2026.02.01), Material 3, `core-ktx`, `activity-compose`, `lifecycle-runtime-ktx`,
`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`. Platform APIs added by this feature:
`ShortcutManager`, `ShortcutInfo`, `Icon`, `ColorMatrixColorFilter`, `Settings.ACTION_HOME_SETTINGS`.
`androidx.compose.runtime.saveable.rememberSaveableStateHolder` is already on the classpath
transitively via Compose UI. Reuses feature 001's `AppListScreen` unchanged and its `AppIconCache` with one added
`(packageName, versionCode)` overload (T010) — same cache keys, same on-disk layout.

**Storage**: None. Nothing this feature produces is persisted by SlowLock — the launcher owns the
shortcut once pinned, and the draft lives only while the screen is composed. The only disk touched
is feature 001's existing icon cache, read-only from here.

**Testing**: **Manual-first**, matching feature 001 — `manual-test-plan.md` is the primary
artifact, because the open questions are what real launchers on real devices do (SC-008 asks for
two vendors). Automated coverage is four JVM test classes over pure functions and injected seams:
treatment matrices, the frozen shortcut contract (including a rename guard on
`ShortcutLaunchActivity`'s FQN), the null `getLaunchIntentForPackage()` path required by the
constitution, and the pin-support gate. No instrumented suite. Gates: `./gradlew assembleDebug`
and `./gradlew test`.

**Target Platform**: Android, `minSdk 33`, `targetSdk`/`compileSdk 37`

**Project Type**: Mobile app — single `:app` Gradle module, `com.slowlock`

**Performance Goals**: Treatment switching updates the preview in under 100 ms with no flicker or
layout shift (SC-004); list → home-screen shortcut in ≤3 taps and under 30 seconds (SC-001)

**Constraints**: Zero permission prompts (FR-023, SC-005); no main-thread icon rasterization or
bitmap treatment (FR-024); pinned shortcuts must survive force-stop and reboot with no service,
receiver, or stored state (FR-017); pinned shortcuts must remain upgradeable to the future delay
behaviour without re-creation (FR-011); no SlowLock screen or recents entry when a shortcut is
tapped (FR-019)

**Scale/Scope**: Two new screens plus one invisible launch activity, 9 new source files and one
widened feature 001 file, 3 icon treatments, 1 shortcut per app

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.0.0. Evaluated pre-research and re-evaluated post-design; both results shown.

| Principle | Binding rule as it applies here | Pre-Phase 0 | Post-Phase 1 |
|---|---|---|---|
| **I. Cooperative User, Not Adversary** | The draft imposes no friction at all yet — the shortcut launches immediately (FR-016). Shortcut removal from the launcher stays an accepted bypass path and is explicitly not closed (spec, Out of Scope). Declining the pin dialog is treated as a legitimate choice, not an error (FR-014). | ✅ PASS | ✅ PASS |
| **II. Simplicity First (YAGNI)** | **Zero new dependencies** — `navigation-compose` rejected (R9), `ShortcutManagerCompat` rejected (R1), Robolectric rejected (R12). No new module, no DI, no persistence engine, no ViewModel for the configuration screen (R10). Nothing built for the delay feature beyond the one frozen seam FR-011 demands. | ✅ PASS | ✅ PASS |
| **III. Permission & Policy Minimalism** | **Zero** new `<uses-permission>` elements; `requestPinShortcut` needs none. No `QUERY_ALL_PACKAGES`, no `AccessibilityService`, no `PACKAGE_USAGE_STATS`. `ShortcutLaunchActivity` is `exported="false"` — the system starts it as the publisher, so no export is needed (R5). No `<queries>` addition: the home-settings intent is guarded with `runCatching`, not `resolveActivity` (R11). Declining the pin dialog leaves the app fully functional (FR-014). | ✅ PASS | ✅ PASS |
| **IV. Platform-Idiomatic Android** | Kotlin + Compose + Material 3, no XML layouts. `isRequestPinShortcutSupported()` gates every pin attempt at the root (FR-028) *and* at the call site (FR-013) — the constitution names this API explicitly. `getLaunchIntentForPackage()` null handled at both call sites (FR-015, FR-018). Icon rasterization and bitmap treatment on `Dispatchers.IO` (FR-024). No service, no receiver, no polling, no wake lock — a pinned shortcut carries everything its launch needs, so battery cost at rest stays zero. The launch originates in the user's tap on the shortcut, started by the system on our behalf — never a background start. | ✅ PASS | ✅ PASS |
| **V. Stable Identifiers** | `packageName` is the shortcut ID, the intent extra, and the only value persisted anywhere (FR-025). The target's `ComponentName` is never persisted or matched. Labels are display-only and copied verbatim into the shortcut, never used as keys. Icons come from 001's cache, keyed `packageName + versionCode`. **One new hazard, guarded**: the persisted intent contains *SlowLock's own* activity FQN, so a rename would silently break every pinned shortcut — a unit test asserts the runtime FQN against a frozen constant, turning it into a build failure (R4, `contracts/pinned-shortcut.md`). | ✅ PASS | ✅ PASS |

**Technology Standards check**: fixed stack honoured (Kotlin/Compose/M3, single module, Java 11,
minSdk 33, targetSdk 37, `com.slowlock`). No dependency added, so `gradle/libs.versions.toml` and
`app/build.gradle.kts` are untouched by this feature. No backend, no network, no analytics, no
third-party SDK.

**Scope boundary check**: "pinned shortcut creation with mirrored icon and label" is the third of
the four items v1 covers. Delay duration, countdown, schedules, and per-app configuration storage
are explicitly out (spec, Out of Scope) and appear here only as the frozen seam FR-011 requires.

**Testing-expectations check — recorded waiver (2026-08-23)**: the constitution mandates an
instrumented test for "`DelayActivity` countdown and hand-off to the target app". Neither
`DelayActivity` nor any countdown exists in this draft, so the clause's named subject is absent.
`ShortcutLaunchActivity` is its ancestor and does perform the hand-off today, which is why this is
recorded as an **explicit waiver rather than a silent non-application**.

- **Waived**: instrumented coverage of the hand-off, for this feature only.
- **Covered instead by**: `ShortcutTargetTest` over the pure resolution path including the null
  `getLaunchIntentForPackage()` case (the constitution's other, unwaived unit-test requirement),
  `ShortcutContractTest`'s rename guard, and manual cases M2.3, M2.4 and M5.1–M5.4.
- **Why accepted here**: what an instrumented test would add over the manual pass is assertion of
  the outgoing intent, which needs `espresso-intents` — a new dependency, and the sole thing that
  would put a row in this plan's otherwise empty Complexity Tracking. For a draft whose launch
  behaviour the delay feature is expected to replace wholesale, that cost was judged not worth
  paying twice.
- **Returns in full** when the delay feature adds the countdown: that feature MUST carry the
  instrumented test for both the countdown and the hand-off, and MUST NOT inherit this waiver.

> **Amended by feature 003 (2026-08-24): this waiver is MOOT, not honoured — and the promise in
> the last bullet is superseded. Do not read it as an outstanding debt.**
>
> **Constitution v1.1.0 removed the clause this waiver was written against.** Instrumented tests
> are no longer merely unrequired; `src/androidTest`, `connectedAndroidTest`, Espresso and UI
> Automator are now **forbidden outright**, as is an agent driving a connected device. So feature
> 003 did not "carry the instrumented test for both the countdown and the hand-off" — it could
> not have, and a future reader must not record that as a missed obligation.
>
> A waiver excuses a requirement that exists. There is no requirement here to excuse: its subject
> was removed from the governing document. The distinction matters because the two readings leave
> very different residue — an honoured waiver would have discharged a debt, whereas a moot one
> means the debt never came due and nothing is owed by any later feature either.
>
> The `espresso-intents` cost that this waiver's third bullet weighed is now moot for the same
> reason, and the fourth bullet's premise turned out to be wrong twice over: the delay feature did
> not replace this screen's launch behaviour wholesale (it wrapped it — see
> `contracts/shortcut-config-screen.md`), and there was never a countdown to test. Feature 003's
> wait screen is deliberately without one.
>
> **What covers the hand-off now**: `WaitTimingTest` over the pure wait arithmetic,
> `ShortcutContractTest`'s unchanged rename guard, and the manual cases in
> `specs/003-launch-delay/manual-test-plan.md` — M5 for the hand-off and the six abandonment
> routes, M6 for durability. That plan is run by the maintainer, which is the constitution's
> intent as amended.

Schedule/time-window logic does not exist yet, so that clause is not engaged.

**Gate result**: PASS, both before research and after design. No deviations — Complexity Tracking
is empty, which is the first time that has been true for this project.

## Project Structure

### Documentation (this feature)

```text
specs/002-shortcut-pinning/
├── plan.md                          # This file (/speckit-plan output)
├── spec.md                          # Feature specification
├── research.md                      # Phase 0 output — R1–R12 decisions
├── data-model.md                    # Phase 1 output — IconTreatment, ShortcutTarget, ShortcutDraft, ShortcutSpec, PinSupport
├── quickstart.md                    # Phase 1 output — build, run, manifest/string changes
├── manual-test-plan.md              # Primary verification artifact — device and launcher matrix
├── contracts/                       # Phase 1 output
│   ├── pinned-shortcut.md           # FROZEN — ID, intent, extra, launch-activity obligations
│   └── shortcut-config-screen.md    # UI contract: signatures, C1–C16, root state, 001 changes
└── tasks.md                         # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml                         # MODIFIED: <activity> ShortcutLaunchActivity, exported=false,
│                                               #   excludeFromRecents, noHistory, taskAffinity="", transparent theme
├── res/values/
│   ├── strings.xml                             # MODIFIED: config screen, treatment names, unsupported screen
│   └── themes.xml                              # MODIFIED: Theme.SlowLock.Invisible for the launch activity
└── java/com/slowlock/
    ├── MainActivity.kt                         # MODIFIED: launchApp() removed; hosts SlowLockRoot
    ├── SlowLockRoot.kt                         # NEW — root state: Unknown / Unsupported / list / config
    ├── apps/                                   # feature 001 — AppListScreen.kt UNCHANGED (the seam working)
    │   └── AppIconCache.kt                     # MODIFIED: (packageName, versionCode) overload; keys unchanged
    └── shortcut/                               # NEW — this feature
        ├── ShortcutContract.kt                 # FROZEN constants + shortcutSpec() + ShortcutSpec
        ├── ShortcutLaunchActivity.kt           # FROZEN name. Invisible; resolves and starts the target
        ├── ShortcutPinner.kt                   # bakes the bitmap, updateShortcuts + requestPinShortcut
        ├── ShortcutTarget.kt                   # resolve(packageName) -> ShortcutTarget?, injectable seams
        ├── IconTreatment.kt                    # the three matrices; Compose + graphics conversions
        ├── PinSupport.kt                       # isRequestPinShortcutSupported() wrapper + state type
        ├── ShortcutConfigScreen.kt             # preview, treatment row, create, back
        └── PinUnsupportedScreen.kt             # explanation + home settings + re-check

app/src/test/java/com/slowlock/shortcut/
├── IconTreatmentTest.kt                        # matrix values; Original has no filter; alpha row identity; Gray = saturation 0
├── ShortcutContractTest.kt                     # FQN rename guard, frozen extra key, ID == package name
├── ShortcutTargetTest.kt                       # null getLaunchIntentForPackage() path (constitution MUST)
└── PinGateTest.kt                              # no pin attempted when support is false (FR-013)

# No androidTest suite — verification is manual, per manual-test-plan.md
# gradle/libs.versions.toml and app/build.gradle.kts UNCHANGED — no new dependencies
```

Feature 001 artifacts updated as part of this work (spec, Assumptions; detailed in
`contracts/shortcut-config-screen.md`): 001's `spec.md` FR-009/FR-018 annotated as superseded,
001's `manual-test-plan.md` T1.12/T1.16 re-written, and 001's `contracts/selection-handoff.md`
marked consumed.

**Structure Decision**: Single `:app` module, unchanged. New code is one flat
`com.slowlock.shortcut` package alongside `com.slowlock.apps`, following the same rule feature 001
set: group by feature, not by `data`/`domain`/`ui` layer, because eight files do not need a
hierarchy and layered packages are the abstraction the constitution counts as a deviation.
`SlowLockRoot.kt` sits at the top level rather than inside either feature package, because it is
what arbitrates between them and belongs to neither. The seams that matter are the ones that make
platform behaviour testable off-device — the frozen constants, the treatment matrices, and target
resolution behind injected lambdas — mirroring how 001 kept `resolveLaunchIntent` injectable.

## Phase Status

| Phase | Output | Status |
|---|---|---|
| Phase 0 — Research | `research.md` (R1–R12) | ✅ Complete, no `NEEDS CLARIFICATION` remaining |
| Phase 1 — Design & Contracts | `data-model.md`, `contracts/`, `quickstart.md`, `manual-test-plan.md`, agent context | ✅ Complete |
| Phase 2 — Tasks | `tasks.md` | ⏭ Not started — run `/speckit-tasks` |

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations.** No new dependency, no new module, no new permission, no new persistence, no
abstraction layer. Nothing to justify.

Refusals worth recording, since each was a live option: `androidx.navigation:navigation-compose`
for the two-screen flow (R9 — `rememberSaveableStateHolder`, already on the classpath, covers the
one thing that actually needed solving); `ShortcutManagerCompat` (R1 — buys nothing above
`minSdk 33`); a `ShortcutConfigViewModel` for symmetry with feature 001 (R10 — more machinery than
the state it would protect); Robolectric to test the shortcut APIs (R12 — a dependency to fake the
very platform behaviour that is in question); and a stored map of pinned apps (data-model.md —
forbidden by FR-027, and stale the moment the user deletes a shortcut).
