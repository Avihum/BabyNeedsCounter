package com.example.babyneedscounter

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.babyneedscounter.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val backendService = remember { BackendService(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val googleSheetUrl by settingsManager.googleSheetUrl.collectAsState(initial = "")
    
    var events by remember { mutableStateOf<List<BackendService.EventItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedEventRows by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<BackendService.EventItem?>(null) }
    
    // Load events on launch
    LaunchedEffect(googleSheetUrl) {
        if (googleSheetUrl.isNotEmpty()) {
            isLoading = true
            val fetchedEvents = backendService.fetchTodayEvents(googleSheetUrl)
            if (fetchedEvents != null) {
                events = fetchedEvents
                Log.d("HistoryScreen", "Loaded ${events.size} events")
            } else {
                snackbarHostState.showSnackbar("Failed to load events")
            }
            isLoading = false
        }
    }
    
    // Function to refresh events
    val refreshEvents: () -> Unit = {
        scope.launch {
            isLoading = true
            val fetchedEvents = backendService.fetchTodayEvents(googleSheetUrl)
            if (fetchedEvents != null) {
                events = fetchedEvents
                selectedEventRows = emptySet()
                isSelectionMode = false
            }
            isLoading = false
        }
    }
    
    // Function to delete selected events
    val deleteSelected: () -> Unit = {
        scope.launch {
            HapticFeedback.mediumImpact(context)
            isLoading = true
            val rowsToDelete = selectedEventRows.toList()
            val success = backendService.deleteEvents(googleSheetUrl, rowsToDelete)
            if (success) {
                HapticFeedback.success(context)
                snackbarHostState.showSnackbar("✓ Deleted ${rowsToDelete.size} event(s)")
                refreshEvents()
            } else {
                HapticFeedback.error(context)
                snackbarHostState.showSnackbar("❌ Failed to delete events")
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) "${selectedEventRows.size} selected" else "Today's Events",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedEventRows = emptySet()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode && selectedEventRows.isNotEmpty()) {
                        IconButton(onClick = deleteSelected) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = refreshEvents, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (isLoading && events.isEmpty()) {
                // Loading indicator
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading events...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else if (events.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📝",
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No events today",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start tracking your baby's activities",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Event list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = rememberLazyListState()
                ) {
                    items(events, key = { it.rowNumber }) { event ->
                        EventCard(
                            event = event,
                            isSelected = selectedEventRows.contains(event.rowNumber),
                            isSelectionMode = isSelectionMode,
                            onSelect = {
                                HapticFeedback.lightTap(context)
                                if (isSelectionMode) {
                                    selectedEventRows = if (selectedEventRows.contains(event.rowNumber)) {
                                        selectedEventRows - event.rowNumber
                                    } else {
                                        selectedEventRows + event.rowNumber
                                    }
                                }
                            },
                            onLongPress = {
                                HapticFeedback.mediumImpact(context)
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedEventRows = setOf(event.rowNumber)
                                }
                            },
                            onEdit = {
                                HapticFeedback.lightTap(context)
                                editingEvent = event
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Edit dialog
    editingEvent?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { editingEvent = null },
            onSave = { updatedEvent ->
                scope.launch {
                    HapticFeedback.mediumImpact(context)
                    isLoading = true
                    val success = backendService.updateEvent(googleSheetUrl, updatedEvent)
                    if (success) {
                        HapticFeedback.success(context)
                        snackbarHostState.showSnackbar("✓ Event updated")
                        editingEvent = null
                        refreshEvents()
                    } else {
                        HapticFeedback.error(context)
                        snackbarHostState.showSnackbar("❌ Failed to update event")
                        isLoading = false
                    }
                }
            }
        )
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
    val context = LocalContext.current
    
    // Parse the ISO timestamp
    val displayTime = remember(event.timestamp) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(event.timestamp)
            val displayFormat = SimpleDateFormat("HH:mm", Locale.US)
            date?.let { displayFormat.format(it) } ?: event.timestamp
        } catch (e: Exception) {
            event.timestamp
        }
    }
    
    val displayDate = remember(event.timestamp) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(event.timestamp)
            val displayFormat = SimpleDateFormat("MMM d", Locale.US)
            date?.let { displayFormat.format(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    // Get color based on event type
    val cardColor = remember(event.type) {
        when {
            event.type.contains("💩") && event.type.contains("💧") -> WarmYellow
            event.type.contains("💧") -> BabyBlue
            event.type.contains("🐄") -> SoftPink
            else -> Color.Gray
        }
    }
    
    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                cardColor.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected)
            BorderStroke(3.dp, cardColor)
        else
            null,
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
            // Left side - Icon and info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Selection indicator or icon
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
                        // Better emoji rendering for combinations
                        when {
                            event.type == "💩💧" -> {
                                // Poop & Pee - display as stack
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "💩", fontSize = 18.sp)
                                    Text(text = "💧", fontSize = 18.sp)
                                }
                            }
                            event.type == "💧🐄" -> {
                                // Pee + Feed - display side by side
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💧", fontSize = 18.sp)
                                    Text(text = "🐄", fontSize = 18.sp)
                                }
                            }
                            event.type == "💩🐄" -> {
                                // Poop + Feed - display as stack
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "💩", fontSize = 18.sp)
                                    Text(text = "🐄", fontSize = 18.sp)
                                }
                            }
                            else -> {
                                // Single emoji
                                Text(
                                    text = event.type,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
            
            // Right side - Actions
            if (!isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Edit button
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Long press to select
                    IconButton(
                        onClick = onLongPress,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Select",
                            tint = MaterialTheme.colorScheme.onSurface
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
    
    // Parse the current timestamp
    val parsedDate = remember(event.timestamp) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            isoFormat.parse(event.timestamp) ?: Date()
        } catch (e: Exception) {
            Date()
        }
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
                // Title
                Text(
                    text = "Edit Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Action Type Selector
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
                    // Better emoji rendering for combinations
                    when (editedType) {
                        "💩💧" -> {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = "💩", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "💧", fontSize = 24.sp)
                            }
                        }
                        "💧🐄", "💩🐄" -> {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                val (emoji1, emoji2) = if (editedType == "💧🐄") "💧" to "🐄" else "💩" to "🐄"
                                Text(text = emoji1, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = emoji2, fontSize = 24.sp)
                            }
                        }
                        else -> {
                            Text(
                                text = editedType,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                
                // Type selector buttons
                AnimatedVisibility(
                    visible = showTypeSelector,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionTypeButton(
                            emoji = "💩💧",
                            label = "Poop & Pee",
                            isSelected = editedType == "💩💧",
                            onClick = {
                                HapticFeedback.lightTap(context)
                                editedType = "💩💧"
                                showTypeSelector = false
                            }
                        )
                        ActionTypeButton(
                            emoji = "💧",
                            label = "Pee Only",
                            isSelected = editedType == "💧",
                            onClick = {
                                HapticFeedback.lightTap(context)
                                editedType = "💧"
                                showTypeSelector = false
                            }
                        )
                        ActionTypeButton(
                            emoji = "🐄",
                            label = "Feed",
                            isSelected = editedType == "🐄",
                            onClick = {
                                HapticFeedback.lightTap(context)
                                editedType = "🐄"
                                showTypeSelector = false
                            }
                        )
                        ActionTypeButton(
                            emoji = "💧🐄",
                            label = "Pee + Feed",
                            isSelected = editedType == "💧🐄",
                            onClick = {
                                HapticFeedback.lightTap(context)
                                editedType = "💧🐄"
                                showTypeSelector = false
                            }
                        )
                        ActionTypeButton(
                            emoji = "💩🐄",
                            label = "Poop + Feed",
                            isSelected = editedType == "💩🐄",
                            onClick = {
                                HapticFeedback.lightTap(context)
                                editedType = "💩🐄"
                                showTypeSelector = false
                            }
                        )
                    }
                }
                
                // Time picker
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
                    // Hour picker
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
                    
                    // Minute picker
                    NumberPicker(
                        value = editedMinute,
                        range = 0..59,
                        onValueChange = { editedMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Notes
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
                
                // Action buttons
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
                            // Create updated event with new timestamp
                            val updatedCalendar = Calendar.getInstance().apply {
                                time = parsedDate
                                set(Calendar.HOUR_OF_DAY, editedHour)
                                set(Calendar.MINUTE, editedMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val newTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                .format(updatedCalendar.time)
                            
                            val updatedEvent = BackendService.EventItem(
                                rowNumber = event.rowNumber,
                                timestamp = newTimestamp,
                                type = editedType,
                                notes = editedNotes
                            )
                            onSave(updatedEvent)
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
            // Better emoji rendering for combinations
            Box(
                modifier = Modifier.padding(end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (emoji) {
                    "💩💧", "💧🐄", "💩🐄" -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (emoji1, emoji2) = when (emoji) {
                                "💩💧" -> "💩" to "💧"
                                "💧🐄" -> "💧" to "🐄"
                                else -> "💩" to "🐄"
                            }
                            Text(text = emoji1, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = emoji2, fontSize = 22.sp)
                        }
                    }
                    else -> {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
