# Quickstart: Public Site & Privacy Policy

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Execution order, end to end. `/speckit-tasks` turns this into `tasks.md`; this file is the shape of
the work and the commands that verify it.

## 0. Before anything

```bash
cd /Users/anatolii/Projects/SlowLock
ls site/                 # "SlowLock Site.html", "SlowLock Privacy Policy.html", PUBLISHING.md
```

Nothing under `app/` is touched by any step below.

## 1. Unpack the bundles (research R2)

A throwaway Python 3 script in the scratchpad, not committed:

- Parse `<script type="__bundler/template">` → JSON string → the page's real HTML.
- Parse `<script type="__bundler/manifest">` → write each `font/woff2` entry to `site/fonts/`.
- Write the two pages to `site/index.html` and `site/privacy.html`.

Then, in the extracted HTML:

- Drop `<x-dc>`, `<helmet>`, the runtime `<script src="…uuid…">`, and the trailing
  `<script type="text/x-dc">`.
- Drop the two `preconnect` links to Google Fonts.
- Repoint every `@font-face` `src` at `fonts/<name>.woff2`, keeping the `unicode-range` splits.
- `sc-camel-view-box` → `viewBox` (2 occurrences, landing page).
- `style-hover="…"` → real CSS `:hover` (2 occurrences) in the new `<style>` block.
- Delete `data-screen-label`.

Check: `grep -c '<script' site/*.html` → **0**.

## 2. Fix the page defects

| Fix | Where | Requirement |
|---|---|---|
| `SlowLock Privacy.dc.html` → `privacy.html` | `index.html` footer | FR-005 |
| `SlowLock Site.dc.html` → `index.html` (×2) | `privacy.html` header + footer | FR-005 |
| **Remove the "DRAFT · NOT LEGALLY REVIEWED" block** | `privacy.html` | FR-015 |
| Real `<title>` on both | both | FR-008 |
| `<meta name="description">`, `og:title/description/url`, favicon link | both | FR-008 |
| `<!-- TODO: replace with the app's Play listing URL -->` | `index.html` CTA | FR-021, R8 |
| Confirm the last-updated date | `privacy.html` | FR-014 |

Check: `grep -rn '\.dc\.html\|Bundled Page\|DRAFT' site/` → **no matches**.

## 3. Make it responsive (research R4)

One `<style>` block per page: the eight containers in the R4 table, a
`@media (max-width: 860px)` block, the two `:hover` rules, and a `max-width` guard for ultrawide
windows.

## 4. Favicon and preview image (research R6)

- `site/assets/icon.svg` — the lock mark from the page header, standalone.
- `site/assets/og.png` — 1200×630, rendered from an SVG:

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless --disable-gpu --screenshot=site/assets/og.png \
  --window-size=1200,630 /path/to/og.svg
```

If this fails, ship without `og:image` rather than with a broken reference.

## 5. Add the licence (FR-022, research R7)

`LICENSE` at the repository root: standard MIT, year 2026. Copyright holder defaults to
`tberchanov` unless the maintainer supplies a name. Add `[MIT](LICENSE)` to the README if one
exists.

## 6. Verify locally — before pushing anything

```bash
python3 -m http.server 8000 --directory site
```

- `http://localhost:8000/` → landing page (FR-003)
- Click through both directions (FR-005)
- **Scripts disabled** → both pages fully readable (FR-006, SC-002)
- DevTools at 390 px → no horizontal scroll (FR-007, SC-003)
- `curl -s http://localhost:8000/privacy.html | grep -c "collects nothing"` → **1** (SC-002)
- `grep -rn 'sc-camel\|style-hover\|data-screen-label\|x-dc' site/` → **no matches**

## 7. Clean up

```bash
rm "site/SlowLock Site.html" "site/SlowLock Privacy Policy.html" site/PUBLISHING.md
touch site/.nojekyll
```

`PUBLISHING.md` goes because everything in it now lives in this feature's spec and plan — and
because anything left in `site/` is served publicly (research R9).

## 8. Confirm the app is untouched

```bash
git status --short app/ gradle/          # expect no output
./gradlew test assembleDebug             # constitution build gate
```

## 9. Publish (research R1)

Add `.github/workflows/pages.yml`:

```yaml
name: Deploy site to Pages
on:
  push:
    branches: [main]
    paths: ['site/**', '.github/workflows/pages.yml']
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with:
          path: site
      - id: deployment
        uses: actions/deploy-pages@v4
```

Then commit, push to `main`, and set **Settings → Pages → Source: GitHub Actions**. Watch the run
in the Actions tab (~30 s).

## 10. Verify live

Run [manual-test-plan.md](./manual-test-plan.md) — 14 numbered cases. The ones that can only be
checked live:

```bash
curl -s https://tberchanov.github.io/SlowLock/privacy.html | grep -c "collects nothing"   # 1
curl -s -o /dev/null -w '%{http_code}\n' https://tberchanov.github.io/SlowLock/           # 200
```

Plus: open both pages on a real phone.

## 11. Google Play

- Play Console → *Policy* → *App content* → *Privacy policy* →
  `https://tberchanov.github.io/SlowLock/privacy.html`
- Fill **Data safety**: no data collected, no data shared — it must not contradict the page
  (FR-017).
- When the listing exists, replace the Play URL (`grep -rn TODO site/`) and push.
