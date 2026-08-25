package com.slowlock.shortcut

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.delay.DelayConfig
import com.slowlock.delay.DelayConfigStore
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.components.SelectableTile
import com.slowlock.ui.theme.SlowLockType
import kotlinx.coroutines.launch

/**
 * The last step: choose how the icon looks, then put the lock on the home screen.
 *
 * Feature 004 restyled it. The preview became a card showing the icon, the app's name and the
 * wait it will impose — roughly what the user is about to create — and the three treatments moved
 * from `FilterChip`s to tiles.
 *
 * **The chips carried selection semantics for free, and the tiles must not lose them.** That is
 * the specific regression the swap risked: a hand-rolled tile signalling selection by fill colour
 * alone tells a screen-reader user nothing. `SelectableTile` re-supplies them through
 * `Modifier.selectable` with `Role.RadioButton` (FR-043, contract U4).
 *
 * **The `create` path is untouched by 005** — resolve fresh, save, pin, then `onCreated`, in that
 * order, with the target re-resolved at the moment of the tap because it may have been uninstalled
 * while this screen was open (002, `contracts/screen-inventory.md` S3). Feature 005 briefly wrote a
 * lock record here and then took it back out: a lock is its pinned shortcut (FR-003a), so `pin()`
 * already creates it and there is nothing left for this screen to record. `initialTreatment` is
 * untouched too — it is the app's *saved* treatment loaded by the root, not a default.
 *
 * The title reads "New lock" where the design source still reads "New shortcut": the terminology
 * decision post-dates the artboard, and this is a deliberate divergence, not drift (FR-041).
 */
@Composable
fun ShortcutConfigScreen(
    packageName: String,
    delaySeconds: Int,
    initialTreatment: IconTreatment,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    BackHandler { onBack() }

    val iconCache = remember(context) { AppIconCache(context) }
    val pinner = remember(context) { ShortcutPinner(context) }
    val store = remember(context) { DelayConfigStore(context) }

    val targetState by produceState<TargetState>(TargetState.Resolving, packageName) {
        value = TargetState.Resolving
        value = resolveShortcutTarget(context, packageName)
            ?.let(TargetState::Resolved)
            ?: TargetState.Missing
    }
    val target = (targetState as? TargetState.Resolved)?.target
    val iconState by produceState<IconState>(IconState.Loading, target) {
        value = IconState.Loading
        val resolved = target ?: return@produceState
        value = iconCache.icon(resolved.packageName, resolved.versionCode)
            ?.let(IconState::Loaded)
            ?: IconState.Failed
    }

    var creating by remember { mutableStateOf(false) }
    var treatment by rememberSaveable { mutableStateOf(initialTreatment) }
    val icon = (iconState as? IconState.Loaded)?.bitmap
    val targetUnavailable = stringResource(R.string.shortcut_target_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = SCREEN_PADDING),
        ) {
            // Step 3 of 3 (005 FR-029). The `BackHandler` this screen already had is what
            // FR-030 asks for and is unchanged.
            ScreenHeader(
                title = stringResource(R.string.shortcut_config_title),
                onBack = onBack,
                step = 3,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    targetState is TargetState.Missing -> Message(targetUnavailable)
                    // Resolving: no spinner for a package-manager lookup (002).
                    target == null -> Unit
                    else -> PreviewCard(
                        label = target.label,
                        icon = icon,
                        colorFilter = treatment.previewFilter(),
                        delaySeconds = delaySeconds,
                    )
                }
            }

            if (iconState is IconState.Failed) {
                Message(stringResource(R.string.shortcut_icon_unavailable))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                TreatmentSection(
                    selected = treatment,
                    onSelect = { treatment = it },
                    icon = icon,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrimaryAction(
                        label = stringResource(R.string.shortcut_config_create),
                        enabled = target != null && icon != null && !creating,
                        onClick = {
                            val loaded = icon ?: return@PrimaryAction
                            creating = true
                            scope.launch {
                                create(
                                    context = context,
                                    packageName = packageName,
                                    icon = loaded,
                                    delaySeconds = delaySeconds,
                                    treatment = treatment,
                                    pinner = pinner,
                                    store = store,
                                    onUnavailable = {
                                        creating = false
                                        snackbarHostState.showSnackbar(targetUnavailable)
                                    },
                                    onCreated = onCreated,
                                )
                            }
                        },
                    )
                    Text(
                        text = stringResource(R.string.shortcut_config_confirm_note),
                        style = SlowLockType.Footnote,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Unchanged from feature 003, and deliberately so — feature 005 added a line here and then took
 * it back out, once a lock stopped being a record and became the pinned shortcut itself.
 *
 * The order matters and is the contract: **re-resolve, then save, then pin.** The target may have
 * been uninstalled while this screen sat open, so the resolution at tap time is the one that
 * counts; and the configuration is written before the pin request goes out so that a launcher
 * which pins asynchronously can never fire the shortcut before its delay exists on disk.
 *
 * **Creating a lock is not a write** (FR-003a). A lock exists exactly when its shortcut is pinned,
 * so `pin()` is what creates it — and only if the user accepts the launcher's dialog. Declining
 * leaves nothing behind, which is the whole point: the app never has to guess at an outcome it is
 * never told.
 */
private suspend fun create(
    context: Context,
    packageName: String,
    icon: ImageBitmap,
    delaySeconds: Int,
    treatment: IconTreatment,
    pinner: ShortcutPinner,
    store: DelayConfigStore,
    onUnavailable: suspend () -> Unit,
    onCreated: () -> Unit,
) {
    val fresh = resolveShortcutTarget(context, packageName)
    if (fresh == null) {
        onUnavailable()
        return
    }
    store.save(packageName, DelayConfig(delaySeconds, treatment))
    pinner.pin(fresh, treatment, icon.asAndroidBitmap())
    onCreated()
}

/**
 * The lock as it will look, roughly: the icon, the app's name, and the wait it imposes.
 *
 * The delay line is the reason this is a card rather than a floating icon — it is the only place
 * in the flow where the two choices the user has made are shown together, immediately above the
 * button that makes them permanent.
 */
@Composable
private fun PreviewCard(
    label: String,
    icon: ImageBitmap?,
    colorFilter: ColorFilter?,
    delaySeconds: Int,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(PREVIEW_ICON)) {
            if (icon == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                Image(
                    bitmap = icon,
                    contentDescription = stringResource(R.string.app_list_icon_description, label),
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = label,
            style = SlowLockType.Body,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = PREVIEW_LABEL_WIDTH),
        )
        Text(
            text = pluralStringResource(R.plurals.delay_wait, delaySeconds, delaySeconds),
            style = SlowLockType.Footnote,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * The `ICON` label and the three treatment tiles.
 *
 * A `selectableGroup`, so assistive technology announces this as one single-choice set. Unlike
 * the delay presets, **one tile is always selected here** — there is no "no treatment" state —
 * and `Role.RadioButton` covers both cases without the tile knowing which caller it has.
 *
 * Each swatch is the **target app's own icon** with the treatment's colour matrix applied, not a
 * flat colour block. The design source draws flat blocks because it has no real app to draw; the
 * running app does, and showing it is what makes the choice legible before it is made.
 */
@Composable
private fun TreatmentSection(
    selected: IconTreatment,
    onSelect: (IconTreatment) -> Unit,
    icon: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.shortcut_config_icon_label),
            style = SlowLockType.EyebrowSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconTreatment.entries.forEach { entry ->
                val isSelected = entry == selected
                val name = stringResource(entry.labelRes)
                SelectableTile(
                    selected = isSelected,
                    onClick = { onSelect(entry) },
                    contentDescription = name,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.size(SWATCH)) {
                        if (icon == null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        } else {
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                colorFilter = entry.previewFilter(),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Text(
                        text = name,
                        style = SlowLockType.TileName.copy(
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/** The colour matrix this treatment applies to a preview, or null for [IconTreatment.Original]. */
private fun IconTreatment.previewFilter(): ColorFilter? =
    matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SlowLockType.Body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@get:StringRes
private val IconTreatment.labelRes: Int
    get() = when (this) {
        IconTreatment.Original -> R.string.shortcut_treatment_original
        IconTreatment.Invert -> R.string.shortcut_treatment_invert
        IconTreatment.Gray -> R.string.shortcut_treatment_gray
    }

private sealed interface TargetState {
    data object Resolving : TargetState
    data class Resolved(val target: ShortcutTarget) : TargetState
    data object Missing : TargetState
}

private sealed interface IconState {
    data object Loading : IconState
    data class Loaded(val bitmap: ImageBitmap) : IconState
    data object Failed : IconState
}

private val SCREEN_PADDING = 20.dp
private val PREVIEW_ICON = 96.dp
private val PREVIEW_LABEL_WIDTH = 160.dp
private val SWATCH = 36.dp
