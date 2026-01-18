package com.example.babyneedscounter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        enableEdgeToEdge()
        setContent {
            BabyNeedsCounterTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onSettingsClick = { navController.navigate("settings") })
        }
        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSettingsClick: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                actions = {
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
    var todayStats by remember { mutableStateOf<BackendService.TodayStats?>(null) }
    var lastRefresh by remember { mutableStateOf(0L) }
    var selectedEvents by remember { mutableStateOf(setOf<String>()) }
    
    // Function to refresh stats
    val refreshStats: () -> Unit = {
        scope.launch {
            if (googleSheetUrl.isNotEmpty()) {
                val stats = backendService.fetchTodayStats(googleSheetUrl)
                todayStats = stats
                lastRefresh = System.currentTimeMillis()
            }
        }
    }
    
    // Refresh stats on launch and when URL changes
    LaunchedEffect(googleSheetUrl) {
        refreshStats()
    }
    
    val logEvent: (String, String) -> Unit = logEvent@{ eventType, eventName ->
        if (isLoading) return@logEvent
        
        scope.launch {
            try {
                isLoading = true
                Log.d("BabyNeeds", "Logging event: $eventType")
                Log.d("BabyNeeds", "Google Sheet URL: $googleSheetUrl")
                
                if (googleSheetUrl.isEmpty()) {
                    snackbarHostState.showSnackbar("⚠️ Please set up your sheet in Settings first")
                    Log.w("BabyNeeds", "No Google Sheets URL configured")
                    isLoading = false
                    return@launch
                }
                
                val event = BackendService.BabyEvent(
                    timestamp = BackendService.getCurrentTimestamp(),
                    type = eventType,
                    notes = ""
                )
                
                snackbarHostState.showSnackbar("Saving $eventName...")
                val success = backendService.logEvent(googleSheetUrl, event)
                
                if (success) {
                    Log.d("BabyNeeds", "Successfully synced to Google Sheets")
                    snackbarHostState.showSnackbar("✓ $eventName tracked!")
                    // Refresh stats after successful log
                    refreshStats()
                } else {
                    Log.e("BabyNeeds", "Failed to sync to Google Sheets")
                    snackbarHostState.showSnackbar("❌ Couldn't save. Check your connection.")
                }
            } catch (e: Exception) {
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
                Text(
                    text = "Baby Needs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleSheetViewUrl))
                        context.startActivity(intent)
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
        
        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val todayCount = todayStats?.totalEvents ?: 0
            val lastTime = todayStats?.lastEventTime?.let { timestamp ->
                // Parse and format the timestamp
                try {
                    Log.d("BabyNeeds", "Parsing timestamp: $timestamp")
                    // Handle both string and Date formats
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val date = sdf.parse(timestamp.toString())
                    val now = Date()
                    val diffMinutes = ((now.time - (date?.time ?: 0)) / 60000).toInt()
                    when {
                        diffMinutes < 1 -> "Just now"
                        diffMinutes < 60 -> "${diffMinutes}m ago"
                        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
                        else -> "${diffMinutes / 1440}d ago"
                    }
                } catch (e: Exception) {
                    Log.e("BabyNeeds", "Error parsing timestamp: $timestamp", e)
                    "—"
                }
            } ?: "—"
            
            Log.d("BabyNeeds", "Today count: $todayCount, Last time: $lastTime")
            QuickStatCard("Today", "$todayCount", "Events")
            QuickStatCard("Last", "—", lastTime)
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
        
        // Log Button - compact
        if (selectedEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
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
                    logEvent(emojiType, displayName)
                    selectedEvents = setOf()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
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
fun QuickStatCard(label: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
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