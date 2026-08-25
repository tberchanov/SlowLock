# Quickstart: Legible system bar and a redesigned Locks screen

**Feature**: `specs/007-locks-screen-polish`

Five files change. Nothing is added, no dependency moves, no module appears.

## The files

| File | Change |
|---|---|
| `app/src/main/java/com/slowlock/MainActivity.kt` | pass explicit `SystemBarStyle`s to `enableEdgeToEdge` |
| `app/src/main/java/com/slowlock/ui/theme/Type.kt` | add `TitleDisplay`, `Count`, `RowTitle`, `Badge` |
| `app/src/main/java/com/slowlock/ui/theme/Shape.kt` | add the 9dp `Badge` shape beside `Pill` |
| `app/src/main/java/com/slowlock/locks/LocksScreen.kt` | new heading block; redesigned available row |
| `app/src/main/res/values/strings.xml` | `locks_title` reworded; `locks_delay_badge` and `locks_count_caption` added; `locks_row_detail` removed |

## The system bar, in one call

```kotlin
enableEdgeToEdge(
    // "light" describes the BACKGROUND, which is why it produces DARK glyphs. The app is
    // light-only (004 FR-008), so the bars must not consult the device's night setting —
    // that is exactly what put white indicators on a bone screen. Contract S2, S3.
    statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
    navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
)
```

`TRANSPARENT` is `android.graphics.Color.TRANSPARENT` — an `Int`, not a Compose `Color`, so it is
not a palette token and not a colour the app paints. Both scrims are transparent because the app
draws its own ground behind the bars (contract S4).

## The heading block

Replaces this screen's `ScreenHeader(...)` call. `ScreenHeader` itself is not edited.

```kotlin
Text(text = stringResource(R.string.locks_title), style = SlowLockType.TitleDisplay, …)
Spacer(Modifier.height(4.dp))
// Two resources: the capitalised one is DRAWN, the ordinary one is SPOKEN. uppercase() at
// display time is a locale trap and is forbidden by contract C8 (see L4).
val spoken = pluralStringResource(R.plurals.locks_count, n, n)
Text(
    text = pluralStringResource(R.plurals.locks_count_caption, n, n),
    style = SlowLockType.Count,
    color = MaterialTheme.colorScheme.outline,     // Ink40 on Bone
    modifier = Modifier.semantics { contentDescription = spoken },
)
```

## The row's trailing badge

```kotlin
val spoken = pluralStringResource(R.plurals.delay_wait, lock.delaySeconds, lock.delaySeconds)
Text(
    text = stringResource(R.string.locks_delay_badge, lock.delaySeconds),
    style = SlowLockType.Badge,
    color = MaterialTheme.colorScheme.onPrimaryContainer,   // AmberDark
    modifier = Modifier
        .clip(BadgeShape)
        .background(MaterialTheme.colorScheme.primaryContainer)  // AmberWash
        .padding(horizontal = 9.dp, vertical = 5.dp)
        .semantics { contentDescription = spoken },             // contract L7
)
```

The row's second line becomes `stringResource(lock.treatment.labelRes)` alone.

## Verifying

```bash
./gradlew test          # SlowLockPaletteTest still guards the eleven colours and the literal scan
./gradlew assembleDebug
```

Then the manual test plan — **the maintainer runs it**, on their device. Nothing in this feature is
observable from a JVM test, and no agent may drive the device to pre-check it.
