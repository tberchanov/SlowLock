# Contract: Layer Boundaries

**Plan**: [../plan.md](../plan.md) · **Constitution**: v5.0.0, Principles II, III, V

Obligations B1–B12. These are the structural claims the feature is delivering, stated so they can be
checked by reading a file rather than by running the app. B1–B5 are the new rules; B6–B12 are what
must not be over-applied.

---

## What a `data` file may contain

- **B1**: a repository implementation reads its source and writes to it, and does nothing else. No
  `filter`, `sortedBy`, `distinct`, `groupBy`, or set operation applied to the *domain* meaning of
  what it holds.
- **B2**: it may decode its source's representation into domain types and encode back, including
  whatever the format demands to yield a well-formed value. `locksFrom`'s split-trim-distinct over a
  newline-joined string is decoding: the stored form is one string, and a list with a repeated
  package is not a well-formed reading of it. `dedupeByPackage` over a launcher enumeration is not:
  the platform's answer is already well-formed, and collapsing it is a rule feature 001 states.
- **B3**: it holds no reference to another repository interface, and no constructor parameter typed
  as one.
- **B4**: it decides nothing a requirement states. A branch is permitted only where the *source*
  forces one — a missing system service, a throwing call, an absent key, a platform constant that
  differs by API level.
- **B5**: no pure decision function is declared in a `data` file. `pinWhenSupported` in
  `ShortcutPinner.kt` is the one that exists today and it moves.

## What a `domain` file may contain

- **B6**: no `android.*` import. `androidx.compose.ui.graphics.ImageBitmap` is permitted and already
  crosses `AppIconRepository` under obligation O1; `android.graphics.Bitmap` is not.
- **B7**: no `androidx.lifecycle.SavedStateHandle`, no `ViewModel`, no Compose runtime type. A rule
  that needs a value from the handle takes it as a parameter (R6).
- **B8**: a use case takes its collaborators through the constructor and names no dispatcher. Every
  repository it calls is main-safe already (O2), so a `withContext` inside one would be a second
  opinion about where work belongs.

## What a `ui` file may contain

- **B9**: a `ViewModel` calls and maps. It may hold a repository only where the call carries no rule
  (FR-011); it may not hold a branch that a requirement states.
- **B10**: a UI state class holds values and the display states derived from them. It applies no rule
  to the data itself — no filter, no sort, no merge. `showsIntro`, `hasNoResults`, `canCreate` and the
  `withLocks` latch are display state and stay.
- **B11**: derived state is computed with the state it appears in, never stored beside its inputs and
  updated by hand (Principle V, and R5's whole argument).

## Over-application

- **B12**: no use case may exist whose `invoke` body is a single call to one repository with no
  branch, no combination and no transformation. That is the forwarding-only use case Principle V
  prohibits, and FR-011 and SC-006 are its check.

---

## How each is checked

| Obligation | Check |
|---|---|
| B1, B3, B4, B5 | Gate 1 in [../quickstart.md](../quickstart.md) — read every file under a `data` package. |
| B2 | Reviewed per site against the two examples above; the frozen-value tests catch a decode that changed. |
| B6, B7 | `grep` for the prohibited imports across `**/domain/**`. |
| B8 | `grep` for `withContext`, `Dispatchers.` and `flowOn` across `**/domain/**`. |
| B9, B10, B11 | Gate 2 — read every holder and every UI state class. |
| B12 | Gate 3 — one pass over the six new use cases, counting branches. |
