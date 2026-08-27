# Contract: Repository Interfaces

**Date**: 2026-08-26 | **Spec**: FR-020, FR-021, FR-024, FR-025, FR-040 | **Plan**: [../plan.md](../plan.md)

Every source of data outside the process is reached through one of the interfaces below. Each is
declared in a `domain` package, takes and returns domain values only, and suspends where it does
work. Implementations live in `data` and are bound by the module beside them.

**General obligations**

- **O1** — No `android.*` type appears in any signature here. `Intent`, `Bitmap`, `Context`,
  `ShortcutInfo` and `ApplicationInfo` stop at the `data` boundary.
- **O2** — Every function is main-safe: it may be called from the main dispatcher and moves itself
  to the injected dispatcher. Callers never wrap a call in `withContext`.
- **O3** — Nothing here throws on the launch path. A failure is expressed in the return type.
- **O4** — Each interface has a real second implementation in tests. None is an
  interface-with-one-implementation of the kind FR-044 forbids.

## `core/domain`

### `DelayConfigRepository`

```
suspend fun load(packageName: String): DelayConfig     // never null; default when absent
suspend fun save(packageName: String, config: DelayConfig)
```

Obligations: **the only route to `slowlock.delay-config`** — no other type opens it. `save`
replaces the whole record through one editor; there is no partial update. `load` sanitises rather
than validates (F2).

### `AppTargetRepository`

```
suspend fun resolve(packageName: String): AppTarget?   // null = gone, disabled, or profile absent
```

Obligations: `null` is an ordinary outcome, not an error — it is the constitution's mandated
null-`getLaunchIntentForPackage()` path and must stay unit-testable without a device. A package
with no launch intent resolves to `null`.

### `AppIconRepository`

```
suspend fun icon(packageName: String, versionCode: Long): ImageBitmap?
suspend fun sweep(keep: List<String>)
```

Obligations: keyed by package **and** version code, so a target's update invalidates its cached
icon. Icons never travel inside UI state — a bitmap in a `StateFlow` is retained as long as the
state is. Rasterisation and file I/O run off the main thread.

## `feature/apps/domain`

### `InstalledAppsRepository`

```
suspend fun load(): List<InstalledApp>
```

Obligations: excludes SlowLock, deduplicates by package, sorts by label under the **current**
locale read at load time. Uses `LauncherApps` — no permission, no dialog, and never
`QUERY_ALL_PACKAGES`.

## `feature/locks/domain`

### `LockOrderRepository`

```
suspend fun loadOrder(): List<String>                  // never null, never throws
suspend fun deriveOrder(pinned: Set<String>): List<String>
```

Obligations: **the only route to `slowlock.locks`**. `deriveOrder` writes back only when the order
actually changed. It must never be called with an empty set standing in for "could not ask".

### `PinnedShortcutsRepository`

```
suspend fun pinnedIds(): Set<String>?                  // null = could not ask
```

Obligations: **`null` and the empty set are opposite claims.** `null` is returned when there is no
`ShortcutManager` and when the call throws — including the direct-boot `IllegalStateException` a
locked device after reboot produces. Treating `null` as empty empties the user's entire lock list.
IDs come back as package names, because the shortcut ID *is* the package name (F4).

## `feature/shortcut/domain`

### `PinSupportRepository`

```
suspend fun current(): PinSupport                      // Supported | Unsupported, never Unknown
```

Obligations: asking always produces an answer; `PinSupport.Unknown` means the question has not been
asked and is the caller's initial value, never a return. A missing `ShortcutManager` reads as
`Unsupported`. Never cached across a background trip — the user may have changed launcher.

### `ShortcutPinRepository`

```
suspend fun requestPin(target: AppTarget, treatment: IconTreatment): PinRequestResult
```

Obligations: `isRequestPinShortcutSupported()` gates **every** attempt. Icon baking runs off the
main thread; the `ShortcutManager` calls stay on the caller's dispatcher. A declined system dialog
is a normal outcome the app degrades gracefully through — it is not an error and creates no lock.

## What is deliberately *not* an interface

- **`WaitTiming`** (`deadlineFrom`, `remainingMillis`) — pure functions over a clock value passed
  in. No seam needed; the caller supplies the time.
- **`DelayRange`**, **`LockList`**, **`ShortcutContract`**, **`shortcutSpec()`** — pure, already
  testable, and wrapping them would be the forwarding-only indirection FR-045 forbids.
