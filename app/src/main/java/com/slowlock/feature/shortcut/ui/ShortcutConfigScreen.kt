package com.slowlock.feature.shortcut.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowlock.R
import com.slowlock.core.domain.IconTreatment
import com.slowlock.ui.components.PrimaryAction
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.components.SelectableTile
import com.slowlock.ui.theme.SlowLockType

/**
 * The last step: choose how the icon looks, then put the lock on the home screen.
 *
 * The treatment tiles are not `FilterChip`s but must not lose what those carried for free: a
 * hand-rolled tile signalling selection by fill colour alone tells a screen-reader user nothing.
 * `SelectableTile` re-supplies the semantics through `Modifier.selectable` with `Role.RadioButton`
 * (FR-043, contract U4).
 *
 * The `create` path is resolve fresh, save, pin, then `onCreated`, in that order — the target is
 * re-resolved at the moment of the tap because it may have been uninstalled while this screen was
 * open (contract S3). Nothing writes a lock record: a lock *is* its pinned shortcut (FR-003a), so
 * `pin()` already creates it. `initialTreatment` is the app's *saved* treatment loaded by the root,
 * not a default.
 *
 * The title reads "New lock" where the design source reads "New shortcut": the terminology decision
 * post-dates the artboard, so this is a deliberate divergence, not drift (FR-041).
 */
@Composable
fun ShortcutConfigScreen(
    packageName: String,
    delaySeconds: Int,
    initialTreatment: IconTreatment,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShortcutConfigViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    BackHandler { onBack() }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(packageName) { viewModel.start(packageName) }

    /*
     * The treatment selection stays in `rememberSaveable` deliberately: its lifetime is specified
     * behaviour. The root drops `CONFIG_KEY` from its `SaveableStateHolder` on every exit from the
     * flow (N3), so a saveable inside that holder survives rotation and process death and dies when
     * the flow is left — exactly what stops an abandoned choice reappearing for a different app.
     *
     * A `hiltViewModel()` here would be scoped to the Activity's store, outlive the exit, and carry
     * the abandoned treatment forward; keying a reset on the package would instead lose the choice
     * across process death (FR-023a is the precedent).
     */
    var treatment by rememberSaveable { mutableStateOf(initialTreatment) }

    val target = state.target
    val icon = state.icon

    // One-shot messages, collected rather than read off the state (FR-038): consuming a value
    // removes it, so a recomposition cannot show the same message twice. The event carries a
    // resource id and this resolves it, keeping the state holder free of a `Context` (V3).
    val resources = LocalContext.current.resources
    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(resources.getString(messageRes))
        }
    }

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
                    state.missing -> Message(stringResource(R.string.shortcut_target_unavailable))
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

            if (state.iconFailed) {
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
                        enabled = state.canCreate,
                        onClick = {
                            viewModel.create(
                                packageName = packageName,
                                delaySeconds = delaySeconds,
                                treatment = treatment,
                                onCreated = onCreated,
                            )
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
 * The lock as it will look, roughly: the icon, the app's name, and the wait it imposes.
 *
 * The delay line is the reason this is a card rather than a floating icon — the only place in the
 * flow where both choices are shown together, immediately above the button that makes them
 * permanent.
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
 * The `ICON` label and the three treatment tiles, in a `selectableGroup` so assistive technology
 * announces one single-choice set. Unlike the delay presets, one tile is always selected here.
 *
 * Each swatch is the target app's own icon with the treatment's colour matrix applied, not a flat
 * colour block: the design source draws flat blocks because it has no real app to draw.
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

private val SCREEN_PADDING = 20.dp
private val PREVIEW_ICON = 96.dp
private val PREVIEW_LABEL_WIDTH = 160.dp
private val SWATCH = 36.dp
