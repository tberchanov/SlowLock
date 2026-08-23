package com.slowlock.shortcut

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowlock.R
import com.slowlock.apps.AppIconCache
import com.slowlock.delay.DelayConfig
import com.slowlock.delay.DelayConfigStore
import kotlinx.coroutines.launch

/**
 * A preview of the shortcut about to be pinned, and the button that pins it.
 *
 * Feature 002 expected this screen to be replaced wholesale by the delay-configuration screen it
 * stood in for. It was not: feature 003 put `DelayConfigScreen` **in front of** it instead, and
 * this screen kept the treatment, the preview, and the pin. What it must not do is compromise
 * `contracts/pinned-shortcut.md`, which is frozen — everything this screen hands to
 * [ShortcutPinner] goes through the pure `shortcutSpec` derivation, so there is no route from
 * here to a shortcut of a different shape.
 *
 * One `String` comes in, exactly as feature 001's `contracts/selection-handoff.md` hands it
 * across. Label, icon, and version code are **re-resolved here** rather than carried, which is
 * obligation C3 of that contract and also what makes FR-015 reachable: the app can be
 * uninstalled while this screen is open.
 *
 * The single exit split in feature 003. [onCreated] is the apply path and [onBack] is both
 * cancel paths — the affordance and the system gesture — because FR-014 makes them lead to
 * different places: back returns to the delay screen with the chosen delay intact, creating
 * returns to the list. Feature 002's rule that the caller "cannot tell which and must not need
 * to" is **narrowed, not reversed**, and its reason is untouched: neither callback says anything
 * to the user, and this screen still cannot tell a honoured pin from a declined one (FR-012,
 * C17, research.md R10). What differs is navigation. Nothing else.
 *
 * [delaySeconds] arrives already chosen and is never edited here — it is written to the store
 * with the treatment on apply (C16). [initialTreatment] is the app's saved treatment, or
 * `Original` for an app with none; the caller decides which, so this screen has no idea whether
 * the app was configured before (C15, FR-013).
 *
 * No ViewModel (research.md R10). The screen holds one async load and one enum.
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

    // FR-021/C13: the system gesture and button behave exactly as the affordance does — the same
    // `onBack`, no separate path that could grow a different meaning. Without this the gesture
    // would leave the app entirely, which is the trap this story exists to close.
    BackHandler { onBack() }

    // Its own instance rather than feature 001's: the ViewModel's cache is not reachable from
    // here without widening `AppListScreen`'s contract, and the tier that matters — the WebP
    // files in cacheDir — is shared anyway, keyed identically (Constitution V). This screen
    // loads exactly one icon, so the second in-memory LruCache holds one entry.
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

    // Guards the window between the tap and `onCreated()`, in which the create path is doing IO.
    // A second tap would issue a second pin request — harmless, since the ID is derived and the
    // second call would simply update in place, but it would also put a second system dialog in
    // front of the user for one deliberate action.
    var creating by remember { mutableStateOf(false) }

    // C15/FR-013: the opening selection is the app's saved treatment, which the caller read
    // before navigating here. `Original` still opens an app that has none — but because the
    // caller passes `DelayConfig.DEFAULT`'s treatment, which is `entries.first()`, so the
    // default and the display order still cannot drift apart (002 FR-006). A Kotlin enum is
    // `Serializable`, which is what the default saver needs to carry the choice through
    // rotation and process death (FR-008, C7).
    var treatment by rememberSaveable { mutableStateOf(initialTreatment) }

    // C5/SC-004: a filter on the already-loaded `ImageBitmap`, never a bitmap baked per tap.
    // Switching treatment recomposes one node and copies no pixels. `Original` carries no
    // matrix, and a null filter means `Image` applies none at all rather than an identity one.
    val previewFilter = remember(treatment) {
        treatment.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }

    val icon = (iconState as? IconState.Loaded)?.bitmap
    val targetUnavailable = stringResource(R.string.shortcut_target_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // C13/FR-020: the affordance and the system gesture are the same exit, and neither
            // creates anything. It stays enabled even while `creating` is true — a back control
            // that silently does nothing is worse than one that cancels, and leaving the screen
            // cancels `scope`, which is the honest reading of "backed out without creating".
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.shortcut_config_back),
                    )
                }
                Text(
                    text = stringResource(R.string.shortcut_config_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            TreatmentRow(
                selected = treatment,
                onSelect = { treatment = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // The app went away before the screen could resolve it. Same outcome as
                    // FR-015, arriving earlier: nothing is created and the user is told.
                    targetState is TargetState.Missing -> Message(targetUnavailable)
                    target == null -> Unit // Resolving: no spinner for a package-manager lookup.
                    else -> Preview(
                        label = target.label,
                        icon = icon,
                        colorFilter = previewFilter,
                    )
                }
            }

            // C12: a pinned shortcut is effectively permanent, so a placeholder icon on the home
            // screen is worse than no shortcut at all and defeats the point of an icon that
            // mirrors the target app. The failure is transient — `AppIconCache` deliberately
            // does not cache failures — so backing out and reopening retries the load.
            if (iconState is IconState.Failed) {
                Message(stringResource(R.string.shortcut_icon_unavailable))
            }

            Button(
                onClick = {
                    val loaded = icon ?: return@Button
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
                enabled = target != null && icon != null && !creating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            ) {
                Text(stringResource(R.string.shortcut_config_create))
            }
        }
    }
}

/**
 * The create path (C8–C11).
 *
 * Re-resolves before pinning, because the target can be uninstalled between the screen opening
 * and this tap (FR-015). On that path nothing is created, the user is told, and **the screen
 * stays open** — the one outcome that is neither [onCreated] nor a back.
 *
 * The pin itself is gated on support being confirmed *at this moment* (FR-013): the user may
 * have changed launcher while the screen was open. When it is not, no shortcut is created and
 * the screen still closes — there is nothing to say here that would not be the confirmation
 * FR-012 forbids, and the root's `ON_START` check is what actually surfaces an unsupported
 * launcher, taking over every screen in the app (FR-029).
 *
 * Nothing is shown on success (FR-012, C9). Confirmation is the launcher's, and the app cannot
 * tell a decline from a success, so it claims neither.
 *
 * The configuration is written **before** the pin is requested (C16, research.md R10). The pin
 * puts a system dialog in front of the user, and a write queued behind it would be a write
 * waiting on a decision it does not depend on; worse, a crash or a kill in between would leave
 * an icon on the home screen whose delay was never saved. In that order the failure mode is the
 * harmless one: a saved configuration with no icon, which the delay screen simply shows again
 * (spec, Accepted limitations).
 *
 * [treatment] is the one showing in the preview. It and the preview's `ColorFilter` are derived
 * from the same [IconTreatment.matrix], which is what makes SC-003 — the icon that lands matches
 * the one previewed — structural rather than a thing to check by eye.
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

    // FR-015, C16: both values, as one record, before the pin request. `packageName` is the key
    // and the only one — never `fresh.label`, which is display text (Constitution V).
    store.save(packageName, DelayConfig(delaySeconds, treatment))

    pinner.pin(fresh, treatment, icon.asAndroidBitmap())
    onCreated()
}

/**
 * The treatment row (C3): exactly [IconTreatment.entries], in declaration order, horizontally
 * scrollable, sitting above the preview it drives.
 *
 * It renders `entries` rather than a hand-written list, so a treatment cannot be added to the
 * enum and forgotten here — and [labelRes]'s exhaustive `when` turns "added a treatment, gave it
 * no name" into a compile error rather than a chip labelled with an enum constant.
 */
@Composable
private fun TreatmentRow(
    selected: IconTreatment,
    onSelect: (IconTreatment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconTreatment.entries.forEach { treatment ->
            FilterChip(
                selected = treatment == selected,
                onClick = { onSelect(treatment) },
                label = { Text(stringResource(treatment.labelRes)) },
            )
        }
    }
}

/**
 * The user-facing name of each treatment.
 *
 * The `when` is exhaustive on purpose: adding a treatment to [IconTreatment] without naming it
 * fails the build here, which is the only place the two can disagree.
 */
@get:StringRes
private val IconTreatment.labelRes: Int
    get() = when (this) {
        IconTreatment.Original -> R.string.shortcut_treatment_original
        IconTreatment.Invert -> R.string.shortcut_treatment_invert
        IconTreatment.Gray -> R.string.shortcut_treatment_gray
    }

/**
 * The shortcut as the home screen will show it: the icon above the label, at roughly
 * home-screen proportions (C2).
 *
 * The label is given a fixed maximum width and two lines, then ellipsized — which is what a
 * launcher does, and keeps a very long label from distorting or resizing the preview (C14).
 *
 * [colorFilter] is the selected treatment, applied to the loaded bitmap rather than baked into a
 * new one (C5, C6). `null` — [IconTreatment.Original] — means no filter is applied at all. The
 * placeholder shown while the icon is missing is deliberately left untreated: there is nothing
 * there to preview, and "Create shortcut" is disabled in that state anyway (C12).
 */
@Composable
private fun Preview(
    label: String,
    icon: ImageBitmap?,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(PREVIEW_ICON_SIZE)) {
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
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = PREVIEW_LABEL_WIDTH),
        )
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Whether the target's display facts are known yet, and whether it still exists. */
private sealed interface TargetState {
    data object Resolving : TargetState
    data class Resolved(val target: ShortcutTarget) : TargetState
    data object Missing : TargetState
}

/**
 * Whether the icon is available. [Failed] is distinct from [Loading] on purpose: it is what
 * disables "Create shortcut" (C12), and a screen that merely never finished loading would leave
 * the button disabled with nothing said.
 */
private sealed interface IconState {
    data object Loading : IconState
    data class Loaded(val bitmap: ImageBitmap) : IconState
    data object Failed : IconState
}

private val PREVIEW_ICON_SIZE = 96.dp
private val PREVIEW_LABEL_WIDTH = 160.dp
