# Quickstart: Visual Redesign (Phase 1)

**Feature**: `004-visual-redesign` | **Plan**: [plan.md](./plan.md)

How to get the assets in, build, and know it worked.

---

## 1. Vendor the fonts (step 1 of nine)

Both families are SIL OFL 1.1. **Google Fonts cannot be the source** — it publishes only variable
builds of both (research R1). The statics come from the upstream project repositories.

```sh
FONT_DIR=app/src/main/res/font
LIC_DIR=app/src/main/assets/licenses      # NOT res/font — see below
mkdir -p "$FONT_DIR" "$LIC_DIR"

# Instrument Sans — three weights, from the upstream repo
BASE=https://raw.githubusercontent.com/Instrument/instrument-sans/master   # default branch is master, not main
curl -sSL "$BASE/fonts/ttf/InstrumentSans-Regular.ttf"  -o "$FONT_DIR/instrument_sans_regular.ttf"
curl -sSL "$BASE/fonts/ttf/InstrumentSans-Medium.ttf"   -o "$FONT_DIR/instrument_sans_medium.ttf"
curl -sSL "$BASE/fonts/ttf/InstrumentSans-SemiBold.ttf" -o "$FONT_DIR/instrument_sans_semibold.ttf"
curl -sSL "$BASE/OFL.txt"                               -o "$LIC_DIR/OFL-InstrumentSans.txt"

# JetBrains Mono — two weights, from the release archive
curl -sSL -o /tmp/jbm.zip \
  https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip
unzip -p /tmp/jbm.zip 'fonts/ttf/JetBrainsMono-Regular.ttf' > "$FONT_DIR/jetbrains_mono_regular.ttf"
unzip -p /tmp/jbm.zip 'fonts/ttf/JetBrainsMono-Medium.ttf'  > "$FONT_DIR/jetbrains_mono_medium.ttf"
unzip -p /tmp/jbm.zip 'OFL.txt'                             > "$LIC_DIR/OFL-JetBrainsMono.txt"
```

**Verify the payload before going further** — this is SC-007's whole story:

```sh
find app/src/main/res/font -name '*.ttf' -exec ls -l {} + | awk '{s+=$5} END {print s" bytes"}'
# expect 807920  (789 KiB) — well inside SC-007's 1.5MB
```

Two things here are load-bearing, both verified against the build:

- **Android resource names must be lowercase with underscores.** The renames are not cosmetic.
- **The licences must NOT go in `res/font/`.** That directory accepts font files only; a `.txt`
  there fails the build outright with *"The file name must end with .xml, .ttf, .ttc or .otf"*.
  They live in `app/src/main/assets/licenses/`, which still packages them into the APK — OFL 1.1
  requires the licence to accompany the fonts wherever they are redistributed, and an APK is
  redistribution.

---

## 2. Build and test

```sh
./gradlew assembleDebug     # gate 1
./gradlew test              # gate 2
```

Both must pass before the feature is complete (Constitution: Build gate).

**The two tests that matter most to this feature:**

```sh
# NOTE: `test` is an aggregate task and rejects --tests. Filtering needs the variant task.
./gradlew testDebugUnitTest --tests '*SlowLockPaletteTest'   # eleven literals + contrast + source scan
./gradlew testDebugUnitTest --tests '*DelayRangeTest'        # presets: literals, range, snap-stability
```

**The tests that must keep passing untouched** — the mechanical guard on FR-038/FR-042, that the
rename to "lock" never reached an identifier:

```sh
./gradlew testDebugUnitTest --tests '*ShortcutContractTest' --tests '*DelayConfigTest' \
                            --tests '*IconTreatmentTest'    --tests '*WaitTimingTest'
```

If `ShortcutContractTest` fails, stop: something renamed a value written into shortcuts already on
users' home screens.

---

## 3. Check the size claim

```sh
ls -l app/build/outputs/apk/debug/app-debug.apk
```

Compare against an APK built from the previous commit. Growth should be **under 790 KiB** — fonts
are deflated in the APK, so the installed cost is below the raw total. Anything materially larger
is unaccounted for and must be investigated before release (SC-007).

---

## 4. Verify by eye

Automated tests cover the palette arithmetic and the preset logic. **Everything else needs a
device**, and per the constitution an agent must not drive it. Run
[manual-test-plan.md](./manual-test-plan.md) — 8 tiers, numbered and traceable to requirements.

The five that catch the most:

| Case | Why it is the one that catches things |
|---|---|
| **M1.3** | Airplane mode, first launch — proves the fonts are in the APK, not fetched |
| **M1.5** | An app with a CJK or Arabic label — the one unverified assumption in the design (research R3) |
| **M5.2** | Dark mode, dark room, tap a pinned icon — the wait screen is the one surface that still follows the system setting |
| **M5.3** | Watch a 30-second wait end to end — nothing may move, and the screen must arrive complete |
| **M7.1** | Largest font scale on the smallest device — the numeral yields, the button stays reachable |

---

## 5. Where the decisions live

| Question | Answer in |
|---|---|
| What colour is this, and may I add one? | `contracts/design-tokens.md` C1–C3 |
| Why is amber never text? | `contracts/design-tokens.md` C2 · research R14 |
| What does this screen keep? | `contracts/screen-inventory.md` |
| What may `ui/components` become? | `contracts/ui-components.md` U5 |
| Why 44dp and not 48dp? | spec FR-045 · research R9 · tokens C10 |
| Why not variable fonts? | spec Clarifications Q5 · research R1 |
| Why is the wait screen allowed to be branded now? | spec FR-037 · plan Recorded rulings |
| What is Phase 2 and what is Phase 3? | spec **Out of Scope** |
