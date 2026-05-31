package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
            onCellClicked = { x, y -> viewModel.onCellClicked(x, y) }
        )

        // Top Toolbar Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color(0xDD2A2A35), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleSimulation() }) { 
                Icon(if (state.isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle Simulation", tint = Color(0xFF00FFCC)) 
            }
            IconButton(onClick = { viewModel.clearGrid() }) { Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFFF5555)) }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { viewModel.showSaveDialog() }) { Icon(Icons.Default.Save, "Save", tint = Color.White) }
            IconButton(onClick = { viewModel.showLoadDialog() }) { Icon(Icons.Default.FolderOpen, "Load", tint = Color.White) }
            IconButton(onClick = { viewModel.showSettingsDialog() }) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
        }

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

        // Bottom Toolbox Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .background(Color(0xD2121215), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x22FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(8.dp)
        ) {
            BottomToolBar(
                selectedCategory = state.selectedCategory,
                selectedTool = state.selectedTool,
                onCategorySelected = { viewModel.selectCategory(it) },
                onToolSelected = { viewModel.selectTool(it) }
            )
        }

        if (state.showSaveDialog) SaveDialog({ viewModel.dismissDialogs() }, { name -> viewModel.saveScheme(name) })
        if (state.showLoadDialog) LoadDialog(savedSchemes, { viewModel.dismissDialogs() }, { scheme -> viewModel.loadScheme(scheme) }, { id -> viewModel.deleteScheme(id) })
        if (state.showSettingsDialog) SettingsDialog({ viewModel.dismissDialogs() }, { w, h -> viewModel.resizeGrid(w, h) }, { viewModel.getShareableString() }, { viewModel.importShareableString(it) })
        state.inspectCoordinates?.let { coords ->
            val comp = state.grid[coords.first][coords.second]
            InspectDialog(
                component = comp,
                onDismiss = { viewModel.dismissInspect() },
                onSave = { data, repair -> viewModel.updateComponentData(coords.first, coords.second, data, repair) }
            )
        }
        
        state.multimeterCoordinates?.let { coords ->
            val comp = state.grid[coords.first][coords.second]
            MultimeterDialog(
                component = comp,
                coords = coords,
                onDismiss = { viewModel.dismissMultimeter() }
            )
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Charge / Fuel", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    LinearProgressIndicator(
                        progress = { if (component.charge > 0) 1f else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Text("${component.charge}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
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
    onCellClicked: (Int, Int) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val baseCellSize = 60f

    val maxCanvasWidth = width * baseCellSize
    val maxCanvasHeight = height * baseCellSize

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
                    detectDragGestures { change, _ ->
                        val localPoint = (change.position - pan) / scale
                        val cellX = (localPoint.x / baseCellSize).toInt()
                        val cellY = (localPoint.y / baseCellSize).toInt()
                        if (cellX in 0 until width && cellY in 0 until height) {
                            onCellClicked(cellX, cellY)
                        }
                    }
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
            withTransform({
                translate(left = pan.x, top = pan.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawRect(Color(0xFF1E1E2E), size = Size(maxCanvasWidth, maxCanvasHeight))
                
                val visibleStartX = maxOf(0, (-pan.x / (scale * baseCellSize)).toInt() - 1)
                val visibleEndX = minOf(width, ((size.width - pan.x) / (scale * baseCellSize)).toInt() + 1)
                val visibleStartY = maxOf(0, (-pan.y / (scale * baseCellSize)).toInt() - 1)
                val visibleEndY = minOf(height, ((size.height - pan.y) / (scale * baseCellSize)).toInt() + 1)
                
                // Draw blueprint grid lines
                for (i in visibleStartX..visibleEndX) {
                    val x = i * baseCellSize
                    drawLine(Color(0x3349454F), start = Offset(x, 0f), end = Offset(x, maxCanvasHeight), strokeWidth = 2f)
                }
                for (j in visibleStartY..visibleEndY) {
                    val y = j * baseCellSize
                    drawLine(Color(0x3349454F), start = Offset(0f, y), end = Offset(maxCanvasWidth, y), strokeWidth = 2f)
                }
                
                drawRect(color = Color(0xFFD0BCFF).copy(alpha = 0.5f), size = Size(maxCanvasWidth, maxCanvasHeight), style = Stroke(width = 4f))

                for (x in visibleStartX until minOf(visibleEndX, width)) {
                    for (y in visibleStartY until minOf(visibleEndY, height)) {
                        if (grid[x][y].type != ComponentType.EMPTY) {
                            drawContext.canvas.save()
                            drawContext.transform.translate(x * baseCellSize, y * baseCellSize)
                            
                            // Apply Rotation
                            val angle = when(grid[x][y].direction) {
                                Direction.UP -> 0f
                                Direction.RIGHT -> 90f
                                Direction.DOWN -> 180f
                                Direction.LEFT -> 270f
                            }
                            if (angle != 0f) {
                                drawContext.transform.rotate(angle, Offset(baseCellSize/2, baseCellSize/2))
                            }
                            
                            com.example.engine.RenderEngine.drawComponent(this, grid, x, y, width, height, grid[x][y], baseCellSize)
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

            // Un-rotate the connection logic because we are drawing inside a rotated canvas.
            // Actually it's simpler: draw from center to edge based on RELATIVE direction.
            val currentDir = component.direction
            val relUp = when(currentDir) { Direction.UP -> up; Direction.RIGHT -> left; Direction.DOWN -> down; Direction.LEFT -> right }
            val relRight = when(currentDir) { Direction.UP -> right; Direction.RIGHT -> up; Direction.DOWN -> left; Direction.LEFT -> down }
            val relDown = when(currentDir) { Direction.UP -> down; Direction.RIGHT -> right; Direction.DOWN -> up; Direction.LEFT -> left }
            val relLeft = when(currentDir) { Direction.UP -> left; Direction.RIGHT -> down; Direction.DOWN -> right; Direction.LEFT -> up }

            if (relUp) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, 0f), strokeWidth = wStroke)
            if (relRight) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cellSize, cy), strokeWidth = wStroke)
            if (relDown) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = wStroke)
            if (relLeft) drawLine(wireColor, start = Offset(cx, cy), end = Offset(0f, cy), strokeWidth = wStroke)
            
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
            if (component.type.category == ComponentCategory.MATERIALS) {
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
                if (component.type == ComponentType.INFINITE_WATER || component.type == ComponentType.INFINITE_LAVA) {
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

@Composable
fun BottomToolBar(selectedCategory: ComponentCategory, selectedTool: ComponentType, onCategorySelected: (ComponentCategory) -> Unit, onToolSelected: (ComponentType) -> Unit) {
    Column(modifier = Modifier.background(Color.Transparent)) {
        ScrollableTabRow(
            selectedTabIndex = ComponentCategory.values().indexOf(selectedCategory),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                androidx.compose.material3.TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[ComponentCategory.values().indexOf(selectedCategory)]),
                    color = Color(0xFF00FFCC),
                    height = 3.dp
                )
            },
            divider = {},
            edgePadding = 16.dp
        ) {
            ComponentCategory.values().forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    text = { 
                        Text(
                            category.title, 
                            fontSize = 13.sp, 
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategory == category) Color(0xFF00FFCC) else Color(0xFFAAAAAA)
                        ) 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val componentsInCategory = ComponentType.values().filter { it.category == selectedCategory }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(120.dp).padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(componentsInCategory) { type ->
                ToolButton(
                    icon = getIconForType(type),
                    text = type.name.replace("_ANY", "").replace("_OPEN", "").replace("_CLOSED", ""),
                    isSelected = selectedTool == type,
                    onClick = { onToolSelected(type) }
                )
            }
        }
    }
}

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
        ComponentType.FLUID_DRAIN, ComponentType.VOID_HOLE -> Icons.Default.HighlightOff
        ComponentType.CONVEYOR_BELT -> Icons.Default.LinearScale
        ComponentType.PISTON -> Icons.Default.ArrowUpward
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

@Composable
fun ToolButton(icon: ImageVector, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF00FFCC) else Color(0x662A2A35)
    val tint = if (isSelected) Color(0xFF121215) else Color.White
    val borderColor = if (isSelected) Color.Transparent else Color(0x33FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .background(bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text.substringBefore("_").take(8), 
                style = MaterialTheme.typography.labelSmall, 
                fontSize = 10.sp,
                color = tint, 
                maxLines = 1,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun InspectDialog(component: GridComponent, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var textData by remember { mutableStateOf(component.extraData) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text("Inspect/Edit Component: ${component.type.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (component.type == ComponentType.MICROCONTROLLER) {
                    Text("Microcontroller Settings", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: cores=X|mhz=Y|mem_kb=Z", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        placeholder = { Text("Example: cores=2|mhz=16|mem_kb=1024\n\n-- SSM Script engine\n-- out(pin, val)\n-- Pin: 0(Top) 1(Right) 2(Bot) 3(Left)\n-- log(Hello World)\n-- if in(0) == 1 then out(1, 1)\n\nif 1==1 then log(Started)\nout(1, 1)") }
                    )
                } else if (component.type == ComponentType.MEMORY_RAM || component.type == ComponentType.MEMORY_ROM) {
                    Text("Memory Tuning", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Configurable properties (pipe separated)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        placeholder = { Text(if(component.type == ComponentType.MEMORY_RAM) "mem_kb=1024" else "mem_kb=16384") }
                    )
                } else if (component.type == ComponentType.MONITOR_OLED || component.type == ComponentType.CRT_MONITOR) {
                    Text("Display Properties", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: display=TextToDisplay", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        placeholder = { Text("e.g. display=Hello") }
                    )
                } else {
                    Text("Properties", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: key=value|key=value\n(v=voltage, c=capacity, r=resistance)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        placeholder = { Text("e.g. v=5|c=1000 or r=330") }
                    )
                    if (component.type in listOf(ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, ComponentType.INFINITE_BATTERY)) {
                        Spacer(modifier=Modifier.height(8.dp))
                        val currentV = if (component.charge >= 0f) {
                            val maxV = textData.split("|").find { it.startsWith("v=") }?.substring(2)?.toFloatOrNull() ?: 9f
                            maxV * (component.charge / com.example.engine.RenderEngine.getMaxCap(component))
                        } else 0f
                        Text("Live State: ${String.format(java.util.Locale.US, "%.1f", component.charge.coerceAtLeast(0f))} mAh / ${String.format(java.util.Locale.US, "%.2f", currentV)} V", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (component.isOverloaded) {
                        Spacer(modifier=Modifier.height(8.dp))
                        Text("STATUS: OVERLOADED/BURNED OUT", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier=Modifier.height(4.dp))
                        Button(onClick = { onSave(textData, true) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Repair Component") }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (component.charge >= 0f && !component.isOverloaded && component.type.category == ComponentCategory.POWER && component.type != ComponentType.AC_SOURCE && component.type != ComponentType.GENERATOR && component.type != ComponentType.SOLAR_PANEL) {
                        TextButton(onClick = { onSave(textData, true) }) { Text("Recharge") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(textData, false) }) { Text("Save & Update") }
                }
            }
        }
    }
}

// Dialogs 
@Composable
fun SettingsDialog(onDismiss: () -> Unit, onResize: (Int, Int) -> Unit, exportScheme: () -> String, importScheme: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(exportScheme().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val text = input.bufferedReader().use { reader -> reader.readText() }
                    importScheme(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Project Integration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { 
                    exportLauncher.launch("blueprint.esshim")
                }, modifier = Modifier.fillMaxWidth()) { 
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export to .esshim file") 
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { 
                    importLauncher.launch(arrayOf("*/*"))
                }, modifier = Modifier.fillMaxWidth()) { 
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import .esshim file") 
                }
                Spacer(Modifier.height(24.dp))
                
                Text("Workspace Setup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text("Warning: Resizing clears your current circuit!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                
                Button(onClick = { onResize(8, 8) }, modifier = Modifier.fillMaxWidth()) { Text("Small Prototype (8x8)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(16, 16) }, modifier = Modifier.fillMaxWidth()) { Text("Mobile Standard (16x16)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(32, 32) }, modifier = Modifier.fillMaxWidth()) { Text("Tablet Expansive (32x32)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(64, 64) }, modifier = Modifier.fillMaxWidth()) { Text("Massive Blueprint (64x64)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(128, 128) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Motherboard Level 1 (128x128)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(256, 256) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Motherboard Level 2 (256x256)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(512, 512) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("City Grid Level (512x512)") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Schema Blueprint") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Schema Name") }, singleLine = true) },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LoadDialog(schemes: List<CircuitScheme>, onDismiss: () -> Unit, onLoad: (CircuitScheme) -> Unit, onDelete: (Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Saved Blueprints", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (schemes.isEmpty()) Text("No saved schemas found.", modifier = Modifier.padding(16.dp))
                else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(schemes) { scheme ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).clickable { onLoad(scheme) }.padding(8.dp)) {
                                    Text(scheme.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("Size: ${scheme.width}x${scheme.height}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                IconButton(onClick = { onDelete(scheme.id) }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}
