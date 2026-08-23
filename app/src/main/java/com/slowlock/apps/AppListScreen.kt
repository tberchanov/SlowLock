package com.slowlock.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slowlock.R

/**
 * The launchable apps on this profile, one row per package, ordered by collated label.
 *
 * The screen owns no navigation and launches nothing: it reports a selection through
 * [onAppSelected] and stops there, which is what keeps swapping the launch for navigation to
 * the future configuration screen a one-line change (`contracts/selection-handoff.md`).
 */
@Composable
fun AppListScreen(
    onAppSelected: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Hoisted above the display-state `when` on purpose: the LazyColumn leaves composition
    // whenever the loading, empty, or no-results branch takes over, and a state remembered
    // inside that branch would lose the scroll offset every time the query stopped matching.
    // Saveable too, so the offset survives the tap-and-return round trip — which backgrounds
    // the process and so exercises FR-017 harder than rotation does — alongside the
    // ViewModel's list and its SavedStateHandle-backed query (manual cases T1.13, T1.16).
    val listState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Re-read on every entry, rather than observing package broadcasts (FR-013, research.md R9).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.refresh() }

    // Keyed by the message, so a second unavailable tap raises it again rather than being
    // swallowed as "no change" (FR-014).
    state.unavailableAppMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onUnavailableMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Only offered once there is something to narrow: a search field over a spinner or
            // over an empty list is a control that cannot do anything.
            if (!state.isLoading && state.apps.isNotEmpty()) {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChanged,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> LoadingState()
                    state.isEmpty -> EmptyState()
                    state.hasNoResults -> NoResultsState(query = state.query)
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.visibleApps, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                iconCache = viewModel.iconCache,
                                // The ViewModel decides whether the package still resolves; only
                                // then does the selection cross the seam (FR-014, contract P2).
                                onClick = { viewModel.onAppTapped(app.packageName, onAppSelected) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Type-to-filter over the loaded list (FR-007). No debounce: filtering ~150 in-memory entries
 * is sub-millisecond, so every keystroke can narrow the list directly (research.md R4).
 *
 * The trailing affordance restores the full list in its collated order (FR-008). It is a glyph
 * rather than a Material icon because `material-icons` is not a dependency of this project and
 * adding one for a single "✕" is not justified (Constitution II).
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.app_list_search_hint)) },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                val clearLabel = stringResource(R.string.app_list_clear_query)
                IconButton(onClick = { onQueryChange("") }) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { contentDescription = clearLabel },
                    )
                }
            }
        },
    )
}

/**
 * Fixed height and a fixed icon box, so a row never changes size once its icon resolves and
 * scrolling cannot jump (SC-003). Long labels ellipsize rather than reflow.
 */
@Composable
private fun AppRow(
    app: InstalledApp,
    iconCache: AppIconCache,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, iconCache = iconCache)
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Loads this row's icon lazily. The effect is keyed by the cache key's two halves and is
 * cancelled with the row's composition, so scrolling past an entry stops its work (FR-011).
 *
 * A failure leaves the placeholder in place; the row stays selectable (FR-016).
 */
@Composable
private fun AppIcon(
    app: InstalledApp,
    iconCache: AppIconCache,
    modifier: Modifier = Modifier,
) {
    var icon by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName, app.versionCode) {
        icon = iconCache.icon(app)
    }

    Box(modifier = modifier.size(ICON_SIZE)) {
        val bitmap = icon
        if (bitmap == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.app_list_icon_description, app.label),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_list_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Shown when apps exist but the query matches none of them (FR-006). Deliberately worded
 * differently from [EmptyState] and naming the query, so "your filter found nothing" is never
 * mistaken for "this device has no apps".
 */
@Composable
private fun NoResultsState(query: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_list_no_results, query),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ROW_HEIGHT = 64.dp
private val ICON_SIZE = 48.dp
