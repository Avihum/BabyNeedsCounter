package com.example.babyneedscounter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.babyneedscounter.ui.theme.CategoryFeeding
import com.example.babyneedscounter.ui.theme.CategoryPee
import com.example.babyneedscounter.ui.theme.CategoryPoop
import com.example.babyneedscounter.ui.theme.CategorySleep
import com.example.babyneedscounter.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class InsightsDay { Today, Yesterday }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InsightsScreen(
    repository: BabyRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onInteractionLockChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by repository.uiState.collectAsState()
    val todayListState = rememberLazyListState()
    val yesterdayListState = rememberLazyListState()

    var selectedEventRows by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<BackendService.EventItem?>(null) }
    var savedDayPage by rememberSaveable { mutableIntStateOf(0) }
    val dayPagerState = rememberPagerState(initialPage = savedDayPage, pageCount = { InsightsDay.entries.size })

    LaunchedEffect(isSelectionMode, editingEvent) {
        onInteractionLockChanged(isSelectionMode || editingEvent != null)
    }

    DisposableEffect(Unit) {
        onDispose {
            onInteractionLockChanged(false)
        }
    }

    LaunchedEffect(uiState.recentEvents) {
        selectedEventRows = selectedEventRows.filterTo(mutableSetOf()) { row ->
            uiState.recentEvents.any { it.rowNumber == row }
        }
        if (selectedEventRows.isEmpty()) {
            isSelectionMode = false
        }
    }

    LaunchedEffect(dayPagerState.currentPage) {
        savedDayPage = dayPagerState.currentPage
        if (selectedEventRows.isNotEmpty()) {
            selectedEventRows = emptySet()
            isSelectionMode = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isRefreshing && !uiState.hasCachedContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Insights…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                InsightsHeader(
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedEventRows.size,
                    onClearSelection = {
                        isSelectionMode = false
                        selectedEventRows = emptySet()
                    },
                    onDeleteSelected = {
                        scope.launch {
                            val success = repository.deleteEvents(selectedEventRows.toList())
                            if (success) {
                                HapticFeedback.success(context)
                                snackbarHostState.showSnackbar("Deleted ${selectedEventRows.size} event(s)")
                                selectedEventRows = emptySet()
                                isSelectionMode = false
                            } else {
                                HapticFeedback.error(context)
                                snackbarHostState.showSnackbar("Failed to delete events")
                            }
                        }
                    },
                    onManualRefresh = {
                        scope.launch {
                            val result = repository.refresh(RefreshTrigger.Manual, force = true)
                            if (!result.success) {
                                snackbarHostState.showSnackbar(result.errorMessage ?: "Refresh failed")
                            }
                        }
                    },
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )

                TabRow(
                    selectedTabIndex = dayPagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InsightsDay.entries.forEachIndexed { index, day ->
                        Tab(
                            selected = dayPagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    dayPagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(if (day == InsightsDay.Today) "Today" else "Yesterday") }
                        )
                    }
                }

                HorizontalPager(
                    state = dayPagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isSelectionMode && editingEvent == null,
                    beyondViewportPageCount = 1,
                ) { page ->
                    val day = InsightsDay.entries[page]
                    InsightsDayPage(
                        day = day,
                        allEvents = uiState.recentEvents,
                        selectedEventRows = selectedEventRows,
                        isSelectionMode = isSelectionMode,
                        onSelectEvent = { rowNumber ->
                            HapticFeedback.lightTap(context)
                            selectedEventRows = if (selectedEventRows.contains(rowNumber)) {
                                selectedEventRows - rowNumber
                            } else {
                                selectedEventRows + rowNumber
                            }
                        },
                        onStartSelection = { rowNumber ->
                            HapticFeedback.mediumImpact(context)
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedEventRows = setOf(rowNumber)
                            }
                        },
                        onEditEvent = { event ->
                            HapticFeedback.lightTap(context)
                            editingEvent = event
                        },
                        listState = if (day == InsightsDay.Today) todayListState else yesterdayListState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    editingEvent?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { editingEvent = null },
            onSave = { updatedEvent ->
                scope.launch {
                    val success = repository.updateEvent(updatedEvent)
                    if (success) {
                        HapticFeedback.success(context)
                        snackbarHostState.showSnackbar("Event updated")
                        editingEvent = null
                    } else {
                        HapticFeedback.error(context)
                        snackbarHostState.showSnackbar("Failed to update event")
                    }
                }
            }
        )
    }
}

@Composable
private fun InsightsHeader(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onManualRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isSelectionMode) "$selectedCount selected" else "Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onManualRefresh, enabled = !isRefreshing) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            if (isSelectionMode) {
                IconButton(onClick = onDeleteSelected, enabled = selectedCount > 0) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Selected",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Done")
                }
            }
        }
    }
}

@Composable
private fun InsightsDayPage(
    day: InsightsDay,
    allEvents: List<BackendService.EventItem>,
    selectedEventRows: Set<Int>,
    isSelectionMode: Boolean,
    onSelectEvent: (Int) -> Unit,
    onStartSelection: (Int) -> Unit,
    onEditEvent: (BackendService.EventItem) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val nowMs = System.currentTimeMillis()
    val nowMinute = nowMs / 60_000L
    val currentBabyDayStart = BabyDay.babyDayStartContaining(nowMs, zone)
    val anchorBabyDayStart = when (day) {
        InsightsDay.Today -> currentBabyDayStart
        InsightsDay.Yesterday -> BabyDay.previousBabyDayStartMs(currentBabyDayStart, zone)
    }
    val compareBabyDayStart = BabyDay.previousBabyDayStartMs(anchorBabyDayStart, zone)

    val bundle = remember(allEvents, anchorBabyDayStart, day, nowMinute) {
        StatsAggregation.insightBundleForBabyDay(
            allEvents,
            anchorBabyDayStart,
            zone,
            capFeedDiaperLogAtNow = day == InsightsDay.Today,
        )
    }
    val compareBundle = remember(allEvents, compareBabyDayStart) {
        StatsAggregation.insightBundleForBabyDay(
            allEvents,
            compareBabyDayStart,
            zone,
            capFeedDiaperLogAtNow = false,
        )
    }
    val displayedRows = remember(allEvents, anchorBabyDayStart, currentBabyDayStart, nowMinute) {
        StatsAggregation.insightsRowsForBabyDay(
            allEvents,
            anchorBabyDayStart,
            zone,
            capAtNow = anchorBabyDayStart == currentBabyDayStart,
            nowMs = nowMs,
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (displayedRows.isEmpty()) {
            item {
                EmptyInsightsCard()
            }
        } else {
            items(displayedRows, key = { it.stableKey }) { row ->
                when (row) {
                    is InsightsRow.RawEvent -> EventCard(
                        event = row.event,
                        isSelected = selectedEventRows.contains(row.event.rowNumber),
                        isSelectionMode = isSelectionMode,
                        onSelect = { onSelectEvent(row.event.rowNumber) },
                        onLongPress = { onStartSelection(row.event.rowNumber) },
                        onEdit = { onEditEvent(row.event) }
                    )

                    is InsightsRow.CompletedSleepSession -> CompletedSleepSessionCard(row)
                    is InsightsRow.ActiveSleep -> ActiveSleepCard(row)
                }
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        item {
            val vsLabel = if (day == InsightsDay.Today) "vs yesterday" else "vs prior day"
            val sleepDelta = StatsAggregation.formatSleepDeltaMinutes(
                bundle.sleep.totalSleepMinutes,
                compareBundle.sleep.totalSleepMinutes,
            )
            val feedDelta = StatsAggregation.formatCountDelta(bundle.feed.feedCount, compareBundle.feed.feedCount)
            val diaperDelta = StatsAggregation.formatCountDelta(bundle.diaper.diaperCount, compareBundle.diaper.diaperCount)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Sleep: $sleepDelta $vsLabel", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Feeds: $feedDelta $vsLabel", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Diapers: $diaperDelta $vsLabel", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
        item {
            InsightSummarySection(
                title = "Sleep",
                lines = buildList {
                    add("Total sleep: ${bundle.sleep.totalSleepMinutes?.let(StatsAggregation::formatShortDurationMinutes) ?: "—"}")
                    add("Longest stretch: ${bundle.sleep.longestSleepMinutes?.let(StatsAggregation::formatShortDurationMinutes) ?: "—"}")
                    add(
                        "Wake windows: ${
                            if (bundle.sleep.wakeWindowSummaries.isEmpty()) "—"
                            else bundle.sleep.wakeWindowSummaries.joinToString()
                        }"
                    )
                }
            )
        }
        item {
            InsightSummarySection(
                title = "Feeding",
                lines = buildList {
                    add("Feeds logged: ${bundle.feed.feedCount}")
                    if (bundle.feed.intervalMinutesBetweenFeeds.isNotEmpty()) {
                        val avg = bundle.feed.intervalMinutesBetweenFeeds.average().toInt()
                        add("Avg. interval: ${StatsAggregation.formatAverageFeedIntervalMinutes(avg)}")
                    } else {
                        add("Intervals: — (need 2+ feeds)")
                    }
                }
            )
        }
        item {
            InsightSummarySection(
                title = "Diapers",
                lines = buildList {
                    add("Changes: ${bundle.diaper.diaperCount}")
                    add("Pee only: ${bundle.diaper.peeOnlyCount} · With poop: ${bundle.diaper.poopRelatedCount}")
                }
            )
        }
    }
}

@Composable
private fun EmptyInsightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No events for this day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Log from Home and the day view will populate here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InsightSummarySection(
    title: String,
    lines: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CompletedSleepSessionCard(row: InsightsRow.CompletedSleepSession) {
    val startTime = remember(row.startEvent.timestamp) {
        ServerDateTimes.formatClock(row.startEvent.timestamp)
    }
    val endTime = remember(row.endEvent.timestamp) {
        ServerDateTimes.formatClock(row.endEvent.timestamp)
    }
    val summaryNotes = remember(row.startEvent.notes, row.endEvent.notes) {
        listOf(row.startEvent.notes, row.endEvent.notes)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, CategorySleep.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SleepBubble(icon = "😴")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sleep",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = StatsAggregation.formatShortDurationMinutes(row.durationMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = CategorySleep,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Ended $endTime · $startTime–$endTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                if (summaryNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summaryNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSleepCard(row: InsightsRow.ActiveSleep) {
    val startTime = remember(row.startEvent.timestamp) {
        ServerDateTimes.formatClock(row.startEvent.timestamp)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, CategorySleep.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SleepBubble(icon = "🌙")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sleep in progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = StatsAggregation.formatShortDurationMinutes(row.elapsedMinutes).ifBlank { "Just started" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = CategorySleep,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Started $startTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                if (row.startEvent.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = row.startEvent.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepBubble(icon: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(CategorySleep.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 24.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: BackendService.EventItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit
) {
    val displayTime = remember(event.timestamp) {
        ServerDateTimes.formatClock(event.timestamp).takeUnless { it == "—" } ?: event.timestamp
    }
    val displayDate = remember(event.timestamp) {
        ServerDateTimes.formatDayLabel(event.timestamp)
    }
    val cardColor = remember(event.type) {
        when {
            SheetEventMarkers.isSleepStart(event.type) -> CategorySleep
            SheetEventMarkers.isSleepEnd(event.type) -> CategorySleep.copy(alpha = 0.85f)
            event.type.contains("💩") && event.type.contains("💧") -> CategoryPoop
            event.type.contains("💧") -> CategoryPee
            event.type.contains("🐄") -> CategoryFeeding
            else -> Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) cardColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(3.dp, cardColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        onClick = {
            if (isSelectionMode) {
                onSelect()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelect() }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(cardColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (event.type) {
                            "💩💧" -> Row {
                                Text(text = "💩", fontSize = 18.sp)
                                Text(text = "💧", fontSize = 18.sp)
                            }
                            "💧🐄", "💩🐄" -> Row {
                                val (first, second) = if (event.type == "💧🐄") "💧" to "🐄" else "💩" to "🐄"
                                Text(text = first, fontSize = 18.sp)
                                Text(text = second, fontSize = 18.sp)
                            }
                            else -> Text(text = event.type, fontSize = 28.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (displayDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    if (event.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onLongPress) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Select",
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: BackendService.EventItem,
    onDismiss: () -> Unit,
    onSave: (BackendService.EventItem) -> Unit
) {
    val context = LocalContext.current

    val parsedDate = remember(event.timestamp) {
        ServerDateTimes.parseDate(event.timestamp) ?: Date()
    }

    val calendar = remember { Calendar.getInstance().apply { time = parsedDate } }
    var editedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var editedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var editedType by remember { mutableStateOf(event.type) }
    var editedNotes by remember { mutableStateOf(event.notes) }
    var showTypeSelector by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Action Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = {
                        HapticFeedback.lightTap(context)
                        showTypeSelector = !showTypeSelector
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = editedType,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showTypeSelector,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "😴" to "Sleep Started",
                            "☀️" to "Sleep Ended",
                            "💩💧" to "Poop & Pee",
                            "💧" to "Pee Only",
                            "🐄" to "Feed",
                            "💧🐄" to "Pee + Feed",
                            "💩🐄" to "Poop + Feed",
                        ).forEach { (emoji, label) ->
                            ActionTypeButton(
                                emoji = emoji,
                                label = label,
                                isSelected = editedType == emoji,
                                onClick = {
                                    HapticFeedback.lightTap(context)
                                    editedType = emoji
                                    showTypeSelector = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberPicker(
                        value = editedHour,
                        range = 0..23,
                        onValueChange = { editedHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NumberPicker(
                        value = editedMinute,
                        range = 0..59,
                        onValueChange = { editedMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add notes...") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticFeedback.lightTap(context)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            HapticFeedback.mediumImpact(context)
                            val updatedCalendar = Calendar.getInstance().apply {
                                time = parsedDate
                                set(Calendar.HOUR_OF_DAY, editedHour)
                                set(Calendar.MINUTE, editedMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val newTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                .format(updatedCalendar.time)

                            onSave(
                                BackendService.EventItem(
                                    rowNumber = event.rowNumber,
                                    timestamp = newTimestamp,
                                    type = editedType,
                                    notes = editedNotes
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTypeButton(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(text = label, fontWeight = FontWeight.SemiBold)
        }
    }
}
