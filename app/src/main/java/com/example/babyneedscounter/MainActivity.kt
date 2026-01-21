package com.example.babyneedscounter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import android.net.Uri
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.babyneedscounter.ui.theme.BabyNeedsCounterTheme
import com.example.babyneedscounter.ui.theme.BabyBlue
import com.example.babyneedscounter.ui.theme.MintGreen
import com.example.babyneedscounter.ui.theme.SoftPink
import com.example.babyneedscounter.ui.theme.TextSecondary
import com.example.babyneedscounter.ui.theme.WarmYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            
            // Update stats widget
            val statsWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(this, BabyStatsWidget::class.java)
            )
            if (statsWidgetIds.isNotEmpty()) {
                val statsIntent = Intent(this, BabyStatsWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, statsWidgetIds)
                }
                sendBroadcast(statsIntent)
                Log.d("MainActivity", "Triggered stats widget update for ${statsWidgetIds.size} widget(s)")
            }
            
            // Update logging widget
            val loggingWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(this, BabyLoggingWidget::class.java)
            )
            if (loggingWidgetIds.isNotEmpty()) {
                val loggingIntent = Intent(this, BabyLoggingWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, loggingWidgetIds)
                }
                sendBroadcast(loggingIntent)
                Log.d("MainActivity", "Triggered logging widget update for ${loggingWidgetIds.size} widget(s)")
            }
            
            // Update feed times widget
            val feedTimesWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(this, BabyFeedTimesWidget::class.java)
            )
            if (feedTimesWidgetIds.isNotEmpty()) {
                val feedTimesIntent = Intent(this, BabyFeedTimesWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, feedTimesWidgetIds)
                }
                sendBroadcast(feedTimesIntent)
                Log.d("MainActivity", "Triggered feed times widget update for ${feedTimesWidgetIds.size} widget(s)")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating widgets", e)
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSettingsClick = { navController.navigate("settings") },
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
            modifier = Modifier.padding(innerPadding),
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyNeedsLogger(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val backendService = remember { BackendService(context) }
    val googleSheetUrl by settingsManager.googleSheetUrl.collectAsState(initial = "")
    val googleSheetViewUrl by settingsManager.googleSheetViewUrl.collectAsState(initial = "")
    
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var todayStats by remember { mutableStateOf<BackendService.TodayStats?>(null) }
    var lastRefresh by remember { mutableStateOf(0L) }
    var selectedEvents by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var useCustomTime by remember { mutableStateOf(false) }
    val calendar = remember { java.util.Calendar.getInstance() }
    var customHour by remember { mutableStateOf(calendar.get(java.util.Calendar.HOUR_OF_DAY)) }
    var customMinute by remember { mutableStateOf(calendar.get(java.util.Calendar.MINUTE)) }
    
    // Function to refresh stats
    val refreshStats: () -> Unit = {
        scope.launch {
            if (googleSheetUrl.isNotEmpty()) {
                try {
                    isRefreshing = true
                    val stats = backendService.fetchTodayStats(googleSheetUrl, useCache = true)
                    if (stats != null) {
                        todayStats = stats
                        lastRefresh = System.currentTimeMillis()
                    }
                } finally {
                    isRefreshing = false
                }
            }
        }
    }
    
    // Load cached stats immediately on launch
    LaunchedEffect(Unit) {
        val cached = backendService.getCachedStats()
        if (cached != null) {
            Log.d("BabyNeeds", "Loaded cached stats on launch")
            todayStats = cached
        }
    }
    
    // Refresh stats on launch and when URL changes
    LaunchedEffect(googleSheetUrl) {
        refreshStats()
    }
    
    // Refresh stats when screen becomes visible (lifecycle aware)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("BabyNeeds", "Screen resumed - refreshing stats")
                refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Periodic refresh every 15 seconds while app is active (tighter tracking)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000L) // 15 seconds (reduced from 30)
            if (googleSheetUrl.isNotEmpty() && !isLoading) {
                Log.d("BabyNeeds", "Periodic stats refresh triggered")
                refreshStats()
            }
        }
    }
    
    // Helper function to update all widgets
    val updateAllWidgets: () -> Unit = {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Update stats widget
            val statsWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BabyStatsWidget::class.java)
            )
            if (statsWidgetIds.isNotEmpty()) {
                val statsIntent = Intent(context, BabyStatsWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, statsWidgetIds)
                }
                context.sendBroadcast(statsIntent)
                Log.d("BabyNeeds", "Triggered stats widget update for ${statsWidgetIds.size} widget(s)")
            }
            
            // Update logging widget (if needed)
            val loggingWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BabyLoggingWidget::class.java)
            )
            if (loggingWidgetIds.isNotEmpty()) {
                val loggingIntent = Intent(context, BabyLoggingWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, loggingWidgetIds)
                }
                context.sendBroadcast(loggingIntent)
                Log.d("BabyNeeds", "Triggered logging widget update for ${loggingWidgetIds.size} widget(s)")
            }
            
            // Update feed times widget
            val feedTimesWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BabyFeedTimesWidget::class.java)
            )
            if (feedTimesWidgetIds.isNotEmpty()) {
                val feedTimesIntent = Intent(context, BabyFeedTimesWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, feedTimesWidgetIds)
                }
                context.sendBroadcast(feedTimesIntent)
                Log.d("BabyNeeds", "Triggered feed times widget update for ${feedTimesWidgetIds.size} widget(s)")
            }
        } catch (e: Exception) {
            Log.e("BabyNeeds", "Error updating widgets", e)
        }
    }
    
    val logEvent: (String, String, String) -> Unit = logEvent@{ eventType, eventName, eventNotes ->
        if (isLoading) return@logEvent
        
        // Haptic feedback on button press
        HapticFeedback.mediumImpact(context)
        
        scope.launch {
            try {
                isLoading = true
                Log.d("BabyNeeds", "Logging event: $eventType with notes: $eventNotes")
                Log.d("BabyNeeds", "Google Sheet URL: $googleSheetUrl")
                
                if (googleSheetUrl.isEmpty()) {
                    HapticFeedback.error(context)
                    snackbarHostState.showSnackbar("⚠️ Please set up your sheet in Settings first")
                    Log.w("BabyNeeds", "No Google Sheets URL configured")
                    isLoading = false
                    return@launch
                }
                
                // Log current state for debugging
                Log.d("BabyNeeds", "useCustomTime: $useCustomTime")
                Log.d("BabyNeeds", "customHour: $customHour, customMinute: $customMinute")
                
                // Use custom time if enabled, otherwise use current time
                val timestamp = if (useCustomTime) {
                    val customCal = java.util.Calendar.getInstance()
                    customCal.set(java.util.Calendar.HOUR_OF_DAY, customHour)
                    customCal.set(java.util.Calendar.MINUTE, customMinute)
                    customCal.set(java.util.Calendar.SECOND, 0)
                    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(customCal.time)
                    Log.d("BabyNeeds", "Generated custom timestamp: $formatted (from $customHour:$customMinute)")
                    formatted
                } else {
                    val current = BackendService.getCurrentTimestamp()
                    Log.d("BabyNeeds", "Using current timestamp: $current")
                    current
                }
                
                val event = BackendService.BabyEvent(
                    timestamp = timestamp,
                    type = eventType,
                    notes = eventNotes
                )
                
                Log.d("BabyNeeds", "Final event timestamp: $timestamp (useCustomTime was: $useCustomTime)")
                
                snackbarHostState.showSnackbar("Saving $eventName...")
                val success = backendService.logEvent(googleSheetUrl, event)
                
                if (success) {
                    // Success haptic feedback
                    HapticFeedback.success(context)
                    
                    Log.d("BabyNeeds", "Successfully synced to Google Sheets")
                    snackbarHostState.showSnackbar("✓ $eventName tracked!")
                    
                    // Clear form after successful logging
                    selectedEvents = setOf()
                    notes = ""
                    
                    // Reset custom time after successful logging
                    useCustomTime = false
                    val now = java.util.Calendar.getInstance()
                    customHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                    customMinute = now.get(java.util.Calendar.MINUTE)
                    
                    // Refresh stats after successful log
                    refreshStats()
                    
                    // Update widgets immediately
                    updateAllWidgets()
                } else {
                    HapticFeedback.error(context)
                    Log.e("BabyNeeds", "Failed to sync to Google Sheets")
                    snackbarHostState.showSnackbar("❌ Couldn't save. Check your connection.")
                }
            } catch (e: Exception) {
                HapticFeedback.error(context)
                Log.e("BabyNeeds", "Error logging event", e)
                snackbarHostState.showSnackbar("❌ Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section - More compact
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Baby Needs",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Refresh indicator
                    if (isRefreshing) {
                        Spacer(modifier = Modifier.width(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "Track your baby's daily activities",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Link to Google Sheet - moved to top right
            if (googleSheetViewUrl.isNotEmpty()) {
                IconButton(
                    onClick = {
                        try {
                            // Try to open in Google Sheets app first
                            val sheetsAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleSheetViewUrl)).apply {
                                setPackage("com.google.android.apps.docs.editors.sheets")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(sheetsAppIntent)
                            Log.d("BabyNeeds", "Opened in Google Sheets app")
                        } catch (e: Exception) {
                            // If Google Sheets app not available, open in browser
                            try {
                                Log.d("BabyNeeds", "Google Sheets app not found, opening in browser")
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleSheetViewUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(browserIntent)
                            } catch (e2: Exception) {
                                Log.e("BabyNeeds", "Failed to open Google Sheet", e2)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "View Progress",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Feed Times Card - Countdown & Next Feed Time
        val timeUntilNextFeed = todayStats?.getTimeUntilNextFeed() ?: "—"
        val nextFeedTime = todayStats?.getNextFeedTime() ?: "—"
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = SoftPink.copy(alpha = 0.12f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(2.dp, SoftPink.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Feed Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Next Feed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "~",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = SoftPink.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = nextFeedTime,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = SoftPink
                        )
                    }
                }
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(SoftPink.copy(alpha = 0.3f))
                )
                
                // Next Feed Countdown
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Next Feed In",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeUntilNextFeed,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = SoftPink
                    )
                }
            }
        }
        
        // Quick Stats Row - 3 Trackers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val peeCount = todayStats?.peeCount ?: 0
            val poopCount = todayStats?.poopCount ?: 0
            val feedTime = todayStats?.getTimeSinceLastFeed() ?: "—"
            
            QuickStatCard("💧 Pee", "$peeCount", "times")
            QuickStatCard("💩 Poop", "$poopCount", "times")
            QuickStatCard("🐄 Feed", feedTime, "ago")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Multi-Select Action Cards - more compact spacing
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectableActionCard(
                title = "Diaper Change",
                subtitle = "Poop & Pee",
                icon = "💩",
                color = WarmYellow,
                isSelected = selectedEvents.contains("poop_pee"),
                onToggle = {
                    selectedEvents = if (selectedEvents.contains("poop_pee")) {
                        selectedEvents - "poop_pee"
                    } else {
                        selectedEvents + "poop_pee"
                    }
                },
                enabled = !isLoading
            )
            
            SelectableActionCard(
                title = "Diaper Change",
                subtitle = "Pee Only",
                icon = "💧",
                color = BabyBlue,
                isSelected = selectedEvents.contains("pee"),
                onToggle = {
                    selectedEvents = if (selectedEvents.contains("pee")) {
                        selectedEvents - "pee"
                    } else {
                        selectedEvents + "pee"
                    }
                },
                enabled = !isLoading
            )
            
            SelectableActionCard(
                title = "Feeding",
                subtitle = "Breastmilk",
                icon = "🐄",
                color = SoftPink,
                isSelected = selectedEvents.contains("feed"),
                onToggle = {
                    selectedEvents = if (selectedEvents.contains("feed")) {
                        selectedEvents - "feed"
                    } else {
                        selectedEvents + "feed"
                    }
                },
                enabled = !isLoading
            )
        }
        
        // Notes input - always visible
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            placeholder = { Text("e.g., yellow, runny, fussy") },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        )
        
        // Custom Time Section
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Log from different time",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            androidx.compose.material3.Switch(
                checked = useCustomTime,
                onCheckedChange = { useCustomTime = it },
                enabled = !isLoading
            )
        }
        
        // Time Picker - shown when custom time is enabled
        if (useCustomTime) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vertical scroll wheel time picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour picker
                NumberPicker(
                    value = customHour,
                    range = 0..23,
                    onValueChange = { customHour = it },
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // Minute picker
                NumberPicker(
                    value = customMinute,
                    range = 0..59,
                    onValueChange = { customMinute = it },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Reset to current time button
            androidx.compose.material3.TextButton(
                onClick = {
                    val now = java.util.Calendar.getInstance()
                    customHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                    customMinute = now.get(java.util.Calendar.MINUTE)
                },
                enabled = !isLoading
            ) {
                Text("Reset to now", fontSize = 12.sp)
            }
        }
        
        // Log Button - compact with animation
        if (selectedEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Button scale animation
            val buttonScale by animateFloatAsState(
                targetValue = if (isLoading) 0.95f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "button_scale"
            )
            
            Button(
                onClick = {
                    // Convert to emoji types
                    val emojiType = selectedEvents.joinToString("") { type ->
                        when(type) {
                            "poop_pee" -> "💩💧"
                            "pee" -> "💧"
                            "feed" -> "🐄"
                            else -> type
                        }
                    }
                    val displayName = selectedEvents.joinToString(" + ") { type ->
                        when(type) {
                            "poop_pee" -> "Poop & Pee"
                            "pee" -> "Pee"
                            "feed" -> "Feed"
                            else -> type
                        }
                    }
                    logEvent(emojiType, displayName, notes)
                    // Note: Reset logic moved inside logEvent to avoid race condition
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Track (${selectedEvents.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        if (isLoading && selectedEvents.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableActionCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    isSelected: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    
    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    Card(
        onClick = {
            // Haptic feedback on tap
            HapticFeedback.lightTap(context)
            onToggle()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                color.copy(alpha = 0.15f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
            pressedElevation = 8.dp
        ),
        border = if (isSelected) 
            BorderStroke(3.dp, color) 
        else null,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 28.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Column
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
            
            // Selection indicator or colored bar
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(4.dp, 40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun RowScope.QuickStatCard(label: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
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
        BabyNeedsLogger()
    }
}