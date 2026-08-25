---
description: "Task list for 008-hero-device-frame"
---

# Tasks: Hero Device Frame Screenshot

**Input**: Design documents from `/specs/008-hero-device-frame/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/hero-image-asset.md](./contracts/hero-image-asset.md),
[quickstart.md](./quickstart.md)

**Tests**: No automated test tasks. The spec requests none, the project has no test framework for
the site, and the constitution forbids adding an instrumented suite. Verification is the scripted
headless-Chrome geometry check (research R8) plus the maintainer's own
[manual-test-plan.md](./manual-test-plan.md).

**Organization**: Tasks are grouped by user story. Because this feature is two edits to one file
plus one new asset, the stories share more machinery than usual — the asset is Foundational, and
Stories 2 and 3 are largely verification of what Story 1 produces. That is stated plainly rather
than padded into artificial independence.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files or read-only, no dependency on an incomplete task)
- **[Story]**: US1, US2, US3 — maps to the user stories in [spec.md](./spec.md)

## Path Conventions

Static site at `site/` in the repository root, published from that folder by GitHub Actions. No
build step. Nothing under `app/` is touched by any task in this list.

---

## Phase 1: Setup

**Purpose**: Confirm the local tools exist and capture the "before" the desktop regression check
will be measured against.

- [X] T001 [P] Confirm the three one-off tools are present: `python3 -c "import PIL"` (Pillow 10.4), `which cwebp`, and `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`. None becomes a project dependency; record versions in the task notes.
- [X] T002 [P] Capture the current, unmodified `site/index.html` in headless Chrome at 320, 390 and 1440px into the scratchpad as `baseline-{320,390,1440}.png`. These are the "before" images T018 compares against, and at 390px the 320/390 shots are the recorded evidence of the bug being fixed.
- [ ] T003 Ask the maintainer for the original device screenshot PNG behind `photo_2026-08-25_19-50-41.jpg` (research R1, quickstart step 0), and record the answer in [research.md](./research.md) R1. **Non-blocking** — do not wait; proceed with the supplied file and let T017 upgrade the asset if the original arrives. Do not capture it from a device: the constitution reserves the device for the maintainer.

**Checkpoint**: Tools confirmed, baseline captured, the resolution question is with the maintainer.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Produce the one new asset. Every user story needs something inside the frame, so this
blocks all three.

**⚠️ CRITICAL**: No user story work can begin until `site/assets/hero-wait-screen.webp` exists and
passes T007.

- [X] T004 Crop `photo_2026-08-25_19-50-41.jpg` to the box `(0, 58) – (576, 1252)` with Pillow, writing `576×1194` to the scratchpad as `hero-crop.png` (research R4, quickstart step 1). If the higher-resolution original arrived via T003, scale the crop box by the same factor and keep the ratio at 0.4824.
- [X] T005 Verify the crop in the scratchpad `hero-crop.png` against data-model V3 and contract C6: row 0 and the last row are the app canvas `#F3F0E9` (±2 per channel), no control is clipped at either edge, and neither the device status bar (clock, NFC, alarm, Wi-Fi, signal, battery) nor the gesture pill survives. Fails → adjust the crop box in T004 and re-run.
- [X] T006 Encode the crop to `site/assets/hero-wait-screen.webp` with `cwebp -q 80 -quiet`. Do not pass `-metadata all` (contract C7). Expect ~15 KB from the 576px source, ~21 KB from a 784px one.
- [X] T007 Verify `site/assets/hero-wait-screen.webp` against [contracts/hero-image-asset.md](./contracts/hero-image-asset.md) §2: WebP without alpha, intrinsic size matches the crop, ratio `0.4824 ± 0.005` (data-model V1), file ≤ 30 KB, no EXIF, and the filename lowercase with no space (contract C3). Record the actual bytes and dimensions — T009 needs the exact pixel values.

**Checkpoint**: The asset exists and satisfies its contract. User stories can begin.

---

## Phase 3: User Story 1 — A visitor on a phone sees the app screen intact (Priority: P1) 🎯 MVP

**Goal**: Kill the collapse. The hero picture keeps a phone's proportions at every width, because
the frame no longer has a height of its own to distribute.

**Independent Test**: Open `site/index.html` at 320px and 390px. The app screen is whole and
correctly spaced, the page does not scroll sideways, and the frame's height-to-width ratio sits
between 1.9 and 2.3.

- [X] T008 [US1] Delete the hand-built screen from `site/index.html` — everything between the status row and the gesture row, currently lines 265–298: both `flex:1` boxes, the drawn app chip, the `104px` "10", the drawn slider with its `1s`/`30s` labels, and the drawn "Choose the icon" button. Leave the 40px status row and the 26px gesture row exactly as they are.
- [X] T009 [US1] Insert the single `<img>` in `site/index.html` where the drawing was, exactly per [contracts/hero-image-asset.md](./contracts/hero-image-asset.md) §3: `src="assets/hero-wait-screen.webp"` (relative — contract C1), `width`/`height` set to the real intrinsic pixels recorded in T007 (contract C8, data-model V2), the `alt` text from research R7 (contract C9), `decoding="async"`, `fetchpriority="high"`, and `style="display:block;width:100%;height:auto;background:#F3F0EA"` (contracts C11, data-model V8). Do **not** add `loading="lazy"` (contract C10).
- [X] T010 [P] [US1] In `site/index.html:196`, change the desktop rule `.phone { width: 412px; height: 820px; }` to `.phone { width: 412px; }`. Add no replacement height and no `aspect-ratio` (contract C13, data-model V6).
- [X] T011 [P] [US1] In `site/index.html:213`, change the ≤860px rule to `.phone { width: 100%; max-width: 412px; justify-self: center; }` — dropping `height: auto`, which is now redundant and misleading (contract C14).
- [X] T012 [US1] Grep `site/index.html` to prove the root cause is gone: no `flex:1` and no wrapper carrying it anywhere inside `.phone` (data-model V5, contract C12), and no `height` or `aspect-ratio` declared on `.phone` at any breakpoint (V6). This is the assertion that makes the bug unrepeatable.
- [X] T013 [US1] Run the headless-Chrome geometry check from quickstart step 5 against `site/index.html` at 320, 390 and 1440px. Assert at each width that the `.phone` element's `offsetHeight / offsetWidth` lands in 1.9–2.3 (SC-001, data-model V7) and that `document.documentElement.scrollWidth <= window.innerWidth` (FR-004). Expected: ≈2.23 at 320, ≈2.20 at 390, ≈2.18 at 1440.

**Checkpoint**: The reported defect is fixed and measurable. This alone is a shippable increment —
the page is correct at every width, showing the real app screen.

---

## Phase 4: User Story 2 — The picture shows the real app (Priority: P2)

**Goal**: What the hero shows is a capture of the app, complete and truthful, and carries nothing
that belongs to the maintainer's phone.

**Independent Test**: Hold the hero picture beside the app's own *Wait before opening* screen; they
show the same thing, including the preset row the old drawing lacked.

- [X] T014 [US2] Verify the rendered hero in `site/index.html` shows all six items of the "Depicted content" list in [data-model.md](./data-model.md): title with `2 / 3`, the Messages chip, the `10`/`SECONDS` readout, the slider with `1s`/`30s`, **the `5s`/`10s`/`30s` preset row with `10s` selected**, and the `Choose the icon` button. The preset row is the specific drift research R2 found in the old drawing — its presence is the evidence FR-001 and SC-006 are satisfied.
- [X] T015 [US2] Verify the published `site/assets/hero-wait-screen.webp` carries none of data-model's "Excluded content" (FR-010): no clock, battery percentage, NFC, alarm, Wi-Fi or signal glyphs, no gesture pill, no real contact name or notification. Confirm the seam where the page-drawn chrome meets the image shows no colour step (research R4, manual case M11).
- [X] T016 [P] [US2] Confirm the `alt` attribute in `site/index.html` reads verbatim: "SlowLock's wait screen: the Messages app selected, a 10-second wait, and a slider from 1 to 30 seconds." (research R7, contract C9). It must describe what the app is doing, not the furniture.
- [X] T017 [US2] Resolve the resolution question from T003. **If the original PNG arrived**: redo T004–T007 at 784px wide, overwrite `site/assets/hero-wait-screen.webp`, and update only the two size attributes in `site/index.html` (data-model V4 — nothing else in the markup may need to change; if it does, the contract has been broken). **If it did not**: leave the 576px asset in place and record the accepted 1.47× limitation in [manual-test-plan.md](./manual-test-plan.md) M08 so the maintainer judges the softness rather than being surprised by it.

**Checkpoint**: The hero is a faithful, private, sharp-as-available picture of the app.

---

## Phase 5: User Story 3 — The desktop hero is unharmed (Priority: P3)

**Goal**: The change costs the desktop layout nothing beyond the one height difference the plan
already tracked and the maintainer signs off on.

**Independent Test**: Put the 1440px render beside the T002 baseline. Everything but the phone's
height is identical.

- [X] T018 [US3] Compare a fresh 1440px headless render of `site/index.html` against the T002 `baseline-1440.png`: the `1fr 412px` grid column, the 560px text column, the 72px gap, the eyebrow, headline, paragraph, Play button and the "Free · no accounts · no analytics" line are all unmoved, and the phone occupies the same column at the same 412px width (FR-005).
- [X] T019 [US3] Measure the `.phone` element in the 1440px render and confirm it is 412 × ≈899px. Record the +79px against the old 820px in [manual-test-plan.md](./manual-test-plan.md) M07 as the tracked deviation from SC-005, with the one-line alternative (hold 820px, frame narrows to 374px) noted so the maintainer can choose. **Do not pick for them.**
- [X] T020 [P] [US3] Confirm the ultrawide guard still holds in `site/index.html`: past 1600px the `.page` cap stops the hero, and the phone, from widening further (SC-001, manual case M09).
- [X] T021 [P] [US3] Confirm the change stayed in its lane: `site/privacy.html`, `site/assets/og.png`, `site/assets/icon.svg` and `site/fonts/` are byte-identical, and within `site/index.html` the header, the "How it works", "Why it exists" and open-source sections and the footer are untouched (spec Assumptions — scope; manual case M16).

**Checkpoint**: All three stories are satisfied. The page is correct everywhere and nothing else moved.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T022 [P] Verify the degraded path against `site/index.html`: with images blocked, the frame keeps its full height, the hero does not reflow, and the alt text stands in (FR-006, FR-007, manual cases M13/M05).
- [X] T023 [P] Verify `site/index.html` still makes no third-party request and contains no JavaScript: grep the hero block for any absolute `http`/`https` asset URL and for `<script>` (contract C2, 006 FR-026/FR-025).
- [X] T024 Run `./gradlew assembleDebug` and `./gradlew test` from the repository root. No file under `app/` changed, so both must pass unchanged — this is insurance that nothing under `site/` leaked into the build (constitution Build gate, quickstart step 8).
- [X] T025 [P] Update [research.md](./research.md) R1 and the plan's Performance Goals with which source actually shipped and the asset's real byte size, so the documents match the artifact.
- [X] T026 Hand [manual-test-plan.md](./manual-test-plan.md) to the maintainer with the cases only they can run marked: M01–M06, M08, M10, M12, M14 on their own phone, and M17 once the page is live. Do not run these; do not simulate them on a device.
- [X] T027 Present the quickstart step 7 decision about `photo_2026-08-25_19-50-41.jpg` — currently staged at the repository root — with the three options and their effects. The maintainer decides; the agent does not `git rm`, `git mv`, or unstage it.

---

## Phase 7: Hand-off

- [X] T028 Report the changed files and **stop**: modified `site/index.html`, new and untracked `site/assets/hero-wait-screen.webp`, still-staged and undecided `photo_2026-08-25_19-50-41.jpg`, plus the spec-directory documents. **Do not commit, push, branch, tag, merge or rebase.** The constitution (Development Workflow & Quality Gates, v1.2.0) reserves every one of those for the maintainer, and a task in this file saying so would not authorize it. Offer the commit; leave it unmade.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T003 is an ask, not a gate — it must not stall Phase 2.
- **Foundational (Phase 2)**: needs T001. **Blocks all three user stories** — every story needs the asset in the frame.
- **User Story 1 (Phase 3)**: needs Phase 2. Delivers the fix on its own.
- **User Story 2 (Phase 4)**: needs Phase 2 for the asset and Phase 3 for the markup that displays it. Its tasks verify and, in T017, upgrade.
- **User Story 3 (Phase 5)**: needs Phase 3, and T018 additionally needs T002's baseline from Phase 1.
- **Polish (Phase 6)**: needs Phases 3–5.
- **Hand-off (Phase 7)**: last. Nothing follows it.

### Honest note on story independence

The template's usual promise — that each story is a separable slice — only half holds here. **US1 is
a genuine independent increment**: complete Phases 1–3 and the reported bug is fixed and shippable.
US2 and US3 are verification-and-refinement layers over the same two edits, not separable features.
They are kept as phases because their acceptance criteria are genuinely distinct (fidelity and
privacy; desktop regression), not because they could be built by different people in parallel.

### Within User Story 1

T008 → T009 (the `<img>` replaces what T008 removed) → T012 → T013.
T010 and T011 are independent of both and of each other.

### Parallel Opportunities

- **Phase 1**: T001 and T002 together. T003 is a message, sent and then set aside.
- **Phase 2**: strictly sequential — T004 → T005 → T006 → T007. Each consumes the previous output.
- **Phase 3**: T010 and T011 in parallel (different lines, different breakpoints) while T008/T009 proceed; T012 and T013 must follow all four.
- **Phase 4**: T016 in parallel with T014/T015. T017 is last — it can invalidate T014's evidence.
- **Phase 5**: T020 and T021 in parallel with T018/T019.
- **Phase 6**: T022, T023 and T025 in parallel. T024 independent of all of them.

---

## Parallel Example: Phase 3 (User Story 1)

```bash
# The two CSS deletions touch different rules at different breakpoints — run together:
Task: "Remove height:820px from the .phone desktop rule at site/index.html:196"
Task: "Remove height:auto from the .phone <=860px rule at site/index.html:213"

# Meanwhile, the markup swap is one sequential thread:
#   T008 delete lines 265-298  ->  T009 insert the <img>
# Then, after all four land:
#   T012 grep-assert no flex:1 / no height  ->  T013 headless geometry check
```

---

## Implementation Strategy

### MVP (User Story 1 only)

1. Phase 1 — Setup (T001–T003)
2. Phase 2 — Foundational (T004–T007) **blocks everything**
3. Phase 3 — User Story 1 (T008–T013)
4. **STOP and VALIDATE**: T013 passes at 320/390/1440; the reported bug is fixed and the page is
   shippable as it stands.

### Incremental delivery

- **+ US2 (T014–T017)** buys fidelity and privacy: the preset row proves the drift is corrected, and
  T015 keeps the maintainer's clock and battery out of the published file. T017 is the only task
  that can be blocked by an outside answer, and it degrades gracefully into a recorded limitation.
- **+ US3 (T018–T021)** buys the desktop regression proof and puts the +79px height decision in
  front of the maintainer rather than in the diff.
- **+ Polish (T022–T027)** covers the degraded path, the build gate, and the two decisions that are
  the maintainer's alone.

### The one thing not to get wrong

T012 is the task that makes this fix permanent. Deleting the drawing without deleting the `flex:1`
boxes and the fixed heights leaves the mechanism that caused the collapse sitting in the file,
waiting for the next person who adds something to the frame.

---

## Task Summary

| Phase | Tasks | Count |
|---|---|---|
| 1 — Setup | T001–T003 | 3 |
| 2 — Foundational | T004–T007 | 4 |
| 3 — US1 (P1, MVP) | T008–T013 | 6 |
| 4 — US2 (P2) | T014–T017 | 4 |
| 5 — US3 (P3) | T018–T021 | 4 |
| 6 — Polish | T022–T027 | 6 |
| 7 — Hand-off | T028 | 1 |
| **Total** | | **28** |

---

## Execution Record (2026-08-25)

27 of 28 tasks complete. **T003 is left open**: the ask for the original device screenshot was
raised with the maintainer and has not been answered. It is non-blocking by design — T017 took its
fallback branch, the 576px asset shipped, and the 1.47x limitation is recorded in
[research.md](./research.md) R1 and manual case M08.

**Measured before/after** (headless Chrome, `.phone` bounding box, iframe rig at true CSS widths):

| viewport | before | ratio | after | ratio |
|---|---|---|---|---|
| 320 | 280x534 | 1.907 | 280x625 | 2.232 |
| 390 | 350x507 | **1.449** | 350x770 | 2.200 |
| 412 | 372x495 | **1.331** | 372x816 | 2.193 |
| 600 | 412x495 | **1.201** | 412x899 | 2.181 |
| 860 | 412x495 | **1.201** | 412x899 | 2.181 |
| 861 | 412x820 | 1.990 | 412x899 | 2.181 |
| 1440 | 412x820 | 1.990 | 412x899 | 2.181 |
| 1920 | 412x820 | 1.990 | 412x899 | 2.181 |

No horizontal overflow at any width, before or after. The collapse was worst between 600px and
860px, where the frame was 40% shorter than its proper height.

**Left for the maintainer**: T003's answer, the M07 height decision, the fate of
`photo_2026-08-25_19-50-41.jpg` (T027), the manual cases in
[manual-test-plan.md](./manual-test-plan.md) (T026), and the commit itself (T028).

