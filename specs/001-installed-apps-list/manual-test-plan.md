# Manual Test Plan: Installed Applications List

**Feature**: `001-installed-apps-list` | **Date**: 2026-08-23

This feature is verified primarily by hand. Automated coverage is deliberately limited to
logic that **cannot be seen on screen** — see §5 for what that is and why.

Cases are tiered so a short session still covers the things that actually break.

---

## 1. Device preparation

You need one device (or emulator) set up as follows. Several cases are impossible to run
without this prep, so do it once and keep the device around.

| Prep | Why |
|---|---|
| **100+ launchable apps installed** | SC-001 and SC-003 are meaningless on a device with 20 apps |
| **One app whose name starts lowercase** (eBay, iA Writer) | Catches case-sensitive sorting — the single most likely sorting bug |
| **One app with an accented or non-Latin name** | Catches collation done with `lowercase()` instead of `Collator` |
| **One very long app name** (many OEM apps qualify: "Samsung Members", "Device Health Services") | Row layout truncation |
| **A second locale you can read installed** (German is ideal — umlaut ordering is visibly wrong if collation is broken) | Locale-change cases |
| **One disposable app you can uninstall** (any free app) | The uninstall cases |
| **Developer Options enabled**, USB debugging on | Needed for the timing/jank measurements |

Also note the device's own app drawer — you will compare against it.

---

## 2. Tier 1 — Must pass

If any of these fail, the feature is not done. ~20 minutes.

### A. The list is correct

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T1.1** | Open SlowLock from a cold start | List of apps appears, each with icon and name. No crash, no blank screen | FR-001, FR-002 |
| **T1.2** | Open your launcher's app drawer side by side. Spot-check 10 apps from the drawer | All 10 appear in SlowLock. Scroll SlowLock end to end — nothing appears that isn't in the drawer | **SC-002**, FR-001 |
| **T1.3** | Search the list for "SlowLock" | Not present | FR-003 |
| **T1.4** | Scroll the whole list looking for the same app name twice | No app appears twice. Pay attention to **Settings**, **Contacts**, and any OEM suite — these commonly expose several launcher entries and are where duplicates show up | FR-004 |
| **T1.5** | Find your lowercase-named app (eBay) | Sorted under **E**, between other E apps — *not* after Z, *not* in a separate lowercase block | FR-005 |
| **T1.6** | Compare 5 rows' icons against the app drawer | Icons match the real apps. No wrong icon on any row | FR-002 |

> **T1.4 and T1.5 are the two highest-value cases in this document.** Both fail silently —
> the screen looks completely normal — and both are the exact failure modes Constitution V
> and FR-004/FR-005 exist to prevent.

### B. Search

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T1.7** | Type part of an app name | List narrows as you type | FR-007 |
| **T1.8** | Type the **middle** of a name, not the start (e.g. "tagram" for Instagram) | Instagram still matches | FR-007 (substring, not prefix) |
| **T1.9** | Type the name in the wrong case ("INSTA", "insta") | Matches either way | FR-007 |
| **T1.10** | Clear the query | Full list returns, same alphabetical order as before | FR-008 |
| **T1.11** | Type nonsense ("zzzqqq") | A message saying nothing matches — **not** a blank white area | FR-006 |

### C. Opening the selected app

> This group is the **feasibility proof**. If T1.12 fails, the product idea does not work —
> everything downstream (configuration, shortcuts, countdown) is built on this one
> mechanism.
>
> **Revised by `002-shortcut-pinning`.** A row tap now opens the shortcut configuration
> screen instead of launching the target (002 FR-001), so T1.12 and T1.16 below are
> re-written against that behaviour. The launch mechanism this group was written to prove is
> still proved — it moved to the pinned shortcut, and 002's `manual-test-plan.md` M2 and M5
> are where it is now exercised, including after a force-stop and a reboot.

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T1.12** | Tap any row | **The shortcut configuration screen opens**, previewing the app you actually tapped — not a neighbouring row. It no longer launches the target (002 FR-001, superseding FR-009) | FR-010, 002 FR-001 |
| **T1.13** | Press back / home from the opened app to return to SlowLock | You land back on the list, scroll position and query intact | FR-017 |
| **T1.16** | Scroll well down the list, type a query, tap a row, then leave the configuration screen via back | You land back on the list with **scroll position and query intact** — the configuration round trip loses nothing (002 FR-022, superseding FR-018's immediacy clause) | FR-017, 002 FR-022 |
| **T1.17** | Tap a row, then open the recents/app switcher | SlowLock and the target app are **separate entries**, not the target stacked inside SlowLock's task (this is `FLAG_ACTIVITY_NEW_TASK` doing its job — and it is the same flag the future countdown screen will need) | FR-009 |
| **T1.18** | Repeat T1.12 for five different apps, including a preinstalled one (Settings, Camera) and a third-party one | Every one opens the correct app | FR-009, FR-010 |

### D. Permissions — the non-negotiable one

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T1.14** | Uninstall SlowLock, reinstall, open it | **Zero permission dialogs.** Not one, at any point | **SC-006**, FR-015 |
| **T1.15** | Settings → Apps → SlowLock → Permissions | "No permissions requested" | FR-015, Constitution III |

---

## 3. Tier 2 — Should pass

Edge cases and performance. ~25 minutes, needs the adb commands.

### E. Performance

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T2.1** | `adb shell am force-stop com.slowlock` then:<br>`adb shell am start -W -n com.slowlock/.MainActivity`<br>Read the **TotalTime** line | Under 1000 ms on a device with ~150 apps | **SC-001** |
| **T2.2** | Developer Options → **Profile GPU Rendering** → *On screen as bars*. Fling the list top to bottom several times | Bars stay under the green line. No blank rows appearing, no icon "popping" into the wrong row | **SC-003** |
| **T2.3** | Clear only the icon cache:<br>`adb shell run-as com.slowlock rm -rf cache/app-icons`<br>Measure with T2.1's command. Then `force-stop` and measure again **without** clearing | Second measurement at least 2× faster than the first | **SC-005**, FR-012 |
| **T2.4** | Fling the list hard during the first second, while it is still loading | No stutter, no ANR, no crash | FR-011 |

### F. State and refresh

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T2.5** | Scroll to the middle, type a query, **rotate the device** | Position and query survive. Critically: **no flash of a loading spinner** and no jump back to the top | FR-017 |
| **T2.6** | With SlowLock backgrounded, install any app. Return to SlowLock | New app is in the list | FR-013 |
| **T2.7** | With SlowLock backgrounded, uninstall your disposable app. Return to SlowLock | App is gone from the list | FR-013 |
| **T2.8** | Uninstall the disposable app **from another device screen while SlowLock is still in the foreground**, then tap its (now stale) row | Message saying the app is no longer available. **No crash**, and no attempt to launch anything. You stay on the list | **FR-014** |

> **T2.8 is the crash case.** It is the one manual case where a failure is a hard crash
> rather than a cosmetic issue, so do not skip it. It pairs with the one unit test in §5.
> Note that the launch path has *two* guards for a reason: the ViewModel's null check
> catches the stale row, and the `runCatching` around `startActivity` catches an app
> uninstalled in the window between resolving and starting. T2.8 exercises the first; the
> second is not reproducible by hand.

### G. Presentation edge cases

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T2.9** | Find your very-long-named app | Name truncates to **one line** with an ellipsis. Row height matches every other row; layout is not pushed out of shape | Spec edge case |
| **T2.10** | Look for two apps sharing a display name (common with dual messengers, or "Photos") | Both rows appear, and **each opens its own app** — this is the case where matching on label instead of package name would silently open the wrong one | Spec edge case, FR-010 |
| **T2.11** | Watch closely while fast-scrolling a region you have not visited | Rows show a neutral placeholder icon briefly, then the real icon. Row never changes height, list never jumps | FR-016, SC-003 |

### H. Locale

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T2.12** | Switch the device language to German. Reopen SlowLock | App names are in German where the app provides them. **Ordering follows German rules** — umlaut names sort with their base letter, not dumped at the end | FR-005, spec edge case |
| **T2.13** | Switch back to English. Reopen | Names and ordering revert | Spec edge case |

---

## 4. Tier 3 — Do once, before release

| # | Steps | Expected | Covers |
|---|---|---|---|
| **T3.1** | Run Tier 1 on a **non-Pixel OEM device** (Samsung/Xiaomi) | Same results. OEM devices have the most apps with multiple launcher activities, so T1.4 matters most here | Constitution, manual verification |
| **T3.2** | If you have Xiaomi Dual Apps or Samsung Secure Folder, note whether cloned apps appear | **Record the result either way** — the constitution requires dual-app behaviour to be recorded as tested or explicitly untested. Spec says clones are out of scope, so "clones do not appear" is a pass | Constitution, spec assumption |
| **T3.3** | Install on a stripped-down emulator image with almost no apps | Explanatory empty state, not a blank screen. *(Skip if you have no such image — this state is nearly unreachable on real hardware)* | FR-006 |


### Recorded results

| Field | Value |
|---|---|
| Date | 2026-08-23 |
| Device | OnePlus 8 (OxygenOS) — non-Pixel OEM |
| T3.1 — Tier 1 re-run on OEM hardware | Executed, reported passing |
| T3.2 — cloned apps (Xiaomi Dual Apps / Samsung Secure Folder) | **Explicitly untested** — the Xiaomi and Samsung features do not exist on OxygenOS, and no clone was set up via the OnePlus equivalent (Parallel Apps), so cloned-app behaviour was never exercised |
| T3.3 — Stripped-down image, empty state | Not run (optional; the plan permits skipping when no such image is available) |

T3.2 is recorded as *untested*, not as a pass. That distinction is the point of the
constitution's manual verification requirement: dual-app behaviour must be recorded either
way rather than assumed. Cloned apps are out of scope per `spec.md` Assumptions, so nothing
in this feature depends on the answer — but OxygenOS does ship an app-cloning feature of its
own (Parallel Apps), so this case is re-runnable on the same OnePlus 8 by cloning one app
and reopening the list. Worth doing before release.

---

## 5. What stays automated, and why

Six assertions in two JVM unit test files (no device, runs in `./gradlew test`). Each one
is here because **manual testing cannot reliably catch it** — not for coverage's sake.

| Assertion | Why manual testing does not catch it |
|---|---|
| `dedupeByPackage` keeps one entry per package | Only reproduces on devices with multi-launcher-activity apps. Your device may have none today and three after an OEM update |
| `excludeSelf` removes SlowLock | Trivially checked by eye — **but** it is one line next to the dedupe logic and free to assert while you are there |
| `sortedByLabel` uses collation, not `lowercase()` | Requires a German device *and* an umlaut-named app to see. The test asserts it in 5 ms with no device at all |
| Filter is case-insensitive substring, blank query returns all, order preserved | Cheap to assert; the "order preserved after clearing" case is easy to miss by eye |
| `iconCacheKey` changes when `versionCode` changes | **Requires waiting for a real app update to observe.** Untestable by hand in any practical session, and the failure is a silently stale icon |
| A null launch intent produces no launch attempt | Requires the race in T2.8 to reproduce. The unit test makes it deterministic — and Constitution §"Testing expectations" requires this one as a unit test |

Everything else — every loading spinner, empty state, snackbar, and row layout — is
verified by the tables above. No Compose UI tests, no `connectedAndroidTest`.

---

## 6. Five-minute smoke test

For re-checking after a small change:

**T1.1** (list appears) → **T1.4** (no duplicates) → **T1.5** (lowercase sorts correctly) →
**T1.7** (search narrows) → **T1.10** (clear restores) → **T1.12** (tap opens the config screen)
→ **T1.13** (return, nothing lost) → **T2.5** (rotate, nothing lost)
