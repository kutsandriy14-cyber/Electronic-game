package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent

object TabletRender {

    // ThreadLocal Path to completely avoid garbage collection allocations during cell-by-cell drawing
    private val pathThreadLocal = object : ThreadLocal<Path>() {
        override fun initialValue(): Path = Path()
    }

    private fun getReusablePath(): Path {
        val p = pathThreadLocal.get() ?: Path()
        p.reset()
        return p
    }

    // Static cached Brushes to eliminate list/gradient allocations inside the drawing loop
    private val batteryBrush = Brush.verticalGradient(listOf(Color(0xFF4F378B), Color(0xFF221144)))
    private val greenEnergyBrush = Brush.verticalGradient(listOf(Color(0xFFCCFF00), Color(0xFF00AA00)))
    private val solarPanelBrush = Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F1D45)))
    private val icBrush = Brush.linearGradient(listOf(Color(0xFF2C2C34), Color(0xFF15151A)))
    private val ramBrush = Brush.linearGradient(listOf(Color(0xFF1E3A28), Color(0xFF0F1E14)))
    private val metalShineBrush = Brush.linearGradient(listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent))

    fun getMaxCap(component: GridComponent): Float {
        val default = when(component.type) { 
            ComponentType.COIN_CELL -> 100f 
            ComponentType.BATTERY_PACK -> 10000f 
            ComponentType.INFINITE_BATTERY -> 9999999f 
            ComponentType.NUCLEAR_REACTOR -> 1000000f 
            else -> 2500f 
        }
        val extra = component.extraData
        if (extra.isEmpty()) return default
        val idx = extra.indexOf("c=")
        if (idx != -1) {
            val end = extra.indexOf('|', idx)
            val extracted = if (end != -1) extra.substring(idx+2, end) else extra.substring(idx+2)
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
            val type = component.type
            if (type == ComponentType.EMPTY || type == ComponentType.PAN || type == ComponentType.ROTATE || type == ComponentType.INSPECT) return

            // LEVEL OF DETAIL (LOD) OPTIMIZATION:
            // Tablet viewport can be huge; if zoomed out structure is very small, bypass all curves, texts, and custom geometries
            // with single clean flat boxes and custom overlays. This gives extreme perf back on giant grids.
            if (cellSize < 32f) {
                val flatColor = when {
                    component.isOverloaded -> Color(0xFFFF3B30)
                    type.category == ComponentCategory.MATERIALS || type.category == ComponentCategory.HYDRAULICS -> {
                        when (type) {
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
                            ComponentType.FLUID_DRAIN, ComponentType.VOID_HOLE -> Color.Black
                            ComponentType.STEEL -> Color(0xFFB0BEC5)
                            ComponentType.COPPER -> Color(0xFFD84315)
                            ComponentType.GOLD -> Color(0xFFFFD54F)
                            ComponentType.ALUMINUM -> Color(0xFFCFD8DC)
                            ComponentType.PLASTIC -> Color(0xFFFFCC80)
                            ComponentType.CLAY -> Color(0xFFBCAAA4)
                            ComponentType.BRICK -> Color(0xFFD32F2F)
                            ComponentType.OBSIDIAN -> Color(0xFF1C1C1C)
                            ComponentType.BEDROCK -> Color(0xFF000000)
                            else -> Color(0xFF49454F)
                        }
                    }
                    type.category == ComponentCategory.CONDUCTORS -> if (component.isPowered) Color(0xFF00FF00) else Color(0xFF49454F)
                    type.category == ComponentCategory.POWER -> Color(0xFFFFEB3B)
                    type.category == ComponentCategory.LOGIC -> Color(0xFFE91E63)
                    type.category == ComponentCategory.SENSORS -> Color(0xFF00BCD4)
                    type.category == ComponentCategory.OUTPUTS -> Color(0xFFFF9800)
                    type.category == ComponentCategory.ANALOG_ICS -> Color(0xFF9C27B0)
                    type.category == ComponentCategory.ADVANCED -> Color(0xFF3F51B5)
                    type.category == ComponentCategory.SWITCHES -> Color(0xFF4CAF50)
                    else -> Color(0xFF424242)
                }
                drawRect(flatColor, size = Size(cellSize, cellSize))
                if (component.isPowered && type.category != ComponentCategory.CONDUCTORS) {
                    drawRect(Color.White.copy(alpha = 0.3f), size = Size(cellSize, cellSize), style = Stroke(2f))
                }
                return
            }

            // Normal rendering with fully-optimized draws for Tablet
            when (type) {
                ComponentType.WIRE_ANY, ComponentType.SUPERCONDUCTOR, ComponentType.HIGH_VOLTAGE_CABLE, ComponentType.FIBER_OPTIC -> {
                    val isSuper = type == ComponentType.SUPERCONDUCTOR || type == ComponentType.HIGH_VOLTAGE_CABLE
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
                    
                    // High-perf connection counts (avoid array/list helper allocations)
                    val connects = (if (up) 1 else 0) + (if (down) 1 else 0) + (if (left) 1 else 0) + (if (right) 1 else 0)
                    if (connects != 2 || (up && down && left && right)) {
                        drawCircle(wireColor, radius = wStroke * 0.8f, center = Offset(cx, cy))
                    } else if (connects == 0) {
                        drawCircle(wireColor, radius = wStroke * 1.5f, center = Offset(cx, cy))
                    }
                }
                
                ComponentType.BATTERY -> {
                    // Use cached Brush static singleton
                    drawRect(
                        brush = batteryBrush,
                        topLeft = Offset(padding, padding * 2), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 4)
                    )
                    val maxCap = getMaxCap(component)
                    if (component.charge in 0f..maxCap) {
                        val pct = (component.charge / maxCap).coerceIn(0f, 1f)
                        drawRect(Color(0xFF4CAF50), topLeft = Offset(padding + strokeSize, padding * 2 + strokeSize + (cellSize - padding * 4 - strokeSize * 2) * (1f - pct)), size = Size(cellSize - padding * 2 - strokeSize * 2, (cellSize - padding * 4 - strokeSize * 2) * pct))
                    }
                    drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.35f, padding), size = Size(cellSize * 0.3f, padding))
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx, cy - padding), end = Offset(cx, cy + padding), strokeWidth=strokeSize*0.5f)
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding, cy), end = Offset(cx + padding, cy), strokeWidth=strokeSize*0.5f)
                }
                
                ComponentType.NUCLEAR_REACTOR, ComponentType.WIND_TURBINE, ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, ComponentType.THERMOELECTRIC_GENERATOR, ComponentType.INFINITE_BATTERY -> {
                    drawRect(
                        brush = greenEnergyBrush,
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 2)
                    )
                    drawCircle(Color.Black, radius = Math.max(1f, strokeSize), center = Offset(cx, cy))
                }
                
                ComponentType.GENERATOR -> {
                    drawCircle(
                        brush = batteryBrush,
                        radius = cellSize * 0.4f, 
                        center = Offset(cx, cy)
                    )
                    drawCircle(Color(0xFFD0BCFF), radius = cellSize * 0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
                    
                    // Sine wave optimized: two quick arcs to avoid Path allocation
                    drawArc(Color(0xFFD0BCFF), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(cx - cellSize * 0.2f, cy - cellSize * 0.1f), size = Size(cellSize * 0.2f, cellSize * 0.2f), style = Stroke(strokeSize * 0.5f))
                    drawArc(Color(0xFFD0BCFF), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(cx, cy - cellSize * 0.1f), size = Size(cellSize * 0.2f, cellSize * 0.2f), style = Stroke(strokeSize * 0.5f))
                }

                ComponentType.SOLAR_PANEL -> {
                    // Modern crystalline dark-blue textured grid solar cell
                    val rectSize = cellSize - padding * 2
                    // Deep blue solar wafer backplane
                    drawRect(Color(0xFF0F2042), topLeft = Offset(padding, padding), size = Size(rectSize, rectSize))
                    // Anti-reflective coating gleam pattern (slanted diagonal paths)
                    drawLine(Color(0x22FFFFFF), start = Offset(padding, padding + rectSize * 0.2f), end = Offset(padding + rectSize * 0.8f, padding), strokeWidth = 2f)
                    drawLine(Color(0x11FFFFFF), start = Offset(padding, padding + rectSize * 0.5f), end = Offset(padding + rectSize * 0.5f, padding), strokeWidth = 3f)
                    
                    // Metallic contact Grid lines (Fingers)
                    for (i in 1..4) {
                        val fraction = i * rectSize / 5f
                        drawLine(Color(0xFFECEFF1), start = Offset(padding + fraction, padding), end = Offset(padding + fraction, cellSize - padding), strokeWidth = 1f)
                        drawLine(Color(0xFFECEFF1), start = Offset(padding, padding + fraction), end = Offset(cellSize - padding, padding + fraction), strokeWidth = 1.5f)
                    }
                    // Thicker silver busbar runs
                    drawLine(Color(0xFFCFD1D3), start = Offset(cx - 2f, padding), end = Offset(cx - 2f, cellSize - padding), strokeWidth = 2.5f)
                    drawLine(Color(0xFFCFD1D3), start = Offset(cx + 2f, padding), end = Offset(cx + 2f, cellSize - padding), strokeWidth = 2.5f)
                    
                    // Framing and corner mounting mounts
                    drawRect(Color(0xFF37474F), topLeft = Offset(padding, padding), size = Size(rectSize, rectSize), style = Stroke(strokeSize * 0.6f))
                    // Active light generation halo when powered
                    if (component.isPowered) {
                        drawRect(Color(0x3300FFCC), topLeft = Offset(padding, padding), size = Size(rectSize, rectSize))
                        drawCircle(Color(0xFF00FFCC), radius = 3f, center = Offset(cx, cy))
                    }
                }

                ComponentType.DIODE -> {
                    // Leads
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.6f)
                    // Cylindrical black package
                    drawRoundRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(cx - padding * 1.3f, padding * 1.8f),
                        size = Size(padding * 2.6f, cellSize - padding * 3.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Silver cathode band towards the top (-) pin
                    drawRect(
                        color = Color(0xFFECEFF1),
                        topLeft = Offset(cx - padding * 1.3f, padding * 1.8f),
                        size = Size(padding * 2.6f, strokeSize * 0.8f)
                    )
                    // Inner arrow direction schematic
                    val arrowPath = getReusablePath()
                    arrowPath.moveTo(cx - padding * 0.8f, cy + padding * 0.8f)
                    arrowPath.lineTo(cx + padding * 0.8f, cy + padding * 0.8f)
                    arrowPath.lineTo(cx, cy - padding * 0.4f)
                    arrowPath.close()
                    drawPath(arrowPath, Color(0x99FFFFFF))
                    drawLine(Color(0x99FFFFFF), start = Offset(cx - padding * 0.8f, cy - padding * 0.4f), end = Offset(cx + padding * 0.8f, cy - padding * 0.4f), strokeWidth = 2f)

                    // Forward biase conduction glow
                    if (component.isPowered) {
                        drawRoundRect(
                            color = Color(0x3300FF00),
                            topLeft = Offset(cx - padding * 1.3f, padding * 1.8f),
                            size = Size(padding * 2.6f, cellSize - padding * 3.6f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }

                ComponentType.ZENER_DIODE -> {
                    // Lead wires
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.6f)
                    // Glass semi-translucent reddish cylindrical package
                    drawRoundRect(
                        color = Color(0xFFE64A19),
                        topLeft = Offset(cx - padding * 1.1f, padding * 2f),
                        size = Size(padding * 2.2f, cellSize - padding * 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    // Glass highlight shine
                    drawRect(
                        color = Color(0x44FFFFFF),
                        topLeft = Offset(cx - padding * 0.9f, padding * 2f),
                        size = Size(padding * 0.4f, cellSize - padding * 4f)
                    )
                    // Black band on cathode (-) side
                    drawRect(
                        color = Color(0xFF212121),
                        topLeft = Offset(cx - padding * 1.1f, padding * 2f),
                        size = Size(padding * 2.2f, strokeSize * 0.8f)
                    )
                    // Zener Z-bent symbol inside matching normal schematic
                    drawLine(Color(0x99FFFFFF), start = Offset(cx - padding * 0.6f, cy - padding * 0.6f), end = Offset(cx + padding * 0.6f, cy - padding * 0.6f), strokeWidth = 1.5f)
                    drawLine(Color(0x99FFFFFF), start = Offset(cx - padding * 0.6f, cy - padding * 0.6f), end = Offset(cx - padding * 0.6f, cy - padding * 0.3f), strokeWidth = 1.5f) // Zener bent left
                    drawLine(Color(0x99FFFFFF), start = Offset(cx + padding * 0.6f, cy - padding * 0.6f), end = Offset(cx + padding * 0.6f, cy - padding * 0.9f), strokeWidth = 1.5f) // Zener bent right
                    
                    if (component.isPowered) {
                        drawCircle(Color(0xFFFF9100), radius = 3f, center = Offset(cx, cy))
                    }
                }

                ComponentType.RESISTOR -> {
                    // Leads out of ends
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.5f)
                    
                    // Resistor ceramic dog-bone style package
                    val bodyTop = padding * 1.8f
                    val bodyHeight = cellSize - padding * 3.6f
                    val rx = cx - padding * 1.2f
                    val rw = padding * 2.4f
                    
                    // Base ceramic body color (classic tan/light beige)
                    drawRoundRect(
                        color = Color(0xFFD7CCC8),
                        topLeft = Offset(rx, bodyTop),
                        size = Size(rw, bodyHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    // Bulged end caps
                    drawRoundRect(color = Color(0xFFCFD8DC), topLeft = Offset(rx, bodyTop - 2f), size = Size(rw, strokeSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                    drawRoundRect(color = Color(0xFFCFD8DC), topLeft = Offset(rx, bodyTop + bodyHeight - strokeSize + 2f), size = Size(rw, strokeSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))

                    // Classic Four-Band Resistor Bands (e.g. Brown, Black, Red, Gold for 1k ohm)
                    // Band 1: Brown (1)
                    drawRect(Color(0xFF5D4037), topLeft = Offset(rx, bodyTop + bodyHeight * 0.25f), size = Size(rw, 4f))
                    // Band 2: Black (0)
                    drawRect(Color(0xFF212121), topLeft = Offset(rx, bodyTop + bodyHeight * 0.45f), size = Size(rw, 4f))
                    // Band 3: Red (x100)
                    drawRect(Color(0xFFE53935), topLeft = Offset(rx, bodyTop + bodyHeight * 0.65f), size = Size(rw, 4f))
                    // Band 4: Golden (Tolerance 5%)
                    drawRect(Color(0xFFFFB300), topLeft = Offset(rx, bodyTop + bodyHeight * 0.82f), size = Size(rw, 4.5f))
                    
                    // Current pathway indicator glow on power
                    if (component.isPowered) {
                        drawRect(Color(0x3300FFCC), topLeft = Offset(rx, bodyTop), size = Size(rw, bodyHeight))
                    }
                }

                ComponentType.CAPACITOR -> {
                    // Lead wires emerging
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.5f)
                    
                    // Aluminum cylindrical canister sleeve (deep electrolytic blue or dark grey)
                    val capT = padding * 1.5f
                    val capH = cellSize - padding * 3.0f
                    val capW = padding * 2.3f
                    val capX = cx - capW / 2
                    
                    drawRoundRect(
                        color = Color(0xFF1E88E5), // Premium Blue capacitor sleeve
                        topLeft = Offset(capX, capT),
                        size = Size(capW, capH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    
                    // Aluminum exposed metallic bottom rubber seal details
                    drawRect(Color(0xFF37474F), topLeft = Offset(capX + 2f, capT + capH - 4f), size = Size(capW - 4f, 4f))

                    // Polarity stripe (Negative white stripe on right side with '-' minus markers)
                    val stripeW = capW * 0.35f
                    drawRect(
                        color = Color(0xFFECEFF1),
                        topLeft = Offset(capX + capW - stripeW - 2f, capT),
                        size = Size(stripeW, capH)
                    )
                    // Mini minus signs printed on stripe
                    for (i in 0..2) {
                        val stripeY = capT + capH * 0.25f + i * capH * 0.25f
                        drawLine(Color(0xFF546E7A), start = Offset(capX + capW - stripeW + 1f, stripeY), end = Offset(capX + capW - 3f, stripeY), strokeWidth = 1.5f)
                    }

                    // Metal safety explosion vent lines (X-vent/K-vent score engraving on top cap)
                    drawLine(Color(0xFF1565C0), start = Offset(cx - 3f, capT + 4f), end = Offset(cx + 3f, capT + 4f), strokeWidth = 1f)
                    drawLine(Color(0xFF1565C0), start = Offset(cx, capT + 1f), end = Offset(cx, capT + 6f), strokeWidth = 1f)

                    // Charge-dependent blue dynamic glow
                    if (component.charge > 0.05f) {
                        val maxCap = getMaxCap(component)
                        val chargeRatio = (component.charge / maxCap).coerceIn(0f, 1f)
                        drawCircle(Color(0xAA00E5FF).copy(alpha = chargeRatio * 0.8f), radius = strokeSize * 2f, center = Offset(cx, cy))
                    }
                }

                ComponentType.INDUCTOR -> {
                    // Lead wires
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.5f)
                    
                    // Toroidal core donut in center
                    val coreRadius = cellSize * 0.32f
                    drawCircle(Color(0xFF37474F), radius = coreRadius, center = Offset(cx, cy))
                    drawCircle(Color(0xFF111111), radius = coreRadius * 0.45f, center = Offset(cx, cy)) // Donut hole
                    
                    // Copper wire windings (wrapped around donut core)
                    val windingColor = Color(0xFFD84315) // Brassy red copper wire
                    for (angle in listOf(0f, 40f, 80f, 120f, 160f, 200f, 240f, 280f, 320f)) {
                        val rad = Math.toRadians(angle.toDouble())
                        val cos = Math.cos(rad).toFloat()
                        val sin = Math.sin(rad).toFloat()
                        
                        // Small copper pill wraps
                        drawLine(
                            color = windingColor,
                            start = Offset(cx + coreRadius * 0.35f * cos, cy + coreRadius * 0.35f * sin),
                            end = Offset(cx + coreRadius * 1.15f * cos, cy + coreRadius * 1.15f * sin),
                            strokeWidth = 3f
                        )
                        // Brighter copper specular highlights
                        drawLine(
                            color = Color(0xFFFF8A65),
                            start = Offset(cx + coreRadius * 0.7f * cos, cy + coreRadius * 0.7f * sin),
                            end = Offset(cx + coreRadius * 1.0f * cos, cy + coreRadius * 1.0f * sin),
                            strokeWidth = 1f
                        )
                    }
                    
                    // Dynamic magnetic field lines glow when powered!
                    if (component.isPowered) {
                        drawCircle(Color(0x33FF9800), radius = coreRadius * 1.35f, center = Offset(cx, cy), style = Stroke(2f))
                        drawCircle(Color(0xFFFF9800), radius = 3f, center = Offset(cx, cy))
                    }
                }

                ComponentType.FUSE -> {
                    // Core connecting line
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.5f)
                    
                    val fuseW = padding * 1.8f
                    val fuseH = cellSize - padding * 4f
                    val fuseX = cx - fuseW / 2
                    val fuseY = padding * 2f
                    
                    // Transparent glass tube cylinder
                    drawRoundRect(
                        color = Color(0x33B2DFDB),
                        topLeft = Offset(fuseX, fuseY),
                        size = Size(fuseW, fuseH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Glass border sheen
                    drawRoundRect(
                        color = Color(0x66FFFFFF),
                        topLeft = Offset(fuseX, fuseY),
                        size = Size(fuseW, fuseH),
                        style = Stroke(1.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    
                    // Metal shiny silver terminal caps on both ends
                    val capHeight = strokeSize * 1.2f
                    drawRect(Color(0xFFECEFF1), topLeft = Offset(fuseX, fuseY), size = Size(fuseW, capHeight))
                    drawRect(Color(0xFF546E7A), topLeft = Offset(fuseX, fuseY + capHeight - 1f), size = Size(fuseW, 1.5f)) // crease line
                    drawRect(Color(0xFFECEFF1), topLeft = Offset(fuseX, fuseY + fuseH - capHeight), size = Size(fuseW, capHeight))
                    drawLine(Color(0xFF546E7A), start = Offset(fuseX, fuseY + fuseH - capHeight + 1f), end = Offset(fuseX + fuseW, fuseY + fuseH - capHeight + 1f), strokeWidth = 1.5f)

                    // Filament wire inside glass
                    if (component.isOverloaded) {
                        // Broken blown filament wire!
                        drawLine(Color(0xFFFF3B30), start = Offset(cx, fuseY + capHeight), end = Offset(cx - 3f, cy - 3f), strokeWidth = 1.5f)
                        drawLine(Color(0xFFFF3B30), start = Offset(cx, fuseY + fuseH - capHeight), end = Offset(cx + 3f, cy + 3f), strokeWidth = 1.5f)
                        // Spark indicator
                        drawCircle(Color(0xFFFF3D00), radius = 2.5f, center = Offset(cx - 1f, cy - 1f))
                    } else {
                        // Intact glowing filament fuse wire
                        val filamentCol = if (component.isPowered) Color(0xFFFFD54F) else Color(0xFF78909C)
                        if (component.isPowered) {
                            // outer glow
                            drawLine(Color(0x88FFD54F), start = Offset(cx, fuseY + capHeight), end = Offset(cx, fuseY + fuseH - capHeight), strokeWidth = 4.0f)
                        }
                        drawLine(filamentCol, start = Offset(cx, fuseY + capHeight), end = Offset(cx, fuseY + fuseH - capHeight), strokeWidth = 1.5f)
                    }
                }

                ComponentType.BULB -> {
                    val bulbColor = if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF332D41)
                    if (component.isPowered) drawCircle(Color(0xFFD0BCFF).copy(alpha = 0.4f), radius = cellSize * 0.45f, center = Offset(cx, cy))
                    drawCircle(bulbColor, radius = cellSize * 0.35f, center = Offset(cx, cy))
                    drawLine(Color(0xFF1C1B1F), start = Offset(cx - padding * 1.5f, cy), end = Offset(cx + padding * 1.5f, cy), strokeWidth = strokeSize * 0.5f)
                    drawLine(color, start = Offset(cx, cellSize * 0.85f), end = Offset(cx, cellSize), strokeWidth = strokeSize)
                }

                ComponentType.DISPLAY_PIXEL -> {
                    val pixelColor = if (component.isPowered) Color.White else Color(0xFF111111)
                    if (component.isPowered) {
                        drawRect(Color(0x22FFFFFF), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize))
                    }
                    drawRect(pixelColor, topLeft = Offset(cellSize * 0.1f, cellSize * 0.1f), size = Size(cellSize * 0.8f, cellSize * 0.8f))
                }
                
                ComponentType.LED -> {
                    val ledColor = if (component.isPowered) Color(0xFFFF3366) else Color(0xFF4A1020)
                    if (component.isPowered) drawCircle(ledColor.copy(alpha = 0.5f), radius = cellSize * 0.4f, center = Offset(cx, cy))
                    drawCircle(ledColor, radius = cellSize * 0.25f, center = Offset(cx, cy))
                    drawLine(color, start = Offset(cx, cellSize * 0.75f), end = Offset(cx, cellSize), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx, 0f), end = Offset(cx, cellSize * 0.25f), strokeWidth = strokeSize)
                }

                ComponentType.MOTOR -> {
                    drawCircle(Color(0xFF2B2930), radius = cellSize * 0.4f, center = Offset(cx, cy))
                    drawCircle(color, radius = cellSize * 0.4f, center = Offset(cx, cy), style = Stroke(strokeSize))
                    
                    val p = getReusablePath()
                    p.moveTo(cx - padding, cy + padding)
                    p.lineTo(cx - padding, cy - padding)
                    p.lineTo(cx, cy)
                    p.lineTo(cx + padding, cy - padding)
                    p.lineTo(cx + padding, cy + padding)
                    drawPath(p, color, style = Stroke(strokeSize * 0.5f))
                }

                ComponentType.SPEAKER -> {
                    val p = getReusablePath()
                    p.moveTo(cx, cy - padding)
                    p.lineTo(cx + padding, cy - padding * 2)
                    p.lineTo(cx + padding, cy + padding * 2)
                    p.lineTo(cx, cy + padding)
                    p.close()
                    drawPath(p, color)
                    drawRect(color, topLeft = Offset(cx - padding, cy - padding), size = Size(padding, padding * 2))
                    if (component.isPowered) {
                        drawArc(color, -60f, 120f, false, topLeft = Offset(cx + padding, cy - padding * 1.5f), size = Size(padding * 2, padding * 3), style = Stroke(strokeSize * 0.5f))
                    }
                }

                ComponentType.BUZZER -> {
                    // Black outer solid circle body
                    val bRad = cellSize * 0.40f
                    drawCircle(Color(0xFF263238), radius = bRad, center = Offset(cx, cy))
                    drawCircle(Color(0xFF1E272C), radius = bRad, center = Offset(cx, cy), style = Stroke(strokeSize * 0.5f))
                    
                    // Central hole revealing golden brass piezo transducer diaphragm inside
                    val hRad = cellSize * 0.16f
                    drawCircle(Color(0xFFFFB74D), radius = hRad, center = Offset(cx, cy)) // Piezo brass disc
                    drawCircle(Color(0xFF212121), radius = hRad * 0.7f, center = Offset(cx, cy)) // Dark inner sound port hole
                    
                    // Labeled plus polarity symbol (+)
                    drawLine(Color(0x77FFFFFF), start = Offset(cx + bRad * 0.5f, cy - bRad * 0.5f - 4f), end = Offset(cx + bRad * 0.5f, cy - bRad * 0.5f + 4f), strokeWidth = 1f)
                    drawLine(Color(0x77FFFFFF), start = Offset(cx + bRad * 0.5f - 4f, cy - bRad * 0.5f), end = Offset(cx + bRad * 0.5f + 4f, cy - bRad * 0.5f), strokeWidth = 1f)

                    // Concentric sonic acoustic waves radiating when active
                    if (component.isPowered) {
                        val pulse = (System.currentTimeMillis() % 600) / 600f
                        drawCircle(
                            color = Color(0xFFFF5722).copy(alpha = 1f - pulse),
                            radius = bRad + pulse * 14f,
                            center = Offset(cx, cy),
                            style = Stroke(1.5f)
                        )
                        drawCircle(Color(0xFFFF5722), radius = 2.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.MICROPHONE -> {
                    // Lead wire traces
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 2.5f)
                    
                    val micRad = cellSize * 0.36f
                    // Round outer casing
                    drawCircle(Color(0xFF37474F), radius = micRad, center = Offset(cx, cy))
                    // Silver casing ring borders
                    drawCircle(Color(0xFF607D8B), radius = micRad, center = Offset(cx, cy), style = Stroke(strokeSize * 0.4f))
                    
                    // Inner electret felt cross guard pad (black acoustic center)
                    drawCircle(Color(0xFF1A1A1A), radius = micRad * 0.75f, center = Offset(cx, cy))
                    
                    // Copper / brass mesh wire guidelines (cross guard grid)
                    val meshCol = Color(0x33B2DFDB)
                    for (i in -2..2) {
                        val offset = i * micRad * 0.28f
                        drawLine(meshCol, start = Offset(cx - micRad * 0.7f, cy + offset), end = Offset(cx + micRad * 0.7f, cy + offset), strokeWidth = 1f)
                        drawLine(meshCol, start = Offset(cx + offset, cy - micRad * 0.7f), end = Offset(cx + offset, cy + micRad * 0.7f), strokeWidth = 1f)
                    }
                    
                    // Labeled "MIC" text symbol representation
                    drawRect(Color(0xCC212121), topLeft = Offset(cx - padding * 1.1f, cy + padding * 0.5f), size = Size(padding * 2.2f, strokeSize * 0.7f))
                    
                    // Dynamic vocal wave ripple halo on power active
                    if (component.isPowered) {
                        val waveAmt = ((System.currentTimeMillis() % 800) / 800f)
                        drawCircle(Color(0xFF00FFCC).copy(alpha = 1f - waveAmt), radius = micRad * (1f + waveAmt * 0.4f), center = Offset(cx, cy), style = Stroke(1.5f))
                        drawCircle(Color(0xFF00FFCC), radius = 3.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.SOLENOID -> {
                    // Outer square iron frame bracket
                    val fW = cellSize - padding * 2.4f
                    val fH = cellSize - padding * 2f
                    val fx = cx - fW / 2
                    val fy = cy - fH / 2
                    
                    // Grey high-precision outline body
                    drawRoundRect(Color(0xFF455A64), topLeft = Offset(fx, fy), size = Size(fW, fH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(strokeSize * 0.4f))
                    
                    // Dense internal copper electromagnet winding block
                    val coilW = fW - strokeSize * 1.5f
                    val coilH = fH - strokeSize * 1.5f
                    drawRect(
                        color = Color(0xFFD84315),
                        topLeft = Offset(cx - coilW / 2, cy - coilH / 2),
                        size = Size(coilW, coilH)
                    )
                    // Highlighting bright wires
                    for (i in -3..3) {
                        val lx = cx + i * coilW / 8f
                        drawLine(Color(0xFFFF7043), start = Offset(lx, cy - coilH / 2), end = Offset(lx, cy + coilH / 2), strokeWidth = 1.5f)
                    }
                    
                    // Sliding plunger armature shaft
                    val pushDist = if (component.isPowered) -cellSize * 0.15f else cellSize * 0.15f
                    val plungerW = strokeSize * 1.6f
                    drawRoundRect(
                        color = Color(0xFFECEFF1), // Chrome plated rod
                        topLeft = Offset(cx - plungerW / 2, cy - coilH / 2 + pushDist),
                        size = Size(plungerW, coilH + 12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                    // Plunger tip eyelet anchor loop
                    drawCircle(Color(0xFFB0BEC5), radius = plungerW * 0.6f, center = Offset(cx, cy - coilH / 2 + pushDist - 3f))
                    drawCircle(Color(0xFF455A64), radius = plungerW * 0.25f, center = Offset(cx, cy - coilH / 2 + pushDist - 3f))

                    // Spark indicator LED
                    val ind = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawCircle(ind, radius = 3.0f, center = Offset(cx + fW / 2 - 4f, cy - fH / 2 + 4f))
                }

                ComponentType.LINEAR_ACTUATOR -> {
                    // Heavy industrial frame guides
                    val ly = padding * 1.5f
                    val lh = cellSize - padding * 3f
                    val lx = cx - padding * 1.2f
                    val lw = padding * 2.4f
                    
                    // Frame guide plate (black/orange heavy equipment backing plate)
                    drawRect(Color(0xFF212121), topLeft = Offset(lx, ly), size = Size(lw, lh))
                    
                    // Yellow/Black warning diagonal hatch stripes along side boundaries
                    val hatchW = 3f
                    for (i in 0..5) {
                        val hx = lx + 1f
                        val hy = ly + i * lh / 6f
                        drawLine(Color(0xFFFFEB3B), start = Offset(hx, hy), end = Offset(hx + lw * 0.25f, hy + lh * 0.08f), strokeWidth = hatchW)
                        drawLine(Color(0xFFFFEB3B), start = Offset(lx + lw - hatchW - lw * 0.25f, hy), end = Offset(lx + lw - hatchW, hy + lh * 0.08f), strokeWidth = hatchW)
                    }

                    // Polished central stainless lead screw shaft thread
                    val threadW = strokeSize * 0.7f
                    drawRect(Color(0xFFCFD8DC), topLeft = Offset(cx - threadW / 2, ly), size = Size(threadW, lh))
                    // Helical pitch notches on the screw thread
                    for (i in 0..10) {
                        val threadY = ly + i * lh / 10f
                        drawLine(Color(0xFF78909C), start = Offset(cx - threadW / 2, threadY), end = Offset(cx + threadW / 2, threadY + 2f), strokeWidth = 1.0f)
                    }

                    // Sliding carriage actuator traveler block
                    val travY = if (component.isPowered) ly + lh * 0.2f else ly + lh * 0.7f
                    val travH = strokeSize * 1.8f
                    val travW = lw - 4f
                    drawRoundRect(
                        color = Color(0xFFF44336), 
                        topLeft = Offset(cx - travW / 2, travY),
                        size = Size(travW, travH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    // Highlighting slide pads
                    drawRect(Color(0xFF1E272C), topLeft = Offset(cx - travW / 2, travY + 2f), size = Size(4f, travH - 4f))
                    drawRect(Color(0xFF1E272C), topLeft = Offset(cx + travW / 2 - 4f, travY + 2f), size = Size(4f, travH - 4f))

                    // Laser limits indicator
                    val led = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFFB300)
                    drawCircle(led, radius = 2.5f, center = Offset(cx, travY + travH / 2))
                }

                ComponentType.LASER_DIODE -> {
                    // Coaxial brass metal module cup
                    val bW = padding * 1.8f
                    val bH = cellSize - padding * 4f
                    val bx = cx - bW / 2
                    val by = padding * 2f
                    
                    // Red-brassy threaded body barrel
                    drawRoundRect(
                        color = Color(0xFFBCAAA4), 
                        topLeft = Offset(bx, by),
                        size = Size(bW, bH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                    // Heavy cooling ribs
                    for (i in 1..3) {
                        drawRect(Color(0xFF5D4037), topLeft = Offset(bx - 3f, by + i * bH / 4f), size = Size(bW + 6f, 3f))
                    }
                    
                    // Focusing lens collar module on exit end (top exit pointing up)
                    drawRect(Color(0xFF263238), topLeft = Offset(cx - bW * 0.3f, by - 4f), size = Size(bW * 0.6f, 6f))
                    
                    // Emitted coherent core laser beam! (Neon red/magenta glow extending up)
                    if (component.isPowered) {
                        // Wide outer bloom halo
                        drawLine(Color(0x33FF0055), start = Offset(cx, by - 3f), end = Offset(cx, 0f), strokeWidth = strokeSize * 1.8f)
                        drawLine(Color(0x99FF0055), start = Offset(cx, by - 3f), end = Offset(cx, 0f), strokeWidth = strokeSize * 0.8f)
                        // Intense white-hot inner optical filament
                        drawLine(Color.White, start = Offset(cx, by - 3f), end = Offset(cx, 0f), strokeWidth = 2.5f)
                        // Tiny laser point spark at lens
                        drawCircle(Color.White, radius = 3.5f, center = Offset(cx, by - 3f))
                        drawCircle(Color(0xFFFF0055), radius = 6.0f, center = Offset(cx, by - 3f))
                    } else {
                        // Subtle red center indicator gem
                        drawCircle(Color(0xFFDD2C00), radius = 2.0f, center = Offset(cx, by))
                    }
                }

                ComponentType.TEMPERATURE_SENSOR, ComponentType.THERMISTOR -> {
                    // Leading trace wires
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 2f)
                    
                    // Metal probe protective cylinder casing
                    drawRoundRect(
                        color = Color(0xFF90A4AE), 
                        topLeft = Offset(cx - padding * 0.6f, padding * 1.5f),
                        size = Size(padding * 1.2f, cellSize - padding * 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                    
                    // Distinct temperature bead/element inside cylinder cap
                    val tVal = component.temperature
                    val tCol = when {
                        tVal < 10f -> Color(0xFF00E5FF) 
                        tVal < 40f -> Color(0xFF4CAF50) 
                        tVal < 150f -> Color(0xFFFF9100) 
                        else -> Color(0xFFFF3D00) 
                    }
                    drawCircle(tCol, radius = padding * 0.4f, center = Offset(cx, padding * 2f))
                    
                    // Small dynamic LED indicator
                    val led = if (component.isPowered) Color(0xFF00FFCC) else Color(0x3300FFCC)
                    drawCircle(led, radius = 2.0f, center = Offset(cx, cy + padding))
                }

                ComponentType.PHOTORESISTOR, ComponentType.LIGHT_SENSOR -> {
                    // Connection traces
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 2.5f)
                    
                    val sR = cellSize * 0.38f
                    // Round white ceramic wafer head
                    drawCircle(Color(0xFFECEFF1), radius = sR, center = Offset(cx, cy))
                    drawCircle(Color(0xFFCFD8DC), radius = sR, center = Offset(cx, cy), style = Stroke(1.5f))
                    
                    // Serpentine Cadmium Sulfide zig-zag trace (glorious reactive orange zig-zag)
                    val snakeW = 2.5f
                    val topY = cy - sR * 0.6f
                    val botY = cy + sR * 0.6f
                    val lX = cx - sR * 0.6f
                    val rX = cx + sR * 0.6f
                    
                    val traceCol = if (component.isPowered) Color(0xFFFF3D00) else Color(0xFFE65100)
                    drawLine(traceCol, start = Offset(lX, topY), end = Offset(rX, topY), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(rX, topY), end = Offset(rX, cy - sR * 0.2f), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(rX, cy - sR * 0.2f), end = Offset(lX, cy - sR * 0.2f), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(lX, cy - sR * 0.2f), end = Offset(lX, cy + sR * 0.2f), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(lX, cy + sR * 0.2f), end = Offset(rX, cy + sR * 0.2f), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(rX, cy + sR * 0.2f), end = Offset(rX, botY), strokeWidth = snakeW)
                    drawLine(traceCol, start = Offset(rX, botY), end = Offset(lX, botY), strokeWidth = snakeW)
                    
                    // High-receptive photocell lens bubble center dot
                    drawCircle(Color(0x7700E5FF), radius = 3f, center = Offset(cx, cy))
                }

                ComponentType.SOUND_SENSOR -> {
                    // Green circuit board plate
                    val rectW = cellSize - padding * 2.2f
                    val rectH = cellSize - padding * 2.2f
                    val bx = cx - rectW / 2
                    val by = cy - rectH / 2
                    
                    drawRoundRect(Color(0xFF2E7D32), topLeft = Offset(bx, by), size = Size(rectW, rectH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                    drawRoundRect(Color(0xFF1B5E20), topLeft = Offset(bx, by), size = Size(rectW, rectH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(1.5f))
                    
                    // Condenser electret mic cylinder at top
                    val micRad = strokeSize * 1.5f
                    drawCircle(Color(0xFFB0BEC5), radius = micRad, center = Offset(cx, by + micRad + 2f))
                    drawCircle(Color(0xFF212121), radius = micRad * 0.75f, center = Offset(cx, by + micRad + 2f)) 
                    
                    // Square beautiful blue multi-turn trimmer potentiometer (calibration trimmer)
                    val trimW = padding * 1.2f
                    val trimX = bx + 6f
                    val trimY = by + rectH * 0.45f
                    drawRect(Color(0xFF0D47A1), topLeft = Offset(trimX, trimY), size = Size(trimW, trimW))
                    drawCircle(Color(0xFFFFD54F), radius = 3.5f, center = Offset(trimX + trimW * 0.5f, trimY + trimW * 0.5f)) 
                    drawLine(Color(0xFF5D4037), start = Offset(trimX + trimW * 0.5f - 2f, trimY + trimW * 0.5f), end = Offset(trimX + trimW * 0.5f + 2f, trimY + trimW * 0.5f), strokeWidth = 1f) 
                    
                    // Status indicator LEDs at bottom
                    val pwrLed = Color(0xFFFF1744) 
                    val outLed = if (component.isPowered) Color(0xFF00FFCC) else Color(0x3300FFCC) 
                    drawCircle(pwrLed, radius = 2f, center = Offset(bx + rectW - 6f, by + rectH - 12f))
                    drawCircle(outLed, radius = 2f, center = Offset(bx + rectW - 6f, by + rectH - 6f))
                }

                ComponentType.ULTRASONIC_SENSOR -> {
                    // Deep rich green module board backplane
                    val bW = cellSize - padding * 2.0f
                    val bH = cellSize - padding * 2.2f
                    val bx = cx - bW / 2
                    val by = cy - bH / 2
                    
                    drawRoundRect(
                        color = Color(0xFF0D47A1), 
                        topLeft = Offset(bx, by),
                        size = Size(bW, bH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    
                    // The double sounding "eyeball" aluminum sensors (T and R)
                    val cylinderRad = cellSize * 0.19f
                    val cyL = cx - cylinderRad - 2f
                    val cyR = cx + cylinderRad + 2f
                    val sensorY = cy - 1f
                    
                    // Transmitter (T) Casing
                    drawCircle(Color(0xFFCFD8DC), radius = cylinderRad, center = Offset(cyL, sensorY))
                    drawCircle(Color(0xFF263238), radius = cylinderRad * 0.75f, center = Offset(cyL, sensorY)) 
                    
                    // Receiver (R) Casing
                    drawCircle(Color(0xFFCFD8DC), radius = cylinderRad, center = Offset(cyR, sensorY))
                    drawCircle(Color(0xFF263238), radius = cylinderRad * 0.75f, center = Offset(cyR, sensorY)) 

                    // Sound letter mark labels inside eyeballs representing ultrasound logic
                    drawLine(Color(0x77FFFFFF), start = Offset(cyL - 3f, sensorY - 3f), end = Offset(cyL + 3f, sensorY - 3f), strokeWidth = 1f) 
                    drawLine(Color(0x77FFFFFF), start = Offset(cyL, sensorY - 3f), end = Offset(cyL, sensorY + 3f), strokeWidth = 1f)
                    
                    drawLine(Color(0x77FFFFFF), start = Offset(cyR - 3f, sensorY + 3f), end = Offset(cyR - 3f, sensorY - 3f), strokeWidth = 1.2f) 
                    drawLine(Color(0x77FFFFFF), start = Offset(cyR - 3f, sensorY - 3f), end = Offset(cyR + 1f, sensorY - 3f), strokeWidth = 1.2f)
                    drawLine(Color(0x77FFFFFF), start = Offset(cyR - 3f, sensorY), end = Offset(cyR + 1f, sensorY), strokeWidth = 1.2f)
                    drawLine(Color(0x77FFFFFF), start = Offset(cyR + 1f, sensorY), end = Offset(cyR - 1f, sensorY + 3f), strokeWidth = 1.2f)

                    // Dynamic wave animation when actively pulsing sonar
                    if (component.isPowered) {
                        val pulse = (System.currentTimeMillis() % 700) / 700f
                        drawCircle(Color(0xAA00E5FF).copy(alpha = 1f - pulse), radius = cylinderRad * (1f + pulse * 0.6f), center = Offset(cyL, sensorY), style = Stroke(1.5f))
                        drawCircle(Color(0xAA00E5FF).copy(alpha = 1f - pulse), radius = cylinderRad * (1f + pulse * 0.6f), center = Offset(cyR, sensorY), style = Stroke(1.5f))
                    }
                }

                ComponentType.SWITCH_OPEN -> {
                    drawCircle(color, radius = strokeSize, center = Offset(cx, padding * 2))
                    drawCircle(color, radius = strokeSize, center = Offset(cx, cellSize - padding * 2))
                    drawLine(if (component.isPowered) Color(0xFFD0BCFF) else Color(0xFF49454F), start = Offset(cx, padding * 2), end = Offset(cx - padding * 2, cellSize * 0.75f), strokeWidth = strokeSize * 0.8f)
                }
                
                ComponentType.SWITCH_CLOSED -> {
                    drawCircle(color, radius = strokeSize, center = Offset(cx, padding * 2))
                    drawCircle(color, radius = strokeSize, center = Offset(cx, cellSize - padding * 2))
                    drawLine(color, start = Offset(cx, padding * 2), end = Offset(cx, cellSize - padding * 2), strokeWidth = strokeSize * 0.8f)
                }

                ComponentType.PUSH_BUTTON -> {
                    drawCircle(color, radius = strokeSize, center = Offset(cx - padding * 1.5f, cy))
                    drawCircle(color, radius = strokeSize, center = Offset(cx + padding * 1.5f, cy))
                    drawRect(if (component.isPowered) color else Color(0xFF49454F), topLeft = Offset(cx - padding * 2, cy - padding * 1.5f), size = Size(padding * 4, strokeSize * 1.5f))
                }

                ComponentType.LOGIC_AND -> {
                    // Logic inputs and outputs signal traces
                    val activeIn = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawLine(activeIn, start = Offset(0f, cy - padding), end = Offset(cx - padding * 0.8f, cy - padding), strokeWidth = 2.5f)
                    drawLine(activeIn, start = Offset(0f, cy + padding), end = Offset(cx - padding * 0.8f, cy + padding), strokeWidth = 2.5f)
                    drawLine(activeIn, start = Offset(cx + padding * 1.5f, cy), end = Offset(cellSize, cy), strokeWidth = 2.5f)
                    
                    // Dual-in-line curved flat-back MIL gate body shape
                    val andPath = getReusablePath()
                    andPath.moveTo(cx - padding * 1.3f, cy - padding * 1.4f)
                    andPath.lineTo(cx, cy - padding * 1.4f)
                    andPath.arcTo(androidx.compose.ui.geometry.Rect(cx - padding * 1.4f, cy - padding * 1.4f, cx + padding * 1.4f, cy + padding * 1.4f), -90f, 180f, false)
                    andPath.lineTo(cx - padding * 1.3f, cy + padding * 1.4f)
                    andPath.close()
                    
                    // Body shading (deep futuristic background with bright neon border lines)
                    drawPath(andPath, Color(0xFF1E1E24))
                    drawPath(andPath, activeIn, style = Stroke(strokeSize * 0.6f))
                    
                    // Inside mathematical/programming AND symbol (&)
                    val symPath = getReusablePath()
                    symPath.moveTo(cx - 3f, cy + 5f)
                    symPath.lineTo(cx + 3f, cy - 5f)
                    symPath.lineTo(cx - 2f, cy - 2f)
                    symPath.lineTo(cx + 2f, cy + 3f)
                    drawPath(symPath, activeIn, style = Stroke(1.5f))
                    
                    // Gate junction status led
                    drawCircle(activeIn, radius = 2.5f, center = Offset(cx, cy))
                }

                ComponentType.LOGIC_OR -> {
                    // Logic signal lines
                    val activeIn = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawLine(activeIn, start = Offset(0f, cy - padding), end = Offset(cx - padding * 0.8f, cy - padding), strokeWidth = 2.5f)
                    drawLine(activeIn, start = Offset(0f, cy + padding), end = Offset(cx - padding * 0.8f, cy + padding), strokeWidth = 2.5f)
                    drawLine(activeIn, start = Offset(cx + padding * 1.5f, cy), end = Offset(cellSize, cy), strokeWidth = 2.5f)
                    
                    // Curved Pointy MIL OR-gate shape body
                    val orPath = getReusablePath()
                    orPath.moveTo(cx - padding * 1.5f, cy - padding * 1.4f)
                    orPath.quadraticTo(cx - padding * 0.5f, cy - padding * 1.4f, cx + padding * 1.5f, cy)
                    orPath.quadraticTo(cx - padding * 0.5f, cy + padding * 1.4f, cx - padding * 1.5f, cy + padding * 1.4f)
                    orPath.quadraticTo(cx - padding * 0.8f, cy, cx - padding * 1.5f, cy - padding * 1.4f)
                    orPath.close()
                    
                    drawPath(orPath, Color(0xFF1E1E24))
                    drawPath(orPath, activeIn, style = Stroke(strokeSize * 0.6f))
                    
                    // Inside programming OR gate symbol (>=1) or wedge
                    drawCircle(activeIn, radius = 2.5f, center = Offset(cx - 2f, cy))
                }

                ComponentType.LOGIC_NOT -> {
                    val activeIn = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawLine(activeIn, start = Offset(0f, cy), end = Offset(cx - padding, cy), strokeWidth = 2.5f)
                    drawLine(activeIn, start = Offset(cx + padding + strokeSize * 4, cy), end = Offset(cellSize, cy), strokeWidth = 2.5f)
                    
                    val p = getReusablePath()
                    p.moveTo(cx - padding, cy - padding)
                    p.lineTo(cx + padding, cy)
                    p.lineTo(cx - padding, cy + padding)
                    p.close()
                    drawPath(p, Color(0xFF1E1E24))
                    drawPath(p, activeIn, style = Stroke(strokeSize))
                    
                    drawCircle(Color(0xFF2B2930), radius = strokeSize * 2, center = Offset(cx + padding + strokeSize * 2, cy))
                    drawCircle(activeIn, radius = strokeSize * 2, center = Offset(cx + padding + strokeSize * 2, cy), style = Stroke(strokeSize))
                }

                ComponentType.RELAY -> {
                    // Outer square box shell
                    val bodyS = cellSize - padding * 2.4f
                    val rx = cx - bodyS / 2
                    val ry = cy - bodyS / 2
                    
                    // Translucent electric housing (translucent blue-shined grey or amber)
                    drawRoundRect(
                        color = Color(0xCC37474F), // Translucent ABS shell
                        topLeft = Offset(rx, ry),
                        size = Size(bodyS, bodyS),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFFA1887F), // Case borders
                        topLeft = Offset(rx, ry),
                        size = Size(bodyS, bodyS),
                        style = Stroke(strokeSize * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    
                    // Electromagnetic control copper coil cylinder on left
                    val coilW = bodyS * 0.4f
                    val coilY = ry + bodyS * 0.15f
                    val coilH = bodyS * 0.7f
                    drawRect(
                        color = Color(0xFFD84315), 
                        topLeft = Offset(rx + bodyS * 0.12f, coilY),
                        size = Size(coilW, coilH)
                    )
                    // Coil iron plunger pin in center
                    drawRect(Color(0xFFCFD8DC), topLeft = Offset(rx + bodyS * 0.28f, coilY), size = Size(4f, coilH))
                    
                    // Coil texture lines
                    for (i in 1..4) {
                        val wireY = coilY + i * coilH / 5f
                        drawLine(Color(0xFFE64A19), start = Offset(rx + bodyS * 0.12f, wireY), end = Offset(rx + bodyS * 0.12f + coilW, wireY), strokeWidth = 1f)
                    }
                    
                    // Contact Armature switch paddle on right side
                    val armX = rx + bodyS * 0.72f
                    // Static terminal pins
                    drawLine(Color(0xFFCFD8DC), start = Offset(armX - 6f, ry + bodyS * 0.2f), end = Offset(armX + 6f, ry + bodyS * 0.2f), strokeWidth = 2f)
                    drawLine(Color(0xFFCFD8DC), start = Offset(armX - 6f, ry + bodyS * 0.8f), end = Offset(armX + 6f, ry + bodyS * 0.8f), strokeWidth = 2f)
                    
                    // Dynamic armature paddle pivoting between pins
                    val armEndY = if (component.isPowered) (ry + bodyS * 0.2f) else (ry + bodyS * 0.8f)
                    drawLine(
                        color = Color(0xFFFFD54F),
                        start = Offset(armX - 10f, cy),
                        end = Offset(armX, armEndY),
                        strokeWidth = strokeSize * 0.5f
                    )
                    
                    // Solder joints representation at bottom
                    drawCircle(Color(0xFFECEFF1), radius = 2f, center = Offset(rx + bodyS * 0.2f, ry + bodyS - 1f))
                    drawCircle(Color(0xFFECEFF1), radius = 2f, center = Offset(rx + bodyS * 0.4f, ry + bodyS - 1f))
                    drawCircle(Color(0xFFECEFF1), radius = 2f, center = Offset(rx + bodyS * 0.8f, ry + bodyS - 1f))
                    
                    // Indication LED
                    val activeLed = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFF424242)
                    drawCircle(activeLed, radius = 3f, center = Offset(cx, ry + 6f))
                }

                ComponentType.TRANSISTOR -> {
                    // Emitter, Base, Collector package pin leads coming out the bottom
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx - padding * 0.7f, cy), end = Offset(cx - padding * 0.7f, cellSize), strokeWidth = 2f)
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 2f)
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx + padding * 0.7f, cy), end = Offset(cx + padding * 0.7f, cellSize), strokeWidth = 2f)
                    
                    // TO-92 semi-circle flat face package body
                    val transPath = getReusablePath()
                    transPath.moveTo(cx - padding * 1.2f, cy + padding * 0.6f)
                    transPath.lineTo(cx - padding * 1.2f, cy - padding * 0.6f)
                    transPath.quadraticTo(cx, cy - padding * 1.4f, cx + padding * 1.2f, cy - padding * 0.6f)
                    transPath.lineTo(cx + padding * 1.2f, cy + padding * 0.6f)
                    transPath.close()
                    drawPath(transPath, Color(0xFF212121))
                    
                    // Flat face highlights to give depth
                    drawRect(Color(0xFF424242), topLeft = Offset(cx - padding * 1.0f, cy + padding * 0.3f), size = Size(padding * 2.0f, 3f))
                    
                    // Base junction schematic diagram over the package body
                    val schemeCol = if (component.isPowered) Color(0xFF00FFCC) else Color(0x77FFFFFF)
                    drawLine(schemeCol, start = Offset(cx - padding * 0.6f, cy - padding * 0.2f), end = Offset(cx - padding * 0.6f, cy + padding * 0.2f), strokeWidth = 2.0f) 
                    drawLine(schemeCol, start = Offset(cx, cy), end = Offset(cx - padding * 0.6f, cy), strokeWidth = 1.0f) 
                    drawLine(schemeCol, start = Offset(cx - padding * 0.6f, cy - padding * 0.1f), end = Offset(cx + padding * 0.6f, cy - padding * 0.5f), strokeWidth = 1.5f) 
                    drawLine(schemeCol, start = Offset(cx - padding * 0.6f, cy + padding * 0.1f), end = Offset(cx + padding * 0.6f, cy + padding * 0.5f), strokeWidth = 1.5f) 
                    
                    if (component.isPowered) {
                        drawCircle(Color(0xFF00FFCC), radius = 2.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.MOSFET -> {
                    // Pins emerging
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx - padding * 0.8f, cy), end = Offset(cx - padding * 0.8f, cellSize), strokeWidth = 2.5f)
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 2.5f)
                    drawLine(Color(0xFFB0BEC5), start = Offset(cx + padding * 0.8f, cy), end = Offset(cx + padding * 0.8f, cellSize), strokeWidth = 2.5f)
                    
                    // Silver metallic mounting tab on top of BJT package
                    val tabW = padding * 2.1f
                    val tabH = padding * 1.2f
                    drawRect(
                        color = Color(0xFFCFD8DC),
                        topLeft = Offset(cx - tabW / 2, padding * 1.2f),
                        size = Size(tabW, tabH)
                    )
                    // Screwhole notch on back of heat sink
                    drawCircle(Color(0xFF37474F), radius = 3.5f, center = Offset(cx, padding * 1.8f))
                    
                    // Heavy plastic main power transistor casing
                    drawRoundRect(
                        color = Color(0xFF212121),
                        topLeft = Offset(cx - padding * 1.2f, padding * 2.2f),
                        size = Size(padding * 2.4f, cellSize - padding * 4.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // High current gate channel conduction glow when active
                    if (component.isPowered) {
                        drawRect(
                            color = Color(0x3300FFCC),
                            topLeft = Offset(cx - padding * 1.2f, padding * 2.2f),
                            size = Size(padding * 2.4f, cellSize - padding * 4.2f)
                        )
                        drawCircle(Color(0xFF00FFCC), radius = 3.0f, center = Offset(cx, cy))
                    }
                }

                ComponentType.RGB_LED -> {
                    val r = if (component.isPowered) 1f else 0.3f
                    val g = if (component.logicState) 1f else 0.3f
                    val ledColor = Color(r, g, 1f) 
                    
                    if (component.isPowered) drawCircle(ledColor.copy(alpha = 0.5f), radius = cellSize * 0.45f, center = Offset(cx, cy))
                    drawCircle(ledColor, radius = cellSize * 0.35f, center = Offset(cx, cy))
                    
                    drawLine(color, start = Offset(cx - padding, cellSize * 0.85f), end = Offset(cx - padding, cellSize), strokeWidth = strokeSize * 0.5f)
                    drawLine(color, start = Offset(cx - padding * 0.33f, cellSize * 0.85f), end = Offset(cx - padding * 0.33f, cellSize), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx + padding * 0.33f, cellSize * 0.85f), end = Offset(cx + padding * 0.33f, cellSize), strokeWidth = strokeSize * 0.5f)
                    drawLine(color, start = Offset(cx + padding, cellSize * 0.85f), end = Offset(cx + padding, cellSize), strokeWidth = strokeSize * 0.5f)
                }

                ComponentType.SEVEN_SEGMENT -> {
                    drawRect(Color(0xFF2B2930), topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2))
                    val lit = if (component.isPowered) Color(0xFFFF3366) else Color(0xFF4A1020)
                    
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
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2), style = Stroke(strokeSize))
                    val p = getReusablePath()
                    p.moveTo(cx - padding * 0.8f, cy)
                    p.lineTo(cx - padding * 0.4f, cy)
                    p.lineTo(cx - padding * 0.4f, cy - padding * 0.8f)
                    p.lineTo(cx + padding * 0.4f, cy - padding * 0.8f)
                    p.lineTo(cx + padding * 0.4f, cy)
                    p.lineTo(cx + padding * 0.8f, cy)
                    drawPath(p, if (component.logicState) Color(0xFF00FFCC) else color, style = Stroke(strokeSize))
                }

                ComponentType.BATTERY_PACK -> {
                    drawRect(Color(0xFF381E72), topLeft = Offset(padding, padding * 1.5f), size = Size(cellSize - padding * 2, cellSize - padding * 3))
                    val maxCap = getMaxCap(component)
                    if (component.charge in 0f..maxCap) {
                        val pct = (component.charge / maxCap).coerceIn(0f, 1f)
                        drawRect(Color(0xFF4CAF50), topLeft = Offset(padding + strokeSize, padding * 1.5f + strokeSize + (cellSize - padding * 3 - strokeSize * 2) * (1f - pct)), size = Size(cellSize - padding * 2 - strokeSize * 2, (cellSize - padding * 3 - strokeSize * 2) * pct))
                    }
                    drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.25f, padding), size = Size(cellSize * 0.15f, padding * 0.5f))
                    drawRect(Color(0xFF4F378B), topLeft = Offset(cellSize * 0.6f, padding), size = Size(cellSize * 0.15f, padding * 0.5f))
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx, cy - padding), end = Offset(cx, cy + padding), strokeWidth=strokeSize*0.5f)
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding * 1.2f, cy), end = Offset(cx + padding * 1.2f, cy), strokeWidth=strokeSize*0.5f)
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx - padding * 1.2f, cy - padding), end = Offset(cx - padding * 1.2f, cy + padding), strokeWidth=strokeSize)
                    drawLine(Color(0xFFD0BCFF), start = Offset(cx + padding * 1.2f, cy - padding), end = Offset(cx + padding * 1.2f, cy + padding), strokeWidth=strokeSize)
                }
                
                ComponentType.INDUCTOR -> {
                    drawLine(color, start = Offset(cx, 0f), end = Offset(cx, padding), strokeWidth = strokeSize)
                    drawLine(color, start = Offset(cx, cellSize - padding), end = Offset(cx, cellSize), strokeWidth = strokeSize)
                    for (i in 0..3) {
                        drawArc(color, -90f, 180f, false, topLeft = Offset(cx - padding, padding + i * (cellSize - padding * 2) / 4), size = Size(padding * 2, (cellSize - padding * 2) / 4), style = Stroke(strokeSize))
                    }
                }
                
                ComponentType.DIP_SWITCH -> {
                    drawRect(Color(0xFFC62828), topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2))
                    for (i in 0..3) {
                        val yOffset = padding * 1.5f + i * (cellSize - padding * 3) / 3
                        drawRect(Color(0xFF1C1B1F), topLeft = Offset(padding * 1.5f, yOffset), size = Size(padding * 3f, strokeSize * 1.5f))
                        val pegX = if (component.logicState) padding * 1.5f else padding * 3.5f 
                        drawRect(Color(0xFFFFF7FA), topLeft = Offset(pegX, yOffset), size = Size(padding, strokeSize * 1.5f))
                    }
                }

                ComponentType.TIMER_555 -> {
                    drawRect(
                        brush = icBrush,
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2), style = Stroke(strokeSize))
                    drawArc(color, 90f, 180f, false, topLeft = Offset(cx - padding * 0.5f, padding - padding * 0.5f), size = Size(padding, padding), style = Stroke(strokeSize * 0.5f))
                    for (i in 1..4) {
                       val yOff = padding + i * (cellSize - padding * 2) / 5
                       drawLine(Color(0xFF9E9E9E), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 4f)
                       drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 4f)
                    }
                    drawCircle(Color(0xFF00FFCC), radius = strokeSize, center = Offset(cx, cy))
                }

                ComponentType.MICROCONTROLLER -> {
                    drawRect(
                        brush = icBrush,
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2), style = Stroke(strokeSize))
                    
                    for (i in 1..3) {
                        drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i * padding * 0.5f, 0f), end = Offset(cx - padding + i * padding * 0.5f, padding), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(cx - padding + i * padding * 0.5f, cellSize), end = Offset(cx - padding + i * padding * 0.5f, cellSize - padding), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(0f, cy - padding + i * padding * 0.5f), end = Offset(padding, cy - padding + i * padding * 0.5f), strokeWidth = 4f)
                        drawLine(Color(0xFF9E9E9E), start = Offset(cellSize, cy - padding + i * padding * 0.5f), end = Offset(cellSize - padding, cy - padding + i * padding * 0.5f), strokeWidth = 4f)
                    }
                    
                    if (component.extraData.isNotEmpty()) {
                        drawCircle(Color(0xFF00FFCC), radius = strokeSize, center = Offset(cx, cy))
                    }
                }

                ComponentType.MEMORY_RAM, ComponentType.MEMORY_ROM -> {
                    drawRect(
                        brush = ramBrush,
                        topLeft = Offset(padding, padding * 1.5f), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 3)
                    )
                    drawRect(color, topLeft = Offset(padding, padding * 1.5f), size = Size(cellSize - padding * 2, cellSize - padding * 3), style = Stroke(strokeSize))
                    
                    for (i in -1..1) {
                        drawRect(Color(0xFF0F1E14), topLeft = Offset(cx + i * (padding * 0.8f) - padding * 0.3f, cy - padding * 0.8f), size = Size(padding * 0.6f, padding * 1.6f))
                    }
                    
                    for (i in 1..4) {
                       drawLine(Color(0xFFD3A82C), start = Offset(padding + i * (cellSize - padding * 2) / 5, cellSize - padding * 1.5f), end = Offset(padding + i * (cellSize - padding * 2) / 5, cellSize), strokeWidth = strokeSize)
                    }
                    if (component.type == ComponentType.MEMORY_RAM) {
                        drawCircle(Color(0xFF4CAF50), radius = strokeSize, center = Offset(cx, cy))
                    } else {
                        drawCircle(Color(0xFF607D8B), radius = strokeSize, center = Offset(cx, cy))
                    }
                }
                
                ComponentType.MONITOR_OLED, ComponentType.CRT_MONITOR -> {
                    drawRect(Color(0xFF111111), topLeft = Offset(padding * 0.5f, padding), size = Size(cellSize - padding, cellSize - padding * 2.5f))
                    drawRect(Color(0xFF49454F), topLeft = Offset(padding * 0.5f, padding), size = Size(cellSize - padding, cellSize - padding * 2.5f), style = Stroke(strokeSize))
                    drawRect(Color(0xFF666666), topLeft = Offset(cx - padding * 0.5f, cellSize - padding * 1.5f), size = Size(padding, padding * 1.5f))
                    
                    if (component.isPowered) {
                        val txt = component.extraData.substringAfter("display=").substringBefore("|").ifEmpty { "PC_OK" }
                        drawRect(Color(0xFF00FF00), topLeft = Offset(padding * 1.5f, padding * 2f), size = Size((cellSize - padding * 3) * (if (txt.length % 2 == 0) 0.5f else 0.8f), padding * 0.8f))
                        if (txt.length > 3) {
                            drawRect(Color(0xFF00FF00), topLeft = Offset(padding * 1.5f, padding * 3f), size = Size((cellSize - padding * 3) * 0.4f, padding * 0.8f))
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
                        brush = icBrush,
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
                        brush = icBrush,
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

                ComponentType.PISTON -> {
                    val dir = component.direction
                    drawRect(Color(0xFF37474F), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2))
                    drawRect(Color(0xFF1A237E), topLeft = Offset(padding, padding), size = Size(cellSize - padding*2, cellSize - padding*2), style = Stroke(strokeSize*0.5f))
                    
                    val shaftCol = Color(0xFFCFD8DC)
                    val plateCol = Color(0xFF8D6E63)
                    val pushDist = if (component.isPowered || component.logicState) cellSize * 0.4f else 0f
                    
                    when (dir) {
                        Direction.UP -> {
                            drawRect(shaftCol, topLeft = Offset(cx - strokeSize, padding - pushDist), size = Size(strokeSize*2f, pushDist + padding))
                            drawRect(plateCol, topLeft = Offset(cx - padding*1.4f, padding - pushDist - strokeSize), size = Size(padding*2.8f, strokeSize))
                        }
                        Direction.DOWN -> {
                            drawRect(shaftCol, topLeft = Offset(cx - strokeSize, cy), size = Size(strokeSize*2f, pushDist + padding))
                            drawRect(plateCol, topLeft = Offset(cx - padding*1.4f, cellSize - padding + pushDist), size = Size(padding*2.8f, strokeSize))
                        }
                        Direction.LEFT -> {
                            drawRect(shaftCol, topLeft = Offset(padding - pushDist, cy - strokeSize), size = Size(pushDist + padding, strokeSize*2f))
                            drawRect(plateCol, topLeft = Offset(padding - pushDist - strokeSize, cy - padding*1.4f), size = Size(strokeSize, padding*2.8f))
                        }
                        Direction.RIGHT -> {
                            drawRect(shaftCol, topLeft = Offset(cx, cy - strokeSize), size = Size(pushDist + padding, strokeSize*2f))
                            drawRect(plateCol, topLeft = Offset(cellSize - padding + pushDist, cy - padding*1.4f), size = Size(strokeSize, padding*2.8f))
                        }
                    }
                    val ledCol = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawCircle(ledCol, radius = 3f, center = Offset(cx, cy))
                }

                ComponentType.DOUBLE_DOOR -> {
                    val isOpened = component.isPowered
                    val dir = component.direction
                    
                    drawRect(Color(0xFF212121), topLeft = Offset(padding*0.6f, padding*0.6f), size = Size(cellSize - padding*1.2f, cellSize - padding*1.2f))
                    drawRect(Color(0xFFB0BEC5), topLeft = Offset(padding*0.6f, padding*0.6f), size = Size(cellSize - padding*1.2f, cellSize - padding*1.2f), style = Stroke(strokeSize*0.6f))
                    
                    val lockCol = if (isOpened) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    val slideLen = if (isOpened) cellSize * 0.35f else 0f
                    
                    when (dir) {
                        Direction.LEFT, Direction.RIGHT -> {
                            drawRect(
                                Color(0xFF455A64), 
                                topLeft = Offset(padding * 0.8f, padding * 0.8f - slideLen), 
                                size = Size(cellSize - padding * 1.6f, cy - padding * 0.8f)
                            )
                            drawRect(
                                Color(0xFF455A64), 
                                topLeft = Offset(padding * 0.8f, cy + slideLen), 
                                size = Size(cellSize - padding * 1.6f, cy - padding * 0.8f)
                            )
                            if (!isOpened) {
                                for (i in 0..3) {
                                    val ly = padding * 1.2f + i * (cellSize - padding * 2.4f) / 3
                                    drawLine(Color(0xFFFFD54F), start = Offset(padding * 1.2f, ly), end = Offset(cellSize - padding * 1.2f, ly + 2f), strokeWidth = 2f)
                                }
                            }
                            drawLine(lockCol, start = Offset(cx, padding), end = Offset(cx, cellSize - padding), strokeWidth = 3f)
                        }
                        Direction.UP, Direction.DOWN -> {
                            drawRect(
                                Color(0xFF455A64), 
                                topLeft = Offset(padding * 0.8f - slideLen, padding * 0.8f), 
                                size = Size(cx - padding * 0.8f, cellSize - padding * 1.6f)
                            )
                            drawRect(
                                Color(0xFF455A64), 
                                topLeft = Offset(cx + slideLen, padding * 0.8f), 
                                size = Size(cx - padding * 0.8f, cellSize - padding * 1.6f)
                            )
                            if (!isOpened) {
                                for (i in 0..3) {
                                    val lx = padding * 1.2f + i * (cellSize - padding * 2.4f) / 3
                                    drawLine(Color(0xFFFFD54F), start = Offset(lx, padding * 1.2f), end = Offset(lx + 2f, cellSize - padding * 1.2f), strokeWidth = 2f)
                                }
                            }
                            drawLine(lockCol, start = Offset(padding, cy), end = Offset(cellSize - padding, cy), strokeWidth = 3f)
                        }
                    }
                    drawCircle(lockCol, radius = 2.5f, center = Offset(cx, cy))
                }

                else -> {
                    if (type.category == ComponentCategory.MATERIALS || type.category == ComponentCategory.HYDRAULICS) {
                        val matColor = when (type) {
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
                            ComponentType.URANIUM -> {
                                val temp = component.temperature
                                if (temp <= 100f) {
                                    Color(0xAA76FF03)
                                } else if (temp < 600f) {
                                    val r = (((temp - 100f) / 500f) * 255f).toInt().coerceIn(0, 255)
                                    val g = (255 - (((temp - 100f) / 500f) * 150f)).toInt().coerceIn(0, 255)
                                    Color(r, g, 3, 230)
                                } else if (temp < 1000f) {
                                    val green = ((((temp - 600f) / 400f) * 150f) + 105f).toInt().coerceIn(105, 255)
                                    val blue = (((temp - 600f) / 400f) * 255f).toInt().coerceIn(0, 255)
                                    Color(255, green, blue, 255)
                                } else {
                                    // Melted flowing corium/elephant's foot mixture - pulsating high temperature glow
                                    val pulse = (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5).toFloat()
                                    val r = (200 + (pulse * 55)).toInt().coerceIn(0, 255)
                                    val g = (30 + (pulse * 100)).toInt().coerceIn(0, 255)
                                    val b = (10 + (pulse * 30)).toInt().coerceIn(0, 255)
                                    Color(r, g, b, 255)
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
                            ComponentType.BRICK -> Color(0xFFD32F2F)
                            ComponentType.OBSIDIAN -> Color(0xFF1C1C1C)
                            ComponentType.BEDROCK -> Color(0xFF000000)
                            else -> Color.Transparent
                        }
                        drawRect(matColor, size = Size(cellSize, cellSize))
                        
                        if (type == ComponentType.BRICK) {
                            drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize * 0.33f), end = Offset(cellSize, cellSize * 0.33f), strokeWidth = 2f)
                            drawLine(Color(0xFFB71C1C), start = Offset(0f, cellSize * 0.66f), end = Offset(cellSize, cellSize * 0.66f), strokeWidth = 2f)
                        } else if (type in listOf(ComponentType.STEEL, ComponentType.COPPER, ComponentType.ALUMINUM, ComponentType.GOLD)) {
                            drawRect(metalShineBrush, size = Size(cellSize, cellSize))
                        } else if (type == ComponentType.DIAMOND) {
                            drawLine(Color.White, start = Offset(cellSize * 0.2f, 0f), end = Offset(cellSize, cellSize * 0.8f), strokeWidth = 1.5f)
                        }
                        if (type == ComponentType.INFINITE_WATER || type == ComponentType.INFINITE_LAVA) {
                            drawRect(Color.White, topLeft = Offset(padding * 2, padding * 2), size = Size(cellSize - padding * 4, cellSize - padding * 4))
                        }
                        if (type == ComponentType.VOID_HOLE || type == ComponentType.FLUID_DRAIN) {
                            drawCircle(Color.Red, radius = strokeSize * 0.5f, center = Offset(cx, cy))
                        }
                        return@apply
                    }
                    
                    val name = type.name
                    val pinCount = when {
                        name == "MICROCONTROLLER" -> 6
                        name.contains("555") -> 4
                        name.contains("OPAMP") || name.contains("358") || name.contains("317") || name.contains("LM") -> 4
                        name.contains("74") || name.contains("4017") || name.contains("RAM") || name.contains("ROM") -> 7
                        else -> 5
                    }
                    
                    drawRect(
                        brush = icBrush,
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 2)
                    )
                    drawRect(color, topLeft = Offset(padding, padding), size = Size(cellSize - padding * 2, cellSize - padding * 2), style = Stroke(strokeSize))
                    
                    if (type == ComponentType.MICROCONTROLLER) {
                        for (i in 0 until pinCount) {
                            val offset = padding + (i + 0.5f) * (cellSize - padding * 2) / pinCount
                            drawLine(Color(0xFFB0BEC5), start = Offset(0f, offset), end = Offset(padding, offset), strokeWidth = 3f)
                            drawLine(Color(0xFFB0BEC5), start = Offset(cellSize, offset), end = Offset(cellSize - padding, offset), strokeWidth = 3f)
                            drawLine(Color(0xFFB0BEC5), start = Offset(offset, 0f), end = Offset(offset, padding), strokeWidth = 3f)
                            drawLine(Color(0xFFB0BEC5), start = Offset(offset, cellSize), end = Offset(offset, cellSize - padding), strokeWidth = 3f)
                        }
                    } else {
                        for (i in 0 until pinCount) {
                            val yOff = padding + (i + 0.5f) * (cellSize - padding * 2) / pinCount
                            drawLine(Color(0xFFB0BEC5), start = Offset(0f, yOff), end = Offset(padding, yOff), strokeWidth = 3f)
                            drawLine(Color(0xFFB0BEC5), start = Offset(cellSize, yOff), end = Offset(cellSize - padding, yOff), strokeWidth = 3f)
                        }
                    }
                    
                    when {
                        name == "MICROCONTROLLER" -> {
                            drawRect(Color(0xFFFFD54F), topLeft = Offset(cx - padding*0.8f, cy - padding*0.8f), size = Size(padding*1.6f, padding*1.6f))
                            drawRect(Color(0xFF424242), topLeft = Offset(cx - padding*0.5f, cy - padding*0.5f), size = Size(padding, padding))
                            drawCircle(if(component.isPowered) Color(0xFF00FFCC) else Color(0xFFB0BEC5), radius = 2.5f, center = Offset(cx, cy))
                        }
                        name.contains("RAM") || name.contains("ROM") -> {
                            val memCol = if (name.contains("RAM")) Color(0xFF00E5FF) else Color(0xFFFF9100)
                            drawRect(memCol.copy(alpha = 0.25f), topLeft = Offset(cx - padding, cy - padding), size = Size(padding*2, padding*2))
                            for (i in 0..1) {
                                val lx = cx - padding + (i + 1f) * padding * 2 / 3
                                drawLine(memCol, start = Offset(lx, cy - padding), end = Offset(lx, cy + padding), strokeWidth = 1.5f)
                                drawLine(memCol, start = Offset(cx - padding, lx), end = Offset(cx + padding, lx), strokeWidth = 1.5f)
                            }
                        }
                        name.contains("OP_AMP") || name.contains("OPAMP") || name.contains("358") || name.contains("324") || name.contains("AMP") -> {
                            val ampPath = Path().apply {
                                moveTo(cx - padding * 0.6f, cy - padding * 0.6f)
                                lineTo(cx + padding * 0.6f, cy)
                                lineTo(cx - padding * 0.6f, cy + padding * 0.6f)
                                close()
                            }
                            drawPath(ampPath, color = Color(0xFF9C27B0), style = Stroke(1.5f))
                            drawLine(Color.White.copy(alpha = 0.7f), start = Offset(cx - padding*0.4f, cy - padding*0.3f), end = Offset(cx - padding*0.2f, cy - padding*0.3f), strokeWidth = 1f)
                            drawLine(Color.White.copy(alpha = 0.7f), start = Offset(cx - padding*0.4f, cy + padding*0.3f), end = Offset(cx - padding*0.2f, cy + padding*0.3f), strokeWidth = 1f)
                        }
                        name.contains("LOGIC") || name.contains("74") || name.contains("4017") -> {
                            drawRoundRect(Color(0xFFE91E63), topLeft = Offset(cx - padding*0.7f, cy - padding*0.5f), size = Size(padding*1.4f, padding*1.0f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = Stroke(1.5f))
                            drawLine(Color(0xFFE91E63), start = Offset(cx - padding*1.0f, cy - padding*0.2f), end = Offset(cx - padding*0.7f, cy - padding*0.2f), strokeWidth = 1f)
                            drawLine(Color(0xFFE91E63), start = Offset(cx - padding*1.0f, cy + padding*0.2f), end = Offset(cx - padding*0.7f, cy + padding*0.2f), strokeWidth = 1f)
                            drawLine(Color(0xFFE91E63), start = Offset(cx + padding*0.7f, cy), end = Offset(cx + padding*1.0f, cy), strokeWidth = 1f)
                        }
                        name.contains("555") || name.contains("TIMER") -> {
                            val wavePath = Path().apply {
                                moveTo(cx - padding*0.8f, cy + padding*0.3f)
                                lineTo(cx - padding*0.4f, cy + padding*0.3f)
                                lineTo(cx - padding*0.4f, cy - padding*0.3f)
                                lineTo(cx, cy - padding*0.3f)
                                lineTo(cx, cy + padding*0.3f)
                                lineTo(cx + padding*0.4f, cy + padding*0.3f)
                                lineTo(cx + padding*0.4f, cy - padding*0.3f)
                                lineTo(cx + padding*0.8f, cy - padding*0.3f)
                            }
                            drawPath(wavePath, color = Color(0xFFFFCC80), style = Stroke(2.0f))
                        }
                        name.contains("ADC") || name.contains("DAC") || name.contains("AMPLIFIER") -> {
                            drawLine(Color(0xFF00BCD4), start = Offset(cx - padding * 0.7f, cy + padding * 0.7f), end = Offset(cx + padding * 0.7f, cy - padding * 0.7f), strokeWidth = 1.5f)
                            drawCircle(Color(0xFF00BCD4), radius = 2f, center = Offset(cx - padding * 0.5f, cy + padding * 0.5f))
                            drawCircle(Color(0xFFE91E63), radius = 2f, center = Offset(cx + padding * 0.5f, cy - padding * 0.5f))
                        }
                        else -> {
                            val symColor = when (type.category) {
                                ComponentCategory.LOGIC -> Color(0xFFE91E63) 
                                ComponentCategory.SENSORS -> Color(0xFF00BCD4)
                                ComponentCategory.OUTPUTS -> Color(0xFFFF9800)
                                ComponentCategory.ANALOG_ICS -> Color(0xFF9C27B0)
                                ComponentCategory.ADVANCED -> Color(0xFF3F51B5)
                                ComponentCategory.SWITCHES -> Color(0xFF4CAF50)
                                ComponentCategory.POWER -> Color(0xFFFFEB3B)
                                else -> Color.Gray
                            }
                            drawCircle(symColor, radius = strokeSize * 1.5f, center = Offset(cx, cy))
                        }
                    }
                    
                    val hash = type.name.hashCode()
                    val r = (hash and 0xFF).toFloat() / 255f
                    val g = ((hash shr 8) and 0xFF).toFloat() / 255f
                    val b = ((hash shr 16) and 0xFF).toFloat() / 255f
                    val ledCol = if (component.isPowered) Color(0xFF00FFCC) else Color(r, g, b)
                    drawCircle(ledCol, radius = strokeSize * 0.8f, center = Offset(cx, cy - strokeSize * 2))
                }
            }
        }
    }
}
