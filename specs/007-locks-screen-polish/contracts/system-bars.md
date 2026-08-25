# Contract S — System bar appearance

**Feature**: `specs/007-locks-screen-polish` | **Owner**: `MainActivity`

Binding on this feature and on every feature after it. Where a later spec needs to change one of
these, it amends this file rather than working around it.

---

**S1 — One writer.** `MainActivity.enableEdgeToEdge(...)` is the *only* place in the app that
decides system bar appearance. No screen, no composable, and no theme may set
`isAppearanceLightStatusBars`, `isAppearanceLightNavigationBars`, `statusBarColor`, or
`navigationBarColor`. Two writers to one platform bit is a bug that only appears in the order the
calls happen to run.

**S2 — Both bars are declared light, always.** Both styles are
`SystemBarStyle.light(TRANSPARENT, TRANSPARENT)`. "Light" names the *background* the platform is
being told about, which is why it produces dark glyphs; the call site must carry that sentence as a
comment, because the API reads backwards.

**S3 — The device's night setting is not an input.** No `SystemBarStyle.auto(...)`, no
`detectDarkMode` lambda, no `values-night` theme for `Theme.SlowLock`, no `isSystemInDarkTheme()`
reaching this decision. The app is light-only (004 FR-008); the bars follow the app, not the phone.
This is the whole of FR-002 and the whole of the defect this feature fixes.

**S4 — No scrim.** Both scrim arguments are transparent. The app draws its own ground behind the
bars and a scrim would put a second colour over it — a colour that is not in the palette.

**S5 — `ShortcutLaunchActivity` is out of scope and stays out.** It does not call
`enableEdgeToEdge`, it resolves its own colours through `values-night`, and it follows the device's
light/dark setting on purpose (004 FR-031, FR-033). S1–S4 bind `MainActivity` only. A future change
that "unifies" the two activities' bar handling breaks the one screen that must not move.

**S6 — Edge-to-edge stays.** This contract changes how the bars are *drawn over*, never whether the
app draws behind them. Insets handling is unchanged.

**S7 — The API 26 limitation is accepted and named.** The platform gained dark navigation-bar
icons in API 27. On API 26 the navigation bar keeps light icons over the bone ground. This is an
accepted limitation in the sense of Constitution I, recorded in the manual test plan as
untested-and-accepted. It MUST NOT be "fixed" by painting a scrim, which would put a dark strip on
API 26–28 to solve a problem only API 26 has.
