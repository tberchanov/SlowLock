# Phase 1 Data Model: Public Site & Privacy Policy

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

There is no database, no persisted state and nothing a visitor can submit. The "model" here is the
set of content artifacts the feature produces, what each must contain, and the one relationship
that can go wrong: a claim on a page drifting away from the app source that makes it true.

---

## Entity: Landing page

**File**: `site/index.html` · **Address**: `https://tberchanov.github.io/SlowLock/`

| Field | Value | Requirement |
|---|---|---|
| Title | `SlowLock — A pause between you and the app` | FR-008 |
| Description | One sentence: pick an app, choose a wait, tap the shortcut | FR-008 |
| Favicon | `assets/icon.svg` | FR-008 |
| Preview image | `assets/og.png`, 1200×630 (omit tag if unavailable) | FR-008, research R6 |
| Sections | header · hero + phone mockup · How it works (3 steps) · Why · Open source · footer | FR-018 |
| Outbound links | repository, Google Play (placeholder), privacy policy | FR-020, FR-021, FR-005 |
| Anchors | `#how`, `#why` | — |

**Validation rules**
- Every behavioural claim must match the app source (FR-019). Currently asserted: 1–30 second wait;
  three icon treatments; no accounts; no analytics; no network calls; three-step setup.
- The "Open source, all of it" claim is valid only while `LICENSE` exists (FR-022).
- No element may appear clickable and do nothing (FR-009) — this is what the two dead
  `style-hover` attributes violate today.

---

## Entity: Privacy policy page

**File**: `site/privacy.html` · **Address**: `https://tberchanov.github.io/SlowLock/privacy.html`

This is the artifact Google Play requires. Its address is a **contract** — once submitted to Play
it must not move (`contracts/published-urls.md`).

| Field | Value | Requirement |
|---|---|---|
| Title | `SlowLock — Privacy Policy` | FR-008 |
| Headline | "SlowLock collects nothing." | — |
| Last updated | A date, revised whenever the text changes | FR-014 |
| Section: what it does with your data | installed-app list read on device; shortcuts stored on device | FR-010 |
| Section: what it does not do | 5 numbered items — no network, no analytics/crash/ads/SDKs, no accounts, no usage record, nothing sold or shared | FR-011 |
| Section: deleting your data | uninstall or clear storage; no copy exists elsewhere | FR-012 |
| Section: children | collects nothing from anyone | — |
| Section: verifying this yourself | link to the repository | FR-016 |
| Section: changes to this policy | page updated, date changed | FR-014 |
| Section: contact | `tberchanov@gmail.com` | FR-013 |
| Draft banner | **must not exist** | FR-015 |

**Validation rules**
- Every claim traceable to app source at publish time (FR-016) — see the table below.
- Must not contradict the Play Data safety form (FR-017).
- Must render in full with scripts disabled (FR-006) — this is a review-tool and crawler concern,
  not a nicety.

---

## Relationship: page claim → app source

The only real integrity constraint in this feature. Each row was verified in this repository on
2026-08-25; each must be re-verified before publishing (FR-016) and whenever the app changes.

| Claim on the page | Source of truth | Verified state |
|---|---|---|
| No network requests | `app/src/main/AndroidManifest.xml` | No `INTERNET` permission declared |
| No analytics / crash / ads / third-party SDK | `gradle/libs.versions.toml`, `app/build.gradle.kts` | Compose + AndroidX only |
| Reads the installed-app list | `AndroidManifest.xml` `<queries>` | `ACTION_MAIN` + `CATEGORY_LAUNCHER` only; no `QUERY_ALL_PACKAGES` |
| Stored on the device only | `locks/LockStore.kt`, `delay/DelayConfigStore.kt`, `apps/AppIconCache.kt` | `SharedPreferences` + local icon cache |
| Uninstall removes everything | `AndroidManifest.xml` | `allowBackup="false"` + `dataExtractionRules` |
| One to thirty seconds | `delay/DelayRange.kt` | `MIN_SECONDS = 1`, `MAX_SECONDS = 30` |
| Own, gray, or inverted icon | `shortcut/IconTreatment.kt` | `Original`, `Invert`, `Gray` |

**Note — the policy currently understates the app.** `allowBackup="false"` *and*
`dataExtractionRules` together mean the data does not leave the device by cloud backup or by
device-to-device transfer either. Adding a sentence is optional (spec FR-010 is already satisfied);
if added it must remain traceable to the manifest like every other claim.

---

## Entity: Site source directory

**Path**: `site/` — kept separate from application source (FR-024), published in its entirety and
exclusively (FR-025).

| Member | Role | Note |
|---|---|---|
| `index.html` | Landing page | Name is what makes the bare address work (FR-003) |
| `privacy.html` | Policy page | No spaces in the filename (FR-004) |
| `.nojekyll` | Empty marker | Skips Jekyll processing |
| `fonts/*.woff2` | Self-hosted faces | Extracted from the bundles (research R3) |
| `assets/icon.svg`, `assets/og.png` | Favicon, preview image | research R6 |
| ~~`SlowLock Site.html`~~, ~~`SlowLock Privacy Policy.html`~~ | Bundle exports | **Deleted** once output is verified |
| ~~`PUBLISHING.md`~~ | Working note | **Deleted** — would otherwise be served publicly (research R9) |

---

## Entity: Published site

What a visitor receives. Derived from `site/` by the workflow; never edited directly.

| Property | Value | Requirement |
|---|---|---|
| Base address | `https://tberchanov.github.io/SlowLock/` | FR-001 |
| Cost | Free (public repository) | SC-009 |
| Update path | Push to `main` touching `site/**` → live within 5 minutes | FR-023 |
| Scripts required to read content | None | FR-006 |
| Third-party requests at view time | None | FR-026 |
| Contents | Exactly the files under `site/` | FR-025 |

**State transitions**

```text
bundle export ──unpack──▶ plain HTML ──fix links/meta/responsive──▶ verified locally
                                                                          │
                                                            push to main (site/**)
                                                                          ▼
                                                            workflow builds + deploys
                                                                          ▼
                                                       live ──▶ verified live ──▶ URL to Play
```

---

## Entity: Repository licence

**File**: `LICENSE` (repository root) — MIT, year 2026.

Not site content, but the landing page's "free" and "open source" claims depend on it (FR-022), so
it is part of this feature. Copyright holder line: see research R7 — defaults to `tberchanov`
pending the maintainer's preferred name.
