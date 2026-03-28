package com.example.babyneedscounter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

private enum class MainSurfacePage(
    val label: String,
) {
    Home("Home"),
    Insights("Insights"),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeInsightsPagerScreen(
    onSettingsClick: () -> Unit,
    widgetOpenTarget: WidgetOpenTarget? = null,
    onWidgetOpenTargetConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsManager = remember { SettingsManager(context) }
    val repository = remember { BabyRepository(context) }
    val googleSheetUrl by settingsManager.googleSheetUrl.collectAsState(initial = "")
    val uiState by repository.uiState.collectAsState()

    var savedPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedPage, pageCount = { MainSurfacePage.entries.size })
    var pagerLocked by remember { mutableStateOf(false) }

    LaunchedEffect(googleSheetUrl) {
        repository.setSourceUrl(googleSheetUrl)
        repository.refresh(RefreshTrigger.Initial, force = true)
    }

    LaunchedEffect(pagerState.currentPage) {
        savedPage = pagerState.currentPage
    }

    LaunchedEffect(widgetOpenTarget) {
        if (widgetOpenTarget != null && pagerState.currentPage != MainSurfacePage.Home.ordinal) {
            pagerState.animateScrollToPage(MainSurfacePage.Home.ordinal)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    repository.refresh(RefreshTrigger.Resume)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Baby Needs",
                                fontWeight = FontWeight.Bold,
                            )
                            formatFreshnessLine(uiState.lastSuccessAt, uiState.isRefreshing)?.let { freshness ->
                                Text(
                                    text = freshness,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    MainSurfacePage.entries.forEachIndexed { index, page ->
                        val icon = if (page == MainSurfacePage.Home) Icons.Default.Home else Icons.Default.BarChart
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(page.label) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = page.label,
                                )
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            beyondViewportPageCount = 1,
            userScrollEnabled = !pagerLocked,
        ) { page ->
            when (MainSurfacePage.entries[page]) {
                MainSurfacePage.Home -> BabyNeedsLogger(
                    repository = repository,
                    modifier = Modifier.fillMaxSize(),
                    snackbarHostState = snackbarHostState,
                    widgetOpenTarget = widgetOpenTarget,
                    onWidgetOpenTargetConsumed = onWidgetOpenTargetConsumed,
                )

                MainSurfacePage.Insights -> InsightsScreen(
                    repository = repository,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.fillMaxSize(),
                    onInteractionLockChanged = { pagerLocked = it },
                )
            }
        }
    }
}

private fun formatFreshnessLine(lastSuccessAt: Long?, isRefreshing: Boolean): String? {
    if (lastSuccessAt == null) return if (isRefreshing) "Refreshing…" else null
    val minutesAgo = ((System.currentTimeMillis() - lastSuccessAt) / 60_000L).coerceAtLeast(0L)
    val freshness = when {
        minutesAgo == 0L -> "Updated just now"
        minutesAgo < 60L -> "Updated ${minutesAgo}m ago"
        else -> "Updated ${minutesAgo / 60L}h ago"
    }
    return if (isRefreshing) "$freshness · Refreshing…" else freshness
}
