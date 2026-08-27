package com.slowlock.feature.apps.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowlock.R
import com.slowlock.feature.apps.domain.InstalledApp
import com.slowlock.core.domain.AppIconRepository
import com.slowlock.ui.components.ScreenHeader
import com.slowlock.ui.theme.SlowLockType

/**
 * The launchable apps on this profile, one row per package, ordered by collated label.
 *
 * The screen owns no navigation and launches nothing: it reports a selection through
 * [onAppSelected] and stops there (`contracts/selection-handoff.md`).
 *
 * It is step 1 of a three-step flow entered from the Locks screen, so [onBack] has somewhere real
 * to go and `1 / 3` is a claim the app can honour (FR-028, FR-029, contract K5).
 */
@Composable
fun AppListScreen(
    onAppSelected: (packageName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // This entry's `ON_START`, so it also fires when the user pops back into the list from the
    // delay step. That extra enumeration is redundant — nothing can be installed without leaving
    // the app — and is accepted rather than guarded against (research R6).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.refresh() }

    // One-shot messages, collected rather than read off the state (FR-038): consuming a value
    // removes it, so no recomposition can show the same message twice. The event carries a resource
    // id and this resolves it, keeping the state holder free of a `Context` (V3).
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
                .padding(contentPadding),
        ) {
            ScreenHeader(
                title = stringResource(R.string.app_list_title),
                onBack = onBack,
                step = 1,
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
                                icons = viewModel.icons,
                                onClick = { viewModel.onAppTapped(app.packageName, onAppSelected) },
                            )
                            // Rows divide with the Fill token rather than the Line hairline used
                            // for borders elsewhere — deliberate, read off the artboard (C9).
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
 * The search box. Still an `OutlinedTextField` because the component already carries the IME
 * behaviour, the cursor, selection and accessibility; only the container colours and the border are
 * overridden (FR-011).
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
    icons: AppIconRepository,
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
        AppIcon(app = app, icons = icons)
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
    icons: AppIconRepository,
    modifier: Modifier = Modifier,
) {
    var icon by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(app.packageName, app.versionCode) {
        icon = icons.icon(app.packageName, app.versionCode)
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

/** The empty and no-results states. */
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
