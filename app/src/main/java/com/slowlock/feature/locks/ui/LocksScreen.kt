package com.slowlock.feature.locks.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.core.domain.IconTreatment
import com.slowlock.feature.locks.domain.Lock
import com.slowlock.feature.locks.domain.isAvailable
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.theme.Badge as BadgeShape
import com.slowlock.ui.theme.SlowLockType

/**
 * The locks the user has made — what a returning user opens on (FR-009, contract K2).
 *
 * Stateless in the strict sense of U5: it holds nothing, loads nothing, persists nothing, and every
 * mutation leaves through a callback.
 *
 * No search, filter, sort, reorder, per-lock toggle or "pin again" — all permanently out of scope.
 * The list holds tens of rows rather than the app list's hundreds, so it needs no query and no
 * index.
 *
 * The row lives here rather than in `ui/components` because it has exactly one caller (U5).
 */
@Composable
fun LocksScreen(
    state: LocksUiState,
    icons: AppIconRepository,
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
                        icons = icons,
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

        // One dialog for the whole screen, driven by one nullable field — never one per row (K4).
        val explaining = state.explainingRemoval
        if (explaining != null) {
            RemovalHelp(
                // Resolved from the row, not re-looked up: a lock whose app is gone has no label,
                // and the package name is then the only thing left to call it (K3).
                label = state.locks.firstOrNull { it.packageName == explaining }?.label
                    ?: explaining,
                onDismiss = onDismissExplanation,
            )
        }
    }
}

/**
 * The screen's title and the count beneath it (contract L1–L5).
 *
 * Deliberately not `ScreenHeader`: that component belongs to the three flow screens, and this block
 * is a title, a gap and a caption (L1). No back tile, no step counter, no menu — the Locks screen
 * is the app's root (L5).
 *
 * The caption states the count and nothing more. Android cannot tell the app whether the icons are
 * still on the home screen, so claiming they are is what Constitution I forbids (FR-011, L3).
 *
 * There is no zero state — an empty list renders [IntroScreen] — so the smallest number this
 * caption can state is one.
 */
@Composable
private fun Heading(count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 8.dp, bottom = 20.dp)) {
        Text(
            text = stringResource(R.string.locks_title),
            style = SlowLockType.TitleDisplay,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        // Two resources for one sentence: the capitalised form is drawn, the ordinary form spoken.
        // `uppercase()` at display time is forbidden by C8 — a locale trap (Turkish dotted/dotless
        // i) that also takes the capitalisation decision away from the translator. The spoken half
        // gives the screen reader "3 locks" rather than spelled-out capitals (FR-008, FR-012, L4).
        val spoken = pluralStringResource(R.plurals.locks_count, count, count)
        Text(
            text = pluralStringResource(R.plurals.locks_count_caption, count, count),
            style = SlowLockType.Count,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.semantics { contentDescription = spoken },
        )
    }
}

/**
 * How to remove a lock (FR-021, FR-022, SC-012, contract K4).
 *
 * Not a confirmation and it has no destructive button, because there is no destructive action to
 * offer: a lock is its pinned shortcut (FR-003a), Android gives no way to unpin one, and the home
 * screen is the only place removal exists. An in-app "Remove" that merely hid the row would leave
 * the icon on the home screen still opening the app, and needed a tombstone record to fake a
 * capability the platform does not have.
 *
 * The wording is the deliverable and lives in `strings.xml` — read the comment on
 * `locks_removal_body` before changing either.
 *
 * Every colour is stated, none inherited: `AlertDialog` derives its container from
 * `surfaceContainerHigh` and its text from `onSurfaceVariant`, neither of which is one of the
 * eleven tokens (K6, FR-033, SC-009).
 */
@Composable
private fun RemovalHelp(
    label: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        // Tapping outside and pressing back are both the dismissal: they keep the lock unchanged.
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.locks_removal_title),
                style = SlowLockType.RowLabel,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.locks_removal_body, label),
                style = SlowLockType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // One button, and it is the dismissal (K4). `confirmButton` is the only mandatory slot, so
        // "OK" goes there and `dismissButton` is left off rather than filled with a synonym.
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.locks_removal_dismiss),
                    style = SlowLockType.RowLabel,
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
 * The unavailable row is deliberately unstyled by the artboards (FR-020, contract L10): its body is
 * a sentence rather than a name plus a detail, and it gets no badge.
 *
 * The row does not block on its icon (FR-015): it draws immediately with the `Fill` placeholder.
 *
 * `heightIn` rather than `height`, because at the largest system font scale the two lines exceed
 * the minimum and the row must grow rather than clip (SC-008).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockRow(
    lock: Lock,
    icons: AppIconRepository,
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
            // K3: an unavailable row carries no click modifier at all — not a disabled one, since a
            // disabled clickable still reports itself to accessibility as a target.
            //
            // K4/FR-021: `combinedClickable` rather than `clickable` plus `pointerInput`, because
            // the two gestures must share a press indication and a hit area or the long press lands
            // on the row under the finger.
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
            // FR-041/SC-011: every row offers the explanation as a custom accessibility action, so
            // it is reachable from TalkBack's action menu without a long press — awkward with an
            // explore-by-touch cursor and impossible with some switch setups (research R6).
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
        LockIcon(lock = lock, icons = icons)
        Spacer(Modifier.width(14.dp))
        if (lock.isAvailable) {
            AvailableRowText(lock = lock, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            DelayBadge(seconds = lock.delaySeconds)
        } else {
            UnavailableRowText(lock = lock, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            // K3: drawn only here. An available row hides the explanation behind a long press
            // because it has a tap target; this row has none, so the control is visible.
            TextButton(onClick = onExplainRemoval) {
                Text(
                    text = stringResource(R.string.locks_removal_help),
                    style = SlowLockType.Footnote,
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
            // Resolved fresh on every read, never stored (FR-012, SC-006), so a renamed app shows
            // its new name without anything migrating.
            text = lock.label.orEmpty(),
            style = SlowLockType.RowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // The label yields first: the delay and treatment are what the row is for (FR-017).
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(lock.treatment.labelRes),
            style = SlowLockType.Footnote,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * The delay, at the trailing edge of an available row (contract L6, L7).
 *
 * Trailing, not right: under RTL it moves to the leading edge with everything else.
 *
 * It never shrinks — the badge carries no `weight`, so a long app name ellipsises and the delay
 * stays whole (FR-017).
 *
 * It shows "10s" and says "10 second wait": a screen reader left with the compact form would read
 * "ten s". The spoken form is the same `delay_wait` plural the preview card uses, so a lock is
 * described the same way before and after it is made (FR-018).
 */
@Composable
private fun DelayBadge(seconds: Int, modifier: Modifier = Modifier) {
    val spoken = pluralStringResource(R.plurals.delay_wait, seconds, seconds)
    Text(
        text = stringResource(R.string.locks_delay_badge, seconds),
        style = SlowLockType.Badge,
        // AmberDark on AmberWash (C2): the accent as a word is only ever the dark token.
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
 * Shown, never hidden: the icon may still be sitting on the home screen, and only the user removes
 * either. The package name is the only thing left to identify it by, so the message carries it.
 */
@Composable
private fun UnavailableRowText(lock: Lock, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.locks_unavailable, lock.packageName),
        style = SlowLockType.Body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * The app's icon, or the `Fill` placeholder until it loads — and permanently, for a package that no
 * longer resolves.
 */
@Composable
private fun LockIcon(
    lock: Lock,
    icons: AppIconRepository,
    modifier: Modifier = Modifier,
) {
    var icon by remember(lock.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(lock.packageName, lock.versionCode, lock.isAvailable) {
        // An unresolvable package has no icon to rasterize, and asking would cost a lookup per dead
        // row on every visit for an answer that cannot change until it is reinstalled.
        icon = if (lock.isAvailable) icons.icon(lock.packageName, lock.versionCode) else null
    }
    Box(modifier = modifier.size(ICON_SIZE)) {
        val bitmap = icon
        if (bitmap == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    // Only the placeholder is clipped: a loaded launcher icon already carries its
                    // own mask, and re-masking would shave the adaptive icons meant to be round.
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
 * A deliberate second copy of the mapping `ShortcutConfigScreen` holds privately: N10 confines this
 * feature's change to that file to two lines. Both read the same frozen `shortcut_treatment_*`
 * resources.
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
