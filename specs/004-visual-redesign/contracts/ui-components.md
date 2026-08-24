# Contract: Shared UI Components

**Feature**: `004-visual-redesign` | **Package**: `com.slowlock.ui.components`

Four composables. Each exists because two or more screens need it; none is an architectural seam,
and none holds state. Feature 005 builds its two new screens from these rather than reinventing
them.

**The rule that keeps this package small**: a composable earns a place here when a *second* screen
needs it. One screen's layout stays in that screen's file.

---

## U1 — `ScreenHeader`

```
ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,     // null = no back control
    modifier: Modifier = Modifier,
)
```

**Obligations**

- Renders a 40dp square `Card`-filled tile at `small` radius **with a `Line` hairline border**,
  containing a back arrow, then the title in the `title` role at 22sp / −0.01em.
- **`onBack == null` renders no tile and no leading space** — the title starts at the content
  edge. This is the app list's case in Phase 1 (FR-010).
- The tile's touch target is 48dp even though it draws at 40dp (C10 — the back tile is *not* one of
  the two groups exempted).
- Carries a content description for the back control.

**Deliberately absent**: the step counter (`1 / 3`). It is drawn in the canvas and is **Phase 2's**
(Out of Scope) — the counter implies a wizard entered from the Locks screen, which does not exist
yet. Adding a `step: Int?` parameter now would be building ahead of need.

**Callers**: delay screen (`onBack` set), icon screen (`onBack` set), app list (`onBack = null`).

---

## U2 — `PrimaryAction`

```
PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

**Obligations**

- Full width, 56dp tall, `medium` radius, `Amber` fill, `Ink` label in the `action` role
  (FR-006, C9).
- **At most one per screen.** This is a design rule the component cannot enforce; it is a review
  item and a manual-test observation.
- Disabled state keeps the shape and reduces emphasis without introducing a colour outside the
  palette.

**Callers**: delay ("Choose the icon"), icon ("Add to home screen"), unsupported ("Choose home
screen app"). Phase 2 adds first-run and Locks.

---

## U3 — `SecondaryAction`

```
SecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Obligations**: full width, 52dp, `medium` radius, transparent fill, `Line` hairline border, `Ink`
label (FR-007, C9). 48dp floor met.

**Callers**: unsupported-launcher screen ("Check again"). A single caller today — it is in this
package rather than that screen's file because it is half of a pair with `PrimaryAction`, and
splitting the pair across two files is how the two drift apart.

---

## U4 — `SelectableTile`

The one component with real behaviour. It backs both the delay presets and the icon treatments,
which look different but select identically.

```
SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)
```

**Obligations**

- Carries `Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)`.
  The **caller** wraps the row in `Modifier.selectableGroup()` (FR-043, research R9).
- Selected: `AmberWash` fill, `Amber` border, `AmberDark` content. Unselected: `Card` fill, `Line`
  border, `Ink60` content.
- **Selection is never signalled by colour alone** — the `selectable` role carries it to assistive
  technology. This is the requirement that made the tile a component rather than a styled `Box`
  (FR-043).
- `contentDescription` is mandatory, not optional, because the preset tiles' visible text is a bare
  value: "5s" must announce as the action it performs, not as two characters (FR-044).
- Ships at the drawn size. **Does not apply a 48dp minimum touch target** — see C10; this is the
  accepted shortfall, and it is here, in this one component, that it lives.

**Two legitimate selection states**

| Caller | Selected member | "None selected" valid? |
|---|---|---|
| Delay presets | `presetFor(seconds)` | **Yes** — any non-preset delay (US2 scenario 3) |
| Icon treatments | the chosen treatment | No — one is always selected |

`Role.RadioButton` expresses both without the tile needing to know which caller it has.

---

## U5 — What this package must not become

- **No state.** Every component above is a pure function of its parameters. None remembers, loads,
  or persists.
- **No navigation.** Components take callbacks; they never decide what happens next.
- **No screen-specific variants.** If a screen needs something these cannot express, that layout
  belongs in the screen's own file until a second screen needs it too.
- **No colour or type literals.** Components read colour from `MaterialTheme.colorScheme` and type
  from `SlowLockType`; neither a hex value nor an `sp` size may appear here (C1, C6).
  `SlowLockPaletteTest` enforces the colour half across the whole source tree.
- **Control dimensions DO belong here**, and this is a correction to an earlier over-strict reading
  of this rule. C9 fixes the 56dp action, the 52dp secondary, the 40dp back tile and the 1dp
  borders — and these four composables *are* where C9 is implemented. Hoisting them into a
  `Dimens` object would be indirection around single-use constants. What must not appear is a
  dimension C9 does not name.
- **`WaitScreen` uses none of these.** It resolves its own colours and type independently so that a
  change here cannot alter it by accident (FR-033, research R6). That isolation is deliberate and
  must survive refactoring.
