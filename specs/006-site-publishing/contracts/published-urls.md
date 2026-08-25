# Contract: Published URLs

**Feature**: [../spec.md](../spec.md) | **Status**: frozen on first publish

The site's public interface is its addresses. Two of them are promises to parties outside this
repository — Google Play's listing, and anyone who has ever linked to the site — and cannot be
changed without breaking something you cannot see from here.

## Frozen addresses

| Address | Serves | Frozen because |
|---|---|---|
| `https://tberchanov.github.io/SlowLock/` | Landing page (`site/index.html`) | Repository-name-derived; changing the repository name changes it. Referenced from the Play listing and the README. |
| `https://tberchanov.github.io/SlowLock/privacy.html` | Privacy policy (`site/privacy.html`) | **Submitted to Google Play.** A 404 here is a policy violation on a live listing, not a broken link. |

**What this forbids, once published:**

- Renaming `site/privacy.html`, or moving it into a subdirectory.
- Renaming the GitHub repository, or transferring it to another account, without immediately
  updating the Play Console listing.
- Making the repository private — Pages for a private repository is a paid feature, and the site
  goes offline.
- Removing `site/index.html`, which is what makes the bare address resolve.

**What this permits:**

- Editing the content of either page freely (that is User Story 3).
- Attaching a custom domain later — both addresses continue to work as redirects, and internal
  links keep working because they are relative (research R5).
- Adding further pages.

## Address rules

- **Relative internal links only.** `href="privacy.html"`, never `href="/privacy.html"`. The site
  is served from a sub-path; a leading slash escapes it (FR-005).
- **No spaces or escaped characters in filenames.** A privacy URL containing `%20` is going into a
  Play Console form and a store listing (FR-004).
- **Lowercase, hyphenated names.** GitHub Pages is case-sensitive; mixed case is a class of 404
  that only appears in production.

## Deployment contract

| Property | Value |
|---|---|
| Source | GitHub Actions (`.github/workflows/pages.yml`) |
| Trigger | Push to `main` touching `site/**` or the workflow itself; plus manual dispatch |
| Published directory | `site/` — and nothing else in the repository (FR-025) |
| Build step | None. Files are uploaded as authored. |
| Time to live | Under 5 minutes from push (FR-023, SC-007) |
| Cost | Zero (public repository) |

A push that touches only Android sources must not redeploy the site — that is what the `paths:`
filter is for, and it is verified as manual case M13.
