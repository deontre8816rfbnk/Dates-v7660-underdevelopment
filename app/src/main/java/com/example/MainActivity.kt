package com.example

import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.collectIsDraggedAsState

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ChronosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ChronosVectorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ChronosVectorScreen(
    viewModel: ChronosViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedDatesList by viewModel.savedDates.collectAsState()
    
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogTitle by remember { mutableStateOf("") }
    var saveDialogYear by remember { mutableStateOf("") }
    var saveDialogMonth by remember { mutableStateOf("") }
    var saveDialogDay by remember { mutableStateOf("") }
    var saveDialogHour by remember { mutableStateOf("") }
    var saveDialogMinute by remember { mutableStateOf("") }
    
    // Derive active accent colors based on zone (Positive: Emerald, Negative Zone: Cosmic Purple / Neon Amber)
    val isNegativeZone = uiState.vectorResult?.isNegative == true
    val accentStart = if (isNegativeZone) Color(0xFFA78BFA) else Color(0xFF10B981) 
    val accentEnd = if (isNegativeZone) Color(0xFFFBBF24) else Color(0xFF06B6D4) 
    val glowColor = if (isNegativeZone) Color(0x3B8B5CF6) else Color(0x3B10B981)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090C)) // absolute deep space dark
            .drawBehind {
                // Ambient soft radial background glow reflecting the current time vector zone
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center.copy(y = size.height * 0.7f),
                        radius = size.width * 0.95f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Header
            Text(
                text = "CHRONOS VECTOR",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // --- 1. DIRECT SCROLL PICKERS BOARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E14)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0xFF1E2430), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segment A: Birthdate Metric Matrix
                    ChronosHexTable(
                        title = "BIRTH TEMPORAL VECTOR COORDINATE",
                        year = uiState.birthYear,
                        month = uiState.birthMonth,
                        day = uiState.birthDay,
                        hour = uiState.birthHour,
                        minute = uiState.birthMinute,
                        second = uiState.birthSecond,
                        onYearChange = { viewModel.updateBirthYear(it) },
                        onMonthChange = { viewModel.updateBirthMonth(it) },
                        onDayChange = { viewModel.updateBirthDay(it) },
                        onHourChange = { viewModel.updateBirthHour(it) },
                        onMinuteChange = { viewModel.updateBirthMinute(it) },
                        onSecondChange = { viewModel.updateBirthSecond(it) },
                        accentColor = accentStart,
                        enabled = true,
                        testTagPrefix = "birth"
                    )

                    HorizontalDivider(color = Color(0xFF1B202A), thickness = 0.5.dp)

                    // Segment B: Target Date Metric Matrix with Dynamic Toggles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TARGET DESTINATION VECTOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isTargetLive) Color.Gray else Color.White,
                                letterSpacing = 1.sp
                            )
                            
                            // Streaming flow switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.isTargetLive) accentEnd.copy(alpha = 0.15f) else Color(0xFF161B22))
                                    .border(0.5.dp, if (uiState.isTargetLive) accentEnd.copy(alpha = 0.4f) else Color(0xFF2E333D), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setTargetLive(!uiState.isTargetLive) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("toggle_live_ticking")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (uiState.isTargetLive) accentEnd else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isTargetLive) "LIVE FLOW" else "STATIC",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (uiState.isTargetLive) Color.White else Color.Gray,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (uiState.isTargetLive) {
                            // High-tech active streaming status overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color(0xFF090D14), RoundedCornerShape(12.dp))
                                    .border(1.dp, accentEnd.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 0.8f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1200, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "ambient_alpha"
                                    )
                                    
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Live Sync",
                                        tint = accentEnd,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .alpha(pulseAlpha)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "INTEGRATED WITH CURRENT SYSTEM REALITY",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Ticking continuously in real-time",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%04d-%02d-%02d  %02d:%02d:%02d", 
                                            uiState.targetYear, uiState.targetMonth, uiState.targetDay,
                                            uiState.targetHour, uiState.targetMinute, uiState.targetSecond),
                                        color = accentEnd,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Target Scroll Matrix is enabled
                            ChronosHexTable(
                                title = "",
                                year = uiState.targetYear,
                                month = uiState.targetMonth,
                                day = uiState.targetDay,
                                hour = uiState.targetHour,
                                minute = uiState.targetMinute,
                                second = uiState.targetSecond,
                                onYearChange = { viewModel.updateTargetYear(it) },
                                onMonthChange = { viewModel.updateTargetMonth(it) },
                                onDayChange = { viewModel.updateTargetDay(it) },
                                onHourChange = { viewModel.updateTargetHour(it) },
                                onMinuteChange = { viewModel.updateTargetMinute(it) },
                                onSecondChange = { viewModel.updateTargetSecond(it) },
                                accentColor = accentEnd,
                                enabled = true,
                                testTagPrefix = "target"
                            )
                        }
                    }
                }
            }

            // Quick-action control center for registry access
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button called Saved dates as requested
                Button(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131722)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color(0xFF2E3545), RoundedCornerShape(12.dp))
                        .testTag("saved_dates_overview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Saved Dates",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVED DATES", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                // Rapid bookmark register button
                Button(
                    onClick = {
                        saveDialogTitle = ""
                        saveDialogYear = uiState.birthYear.toString()
                        saveDialogMonth = uiState.birthMonth.toString()
                        saveDialogDay = uiState.birthDay.toString()
                        saveDialogHour = uiState.birthHour.toString()
                        saveDialogMinute = uiState.birthMinute.toString()
                        showSaveDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentStart.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, accentStart.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("save_current_coordinates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Coordinate",
                        tint = accentStart,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE COORDINATE", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace,
                        color = accentStart
                    )
                }
            }

            // High precision dialog to register coordinates
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    containerColor = Color(0xFF0F131C),
                    title = {
                        Text(
                            "REGISTER COUPLING SEGMENT",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = saveDialogTitle,
                                onValueChange = { saveDialogTitle = it },
                                label = { Text("Label (e.g. birthday)", color = Color.Gray, fontSize = 11.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentStart,
                                    unfocusedBorderColor = Color(0xFF2E3545),
                                    focusedLabelColor = accentStart,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("save_date_title_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        saveDialogYear = uiState.birthYear.toString()
                                        saveDialogMonth = uiState.birthMonth.toString()
                                        saveDialogDay = uiState.birthDay.toString()
                                        saveDialogHour = uiState.birthHour.toString()
                                        saveDialogMinute = uiState.birthMinute.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("USE BIRTH", color = accentStart, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = {
                                        saveDialogYear = uiState.targetYear.toString()
                                        saveDialogMonth = uiState.targetMonth.toString()
                                        saveDialogDay = uiState.targetDay.toString()
                                        saveDialogHour = uiState.targetHour.toString()
                                        saveDialogMinute = uiState.targetMinute.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("USE TARGET", color = accentEnd, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.weight(1.5f)) {
                                    OutlinedTextField(
                                        value = saveDialogYear,
                                        onValueChange = { saveDialogYear = it.filter { c -> c.isDigit() } },
                                        label = { Text("Year", color = Color.Gray, fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentStart, unfocusedBorderColor = Color(0xFF2E3545)),
                                        modifier = Modifier.fillMaxWidth().testTag("save_date_year_input")
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = saveDialogMonth,
                                        onValueChange = { saveDialogMonth = it.filter { c -> c.isDigit() } },
                                        label = { Text("Month", color = Color.Gray, fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentStart, unfocusedBorderColor = Color(0xFF2E3545)),
                                        modifier = Modifier.fillMaxWidth().testTag("save_date_month_input")
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = saveDialogDay,
                                        onValueChange = { saveDialogDay = it.filter { c -> c.isDigit() } },
                                        label = { Text("Day", color = Color.Gray, fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentStart, unfocusedBorderColor = Color(0xFF2E3545)),
                                        modifier = Modifier.fillMaxWidth().testTag("save_date_day_input")
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = saveDialogHour,
                                        onValueChange = { saveDialogHour = it.filter { c -> c.isDigit() } },
                                        label = { Text("Hour", color = Color.Gray, fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentStart, unfocusedBorderColor = Color(0xFF2E3545)),
                                        modifier = Modifier.fillMaxWidth().testTag("save_date_hour_input")
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = saveDialogMinute,
                                        onValueChange = { saveDialogMinute = it.filter { c -> c.isDigit() } },
                                        label = { Text("Min", color = Color.Gray, fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentStart, unfocusedBorderColor = Color(0xFF2E3545)),
                                        modifier = Modifier.fillMaxWidth().testTag("save_date_minute_input")
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val finalTitle = saveDialogTitle.trim().ifEmpty { "Saved Coord" }
                                val y = saveDialogYear.toIntOrNull() ?: 2026
                                val m = saveDialogMonth.toIntOrNull() ?: 1
                                val d = saveDialogDay.toIntOrNull() ?: 1
                                val h = saveDialogHour.toIntOrNull() ?: 0
                                val min = saveDialogMinute.toIntOrNull() ?: 0
                                viewModel.saveDate(finalTitle, y, m, d, h, min, 0)
                                showSaveDialog = false
                                saveDialogTitle = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentStart),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_date_dialog_confirm")
                        ) {
                            Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showSaveDialog = false }
                        ) {
                            Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }

            // --- 2. VECTOR FILTERS + HIGHPRIZE DISPLAY ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Filter Tabs (above main calculated result)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeFilter.values().forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(
                                        listOf(accentStart, accentEnd)
                                    ) else SolidColor(Color(0xFF161B22))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFF2E353F),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.onFilterSelected(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_tab_${filter.name.lowercase(Locale.US)}")
                        ) {
                            Text(
                                text = filter.label,
                                color = if (isSelected) Color(0xFF07090C) else Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High Precision Vector Display Area (Tappable to toggle breakdown mode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_area_toggle")
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.toggleDisplayMode() }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Helpful visual instruction badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .alpha(0.45f)
                                .padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.displayGrandTotal) "TAP NUMBER FOR HUMAN BREAKDOWN" else "TAP FOR RAW MASSIVE VECTOR VALUE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.LightGray
                            )
                        }

                        if (uiState.vectorResult != null) {
                            AnimatedContent(
                                targetState = uiState.displayGrandTotal,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                                },
                                label = "modeTransition"
                            ) { showGrandTotal ->
                                if (showGrandTotal) {
                                    // Massive High Precision continuous Vector
                                    val countText = getFormattedGrandTotal(uiState.vectorResult!!, uiState.selectedFilter)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = countText,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = if (countText.length > 14) 22.sp else 32.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = getUnitLabel(uiState.selectedFilter, uiState.vectorResult!!.isNegative),
                                            fontSize = 12.sp,
                                            color = accentEnd,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                } else {
                                    // Poetic segmented breakdown grid
                                    RenderStructuredBreakdown(
                                        result = uiState.vectorResult!!,
                                        selectedFilter = uiState.selectedFilter,
                                        accentColor = accentStart,
                                        accentColorSec = accentEnd
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "⏳",
                                fontSize = 36.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "Awaiting coordinate initialization...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Narrative Storytelling Line
                Text(
                    text = uiState.storytelling,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Light,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("storytelling_box")
                )
            }

            // --- SAVED DATES SECTION ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E14)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0xFF1E2430), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .testTag("saved_dates_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAVED DATES REGISTRY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )

                        TextButton(
                            onClick = {
                                saveDialogTitle = ""
                                saveDialogYear = uiState.birthYear.toString()
                                saveDialogMonth = uiState.birthMonth.toString()
                                saveDialogDay = uiState.birthDay.toString()
                                saveDialogHour = uiState.birthHour.toString()
                                saveDialogMinute = uiState.birthMinute.toString()
                                showSaveDialog = true
                            },
                            modifier = Modifier.testTag("add_saved_date_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Label",
                                tint = accentStart,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "REGISTER NEW", 
                                color = accentStart, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold, 
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (savedDatesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No registered coordinates. Bookmark a vector to begin.",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            savedDatesList.forEach { saved ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF090D14), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, Color(0xFF1B202A), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = saved.title.uppercase(Locale.US),
                                            color = accentStart,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%02d/%02d/%04d %02d:%02d:%02d", 
                                                saved.day, saved.month, saved.year, saved.hour, saved.minute, saved.second),
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Apply as Birth button
                                        TextButton(
                                            onClick = { viewModel.applySavedDateAsBirth(saved) },
                                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF131A26)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp).testTag("apply_birth_${saved.id}")
                                        ) {
                                            Text("BIRTH", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }

                                        // Apply as Target button
                                        TextButton(
                                            onClick = { viewModel.applySavedDateAsTarget(saved) },
                                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF1A222D)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp).testTag("apply_target_${saved.id}")
                                        ) {
                                            Text("TARGET", color = accentEnd, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = { viewModel.deleteSavedDate(saved.id) },
                                            modifier = Modifier.size(28.dp).testTag("delete_saved_${saved.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. BOTTOM ZONE ACCENTS: SYMMETRY BADGES & METADATA ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live ticking indicator
                if (uiState.isTargetLive && uiState.vectorResult != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentStart)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE AGE VECTOR TICKING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = accentStart
                        )
                    }
                }

                // Alignment Sync Badges (Day of the Week & Chrono Time)
                AnimatedVisibility(
                    visible = uiState.daySymmetry || uiState.chronoSync,
                    enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        if (uiState.daySymmetry) {
                            SymmetryBadge(
                                message = uiState.daySymmetryMsg,
                                accentColor = accentStart
                            )
                        }
                        if (uiState.chronoSync) {
                            SymmetryBadge(
                                message = uiState.chronoSyncMsg,
                                accentColor = accentEnd
                            )
                        }
                    }
                }

                // Footer metadata copyright
                Text(
                    text = "© CHRONOS VECTOR. ABSOLUTE MINIMALISM, DEEP METRIC.",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.2f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ChronosHexTable(
    title: String,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onSecondChange: (Int) -> Unit,
    accentColor: Color,
    enabled: Boolean,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val yearsRange = remember { (1900..2100).toList() }
    val monthsRange = remember { (1..12).toList() }
    val daysRange = remember(year, month) {
        val maxDays = when (month) {
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            else -> 31
        }
        (1..maxDays).toList()
    }
    val hoursRange = remember { (0..23).toList() }
    val minutesRange = remember { (0..59).toList() }
    val secondsRange = remember { (0..59).toList() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricScrollColumn(
                label = "YEAR",
                currentValue = year,
                items = yearsRange,
                onValueSelected = onYearChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_year_scroll")
            )
            MetricScrollColumn(
                label = "MTH",
                currentValue = month,
                items = monthsRange,
                onValueSelected = onMonthChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_month_scroll")
            )
            MetricScrollColumn(
                label = "DAY",
                currentValue = day,
                items = daysRange,
                onValueSelected = onDayChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_day_scroll")
            )
            MetricScrollColumn(
                label = "HR",
                currentValue = hour,
                items = hoursRange,
                onValueSelected = onHourChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_hour_scroll")
            )
            MetricScrollColumn(
                label = "MIN",
                currentValue = minute,
                items = minutesRange,
                onValueSelected = onMinuteChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_minute_scroll")
            )
            MetricScrollColumn(
                label = "SEC",
                currentValue = second,
                items = secondsRange,
                onValueSelected = onSecondChange,
                activeColor = accentColor,
                enabled = enabled,
                modifier = Modifier.testTag("${testTagPrefix}_second_scroll")
            )
        }
    }
}

@Composable
fun MetricScrollColumn(
    label: String,
    currentValue: Int,
    items: List<Int>,
    onValueSelected: (Int) -> Unit,
    activeColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(currentValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    // Track when user is actively dragging the column
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var wasDragged by remember { mutableStateOf(false) }

    if (isDragged) {
        wasDragged = true
    }

    // Automatically align centered index when currentValue changes programmatically
    LaunchedEffect(currentValue, items) {
        if (!listState.isScrollInProgress && !isDragged) {
            listState.scrollToItem(selectedIndex)
        }
    }

    // Snapping Fling Behavior so scroll cannot stop in the middle of elements
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Automatically trigger selection of closest center item ONLY when user scroll/drag settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && wasDragged) {
            wasDragged = false
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val center = layoutInfo.viewportSize.height / 2
                val closestItem = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    Math.abs(itemCenter - center)
                }
                closestItem?.let {
                    val targetValue = items.getOrNull(it.index)
                    if (targetValue != null && targetValue != currentValue) {
                        onValueSelected(targetValue)
                    }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(52.dp)
            .height(115.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(Color(0xFF0F1217), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color(0xFF2E333D), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Highlighter Overlay Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(activeColor.copy(alpha = 0.12f))
                    .border(0.5.dp, activeColor.copy(alpha = 0.4f))
            )

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    val isSelected = item == currentValue
                    val alphaValue = if (isSelected) 1f else 0.35f
                    val scaleValue = if (isSelected) 1.25f else 0.85f
                    val formattedItem = when (label) {
                        "MTH" -> getMonthName(item)
                        else -> String.format(Locale.US, "%02d", item)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clickable(enabled = enabled) {
                                onValueSelected(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (label == "YEAR") item.toString() else formattedItem,
                            color = if (isSelected) activeColor else Color.White,
                            fontSize = if (label == "YEAR" || label == "MTH") 11.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .alpha(alphaValue)
                                .scale(scaleValue)
                        )
                    }
                }
            }
        }
    }
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> month.toString()
    }
}

@Composable
fun SymmetryBadge(
    message: String,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF11151D)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(1.dp)
            .scale(pulseScale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun RenderStructuredBreakdown(
    result: TimeVectorResult,
    selectedFilter: TimeFilter,
    accentColor: Color,
    accentColorSec: Color
) {
    val signPrefix = if (result.isNegative) "-" else ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedFilter) {
            TimeFilter.YEARS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Years", value = "$signPrefix${result.breakdownYears}", accentColor)
                    BreakdownItem(unit = "Months", value = "$signPrefix${result.breakdownMonths}", accentColor)
                    BreakdownItem(unit = "Days", value = "$signPrefix${result.breakdownDays}", accentColor)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Hours", value = "$signPrefix${result.breakdownHours}", accentColorSec)
                    BreakdownItem(unit = "Mins", value = "$signPrefix${result.breakdownMinutes}", accentColorSec)
                    BreakdownItem(unit = "Secs", value = "$signPrefix${result.breakdownSeconds}", accentColorSec)
                }
            }
            TimeFilter.MONTHS -> {
                val absMonths = result.breakdownYears * 12 + result.breakdownMonths
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Total Months", value = "$signPrefix$absMonths", accentColor)
                    BreakdownItem(unit = "Days", value = "$signPrefix${result.breakdownDays}", accentColor)
                    BreakdownItem(unit = "Hours", value = "$signPrefix${result.breakdownHours}", accentColorSec)
                }
            }
            TimeFilter.WEEKS -> {
                val totalDays = Math.abs(result.totalDays)
                val totalWeeks = totalDays / 7
                val remainingDays = totalDays % 7
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Total Weeks", value = "$signPrefix$totalWeeks", accentColor)
                    BreakdownItem(unit = "Days", value = "$signPrefix$remainingDays", accentColor)
                    BreakdownItem(unit = "Hours", value = "$signPrefix${result.breakdownHours}", accentColorSec)
                }
            }
            TimeFilter.DAYS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Total Days", value = "$signPrefix${Math.abs(result.totalDays)}", accentColor)
                    BreakdownItem(unit = "Hours", value = "$signPrefix${result.breakdownHours}", accentColorSec)
                    BreakdownItem(unit = "Mins", value = "$signPrefix${result.breakdownMinutes}", accentColorSec)
                }
            }
            TimeFilter.HOURS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Total Hours", value = "$signPrefix${Math.abs(result.totalHours)}", accentColor)
                    BreakdownItem(unit = "Mins", value = "$signPrefix${result.breakdownMinutes}", accentColorSec)
                    BreakdownItem(unit = "Secs", value = "$signPrefix${result.breakdownSeconds}", accentColorSec)
                }
            }
            TimeFilter.SECONDS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BreakdownItem(unit = "Total Seconds", value = "$signPrefix${Math.abs(result.totalSeconds)}", accentColor)
                    BreakdownItem(unit = "Millis", value = "$signPrefix${result.breakdownMillis}", accentColorSec)
                }
            }
        }
    }
}

@Composable
fun BreakdownItem(
    unit: String,
    value: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = unit.uppercase(Locale.US),
            fontSize = 9.sp,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

private fun getUnitLabel(filter: TimeFilter, isNegative: Boolean): String {
    val signToken = if (isNegative) " (NEGATIVE ENTROPY)" else " (ELAPSED VECTOR)"
    return filter.name + signToken
}

private fun getFormattedGrandTotal(result: TimeVectorResult, filter: TimeFilter): String {
    return when (filter) {
        TimeFilter.YEARS -> String.format(Locale.US, "%,.9f", result.totalYears)
        TimeFilter.MONTHS -> String.format(Locale.US, "%,.8f", result.totalMonths)
        TimeFilter.WEEKS -> String.format(Locale.US, "%,.8f", result.totalWeeks)
        TimeFilter.DAYS -> {
            val fractionalDays = result.totalDays.toDouble() + (result.breakdownHours / 24.0) + (result.breakdownMinutes / 1440.0) + (result.breakdownSeconds / 86400.0)
            String.format(Locale.US, "%,.7f", if (result.isNegative && fractionalDays > 0) -fractionalDays else fractionalDays)
        }
        TimeFilter.HOURS -> {
            val fractionalHours = result.totalHours.toDouble() + (result.breakdownMinutes / 60.0) + (result.breakdownSeconds / 3600.0)
            String.format(Locale.US, "%,.5f", if (result.isNegative && fractionalHours > 0) -fractionalHours else fractionalHours)
        }
        TimeFilter.SECONDS -> {
            val fractionalSeconds = result.totalSeconds.toDouble() + (result.breakdownMillis / 1000.0)
            String.format(Locale.US, "%,.3f", if (result.isNegative && fractionalSeconds > 0) -fractionalSeconds else fractionalSeconds)
        }
    }
}
