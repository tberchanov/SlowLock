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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.theme.SlowLockType

/**
 * The launchable apps on this profile, one row per package, ordered by collated label.
 *
 * The screen owns no navigation and launches nothing: it reports a selection through
 * [onAppSelected] and stops there, which is what keeps swapping the launch for navigation to
 * the future configuration screen a one-line change (`contracts/selection-handoff.md`).
 *
 * Feature 004 restyled it and changed nothing else. The view model, the enumeration, the query,
 * the icon cache and the scroll position that survives a round trip through the delay and icon
 * screens are all untouched (`contracts/screen-inventory.md` S1).
 *
 * **No back control and no step counter**, though the design source draws both. This screen is
 * the app's root in Phase 1, so a back tile would duplicate the system gesture, and a `1 / 3`
 * counter would claim a wizard entered from a Locks screen that does not exist yet. Both arrive
 * in Phase 2 (spec **Out of Scope**, contract U1).
 */
@Composable
fun AppListScreen(
    onAppSelected: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.refresh() }

    state.unavailableAppMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onUnavailableMessageShown()
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
                .padding(contentPadding),
        ) {
            ScreenHeader(
                title = stringResource(R.string.app_list_title),
                onBack = null,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            )
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = HORIZONTAL_PADDING),
                    ) {
                        items(state.visibleApps, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                iconCache = viewModel.iconCache,
                                onClick = { viewModel.onAppTapped(app.packageName, onAppSelected) },
                            )
                            // The design divides rows with the Fill token rather than the Line
                            // hairline used for borders elsewhere — a deliberate difference read
                            // off the artboard, not an oversight (contract C9, S1).
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The search box: 52dp, 14dp corners, card fill, hairline border.
 *
 * Still an `OutlinedTextField` — the design changes how it looks, not what it does, and the
 * component already carries the IME behaviour, the cursor, selection and accessibility. Only the
 * container colours and the border are overridden (FR-011).
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
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 8.dp)
            .height(SEARCH_HEIGHT),
        placeholder = {
            Text(
                text = stringResource(R.string.app_list_search_hint),
                style = SlowLockType.Body,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        textStyle = SlowLockType.Body,
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
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
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, iconCache = iconCache)
        Spacer(Modifier.width(14.dp))
        Text(
            text = app.label,
            style = SlowLockType.RowLabel,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
                    .clip(MaterialTheme.shapes.extraSmall)
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Message(text = stringResource(R.string.app_list_empty), modifier = modifier)
}

@Composable
private fun NoResultsState(query: String, modifier: Modifier = Modifier) {
    Message(text = stringResource(R.string.app_list_no_results, query), modifier = modifier)
}

/** The empty and no-results states. Restyled; wording deliberately unchanged (FR-013). */
@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SlowLockType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ROW_HEIGHT = 64.dp
private val ICON_SIZE = 44.dp
private val SEARCH_HEIGHT = 52.dp
private val HORIZONTAL_PADDING = 20.dp
