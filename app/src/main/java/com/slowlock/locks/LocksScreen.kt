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
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.shortcut.IconTreatment
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.theme.SlowLockType

/**
 * The locks the user has made — what a returning user opens on (FR-009, contract K2).
 *
 * **Stateless in the strict sense of U5**: it holds nothing, loads nothing, persists nothing, and
 * every mutation leaves through a callback. [LocksViewModel] does the reading; this draws the
 * answer.
 *
 * **The count states the number and nothing more** (FR-011). The design source reads
 * "3 ON YOUR HOME SCREEN" and that half is deliberately not shipped: Android cannot tell the app
 * whether those icons are still there, and telling a user who deleted one that it is still on
 * their home screen is exactly the claim Constitution I forbids.
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
            // `onBack = null`: this screen is the app's root, so there is nowhere to go back to
            // and no step to count. U1's rule then renders no tile and no leading space.
            ScreenHeader(
                title = stringResource(R.string.locks_title),
                onBack = null,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.locks_count,
                    state.locks.size,
                    state.locks.size,
                ),
                style = SlowLockType.Footnote,
                // Ink40 on Bone (C1, C3). Mono, because it is a number the user reads (C6).
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp),
            )

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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LockIcon(lock = lock, iconCache = iconCache)
        Spacer(Modifier.width(14.dp))
        if (lock.isAvailable) {
            AvailableRowText(lock = lock, modifier = Modifier.weight(1f))
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            // Resolved fresh on every read, never stored (FR-012, SC-006). A renamed app shows its
            // new name here without anything migrating.
            text = lock.label.orEmpty(),
            style = SlowLockType.RowLabel,
            // Ink on Card (C3).
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // The label yields first: the delay and the treatment are what the row is *for*, and
            // they stay legible (spec edge case).
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.locks_row_detail,
                pluralStringResource(R.plurals.delay_wait, lock.delaySeconds, lock.delaySeconds),
                stringResource(lock.treatment.labelRes),
            ),
            style = SlowLockType.Footnote,
            // Ink40 on Card (C3). Mono, because the delay is a number the user reads (C6).
            color = MaterialTheme.colorScheme.outline,
        )
    }
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
                    .clip(MaterialTheme.shapes.extraSmall)
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
private val ICON_SIZE = 44.dp
private val HORIZONTAL_PADDING = 20.dp
