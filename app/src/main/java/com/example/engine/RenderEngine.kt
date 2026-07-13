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
        val default = when(component.type) { 
            ComponentType.COIN_CELL -> com.example.functional.Battery.DEFAULT_COIN_CELL_CAPACITY 
            ComponentType.BATTERY_PACK -> com.example.functional.Battery.DEFAULT_BATTERY_PACK_CAPACITY 
            ComponentType.INFINITE_BATTERY -> 9999999f 
            ComponentType.NUCLEAR_REACTOR -> 1000000f 
            else -> com.example.functional.Battery.DEFAULT_BATTERY_CAPACITY 
        }
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

                    if (up) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, 0f), strokeWidth = wStroke)
                    if (right) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cellSize, cy), strokeWidth = wStroke)
                    if (down) drawLine(wireColor, start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = wStroke)
                    if (left) drawLine(wireColor, start = Offset(cx, cy), end = Offset(0f, cy), strokeWidth = wStroke)
                    
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
                
                ComponentType.NUCLEAR_REACTOR, ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, ComponentType.THERMOELECTRIC_GENERATOR, ComponentType.INFINITE_BATTERY -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFCCFF00), Color(0xFF00AA00))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawCircle(Color.Black, radius = Math.max(1f, strokeSize), center = Offset(cx, cy))
                }
                
                ComponentType.WIND_TURBINE -> {
                    // Draw base mast support stand
                    drawLine(
                        color = Color(0xFF90A4AE),
                        start = Offset(cx, cellSize - padding * 1.5f),
                        end = Offset(cx, cy),
                        strokeWidth = strokeSize * 1.5f
                    )
                    // Draw nacelle generator block
                    drawCircle(
                        color = Color(0xFF607D8B),
                        radius = cellSize * 0.15f,
                        center = Offset(cx, cy)
                    )
                    
                    // Rotor blades calculation: if powered, spin the blades!
                    val angleSpeedRad = if (component.isPowered) {
                        // Calculate a dynamic angle that rotates over time
                        val timeSecs = System.currentTimeMillis() / 1000f
                        timeSecs * 6f
                    } else {
                        0f
                    }

                    val bladeLen = cellSize * 0.35f
                    for (i in 0..2) {
                        val angleRad = angleSpeedRad + (i * 120f * (Math.PI / 180f)).toFloat()
                        val endX = cx + kotlin.math.cos(angleRad) * bladeLen
                        val endY = cy + kotlin.math.sin(angleRad) * bladeLen
                        drawLine(
                            color = Color.White,
                            start = Offset(cx, cy),
                            end = Offset(endX, endY),
                            strokeWidth = strokeSize * 1.2f
                        )
                    }
                    
                    // Central rotor hub
                    drawCircle(
                        color = Color(0xFFECEFF1),
                        radius = cellSize * 0.08f,
                        center = Offset(cx, cy)
                    )
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
                ComponentType.LOGIC_NAND -> {
                    val path = Path().apply {
                        moveTo(cx - padding*1.5f, cy - padding*1.2f)
                        lineTo(cx, cy - padding*1.2f)
                        arcTo(androidx.compose.ui.geometry.Rect(cx - padding*1.2f, cy - padding*1.2f, cx + padding*1.2f, cy + padding*1.2f), -90f, 180f, false)
                        lineTo(cx - padding*1.5f, cy + padding*1.2f)
                        close()
                    }
                    drawPath(path, color, style = Stroke(strokeSize))
                    drawCircle(Color(0xFF2B2930), radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*0.7f, cy))
                    drawCircle(color, radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*0.7f, cy), style = Stroke(strokeSize))
                    
                    drawLine(color, start = Offset(cx - padding*1.5f, cy - padding), end = Offset(0f, cy - padding), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx - padding*1.5f, cy + padding), end = Offset(0f, cy + padding), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx + padding*1.2f + strokeSize*1.4f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
                }

                ComponentType.LOGIC_NOR -> {
                    val path = Path().apply {
                        moveTo(cx - padding*1.5f, cy - padding*1.2f)
                        quadraticTo(cx - padding*0.5f, cy, cx - padding*1.5f, cy + padding*1.2f)
                        quadraticTo(cx + padding*0.2f, cy + padding*1.2f, cx + padding*1.2f, cy)
                        quadraticTo(cx + padding*0.2f, cy - padding*1.2f, cx - padding*1.5f, cy - padding*1.2f)
                    }
                    drawPath(path, color, style = Stroke(strokeSize))
                    drawCircle(Color(0xFF2B2930), radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*1.1f, cy))
                    drawCircle(color, radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*1.1f, cy), style = Stroke(strokeSize))
                    
                    drawLine(color, start = Offset(cx - padding*1.1f, cy - padding*0.5f), end = Offset(0f, cy - padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx - padding*1.1f, cy + padding*0.5f), end = Offset(0f, cy + padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx + padding*1.2f + strokeSize*1.8f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
                }

                ComponentType.LOGIC_XOR -> {
                    val path1 = Path().apply {
                        moveTo(cx - padding*1.7f, cy - padding*1.2f)
                        quadraticTo(cx - padding*0.7f, cy, cx - padding*1.7f, cy + padding*1.2f)
                    }
                    drawPath(path1, color, style = Stroke(strokeSize))
                    val path = Path().apply {
                        moveTo(cx - padding*1.3f, cy - padding*1.2f)
                        quadraticTo(cx - padding*0.3f, cy, cx - padding*1.3f, cy + padding*1.2f)
                        quadraticTo(cx + padding*0.2f, cy + padding*1.2f, cx + padding*1.2f, cy)
                        quadraticTo(cx + padding*0.2f, cy - padding*1.2f, cx - padding*1.3f, cy - padding*1.2f)
                    }
                    drawPath(path, color, style = Stroke(strokeSize))
                    
                    drawLine(color, start = Offset(cx - padding*0.9f, cy - padding*0.5f), end = Offset(0f, cy - padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx - padding*0.9f, cy + padding*0.5f), end = Offset(0f, cy + padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx + padding*1.2f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
                }

                ComponentType.LOGIC_XNOR -> {
                    val path1 = Path().apply {
                        moveTo(cx - padding*1.7f, cy - padding*1.2f)
                        quadraticTo(cx - padding*0.7f, cy, cx - padding*1.7f, cy + padding*1.2f)
                    }
                    drawPath(path1, color, style = Stroke(strokeSize))
                    val path = Path().apply {
                        moveTo(cx - padding*1.3f, cy - padding*1.2f)
                        quadraticTo(cx - padding*0.3f, cy, cx - padding*1.3f, cy + padding*1.2f)
                        quadraticTo(cx + padding*0.2f, cy + padding*1.2f, cx + padding*1.2f, cy)
                        quadraticTo(cx + padding*0.2f, cy - padding*1.2f, cx - padding*1.3f, cy - padding*1.2f)
                    }
                    drawPath(path, color, style = Stroke(strokeSize))
                    drawCircle(Color(0xFF2B2930), radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*1.1f, cy))
                    drawCircle(color, radius = strokeSize*0.7f, center = Offset(cx + padding*1.2f + strokeSize*1.1f, cy), style = Stroke(strokeSize))
                    
                    drawLine(color, start = Offset(cx - padding*0.9f, cy - padding*0.5f), end = Offset(0f, cy - padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx - padding*0.9f, cy + padding*0.5f), end = Offset(0f, cy + padding*0.5f), strokeWidth = strokeSize*0.5f)
                    drawLine(color, start = Offset(cx + padding*1.2f + strokeSize*1.8f, cy), end = Offset(cellSize, cy), strokeWidth = strokeSize*0.5f)
                }

                ComponentType.D_FLIP_FLOP, ComponentType.T_FLIP_FLOP, ComponentType.JK_FLIP_FLOP, ComponentType.LATCH_SR -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    
                    val stateCol = if (component.logicState) Color(0xFF00FFCC) else Color(0xFF333333)
                    drawCircle(stateCol, radius = strokeSize*1.2f, center = Offset(cx, cy))
                    
                    when(component.type) {
                        ComponentType.D_FLIP_FLOP -> {
                            val dPath = Path().apply {
                                moveTo(cx - padding*0.8f, cy - padding*0.6f)
                                lineTo(cx - padding*0.2f, cy - padding*0.6f)
                                arcTo(androidx.compose.ui.geometry.Rect(cx - padding*0.5f, cy - padding*0.6f, cx + padding*0.3f, cy + padding*0.6f), -90f, 180f, false)
                                lineTo(cx - padding*0.8f, cy + padding*0.6f)
                                close()
                            }
                            drawPath(dPath, color, style = Stroke(3f))
                        }
                        ComponentType.T_FLIP_FLOP -> {
                            drawLine(color, start = Offset(cx - padding*0.6f, cy - padding*0.6f), end = Offset(cx + padding*0.6f, cy - padding*0.6f), strokeWidth = 4f)
                            drawLine(color, start = Offset(cx, cy - padding*0.6f), end = Offset(cx, cy + padding*0.6f), strokeWidth = 4f)
                        }
                        ComponentType.JK_FLIP_FLOP -> {
                            drawLine(color, start = Offset(cx - padding*0.7f, cy - padding*0.7f), end = Offset(cx - padding*0.3f, cy - padding*0.7f), strokeWidth = 3f)
                            drawLine(color, start = Offset(cx - padding*0.5f, cy - padding*0.7f), end = Offset(cx - padding*0.5f, cy + padding*0.5f), strokeWidth = 3f)
                            drawArc(color, 0f, 180f, false, topLeft = Offset(cx - padding*0.8f, cy + padding*0.1f), size = Size(padding*0.5f, padding*0.5f), style = Stroke(3f))
                        }
                        ComponentType.LATCH_SR -> {
                            drawLine(color, start = Offset(cx - padding*0.7f, cy - padding*0.6f), end = Offset(cx - padding*0.4f, cy), strokeWidth = 3f)
                            drawLine(color, start = Offset(cx + padding*0.4f, cy), end = Offset(cx + padding*0.7f, cy + padding*0.6f), strokeWidth = 3f)
                        }
                        else -> {}
                    }
                }

                ComponentType.IC_7400_NAND, ComponentType.IC_7402_NOR, ComponentType.IC_7404_NOT, ComponentType.IC_7408_AND, 
                ComponentType.IC_7432_OR, ComponentType.IC_7486_XOR, ComponentType.IC_7447_DECODER, ComponentType.IC_CD4017_DECADE -> {
                    val isDecoder = component.type == ComponentType.IC_7447_DECODER
                    val isDecade = component.type == ComponentType.IC_CD4017_DECADE
                    
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF202026), Color(0xFF101014))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize * 0.7f))
                    
                    drawArc(color, 0f, 180f, false, topLeft = Offset(cx - padding*0.6f, padding - padding*0.2f), size = Size(padding*1.2f, padding*0.6f), style = Stroke(strokeSize * 0.5f))
                    
                    for (i in 0..3) {
                        val yOff = padding + i * (cellSize - padding*2) / 3
                        drawLine(Color(0xFFD3A82C), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 3f)
                        drawLine(Color(0xFFD3A82C), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 3f)
                    }
                    
                    if (isDecoder) {
                        val segCol = if (component.isPowered) Color(0xFFFF2255) else Color(0xFF4A0E17)
                        val segW = padding * 1.1f
                        val segH = 2f
                        drawRect(segCol, topLeft = Offset(cx - segW/2, cy - padding*0.7f), size = Size(segW, segH))
                        drawRect(segCol, topLeft = Offset(cx - segW/2, cy), size = Size(segW, segH))
                        drawRect(segCol, topLeft = Offset(cx - segW/2, cy + padding*0.7f), size = Size(segW, segH))
                        drawRect(segCol, topLeft = Offset(cx - segW/2 - segH, cy - padding*0.7f), size = Size(segH, padding*0.7f))
                        drawRect(segCol, topLeft = Offset(cx + segW/2, cy - padding*0.7f), size = Size(segH, padding*0.7f))
                        drawRect(segCol, topLeft = Offset(cx - segW/2 - segH, cy), size = Size(segH, padding*0.7f))
                        drawRect(segCol, topLeft = Offset(cx + segW/2, cy), size = Size(segH, padding*0.7f))
                    } else if (isDecade) {
                        val activeDot = ((System.currentTimeMillis() / 250) % 10).toInt()
                        for (d in 0..9) {
                            val angle = d * 36f * (Math.PI / 180f)
                            val rx = cx + (padding * 1.1f) * Math.cos(angle).toFloat()
                            val ry = cy + (padding * 1.1f) * Math.sin(angle).toFloat()
                            val dotCol = if (component.isPowered && d == activeDot) Color(0xFF00FFCC) else Color(0x3300FFCC)
                            drawCircle(dotCol, radius = 3f, center = Offset(rx, ry))
                        }
                    } else {
                        val gateCol = when(component.type) {
                            ComponentType.IC_7400_NAND -> Color(0xFF00FFCC)
                            ComponentType.IC_7402_NOR -> Color(0xFFFF2255)
                            ComponentType.IC_7404_NOT -> Color(0xFF22C55E)
                            ComponentType.IC_7408_AND -> Color(0xFF3B82F6)
                            ComponentType.IC_7432_OR -> Color(0xFFF97316)
                            ComponentType.IC_7486_XOR -> Color(0xFFA855F7)
                            else -> Color(0xFFD3A82C)
                        }
                        
                        drawCircle(gateCol.copy(alpha = 0.25f), radius = padding * 1.2f, center = Offset(cx, cy))
                        drawCircle(gateCol, radius = strokeSize * 0.7f, center = Offset(cx, cy))
                        drawLine(gateCol.copy(alpha = 0.5f), start = Offset(cx - padding, cy - padding), end = Offset(cx, cy), strokeWidth = 2f)
                        drawLine(gateCol.copy(alpha = 0.5f), start = Offset(cx - padding, cy + padding), end = Offset(cx, cy), strokeWidth = 2f)
                        drawLine(gateCol.copy(alpha = 0.5f), start = Offset(cx, cy), end = Offset(cx + padding, cy), strokeWidth = 2f)
                    }
                }

                ComponentType.IC_LM358_OPAMP, ComponentType.IC_LM324_OPAMP -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A))),
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize * 0.7f))
                    
                    drawArc(color, 0f, 180f, false, topLeft = Offset(cx - padding*0.5f, padding - padding*0.2f), size = Size(padding, padding*0.4f), style = Stroke(strokeSize * 0.5f))
                    
                    val triCol = if (component.isPowered) Color(0xFFA855F7) else Color(0xFF581C87)
                    val oPath = Path().apply {
                        moveTo(cx - padding*1.1f, cy - padding*0.8f)
                        lineTo(cx - padding*0.1f, cy - padding*0.4f)
                        lineTo(cx - padding*1.1f, cy)
                        close()
                    }
                    drawPath(oPath, triCol, style = Stroke(2f))
                    if (component.type == ComponentType.IC_LM324_OPAMP) {
                        val oPath2 = Path().apply {
                            moveTo(cx + padding*0.1f, cy + padding*0.1f)
                            lineTo(cx + padding*1.1f, cy + padding*0.5f)
                            lineTo(cx + padding*0.1f, cy + padding*0.9f)
                            close()
                        }
                        drawPath(oPath2, triCol, style = Stroke(2f))
                    }
                }

                ComponentType.IC_LM317_REG -> {
                    drawRect(Color(0xFF292830), topLeft = Offset(padding*1.2f, cy - padding*0.5f), size = Size(cellSize - padding*2.4f, cellSize/2))
                    drawRect(color, topLeft = Offset(padding*1.2f, cy - padding*0.5f), size = Size(cellSize - padding*2.4f, cellSize/2), style = Stroke(strokeSize*0.5f))
                    
                    drawRect(Color(0xFFB0BEC5), topLeft = Offset(padding*1.5f, padding), size = Size(cellSize - padding*3f, cy - padding*0.5f))
                    drawCircle(Color(0xFF37474F), radius = strokeSize * 0.8f, center = Offset(cx, padding + (cy - padding*1.5f)/2))
                    
                    val pinCol = Color(0xFFECEFF1)
                    drawLine(pinCol, start = Offset(cx - padding, cy), end = Offset(cx - padding, cellSize), strokeWidth = 3f)
                    drawLine(pinCol, start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 3f)
                    drawLine(pinCol, start = Offset(cx + padding, cy), end = Offset(cx + padding, cellSize), strokeWidth = 3f)
                }

                ComponentType.IC_L298N_MOTOR -> {
                    drawRect(Color(0xFF15151A), topLeft = Offset(padding, padding*2), size = Size(cellSize - padding*2, cellSize - padding*3))
                    
                    val heatsinkCol = Color(0xFFDC2626)
                    drawRect(heatsinkCol, topLeft = Offset(padding*1.4f, padding*1.1f), size = Size(cellSize - padding*2.8f, padding*1.7f))
                    
                    for (i in 0..4) {
                        val finX = padding*1.8f + i * (cellSize - padding*3.6f)/4
                        drawRect(Color(0xFFB91C1C), topLeft = Offset(finX - 2f, padding*0.5f), size = Size(4f, padding*0.6f))
                    }
                    
                    for (i in 0..5) {
                        val pX = padding*1.2f + i * (cellSize - padding*2.4f)/5
                        drawLine(Color(0xFFFFD54F), start = Offset(pX, cellSize - padding*1.2f), end = Offset(pX, cellSize), strokeWidth = 2.5f)
                    }
                }

                ComponentType.IC_ULN2003 -> {
                    drawRect(Color(0xFF1E1C24), topLeft = Offset(padding, padding*1.2f), size = Size(cellSize - padding*2, cellSize - padding*2.4f))
                    drawRect(color, topLeft = Offset(padding, padding*1.2f), size = Size(cellSize - padding*2, cellSize - padding*2.4f), style = Stroke(strokeSize * 0.7f))
                    
                    val darCol = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFF4A3E56)
                    for (i in 0..4) {
                        val dy = padding*1.8f + i * (cellSize - padding*3.6f)/4
                        drawLine(darCol, start = Offset(padding*1.8f, dy), end = Offset(cellSize - padding*1.8f, dy), strokeWidth = 2.5f)
                        drawLine(darCol, start = Offset(cellSize - padding*1.8f, dy), end = Offset(cellSize - padding*2.3f, dy - padding*0.3f), strokeWidth = 2f)
                    }
                    for(i in 0..4) {
                        val py = padding*1.6f + i * (cellSize - padding*3.2f)/4
                        drawLine(Color(0xFFB0BEC5), start = Offset(0f, py), end = Offset(padding, py), strokeWidth = 2f)
                        drawLine(Color(0xFFB0BEC5), start = Offset(cellSize, py), end = Offset(cellSize - padding, py), strokeWidth = 2f)
                    }
                }

                ComponentType.MULTIPLEXER -> {
                    val path = Path().apply {
                        moveTo(cx - padding*1.5f, cy - padding*1.6f)
                        lineTo(cx + padding*1.5f, cy - padding*0.9f)
                        lineTo(cx + padding*1.5f, cy + padding*0.9f)
                        lineTo(cx - padding*1.5f, cy + padding*1.6f)
                        close()
                    }
                    drawPath(path, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF7F3DEC), Color(0xFF3F1D7C))))
                    drawPath(path, color, style = Stroke(strokeSize))
                    drawLine(color, start = Offset(cx - padding*0.5f, cy - padding*0.4f), end = Offset(cx + padding*0.5f, cy), strokeWidth = 3f)
                }

                ComponentType.DEMULTIPLEXER -> {
                    val path = Path().apply {
                        moveTo(cx - padding*1.5f, cy - padding*0.9f)
                        lineTo(cx + padding*1.5f, cy - padding*1.6f)
                        lineTo(cx + padding*1.5f, cy + padding*1.6f)
                        lineTo(cx - padding*1.5f, cy + padding*0.9f)
                        close()
                    }
                    drawPath(path, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF7F3DEC), Color(0xFF3F1D7C))))
                    drawPath(path, color, style = Stroke(strokeSize))
                    drawLine(color, start = Offset(cx - padding*0.5f, cy), end = Offset(cx + padding*0.5f, cy - padding*0.4f), strokeWidth = 3f)
                }

                ComponentType.SHIFT_REGISTER -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF005577), Color(0xFF002233))), 
                        topLeft = Offset(padding, padding*1.5f), 
                        size = Size(cellSize - padding*2, cellSize - padding*3)
                    )
                    drawRect(color, topLeft = Offset(padding, padding*1.5f), size = Size(cellSize - padding*2, cellSize - padding*3), style = Stroke(strokeSize))
                    for (i in 1..3) {
                        val dx = padding + i * (cellSize - padding*2) / 4
                        drawLine(color, start = Offset(dx, padding*1.5f), end = Offset(dx, cellSize - padding*1.5f), strokeWidth = strokeSize*0.5f)
                        drawCircle(if (component.logicState) Color(0xFF00FFCC) else Color(0xFF005577), radius = strokeSize*0.6f, center = Offset(dx - (cellSize - padding*2)/8, cy))
                    }
                    drawCircle(if (component.logicState) Color(0xFF00FFCC) else Color(0xFF005577), radius = strokeSize*0.6f, center = Offset(cellSize - padding - (cellSize - padding*2)/8, cy))
                }

                ComponentType.HALF_ADDER, ComponentType.FULL_ADDER -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF7F3DEC), Color(0xFF3F1D7C))), 
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    val addCol = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFD0BCFF)
                    drawLine(addCol, start = Offset(cx - padding*1.1f, cy), end = Offset(cx + padding*1.1f, cy), strokeWidth = strokeSize*1.2f)
                    drawLine(addCol, start = Offset(cx, cy - padding*1.1f), end = Offset(cx, cy + padding*1.1f), strokeWidth = strokeSize*1.2f)
                    if (component.type == ComponentType.FULL_ADDER) {
                        drawCircle(addCol, radius = strokeSize*0.6f, center = Offset(cx + padding*1.2f, cy + padding*1.2f))
                    }
                }

                ComponentType.ADC, ComponentType.DAC -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF172554))), 
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    if (component.type == ComponentType.ADC) {
                        val stairColor = if (component.isPowered) Color(0xFF38BDF8) else Color(0xFF1E40AF)
                        val path = Path().apply {
                            moveTo(cx - padding, cy + padding)
                            lineTo(cx - padding*0.5f, cy + padding)
                            lineTo(cx - padding*0.5f, cy)
                            lineTo(cx, cy)
                            lineTo(cx, cy - padding*0.5f)
                            lineTo(cx + padding*0.5f, cy - padding*0.5f)
                            lineTo(cx + padding*0.5f, cy - padding)
                            lineTo(cx + padding, cy - padding)
                        }
                        drawPath(path, stairColor, style = Stroke(strokeSize*0.6f))
                    } else {
                        val cvColor = if (component.isPowered) Color(0xFFF43F5E) else Color(0xFF9F1239)
                        val path = Path().apply {
                            moveTo(cx - padding, cy + padding)
                            quadraticTo(cx, cy, cx + padding, cy - padding)
                        }
                        drawPath(path, cvColor, style = Stroke(strokeSize*0.6f))
                    }
                }

                ComponentType.COMPARATOR, ComponentType.VOLTAGE_REGULATOR, ComponentType.AMPLIFIER, ComponentType.BUFFER -> {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF120024))), 
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding*2, cellSize - padding*2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize*0.7f))
                    val symCol = if (component.isPowered) Color(0xFFE9D5FF) else Color(0xFF6B21A8)
                    when(component.type) {
                        ComponentType.COMPARATOR -> {
                            drawCircle(symCol, radius = strokeSize*1.5f, center = Offset(cx, cy), style = Stroke(3f))
                            drawLine(symCol, start = Offset(cx - strokeSize*0.7f, cy), end = Offset(cx + strokeSize*0.7f, cy), strokeWidth = 3f)
                            drawLine(symCol, start = Offset(cx, cy - strokeSize*0.7f), end = Offset(cx, cy + strokeSize*0.7f), strokeWidth = 3f)
                        }
                        ComponentType.VOLTAGE_REGULATOR -> {
                            drawLine(symCol, start = Offset(cx - padding, cy - strokeSize*0.5f), end = Offset(cx + padding, cy - strokeSize*0.5f), strokeWidth = 4f)
                            drawLine(symCol, start = Offset(cx - padding*0.5f, cy + strokeSize*0.5f), end = Offset(cx + padding*0.5f, cy + strokeSize*0.5f), strokeWidth = 3f)
                        }
                        ComponentType.AMPLIFIER, ComponentType.BUFFER -> {
                            val ampPath = Path().apply {
                                moveTo(cx - padding*0.8f, cy - padding*0.8f)
                                lineTo(cx + padding*0.8f, cy)
                                lineTo(cx - padding*0.8f, cy + padding*0.8f)
                                close()
                            }
                            drawPath(ampPath, symCol, style = Stroke(3f))
                            if (component.type == ComponentType.BUFFER) {
                                drawCircle(symCol, radius = 3f, center = Offset(cx + padding*0.8f + 4f, cy))
                            }
                        }
                        else -> {}
                    }
                }

                ComponentType.KEYPAD_4X4 -> {
                    drawRect(Color(0xFF212121), topLeft = Offset(padding*0.5f, padding*0.5f), size = Size(cellSize - padding, cellSize - padding))
                    drawRect(color, topLeft = Offset(padding*0.5f, padding*0.5f), size = Size(cellSize - padding, cellSize - padding), style = Stroke(strokeSize*0.4f))
                    for (row in 0..3) {
                        for (col in 0..3) {
                            val bx = padding * 1.1f + col * (cellSize - padding * 2.2f) / 3
                            val by = padding * 1.1f + row * (cellSize - padding * 2.2f) / 3
                            val btnActive = component.logicState && (row == 1 && col == 1)
                            val btnColor = if (btnActive) Color(0xFFFFEB3B) else Color(0xFF616161)
                            drawRect(btnColor, topLeft = Offset(bx - 3f, by - 3f), size = Size(6f, 6f))
                            drawRect(Color.Black, topLeft = Offset(bx - 4f, by - 4f), size = Size(8f, 8f), style = Stroke(1.5f))
                        }
                    }
                }

                ComponentType.JOYSTICK -> {
                    drawCircle(Color(0xFF303030), radius = cellSize*0.45f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize*0.45f, center = Offset(cx, cy), style = Stroke(strokeSize*0.5f))
                    drawLine(color.copy(alpha = 0.4f), start = Offset(cx - cellSize*0.4f, cy), end = Offset(cx + cellSize*0.4f, cy), strokeWidth = 1.5f)
                    drawLine(color.copy(alpha = 0.4f), start = Offset(cx, cy - cellSize*0.4f), end = Offset(cx, cy + cellSize*0.4f), strokeWidth = 1.5f)
                    drawRect(Color(0xFF00E5FF), topLeft = Offset(cx - 3f, padding), size = Size(6f, 6f))
                    drawRect(Color(0xFF00E5FF), topLeft = Offset(padding, cy - 3f), size = Size(6f, 6f))
                    
                    val stateX = if(component.isPowered) cx + padding*0.4f else cx
                    val stateY = if(component.isPowered) cy - padding*0.4f else cy
                    drawCircle(Color.Black, radius = cellSize*0.25f, center = Offset(stateX, stateY))
                    drawCircle(Color(0xFFE0E0E0), radius = cellSize*0.25f, center = Offset(stateX, stateY), style = Stroke(strokeSize*0.3f))
                    drawCircle(Color(0xFF888888), radius = strokeSize * 0.8f, center = Offset(stateX, stateY))
                }

                ComponentType.ENCODER -> {
                    drawCircle(Color(0xFF424242), radius = cellSize*0.42f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize*0.42f, center = Offset(cx, cy), style = Stroke(strokeSize*0.6f))
                    for (i in 0..11) {
                        val angle = i * 30f * (Math.PI / 180f)
                        val sin = Math.sin(angle).toFloat()
                        val cos = Math.cos(angle).toFloat()
                        drawLine(Color.Black, start = Offset(cx + (cellSize*0.33f)*cos, cy + (cellSize*0.33f)*sin), end = Offset(cx + (cellSize*0.42f)*cos, cy + (cellSize*0.42f)*sin), strokeWidth = 2f)
                    }
                    val slotAngle = if (component.logicState) 135f else 45f
                    val sRad = slotAngle * (Math.PI / 180f)
                    drawLine(Color.White, start = Offset(cx, cy), end = Offset(cx + (cellSize*0.33f)*Math.cos(sRad).toFloat(), cy + (cellSize*0.33f)*Math.sin(sRad).toFloat()), strokeWidth = 4f)
                }

                ComponentType.POTENTIOMETER -> {
                    drawCircle(Color(0xFF1976D2), radius = cellSize*0.4f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize*0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
                    drawCircle(Color(0xFFB0BEC5), radius = cellSize*0.2f, center = Offset(cx, cy))
                    val angleScrew = if (component.logicState) 45f else -45f
                    val sRad = angleScrew * (Math.PI / 180f)
                    drawLine(Color(0xFF37474F), start = Offset(cx - (cellSize*0.15f)*Math.cos(sRad).toFloat(), cy - (cellSize*0.15f)*Math.sin(sRad).toFloat()), end = Offset(cx + (cellSize*0.15f)*Math.cos(sRad).toFloat(), cy + (cellSize*0.15f)*Math.sin(sRad).toFloat()), strokeWidth = 4f)
                }

                ComponentType.DISPLAY_7SEG_4DIGIT -> {
                    drawRect(Color(0xFF121212), topLeft = Offset(padding*0.4f, padding*1.2f), size = Size(cellSize - padding*0.8f, cellSize - padding*2.4f))
                    drawRect(color, topLeft = Offset(padding*0.4f, padding*1.2f), size = Size(cellSize - padding*0.8f, cellSize - padding*2.4f), style = Stroke(strokeSize*0.4f))
                    val lit = if (component.isPowered) Color(0xFFFF1744) else Color(0xFF3E000C)
                    for (d in 0..3) {
                        val dx = padding * 0.8f + d * (cellSize - padding * 2f) / 3
                        drawRect(lit, topLeft = Offset(dx, cy - padding*0.6f), size = Size(8f, 2f))
                        drawRect(lit, topLeft = Offset(dx, cy), size = Size(8f, 2f))
                        drawRect(lit, topLeft = Offset(dx, cy + padding*0.6f), size = Size(8f, 2f))
                        drawRect(lit, topLeft = Offset(dx - 1f, cy - padding*0.6f), size = Size(2f, padding*0.6f))
                        drawRect(lit, topLeft = Offset(dx + 7f, cy - padding*0.6f), size = Size(2f, padding*0.6f))
                        drawRect(lit, topLeft = Offset(dx - 1f, cy), size = Size(2f, padding*0.6f))
                        drawRect(lit, topLeft = Offset(dx + 7f, cy), size = Size(2f, padding*0.6f))
                    }
                }

                ComponentType.DISPLAY_OLED_128X64 -> {
                    drawRect(Color(0xFF0D0E15), topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f))
                    drawRect(color, topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f), style = Stroke(strokeSize*0.5f))
                    if (component.isPowered) {
                        drawRect(Color(0xFFFFEA00), topLeft = Offset(padding*1.1f, padding * 1.2f), size = Size(cellSize - padding*2.2f, padding*0.4f))
                        drawRect(Color(0xFF00E5FF), topLeft = Offset(padding*1.1f, padding * 1.8f), size = Size(cellSize - padding*2.2f, cellSize - padding*3f))
                        drawLine(Color(0xFF0D0E15), start = Offset(cx - padding, cy + 3f), end = Offset(cx + padding, cy - 3f), strokeWidth = 2f)
                    }
                }

                ComponentType.DISPLAY_TFT_24 -> {
                    drawRect(Color.Black, topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f))
                    drawRect(color, topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f), style = Stroke(strokeSize*0.5f))
                    if (component.isPowered) {
                        val barW = (cellSize - padding * 1.4f) / 6
                        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
                        for (i in 0..5) {
                            drawRect(colors[i], topLeft = Offset(padding * 0.7f + i * barW, padding * 1.1f), size = Size(barW, cellSize - padding * 2.2f))
                        }
                    }
                }

                ComponentType.E_PAPER_DISPLAY -> {
                    drawRect(Color(0xFFECEFF1), topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f))
                    drawRect(Color(0xFF37474F), topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f), style = Stroke(strokeSize*0.5f))
                    val eInkCol = Color(0xFF263238)
                    drawRect(eInkCol, topLeft = Offset(padding*1.5f, padding*1.5f), size = Size(cellSize - padding*3f, strokeSize*0.8f))
                    drawRect(eInkCol, topLeft = Offset(padding*1.5f, cy), size = Size(cellSize - padding*3f, strokeSize*0.8f))
                    drawLine(eInkCol, start = Offset(cx, cy - padding), end = Offset(cx, cy + padding), strokeWidth = 3f)
                }

                ComponentType.SOLENOID -> {
                    val shaftCol = if (component.isPowered) Color(0xFFCFD8DC) else Color(0xFF78909C)
                    val extendShift = if (component.isPowered) padding*1.1f else 0f
                    drawRect(shaftCol, topLeft = Offset(cx - strokeSize*0.8f, padding + extendShift), size = Size(strokeSize*1.6f, cellSize - padding*2))
                    
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFE64A19), Color(0xFFFFCC80))), 
                        topLeft = Offset(cx - padding*1.4f, padding*2), 
                        size = Size(padding*2.8f, cellSize - padding*4)
                    )
                    drawRect(Color(0xFF5D4037), topLeft = Offset(cx - padding*1.4f, padding*2), size = Size(padding*2.8f, cellSize - padding*4), style = Stroke(strokeSize*0.4f))
                    
                    for (i in 0..4) {
                        val yLine = padding*2.4f + i * (cellSize - padding*4.8f)/4
                        drawLine(Color(0xFFD84315), start = Offset(cx - padding*1.4f, yLine), end = Offset(cx + padding*1.4f, yLine), strokeWidth = 2.5f)
                    }
                }

                ComponentType.LINEAR_ACTUATOR -> {
                    drawRect(Color(0xFF455A64), topLeft = Offset(cx - padding, padding), size = Size(padding*2, cellSize/2))
                    drawRect(color, topLeft = Offset(cx - padding, padding), size = Size(padding*2, cellSize/2), style = Stroke(strokeSize*0.5f))
                    
                    val extCol = if(component.isPowered) Color(0xFFECEFF1) else Color(0xFF78909C)
                    val extLen = if(component.isPowered) cellSize/2 else cellSize/3
                    drawRect(extCol, topLeft = Offset(cx - strokeSize*0.6f, cy), size = Size(strokeSize*1.2f, extLen))
                    
                    for (i in 0..3) {
                        val gy = cy + i * extLen / 4
                        drawLine(Color(0xFF37474F), start = Offset(cx - strokeSize*0.6f, gy), end = Offset(cx + strokeSize*0.6f, gy), strokeWidth = 1.5f)
                    }
                }

                ComponentType.RELAY_MODULE_4CH -> {
                    drawRect(Color(0xFF121212), topLeft = Offset(padding*0.5f, padding*0.8f), size = Size(cellSize - padding, cellSize - padding*1.6f))
                    val activeRelay = if (component.isPowered) 1 else -1
                    for(i in 0..3) {
                        val rx = padding*0.8f + i * (cellSize - padding*1.6f)/4
                        val rW = (cellSize - padding*2f)/4
                        val relayColor = if (i == activeRelay) Color(0xFF00B0FF) else Color(0xFF01579B)
                        drawRect(relayColor, topLeft = Offset(rx, padding*1.5f), size = Size(rW, cellSize - padding*3f))
                        drawRect(Color.White.copy(alpha = 0.5f), topLeft = Offset(rx, padding*1.5f), size = Size(rW, cellSize - padding*3f), style = Stroke(1f))
                        val ledColor = if (i == activeRelay) Color(0xFFFF3300) else Color(0x33FF3300)
                        drawCircle(ledColor, radius = 2f, center = Offset(rx + rW/2, padding*1.1f))
                    }
                }

                ComponentType.PELTIER_MODULE -> {
                    drawRect(Color(0xFFFAFAFA), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
                    drawRect(Color(0xFFCFD8DC), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize*0.5f))
                    val pillCol = if(component.isPowered) Color(0xFFFF5722) else Color(0xFF37474F)
                    for (i in 0..2) {
                        for(j in 0..2) {
                            drawRect(pillCol, topLeft = Offset(padding*1.8f + i*padding*1.6f, padding*1.8f + j*padding*1.6f), size = Size(4f, 4f))
                        }
                    }
                    drawLine(Color(0xFFD32F2F), start = Offset(padding, cellSize - padding*1.5f), end = Offset(0f, cellSize), strokeWidth=2.5f)
                    drawLine(Color.Black, start = Offset(cellSize - padding, cellSize - padding*1.5f), end = Offset(cellSize, cellSize), strokeWidth=2.5f)
                }

                ComponentType.ACCELEROMETER, ComponentType.GYROSCOPE, ComponentType.MAGNETOMETER -> {
                    drawCircle(Color(0xFF263238), radius = cellSize*0.42f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize*0.42f, center = Offset(cx, cy), style = Stroke(strokeSize*0.5f))
                    val arrowCol = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFF546E7A)
                    drawLine(arrowCol, start = Offset(cx - padding*1.2f, cy), end = Offset(cx + padding*1.2f, cy), strokeWidth = 2f)
                    drawLine(arrowCol, start = Offset(cx, cy - padding*1.2f), end = Offset(cx, cy + padding*1.2f), strokeWidth = 2f)
                    drawCircle(arrowCol, radius = 5f, center = Offset(cx, cy))
                }

                ComponentType.CAMERA_MODULE -> {
                    drawRect(Color(0xFF1B5E20), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize*0.5f))
                    drawRect(Color(0xFF212121), topLeft = Offset(cx - padding*1.3f, cy - padding*1.3f), size = Size(padding*2.6f, padding*2.6f))
                    drawCircle(Color(0xFF455A64), radius = strokeSize*2f, center = Offset(cx, cy))
                    drawCircle(Color(0xFF0D47A1), radius = strokeSize*1.1f, center = Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(cx + strokeSize*0.5f, cy - strokeSize*0.5f))
                }

                ComponentType.FINGERPRINT_SCANNER -> {
                    drawRect(Color(0xFF263238), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize))
                    val cyanCol = if (component.isPowered) Color(0xFF00E5FF) else Color(0x4400E5FF)
                    for (i in 1..3) {
                        drawArc(cyanCol, 180f, 180f, false, topLeft = Offset(cx - i*padding*0.5f, cy - i*padding*0.5f), size = Size(i*padding, i*padding), style = Stroke(2.5f))
                        drawArc(cyanCol, 0f, 180f, false, topLeft = Offset(cx - i*padding*0.5f, cy - i*padding*0.3f), size = Size(i*padding, i*padding), style = Stroke(2.5f))
                    }
                }

                ComponentType.MICROPHONE -> {
                    drawCircle(Color(0xFFCFD8DC), radius = cellSize*0.4f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize*0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
                    val grCol = Color(0xFF37474F)
                    for (i in -2..2) {
                        val dx = cx + i * (cellSize*0.12f)
                        drawLine(grCol, start = Offset(dx, cy - cellSize*0.3f), end = Offset(dx, cy + cellSize*0.3f), strokeWidth = 1.5f)
                        drawLine(grCol, start = Offset(cx - cellSize*0.3f, dx), end = Offset(cx + cellSize*0.3f, dx), strokeWidth = 1.5f)
                    }
                }

                else -> {
                    if (component.type.category == ComponentCategory.MATERIALS || component.type.category == ComponentCategory.HYDRAULICS) {
                        val matColor = when(component.type) {
                            ComponentType.WATER, ComponentType.INFINITE_WATER -> Color(0xAA2196F3)
                            ComponentType.LAVA, ComponentType.INFINITE_LAVA -> Color(0xAAFF5722)
                            ComponentType.OIL, ComponentType.INFINITE_OIL -> Color(0xAA212121)
                            ComponentType.ACID, ComponentType.INFINITE_ACID -> Color(0xAA8BC34A)
                            ComponentType.SAND -> Color(0xFFFFC107)
                            ComponentType.DIRT -> Color(0xFF795548)
                            ComponentType.STONE -> Color(0xFF9E9E9E)
                            ComponentType.GLASS -> Color(0x44FFFFFF)
                            ComponentType.WOOD -> Color(0xFF8D6E63)
                            ComponentType.FIRE -> Color(0xFFFF9800)
                            ComponentType.ICE -> Color(0xAA80DEEA)
                            ComponentType.STEAM, ComponentType.INFINITE_STEAM -> Color(0xAAE0E0E0)
                            ComponentType.SLIME, ComponentType.INFINITE_SLIME -> Color(0xAA00FF00)
                            ComponentType.RUBBER -> Color(0xFF424242)
                            ComponentType.DIAMOND -> Color(0xAA00BCD4)
                            ComponentType.COAL -> Color(0xFF3E2723)
                            ComponentType.SPONGE -> Color(0xFFFFEB3B)
                            ComponentType.GASOLINE, ComponentType.INFINITE_GASOLINE -> Color(0xAAFFC107)
                            ComponentType.LIQUID_NITROGEN, ComponentType.INFINITE_LIQUID_NITROGEN -> Color(0xAA4DD0E1)
                            ComponentType.HELIUM -> Color(0xAAFF80DF)
                            ComponentType.HYDROGEN -> Color(0xAA33FFCC)
                            ComponentType.METHANE -> Color(0xAA7FFF00)
                            ComponentType.CARBON_DIOXIDE -> Color(0xAA808099)
                            ComponentType.URANIUM -> {
                                val temp = component.temperature
                                if (temp <= 100f) {
                                    Color(0xAA76FF03)
                                } else if (temp < 600f) {
                                    val r = (((temp - 100f) / 500f) * 255f).toInt().coerceIn(0, 255)
                                    val g = (255 - (((temp - 100f) / 500f) * 150f)).toInt().coerceIn(0, 255)
                                    Color(r, g, 3, 230)
                                } else {
                                    val green = ((((temp - 600f) / 1400f) * 255f) + 105f).toInt().coerceIn(105, 255)
                                    val blue = (((temp - 600f) / 1400f) * 255f).toInt().coerceIn(0, 255)
                                    Color(255, green, blue, 255)
                                }
                            }
                            ComponentType.MAGIC_DUST -> Color(0xAAE040FB)
                            ComponentType.PIPE -> Color(0xFF546E7A)
                            ComponentType.FLUID_DRAIN, ComponentType.VOID_HOLE -> Color(0xFF000000)
                            
                            ComponentType.STEEL -> Color(0xFFB0BEC5)
                            ComponentType.COPPER -> Color(0xFFD84315)
                            ComponentType.GOLD -> Color(0xFFFFD54F)
                            ComponentType.ALUMINUM -> Color(0xFFCFD8DC)
                            ComponentType.PLASTIC -> Color(0xFFFFCC80)
                            ComponentType.CLAY -> Color(0xFFBCAAA4)
                            ComponentType.BRICK -> Color(android.graphics.Color.parseColor(com.example.engine.JavaModEngine.brickColorHex))
                            ComponentType.OBSIDIAN -> Color(0xFF1C1C1C)
                            ComponentType.BEDROCK -> Color(0xFF000000)
                            
                            else -> Color.Transparent
                        }
                        drawRect(matColor, size = Size(cellSize, cellSize))
                        
                        // Render customized textures with fine details per component type
                        when (component.type) {
                            ComponentType.SAND -> {
                                drawCircle(Color(0xFFE5A900), radius = cellSize * 0.04f, center = Offset(cellSize * 0.25f, cellSize * 0.3f))
                                drawCircle(Color(0xFFFFD54F), radius = cellSize * 0.03f, center = Offset(cellSize * 0.75f, cellSize * 0.45f))
                                drawCircle(Color(0xFFD48F00), radius = cellSize * 0.04f, center = Offset(cellSize * 0.45f, cellSize * 0.8f))
                            }
                            ComponentType.DIRT -> {
                                drawRect(Color(0xFF5D4037), topLeft = Offset(cellSize * 0.15f, cellSize * 0.25f), size = Size(cellSize * 0.15f, cellSize * 0.10f))
                                drawRect(Color(0xFF5D4037), topLeft = Offset(cellSize * 0.6f, cellSize * 0.7f), size = Size(cellSize * 0.2f, cellSize * 0.15f))
                                drawCircle(Color(0xFF43281C), radius = cellSize * 0.05f, center = Offset(cellSize * 0.4f, cellSize * 0.5f))
                            }
                            ComponentType.STONE -> {
                                drawLine(Color(0xFF555555), start = Offset(0f, cellSize * 0.2f), end = Offset(cellSize * 0.4f, cellSize * 0.4f), strokeWidth = 2f)
                                drawLine(Color(0xFF555555), start = Offset(cellSize * 0.4f, cellSize * 0.4f), end = Offset(cellSize * 0.3f, cellSize * 0.8f), strokeWidth = 2f)
                                drawLine(Color(0xFF555555), start = Offset(cellSize * 0.4f, cellSize * 0.4f), end = Offset(cellSize * 0.8f, cellSize * 0.3f), strokeWidth = 2f)
                            }
                            ComponentType.GLASS -> {
                                drawLine(Color(0x66FFFFFF), start = Offset(cellSize * 0.2f, 0f), end = Offset(cellSize, cellSize * 0.8f), strokeWidth = cellSize * 0.08f)
                                drawLine(Color(0xAAFFFFFF), start = Offset(0f, cellSize * 0.4f), end = Offset(cellSize * 0.6f, cellSize), strokeWidth = 1.5f)
                            }
                            ComponentType.WOOD -> {
                                drawArc(Color(0xFF5D4037), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(-cellSize * 0.2f, -cellSize * 0.2f), size = Size(cellSize * 1.4f, cellSize * 1.4f), style = Stroke(1.8f))
                                drawArc(Color(0xFF5D4037), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(-cellSize * 0.5f, -cellSize * 0.5f), size = Size(cellSize * 2.0f, cellSize * 2.0f), style = Stroke(1.8f))
                                drawLine(Color(0xFF5D4037), start = Offset(cellSize * 0.8f, 0f), end = Offset(cellSize * 0.9f, cellSize), strokeWidth = 1.2f)
                            }
                            ComponentType.ICE -> {
                                drawLine(Color(0xFFE0F7FA), start = Offset(0f, 0f), end = Offset(cellSize, cellSize), strokeWidth = 1.8f)
                                drawLine(Color(0xFFE0F7FA), start = Offset(cellSize, 0f), end = Offset(0f, cellSize), strokeWidth = 1.2f)
                            }
                            ComponentType.COAL -> {
                                val coalPath = Path().apply {
                                    moveTo(cellSize * 0.1f, cellSize * 0.5f)
                                    lineTo(cellSize * 0.5f, cellSize * 0.1f)
                                    lineTo(cellSize * 0.9f, cellSize * 0.4f)
                                    lineTo(cellSize * 0.8f, cellSize * 0.8f)
                                    lineTo(cellSize * 0.2f, cellSize * 0.8f)
                                    close()
                                }
                                drawPath(coalPath, Color(0xFF212121), style = Stroke(1.8f))
                                drawLine(Color(0xFF212121), start = Offset(cellSize * 0.5f, cellSize * 0.1f), end = Offset(cellSize * 0.5f, cellSize * 0.8f), strokeWidth = 1.2f)
                            }
                            ComponentType.SPONGE -> {
                                drawCircle(Color(0xFFFBC02D), radius = cellSize * 0.06f, center = Offset(cellSize * 0.3f, cellSize * 0.3f))
                                drawCircle(Color(0xFFFBC02D), radius = cellSize * 0.08f, center = Offset(cellSize * 0.7f, cellSize * 0.35f))
                                drawCircle(Color(0xFFFBC02D), radius = cellSize * 0.05f, center = Offset(cellSize * 0.4f, cellSize * 0.75f))
                                drawCircle(Color(0xFFFBC02D), radius = cellSize * 0.07f, center = Offset(cellSize * 0.8f, cellSize * 0.75f))
                            }
                            ComponentType.SLIME, ComponentType.INFINITE_SLIME -> {
                                drawCircle(Color(0x88FFFFFF), radius = cellSize * 0.08f, center = Offset(cellSize * 0.3f, cellSize * 0.4f))
                                drawCircle(Color(0x88FFFFFF), radius = cellSize * 0.05f, center = Offset(cellSize * 0.65f, cellSize * 0.65f))
                            }
                            ComponentType.MAGIC_DUST -> {
                                val scaleStar = 0.5f + 0.5f * kotlin.math.sin((System.currentTimeMillis() % 2000) / 2000f * 2.0 * Math.PI).toFloat()
                                drawLine(Color.White, start = Offset(cellSize * 0.3f - cellSize * 0.1f * scaleStar, cellSize * 0.3f), end = Offset(cellSize * 0.3f + cellSize * 0.1f * scaleStar, cellSize * 0.3f), strokeWidth = 2f)
                                drawLine(Color.White, start = Offset(cellSize * 0.3f, cellSize * 0.3f - cellSize * 0.1f * scaleStar), end = Offset(cellSize * 0.3f, cellSize * 0.3f + cellSize * 0.1f * scaleStar), strokeWidth = 2f)
                                drawLine(Color.White, start = Offset(cellSize * 0.7f - cellSize * 0.08f, cellSize * 0.7f), end = Offset(cellSize * 0.7f + cellSize * 0.08f, cellSize * 0.7f), strokeWidth = 1.5f)
                                drawLine(Color.White, start = Offset(cellSize * 0.7f, cellSize * 0.7f - cellSize * 0.08f), end = Offset(cellSize * 0.7f, cellSize * 0.7f + cellSize * 0.08f), strokeWidth = 1.5f)
                            }
                            ComponentType.URANIUM -> {
                                val scaleU = 0.8f + 0.2f * kotlin.math.sin((System.currentTimeMillis() % 1500) / 1500f * 2.0 * Math.PI).toFloat()
                                drawCircle(Color(0xCC00FF00), radius = cellSize * 0.16f * scaleU, center = Offset(cellSize * 0.5f, cellSize * 0.5f))
                                drawCircle(Color.Black.copy(alpha = 0.35f), radius = cellSize * 0.26f, center = Offset(cellSize * 0.5f, cellSize * 0.5f), style = Stroke(1.5f))
                            }
                            ComponentType.OBSIDIAN -> {
                                val swirlPath = Path().apply {
                                    moveTo(0f, cellSize * 0.5f)
                                    quadraticTo(cellSize * 0.5f, cellSize * 0.2f, cellSize, cellSize * 0.7f)
                                }
                                drawPath(swirlPath, Color(0xFF6A1B9A), style = Stroke(2.2f))
                            }
                            ComponentType.BEDROCK -> {
                                drawRect(Color(0xFF333333), topLeft = Offset(cellSize*0.1f, cellSize*0.1f), size = Size(cellSize*0.8f, cellSize*0.8f), style = Stroke(2f))
                                drawCircle(Color(0xFF424242), radius = cellSize*0.06f, center = Offset(cellSize*0.25f, cellSize*0.25f))
                                drawCircle(Color(0xFF424242), radius = cellSize*0.06f, center = Offset(cellSize*0.75f, cellSize*0.75f))
                                drawCircle(Color(0xFF424242), radius = cellSize*0.06f, center = Offset(cellSize*0.25f, cellSize*0.75f))
                                drawCircle(Color(0xFF424242), radius = cellSize*0.06f, center = Offset(cellSize*0.75f, cellSize*0.25f))
                            }
                            ComponentType.BRICK -> {
                                drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize*0.33f), end = Offset(cellSize, cellSize*0.33f), strokeWidth = 2f)
                                drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize*0.66f), end = Offset(cellSize, cellSize*0.66f), strokeWidth = 2f)
                                drawLine(Color(0xFFB71C1C), start = Offset(cellSize*0.5f, 0f), end = Offset(cellSize*0.5f, cellSize*0.33f), strokeWidth = 2f)
                                drawLine(Color(0xFFB71C1C), start = Offset(cellSize*0.25f, cellSize*0.33f), end = Offset(cellSize*0.25f, cellSize*0.66f), strokeWidth = 2f)
                                drawLine(Color(0xFFB71C1C), start = Offset(cellSize*0.75f, cellSize*0.33f), end = Offset(cellSize*0.75f, cellSize*0.66f), strokeWidth = 2f)
                                drawLine(Color(0xFFB71C1C), start = Offset(cellSize*0.5f, cellSize*0.66f), end = Offset(cellSize*0.5f, cellSize), strokeWidth = 2f)
                            }
                            ComponentType.DIAMOND -> {
                                val dPath = Path().apply {
                                    moveTo(cx, cellSize * 0.15f)
                                    lineTo(cellSize * 0.85f, cy)
                                    lineTo(cx, cellSize * 0.85f)
                                    lineTo(cellSize * 0.15f, cy)
                                    close()
                                }
                                drawPath(dPath, matColor)
                                drawPath(dPath, Color.White, style = Stroke(1.5f))
                                drawLine(Color.White, start = Offset(cx, cellSize * 0.15f), end = Offset(cx, cellSize * 0.85f), strokeWidth = 1f)
                                drawLine(Color.White, start = Offset(cellSize * 0.15f, cy), end = Offset(cellSize * 0.85f, cy), strokeWidth = 1f)
                            }
                            ComponentType.STEEL -> {
                                drawRect(Color(0xFF78909C), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize), style = Stroke(2.0f))
                                val steelShine = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent))
                                drawRect(steelShine, size = Size(cellSize, cellSize))
                                drawCircle(Color(0xFF455A64), radius = 2f, center = Offset(4f, 4f))
                                drawCircle(Color(0xFF455A64), radius = 2f, center = Offset(cellSize - 4f, 4f))
                                drawCircle(Color(0xFF455A64), radius = 2f, center = Offset(4f, cellSize - 4f))
                                drawCircle(Color(0xFF455A64), radius = 2f, center = Offset(cellSize - 4f, cellSize - 4f))
                            }
                            ComponentType.COPPER -> {
                                val copperR = if (component.isPowered) Color(0xFFFF5722) else Color(0xFFA23210)
                                drawLine(copperR, start = Offset(cellSize * 0.2f, 0f), end = Offset(cellSize * 0.2f, cellSize), strokeWidth = 1.5f)
                                drawLine(copperR, start = Offset(cellSize * 0.5f, 0f), end = Offset(cellSize * 0.5f, cellSize), strokeWidth = 2.5f)
                                drawLine(copperR, start = Offset(cellSize * 0.8f, 0f), end = Offset(cellSize * 0.8f, cellSize), strokeWidth = 1.5f)
                            }
                            ComponentType.GOLD -> {
                                drawRect(Color(0xFFFFB300), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize), style = Stroke(2.5f))
                                val goldShine = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Transparent, Color(0x77FFFFFF), Color.Transparent))
                                drawRect(goldShine, size = Size(cellSize, cellSize))
                                drawLine(Color.White, start = Offset(cx - 3f, cy - 3f), end = Offset(cx + 3f, cy + 3f), strokeWidth = 1.5f)
                                drawLine(Color.White, start = Offset(cx + 3f, cy - 3f), end = Offset(cx - 3f, cy + 3f), strokeWidth = 1.5f)
                            }
                            ComponentType.ALUMINUM -> {
                                for(i in 1..3) {
                                    val h = i * (cellSize / 4)
                                    drawLine(Color(0x33FFFFFF), start = Offset(0f, h), end = Offset(cellSize, h), strokeWidth = 1f)
                                }
                            }
                            ComponentType.PIPE -> {
                                val arrowColor = Color(0xFF00E676)
                                drawRect(Color(0xFF263238), topLeft = Offset(cellSize*0.2f, cellSize*0.2f), size = Size(cellSize*0.6f, cellSize*0.6f))
                                when (component.direction) {
                                    Direction.RIGHT -> {
                                        drawLine(arrowColor, start = Offset(cellSize*0.2f, cy), end = Offset(cellSize*0.8f, cy), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cellSize*0.8f, cy), end = Offset(cellSize*0.5f, cy - cellSize*0.2f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cellSize*0.8f, cy), end = Offset(cellSize*0.5f, cy + cellSize*0.2f), strokeWidth = 4f)
                                    }
                                    Direction.LEFT -> {
                                        drawLine(arrowColor, start = Offset(cellSize*0.8f, cy), end = Offset(cellSize*0.2f, cy), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cellSize*0.2f, cy), end = Offset(cellSize*0.5f, cy - cellSize*0.2f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cellSize*0.2f, cy), end = Offset(cellSize*0.5f, cy + cellSize*0.2f), strokeWidth = 4f)
                                    }
                                    Direction.UP -> {
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.8f), end = Offset(cx, cellSize*0.2f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.2f), end = Offset(cx - cellSize*0.2f, cellSize*0.5f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.2f), end = Offset(cx + cellSize*0.2f, cellSize*0.5f), strokeWidth = 4f)
                                    }
                                    Direction.DOWN -> {
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.2f), end = Offset(cx, cellSize*0.8f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.8f), end = Offset(cx - cellSize*0.2f, cellSize*0.5f), strokeWidth = 4f)
                                        drawLine(arrowColor, start = Offset(cx, cellSize*0.8f), end = Offset(cx + cellSize*0.2f, cellSize*0.5f), strokeWidth = 4f)
                                    }
                                }
                            }
                            else -> {
                                // Default simple shine gradient for any unhandled solid elements
                                val fineShine = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent))
                                drawRect(fineShine, size = Size(cellSize, cellSize))
                            }
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
