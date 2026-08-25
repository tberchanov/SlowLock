package com.slowlock.locks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.shortcut.IconTreatment
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.theme.Badge as BadgeShape
import com.slowlock.ui.theme.SlowLockType

/**
 * The locks the user has made — what a returning user opens on (FR-009, contract K2).
 *
 * **Stateless in the strict sense of U5**: it holds nothing, loads nothing, persists nothing, and
 * every mutation leaves through a callback. [LocksViewModel] does the reading; this draws the
 * answer.
 *
 * **Feature 007 restyled this screen from the `New · Locks` artboard** and changed nothing about
 * what it does. Two things moved: the heading became this screen's own rather than the flow's
 * `ScreenHeader` (see [Heading], contract L1), and a row's delay left the joined second line for a
 * badge at the trailing edge (see [LockRow], contract L9). Every behavioural rule feature 005 wrote
 * — K2 through K6 — is untouched.
 *
 * **The count states the number and nothing more** (FR-011, and still true after 007). The reason
 * is on [Heading], along with the rest of the heading block.
 *
 * **No search, no filter, no sort, no reorder, no per-lock toggle, no "pin again."** All are
 * permanently out of scope, and this list is expected to hold tens of rows rather than the app
 * list's hundreds — which is also why it needs no query and no index.
 *
 * The row lives in this file rather than in `ui/components` because it has exactly one caller
 * (U5). A second screen needing it is what would move it, and no second screen does.
 */
@Composable
fun LocksScreen(
    state: LocksUiState,
    iconCache: AppIconCache,
    onNewLock: () -> Unit,
    onEdit: (packageName: String) -> Unit,
    onExplainRemoval: (packageName: String) -> Unit,
    onDismissExplanation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = HORIZONTAL_PADDING),
        ) {
            Heading(count = state.locks.size)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.locks, key = { it.packageName }) { lock ->
                    LockRow(
                        lock = lock,
                        iconCache = iconCache,
                        onEdit = { onEdit(lock.packageName) },
                        onExplainRemoval = { onExplainRemoval(lock.packageName) },
                    )
                }
            }

            // Exactly one (U2), and the only route back into the flow from here (FR-014).
            PrimaryAction(
                label = stringResource(R.string.locks_new),
                onClick = onNewLock,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )
        }

        // One dialog for the whole screen, driven by one nullable field — never one per row
        // (contract K4). Two cannot be open at once because there is only one place for a package
        // to sit.
        val explaining = state.explainingRemoval
        if (explaining != null) {
            RemovalHelp(
                // The label the confirmation names, resolved from the row rather than re-looked
                // up: a lock whose app is gone has none, and the package name is then the only
                // thing left to call it (K3).
                label = state.locks.firstOrNull { it.packageName == explaining }?.label
                    ?: explaining,
                onDismiss = onDismissExplanation,
            )
        }
    }
}

/**
 * The screen's title and the count beneath it (007 US2, contract L1–L5).
 *
 * **This screen draws its own heading and deliberately does not use `ScreenHeader`.** That
 * component belongs to the three flow screens, and generalising it into a large-title variant
 * would put this screen's design decisions inside a component with three other callers for no
 * gain — the block is a title, a gap, and a caption (contract L1). `ScreenHeader` is unchanged.
 *
 * **No controls.** No back tile, no step counter, no menu, no search: the Locks screen is the app's
 * root and there is nowhere to go back to (contract L5).
 *
 * **The caption states the count and nothing more.** The artboard reads "3 ON YOUR HOME SCREEN"
 * and the second half is still not shipped — Android cannot tell the app whether those icons are
 * still there, and telling a user who deleted one that it is still on their home screen is exactly
 * the claim Constitution I forbids (005 FR-011, contract L3). 007 adopted the *styling* of that
 * line and refused its claim.
 *
 * **There is no zero state.** An empty list renders [IntroScreen] instead, so the smallest number
 * this caption can ever state is one (`LocksUiState.showsLocks`).
 */
@Composable
private fun Heading(count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 8.dp, bottom = 20.dp)) {
        Text(
            text = stringResource(R.string.locks_title),
            style = SlowLockType.TitleDisplay,
            // Ink on Bone (C3).
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        // **Two resources for one sentence, and the split is the point.** The capitalised form is
        // drawn; the ordinary form is spoken. `uppercase()` at display time was the obvious way to
        // avoid the duplication and is forbidden by contract C8 — it is a locale trap (Turkish
        // dotted and dotless i is the standing example), and it takes the capitalisation decision
        // away from the translator, who is the only person who knows whether their script has case
        // at all. Handing the un-capitalised string to the screen reader is the other half: it
        // hears "3 locks" rather than being asked to spell out capitals (FR-008, FR-012, L4).
        val spoken = pluralStringResource(R.plurals.locks_count, count, count)
        Text(
            text = pluralStringResource(R.plurals.locks_count_caption, count, count),
            style = SlowLockType.Count,
            // Ink40 on Bone (C1, C3). Mono, because it leads with a number the user reads (C6).
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.semantics { contentDescription = spoken },
        )
    }
}

/**
 * How to remove a lock (FR-021, FR-022, SC-012, contract K4).
 *
 * **Not a confirmation, and it has no destructive button** — because there is no destructive
 * action to offer. A lock is its pinned shortcut (FR-003a), Android gives no way to unpin one, and
 * the home screen is the only place removal exists. The honest dialog is the one that says so and
 * gets out of the way, which is why the only button is "OK".
 *
 * An in-app "Remove" that merely hid the row would have been worse than nothing: the icon would
 * stay on the home screen, still waiting and still opening the app, while the list quietly stopped
 * meaning what it says. The version of this screen that shipped that button also needed a
 * tombstone record to keep the row hidden from the next derivation — a persistence layer built to
 * fake a capability the platform does not have.
 *
 * **The wording is the deliverable, and it lives in `strings.xml`** — read the comment on
 * `locks_removal_body` before changing either.
 *
 * **Every colour is stated, none inherited.** `AlertDialog` derives its container from
 * `surfaceContainerHigh` and its text from `onSurfaceVariant`, and neither is one of the eleven
 * tokens — Material would quietly paint a twelfth and a thirteenth colour here (K6, FR-033,
 * SC-009). `Card` for the container, `Ink` for the title, `Ink60` for the body, spelled out.
 */
@Composable
private fun RemovalHelp(
    label: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // Both are the dismissal (US5 scenario 3): tapping outside or pressing back keeps the
        // lock, changes nothing, and is the safe half of a destructive choice.
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.locks_removal_title),
                style = SlowLockType.RowLabel,
                // Ink on Card (C3).
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.locks_removal_body, label),
                style = SlowLockType.Body,
                // Ink60 on Card (C3).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // **One button, and it is the dismissal.** There is no second thing this dialog can do
        // (K4). `confirmButton` is `AlertDialog`'s only mandatory slot, so "OK" goes there and
        // `dismissButton` is left off rather than filled with a synonym.
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.locks_removal_dismiss),
                    style = SlowLockType.RowLabel,
                    // Ink on Card (C3).
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

/**
 * One lock: its app's icon, its label, and what it does.
 *
 * A card in a list, so `shapes.large` — the 18dp slot C9 reserved for this screen by name — with
 * the `Card` fill and the `Line` hairline the design gives raised surfaces.
 *
 * **Feature 007 restyled the available row from the `New · Locks` artboard** (contract L9). The
 * icon grew to 48dp, the padding to 14dp, the name to Medium, and — the one change that is not
 * purely visual — the delay left the joined second line for [DelayBadge] at the trailing edge,
 * leaving the treatment alone on line 2. This **amends feature 005's contract K row layout and
 * nothing else about it**: the row still carries app name, delay and treatment, so nothing 005
 * promised the user is withdrawn, and tap-to-edit, long-press-to-explain, the custom accessibility
 * action, and the unavailable row's whole treatment are all exactly as 005 left them.
 *
 * **The unavailable row was deliberately not restyled** (FR-020, contract L10). The artboards do
 * not draw it, its body is a sentence rather than a name plus a detail, and it gets no badge —
 * inventing an artboard for it is not this feature's job.
 *
 * **The row does not block on its icon** (FR-015): it draws immediately with the `Fill`
 * placeholder and the bitmap arrives when it arrives, the same way `AppListRow` already does it.
 *
 * `heightIn` rather than `height`: at the largest system font scale the two lines are taller than
 * the minimum, and the row must grow rather than clip (SC-008).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockRow(
    lock: Lock,
    iconCache: AppIconCache,
    onEdit: () -> Unit,
    onExplainRemoval: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    val removalHelpLabel = stringResource(R.string.locks_removal_action)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            // K3: an unavailable row carries **no click modifier at all**. Not a disabled one —
            // a disabled clickable is still a target that reports itself to accessibility as one.
            //
            // K4/FR-021: an available row gets both gestures from one modifier. `combinedClickable`
            // rather than a `clickable` plus a `pointerInput`, because the two have to share a
            // press indication and a hit area or the long press starts landing on the row *under*
            // the finger. The long press is no longer destructive — it opens an explanation, not a
            // confirmation — which is why it needs no second safeguard of its own.
            .then(
                if (lock.isAvailable) {
                    Modifier.combinedClickable(
                        onClick = onEdit,
                        onLongClick = onExplainRemoval,
                        onLongClickLabel = removalHelpLabel,
                    )
                } else {
                    Modifier
                }
            )
            // FR-041/SC-011: **every** row, available or not, offers the explanation as a custom
            // accessibility action, so it is reachable from TalkBack's action menu without a long
            // press — a gesture that is awkward with an explore-by-touch cursor and impossible
            // with some switch setups (research R6).
            //
            // On the unavailable row this is the accessible half of the visible control below;
            // both call the same lambda, so they cannot drift.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(removalHelpLabel) {
                        onExplainRemoval()
                        true
                    },
                )
            }
            .heightIn(min = ROW_MIN_HEIGHT)
            .padding(ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LockIcon(lock = lock, iconCache = iconCache)
        Spacer(Modifier.width(14.dp))
        if (lock.isAvailable) {
            AvailableRowText(lock = lock, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            DelayBadge(seconds = lock.delaySeconds)
        } else {
            UnavailableRowText(lock = lock, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            // K3: the visible control, and only here. An available row puts the explanation behind
            // a long press because it has a tap target to attach one to; this row has none — its
            // whole point is that it is not tappable — so the control is drawn.
            TextButton(onClick = onExplainRemoval) {
                Text(
                    text = stringResource(R.string.locks_removal_help),
                    style = SlowLockType.Footnote,
                    // Ink60 on Card (C3).
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AvailableRowText(lock: Lock, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            // Resolved fresh on every read, never stored (FR-012, SC-006). A renamed app shows its
            // new name here without anything migrating.
            text = lock.label.orEmpty(),
            style = SlowLockType.RowTitle,
            // Ink on Card (C3).
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // The label yields first: the delay and the treatment are what the row is *for*, and
            // they stay legible (spec edge case, 007 FR-017).
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // **The treatment alone since 007** (FR-014, contract L9). The delay used to share
            // this line through the `locks_row_detail` joiner; it now sits in the badge, where it
            // is a number the eye lands on rather than a clause the eye reads.
            text = stringResource(lock.treatment.labelRes),
            style = SlowLockType.Footnote,
            // Ink40 on Card (C3).
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * The delay, at the trailing edge of an available row (007 FR-015, contract L6, L7).
 *
 * **This is where the delay lives now.** Feature 005 joined it to the treatment on the row's second
 * line; the `New · Locks` artboard pulls it out into a badge, because the delay is the product's
 * central value and a number the eye lands on says that where a clause does not.
 *
 * **Trailing, not right.** Under RTL the badge moves to the leading edge with everything else.
 *
 * **It never shrinks.** The badge carries no `weight`, so a long app name ellipsises and the delay
 * stays whole — the row's name is the part that can afford to yield (FR-017).
 *
 * **It shows "10s" and says "10 second wait".** The compact form is for the eye alone; a screen
 * reader left with it would read "ten s". The spoken form is the same `delay_wait` plural the
 * preview card uses, so a lock is described the same way before and after it is made (FR-018).
 *
 * `AmberDark on AmberWash` — 5.84:1, and one of the pairings `SlowLockPaletteTest` already asserts.
 */
@Composable
private fun DelayBadge(seconds: Int, modifier: Modifier = Modifier) {
    val spoken = pluralStringResource(R.plurals.delay_wait, seconds, seconds)
    Text(
        text = stringResource(R.string.locks_delay_badge, seconds),
        style = SlowLockType.Badge,
        // AmberDark on AmberWash (C2, C3): the accent as a word is only ever the dark token.
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .clip(BadgeShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .semantics { contentDescription = spoken },
    )
}

/**
 * The row for a lock whose app is gone (FR-020, contract K3).
 *
 * **Shown, never hidden.** The lock is still a lock, the icon may still be sitting on the home
 * screen, and only the user removes either. The package name is the only thing left to identify it
 * by, so the message carries it.
 *
 * Its visible remove control lives in [LockRow] beside it (K3): this row has no tap target for a
 * long press to attach to, which is the whole reason that control is drawn rather than hidden
 * behind a gesture.
 */
@Composable
private fun UnavailableRowText(lock: Lock, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.locks_unavailable, lock.packageName),
        style = SlowLockType.Body,
        // Ink60 on Card (C3).
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * The app's icon, or the `Fill` placeholder until it loads — and permanently, for a package that
 * no longer resolves.
 */
@Composable
private fun LockIcon(
    lock: Lock,
    iconCache: AppIconCache,
    modifier: Modifier = Modifier,
) {
    var icon by remember(lock.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(lock.packageName, lock.versionCode, lock.isAvailable) {
        // An unresolvable package has no icon to rasterize, and asking would cost a lookup per
        // dead row on every visit for an answer that cannot change until it is reinstalled.
        icon = if (lock.isAvailable) iconCache.icon(lock.packageName, lock.versionCode) else null
    }
    Box(modifier = modifier.size(ICON_SIZE)) {
        val bitmap = icon
        if (bitmap == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    // 14dp, the artboard's placeholder corner. Only the PLACEHOLDER is
                    // clipped: a loaded launcher icon already carries its own mask, and
                    // re-masking it here would shave the adaptive icons meant to be round.
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(
                    R.string.locks_icon_description,
                    lock.label.orEmpty(),
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The treatment's display name.
 *
 * A second copy of the mapping `ShortcutConfigScreen` holds privately, and deliberately not a
 * refactor of it: `contracts/root-navigation.md` N10 confines this feature's change to that file
 * to two lines, and widening it to export a helper is exactly the kind of drive-by this feature
 * is not allowed to make. Both read the same frozen `shortcut_treatment_*` resources.
 */
private val IconTreatment.labelRes: Int
    get() = when (this) {
        IconTreatment.Original -> R.string.shortcut_treatment_original
        IconTreatment.Invert -> R.string.shortcut_treatment_invert
        IconTreatment.Gray -> R.string.shortcut_treatment_gray
    }

private val ROW_MIN_HEIGHT = 64.dp
private val ROW_PADDING = 14.dp
private val ICON_SIZE = 48.dp
private val HORIZONTAL_PADDING = 20.dp
