package com.example.babyneedscounter

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.BarChart
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.babyneedscounter.ui.theme.BabyNeedsCounterTheme
import com.example.babyneedscounter.ui.theme.CategoryFeeding
import com.example.babyneedscounter.ui.theme.CategoryPee
import com.example.babyneedscounter.ui.theme.CategoryPoop
import com.example.babyneedscounter.ui.theme.CategorySleep
import com.example.babyneedscounter.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ActivityQuickKind {
    POOP_PEE, PEE, FEED
}

private fun ActivityQuickKind.emojiType(): String = when (this) {
    ActivityQuickKind.POOP_PEE -> "💩💧"
    ActivityQuickKind.PEE -> "💧"
    ActivityQuickKind.FEED -> "🐄"
}

private fun ActivityQuickKind.displayName(): String = when (this) {
    ActivityQuickKind.POOP_PEE -> "Poop & Pee"
    ActivityQuickKind.PEE -> "Pee"
    ActivityQuickKind.FEED -> "Feed"
}

private sealed class HomeSheet {
    data class ActivityMenu(val kind: ActivityQuickKind) : HomeSheet()
    data class ActivityTime(val kind: ActivityQuickKind) : HomeSheet()
    data class ActivityNote(val kind: ActivityQuickKind) : HomeSheet()
    data object SleepMenu : HomeSheet()
    data object SleepTime : HomeSheet()
    data object SleepNote : HomeSheet()
}

/** Rich text layout for home logging cards (hierarchy + scanability). */
sealed class HomeCardBodyStyle {
    data class Standard(val subtitle: String) : HomeCardBodyStyle()
    data class Feeding(
        val nextAt: String,
        val nextIn: String,
        val lastAgo: String,
        val todayFeedCount: Int
    ) : HomeCardBodyStyle()
    data class Sleep(val state: String, val duration: String, val previousSleepLine: String) : HomeCardBodyStyle()
    data class Diaper(val line: String) : HomeCardBodyStyle()
}

private fun sleepDurationPrimaryLine(
    todayStats: BackendService.TodayStats?,
    sleeping: Boolean,
    awakeFromLastWake: Boolean,
): String {
    if (todayStats == null) return "—"
    return when {
        sleeping -> todayStats.getSleepDurationLabel()
        awakeFromLastWake -> todayStats.getAwakeDurationLabel()
        else -> "—"
    }
}

private fun formatPreviousSleepLine(previousSleepDurationLabel: String?): String {
    val label = previousSleepDurationLabel?.takeIf { it.isNotBlank() } ?: "—"
    return "Previous sleep cycle: $label"
}

/** One line: today count + last clock time or "none today". */
private fun diaperHomeSummaryLine(todayCount: Int, lastTimeIso: String?, clockLocal: String): String {
    val last = if (lastTimeIso == null) "none today" else clockLocal
    return "Today: $todayCount · Last: $last"
}

/** Second line on Feeding card: small "Last: …" with trailing "ago" when appropriate. */
private fun formatLastFeedSecondaryLine(lastAgo: String): String = when {
    lastAgo == "—" -> "Last: —"
    lastAgo.equals("just now", ignoreCase = true) -> "Last: just now"
    else -> "Last: $lastAgo ago"
}

/** Compact Feeding card metadata: next countdown plus the existing last/today context. */
private fun formatCompactFeedMetaLine(nextIn: String, lastAgo: String, todayFeedCount: Int): String {
    val next = when {
        nextIn == "—" -> "In —"
        nextIn == "Now!" -> "Now"
        else -> "In $nextIn"
    }
    val last = when {
        lastAgo == "—" -> "Last —"
        lastAgo.equals("just now", ignoreCase = true) -> "Last now"
        else -> "Last $lastAgo"
    }
    return "$next · $last · $todayFeedCount today"
}

private fun formatFeedDetailLine(nextIn: String, lastAgo: String, todayFeedCount: Int): String {
    val next = when {
        nextIn == "—" -> "In —"
        nextIn == "Now!" -> "Now"
        else -> "In $nextIn"
    }
    return "$next · ${formatLastFeedSecondaryLine(lastAgo)} · Today: $todayFeedCount feeds"
}

private fun derivePreviousSleepDurationLabel(events: List<BackendService.EventItem>): String? {
    val sleepMarkers = events.mapNotNull { event ->
        val timeMs = StatsAggregation.parseEventTimeMs(event.timestamp) ?: return@mapNotNull null
        when {
            SheetEventMarkers.isSleepStart(event.type) -> timeMs to true
            SheetEventMarkers.isSleepEnd(event.type) -> timeMs to false
            else -> null
        }
    }.sortedBy { it.first }

    var previousSleepMinutes: Int? = null
    for (i in 0 until sleepMarkers.size - 1) {
        val current = sleepMarkers[i]
        val next = sleepMarkers[i + 1]
        if (current.second && !next.second) {
            val diffMinutes = ((next.first - current.first) / 60_000L).toInt()
            if (diffMinutes > 0) previousSleepMinutes = diffMinutes
        }
    }
    return previousSleepMinutes?.let(StatsAggregation::formatShortDurationMinutes)
}

private fun deriveLatestEventIso(events: List<BackendService.EventItem>, marker: String): String? =
    events.firstOrNull { it.type.contains(marker) }?.timestamp

/** Sheet emoji (column B) + short UI label for dialogs — never use legacy text for new logs. */
private fun nextSleepEvent(isSleeping: Boolean): Pair<String, String> =
    if (isSleeping) SheetEventMarkers.SLEEP_ENDED to "wake-up"
    else SheetEventMarkers.SLEEP_STARTED to "sleep start"

private fun buildEventTimestamp(useCustom: Boolean, hour: Int, minute: Int): String {
    return if (useCustom) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(cal.time)
    } else {
        BackendService.getCurrentTimestamp()
    }
}

private suspend fun completeLogAndSnackbar(
    snackbarHostState: SnackbarHostState,
    context: Context,
    repository: BabyRepository,
    googleSheetUrl: String,
    event: BackendService.BabyEvent,
): Boolean {
    if (googleSheetUrl.isEmpty()) {
        HapticFeedback.error(context)
        snackbarHostState.showSnackbar("Please set up your sheet in Settings first")
        return false
    }
    HapticFeedback.mediumImpact(context)
    return try {
        val result = repository.logEvent(event)
        if (result.success) {
            HapticFeedback.success(context)
            val row = result.rowNumber
            val snackResult = snackbarHostState.showSnackbar(
                message = "Saved",
                actionLabel = if (row != null) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (snackResult == SnackbarResult.ActionPerformed && row != null) {
                val deleted = repository.deleteEvents(listOf(row))
                if (deleted) {
                    snackbarHostState.showSnackbar("Undo complete")
                }
            }
            true
        } else {
            HapticFeedback.error(context)
            snackbarHostState.showSnackbar("Couldn't save. Check your connection.")
            false
        }
    } catch (e: Exception) {
        HapticFeedback.error(context)
        snackbarHostState.showSnackbar("Error: ${e.message}")
        false
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BabySyncScheduler.ensureScheduled(this)
        setContent {
            BabyNeedsCounterTheme {
                AppNavigation()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update widgets when app comes to foreground
        updateAllWidgets()
    }
    
    override fun onPause() {
        super.onPause()
        // Update widgets when app goes to background
        updateAllWidgets()
    }
    
    private fun updateAllWidgets() {
        WidgetUpdater.requestUpdateAll(this)
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") {
                HomeInsightsPagerScreen(
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onInsightsClick: () -> Unit,
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val repository = remember { BabyRepository(context) }
    val googleSheetUrl by settingsManager.googleSheetUrl.collectAsState(initial = "")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(googleSheetUrl) {
        repository.setSourceUrl(googleSheetUrl)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onInsightsClick) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Insights and history",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BabyNeedsLogger(
            repository = repository,
            modifier = Modifier.padding(innerPadding),
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyNeedsLogger(
    repository: BabyRepository,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by repository.uiState.collectAsState()
    val googleSheetUrl = uiState.currentSourceUrl
    
    var isLoading by remember { mutableStateOf(false) }
    var homeSheet by remember { mutableStateOf<HomeSheet?>(null) }
    var flashActivity by remember { mutableStateOf<ActivityQuickKind?>(null) }
    var sleepFlash by remember { mutableStateOf(false) }
    var timeHour by remember { mutableIntStateOf(0) }
    var timeMinute by remember { mutableIntStateOf(0) }
    var noteText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sleepDurationTick by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(flashActivity) {
        if (flashActivity != null) {
            kotlinx.coroutines.delay(600)
            flashActivity = null
        }
    }
    
    LaunchedEffect(sleepFlash) {
        if (sleepFlash) {
            kotlinx.coroutines.delay(600)
            sleepFlash = false
        }
    }
    
    LaunchedEffect(homeSheet) {
        when (homeSheet) {
            is HomeSheet.ActivityTime, is HomeSheet.SleepTime -> {
                val c = Calendar.getInstance()
                timeHour = c.get(Calendar.HOUR_OF_DAY)
                timeMinute = c.get(Calendar.MINUTE)
            }
            is HomeSheet.ActivityNote, is HomeSheet.SleepNote -> {
                noteText = ""
            }
            else -> {}
        }
    }
    
    val todayStats = uiState.homeStats
    val isRefreshing = uiState.isRefreshing
    val showInitialHomeState = todayStats == null && !uiState.hasCachedContent
    val sleeping = todayStats?.isSleeping() == true
    LaunchedEffect(todayStats?.lastSleepEventTimeISO, todayStats?.lastSleepEventType) {
        if (todayStats?.lastSleepEventTimeISO != null) {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                sleepDurationTick++
            }
        }
    }
    
    val onQuickFeedTap: () -> Unit = {
        if (!isLoading) {
            scope.launch {
                isLoading = true
                try {
                    val ts = buildEventTimestamp(false, 0, 0)
                    val event = BackendService.BabyEvent(
                        ts,
                        ActivityQuickKind.FEED.emojiType(),
                        ""
                    )
                    val ok = completeLogAndSnackbar(
                        snackbarHostState, context, repository, googleSheetUrl,
                        event
                    )
                    if (ok) flashActivity = ActivityQuickKind.FEED
                } finally {
                    isLoading = false
                }
            }
        }
    }
    val onQuickSleepTap: () -> Unit = {
        if (!isLoading) {
            val (type, _) = nextSleepEvent(sleeping)
            scope.launch {
                isLoading = true
                try {
                    val ts = buildEventTimestamp(false, 0, 0)
                    val event = BackendService.BabyEvent(ts, type, "")
                    val ok = completeLogAndSnackbar(
                        snackbarHostState, context, repository, googleSheetUrl,
                        event
                    )
                    if (ok) sleepFlash = true
                } finally {
                    isLoading = false
                }
            }
        }
    }
    val onQuickPoopTap: () -> Unit = {
        if (!isLoading) {
            scope.launch {
                isLoading = true
                try {
                    val ts = buildEventTimestamp(false, 0, 0)
                    val event = BackendService.BabyEvent(
                        ts,
                        ActivityQuickKind.POOP_PEE.emojiType(),
                        ""
                    )
                    val ok = completeLogAndSnackbar(
                        snackbarHostState, context, repository, googleSheetUrl,
                        event
                    )
                    if (ok) flashActivity = ActivityQuickKind.POOP_PEE
                } finally {
                    isLoading = false
                }
            }
        }
    }
    val onQuickPeeTap: () -> Unit = {
        if (!isLoading) {
            scope.launch {
                isLoading = true
                try {
                    val ts = buildEventTimestamp(false, 0, 0)
                    val event = BackendService.BabyEvent(
                        ts,
                        ActivityQuickKind.PEE.emojiType(),
                        ""
                    )
                    val ok = completeLogAndSnackbar(
                        snackbarHostState, context, repository, googleSheetUrl,
                        event
                    )
                    if (ok) flashActivity = ActivityQuickKind.PEE
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showInitialHomeState) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Text(
                            text = "Loading your latest baby status…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Once the first sync completes, Home will open straight into your last known state.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = if (googleSheetUrl.isBlank()) "Set up your sheet in Settings to start tracking." else "No local data yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (googleSheetUrl.isBlank()) "After setup, the app will cache the latest state for instant startup." else "Pull to refresh or wait for the first sync to finish.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val timeUntilNextFeed = todayStats?.getTimeUntilNextFeed() ?: "—"
                val nextFeedAt = todayStats?.getNextFeedTime() ?: "—"
                val lastFeedAgo = todayStats?.getTimeSinceLastFeed() ?: "—"
                val feedCountToday = todayStats?.feedCount ?: 0
                val peeCount = todayStats?.peeCount ?: 0
                val poopCount = todayStats?.poopCount ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HomeCardListSpacing)
                ) {
                    QuickActionCard(
                        title = "Next feed",
                        bodyStyle = HomeCardBodyStyle.Feeding(
                            nextAt = nextFeedAt,
                            nextIn = timeUntilNextFeed,
                            lastAgo = lastFeedAgo,
                            todayFeedCount = feedCountToday
                        ),
                        icon = "",
                        color = CategoryFeeding,
                        iconDrawableRes = R.drawable.marshmallow_feed,
                        showSuccessFlash = flashActivity == ActivityQuickKind.FEED,
                        enabled = !isLoading,
                        onQuickTap = onQuickFeedTap,
                        onEdit = { homeSheet = HomeSheet.ActivityMenu(ActivityQuickKind.FEED) },
                        modifier = Modifier.fillMaxWidth(),
                        compactList = true,
                        titleTrailing = {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    SleepQuickCard(
                        todayStats = todayStats,
                        sleepDurationTick = sleepDurationTick,
                        showSuccessFlash = sleepFlash,
                        enabled = !isLoading,
                        onQuickTap = onQuickSleepTap,
                        onEdit = { homeSheet = HomeSheet.SleepMenu },
                        onSleepWindowTap = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Sleep window — coming soon")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        compactList = true,
                    )
                    QuickActionCard(
                        title = "Poop",
                            bodyStyle = HomeCardBodyStyle.Diaper(
                                diaperHomeSummaryLine(
                                    poopCount,
                                    todayStats?.lastPoopTimeISO,
                                    todayStats?.getLastPoopClock() ?: "—"
                                )
                            ),
                        icon = "",
                        color = CategoryPoop,
                        iconDrawableRes = R.drawable.marshmallow_poop,
                        showSuccessFlash = flashActivity == ActivityQuickKind.POOP_PEE,
                        enabled = !isLoading,
                        onQuickTap = onQuickPoopTap,
                        onEdit = { homeSheet = HomeSheet.ActivityMenu(ActivityQuickKind.POOP_PEE) },
                        modifier = Modifier.fillMaxWidth(),
                        compactList = true
                    )
                    QuickActionCard(
                        title = "Pee",
                            bodyStyle = HomeCardBodyStyle.Diaper(
                                diaperHomeSummaryLine(
                                    peeCount,
                                    todayStats?.lastPeeTimeISO,
                                    todayStats?.getLastPeeClock() ?: "—"
                                )
                            ),
                        icon = "",
                        color = CategoryPee,
                        iconDrawableRes = R.drawable.marshmallow_pee,
                        showSuccessFlash = flashActivity == ActivityQuickKind.PEE,
                        enabled = !isLoading,
                        onQuickTap = onQuickPeeTap,
                        onEdit = { homeSheet = HomeSheet.ActivityMenu(ActivityQuickKind.PEE) },
                        modifier = Modifier.fillMaxWidth(),
                        compactList = true
                    )
                }
            }
        }
    }
    
    if (homeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { homeSheet = null },
            sheetState = sheetState
        ) {
            when (val s = homeSheet!!) {
                is HomeSheet.ActivityMenu -> {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            "More options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        ListItem(
                            headlineContent = { Text("🕒 Log from different time") },
                            modifier = Modifier.clickable {
                                homeSheet = HomeSheet.ActivityTime(s.kind)
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("📝 Add note") },
                            modifier = Modifier.clickable {
                                homeSheet = HomeSheet.ActivityNote(s.kind)
                            }
                        )
                    }
                }
                is HomeSheet.ActivityTime -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Log ${s.kind.displayName()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberPicker(
                                value = timeHour,
                                range = 0..23,
                                onValueChange = { timeHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                ":",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            NumberPicker(
                                value = timeMinute,
                                range = 0..59,
                                onValueChange = { timeMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val ts = buildEventTimestamp(true, timeHour, timeMinute)
                                        val event = BackendService.BabyEvent(
                                            ts,
                                            s.kind.emojiType(),
                                            ""
                                        )
                                        val ok = completeLogAndSnackbar(
                                            snackbarHostState, context, repository,
                                            googleSheetUrl, event
                                        )
                                        if (ok) {
                                            flashActivity = s.kind
                                            homeSheet = null
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
                is HomeSheet.ActivityNote -> {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Note for ${s.kind.displayName()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note") },
                            singleLine = false,
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val ts = buildEventTimestamp(false, 0, 0)
                                        val event = BackendService.BabyEvent(
                                            ts,
                                            s.kind.emojiType(),
                                            noteText.trim()
                                        )
                                        val ok = completeLogAndSnackbar(
                                            snackbarHostState, context, repository,
                                            googleSheetUrl, event
                                        )
                                        if (ok) {
                                            flashActivity = s.kind
                                            homeSheet = null
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
                HomeSheet.SleepMenu -> {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            "More options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        ListItem(
                            headlineContent = { Text("🕒 Log from different time") },
                            modifier = Modifier.clickable { homeSheet = HomeSheet.SleepTime }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("📝 Add note") },
                            modifier = Modifier.clickable { homeSheet = HomeSheet.SleepNote }
                        )
                    }
                }
                HomeSheet.SleepTime -> {
                    val (nextEmoji, logLabel) = nextSleepEvent(sleeping)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Log $logLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberPicker(
                                value = timeHour,
                                range = 0..23,
                                onValueChange = { timeHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                ":",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            NumberPicker(
                                value = timeMinute,
                                range = 0..59,
                                onValueChange = { timeMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val ts = buildEventTimestamp(true, timeHour, timeMinute)
                                        val event = BackendService.BabyEvent(ts, nextEmoji, "")
                                        val ok = completeLogAndSnackbar(
                                            snackbarHostState, context, repository,
                                            googleSheetUrl, event
                                        )
                                        if (ok) {
                                            sleepFlash = true
                                            homeSheet = null
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
                HomeSheet.SleepNote -> {
                    val (nextEmoji, noteLabel) = nextSleepEvent(sleeping)
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Note for $noteLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note") },
                            singleLine = false,
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val ts = buildEventTimestamp(false, 0, 0)
                                        val event = BackendService.BabyEvent(
                                            ts,
                                            nextEmoji,
                                            noteText.trim()
                                        )
                                        val ok = completeLogAndSnackbar(
                                            snackbarHostState, context, repository,
                                            googleSheetUrl, event
                                        )
                                        if (ok) {
                                            sleepFlash = true
                                            homeSheet = null
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

/** Non-compact home cards: mascot column height (primary visual; no extra layers). */
private val QuickLogIconSize = 96.dp

/** Compact list: dominant mascot column (fills most of row height on typical two-line cards). */
private val QuickLogIconSizeCompact = 96.dp

/** Home quick-log cards (compact): tight padding so large icons still fit without growing the card much. */
private val CompactCardPaddingStart = 12.dp
private val CompactCardPaddingEnd = 6.dp
private val CompactCardPaddingVertical = 12.dp
private val HomeCardListSpacing = 16.dp
private val AvatarMaskShape = RoundedCornerShape(40)

/**
 * Home card: [icon column | text column] (tap to log) + right-aligned edit hit target, vertically centered.
 * Optional [onLongClickPrimary] (e.g. Sleep → sleep window placeholder): long-press on the main tap area; tap still logs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickLogCardLayout(
    iconEmoji: String,
    /** When set, shown instead of [iconEmoji] (e.g. Sleep card marshmallow art). */
    iconDrawableRes: Int? = null,
    accentColor: Color,
    title: String,
    bodyStyle: HomeCardBodyStyle,
    showSuccessFlash: Boolean,
    onQuickTap: () -> Unit,
    onEdit: () -> Unit,
    onLongClickPrimary: (() -> Unit)? = null,
    enabled: Boolean,
    animationLabel: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    compactList: Boolean = false,
    titleTrailing: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val padStart = if (compactList) CompactCardPaddingStart else 16.dp
    val padEnd = if (compactList) CompactCardPaddingEnd else 10.dp
    val padTop = if (compactList) CompactCardPaddingVertical else 12.dp
    val padBottom = if (compactList) CompactCardPaddingVertical else 12.dp
    val iconSize = if (compactList) QuickLogIconSizeCompact else QuickLogIconSize
    val compactFeedLayout = compactList && bodyStyle is HomeCardBodyStyle.Feeding
    val iconColumnWidth = when {
        compactFeedLayout -> 94.dp
        compactList -> 102.dp
        else -> QuickLogIconSize + 8.dp
    }
    val emojiSp = if (compactList) 46.sp else 40.sp
    val titleSp = if (compactList) 13.sp else 18.sp
    val titleLineH = if (compactList) 14.sp else 20.sp
    val gapIconToText = if (compactFeedLayout) 8.dp else if (compactList) 10.dp else 14.dp
    val feedPrimary = if (compactList) 30.sp else 20.sp
    val feedPrimaryLineH = if (compactList) 32.sp else 24.sp
    val feedSecondary = if (compactList) 12.sp else 13.sp
    val feedSecondaryLineH = if (compactList) 13.sp else 15.sp
    val sleepStateSp = if (compactList) 12.sp else 16.sp
    val sleepStateLineH = if (compactList) 13.sp else 17.sp
    val sleepDurationSp = if (compactList) 32.sp else 26.sp
    val sleepDurationLineH = if (compactList) 34.sp else 28.sp
    val sleepMetaSp = if (compactList) 11.sp else 13.sp
    val sleepMetaLineH = if (compactList) 12.sp else 14.sp
    val diaperLineSp = if (compactList) 14.sp else 16.sp
    val diaperLineH = if (compactList) 15.sp else 17.sp
    val standardSubSp = if (compactList) 14.sp else 14.sp
    val hideTitleRow = compactList && when (bodyStyle) {
        is HomeCardBodyStyle.Sleep, is HomeCardBodyStyle.Diaper -> true
        else -> false
    }
    val scale by animateFloatAsState(
        targetValue = if (showSuccessFlash) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = animationLabel
    )
    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 5.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = padStart, end = padEnd, top = padTop, bottom = padBottom),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onLongClickPrimary != null) {
                                Modifier.combinedClickable(
                                    enabled = enabled,
                                    onLongClickLabel = "Sleep window",
                                    onLongClick = {
                                        HapticFeedback.mediumImpact(context)
                                        onLongClickPrimary()
                                    },
                                    onClick = {
                                        HapticFeedback.lightTap(context)
                                        onQuickTap()
                                    }
                                )
                            } else {
                                Modifier.clickable(enabled = enabled) {
                                    HapticFeedback.lightTap(context)
                                    onQuickTap()
                                }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(iconColumnWidth)
                            .height(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        val drawableId = iconDrawableRes
                        if (drawableId != null) {
                            Image(
                                painter = painterResource(drawableId),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (compactList) 4.dp else 6.dp)
                                    .clip(AvatarMaskShape)
                                    .background(Color.Transparent),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = iconEmoji,
                                fontSize = emojiSp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(gapIconToText))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!hideTitleRow) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = if (compactFeedLayout) 12.sp else titleSp,
                                    lineHeight = if (compactFeedLayout) 13.sp else titleLineH,
                                    fontWeight = if (compactFeedLayout) FontWeight.Medium else FontWeight.Bold,
                                    color = if (compactFeedLayout) TextSecondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                titleTrailing?.invoke()
                            }
                        }
                        when (bodyStyle) {
                            is HomeCardBodyStyle.Feeding -> {
                                if (compactFeedLayout) {
                                    Text(
                                        text = bodyStyle.nextAt,
                                        fontSize = feedPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = feedPrimaryLineH,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatCompactFeedMetaLine(
                                            nextIn = bodyStyle.nextIn,
                                            lastAgo = bodyStyle.lastAgo,
                                            todayFeedCount = bodyStyle.todayFeedCount
                                        ),
                                        fontSize = feedSecondary,
                                        lineHeight = feedSecondaryLineH,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = bodyStyle.nextAt,
                                        fontSize = feedPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = feedPrimaryLineH,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatFeedDetailLine(
                                            nextIn = bodyStyle.nextIn,
                                            lastAgo = bodyStyle.lastAgo,
                                            todayFeedCount = bodyStyle.todayFeedCount
                                        ),
                                        fontSize = feedSecondary,
                                        lineHeight = feedSecondaryLineH,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            is HomeCardBodyStyle.Sleep -> {
                                Text(
                                    text = bodyStyle.state,
                                    fontSize = sleepStateSp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = sleepStateLineH,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = bodyStyle.duration,
                                    fontSize = sleepDurationSp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = sleepDurationLineH,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bodyStyle.previousSleepLine,
                                    fontSize = sleepMetaSp,
                                    lineHeight = sleepMetaLineH,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            is HomeCardBodyStyle.Diaper -> {
                                Text(
                                    text = bodyStyle.line,
                                    fontSize = diaperLineSp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = diaperLineH,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            is HomeCardBodyStyle.Standard -> {
                                Text(
                                    text = bodyStyle.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = standardSubSp,
                                    lineHeight = 16.sp,
                                    color = TextSecondary,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(if (compactList) 6.dp else 8.dp))
                Box(
                    modifier = Modifier
                        .size(if (compactList) 50.dp else 48.dp)
                        .clickable(enabled = enabled) {
                            HapticFeedback.lightTap(context)
                            onEdit()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "More options",
                        modifier = Modifier.size(if (compactList) 28.dp else 24.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
                    )
                }
            }
            if (showSuccessFlash) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    bodyStyle: HomeCardBodyStyle,
    icon: String,
    color: Color,
    showSuccessFlash: Boolean,
    onQuickTap: () -> Unit,
    onEdit: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    compactList: Boolean = false,
    titleTrailing: (@Composable () -> Unit)? = null,
    /** When set, shown instead of [icon] (e.g. Feeding cow art). */
    iconDrawableRes: Int? = null,
) {
    QuickLogCardLayout(
        iconEmoji = icon,
        iconDrawableRes = iconDrawableRes,
        accentColor = color,
        title = title,
        bodyStyle = bodyStyle,
        showSuccessFlash = showSuccessFlash,
        onQuickTap = onQuickTap,
        onEdit = onEdit,
        onLongClickPrimary = null,
        enabled = enabled,
        animationLabel = "quick_card_flash",
        modifier = modifier,
        compactList = compactList,
        titleTrailing = titleTrailing,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SleepQuickCard(
    todayStats: BackendService.TodayStats?,
    sleepDurationTick: Int,
    showSuccessFlash: Boolean,
    onQuickTap: () -> Unit,
    onEdit: () -> Unit,
    /** Long-press on card body — sleep window flow (TBD). Tap still quick-logs sleep/wake. */
    onSleepWindowTap: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    compactList: Boolean = false,
) {
    val sleeping = todayStats?.isSleeping() == true
    val awakeFromLastWake = todayStats?.isAwakeFromLastSleepEvent() == true
    val bodyStyle = remember(sleepDurationTick, sleeping, awakeFromLastWake, todayStats) {
        val state = if (sleeping) "Sleeping" else "Awake"
        HomeCardBodyStyle.Sleep(
            state = state,
            duration = sleepDurationPrimaryLine(todayStats, sleeping, awakeFromLastWake),
            previousSleepLine = formatPreviousSleepLine(todayStats?.previousSleepDurationLabel)
        )
    }
    QuickLogCardLayout(
        iconEmoji = "",
        iconDrawableRes = if (sleeping) {
            R.drawable.marshmallow_sleep
        } else {
            R.drawable.marshmallow_awake
        },
        accentColor = CategorySleep,
        title = "Sleep",
        bodyStyle = bodyStyle,
        showSuccessFlash = showSuccessFlash,
        onQuickTap = onQuickTap,
        onEdit = onEdit,
        onLongClickPrimary = onSleepWindowTap,
        enabled = enabled,
        animationLabel = "sleep_card_flash",
        modifier = modifier,
        compactList = compactList,
    )
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    
    // Create a scrollable state
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll to initial value centered on screen (only once on mount)
    LaunchedEffect(Unit) {
        val targetIndex = value - range.first // No +1 needed with contentPadding approach
        listState.scrollToItem(targetIndex)
    }
    
    // Track if we're currently scrolling
    var wasScrolling by remember { mutableStateOf(false) }
    
    // Only update value when scroll actually stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (wasScrolling && !listState.isScrollInProgress) {
            // Scrolling just stopped - determine centered item
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val centerY = layoutInfo.viewportEndOffset / 2
                
                val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - centerY)
                }
                
                centerItem?.let { item ->
                    // Calculate the number (no padding adjustment needed with contentPadding)
                    val numberIndex = item.index
                    if (numberIndex >= 0 && numberIndex < range.count()) {
                        val newValue = range.first + numberIndex
                        if (newValue != value) {
                            onValueChange(newValue)
                        }
                        // Snap to center
                        listState.animateScrollToItem(item.index)
                    }
                }
            }
        }
        wasScrolling = listState.isScrollInProgress
    }
    
    Box(
        modifier = modifier
            .height(120.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(itemHeight)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                )
        )
        
        // Scrollable list with content padding to center items
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 40.dp)
        ) {
            // Numbers (no manual padding needed)
            items(range.count()) { index ->
                val number = range.first + index
                val isSelected = number == value
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", number),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        },
                        fontSize = if (isSelected) 32.sp else 24.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BabyNeedsLoggerPreview() {
    BabyNeedsCounterTheme {
        Text(
            text = "Preview unavailable for repository-backed Home screen",
            modifier = Modifier.padding(24.dp)
        )
    }
}
