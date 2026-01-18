package com.example.babyneedscounter

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyneedscounter.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val backendService = remember { BackendService(context) }
    val scope = rememberCoroutineScope()
    
    var googleSheetUrl by remember { mutableStateOf("") }
    var googleSheetViewUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    
    // Load saved URLs
    LaunchedEffect(Unit) {
        launch {
            settingsManager.googleSheetUrl.collect { url ->
                googleSheetUrl = url
            }
        }
        launch {
            settingsManager.googleSheetViewUrl.collect { url ->
                googleSheetViewUrl = url
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Google Sheets Section
            SettingsSection(
                title = "Backend Configuration",
                description = "Connect your app to a Google Sheets backend"
            ) {
                OutlinedTextField(
                    value = googleSheetUrl,
                    onValueChange = { 
                        googleSheetUrl = it
                        showSavedMessage = false
                    },
                    label = { Text("Google Sheets Web App URL", fontSize = 16.sp) },
                    placeholder = { 
                        Text(
                            "https://script.google.com/macros/s/.../exec",
                            fontSize = 14.sp,
                            color = TextSecondary
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = googleSheetViewUrl,
                    onValueChange = { 
                        googleSheetViewUrl = it
                        showSavedMessage = false
                    },
                    label = { Text("Google Sheet View URL (for viewing)", fontSize = 16.sp) },
                    placeholder = { 
                        Text(
                            "https://docs.google.com/spreadsheets/d/.../edit",
                            fontSize = 14.sp,
                            color = TextSecondary
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                settingsManager.saveGoogleSheetUrl(googleSheetUrl)
                                settingsManager.saveGoogleSheetViewUrl(googleSheetViewUrl)
                                isSaving = false
                                showSavedMessage = true
                                testResult = ""
                                Log.d("Settings", "Saved Web App URL: $googleSheetUrl")
                                Log.d("Settings", "Saved View URL: $googleSheetViewUrl")
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !isSaving && !isTesting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testResult = "Testing connection..."
                                showSavedMessage = false
                                Log.d("Settings", "Testing URL: $googleSheetUrl")
                                
                                val testEvent = BackendService.BabyEvent(
                                    timestamp = BackendService.getCurrentTimestamp(),
                                    type = "🧪",
                                    notes = "Test from settings"
                                )
                                
                                val success = backendService.logEvent(googleSheetUrl, testEvent)
                                testResult = if (success) {
                                    "✓ Connection successful!"
                                } else {
                                    "❌ Connection failed. Check URL and logs."
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !isSaving && !isTesting && googleSheetUrl.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (showSavedMessage) {
                    Text(
                        text = "✓ Settings saved successfully",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                if (testResult.isNotEmpty() && !isTesting) {
                    Text(
                        text = testResult,
                        color = if (testResult.startsWith("✓")) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Instructions Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How to Set Up Google Sheets Backend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InstructionStep(
                        number = "1",
                        text = "Create a Google Sheet with columns: Timestamp, Type, Notes"
                    )
                    
                    InstructionStep(
                        number = "2",
                        text = "Go to Extensions → Apps Script in your Google Sheet"
                    )
                    
                    InstructionStep(
                        number = "3",
                        text = "Use the GoogleAppsScript.js code from the project"
                    )
                    
                    InstructionStep(
                        number = "4",
                        text = "Deploy as Web App and copy both URLs (Web App URL for API, Sheet URL for viewing)"
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        
        content()
    }
}

@Composable
fun InstructionStep(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
