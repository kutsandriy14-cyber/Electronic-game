package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent

object RenderEngine {

    fun getMaxCap(component: GridComponent): Float {
        val default = when(component.type) { ComponentType.COIN_CELL -> 100f; ComponentType.BATTERY_PACK -> 10000f; ComponentType.INFINITE_BATTERY -> 9999999f; ComponentType.NUCLEAR_REACTOR -> 1000000f; else -> 2500f }
        if (component.extraData.isEmpty()) return default
        val idx = component.extraData.indexOf("c=")
        if (idx != -1) {
            val end = component.extraData.indexOf('|', idx)
            val extracted = if (end != -1) component.extraData.substring(idx+2, end) else component.extraData.substring(idx+2)
            return extracted.toFloatOrNull() ?: default
        }
        return default
    }

    fun drawComponent(drawScope: DrawScope, grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int, component: GridComponent, cellSize: Float) {
        val cx = cellSize / 2
        val cy = cellSize / 2
        val padding = cellSize * 0.15f
        val color = if (component.isOverloaded) Color(0xFFFF3B30) else (if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF49454F))
        val strokeSize = cellSize * 0.1f

        drawScope.apply {
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

                    val up = y > 0 && grid[x][y-1].type != ComponentType.EMPTY && grid[x][y-1].type.category != ComponentCategory.TOOLS && grid[x][y-1].type.category != ComponentCategory.MATERIALS
                    val down = y < height - 1 && grid[x][y+1].type != ComponentType.EMPTY && grid[x][y+1].type.category != ComponentCategory.TOOLS && grid[x][y+1].type.category != ComponentCategory.MATERIALS
                    val left = x > 0 && grid[x-1][y].type != ComponentType.EMPTY && grid[x-1][y].type.category != ComponentCategory.TOOLS && grid[x-1][y].type.category != ComponentCategory.MATERIALS
                    val right = x < width - 1 && grid[x+1][y].type != ComponentType.EMPTY && grid[x+1][y].type.category != ComponentCategory.TOOLS && grid[x+1][y].type.category != ComponentCategory.MATERIALS

                    val currentDir = component.direction
                    val relUp = when(currentDir) { Direction.UP -> up; Direction.RIGHT -> left; Direction.DOWN -> down; Direction.LEFT -> right }
                    val relRight = when(currentDir) { Direction.UP -> right; Direction.RIGHT -> up; Direction.DOWN -> left; Direction.LEFT -> down }
                    val relDown = when(currentDir) { Direction.UP -> down; Direction.RIGHT -> right; Direction.DOWN -> up; Direction.LEFT -> left }
                    val relLeft = when(currentDir) { Direction.UP -> left; Direction.RIGHT -> down; Direction.DOWN -> right; Direction.LEFT -> up }

                    if (relUp) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, 0f), strokeWidth = wStroke)
                    if (relRight) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cellSize, cy), strokeWidth = wStroke)
                    if (relDown) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = wStroke)
                    if (relLeft) drawLine(wireColor, start = Offset(cx, cy), end = Offset(0f, cy), strokeWidth = wStroke)
                    
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
                    val maxCap = getMaxCap(component)
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
                    drawLine(color, start = Offset(cx, 0f), end = Offset(cx, padding*2), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx, cellSize - padding*2), end = Offset(cx, cellSize), strokeWidth = strokeSize)
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

                ComponentType.DISPLAY_PIXEL -> {
                    val pixelColor = if (component.isPowered) Color.White else Color(0xFF111111)
                    val glow = if (component.isPowered) Color(0x33FFFFFF) else Color.Transparent
                    drawRect(glow, topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize))
                    drawRect(pixelColor, topLeft = Offset(cellSize * 0.1f, cellSize * 0.1f), size = Size(cellSize * 0.8f, cellSize * 0.8f))
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
                    drawLine(color, start = Offset(cx - padding*1.5f, cy - padding), end = Offset(0f, cy - padding), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx - padding*1.5f, cy + padding), end = Offset(0f, cy + padding), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx + padding*1.5f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
                    
                    drawLine(color, start = Offset(cx - strokeSize, cy + strokeSize), end = Offset(cx, cy - strokeSize), strokeWidth=2f)
                    drawLine(color, start = Offset(cx, cy - strokeSize), end = Offset(cx + strokeSize, cy + strokeSize), strokeWidth=2f)
                }

                ComponentType.LOGIC_OR -> {
                   drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                   drawLine(color, start=Offset(cx-padding, cy-padding), end=Offset(cx+padding, cy+padding), strokeWidth=2f)
                }
                
                ComponentType.LOGIC_NOT -> {
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
                    val path = Path().apply {
                        moveTo(cx - padding, padding*1.5f + strokeSize)
                        quadraticTo(cx, cy, cx - padding, cellSize - padding*1.5f - strokeSize)
                    }
                    drawPath(path, color, style = Stroke(strokeSize*0.5f))
                    drawLine(if (component.logicState) Color(0xFFD0BCFF) else color, start = Offset(cx + padding*0.5f, padding*1.5f + strokeSize), end = Offset(cx + padding*0.5f, cellSize - padding*1.5f - strokeSize), strokeWidth = strokeSize)
                }

                ComponentType.TRANSISTOR -> {
                    drawCircle(color, radius = cellSize * 0.35f, center = Offset(cx, cy), style = Stroke(strokeSize))
                    drawLine(color, start = Offset(cx - padding, cy - padding), end = Offset(cx - padding, cy + padding), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(0f, cy), end = Offset(cx - padding, cy), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx - padding, cy - padding*0.5f), end = Offset(cx + padding, 0f), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx - padding, cy + padding*0.5f), end = Offset(cx + padding, cellSize), strokeWidth = strokeSize)
                }

                ComponentType.RGB_LED -> {
                    val r = if(component.isPowered) 1f else 0.3f
                    val g = if(component.logicState) 1f else 0.3f
                    val ledColor = Color(r, g, 1f) 
                    
                    if (component.isPowered) drawCircle(ledColor.copy(alpha = 0.6f), radius = cellSize * 0.45f, center = Offset(cx, cy))
                    drawCircle(ledColor, radius = cellSize * 0.35f, center = Offset(cx, cy))
                    
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
                    val maxCap = getMaxCap(component)
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
                    for(i in 0..3) {
                        drawArc(color, -90f, 180f, false, topLeft = Offset(cx - padding, padding + i*(cellSize-padding*2)/4), size = Size(padding*2, (cellSize-padding*2)/4), style = Stroke(strokeSize))
                    }
                }
                
                ComponentType.DIP_SWITCH -> {
                    drawRect(Color(0xFFC62828), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
                    for(i in 0..3) {
                        val yOffset = padding*1.5f + i*(cellSize - padding*3)/3
                        drawRect(Color(0xFF1C1B1F), topLeft = Offset(padding*1.5f, yOffset), size = Size(padding*3f, strokeSize*1.5f))
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
                    drawArc(color, 90f, 180f, false, topLeft = Offset(cx - padding*0.5f, padding-padding*0.5f), size = Size(padding, padding), style = Stroke(strokeSize*0.5f))
                    for (i in 1..4) {
                       val yOff = padding + i*(cellSize-padding*2)/5
                       drawLine(Color(0xFF9E9E9E), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 4f)
                       drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 4f)
                    }
                    drawCircle(Color(0xFF00FFCC), radius = strokeSize, center = Offset(cx, cy))
                }

                ComponentType.MICROCONTROLLER -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    
                    for (i in 1..3) {
                        drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i*padding*0.5f, 0f), end = Offset(cx - padding + i*padding*0.5f, padding), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i*padding*0.5f, cellSize), end = Offset(cx - padding + i*padding*0.5f, cellSize - padding), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(0f, cy - padding + i*padding*0.5f), end = Offset(padding, cy - padding + i*padding*0.5f), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, cy - padding + i*padding*0.5f), end = Offset(cellSize - padding, cy - padding + i*padding*0.5f), strokeWidth = 4f)
                    }
                    
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
                    
                    for(i in -1..1) {
                        drawRect(Color(0xFF0F1E14), topLeft = Offset(cx + i*(padding*0.8f) - padding*0.3f, cy - padding*0.8f), size = Size(padding*0.6f, padding*1.6f))
                    }
                    
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
                    drawRect(Color(0xFF666666), topLeft = Offset(cx - padding*0.5f, cellSize - padding*1.5f), size = Size(padding, padding*1.5f))
                    
                    if (component.isPowered) {
                        val txt = component.extraData.substringAfter("display=").substringBefore("|").ifEmpty { "PC_OK" }
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
                        return@apply
                    }
                    
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    
                    for (i in 1..2) {
                        val yOff = cy - padding + i*padding
                        drawLine(Color(0xFF9E9E9E), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 4f)
                    }
                    
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
                    
                    val hash = component.type.name.hashCode()
                    val r = (hash and 0xFF).toFloat() / 255f
                    val g = ((hash shr 8) and 0xFF).toFloat() / 255f
                    val b = ((hash shr 16) and 0xFF).toFloat() / 255f
                    val dotColor = Color(r, g, b, 1f)
                    drawCircle(dotColor, radius = strokeSize * 0.8f, center = Offset(cx, cy - strokeSize * 2))
                }
            }
        }
    }
}
