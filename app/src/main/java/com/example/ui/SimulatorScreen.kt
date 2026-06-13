package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import com.example.lang.AppLanguage
import com.example.lang.Lang
import com.example.ui.components.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import android.app.ActivityManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.BufferedReader
import java.io.FileReader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.CircuitScheme
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.Telemetry
import com.example.viewmodel.SimulatorViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSchemes by viewModel.savedSchemes.collectAsStateWithLifecycle()
    var telemetryState by remember { mutableStateOf("FULL") } // "FULL", "COMPACT", "HIDDEN"
    var telemetryTab by remember { mutableStateOf("ELECTR") } // "ELECTR", "SIM", "PHONE"
    var showWikiDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var deviceCores by remember { mutableIntStateOf(Runtime.getRuntime().availableProcessors()) }
    var deviceMaxFreq by remember { mutableStateOf("2.40 GHz") }
    var deviceCurrentFreq by remember { mutableStateOf("1.80 GHz") }
    var deviceTotalRam by remember { mutableStateOf("0.0 GB") }
    var deviceAvailRam by remember { mutableStateOf("0.0 GB") }
    var appRamUsedByGame by remember { mutableStateOf("0.0 MB") }

    LaunchedEffect(Unit) {
        deviceCores = Runtime.getRuntime().availableProcessors()
        
        try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            val actFile = if (file.exists()) file else File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq")
            if (actFile.exists()) {
                val reader = BufferedReader(FileReader(actFile))
                val line = reader.readLine()
                reader.close()
                val freqKhz = line?.trim()?.toLongOrNull()
                if (freqKhz != null) {
                    val freqGhz = freqKhz / 1000000.0
                    deviceMaxFreq = String.format(Locale.US, "%.2f GHz", freqGhz)
                }
            } else {
                deviceMaxFreq = "2.40 GHz"
            }
        } catch (e: Exception) {
            deviceMaxFreq = "2.40 GHz"
        }

        while (true) {
            try {
                val mi = ActivityManager.MemoryInfo()
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                val totalGbs = mi.totalMem / (1024.0 * 1024.0 * 1024.0)
                val availGbs = mi.availMem / (1024.0 * 1024.0 * 1024.0)
                deviceTotalRam = String.format(Locale.US, "%.1f GB", totalGbs)
                deviceAvailRam = String.format(Locale.US, "%.1f GB", availGbs)
            } catch (e: Exception) {
                deviceTotalRam = "6.0 GB"
                deviceAvailRam = "2.4 GB"
            }

            try {
                val runtime = Runtime.getRuntime()
                val usedMemBytes = runtime.totalMemory() - runtime.freeMemory()
                appRamUsedByGame = String.format(Locale.US, "%.1f MB", usedMemBytes / (1024.0 * 1024.0))
            } catch (e: Exception) {
                appRamUsedByGame = "45.2 MB"
            }

            try {
                val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                if (file.exists()) {
                    val reader = BufferedReader(FileReader(file))
                    val line = reader.readLine()
                    reader.close()
                    val curKhz = line?.trim()?.toLongOrNull()
                    if (curKhz != null) {
                        val curGhz = curKhz / 1000000.0
                        deviceCurrentFreq = String.format(Locale.US, "%.2f GHz", curGhz)
                    }
                } else {
                    val baseFreq = if (state.isSimulationRunning) 2.10f else 1.20f
                    val drift = ((System.currentTimeMillis() % 1000) - 500) / 2500f
                    val actualEst = baseFreq + drift
                    deviceCurrentFreq = String.format(Locale.US, "%.2f GHz", actualEst)
                }
            } catch (e: Exception) {
                deviceCurrentFreq = "1.80 GHz"
            }

            kotlinx.coroutines.delay(1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E)) // Dark background
    ) {
        CircuitGridCanvas(
            grid = state.grid,
            width = state.width,
            height = state.height,
            tick = state.simulationTick,
            selectedTool = state.selectedTool,
            canvasStyle = state.canvasStyle,
            onCellClicked = { x, y -> viewModel.onCellClicked(x, y) }
        )

        // Top Toolbar Overlay
        UIEngine.TopToolBar(
            lang = state.appLanguage,
            isSimulationRunning = state.isSimulationRunning,
            timeMultiplier = state.timeMultiplier,
            onToggleSimulation = { viewModel.toggleSimulation() },
            onClearGrid = { viewModel.clearGrid() },
            onSetTimeMultiplier = { viewModel.setTimeMultiplier(it) },
            onShowWiki = { showWikiDialog = true },
            onShowSave = { viewModel.showSaveDialog() },
            onShowLoad = { viewModel.showLoadDialog() },
            onShowSettings = { viewModel.showSettingsDialog() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Logs Overlay
        if (state.logs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
                    .width(220.dp)
                    .height(200.dp)
                    .background(Color(0xD2121215), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x3300FFCC), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(state.logs.size) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text("MCU LOGS (SSM)", fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    state.logs.forEach { logMsg ->
                        Text(logMsg, fontSize = 9.sp, color = Color(0xFFCCCCCC), fontFamily = FontFamily.Monospace, lineHeight = 12.sp)
                    }
                }
            }
        }

        // High-Tech Telemetry HUD Overlay
        UIEngine.TelemetryHUD(
            lang = state.appLanguage,
            telemetry = state.telemetry,
            isSimulationRunning = state.isSimulationRunning,
            grid = state.grid,
            deviceCores = deviceCores,
            deviceMaxFreq = deviceMaxFreq,
            deviceCurrentFreq = deviceCurrentFreq,
            deviceTotalRam = deviceTotalRam,
            deviceAvailRam = deviceAvailRam,
            appRamUsedByGame = appRamUsedByGame,
            telemetryTab = telemetryTab,
            telemetryState = telemetryState,
            onTabChanged = { telemetryTab = it },
            onStateChanged = { telemetryState = it },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Bottom Toolbox Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .background(Color(0xD2121215), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x22FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .padding(8.dp)
        ) {
            UIEngine.BottomToolBar(
                lang = state.appLanguage,
                selectedCategory = state.selectedCategory,
                selectedTool = state.selectedTool,
                onCategorySelected = { viewModel.selectCategory(it) },
                onToolSelected = { viewModel.selectTool(it) }
            )
        }

        if (state.showSaveDialog) SaveDialog(lang = state.appLanguage, onDismiss = { viewModel.dismissDialogs() }, onSave = { name -> viewModel.saveScheme(name) })
        if (state.showLoadDialog) LoadDialog(lang = state.appLanguage, schemes = savedSchemes, onDismiss = { viewModel.dismissDialogs() }, onLoad = { scheme -> viewModel.loadScheme(scheme) }, onDelete = { id -> viewModel.deleteScheme(id) })
        if (state.showSettingsDialog) SettingsDialog(
            lang = state.appLanguage,
            onChangeLang = { viewModel.changeLanguage(it) },
            onDismiss = { viewModel.dismissDialogs() },
            onResize = { w, h -> viewModel.resizeGrid(w, h) },
            exportScheme = { viewModel.getShareableString() },
            importScheme = { viewModel.importShareableString(it) },
            allocatedRamGb = state.allocatedRamGbytes,
            allocatedCores = state.allocatedCores,
            clockMhz = state.clockMhz,
            canvasStyle = state.canvasStyle,
            onHardwareUpdate = { ram, cores, mhz, style -> viewModel.updateHardwareSettings(ram, cores, mhz, style) }
        )
        if (showWikiDialog) UIEngine.WikiDialog(lang = state.appLanguage, onDismiss = { showWikiDialog = false })
        state.inspectCoordinates?.let { coords ->
            val gridBoundsValid = coords.first in state.grid.indices && coords.second in state.grid[coords.first].indices
            if (gridBoundsValid) {
                val comp = state.grid[coords.first][coords.second]
                InspectDialog(
                    lang = state.appLanguage,
                    component = comp,
                    onDismiss = { viewModel.dismissInspect() },
                    onSave = { data, repair -> viewModel.updateComponentData(coords.first, coords.second, data, repair) }
                )
            } else {
                viewModel.dismissInspect()
            }
        }
        
        state.multimeterCoordinates?.let { coords ->
            val gridBoundsValid = coords.first in state.grid.indices && coords.second in state.grid[coords.first].indices
            if (gridBoundsValid) {
                val comp = state.grid[coords.first][coords.second]
                MultimeterDialog(
                    lang = state.appLanguage,
                    component = comp,
                    coords = coords,
                    onDismiss = { viewModel.dismissMultimeter() }
                )
            } else {
                viewModel.dismissMultimeter()
            }
        }
        
        if (state.isEasterEggActive) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissEasterEgg() },
                title = { Text("Power Overload!") },
                text = { Text("Phenomenal cosmic power!!!") },
                confirmButton = { TextButton(onClick = { viewModel.dismissEasterEgg() }) { Text("Phew") } }
            )
        }
    }
}

@Composable
fun MultimeterDialog(component: GridComponent, coords: Pair<Int, Int>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MULTIMETER", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00BCD4), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Component: ${component.type.name}", style = MaterialTheme.typography.bodyLarge)
                Text("Position: X: ${coords.first} Y: ${coords.second}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (component.type == ComponentType.EMPTY) {
                   Text("Empty space", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Power State", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.isPowered) "POWERED" else "OFF", style = MaterialTheme.typography.bodyLarge, color = if (component.isPowered) Color(0xFF4CAF50) else Color(0xFFE53935))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Logic State", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.logicState) "HIGH (1)" else "LOW (0)", style = MaterialTheme.typography.bodyLarge, color = if (component.logicState) Color(0xFF2196F3) else Color(0xFF9E9E9E))
                        }
                    }
                    val isStorage = component.type in listOf(ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, ComponentType.CAPACITOR)
                    if (isStorage) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (component.type == ComponentType.CAPACITOR) "Capacitor Charge" else "Battery Charge", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        val maxCap = if (component.type == ComponentType.CAPACITOR) component.voltage * 1000f else com.example.engine.RenderEngine.getMaxCap(component)
                        val doubleMaxCap = if (maxCap > 0f) maxCap else 1f
                        val fraction = (component.charge / doubleMaxCap).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                        val displayStr = if (component.type == ComponentType.CAPACITOR) {
                            String.format(java.util.Locale.US, "%.1f uC", component.charge)
                        } else {
                            String.format(java.util.Locale.US, "%.0f / %.0f mAh", component.charge.coerceAtLeast(0f), maxCap)
                        }
                        Text(displayStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ELECTRICAL DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Voltage Drop", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(String.format(java.util.Locale.US, "%.2f V", component.voltage), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.current >= 1000f) String.format(java.util.Locale.US, "%.2f A", component.current / 1000f) else String.format(java.util.Locale.US, "%.1f mA", component.current), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Path Resistance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.resistance >= 1000f) String.format(java.util.Locale.US, "%.1f kΩ", component.resistance / 1000f) else String.format(java.util.Locale.US, "%.1f Ω", component.resistance), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Power Consumption", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val powerMw = component.voltage * component.current
                            Text(if (powerMw >= 1000f) String.format(java.util.Locale.US, "%.2f W", powerMw / 1000f) else String.format(java.util.Locale.US, "%.1f mW", powerMw), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("THERMAL & FLUIDICS DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Heat Indicator", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(String.format(java.util.Locale.US, "%.1f °C", component.temperature), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Local Pressure", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(String.format(java.util.Locale.US, "%.1f kPa", component.pressure), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }

                    if (component.isOverloaded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Overloaded! Needs repair.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun CircuitGridCanvas(
    grid: Array<Array<GridComponent>>,
    width: Int,
    height: Int,
    tick: Long,
    selectedTool: ComponentType,
    canvasStyle: String = "dark_neon",
    onCellClicked: (Int, Int) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val baseCellSize = 60f

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val maxCanvasWidth = width * baseCellSize
    val maxCanvasHeight = height * baseCellSize
    
    // Live feedback placement HUD targeting the exact cell under the finger
    var activeHoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedTool, scale, pan, width, height) {
                if (selectedTool == ComponentType.PAN) {
                    detectTransformGestures(panZoomLock = true) { _, panChange, zoomChange, _ ->
                        scale = (scale * zoomChange).coerceIn(0.1f, 5f)
                        pan += panChange
                    }
                } else {
                    var lastCellX: Int? = null
                    var lastCellY: Int? = null
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val localPoint = (startOffset - pan) / scale
                            val cellX = (localPoint.x / baseCellSize).toInt()
                            val cellY = (localPoint.y / baseCellSize).toInt()
                            if (cellX in 0 until width && cellY in 0 until height) {
                                onCellClicked(cellX, cellY)
                            }
                            lastCellX = cellX
                            lastCellY = cellY
                            activeHoverCell = Pair(cellX, cellY)
                        },
                        onDragEnd = {
                            lastCellX = null
                            lastCellY = null
                            activeHoverCell = null
                        },
                        onDragCancel = {
                            lastCellX = null
                            lastCellY = null
                            activeHoverCell = null
                        },
                        onDrag = { change, _ ->
                            val localPoint = (change.position - pan) / scale
                            val cellX = (localPoint.x / baseCellSize).toInt()
                            val cellY = (localPoint.y / baseCellSize).toInt()
                            activeHoverCell = Pair(cellX, cellY)
                            
                            val previousX = lastCellX
                            val previousY = lastCellY
                            
                            if (previousX != null && previousY != null) {
                                val dx = Math.abs(cellX - previousX)
                                val dy = Math.abs(cellY - previousY)
                                val sx = if (previousX < cellX) 1 else -1
                                val sy = if (previousY < cellY) 1 else -1
                                var err = dx - dy
                                
                                var currX = previousX
                                var currY = previousY
                                
                                while (true) {
                                    if (currX in 0 until width && currY in 0 until height) {
                                        onCellClicked(currX, currY)
                                    }
                                    if (currX == cellX && currY == cellY) break
                                    val e2 = 2 * err
                                    if (e2 > -dy) {
                                        err -= dy
                                        currX += sx
                                    }
                                    if (e2 < dx) {
                                        err += dx
                                        currY += sy
                                    }
                                }
                            } else {
                                if (cellX in 0 until width && cellY in 0 until height) {
                                    onCellClicked(cellX, cellY)
                                }
                            }
                            lastCellX = cellX
                            lastCellY = cellY
                        }
                    )
                }
            }
            .pointerInput(selectedTool, scale, pan, width, height) {
                if (selectedTool != ComponentType.PAN) {
                    detectTapGestures { tapOffset ->
                        val localPoint = (tapOffset - pan) / scale
                        val cellX = (localPoint.x / baseCellSize).toInt()
                        val cellY = (localPoint.y / baseCellSize).toInt()
                        if (cellX in 0 until width && cellY in 0 until height) {
                            onCellClicked(cellX, cellY)
                        }
                    }
                }
            }
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        
        LaunchedEffect(width, height, viewWidth, viewHeight) {
            if (viewWidth > 0f && viewHeight > 0f) {
                val scaleX = viewWidth / maxCanvasWidth
                val scaleY = viewHeight / maxCanvasHeight
                scale = minOf(scaleX, scaleY).coerceIn(0.1f, 1.5f)
                
                val contentWidth = maxCanvasWidth * scale
                val contentHeight = maxCanvasHeight * scale
                
                pan = Offset(
                    (viewWidth - contentWidth) / 2f,
                    (viewHeight - contentHeight) / 2f
                )
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val forceRedrawOnTick = tick // Force Jetpack Compose to redraw the canvas on every simulation cycle
            withTransform({
                translate(left = pan.x, top = pan.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val bgColor = when (canvasStyle) {
                    "blueprint" -> Color(0xFF0F172A) // Rich deep engineering blueprint blue
                    "oled_black" -> Color(0xFF000000) // Pure OLED pitch black
                    else -> Color(0xFF1E1E2E) // Classic neon slate dark
                }
                val gridLineColor = when (canvasStyle) {
                    "blueprint" -> Color(0x333B82F6) // Brighter blueprint alignment blue
                    "oled_black" -> Color(0x19444444) // Very subtle slate gray
                    else -> Color(0x3349454F) // Classic neon border
                }
                val outerBoundaryColor = when (canvasStyle) {
                    "blueprint" -> Color(0xFF38BDF8).copy(alpha = 0.5f) // Electric blueprint blue
                    "oled_black" -> Color(0xFF555555) // Muted silver boundary
                    else -> Color(0xFFD0BCFF).copy(alpha = 0.5f) // Classic soft neon purple
                }
                
                drawRect(bgColor, size = Size(maxCanvasWidth, maxCanvasHeight))
                
                val visibleStartX = maxOf(0, (-pan.x / (scale * baseCellSize)).toInt() - 1)
                val visibleEndX = minOf(width, ((size.width - pan.x) / (scale * baseCellSize)).toInt() + 1)
                val visibleStartY = maxOf(0, (-pan.y / (scale * baseCellSize)).toInt() - 1)
                val visibleEndY = minOf(height, ((size.height - pan.y) / (scale * baseCellSize)).toInt() + 1)
                
                // Draw blueprint grid lines
                for (i in visibleStartX..visibleEndX) {
                    val x = i * baseCellSize
                    drawLine(gridLineColor, start = Offset(x, 0f), end = Offset(x, maxCanvasHeight), strokeWidth = 2f)
                }
                for (j in visibleStartY..visibleEndY) {
                    val y = j * baseCellSize
                    drawLine(gridLineColor, start = Offset(0f, y), end = Offset(maxCanvasWidth, y), strokeWidth = 2f)
                }
                
                drawRect(color = outerBoundaryColor, size = Size(maxCanvasWidth, maxCanvasHeight), style = Stroke(width = 4f))

                // Precision placement crosshair glow ring HUD
                val cellHover = activeHoverCell
                if (cellHover != null && cellHover.first in 0 until width && cellHover.second in 0 until height) {
                    val hX = cellHover.first * baseCellSize
                    val hY = cellHover.second * baseCellSize
                    
                    // Outer glow box
                    drawRect(
                        color = Color(0xFF00FFCC),
                        topLeft = Offset(hX, hY),
                        size = Size(baseCellSize, baseCellSize),
                        style = Stroke(width = 3.5f)
                    )
                    
                    // Subtle translucent inside fill highlighting targeted cell
                    drawRect(
                        color = Color(0x2200FFCC),
                        topLeft = Offset(hX, hY),
                        size = Size(baseCellSize, baseCellSize)
                    )

                    // Inner precise crosshair lines
                    val half = baseCellSize / 2f
                    drawLine(
                        color = Color(0xFF00FFCC),
                        start = Offset(hX + half, hY + 10f),
                        end = Offset(hX + half, hY + baseCellSize - 10f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF00FFCC),
                        start = Offset(hX + 10f, hY + half),
                        end = Offset(hX + baseCellSize - 10f, hY + half),
                        strokeWidth = 2f
                    )
                }

                val actualGridWidth = grid.size
                val actualGridHeight = if (actualGridWidth > 0) grid[0].size else 0

                for (x in visibleStartX until minOf(visibleEndX, actualGridWidth)) {
                    for (y in visibleStartY until minOf(visibleEndY, actualGridHeight)) {
                        val comp = grid[x][y]
                        if (comp.type != ComponentType.EMPTY) {
                            drawContext.canvas.save()
                            drawContext.transform.translate(x * baseCellSize, y * baseCellSize)
                            
                            if (scale < 0.35f) {
                                // High-Performance LOD Draw Call Bypassing Heavy Rendering for zoomed out states
                                val cat = comp.type.category
                                val color = when {
                                    comp.isOverloaded -> Color(0xFFFF3366)
                                    comp.type == ComponentType.URANIUM -> {
                                        if (comp.temperature > 1000f) Color(0xFFFF4500) else Color(0xFF76FF03)
                                    }
                                    cat.name == "POWER" || comp.type == ComponentType.NUCLEAR_REACTOR -> {
                                        if (comp.isPowered) Color(0xFFFF9800) else Color(0xFFD32F2F)
                                    }
                                    cat.name == "WIRE" -> {
                                        if (comp.isPowered) Color(0xFF00FFCC) else Color(0xFF49454F)
                                    }
                                    cat.name == "OUTPUTS" -> {
                                        if (comp.isPowered) Color(0xFFFFEB3B) else Color(0xFF5D4037)
                                    }
                                    cat.name == "LOGIC" -> {
                                        if (comp.isPowered) Color(0xFFE040FB) else Color(0xFF7B1FA2)
                                    }
                                    cat.name == "MATERIALS" || cat.name == "HYDRAULICS" -> {
                                        if (comp.type == ComponentType.WATER || comp.type == ComponentType.INFINITE_WATER) Color(0xFF03A9F4)
                                        else if (comp.type == ComponentType.LAVA || comp.type == ComponentType.INFINITE_LAVA) Color(0xFFF44336)
                                        else if (comp.type == ComponentType.OIL || comp.type == ComponentType.INFINITE_OIL) Color(0xFF212121)
                                        else if (comp.type == ComponentType.ACID || comp.type == ComponentType.INFINITE_ACID) Color(0xFF8BC34A)
                                        else if (comp.type == ComponentType.SLIME || comp.type == ComponentType.INFINITE_SLIME) Color(0xFF4CAF50)
                                        else if (comp.type == ComponentType.GASOLINE || comp.type == ComponentType.INFINITE_GASOLINE) Color(0xFFFFC107)
                                        else if (comp.type == ComponentType.LIQUID_NITROGEN || comp.type == ComponentType.INFINITE_LIQUID_NITROGEN) Color(0xFF4DD0E1)
                                        else if (comp.type == ComponentType.STEAM || comp.type == ComponentType.INFINITE_STEAM) Color(0xAAE0E0E0)
                                        else Color(0xFF795548)
                                    }
                                    else -> if (comp.isPowered) Color(0xFF00FFCC) else Color(0xFF49454F)
                                }
                                drawRect(color = color, size = Size(baseCellSize, baseCellSize))
                                drawRect(color = Color(0x33FFFFFF), size = Size(baseCellSize, baseCellSize), style = Stroke(width = 1f))
                            } else {
                                // Apply Rotation (omit for symmetric/multi-connection wires to ensure flawless drawing)
                                val compType = comp.type
                                val isWire = compType == ComponentType.WIRE_ANY || 
                                             compType == ComponentType.SUPERCONDUCTOR || 
                                             compType == ComponentType.HIGH_VOLTAGE_CABLE || 
                                             compType == ComponentType.FIBER_OPTIC
                                val angle = if (isWire) {
                                    0f
                                } else {
                                    when(comp.direction) {
                                        Direction.UP -> 0f
                                        Direction.RIGHT -> 90f
                                        Direction.DOWN -> 180f
                                        Direction.LEFT -> 270f
                                    }
                                }
                                if (angle != 0f) {
                                    drawContext.transform.rotate(angle, Offset(baseCellSize/2, baseCellSize/2))
                                }
                                
                                com.example.engine.TabletRender.drawComponent(this, grid, x, y, actualGridWidth, actualGridHeight, comp, baseCellSize)
                            }
                            drawContext.canvas.restore()
                        }
                    }
                }
            }
        }
    }
}

/* fun GridComponent.getMaxCap(): Float {
    val default = when(type) { ComponentType.COIN_CELL -> 100f; ComponentType.BATTERY_PACK -> 10000f; ComponentType.INFINITE_BATTERY -> 9999999f; ComponentType.NUCLEAR_REACTOR -> 1000000f; else -> 2500f }
    if (extraData.isEmpty()) return default
    val idx = extraData.indexOf("c=")
    if (idx != -1) {
        val end = extraData.indexOf('|', idx)
        val extracted = if (end != -1) extraData.substring(idx+2, end) else extraData.substring(idx+2)
        return extracted.toFloatOrNull() ?: default
    }
    return default
}

fun DrawScope.drawComponent(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int, component: GridComponent, cellSize: Float) {
    val cx = cellSize / 2
    val cy = cellSize / 2
    val padding = cellSize * 0.15f
    val color = if (component.isOverloaded) Color(0xFFFF3B30) else (if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF49454F))
    val strokeSize = cellSize * 0.1f
    val shadowColor = if (component.isPowered) Color(0x66D0BCFF) else Color(0x22000000)

    when (component.type) {
        ComponentType.EMPTY, ComponentType.PAN, ComponentType.ROTATE, ComponentType.INSPECT -> {}
        
        ComponentType.WIRE_ANY, ComponentType.SUPERCONDUCTOR, ComponentType.HIGH_VOLTAGE_CABLE, ComponentType.FIBER_OPTIC -> {
            val isSuper = component.type == ComponentType.SUPERCONDUCTOR || component.type == ComponentType.HIGH_VOLTAGE_CABLE
            val wireColor = if (component.isPowered) {
                if (isSuper) Color(0xFF00E5FF) else Color(0xFF00FF00)
            } else {
                if (isSuper) Color(0xFF114455) else Color(0xFF49454F)
            }
            val wStroke = if (isSuper) strokeSize * 1.5f else strokeSize * 1.2f

            // Check neighbors. But since we might be rotated, it's easier to just draw lines to absolute directions 
            // where neighbors exist, and then the rotation transform might affect it. Wait! The canvas is already rotated.
            // If the canvas is rotated for WIRE_ANY it doesn't matter much unless we check grid manually.
            // Let's just draw standard directions. Wait, to check grid we use absolute x, y!
            val up = y > 0 && grid[x][y-1].type != ComponentType.EMPTY && grid[x][y-1].type.category != ComponentCategory.TOOLS && grid[x][y-1].type.category != ComponentCategory.MATERIALS
            val down = y < height - 1 && grid[x][y+1].type != ComponentType.EMPTY && grid[x][y+1].type.category != ComponentCategory.TOOLS && grid[x][y+1].type.category != ComponentCategory.MATERIALS
            val left = x > 0 && grid[x-1][y].type != ComponentType.EMPTY && grid[x-1][y].type.category != ComponentCategory.TOOLS && grid[x-1][y].type.category != ComponentCategory.MATERIALS
            val right = x < width - 1 && grid[x+1][y].type != ComponentType.EMPTY && grid[x+1][y].type.category != ComponentCategory.TOOLS && grid[x+1][y].type.category != ComponentCategory.MATERIALS

            if (up) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, 0f), strokeWidth = wStroke)
            if (right) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cellSize, cy), strokeWidth = wStroke)
            if (down) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = wStroke)
            if (left) drawLine(wireColor, start = Offset(cx, cy), end = Offset(0f, cy), strokeWidth = wStroke)
            
            // Draw center dot if no neighbors or multiple
            val connects = listOf(up, down, left, right).count { it }
            if (connects != 2 || (up && down && left && right)) {
                drawCircle(wireColor, radius = wStroke * 0.8f, center = Offset(cx, cy))
            } else if (connects == 0) {
                drawCircle(wireColor, radius = wStroke * 1.5f, center = Offset(cx, cy))
            }
        }
        
        ComponentType.BATTERY -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF4F378B), Color(0xFF221144))),
                topLeft = Offset(padding, padding*2), 
                size = Size(cellSize - padding*2, cellSize - padding*4)
            )
            val maxCap = component.getMaxCap()
            if (component.charge in 0f..maxCap) {
                val pct = (component.charge / maxCap).coerceIn(0f, 1f)
                drawRect(Color(0xFF4CAF50), topLeft = Offset(padding + strokeSize, padding*2 + strokeSize + (cellSize - padding*4 - strokeSize*2)*(1f-pct)), size = Size(cellSize - padding*2 - strokeSize*2, (cellSize - padding*4 - strokeSize*2)*pct))
            }
            drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.35f, padding), size = Size(cellSize * 0.3f, padding))
            drawLine(Color(0xFFD0BCFF), start = Offset(cx, cy - padding), end = Offset(cx, cy + padding), strokeWidth=strokeSize*0.5f)
            drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding, cy), end = Offset(cx + padding, cy), strokeWidth=strokeSize*0.5f)
        }
        
        ComponentType.NUCLEAR_REACTOR, ComponentType.WIND_TURBINE, ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, ComponentType.THERMOELECTRIC_GENERATOR, ComponentType.INFINITE_BATTERY -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFCCFF00), Color(0xFF00AA00))),
                topLeft = Offset(padding, padding), 
                size = Size(cellSize - padding*2, cellSize - padding*2)
            )
            drawCircle(Color.Black, radius = Math.max(1f, strokeSize), center = Offset(cx, cy))
        }
        
        ComponentType.GENERATOR -> {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF4F378B), Color(0xFF221144)), center = Offset(cx, cy), radius = cellSize*0.4f),
                radius = cellSize * 0.4f, 
                center = Offset(cx, cy)
            )
            drawCircle(Color(0xFFD0BCFF), radius = cellSize * 0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
            // Draw Sine Wave
            val path = Path().apply {
                moveTo(cx - cellSize*0.2f, cy)
                cubicTo(cx - cellSize*0.1f, cy - cellSize*0.2f, cx + cellSize*0.1f, cy + cellSize*0.2f, cx + cellSize*0.2f, cy)
            }
            drawPath(path, Color(0xFFD0BCFF), style = Stroke(strokeSize*0.5f))
        }

        ComponentType.SOLAR_PANEL -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F1D45))),
                topLeft = Offset(padding, padding), 
                size = Size(cellSize - padding*2, cellSize - padding*2)
            )
            drawRect(Color(0xFF49454F), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
            drawLine(Color(0xFFADD8E6), start = Offset(cx, padding), end = Offset(cx, cellSize-padding), strokeWidth=strokeSize*0.3f)
            drawLine(Color(0xFFADD8E6), start = Offset(padding, cy), end = Offset(cellSize-padding, cy), strokeWidth=strokeSize*0.3f)
        }

        ComponentType.DIODE -> {
            drawLine(color, start = Offset(cx, 0f), end = Offset(cx, padding*2), strokeWidth = strokeSize) // Top wire
            drawLine(color, start = Offset(cx, cellSize - padding*2), end = Offset(cx, cellSize), strokeWidth = strokeSize) // Bottom wire
            val path = Path().apply {
                moveTo(cx - padding*1.5f, cellSize - padding*2)
                lineTo(cx + padding*1.5f, cellSize - padding*2)
                lineTo(cx, padding*2)
                close()
            }
            drawPath(path, color)
            drawLine(color, start = Offset(cx - padding*1.5f, padding*2), end = Offset(cx + padding*1.5f, padding*2), strokeWidth = strokeSize)
        }

        ComponentType.RESISTOR -> {
            drawLine(color, start = Offset(cx, 0f), end = Offset(cx, padding*1.5f), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx, cellSize - padding*1.5f), end = Offset(cx, cellSize), strokeWidth = strokeSize)
            drawRect(Color(0xFF4F378B), topLeft = Offset(cx - padding, padding*1.5f), size = Size(padding*2, cellSize - padding*3))
            drawRect(Color.Red, topLeft = Offset(cx - padding, padding*1.5f + 4f), size = Size(padding*2, strokeSize*0.6f))
            drawRect(Color.Yellow, topLeft = Offset(cx - padding, cy - strokeSize*0.3f), size = Size(padding*2, strokeSize*0.6f))
        }

        ComponentType.CAPACITOR -> {
            drawLine(color, start = Offset(cx, 0f), end = Offset(cx, cy - padding), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx, cy + padding), end = Offset(cx, cellSize), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx - padding*1.5f, cy - padding), end = Offset(cx + padding*1.5f, cy - padding), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx - padding*1.5f, cy + padding), end = Offset(cx + padding*1.5f, cy + padding), strokeWidth = strokeSize)
        }

        ComponentType.BULB -> {
            val bulbColor = if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF332D41)
            if (component.isPowered) drawCircle(Color(0xFFD0BCFF).copy(alpha = 0.4f), radius = cellSize * 0.45f, center = Offset(cx, cy))
            drawCircle(bulbColor, radius = cellSize * 0.35f, center = Offset(cx, cy))
            drawLine(Color(0xFF1C1B1F), start = Offset(cx - padding*1.5f, cy), end = Offset(cx + padding*1.5f, cy), strokeWidth = strokeSize*0.5f)
            drawLine(color, start = Offset(cx, cellSize * 0.85f), end = Offset(cx, cellSize), strokeWidth = strokeSize)
        }

        ComponentType.LED -> {
            val ledColor = if (component.isPowered) Color(0xFFFF3366) else Color(0xFF4A1020)
            if (component.isPowered) drawCircle(ledColor.copy(alpha = 0.6f), radius = cellSize * 0.4f, center = Offset(cx, cy))
            drawCircle(ledColor, radius = cellSize * 0.25f, center = Offset(cx, cy))
            drawLine(color, start = Offset(cx, cellSize * 0.75f), end = Offset(cx, cellSize), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx, 0f), end = Offset(cx, cellSize * 0.25f), strokeWidth = strokeSize)
        }

        ComponentType.MOTOR -> {
            drawCircle(Color(0xFF2B2930), radius = cellSize * 0.4f, center = Offset(cx, cy))
            drawCircle(color, radius = cellSize * 0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
            // Draw 'M'
            val path = Path().apply {
                moveTo(cx - padding, cy + padding)
                lineTo(cx - padding, cy - padding)
                lineTo(cx, cy)
                lineTo(cx + padding, cy - padding)
                lineTo(cx + padding, cy + padding)
            }
            drawPath(path, color, style = Stroke(strokeSize*0.5f))
        }

        ComponentType.SPEAKER -> {
            val path = Path().apply {
                moveTo(cx, cy - padding)
                lineTo(cx + padding, cy - padding*2)
                lineTo(cx + padding, cy + padding*2)
                lineTo(cx, cy + padding)
                close()
            }
            drawPath(path, color)
            drawRect(color, topLeft = Offset(cx - padding, cy - padding), size = Size(padding, padding*2))
            if (component.isPowered) {
                drawArc(color, -60f, 120f, false, topLeft = Offset(cx + padding, cy - padding*1.5f), size = Size(padding*2, padding*3), style = Stroke(strokeSize*0.5f))
            }
        }

        ComponentType.SWITCH_OPEN -> {
            drawCircle(color, radius = strokeSize, center = Offset(cx, padding*2))
            drawCircle(color, radius = strokeSize, center = Offset(cx, cellSize - padding*2))
            drawLine(if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF49454F), start = Offset(cx, padding*2), end = Offset(cx - padding*2, cellSize*0.75f), strokeWidth = strokeSize * 0.8f)
        }
        
        ComponentType.SWITCH_CLOSED -> {
            drawCircle(color, radius = strokeSize, center = Offset(cx, padding*2))
            drawCircle(color, radius = strokeSize, center = Offset(cx, cellSize - padding*2))
            drawLine(color, start = Offset(cx, padding*2), end = Offset(cx, cellSize - padding*2), strokeWidth = strokeSize * 0.8f)
        }

        ComponentType.PUSH_BUTTON -> {
            drawCircle(color, radius = strokeSize, center = Offset(cx - padding*1.5f, cy))
            drawCircle(color, radius = strokeSize, center = Offset(cx + padding*1.5f, cy))
            // Button Top
            drawRect(if(component.isPowered) color else Color(0xFF49454F), topLeft = Offset(cx - padding*2, cy - padding*1.5f), size = Size(padding*4, strokeSize*1.5f))
        }

        ComponentType.LOGIC_AND -> {
            val path = Path().apply {
                moveTo(cx - padding*1.5f, cy - padding*1.5f)
                lineTo(cx, cy - padding*1.5f)
                arcTo(androidx.compose.ui.geometry.Rect(cx - padding*1.5f, cy - padding*1.5f, cx + padding*1.5f, cy + padding*1.5f), -90f, 180f, false)
                lineTo(cx - padding*1.5f, cy + padding*1.5f)
                close()
            }
            drawPath(path, color, style = Stroke(strokeSize))
            // Inputs/Outputs
            drawLine(color, start = Offset(cx - padding*1.5f, cy - padding), end = Offset(0f, cy - padding), strokeWidth = strokeSize*0.5f)
            drawLine(color, start = Offset(cx - padding*1.5f, cy + padding), end = Offset(0f, cy + padding), strokeWidth = strokeSize*0.5f)
            drawLine(color, start = Offset(cx + padding*1.5f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
            
            // Text 
            drawLine(color, start = Offset(cx - strokeSize, cy + strokeSize), end = Offset(cx, cy - strokeSize), strokeWidth=2f)
            drawLine(color, start = Offset(cx, cy - strokeSize), end = Offset(cx + strokeSize, cy + strokeSize), strokeWidth=2f)
        }

        ComponentType.LOGIC_OR -> {
           // Simplify LOGIC_OR visually to a diamond for speed, the path takes effort to get perfect 
           drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
           drawLine(color, start=Offset(cx-padding, cy-padding), end=Offset(cx+padding, cy+padding), strokeWidth=2f)
        }
        
        ComponentType.LOGIC_NOT -> {
            // Triangle
            val path = Path().apply {
                moveTo(cx - padding, cy - padding)
                lineTo(cx + padding, cy)
                lineTo(cx - padding, cy + padding)
                close()
            }
            drawPath(path, color, style=Stroke(strokeSize))
            drawCircle(Color(0xFF2B2930), radius = strokeSize*2, center = Offset(cx + padding + strokeSize*2, cy))
            drawCircle(color, radius = strokeSize*2, center = Offset(cx + padding + strokeSize*2, cy), style=Stroke(strokeSize))
        }

        ComponentType.RELAY -> {
            drawRect(color, topLeft = Offset(padding, padding*1.5f), size = Size(cellSize - padding*2, cellSize - padding*3), style = Stroke(strokeSize))
            // Coil
            val path = Path().apply {
                moveTo(cx - padding, padding*1.5f + strokeSize)
                quadraticBezierTo(cx, cy, cx - padding, cellSize - padding*1.5f - strokeSize)
            }
            drawPath(path, color, style = Stroke(strokeSize*0.5f))
            drawLine(if (component.logicState) Color(0xFFD0BCFF) else color, start = Offset(cx + padding*0.5f, padding*1.5f + strokeSize), end = Offset(cx + padding*0.5f, cellSize - padding*1.5f - strokeSize), strokeWidth = strokeSize)
        }

        ComponentType.TRANSISTOR -> {
            drawCircle(color, radius = cellSize * 0.35f, center = Offset(cx, cy), style = Stroke(strokeSize))
            drawLine(color, start = Offset(cx - padding, cy - padding), end = Offset(cx - padding, cy + padding), strokeWidth = strokeSize)
            // Base
            drawLine(color, start = Offset(0f, cy), end = Offset(cx - padding, cy), strokeWidth = strokeSize)
            // Collector
            drawLine(color, start = Offset(cx - padding, cy - padding*0.5f), end = Offset(cx + padding, 0f), strokeWidth = strokeSize)
            // Emitter
            drawLine(color, start = Offset(cx - padding, cy + padding*0.5f), end = Offset(cx + padding, cellSize), strokeWidth = strokeSize)
        }

        ComponentType.RGB_LED -> {
            val r = if(component.isPowered) 1f else 0.3f
            val g = if(component.logicState) 1f else 0.3f
            val ledColor = Color(r, g, 1f) // Just a visual fake
            
            if (component.isPowered) drawCircle(ledColor.copy(alpha = 0.6f), radius = cellSize * 0.45f, center = Offset(cx, cy))
            drawCircle(ledColor, radius = cellSize * 0.35f, center = Offset(cx, cy))
            
            // Four pins
            drawLine(color, start = Offset(cx - padding, cellSize * 0.85f), end = Offset(cx - padding, cellSize), strokeWidth = strokeSize*0.5f)
            drawLine(color, start = Offset(cx - padding*0.33f, cellSize * 0.85f), end = Offset(cx - padding*0.33f, cellSize), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx + padding*0.33f, cellSize * 0.85f), end = Offset(cx + padding*0.33f, cellSize), strokeWidth = strokeSize*0.5f)
            drawLine(color, start = Offset(cx + padding, cellSize * 0.85f), end = Offset(cx + padding, cellSize), strokeWidth = strokeSize*0.5f)
        }

        ComponentType.SEVEN_SEGMENT -> {
            drawRect(Color(0xFF2B2930), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
            val lit = if(component.isPowered) Color(0xFFFF3366) else Color(0xFF4A1020)
            
            val segW = cellSize * 0.3f
            val segH = strokeSize
            // Fake an '8'
            drawRect(lit, topLeft = Offset(cx - segW/2, cy - padding - segH), size = Size(segW, segH)) // A
            drawRect(lit, topLeft = Offset(cx - segW/2, cy - segH/2), size = Size(segW, segH)) // G
            drawRect(lit, topLeft = Offset(cx - segW/2, cy + padding), size = Size(segW, segH)) // D
            
            drawRect(lit, topLeft = Offset(cx + segW/2, cy - padding), size = Size(segH, padding)) // B
            drawRect(lit, topLeft = Offset(cx + segW/2, cy), size = Size(segH, padding)) // C
            drawRect(lit, topLeft = Offset(cx - segW/2 - segH, cy - padding), size = Size(segH, padding)) // F
            drawRect(lit, topLeft = Offset(cx - segW/2 - segH, cy), size = Size(segH, padding)) // E
        }

        ComponentType.PULSE_GENERATOR -> {
            drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
            val path = Path().apply {
                moveTo(cx - padding*0.8f, cy)
                lineTo(cx - padding*0.4f, cy)
                lineTo(cx - padding*0.4f, cy - padding*0.8f)
                lineTo(cx + padding*0.4f, cy - padding*0.8f)
                lineTo(cx + padding*0.4f, cy)
                lineTo(cx + padding*0.8f, cy)
            }
            drawPath(path, if(component.logicState) Color(0xFF00FFCC) else color, style = Stroke(strokeSize))
        }

        ComponentType.BATTERY_PACK -> {
            drawRect(Color(0xFF381E72), topLeft = Offset(padding, padding*1.5f), size = Size(cellSize - padding*2, cellSize - padding*3))
            val maxCap = component.getMaxCap()
            if (component.charge in 0f..maxCap) {
                val pct = (component.charge / maxCap).coerceIn(0f, 1f)
                drawRect(Color(0xFF4CAF50), topLeft = Offset(padding + strokeSize, padding*1.5f + strokeSize + (cellSize - padding*3 - strokeSize*2)*(1f-pct)), size = Size(cellSize - padding*2 - strokeSize*2, (cellSize - padding*3 - strokeSize*2)*pct))
            }
            drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.25f, padding), size = Size(cellSize * 0.15f, padding*0.5f))
            drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.6f, padding), size = Size(cellSize * 0.15f, padding*0.5f))
            drawLine(Color(0xFFD0BCFF), start = Offset(cx, cy - padding), end = Offset(cx, cy + padding), strokeWidth=strokeSize*0.5f)
            drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding*1.2f, cy), end = Offset(cx + padding*1.2f, cy), strokeWidth=strokeSize*0.5f)
            drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding*1.2f, cy-padding), end = Offset(cx - padding*1.2f, cy+padding), strokeWidth=strokeSize) // - marker
            drawLine(Color(0xFFD0BCFF), start = Offset(cx + padding*1.2f, cy-padding), end = Offset(cx + padding*1.2f, cy+padding), strokeWidth=strokeSize) // + marker
        }
        
        ComponentType.INDUCTOR -> {
            drawLine(color, start = Offset(cx, 0f), end = Offset(cx, padding), strokeWidth = strokeSize)
            drawLine(color, start = Offset(cx, cellSize - padding), end = Offset(cx, cellSize), strokeWidth = strokeSize)
            // 4 loops
            for(i in 0..3) {
                drawArc(color, -90f, 180f, false, topLeft = Offset(cx - padding, padding + i*(cellSize-padding*2)/4), size = Size(padding*2, (cellSize-padding*2)/4), style = Stroke(strokeSize))
            }
        }
        
        ComponentType.DIP_SWITCH -> {
            drawRect(Color(0xFFC62828), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
            for(i in 0..3) {
                val yOffset = padding*1.5f + i*(cellSize - padding*3)/3
                drawRect(Color(0xFF1C1B1F), topLeft = Offset(padding*1.5f, yOffset), size = Size(padding*3f, strokeSize*1.5f))
                // small switch peg
                val pegX = if (component.logicState) padding*1.5f else padding*3.5f 
                drawRect(Color(0xFFFFF7FA), topLeft = Offset(pegX, yOffset), size = Size(padding, strokeSize*1.5f))
            }
        }

        ComponentType.TIMER_555 -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                topLeft = Offset(padding, padding), 
                size = Size(cellSize - padding*2, cellSize - padding*2)
            )
            drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
            // Notch
            drawArc(color, 90f, 180f, false, topLeft = Offset(cx - padding*0.5f, padding-padding*0.5f), size = Size(padding, padding), style = Stroke(strokeSize*0.5f))
            // 8 pins total
            for (i in 1..4) {
               val yOff = padding + i*(cellSize-padding*2)/5
               // Left
               drawLine(Color(0xFF9E9E9E), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 4f)
               // Right
               drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 4f)
            }
            // 555 symbol indicator inside
            drawCircle(Color(0xFF00FFCC), radius = strokeSize, center = Offset(cx, cy))
        }

        ComponentType.MICROCONTROLLER -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                topLeft = Offset(padding, padding), 
                size = Size(cellSize - padding*2, cellSize - padding*2)
            )
            drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
            
            // Draw Pins
            for (i in 1..3) {
                // Top
                drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i*padding*0.5f, 0f), end = Offset(cx - padding + i*padding*0.5f, padding), strokeWidth = 4f)
                // Bottom
                drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i*padding*0.5f, cellSize), end = Offset(cx - padding + i*padding*0.5f, cellSize - padding), strokeWidth = 4f)
                // Left
                drawLine(Color(0xFF9E9E9E), start = Offset(0f, cy - padding + i*padding*0.5f), end = Offset(padding, cy - padding + i*padding*0.5f), strokeWidth = 4f)
                // Right
                drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, cy - padding + i*padding*0.5f), end = Offset(cellSize - padding, cy - padding + i*padding*0.5f), strokeWidth = 4f)
            }
            
            // Script indicator
            if (component.extraData.isNotEmpty()) {
                drawCircle(Color(0xFF00FFCC), radius = strokeSize, center = Offset(cx, cy))
            }
        }
        ComponentType.MEMORY_RAM, ComponentType.MEMORY_ROM -> {
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1E3A28), Color(0xFF0F1E14))),
                topLeft = Offset(padding, padding*1.5f), 
                size = Size(cellSize - padding*2, cellSize - padding*3)
            )
            drawRect(color, topLeft = Offset(padding, padding*1.5f), size = Size(cellSize - padding*2, cellSize - padding*3), style = Stroke(strokeSize))
            
            // Memory chips on board
            for(i in -1..1) {
                drawRect(Color(0xFF0F1E14), topLeft = Offset(cx + i*(padding*0.8f) - padding*0.3f, cy - padding*0.8f), size = Size(padding*0.6f, padding*1.6f))
            }
            
            // Output pins bottom edge
            for(i in 1..4) {
               drawLine(Color(0xFFD3A82C), start = Offset(padding + i*(cellSize-padding*2)/5, cellSize-padding*1.5f), end = Offset(padding + i*(cellSize-padding*2)/5, cellSize), strokeWidth=strokeSize)
            }
            if (component.type == ComponentType.MEMORY_RAM) {
                drawCircle(Color(0xFF4CAF50), radius = strokeSize, center = Offset(cx, cy))
            } else {
                drawCircle(Color(0xFF607D8B), radius = strokeSize, center = Offset(cx, cy))
            }
        }
        
        ComponentType.MONITOR_OLED, ComponentType.CRT_MONITOR -> {
            drawRect(Color(0xFF111111), topLeft = Offset(padding*0.5f, padding), size = Size(cellSize - padding, cellSize - padding*2.5f))
            drawRect(Color(0xFF49454F), topLeft = Offset(padding*0.5f, padding), size = Size(cellSize - padding, cellSize - padding*2.5f), style = Stroke(strokeSize))
            // Stand
            drawRect(Color(0xFF666666), topLeft = Offset(cx - padding*0.5f, cellSize - padding*1.5f), size = Size(padding, padding*1.5f))
            
            if (component.isPowered) {
                // Determine text/data to display
                val txt = component.extraData.substringAfter("display=").substringBefore("|").ifEmpty { "PC_OK" }
                
                // Draw a retro glowing text or just green bars for now since we don't have text draw scope easily
                drawRect(Color(0xFF00FF00), topLeft = Offset(padding*1.5f, padding*2f), size = Size((cellSize - padding*3) * (if (txt.length % 2 == 0) 0.5f else 0.8f), padding*0.8f))
                if (txt.length > 3) {
                    drawRect(Color(0xFF00FF00), topLeft = Offset(padding*1.5f, padding*3f), size = Size((cellSize - padding*3) * 0.4f, padding*0.8f))
                }
            }
        }
        else -> {
            if (component.type.category == ComponentCategory.MATERIALS || component.type.category == ComponentCategory.HYDRAULICS) {
                val matColor = when(component.type) {
                    ComponentType.WATER, ComponentType.INFINITE_WATER -> Color(0xAA2196F3)
                    ComponentType.LAVA, ComponentType.INFINITE_LAVA -> Color(0xAAFF5722)
                    ComponentType.OIL -> Color(0xAA212121)
                    ComponentType.ACID -> Color(0xAA8BC34A)
                    ComponentType.SAND -> Color(0xFFFFC107)
                    ComponentType.DIRT -> Color(0xFF795548)
                    ComponentType.STONE -> Color(0xFF9E9E9E)
                    ComponentType.GLASS -> Color(0x44FFFFFF)
                    ComponentType.WOOD -> Color(0xFF8D6E63)
                    ComponentType.FIRE -> Color(0xFFFF9800)
                    ComponentType.ICE -> Color(0xAA80DEEA)
                    ComponentType.STEAM -> Color(0xAAE0E0E0)
                    ComponentType.SLIME -> Color(0xAA00FF00)
                    ComponentType.RUBBER -> Color(0xFF424242)
                    ComponentType.DIAMOND -> Color(0xAA00BCD4)
                    ComponentType.COAL -> Color(0xFF3E2723)
                    ComponentType.SPONGE -> Color(0xFFFFEB3B)
                    ComponentType.GASOLINE -> Color(0xAAFFC107)
                    ComponentType.LIQUID_NITROGEN -> Color(0xAA4DD0E1)
                    ComponentType.URANIUM -> Color(0xAA76FF03)
                    ComponentType.MAGIC_DUST -> Color(0xAAE040FB)
                    ComponentType.PIPE -> Color(0xFF546E7A)
                    ComponentType.FLUID_DRAIN, ComponentType.VOID_HOLE -> Color(0xFF000000)
                    
                    ComponentType.STEEL -> Color(0xFFB0BEC5)
                    ComponentType.COPPER -> Color(0xFFD84315)
                    ComponentType.GOLD -> Color(0xFFFFD54F)
                    ComponentType.ALUMINUM -> Color(0xFFCFD8DC)
                    ComponentType.PLASTIC -> Color(0xFFFFCC80)
                    ComponentType.CLAY -> Color(0xFFBCAAA4)
                    ComponentType.BRICK -> Color(0xFFD32F2F)
                    ComponentType.OBSIDIAN -> Color(0xFF1C1C1C)
                    ComponentType.BEDROCK -> Color(0xFF000000)
                    
                    else -> Color.Transparent
                }
                drawRect(matColor, size = Size(cellSize, cellSize))
                
                // Texture overlays
                if (component.type == ComponentType.BRICK) {
                    drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize*0.33f), end = Offset(cellSize, cellSize*0.33f), strokeWidth = 2f)
                    drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize*0.66f), end = Offset(cellSize, cellSize*0.66f), strokeWidth = 2f)
                } else if (component.type in listOf(ComponentType.STEEL, ComponentType.COPPER, ComponentType.ALUMINUM, ComponentType.GOLD)) {
                    val shine = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Transparent, Color(0x44FFFFFF), Color.Transparent))
                    drawRect(shine, size = Size(cellSize, cellSize))
                } else if (component.type == ComponentType.DIAMOND) {
                    drawLine(Color.White, start = Offset(cellSize*0.2f, 0f), end = Offset(cellSize, cellSize*0.8f), strokeWidth = 1.5f)
                }
                if (component.type == ComponentType.INFINITE_WATER || 
                    component.type == ComponentType.INFINITE_LAVA ||
                    component.type == ComponentType.INFINITE_OIL ||
                    component.type == ComponentType.INFINITE_ACID ||
                    component.type == ComponentType.INFINITE_SLIME ||
                    component.type == ComponentType.INFINITE_GASOLINE ||
                    component.type == ComponentType.INFINITE_LIQUID_NITROGEN ||
                    component.type == ComponentType.INFINITE_STEAM) {
                    drawRect(Color.White, topLeft = Offset(padding*2, padding*2), size = Size(cellSize - padding*4, cellSize - padding*4))
                }
                if (component.type == ComponentType.VOID_HOLE || component.type == ComponentType.FLUID_DRAIN) {
                    drawCircle(Color.Red, radius = strokeSize*0.5f, center = Offset(cx, cy))
                }
                return // skip the fallback pins
            }
            
            // Generic 8-pin IC fallback for advanced components (Logic gates, sensors, mux)
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                topLeft = Offset(padding, padding), 
                size = Size(cellSize - padding*2, cellSize - padding*2)
            )
            drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
            
            // Draw generic pins
            for (i in 1..2) {
                val yOff = cy - padding + i*padding
                // Left
                drawLine(Color(0xFF9E9E9E), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 4f)
                // Right
                drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 4f)
            }
            
            // Simple symbol for category
            val symColor = when(component.type.category) {
                ComponentCategory.LOGIC -> Color(0xFFE91E63) 
                ComponentCategory.SENSORS -> Color(0xFF00BCD4)
                ComponentCategory.OUTPUTS -> Color(0xFFFF9800)
                ComponentCategory.ANALOG_ICS -> Color(0xFF9C27B0)
                ComponentCategory.ADVANCED -> Color(0xFF3F51B5)
                ComponentCategory.SWITCHES -> Color(0xFF4CAF50)
                ComponentCategory.POWER -> Color(0xFFFFEB3B)
                else -> Color.Gray
            }
            drawCircle(symColor, radius = strokeSize*1.5f, center = Offset(cx, cy))
            
            // Draw a small indicator of the name
            val hash = component.type.name.hashCode()
            val r = (hash and 0xFF).toFloat() / 255f
            val g = ((hash shr 8) and 0xFF).toFloat() / 255f
            val b = ((hash shr 16) and 0xFF).toFloat() / 255f
            val dotColor = Color(r, g, b, 1f)
            drawCircle(dotColor, radius = strokeSize * 0.8f, center = Offset(cx, cy - strokeSize * 2))
        }
    }
} */



fun getIconForType(type: ComponentType): ImageVector {
    return when(type) {
        ComponentType.PAN -> Icons.Default.PanTool
        ComponentType.ROTATE -> Icons.Default.RotateRight
        ComponentType.INSPECT -> Icons.Default.Code
        ComponentType.MULTIMETER -> Icons.Default.Speed
        ComponentType.EMPTY -> Icons.Default.Backspace
        ComponentType.BATTERY, ComponentType.INFINITE_BATTERY -> Icons.Default.BatteryChargingFull
        ComponentType.BATTERY_PACK -> Icons.Default.BatteryFull
        ComponentType.COIN_CELL -> Icons.Default.Adjust
        ComponentType.GENERATOR -> Icons.Default.Sync
        ComponentType.SOLAR_PANEL -> Icons.Default.SolarPower
        ComponentType.AC_SOURCE -> Icons.Default.ElectricalServices
        ComponentType.WIND_TURBINE -> Icons.Default.Loop
        ComponentType.NUCLEAR_REACTOR -> Icons.Default.Warning
        ComponentType.GEOTHERMAL_GENERATOR -> Icons.Default.Whatshot
        ComponentType.HYDRO_GENERATOR -> Icons.Default.Waves
        ComponentType.THERMOELECTRIC_GENERATOR -> Icons.Default.Thermostat
        ComponentType.WIRE_ANY -> Icons.Default.Timeline
        ComponentType.RESISTOR -> Icons.Default.HorizontalRule
        ComponentType.CAPACITOR -> Icons.Default.ViewStream
        ComponentType.INDUCTOR -> Icons.Default.Gesture
        ComponentType.DIODE -> Icons.Default.ChangeHistory
        ComponentType.ZENER_DIODE -> Icons.Default.Details
        ComponentType.FUSE -> Icons.Default.Remove
        ComponentType.TRANSFORMER -> Icons.Default.Transform
        ComponentType.SUPERCONDUCTOR, ComponentType.ICE -> Icons.Default.AcUnit
        ComponentType.HIGH_VOLTAGE_CABLE -> Icons.Default.FlashOn
        ComponentType.FIBER_OPTIC -> Icons.Default.Cable
        ComponentType.SWITCH_OPEN, ComponentType.SWITCH_CLOSED -> Icons.Default.ToggleOff
        ComponentType.PUSH_BUTTON -> Icons.Default.SmartButton
        ComponentType.DIP_SWITCH -> Icons.Default.Tune
        ComponentType.REED_SWITCH -> Icons.Default.SwapHoriz
        ComponentType.LIMIT_SWITCH, ComponentType.MAGNET -> Icons.Default.VerticalAlignBottom
        ComponentType.RELAY -> Icons.Default.Transform
        ComponentType.TRANSISTOR -> Icons.Default.Toll
        ComponentType.MOSFET, ComponentType.MAGNETIC_CONTACT -> Icons.Default.SettingsInputComponent
        ComponentType.POTENTIOMETER -> Icons.Default.VolumeMute
        ComponentType.PRESSURE_PAD -> Icons.Default.KeyboardTab
        ComponentType.PHOTORESISTOR -> Icons.Default.WbTwilight
        ComponentType.THERMISTOR, ComponentType.TEMPERATURE_SENSOR -> Icons.Default.DeviceThermostat
        ComponentType.LIGHT_SENSOR, ComponentType.BULB -> Icons.Default.Lightbulb
        ComponentType.PROXIMITY_SENSOR -> Icons.Default.Sensors
        ComponentType.ULTRASONIC_SENSOR -> Icons.Default.CellWifi
        ComponentType.SOUND_SENSOR -> Icons.Default.Mic
        ComponentType.VIBRATION_SENSOR -> Icons.Default.Vibration
        ComponentType.GAS_SENSOR, ComponentType.STEAM -> Icons.Default.Cloud
        ComponentType.MOISTURE_SENSOR -> Icons.Default.WaterDrop
        ComponentType.HALL_EFFECT_SENSOR -> Icons.Default.CompassCalibration
        ComponentType.PIR_MOTION_SENSOR -> Icons.Default.DirectionsRun
        ComponentType.LED -> Icons.Default.CrisisAlert
        ComponentType.RGB_LED -> Icons.Default.Palette
        ComponentType.SEVEN_SEGMENT -> Icons.Default.Score
        ComponentType.FOURTEEN_SEGMENT -> Icons.Default.TextFormat
        ComponentType.LCD_DISPLAY_16X2 -> Icons.Default.DesktopWindows
        ComponentType.MOTOR -> Icons.Default.Settings
        ComponentType.SERVO_MOTOR -> Icons.Default.PrecisionManufacturing
        ComponentType.STEPPER_MOTOR -> Icons.Default.RotateRight
        ComponentType.SPEAKER, ComponentType.AMPLIFIER -> Icons.Default.VolumeUp
        ComponentType.BUZZER -> Icons.Default.NotificationsActive
        ComponentType.LASER_DIODE -> Icons.Default.Flare
        ComponentType.HEATER, ComponentType.FIRE -> Icons.Default.LocalFireDepartment
        ComponentType.COOLER -> Icons.Default.AcUnit
        ComponentType.WATER_PUMP, ComponentType.WATER, ComponentType.LAVA, ComponentType.OIL, ComponentType.ACID, ComponentType.INFINITE_WATER, ComponentType.INFINITE_LAVA -> Icons.Default.Water
        ComponentType.FAN -> Icons.Default.Air
        ComponentType.MONITOR_OLED, ComponentType.CRT_MONITOR -> Icons.Default.Monitor
        ComponentType.LOGIC_AND, ComponentType.LOGIC_OR, ComponentType.LOGIC_NOT -> Icons.Default.AccountTree
        ComponentType.LOGIC_NAND -> Icons.Default.MergeType
        ComponentType.LOGIC_NOR -> Icons.Default.AltRoute
        ComponentType.LOGIC_XOR -> Icons.Default.CallSplit
        ComponentType.LOGIC_XNOR, ComponentType.MULTIPLEXER -> Icons.Default.CallMerge
        ComponentType.PULSE_GENERATOR -> Icons.Default.AvTimer
        ComponentType.D_FLIP_FLOP, ComponentType.T_FLIP_FLOP, ComponentType.JK_FLIP_FLOP -> Icons.Default.Dns
        ComponentType.DEMULTIPLEXER -> Icons.Default.CallSplit
        ComponentType.SHIFT_REGISTER -> Icons.Default.ViewArray
        ComponentType.HALF_ADDER -> Icons.Default.PlusOne
        ComponentType.FULL_ADDER -> Icons.Default.Add
        ComponentType.LATCH_SR -> Icons.Default.Lock
        ComponentType.TIMER_555 -> Icons.Default.Timer
        ComponentType.OP_AMP -> Icons.Default.ChangeHistory
        ComponentType.ADC -> Icons.Default.GraphicEq
        ComponentType.DAC -> Icons.Default.ShowChart
        ComponentType.COMPARATOR -> Icons.Default.CompareArrows
        ComponentType.VOLTAGE_REGULATOR -> Icons.Default.BatterySaver
        ComponentType.BUFFER -> Icons.Default.SkipNext
        ComponentType.MICROCONTROLLER, ComponentType.MEMORY_RAM -> Icons.Default.Memory
        ComponentType.MEMORY_ROM -> Icons.Default.SdCard
        ComponentType.SAND, ComponentType.DIRT, ComponentType.STONE -> Icons.Default.Landscape
        ComponentType.GLASS -> Icons.Default.Window
        ComponentType.WOOD -> Icons.Default.Forest
        ComponentType.SLIME -> Icons.Default.Biotech
        ComponentType.RUBBER -> Icons.Default.Album
        ComponentType.DIAMOND -> Icons.Default.Diamond
        ComponentType.COAL -> Icons.Default.Co2
        ComponentType.SPONGE -> Icons.Default.CropSquare
        ComponentType.GASOLINE -> Icons.Default.LocalGasStation
        ComponentType.LIQUID_NITROGEN -> Icons.Default.SevereCold
        ComponentType.URANIUM -> Icons.Default.Warning
        ComponentType.MAGIC_DUST -> Icons.Default.AutoAwesome
        ComponentType.PIPE -> Icons.Default.Tune
        ComponentType.FLUID_DRAIN, ComponentType.VOID_HOLE -> Icons.Default.HighlightOff
        ComponentType.CONVEYOR_BELT -> Icons.Default.LinearScale
        ComponentType.PISTON -> Icons.Default.ArrowUpward
        ComponentType.DOUBLE_DOOR -> Icons.Default.MeetingRoom
        else -> {
            when (type.category) {
                ComponentCategory.LOGIC -> Icons.Default.AccountTree
                ComponentCategory.ADVANCED -> Icons.Default.Memory
                ComponentCategory.MATERIALS -> Icons.Default.Landscape
                ComponentCategory.OUTPUTS -> Icons.Default.DisplaySettings
                ComponentCategory.POWER -> Icons.Default.Power
                ComponentCategory.ANALOG_ICS -> Icons.Default.SettingsEthernet
                ComponentCategory.SENSORS -> Icons.Default.Sensors
                ComponentCategory.SWITCHES -> Icons.Default.ToggleOn
                else -> Icons.Default.Build
            }
        }
    }
}

// Modularized components section


// End of file

