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
            ComponentType.COIN_CELL -> com.example.functional.Battery.DEFAULT_COIN_CELL_CAPACITY 
            ComponentType.BATTERY_PACK -> com.example.functional.Battery.DEFAULT_BATTERY_PACK_CAPACITY 
            ComponentType.INFINITE_BATTERY -> 9999999f 
            ComponentType.NUCLEAR_REACTOR -> 1000000f 
            else -> com.example.functional.Battery.DEFAULT_BATTERY_CAPACITY 
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

    private fun isFluidType(type: ComponentType): Boolean {
        return type == ComponentType.WATER || type == ComponentType.INFINITE_WATER ||
               type == ComponentType.LAVA || type == ComponentType.INFINITE_LAVA ||
               type == ComponentType.OIL || type == ComponentType.INFINITE_OIL ||
               type == ComponentType.ACID || type == ComponentType.INFINITE_ACID ||
               type == ComponentType.SLIME || type == ComponentType.INFINITE_SLIME ||
               type == ComponentType.GASOLINE || type == ComponentType.INFINITE_GASOLINE ||
               type == ComponentType.LIQUID_NITROGEN || type == ComponentType.INFINITE_LIQUID_NITROGEN
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
                            ComponentType.WATER, ComponentType.INFINITE_WATER -> Color(android.graphics.Color.parseColor(com.example.engine.JavaModEngine.waterColorHex))
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
                            ComponentType.BRICK -> Color(android.graphics.Color.parseColor(com.example.engine.JavaModEngine.brickColorHex))
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
                val isFluid = isFluidType(type)

                if (isFluid) {
                    val rx = cellSize / 2f
                    val ry = cellSize / 2f
                    val upSame = y > 0 && isFluidType(grid[x][y-1].type)
                    val downSame = y < height - 1 && isFluidType(grid[x][y+1].type)
                    val leftSame = x > 0 && isFluidType(grid[x-1][y].type)
                    val rightSame = x < width - 1 && isFluidType(grid[x+1][y].type)

                    val bridgeWidth = cellSize * 0.74f
                    if (upSame) drawLine(flatColor, start = Offset(rx, ry), end = Offset(rx, 0f), strokeWidth = bridgeWidth)
                    if (downSame) drawLine(flatColor, start = Offset(rx, ry), end = Offset(rx, cellSize), strokeWidth = bridgeWidth)
                    if (leftSame) drawLine(flatColor, start = Offset(rx, ry), end = Offset(0f, ry), strokeWidth = bridgeWidth)
                    if (rightSame) drawLine(flatColor, start = Offset(rx, ry), end = Offset(cellSize, ry), strokeWidth = bridgeWidth)

                    drawCircle(flatColor, radius = cellSize * 0.36f, center = Offset(rx, ry))

                    val seed = (x * 17 + y * 31)
                    val drop1Size = cellSize * 0.14f
                    val drop2Size = cellSize * 0.09f

                    if (!downSame) {
                        val ox1 = ((seed % 7) - 3) / 20f * cellSize
                        val oy1 = ((seed % 9) - 1) / 15f * cellSize + (cellSize * 0.22f)
                        drawCircle(flatColor.copy(alpha = 0.9f), radius = drop1Size, center = Offset(rx + ox1, ry + oy1))
                    }
                    if (!upSame) {
                        val ox2 = (((seed + 3) % 9) - 4) / 25f * cellSize
                        val oy2 = -(((seed + 3) % 7) + 1) / 20f * cellSize - (cellSize * 0.16f)
                        drawCircle(flatColor.copy(alpha = 0.85f), radius = drop2Size, center = Offset(rx + ox2, ry + oy2))
                    }
                } else {
                    drawRect(flatColor, size = Size(cellSize, cellSize))
                }
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
                
                ComponentType.INFINITE_BATTERY -> {
                    // Outer battery heavy-duty body
                    drawRect(
                        color = Color(0xFF1E1B4B), // Majestic deep indigo
                        topLeft = Offset(padding, padding + strokeSize * 0.4f),
                        size = Size(cellSize - padding * 2, cellSize - (padding * 2 + strokeSize * 0.8f))
                    )
                    // Golden/Cyan metallic terminals
                    drawRect(Color(0xFFFFD700), topLeft = Offset(cellSize * 0.35f, padding), size = Size(cellSize * 0.3f, strokeSize * 0.4f))
                    drawRect(Color(0xFFFFD700), topLeft = Offset(cellSize * 0.35f, cellSize - padding - strokeSize * 0.4f), size = Size(cellSize * 0.3f, strokeSize * 0.4f))
                    
                    // High-contrast neon frame indicating infinite charge status
                    drawRect(
                        color = Color(0xFF00FFCC),
                        topLeft = Offset(padding, padding + strokeSize * 0.4f),
                        size = Size(cellSize - padding * 2, cellSize - (padding * 2 + strokeSize * 0.8f)),
                        style = Stroke(width = strokeSize * 0.3f)
                    )
                    
                    // Glowing Infinity symbol (∞) in the center using smooth overlapping terminal loops
                    val loopRadius = cellSize * 0.12f
                    val offsetDistance = cellSize * 0.11f
                    
                    // Draw Left Loop
                    drawCircle(
                        color = Color(0xFF00FFCC),
                        radius = loopRadius,
                        center = Offset(cx - offsetDistance, cy),
                        style = Stroke(width = strokeSize * 0.4f)
                    )
                    // Draw Right Loop
                    drawCircle(
                        color = Color(0xFF00FFCC),
                        radius = loopRadius,
                        center = Offset(cx + offsetDistance, cy),
                        style = Stroke(width = strokeSize * 0.4f)
                    )
                }

                ComponentType.NUCLEAR_REACTOR, ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, ComponentType.THERMOELECTRIC_GENERATOR -> {
                    drawRect(
                        brush = greenEnergyBrush,
                        topLeft = Offset(padding, padding), 
                        size = Size(cellSize - padding * 2, cellSize - padding * 2)
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
                    // Leads running under body
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    // Beautiful 3D cylindrical premium black molding
                    val dW = padding * 2.8f
                    val dH = cellSize - padding * 3.4f
                    val dX = cx - dW / 2
                    val dY = padding * 1.7f
                    drawRoundRect(
                        color = Color(0xFF1E262B),
                        topLeft = Offset(dX, dY),
                        size = Size(dW, dH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                    // Silver cathode band towards the top (-) pin
                    drawRect(
                        color = Color(0xFFCFD8DC),
                        topLeft = Offset(dX, dY),
                        size = Size(dW, strokeSize * 1.1f)
                    )
                    // Specular 3D highlight shine line
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(cx - dW * 0.3f, dY),
                        end = Offset(cx - dW * 0.3f, dY + dH),
                        strokeWidth = 2f
                    )
                    // High-fidelity internal arrow schematic
                    val arrowPath = getReusablePath()
                    arrowPath.moveTo(cx - padding * 0.9f, cy + padding * 0.6f)
                    arrowPath.lineTo(cx + padding * 0.9f, cy + padding * 0.6f)
                    arrowPath.lineTo(cx, cy - padding * 0.5f)
                    arrowPath.close()
                    drawPath(arrowPath, Color(0xBBFFFFFF))
                    drawLine(Color(0xBBFFFFFF), start = Offset(cx - padding * 0.9f, cy - padding * 0.5f), end = Offset(cx + padding * 0.9f, cy - padding * 0.5f), strokeWidth = 2.5f)

                    // Conducting forward bias emerald-green neon wave
                    if (component.isPowered) {
                        drawRoundRect(
                            color = Color(0x4400E676),
                            topLeft = Offset(dX, dY),
                            size = Size(dW, dH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        drawCircle(Color(0xFF00FFCC), radius = 3.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.ZENER_DIODE -> {
                    // Copper/Gold lead wires
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    // Glass semi-translucent glowing amber capsule
                    val zW = padding * 2.4f
                    val zH = cellSize - padding * 3.8f
                    val zX = cx - zW / 2
                    val zY = padding * 1.9f
                    drawRoundRect(
                        color = Color(0xFFFF5722),
                        topLeft = Offset(zX, zY),
                        size = Size(zW, zH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Pure shiny glass highlight 
                    drawRect(
                        color = Color(0x66FFFFFF),
                        topLeft = Offset(zX + 2f, zY),
                        size = Size(zW * 0.25f, zH)
                    )
                    // Thick black band on cathode (-) side
                    drawRect(
                        color = Color(0xFF151B1E),
                        topLeft = Offset(zX, zY),
                        size = Size(zW, strokeSize * 0.9f)
                    )
                    // Custom Zener Z-bent line
                    drawLine(Color(0xE0FFFFFF), start = Offset(cx - padding * 0.7f, cy - padding * 0.5f), end = Offset(cx + padding * 0.7f, cy - padding * 0.5f), strokeWidth = 1.8f)
                    drawLine(Color(0xE0FFFFFF), start = Offset(cx - padding * 0.7f, cy - padding * 0.5f), end = Offset(cx - padding * 0.7f, cy - padding * 0.2f), strokeWidth = 1.8f) 
                    drawLine(Color(0xE0FFFFFF), start = Offset(cx + padding * 0.7f, cy - padding * 0.5f), end = Offset(cx + padding * 0.7f, cy - padding * 0.8f), strokeWidth = 1.8f) 
                    
                    if (component.isPowered) {
                        drawCircle(Color(0xFFFFEA00), radius = 3.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.RESISTOR -> {
                    // Metal lead rods
                    drawLine(Color(0xFFECEFF1), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    
                    // Resistor ceramic dog-bone style body
                    val bodyTop = padding * 1.7f
                    val bodyHeight = cellSize - padding * 3.4f
                    val rx = cx - padding * 1.3f
                    val rw = padding * 2.6f
                    
                    // Premium light sand/beige ceramic body
                    drawRoundRect(
                        color = Color(0xFFEFEBE9),
                        topLeft = Offset(rx, bodyTop),
                        size = Size(rw, bodyHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    // Outer structural metal solder caps
                    drawRoundRect(color = Color(0xFFCFD8DC), topLeft = Offset(rx, bodyTop - 1f), size = Size(rw, strokeSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                    drawRoundRect(color = Color(0xFFCFD8DC), topLeft = Offset(rx, bodyTop + bodyHeight - strokeSize + 1f), size = Size(rw, strokeSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))

                    // Highly detailed color rings representing 10k resistor multiplier
                    // Ring 1: Brown (1)
                    drawRect(Color(0xFF6D4C41), topLeft = Offset(rx, bodyTop + bodyHeight * 0.22f), size = Size(rw, 4.5f))
                    // Ring 2: Black (0)
                    drawRect(Color(0xFF212121), topLeft = Offset(rx, bodyTop + bodyHeight * 0.42f), size = Size(rw, 4.5f))
                    // Ring 3: Orange (Power x1000)
                    drawRect(Color(0xFFFF9800), topLeft = Offset(rx, bodyTop + bodyHeight * 0.62f), size = Size(rw, 4.5f))
                    // Ring 4: Metallic Gold Tolerance
                    drawRect(Color(0xFFFFD54F), topLeft = Offset(rx, bodyTop + bodyHeight * 0.84f), size = Size(rw, 5f))
                    
                    // Neon electric pathway flow aura inside resistor
                    if (component.isPowered) {
                        drawRect(Color(0x3300FF99), topLeft = Offset(rx, bodyTop), size = Size(rw, bodyHeight))
                    }
                }

                ComponentType.CAPACITOR -> {
                    // Heavy terminal wire tracks
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    
                    // Electrolytic cylindrical battery/cooler-colored canister sleeve
                    val capT = padding * 1.4f
                    val capH = cellSize - padding * 2.8f
                    val capW = padding * 2.5f
                    val capX = cx - capW / 2
                    
                    drawRoundRect(
                        color = Color(0xFF0D47A1), // Deep premium navy-blue sleeve
                        topLeft = Offset(capX, capT),
                        size = Size(capW, capH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    
                    // Heavy grey composite socket gasket base
                    drawRect(Color(0xFF263238), topLeft = Offset(capX + 2f, capT + capH - 4f), size = Size(capW - 4f, 4f))

                    // Polarity band with negative (-) markers
                    val stripeW = capW * 0.32f
                    drawRect(
                        color = Color(0xFFF5F5F5),
                        topLeft = Offset(capX + capW - stripeW - 2f, capT),
                        size = Size(stripeW, capH)
                    )
                    // High-fidelity minus markers printed on stripe
                    for (i in 0..2) {
                        val stripeY = capT + capH * 0.22f + i * capH * 0.26f
                        drawLine(Color(0xFF37474F), start = Offset(capX + capW - stripeW + 1f, stripeY), end = Offset(capX + capW - 3f, stripeY), strokeWidth = 2.0f)
                    }

                    // Exploding safety relief score vents (K-type groove cross) on top dome
                    drawLine(Color(0x80FFFFFF), start = Offset(cx - 4f, capT + 4f), end = Offset(cx + 4f, capT + 4f), strokeWidth = 1.2f)
                    drawLine(Color(0x80FFFFFF), start = Offset(cx, capT + 1f), end = Offset(cx, capT + 7f), strokeWidth = 1.2f)

                    // Dynamic electrical charge discharge blue particles cloud
                    if (component.charge > 0.05f) {
                        val maxCap = getMaxCap(component)
                        val chargeRatio = (component.charge / maxCap).coerceIn(0f, 1f)
                        drawCircle(Color(0xE600E5FF).copy(alpha = chargeRatio * 0.85f), radius = strokeSize * 2.2f, center = Offset(cx, cy))
                    }
                }

                ComponentType.INDUCTOR -> {
                    // Thick lead connection pins
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    
                    // High-permeability green iron powder core torus
                    val coreRadius = cellSize * 0.33f
                    drawCircle(Color(0xFF1B5E20), radius = coreRadius, center = Offset(cx, cy))
                    drawCircle(Color(0xFF0B1F0E), radius = coreRadius * 0.46f, center = Offset(cx, cy)) // Ring donut hollow
                    
                    // Hand-wrapped copper coil wiring sections with lighting gradients
                    val windingColor = Color(0xFFBF360C) // Heavy copper reddish coil
                    for (angle in listOf(0f, 40f, 80f, 120f, 160f, 200f, 240f, 280f, 320f)) {
                        val rad = Math.toRadians(angle.toDouble())
                        val cos = Math.cos(rad).toFloat()
                        val sin = Math.sin(rad).toFloat()
                        
                        // Main copper winding line loop
                        drawLine(
                            color = windingColor,
                            start = Offset(cx + coreRadius * 0.38f * cos, cy + coreRadius * 0.38f * sin),
                            end = Offset(cx + coreRadius * 1.18f * cos, cy + coreRadius * 1.18f * sin),
                            strokeWidth = 3.5f
                        )
                        // Specular copper reflection path
                        drawLine(
                            color = Color(0xFFFFCC80),
                            start = Offset(cx + coreRadius * 0.72f * cos, cy + coreRadius * 0.72f * sin),
                            end = Offset(cx + coreRadius * 1.05f * cos, cy + coreRadius * 1.05f * sin),
                            strokeWidth = 1.2f
                        )
                    }
                    
                    // Spherical magnetic pressure waves running outwards
                    if (component.isPowered) {
                        drawCircle(Color(0x40FF9800), radius = coreRadius * 1.4f, center = Offset(cx, cy), style = Stroke(2.2f))
                        drawCircle(Color(0xFFFFE082), radius = 3.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.FUSE -> {
                    // Center bridge line
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = strokeSize * 0.7f)
                    
                    val fuseW = padding * 2.0f
                    val fuseH = cellSize - padding * 3.8f
                    val fuseX = cx - fuseW / 2
                    val fuseY = padding * 1.9f
                    
                    // High-quality glass insulator sleeve
                    drawRoundRect(
                        color = Color(0x2B80DEEA),
                        topLeft = Offset(fuseX, fuseY),
                        size = Size(fuseW, fuseH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                    drawRoundRect(
                        color = Color(0x77FFFFFF),
                        topLeft = Offset(fuseX, fuseY),
                        size = Size(fuseW, fuseH),
                        style = Stroke(1.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                    
                    // Heavily polished steel/silver end contacts
                    val capHeight = strokeSize * 1.3f
                    drawRect(Color(0xFFECEFF1), topLeft = Offset(fuseX, fuseY), size = Size(fuseW, capHeight))
                    drawRect(Color(0xFF78909C), topLeft = Offset(fuseX, fuseY + capHeight - 1f), size = Size(fuseW, 1.5f)) 
                    drawRect(Color(0xFFECEFF1), topLeft = Offset(fuseX, fuseY + fuseH - capHeight), size = Size(fuseW, capHeight))
                    drawLine(Color(0xFF78909C), start = Offset(fuseX, fuseY + fuseH - capHeight + 1f), end = Offset(fuseX + fuseW, fuseY + fuseH - capHeight + 1f), strokeWidth = 1.5f)

                    // Filament wire inside quartz sleeve
                    if (component.isOverloaded) {
                        // Exploded broken filament with molten bronze bits on terminals
                        drawLine(Color(0xFFFF1744), start = Offset(cx, fuseY + capHeight), end = Offset(cx - 3.5f, cy - 4f), strokeWidth = 1.8f)
                        drawLine(Color(0xFFFF1744), start = Offset(cx, fuseY + fuseH - capHeight), end = Offset(cx + 3.5f, cy + 4f), strokeWidth = 1.8f)
                        // Electric soot blast mark inside glass
                        drawCircle(Color(0xDD303030), radius = fuseW * 0.45f, center = Offset(cx, cy))
                        drawCircle(Color(0xFFFF3D00), radius = 2.5f, center = Offset(cx - 1.5f, cy - 1.5f))
                    } else {
                        // Pristine filament showing high-efficiency operation
                        val filamentCol = if (component.isPowered) Color(0xFFFFD54F) else Color(0xFF607D8B)
                        if (component.isPowered) {
                            drawLine(Color(0x99FFEA00), start = Offset(cx, fuseY + capHeight), end = Offset(cx, fuseY + fuseH - capHeight), strokeWidth = 4.5f)
                        }
                        drawLine(filamentCol, start = Offset(cx, fuseY + capHeight), end = Offset(cx, fuseY + fuseH - capHeight), strokeWidth = 1.8f)
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
                    val bRad = cellSize * 0.42f
                    drawCircle(Color(0xFF21272B), radius = bRad, center = Offset(cx, cy))
                    drawCircle(Color(0xFF455A64), radius = bRad, center = Offset(cx, cy), style = Stroke(strokeSize * 0.5f))
                    
                    // Central hole revealing golden brass piezo transducer diaphragm inside
                    val hRad = cellSize * 0.18f
                    drawCircle(Color(0xFFFFC107), radius = hRad, center = Offset(cx, cy)) // Piezo brass disc
                    drawCircle(Color(0xFF0D1113), radius = hRad * 0.65f, center = Offset(cx, cy)) // Dark inner sound port hole
                    
                    // Polished specular highlight round rim
                    drawArc(Color.White.copy(alpha = 0.2f), startAngle = -135f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - bRad, cy - bRad), size = Size(bRad * 2f, bRad * 2f), style = Stroke(2f))

                    // Labeled plus polarity symbol (+)
                    drawLine(Color(0x99FFFFFF), start = Offset(cx + bRad * 0.5f, cy - bRad * 0.5f - 4f), end = Offset(cx + bRad * 0.5f, cy - bRad * 0.5f + 4f), strokeWidth = 1.2f)
                    drawLine(Color(0x99FFFFFF), start = Offset(cx + bRad * 0.5f - 4f, cy - bRad * 0.5f), end = Offset(cx + bRad * 0.5f + 4f, cy - bRad * 0.5f), strokeWidth = 1.2f)

                    // Concentric sonic acoustic waves radiating when active
                    if (component.isPowered) {
                        val pulse = (System.currentTimeMillis() % 500) / 500f
                        drawCircle(
                            color = Color(0xFFFF3D00).copy(alpha = 1f - pulse),
                            radius = bRad + pulse * 18f,
                            center = Offset(cx, cy),
                            style = Stroke(1.8f)
                        )
                        drawCircle(Color(0xFFFFEA00), radius = 3.5f, center = Offset(cx, cy))
                    }
                }

                ComponentType.MICROPHONE -> {
                    // Lead wire traces
                    drawLine(Color(0xFFCFD8DC), start = Offset(cx, cy), end = Offset(cx, cellSize), strokeWidth = 3f)
                    
                    val micRad = cellSize * 0.38f
                    // Round outer casing
                    drawCircle(Color(0xFF37474F), radius = micRad, center = Offset(cx, cy))
                    // Silver casing ring borders
                    drawCircle(Color(0xFF90A4AE), radius = micRad, center = Offset(cx, cy), style = Stroke(strokeSize * 0.5f))
                    
                    // Inner electret felt cross guard pad (black acoustic center)
                    drawCircle(Color(0xFF151515), radius = micRad * 0.75f, center = Offset(cx, cy))
                    
                    // Copper / brass mesh wire guidelines (cross guard grid)
                    val meshCol = Color(0x4000E5FF)
                    for (i in -2..2) {
                        val offset = i * micRad * 0.26f
                        drawLine(meshCol, start = Offset(cx - micRad * 0.7f, cy + offset), end = Offset(cx + micRad * 0.7f, cy + offset), strokeWidth = 1f)
                        drawLine(meshCol, start = Offset(cx + offset, cy - micRad * 0.7f), end = Offset(cx + offset, cy + micRad * 0.7f), strokeWidth = 1f)
                    }
                    
                    // Labeled "MIC" text symbol representation
                    drawRect(Color(0xEE212121), topLeft = Offset(cx - padding * 1.1f, cy + padding * 0.4f), size = Size(padding * 2.2f, strokeSize * 0.8f))
                    
                    // Dynamic vocal wave ripple halo on power active
                    if (component.isPowered) {
                        val waveAmt = ((System.currentTimeMillis() % 600) / 600f)
                        drawCircle(Color(0xFF00E676).copy(alpha = 1f - waveAmt), radius = micRad * (1f + waveAmt * 0.5f), center = Offset(cx, cy), style = Stroke(2f))
                        drawCircle(Color(0xFF00FFCC), radius = 4f, center = Offset(cx, cy))
                    }
                }

                ComponentType.SOLENOID -> {
                    // Outer square iron frame bracket
                    val fW = cellSize - padding * 2.2f
                    val fH = cellSize - padding * 1.8f
                    val fx = cx - fW / 2
                    val fy = cy - fH / 2
                    
                    // Grey high-precision outline body
                    drawRoundRect(Color(0xFF37474F), topLeft = Offset(fx, fy), size = Size(fW, fH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f), style = Stroke(strokeSize * 0.5f))
                    
                    // Dense internal copper electromagnet winding block
                    val coilW = fW - strokeSize * 1.6f
                    val coilH = fH - strokeSize * 1.6f
                    drawRect(
                        color = Color(0xFFBF360C),
                        topLeft = Offset(cx - coilW / 2, cy - coilH / 2),
                        size = Size(coilW, coilH)
                    )
                    // Highlighting bright golden/orange windings wires
                    for (i in -3..3) {
                        val lx = cx + i * coilW / 8f
                        drawLine(Color(0xFFFF8A65), start = Offset(lx, cy - coilH / 2), end = Offset(lx, cy + coilH / 2), strokeWidth = 1.8f)
                    }
                    
                    // Sliding plunger armature shaft
                    val pushDist = if (component.isPowered) -cellSize * 0.18f else cellSize * 0.18f
                    val plungerW = strokeSize * 1.8f
                    drawRoundRect(
                        color = Color(0xFFECEFF1), // Chrome plated rod
                        topLeft = Offset(cx - plungerW / 2, cy - coilH / 2 + pushDist),
                        size = Size(plungerW, coilH + 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    // Plunger tip eyelet anchor loop
                    drawCircle(Color(0xFFCFD8DC), radius = plungerW * 0.65f, center = Offset(cx, cy - coilH / 2 + pushDist - 3f))
                    drawCircle(Color(0xFF37474F), radius = plungerW * 0.3f, center = Offset(cx, cy - coilH / 2 + pushDist - 3f))

                    // Spark indicator LED
                    val ind = if (component.isPowered) Color(0xFF00FFCC) else Color(0xFFFF1744)
                    drawCircle(ind, radius = 3.5f, center = Offset(cx + fW / 2 - 4f, cy - fH / 2 + 4f))
                }

                ComponentType.LINEAR_ACTUATOR -> {
                    // Heavy industrial frame guides
                    val ly = padding * 1.4f
                    val lh = cellSize - padding * 2.8f
                    val lx = cx - padding * 1.3f
                    val lw = padding * 2.6f
                    
                    // Frame guide plate (black/orange heavy equipment backing plate)
                    drawRect(Color(0xFF1E1E1E), topLeft = Offset(lx, ly), size = Size(lw, lh))
                    
                    // Yellow/Black warning diagonal hatch stripes along side boundaries
                    val hatchW = 3.2f
                    for (i in 0..5) {
                        val hx = lx + 1f
                        val hy = ly + i * lh / 6f
                        drawLine(Color(0xFFFFEA00), start = Offset(hx, hy), end = Offset(hx + lw * 0.25f, hy + lh * 0.08f), strokeWidth = hatchW)
                        drawLine(Color(0xFFFFEA00), start = Offset(lx + lw - hatchW - lw * 0.25f, hy), end = Offset(lx + lw - hatchW, hy + lh * 0.08f), strokeWidth = hatchW)
                    }

                    // Polished central stainless lead screw shaft thread
                    val threadW = strokeSize * 0.8f
                    drawRect(Color(0xFFCFD8DC), topLeft = Offset(cx - threadW / 2, ly), size = Size(threadW, lh))
                    // Helical pitch notches on the screw thread
                    for (i in 0..10) {
                        val threadY = ly + i * lh / 10f
                        drawLine(Color(0xFF78909C), start = Offset(cx - threadW / 2, threadY), end = Offset(cx + threadW / 2, threadY + 2f), strokeWidth = 1.2f)
                    }

                    // Sliding carriage actuator traveler block
                    val travY = if (component.isPowered) ly + lh * 0.18f else ly + lh * 0.68f
                    val travH = strokeSize * 2.0f
                    val travW = lw - 4f
                    drawRoundRect(
                        color = Color(0xFFD32F2F), 
                        topLeft = Offset(cx - travW / 2, travY),
                        size = Size(travW, travH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
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
                            ComponentType.WATER, ComponentType.INFINITE_WATER -> Color(android.graphics.Color.parseColor(com.example.engine.JavaModEngine.waterColorHex))
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
                            ComponentType.BRICK -> Color(android.graphics.Color.parseColor(com.example.engine.JavaModEngine.brickColorHex))
                            ComponentType.OBSIDIAN -> Color(0xFF1C1C1C)
                            ComponentType.BEDROCK -> Color(0xFF000000)
                            ComponentType.CORIUM -> {
                                val pulse = (Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5).toFloat()
                                val r = (210 + (pulse * 45)).toInt().coerceIn(0, 255)
                                val g = (40 + (pulse * 40)).toInt().coerceIn(0, 255)
                                val b = (10 + (pulse * 20)).toInt().coerceIn(0, 255)
                                Color(r, g, b, 255)
                            }
                            ComponentType.PLASMA, ComponentType.INFINITE_PLASMA -> Color(0xFF7B1FA2)
                            ComponentType.BLACK_HOLE -> Color(0xFF05050A)
                            ComponentType.PORTAL_IN -> Color(0xFF1E1005)
                            ComponentType.PORTAL_OUT -> Color(0xFF0A1128)
                            ComponentType.TESLA_COIL -> Color(0xFF263238)
                            ComponentType.MERCURY -> Color(0xFFCFD8DC)
                            ComponentType.LIGHTNING_ROD -> Color(0xFF37474F)
                            ComponentType.STIRLING_ENGINE -> Color(0xFF455A64)
                            ComponentType.QUANTUM_SUPERCONDUCTOR -> Color(0xFF006064)
                            ComponentType.PCM_CELL -> Color(0xFF00838F)
                            ComponentType.LASER_RECEIVER -> Color(0xFF1A237E)
                            ComponentType.GRAPHITE_ROD -> Color(0xFF212121)
                            ComponentType.PIEZO_SENSOR -> Color(0xFF004D40)
                            else -> Color.Transparent
                        }
                        val isFluid = isFluidType(type)

                        if (isFluid) {
                            val rx = cellSize / 2f
                            val ry = cellSize / 2f
                            val upSame = y > 0 && isFluidType(grid[x][y-1].type)
                            val downSame = y < height - 1 && isFluidType(grid[x][y+1].type)
                            val leftSame = x > 0 && isFluidType(grid[x-1][y].type)
                            val rightSame = x < width - 1 && isFluidType(grid[x+1][y].type)

                            val bridgeWidth = cellSize * 0.76f
                            if (upSame) drawLine(matColor, start = Offset(rx, ry), end = Offset(rx, 0f), strokeWidth = bridgeWidth)
                            if (downSame) drawLine(matColor, start = Offset(rx, ry), end = Offset(rx, cellSize), strokeWidth = bridgeWidth)
                            if (leftSame) drawLine(matColor, start = Offset(rx, ry), end = Offset(0f, ry), strokeWidth = bridgeWidth)
                            if (rightSame) drawLine(matColor, start = Offset(rx, ry), end = Offset(cellSize, ry), strokeWidth = bridgeWidth)

                            drawCircle(
                                color = matColor,
                                radius = cellSize * 0.38f,
                                center = Offset(rx, ry)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.82f),
                                radius = cellSize * 0.11f,
                                center = Offset(rx - cellSize * 0.12f, ry - cellSize * 0.12f)
                            )

                            val seed = (x * 19 + y * 29)
                            val d1 = cellSize * 0.15f
                            val d2 = cellSize * 0.10f
                            val d3 = cellSize * 0.08f

                            if (!downSame) {
                                val ox1 = ((seed % 10) - 5) / 20f * cellSize
                                val oy1 = ((seed % 13) - 2) / 12f * cellSize + (cellSize * 0.24f)
                                drawCircle(
                                    color = matColor.copy(alpha = 0.9f),
                                    radius = d1,
                                    center = Offset(rx + ox1, ry + oy1)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    radius = d1 * 0.3f,
                                    center = Offset(rx + ox1 - d1 * 0.3f, ry + oy1 - d1 * 0.3f)
                                )
                            }
                            if (!upSame) {
                                val ox2 = (((seed + 5) % 11) - 5) / 22f * cellSize
                                val oy2 = -(((seed + 3) % 8) + 2) / 18f * cellSize - (cellSize * 0.18f)
                                drawCircle(
                                    color = matColor.copy(alpha = 0.85f),
                                    radius = d2,
                                    center = Offset(rx + ox2, ry + oy2)
                                )
                            }
                            if (!leftSame && !rightSame) {
                                val jitterX = ((seed % 30 - 15) / 100f) * cellSize
                                val jitterY = (((seed * 7) % 30 - 15) / 100f) * cellSize
                                drawCircle(
                                    color = matColor.copy(alpha = 0.8f),
                                    radius = d3,
                                    center = Offset(rx + jitterX, ry + jitterY)
                                )
                            }
                        } else {
                            drawRect(matColor, size = Size(cellSize, cellSize))
                        }
                        
                        // Render customized textures with fine details per component type
                        when (type) {
                            ComponentType.SAND -> {
                                // Draw horizontal sandy dunes waving lines
                                val dunePath1 = getReusablePath()
                                dunePath1.moveTo(0f, cellSize * 0.25f)
                                dunePath1.quadraticTo(cellSize * 0.35f, cellSize * 0.15f, cellSize * 0.7f, cellSize * 0.35f)
                                dunePath1.quadraticTo(cellSize * 0.85f, cellSize * 0.45f, cellSize, cellSize * 0.3f)
                                drawPath(dunePath1, Color(0xFFD48F00), style = Stroke(2f))

                                val dunePath2 = getReusablePath()
                                dunePath2.moveTo(0f, cellSize * 0.75f)
                                dunePath2.quadraticTo(cellSize * 0.4f, cellSize * 0.85f, cellSize * 0.75f, cellSize * 0.65f)
                                dunePath2.quadraticTo(cellSize * 0.9f, cellSize * 0.55f, cellSize, cellSize * 0.7f)
                                drawPath(dunePath2, Color(0xFFB57C00), style = Stroke(2f))

                                // Granular golden sand grains
                                drawCircle(Color(0xFFFFD54F), radius = cellSize * 0.05f, center = Offset(cellSize * 0.22f, cellSize * 0.45f))
                                drawCircle(Color(0xFFFFEE58), radius = cellSize * 0.04f, center = Offset(cellSize * 0.5f, cellSize * 0.2f))
                                drawCircle(Color(0xFFFFD54F), radius = cellSize * 0.04f, center = Offset(cellSize * 0.82f, cellSize * 0.5f))
                                drawCircle(Color(0xFFE5A900), radius = cellSize * 0.04f, center = Offset(cellSize * 0.15f, cellSize * 0.85f))
                                drawCircle(Color(0xFFFFEE58), radius = cellSize * 0.03f, center = Offset(cellSize * 0.62f, cellSize * 0.88f))
                            }
                            ComponentType.DIRT -> {
                                // Rich organic soil with clay-stone-humus layers and sub-roots
                                drawRect(Color(0xFF3E2723), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize * 0.5f)) // Humus top layer
                                drawRect(Color(0xFF271714), topLeft = Offset(0f, cellSize * 0.5f), size = Size(cellSize, cellSize * 0.5f)) // Dense bottom layer
                                
                                // Organic root lines branching downwards with sub-branches
                                val rootPath = getReusablePath()
                                rootPath.moveTo(cellSize * 0.45f, 0f)
                                rootPath.quadraticTo(cellSize * 0.52f, cellSize * 0.3f, cellSize * 0.35f, cellSize * 0.6f)
                                rootPath.quadraticTo(cellSize * 0.28f, cellSize * 0.82f, cellSize * 0.2f, cellSize * 0.95f)
                                drawPath(rootPath, Color(0xFF8D6E63), style = Stroke(1.8f))

                                val subRoot = getReusablePath()
                                subRoot.moveTo(cellSize * 0.35f, cellSize * 0.6f)
                                subRoot.quadraticTo(cellSize * 0.55f, cellSize * 0.75f, cellSize * 0.7f, cellSize * 0.9f)
                                drawPath(subRoot, Color(0xFFA1887F), style = Stroke(1.2f))

                                // Micro organic granules and small pebble stone flakes
                                drawCircle(Color(0xFF4E342E), radius = cellSize * 0.07f, center = Offset(cellSize * 0.2f, cellSize * 0.25f))
                                drawCircle(Color(0xFF795548), radius = cellSize * 0.05f, center = Offset(cellSize * 0.85f, cellSize * 0.4f))
                                drawCircle(Color(0xFF5D4037), radius = cellSize * 0.06f, center = Offset(cellSize * 0.75f, cellSize * 0.75f))
                            }
                            ComponentType.STONE -> {
                                // Hard basalt/slate cobblestone fracture geometric patterns
                                drawLine(Color(0xFF263238), start = Offset(0f, cellSize * 0.4f), end = Offset(cellSize * 0.45f, cellSize * 0.35f), strokeWidth = 2.5f)
                                drawLine(Color(0xFF263238), start = Offset(cellSize * 0.45f, cellSize * 0.35f), end = Offset(cellSize * 0.6f, cellSize * 0.95f), strokeWidth = 2.5f)
                                drawLine(Color(0xFF263238), start = Offset(cellSize * 0.45f, cellSize * 0.35f), end = Offset(cellSize, cellSize * 0.25f), strokeWidth = 2.5f)
                                
                                // Fractured cleft lines
                                drawLine(Color(0xFF1E272C), start = Offset(cellSize * 0.25f, cellSize * 0.68f), end = Offset(cellSize * 0.6f, cellSize * 0.95f), strokeWidth = 1.8f)
                                drawLine(Color(0xFF1E272C), start = Offset(cellSize * 0.45f, cellSize * 0.35f), end = Offset(0f, cellSize * 0.1f), strokeWidth = 1.8f)
                                
                                // Specular rock bevel high-contrast illuminated edges
                                drawLine(Color(0xFF78909C), start = Offset(0f, cellSize * 0.37f), end = Offset(cellSize * 0.45f, cellSize * 0.32f), strokeWidth = 1.2f)
                                drawLine(Color(0xFF78909C), start = Offset(cellSize * 0.45f, cellSize * 0.32f), end = Offset(cellSize, cellSize * 0.22f), strokeWidth = 1.2f)
                                drawLine(Color(0xFF78909C), start = Offset(cellSize * 0.25f, cellSize * 0.65f), end = Offset(cellSize * 0.6f, cellSize * 0.92f), strokeWidth = 1.0f)
                            }
                            ComponentType.GLASS -> {
                                // Modern pristine glass window pane with dual bright glare bands
                                drawLine(Color(0x90FFFFFF), start = Offset(cellSize * 0.1f, 0f), end = Offset(cellSize, cellSize * 0.9f), strokeWidth = cellSize * 0.08f)
                                drawLine(Color(0xCCFFFFFF), start = Offset(0f, cellSize * 0.3f), end = Offset(cellSize * 0.7f, cellSize), strokeWidth = 2.5f)
                                drawLine(Color(0x2B4DD0E1), start = Offset(0f, cellSize * 0.05f), end = Offset(cellSize * 0.95f, cellSize), strokeWidth = cellSize * 0.18f)
                                
                                // High-tech double safety border profile
                                drawRect(Color(0x66FFFFFF), topLeft = Offset(1.5f, 1.5f), size = Size(cellSize - 3f, cellSize - 3f), style = Stroke(1.2f))
                                
                                // Corner chrome mounting pins/rivets
                                drawCircle(Color(0xB3FFFFFF), radius = 2.0f, center = Offset(3.5f, 3.5f))
                                drawCircle(Color(0xB3FFFFFF), radius = 2.0f, center = Offset(cellSize - 3.5f, 3.5f))
                                drawCircle(Color(0xB3FFFFFF), radius = 2.0f, center = Offset(3.5f, cellSize - 3.5f))
                                drawCircle(Color(0xB3FFFFFF), radius = 2.0f, center = Offset(cellSize - 3.5f, cellSize - 3.5f))
                            }
                            ComponentType.WOOD -> {
                                // Elegant mahogany wood grain with concentric growth rings and centered knot
                                val knotX = cellSize * 0.35f
                                val knotY = cellSize * 0.4f
                                drawCircle(Color(0xFF3E2723), radius = cellSize * 0.09f, center = Offset(knotX, knotY))
                                
                                // Perfect wood whorls wrapping around knot
                                drawArc(Color(0xFF4E342E), startAngle = 15f, sweepAngle = 175f, useCenter = false, topLeft = Offset(knotX - cellSize * 0.45f, knotY - cellSize * 0.45f), size = Size(cellSize * 0.9f, cellSize * 0.9f), style = Stroke(2.0f))
                                drawArc(Color(0xFF3E2723), startAngle = 30f, sweepAngle = 160f, useCenter = false, topLeft = Offset(knotX - cellSize * 0.75f, knotY - cellSize * 0.75f), size = Size(cellSize * 1.5f, cellSize * 1.5f), style = Stroke(1.6f))
                                drawArc(Color(0xFF5D4037), startAngle = 0f, sweepAngle = 190f, useCenter = false, topLeft = Offset(knotX - cellSize * 1.05f, knotY - cellSize * 1.05f), size = Size(cellSize * 2.1f, cellSize * 2.1f), style = Stroke(1.2f))
                                
                                // Secondary wood bark veins running across
                                drawLine(Color(0xFF3E2723), start = Offset(0f, cellSize * 0.15f), end = Offset(cellSize * 0.25f, cellSize * 0.13f), strokeWidth = 1.2f)
                                drawLine(Color(0xFF3E2723), start = Offset(cellSize * 0.75f, cellSize * 0.85f), end = Offset(cellSize, cellSize * 0.83f), strokeWidth = 1.2f)
                            }
                            ComponentType.ICE -> {
                                // Crystallized winter ice with radial symmetrical star and internal deep cold fractures
                                drawLine(Color(0xFFB2EBF2), start = Offset(0f, 0f), end = Offset(cellSize, cellSize), strokeWidth = 2.0f)
                                drawLine(Color(0xFFB2EBF2), start = Offset(cellSize, 0f), end = Offset(0f, cellSize), strokeWidth = 1.5f)
                                drawLine(Color(0x99FFFFFF), start = Offset(cx, 0f), end = Offset(cx, cellSize), strokeWidth = 1.2f)
                                drawLine(Color(0x99FFFFFF), start = Offset(0f, cy), end = Offset(cellSize, cy), strokeWidth = 1.2f)
                                
                                // Radiant core frost star
                                drawCircle(Color(0xFFE0F7FA), radius = cellSize * 0.1f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.04f, center = Offset(cx, cy))
                                
                                // Fractured facets and micro air bubbles inside glaciers
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = 2f, center = Offset(cellSize * 0.25f, cellSize * 0.35f))
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.8f, center = Offset(cellSize * 0.72f, cellSize * 0.18f))
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = 2.2f, center = Offset(cellSize * 0.18f, cellSize * 0.78f))
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.5f, center = Offset(cellSize * 0.8f, cellSize * 0.68f))
                            }
                            ComponentType.COAL -> {
                                // Carbon crystalline obsidian coal chunk with facets
                                val coalPath = getReusablePath()
                                coalPath.moveTo(cellSize * 0.18f, cellSize * 0.52f)
                                coalPath.lineTo(cellSize * 0.52f, cellSize * 0.15f)
                                coalPath.lineTo(cellSize * 0.88f, cellSize * 0.38f)
                                coalPath.lineTo(cellSize * 0.78f, cellSize * 0.88f)
                                coalPath.lineTo(cellSize * 0.28f, cellSize * 0.82f)
                                coalPath.close()
                                
                                drawPath(coalPath, Color(0xFF0F0F12)) // Deep space matte core
                                drawPath(coalPath, Color(0xFF546E7A), style = Stroke(2.2f)) // Rich anthracite mineral outline
                                
                                // Sparkling carbon reflect facets
                                drawLine(Color(0xFF78909C), start = Offset(cellSize * 0.52f, cellSize * 0.15f), end = Offset(cellSize * 0.52f, cellSize * 0.82f), strokeWidth = 1.8f)
                                drawLine(Color(0xFF546E7A), start = Offset(cellSize * 0.18f, cellSize * 0.52f), end = Offset(cellSize * 0.52f, cellSize * 0.52f), strokeWidth = 1.5f)
                                drawLine(Color(0xFF546E7A), start = Offset(cellSize * 0.88f, cellSize * 0.38f), end = Offset(cellSize * 0.52f, cellSize * 0.52f), strokeWidth = 1.5f)
                                drawCircle(Color.White.copy(alpha = 0.3f), radius = 3f, center = Offset(cellSize * 0.52f, cellSize * 0.52f))
                            }
                            ComponentType.SPONGE -> {
                                // Porous organic sponge structure with dual-tone overlapping volumetric vacuoles
                                drawCircle(Color(0xFFE65100), radius = cellSize * 0.11f, center = Offset(cellSize * 0.26f, cellSize * 0.32f))
                                drawCircle(Color(0xFFFFB74D), radius = cellSize * 0.07f, center = Offset(cellSize * 0.26f, cellSize * 0.32f))
                                
                                drawCircle(Color(0xFFE65100), radius = cellSize * 0.13f, center = Offset(cellSize * 0.74f, cellSize * 0.36f))
                                drawCircle(Color(0xFFFFB74D), radius = cellSize * 0.09f, center = Offset(cellSize * 0.74f, cellSize * 0.36f))
                                
                                drawCircle(Color(0xFFE65100), radius = cellSize * 0.10f, center = Offset(cellSize * 0.36f, cellSize * 0.78f))
                                drawCircle(Color(0xFFFFB74D), radius = cellSize * 0.06f, center = Offset(cellSize * 0.36f, cellSize * 0.78f))
                                
                                drawCircle(Color(0xFFE65100), radius = cellSize * 0.12f, center = Offset(cellSize * 0.78f, cellSize * 0.78f))
                                drawCircle(Color(0xFFFFB74D), radius = cellSize * 0.08f, center = Offset(cellSize * 0.78f, cellSize * 0.78f))

                                // Tiny micro-spores
                                drawCircle(Color(0xFFE65100), radius = 1.8f, center = Offset(cellSize * 0.5f, cellSize * 0.2f))
                                drawCircle(Color(0xFFE65100), radius = 1.8f, center = Offset(cellSize * 0.52f, cellSize * 0.56f))
                                drawCircle(Color(0xFFE65100), radius = 1.5f, center = Offset(cellSize * 0.15f, cellSize * 0.6f))
                            }
                            ComponentType.SLIME, ComponentType.INFINITE_SLIME -> {
                                // Vivid glowing toxic slime mass with animated pulsing core bubbles
                                val pulseSl = (kotlin.math.sin((System.currentTimeMillis() % 1400) / 1400f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                val outerRad = cellSize * (0.35f + 0.08f * pulseSl)
                                val innerRad = cellSize * (0.24f + 0.05f * pulseSl)
                                
                                drawCircle(Color(0xBB00E676), radius = outerRad, center = Offset(cx, cy))
                                drawCircle(Color(0x80CCFF90), radius = innerRad, center = Offset(cx, cy))
                                
                                // Internal biological active green nucleus vacuoles
                                drawCircle(Color.White.copy(alpha = 0.85f), radius = cellSize * 0.06f, center = Offset(cx - cellSize * 0.08f, cy - cellSize * 0.08f))
                                drawCircle(Color.White.copy(alpha = 0.55f), radius = cellSize * 0.04f, center = Offset(cx + cellSize * 0.12f, cy + cellSize * 0.1f))
                                drawCircle(Color(0xFF00C853), radius = 2f, center = Offset(cx + cellSize * 0.02f, cy - cellSize * 0.18f))
                            }
                            ComponentType.MAGIC_DUST -> {
                                // Pulsing spatial magic dust energy nexus with circular target coordinates
                                val scaleSt = 0.45f + 0.55f * kotlin.math.sin((System.currentTimeMillis() % 1400) / 1400f * 2.0 * java.lang.Math.PI).toFloat()
                                drawCircle(Color(0xFFE040FB).copy(alpha = 0.35f), radius = cellSize * 0.44f * scaleSt, center = Offset(cx, cy))
                                
                                // Central high-luminosity cross star flare
                                drawLine(Color.White, start = Offset(cx - cellSize * 0.32f * scaleSt, cy), end = Offset(cx + cellSize * 0.32f * scaleSt, cy), strokeWidth = 2.5f)
                                drawLine(Color.White, start = Offset(cx, cy - cellSize * 0.32f * scaleSt), end = Offset(cx, cy + cellSize * 0.32f * scaleSt), strokeWidth = 2.5f)
                                
                                drawCircle(Color(0xFFE040FB), radius = cellSize * 0.12f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.06f, center = Offset(cx, cy))
                                
                                // Floating high-energy magic fuel dust nodes
                                drawCircle(Color.White, radius = 2.0f, center = Offset(cellSize * 0.22f, cellSize * 0.78f))
                                drawCircle(Color(0xFFD500F9), radius = 2.5f, center = Offset(cellSize * 0.78f, cellSize * 0.22f))
                                drawCircle(Color(0xFFEA80FC), radius = 1.8f, center = Offset(cellSize * 0.18f, cellSize * 0.25f))
                            }
                            ComponentType.URANIUM -> {
                                // Intensified hazard radiation field with central core nuclear markers
                                val pulseU = (kotlin.math.sin((System.currentTimeMillis() % 1000) / 1000f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                
                                // Glowing emerald green corona cloud field
                                drawCircle(Color(0x2200E676), radius = cellSize * (0.46f + 0.12f * pulseU), center = Offset(cx, cy))
                                drawCircle(Color(0x5500C853), radius = cellSize * (0.33f + 0.08f * pulseU), center = Offset(cx, cy))
                                drawCircle(Color(0xDD00E676), radius = cellSize * 0.22f, center = Offset(cx, cy))
                                
                                // Inner carbonized radioactive heavy isotope shell
                                drawCircle(Color(0xFF1B5E20), radius = cellSize * 0.12f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
                            }
                            ComponentType.OBSIDIAN -> {
                                // Crystalline deep onyx block with ultra high-voltage neon pink/purple molten magma fracture veins
                                val sid = cellSize
                                val p1 = getReusablePath()
                                p1.moveTo(0f, sid * 0.65f)
                                p1.lineTo(sid * 0.45f, sid * 0.3f)
                                p1.lineTo(sid, sid * 0.6f)
                                drawPath(p1, Color(0xFFFF00FF), style = Stroke(2.5f))
                                drawPath(p1, Color.White.copy(alpha = 0.5f), style = Stroke(1.0f))

                                val p2 = getReusablePath()
                                p2.moveTo(sid * 0.25f, sid * 0.95f)
                                p2.lineTo(sid * 0.55f, sid * 0.48f)
                                p2.lineTo(sid * 0.88f, sid * 0.88f)
                                drawPath(p2, Color(0xFFD500F9), style = Stroke(2.0f))
                                
                                // Shiny glassy obsidian corner glares
                                drawLine(Color(0x44FFFFFF), start = Offset(0f, sid * 0.2f), end = Offset(sid * 0.5f, 0f), strokeWidth = 2f)
                            }
                            ComponentType.BEDROCK -> {
                                // High-strength dark slate bedrock block with steel support frames, safety stripes and rivets
                                drawRect(Color(0xFF111115), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize))
                                drawRect(Color(0xFF37474F), topLeft = Offset(cellSize * 0.15f, cellSize * 0.15f), size = Size(cellSize * 0.7f, cellSize * 0.7f), style = Stroke(1.8f))
                                
                                // Bold diagonal yellow/black warning panels
                                for (i in 0..4) {
                                     val offset = i * (cellSize / 4.5f)
                                     drawLine(Color(0xFFFFD600), start = Offset(offset, 0f), end = Offset(offset - cellSize * 0.25f, cellSize), strokeWidth = 3f)
                                }
                                
                                // Center heavy nuclear steel reinforcement anchor plate
                                drawRect(Color(0xFF212121), topLeft = Offset(cellSize * 0.32f, cellSize * 0.32f), size = Size(cellSize * 0.36f, cellSize * 0.36f))
                                
                                // High-strength carbon steel structural boundary corner rivets
                                drawCircle(Color(0xFFCFD8DC), radius = 2.0f, center = Offset(4f, 4f))
                                drawCircle(Color(0xFFCFD8DC), radius = 2.0f, center = Offset(cellSize - 4f, 4f))
                                drawCircle(Color(0xFFCFD8DC), radius = 2.0f, center = Offset(4f, cellSize - 4f))
                                drawCircle(Color(0xFFCFD8DC), radius = 2.0f, center = Offset(cellSize - 4f, cellSize - 4f))
                            }
                            ComponentType.BRICK -> {
                                // Deep crimson structural engineering bricks with masonry mortar layering
                                drawLine(Color(0xFF3E2723), start = Offset(0f, cellSize * 0.33f), end = Offset(cellSize, cellSize * 0.33f), strokeWidth = 2.2f)
                                drawLine(Color(0xFF3E2723), start = Offset(0f, cellSize * 0.66f), end = Offset(cellSize, cellSize * 0.66f), strokeWidth = 2.2f)
                                
                                // Vertical alignment mortar patterns
                                drawLine(Color(0xFF3E2723), start = Offset(cellSize * 0.5f, 0f), end = Offset(cellSize * 0.5f, cellSize * 0.33f), strokeWidth = 2.0f)
                                drawLine(Color(0xFF3E2723), start = Offset(cellSize * 0.25f, cellSize * 0.33f), end = Offset(cellSize * 0.25f, cellSize * 0.66f), strokeWidth = 2.0f)
                                drawLine(Color(0xFF3E2723), start = Offset(cellSize * 0.75f, cellSize * 0.33f), end = Offset(cellSize * 0.75f, cellSize * 0.66f), strokeWidth = 2.0f)
                                drawLine(Color(0xFF3E2723), start = Offset(cellSize * 0.5f, cellSize * 0.66f), end = Offset(cellSize * 0.5f, cellSize), strokeWidth = 2.0f)
                                
                                // Highlighted concrete grout shadows
                                drawLine(Color(0xFFBCAAA4), start = Offset(0f, cellSize * 0.31f), end = Offset(cellSize, cellSize * 0.31f), strokeWidth = 0.8f)
                                drawLine(Color(0xFFBCAAA4), start = Offset(0f, cellSize * 0.64f), end = Offset(cellSize, cellSize * 0.64f), strokeWidth = 0.8f)
                            }
                            ComponentType.DIAMOND -> {
                                // Radiant brilliant cut blue sapphire/diamond shape with high-refraction sparkling flares
                                val dPath = getReusablePath()
                                dPath.moveTo(cx, cellSize * 0.1f)
                                dPath.lineTo(cellSize * 0.9f, cy)
                                dPath.lineTo(cx, cellSize * 0.9f)
                                dPath.lineTo(cellSize * 0.1f, cy)
                                dPath.close()
                                
                                drawPath(dPath, matColor)
                                drawPath(dPath, Color.White, style = Stroke(1.8f))
                                
                                // Sparkling central facet joints
                                drawLine(Color(0xDDFFFFFF), start = Offset(cx, cellSize * 0.1f), end = Offset(cx, cellSize * 0.9f), strokeWidth = 1.2f)
                                drawLine(Color(0xDDFFFFFF), start = Offset(cellSize * 0.1f, cy), end = Offset(cellSize * 0.9f, cy), strokeWidth = 1.2f)
                                
                                // High-carat internal gemstone facets
                                drawLine(Color(0x80FFFFFF), start = Offset(cellSize * 0.3f, cellSize * 0.3f), end = Offset(cellSize * 0.7f, cellSize * 0.3f), strokeWidth = 1.0f)
                                drawLine(Color(0x80FFFFFF), start = Offset(cellSize * 0.3f, cellSize * 0.7f), end = Offset(cellSize * 0.3f, cellSize * 0.3f), strokeWidth = 1.0f)
                                drawLine(Color(0x80FFFFFF), start = Offset(cellSize * 0.7f, cellSize * 0.7f), end = Offset(cellSize * 0.7f, cellSize * 0.3f), strokeWidth = 1.0f)
                                drawLine(Color(0x80FFFFFF), start = Offset(cellSize * 0.3f, cellSize * 0.7f), end = Offset(cellSize * 0.7f, cellSize * 0.7f), strokeWidth = 1.0f)
                                
                                // Center crystal sparkle spot
                                drawCircle(Color.White, radius = 3.0f, center = Offset(cx, cy))
                            }
                            ComponentType.STEEL -> {
                                // Double brushed steel industrial armor panel with central welding seam lines & fasteners
                                drawRect(Color(0xFF263238), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize), style = Stroke(2.2f))
                                drawRect(metalShineBrush, size = Size(cellSize, cellSize))
                                
                                // Horizontal safety weld-panel beam
                                drawLine(Color(0xFF455A64), start = Offset(0f, cy), end = Offset(cellSize, cy), strokeWidth = 2.0f)
                                drawLine(Color(0x44FFFFFF), start = Offset(0f, cy - 2f), end = Offset(cellSize, cy - 2f), strokeWidth = 1.0f)
                                
                                // Brushed plates safety corner rivets
                                drawCircle(Color(0xFFB0BEC5), radius = 2.2f, center = Offset(cellSize * 0.16f, cellSize * 0.16f))
                                drawCircle(Color(0xFFB0BEC5), radius = 2.2f, center = Offset(cellSize * 0.84f, cellSize * 0.16f))
                                drawCircle(Color(0xFFB0BEC5), radius = 2.2f, center = Offset(cellSize * 0.16f, cellSize * 0.84f))
                                drawCircle(Color(0xFFB0BEC5), radius = 2.2f, center = Offset(cellSize * 0.84f, cellSize * 0.84f))
                            }
                            ComponentType.COPPER -> {
                                // Professional diagonal bus copper layout with connecting solder joints (neon gold glow when powered)
                                val trackCol = if (component.isPowered) Color(0xFFFF5722) else Color(0xFFB23C17)
                                val trackJointCol = if (component.isPowered) Color(0xFFFFCC80) else Color(0xFFE64A19)
                                
                                // Main high-efficiency copper paths
                                drawLine(trackCol, start = Offset(cellSize * 0.28f, 0f), end = Offset(cellSize * 0.28f, cellSize), strokeWidth = 3.0f)
                                drawLine(trackCol, start = Offset(cellSize * 0.72f, 0f), end = Offset(cellSize * 0.72f, cellSize), strokeWidth = 3.0f)
                                
                                // Interconnecting cross copper diagonal bridge bus
                                drawLine(trackCol, start = Offset(cellSize * 0.28f, cellSize * 0.35f), end = Offset(cellSize * 0.72f, cellSize * 0.65f), strokeWidth = 2.2f)
                                
                                // Highly polished solder pads
                                drawCircle(trackJointCol, radius = cellSize * 0.09f, center = Offset(cellSize * 0.28f, cellSize * 0.35f))
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = cellSize * 0.03f, center = Offset(cellSize * 0.28f, cellSize * 0.35f))
                                
                                drawCircle(trackJointCol, radius = cellSize * 0.09f, center = Offset(cellSize * 0.72f, cellSize * 0.65f))
                                drawCircle(Color.White.copy(alpha = 0.6f), radius = cellSize * 0.03f, center = Offset(cellSize * 0.72f, cellSize * 0.65f))
                            }
                            ComponentType.GOLD -> {
                                // Premium solid gold bullion blocks stacked vertically with high-contrast sunburst reflection lines
                                drawRect(Color(0xFFD4AF37), topLeft = Offset(0f, 0f), size = Size(cellSize, cellSize), style = Stroke(2.5f))
                                drawRect(metalShineBrush, size = Size(cellSize, cellSize))
                                
                                // Symmetrically aligned double heavy gold ingots borders
                                drawLine(Color(0xFFFFD700), start = Offset(cellSize * 0.12f, cellSize * 0.28f), end = Offset(cellSize * 0.88f, cellSize * 0.28f), strokeWidth = 2.2f)
                                drawLine(Color(0xFFC5A02F), start = Offset(cellSize * 0.12f, cellSize * 0.32f), end = Offset(cellSize * 0.88f, cellSize * 0.32f), strokeWidth = 1.5f)
                                
                                drawLine(Color(0xFFFFD700), start = Offset(cellSize * 0.12f, cellSize * 0.72f), end = Offset(cellSize * 0.88f, cellSize * 0.72f), strokeWidth = 2.2f)
                                drawLine(Color(0xFFC5A02F), start = Offset(cellSize * 0.12f, cellSize * 0.76f), end = Offset(cellSize * 0.88f, cellSize * 0.76f), strokeWidth = 1.5f)
                                
                                // Spectacular glittering gold star burst
                                drawLine(Color.White, start = Offset(cx - 5f, cy), end = Offset(cx + 5f, cy), strokeWidth = 1.5f)
                                drawLine(Color.White, start = Offset(cx, cy - 5f), end = Offset(cx, cy + 5f), strokeWidth = 1.5f)
                                drawCircle(Color.White, radius = 1.5f, center = Offset(cx, cy))
                            }
                            ComponentType.ALUMINUM -> {
                                // High-quality aerospace corrugated aluminum slats on satin background
                                for (i in 1..4) {
                                    val rowY = i * (cellSize / 5f)
                                    drawLine(Color(0x55FFFFFF), start = Offset(0f, rowY), end = Offset(cellSize, rowY), strokeWidth = 1.8f)
                                    drawLine(Color(0x33000000), start = Offset(0f, rowY + 1.5f), end = Offset(cellSize, rowY + 1.5f), strokeWidth = 1.2f)
                                    
                                    // Symmetrical micro structural safety alignment pins
                                    drawCircle(Color(0xFF90A4AE), radius = 1.2f, center = Offset(cellSize * 0.18f, rowY - 2.5f))
                                    drawCircle(Color(0xFF90A4AE), radius = 1.2f, center = Offset(cellSize * 0.82f, rowY - 2.5f))
                                }
                            }
                            ComponentType.PLASMA, ComponentType.INFINITE_PLASMA -> {
                                // Solar fusion reactor core with high-velocity primary corona rings and outer solar shockwaves
                                val pulsePl = (kotlin.math.sin((System.currentTimeMillis() % 800) / 800f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                
                                // Primary solar corona shell
                                drawCircle(Color(0xFF7B1FA2).copy(alpha = 0.35f), radius = cellSize * (0.46f + 0.12f * pulsePl), center = Offset(cx, cy))
                                drawCircle(Color(0xFFFF00FF), radius = cellSize * (0.24f + 0.08f * pulsePl), center = Offset(cx, cy))
                                
                                // Ultra high-temperature helium fusion thermal center
                                drawCircle(Color(0xFFFFF176), radius = cellSize * 0.11f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.05f, center = Offset(cx, cy))
                                
                                // Kinetic solar energy flux spikes
                                drawLine(Color(0xFFFFEA00), start = Offset(cx - cellSize * 0.38f, cy), end = Offset(cx + cellSize * 0.38f, cy), strokeWidth = 2.0f)
                                drawLine(Color(0xFFFFEA00), start = Offset(cx, cy - cellSize * 0.38f), end = Offset(cx, cy + cellSize * 0.38f), strokeWidth = 2.0f)
                            }
                            ComponentType.BLACK_HOLE -> {
                                val rot = ((System.currentTimeMillis() % 2400) / 2400f) * 360f
                                // Matte dark singular core
                                drawCircle(Color(0xFF020205), radius = cellSize * 0.25f, center = Offset(cx, cy))
                                // Core cosmic event horizon boundary glow
                                drawCircle(Color(0xFF6200EA), radius = cellSize * 0.32f, center = Offset(cx, cy), style = Stroke(cellSize * 0.04f))
                                // Gravitational accretion accretion disc glow
                                drawCircle(Color(0xFFD500F9).copy(alpha = 0.25f), radius = cellSize * 0.45f, center = Offset(cx, cy))
                                
                                // Twisting spiraling plasma arms
                                val numSpokes = 4
                                for (i in 0 until numSpokes) {
                                    val angleRad = Math.toRadians((rot + i * (360f / numSpokes)).toDouble())
                                    val stopX = cx + (cellSize * 0.44f * Math.cos(angleRad)).toFloat()
                                    val stopY = cy + (cellSize * 0.44f * Math.sin(angleRad)).toFloat()
                                    drawLine(
                                        color = Color(0xFFA000FF).copy(alpha = 0.8f),
                                        start = Offset(cx, cy),
                                        end = Offset(stopX, stopY),
                                        strokeWidth = 3f
                                    )
                                    // Innermost particles line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(cx + (cellSize * 0.15f * Math.cos(angleRad)).toFloat(), cy + (cellSize * 0.15f * Math.sin(angleRad)).toFloat()),
                                        end = Offset(cx + (cellSize * 0.32f * Math.cos(angleRad)).toFloat(), cy + (cellSize * 0.32f * Math.sin(angleRad)).toFloat()),
                                        strokeWidth = 1.2f
                                    )
                                }
                            }
                            ComponentType.PORTAL_IN -> {
                                val rot = ((System.currentTimeMillis() % 1600) / 1600f) * 360f
                                // Deep spatial orange accretion portal rim
                                drawCircle(Color(0xFFE65100), radius = cellSize * 0.42f, center = Offset(cx, cy), style = Stroke(cellSize * 0.08f))
                                // Inner event horizon swirl
                                drawCircle(Color(0x33FF6D00), radius = cellSize * 0.34f, center = Offset(cx, cy))
                                
                                val angleRad = Math.toRadians(rot.toDouble())
                                val endX = cx + (cellSize * 0.38f * Math.cos(angleRad)).toFloat()
                                val endY = cy + (cellSize * 0.38f * Math.sin(angleRad)).toFloat()
                                // Rotating gateway flame bar
                                drawLine(Color(0xFFFFD180), start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = 3.5f)
                                drawCircle(Color(0xFFFF6D00), radius = cellSize * 0.12f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.05f, center = Offset(cx, cy))
                            }
                            ComponentType.PORTAL_OUT -> {
                                val rot = -((System.currentTimeMillis() % 1600) / 1600f) * 360f
                                // Cosmic cyan exiting dimensional gateway ring
                                drawCircle(Color(0xFF0091EA), radius = cellSize * 0.42f, center = Offset(cx, cy), style = Stroke(cellSize * 0.08f))
                                // Antigravity blue spatial aura
                                drawCircle(Color(0x3300B0FF), radius = cellSize * 0.34f, center = Offset(cx, cy))
                                
                                val angleRad = Math.toRadians(rot.toDouble())
                                val endX = cx + (cellSize * 0.38f * Math.cos(angleRad)).toFloat()
                                val endY = cy + (cellSize * 0.38f * Math.sin(angleRad)).toFloat()
                                // Rotating exit gravity bar
                                drawLine(Color(0xFF80D8FF), start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = 3.5f)
                                drawCircle(Color(0xFF00B0FF), radius = cellSize * 0.12f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.05f, center = Offset(cx, cy))
                            }
                            ComponentType.TESLA_COIL -> {
                                // Reinforced non-conductive dark base
                                drawRect(Color(0xFF263238), topLeft = Offset(cellSize * 0.15f, cellSize * 0.78f), size = Size(cellSize * 0.7f, cellSize * 0.14f))
                                // Secondary winding inductor pole tower
                                drawLine(Color(0xFFB0BEC5), start = Offset(cx, cellSize * 0.8f), end = Offset(cx, cy + cellSize * 0.16f), strokeWidth = 5.5f) 
                                // Copper secondary wire layers around pole
                                val coilYStart = cy + cellSize * 0.18f
                                val coilYEnd = cellSize * 0.75f
                                for (i in 0..6) {
                                    val rowY = coilYStart + i * (coilYEnd - coilYStart) / 6f
                                    drawLine(Color(0xFFD84315), start = Offset(cx - 5f, rowY), end = Offset(cx + 5f, rowY), strokeWidth = 2.2f)
                                }
                                
                                val pulse = (kotlin.math.sin((System.currentTimeMillis() % 1000) / 1000f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                val glowColor = if (component.isPowered) Color(0xFFE040FB) else Color(0xFF00E5FF).copy(alpha = 0.4f + 0.5f * pulse)
                                // Top high-voltage primary aluminum sphere
                                drawCircle(glowColor, radius = cellSize * 0.24f, center = Offset(cx, cy))
                                drawCircle(Color(0xFFECEFF1), radius = cellSize * 0.14f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.06f, center = Offset(cx, cy))
                                
                                if (component.isPowered) {
                                    // Active branching high-frequency plasma spark arcs!
                                    drawLine(Color.White, start = Offset(cx, cy), end = Offset(cx - cellSize*0.48f, cy + cellSize*0.12f), strokeWidth = 2.5f)
                                    drawLine(Color.White, start = Offset(cx, cy), end = Offset(cx + cellSize*0.48f, cy - cellSize*0.18f), strokeWidth = 2.5f)
                                    drawCircle(Color(0xFFE040FB), radius = 3.5f, center = Offset(cx - cellSize*0.48f, cy + cellSize*0.12f))
                                    drawCircle(Color(0xFFE040FB), radius = 3.5f, center = Offset(cx + cellSize*0.48f, cy - cellSize*0.18f))
                                }
                            }
                            ComponentType.MERCURY -> {
                                val pulse = (kotlin.math.sin((System.currentTimeMillis() % 1200) / 1200f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                // Sealed quartz mercury pool container glass backing
                                drawRect(Color(0xFF212121), size = Size(cellSize, cellSize))
                                drawRect(Color(0xFF37474F), topLeft = Offset(2f, 2f), size = Size(cellSize - 4f, cellSize - 4f), style = Stroke(2f))
                                
                                // Beautiful molten 3D liquid mercury droplet pool
                                val mercuryColor = Color(0xFFECEFF1)
                                drawCircle(mercuryColor, radius = cellSize * (0.33f + 0.04f * pulse), center = Offset(cx, cy))
                                // Specular bright liquid shine curves
                                drawCircle(Color.White, radius = cellSize * (0.16f + 0.02f * pulse), center = Offset(cx - cellSize*0.12f, cy - cellSize*0.1f))
                                // Stray floating micro-droplets
                                drawCircle(Color(0xFFB0BEC5), radius = cellSize * 0.08f, center = Offset(cx + cellSize*0.22f, cy + cellSize*0.22f))
                                drawCircle(Color(0xFFB0BEC5), radius = cellSize * 0.05f, center = Offset(cx - cellSize*0.25f, cy + cellSize*0.2f))
                            }
                            ComponentType.LIGHTNING_ROD -> {
                                // Earth grounding tower line
                                drawLine(Color(0xFF90A4AE), start = Offset(cx, cellSize), end = Offset(cx, cellSize * 0.16f), strokeWidth = 4f)
                                // Top heavy copper ion emitter sphere
                                drawCircle(Color(0xFFFFC107), radius = cellSize * 0.14f, center = Offset(cx, cellSize * 0.16f))
                                drawCircle(Color.White, radius = cellSize * 0.06f, center = Offset(cx, cellSize * 0.16f))
                                
                                if (component.isPowered) {
                                    val pulse = (System.currentTimeMillis() % 300) / 300f
                                    // High emission electric field branches
                                    drawLine(Color(0xFF00E5FF), start = Offset(cx, cellSize * 0.16f), end = Offset(cx - cellSize * 0.48f * pulse, cellSize * 0.42f), strokeWidth = 2.5f)
                                    drawLine(Color(0xFF00E5FF), start = Offset(cx, cellSize * 0.16f), end = Offset(cx + cellSize * 0.48f * pulse, cellSize * 0.42f), strokeWidth = 2.5f)
                                    drawCircle(Color.White, radius = 3f, center = Offset(cx - cellSize * 0.48f * pulse, cellSize * 0.42f))
                                    drawCircle(Color.White, radius = 3f, center = Offset(cx + cellSize * 0.48f * pulse, cellSize * 0.42f))
                                }
                            }
                            ComponentType.STIRLING_ENGINE -> {
                                // Cylinder ring outline base
                                drawCircle(Color(0xFF546E7A), radius = cellSize * 0.40f, center = Offset(cx, cy), style = Stroke(3.5f))
                                val angle = ((System.currentTimeMillis() % 1400) / 1400f) * 2f * Math.PI
                                val endX = cx + (cellSize * 0.35f * Math.cos(angle)).toFloat()
                                val endY = cy + (cellSize * 0.35f * Math.sin(angle)).toFloat()
                                
                                // Piston con-rod running to crank
                                drawLine(Color(0xFFB0BEC5), start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = 4f)
                                // Brass crank counterweight
                                drawCircle(Color(0xFFFFB300), radius = cellSize * 0.16f, center = Offset(cx, cy))
                                // Ruby crankshaft pin pivot
                                drawCircle(Color(0xFFD81B60), radius = cellSize * 0.07f, center = Offset(cx, cy))
                                
                                // Thermal burner indicator dot
                                if (component.isPowered) {
                                    drawCircle(Color(0xFFFF3D00), radius = 5f, center = Offset(cx + cellSize*0.28f, cy - cellSize*0.28f))
                                }
                            }
                            ComponentType.QUANTUM_SUPERCONDUCTOR -> {
                                val isCold = component.temperature < -150f
                                val railColor = if (isCold) Color(0xFF00E5FF) else Color(0x6600838F)
                                // Double quantum-flux locking rails
                                drawLine(railColor, start = Offset(0f, cy - 4f), end = Offset(cellSize, cy - 4f), strokeWidth = 2.5f)
                                drawLine(railColor, start = Offset(0f, cy + 4f), end = Offset(cellSize, cy + 4f), strokeWidth = 2.5f)
                                
                                if (isCold) {
                                    // Quantum-locked floating sapphire magnet disc
                                    drawCircle(Color(0xFF00B0FF), radius = cellSize * 0.11f, center = Offset(cx, cy - 12f))
                                    drawCircle(Color.White, radius = cellSize * 0.05f, center = Offset(cx, cy - 12f))
                                    if (component.isPowered) {
                                        // Meissner effect freezing fog cloud aura
                                        drawCircle(Color(0x7700E5FF), radius = cellSize * 0.38f, center = Offset(cx, cy))
                                    }
                                }
                            }
                            ComponentType.PCM_CELL -> {
                                // Glass vacuum heat insulator module frame
                                drawRect(Color(0xFF006064), topLeft = Offset(cellSize*0.08f, cellSize*0.08f), size = Size(cellSize*0.84f, cellSize*0.84f))
                                drawRect(Color(0xFFE0F7FA), topLeft = Offset(cellSize*0.14f, cellSize*0.14f), size = Size(cellSize*0.72f, cellSize*0.72f), style = Stroke(1.8f))
                                
                                // Dynamic cellular crystalline state lattice transitions
                                val statePulse = (kotlin.math.sin((System.currentTimeMillis() % 2000) / 2000f * 2.0 * java.lang.Math.PI).toFloat() + 1f) / 2f
                                val gridColor = if (component.temperature > 50f) Color(0xFFF44336) else Color(0xFF00838F)
                                drawLine(gridColor, start = Offset(cellSize*0.18f, cellSize*0.18f), end = Offset(cellSize*0.82f, cellSize*0.82f), strokeWidth = 2.2f * statePulse)
                                drawLine(gridColor, start = Offset(cellSize*0.82f, cellSize*0.18f), end = Offset(cellSize*0.18f, cellSize*0.82f), strokeWidth = 2.2f * statePulse)
                                
                                drawCircle(Color.White, radius = 3.5f, center = Offset(cx, cy))
                            }
                            ComponentType.LASER_RECEIVER -> {
                                // Industrial chassis frame with focus optics base
                                drawRect(Color(0xFF283593), topLeft = Offset(cellSize*0.14f, cellSize*0.14f), size = Size(cellSize*0.72f, cellSize*0.72f))
                                drawRect(Color(0xFFFFD54F), topLeft = Offset(cellSize*0.14f, cellSize*0.14f), size = Size(cellSize*0.72f, cellSize*0.72f), style = Stroke(1.8f))
                                
                                val eyeColor = if (component.isPowered) Color(0xFFD50000) else Color(0xFF0D47A1)
                                // Central focal sapphire alignment aperture prism
                                drawCircle(eyeColor, radius = cellSize * 0.22f, center = Offset(cx, cy))
                                drawCircle(Color.White, radius = cellSize * 0.09f, center = Offset(cx, cy))
                                
                                // Laser safety alignment reticle pins
                                drawLine(Color(0xDDFF1744), start = Offset(cx - cellSize*0.3f, cy), end = Offset(cx - cellSize*0.15f, cy), strokeWidth = 1.2f)
                                drawLine(Color(0xDDFF1744), start = Offset(cx + cellSize*0.15f, cy), end = Offset(cx + cellSize*0.3f, cy), strokeWidth = 1.2f)
                            }
                            ComponentType.GRAPHITE_ROD -> {
                                drawRect(Color(0xFF212121), topLeft = Offset(cellSize*0.25f, 0f), size = Size(cellSize*0.5f, cellSize))
                                for (i in 1..4) {
                                    val yPos = i * (cellSize / 5f)
                                    drawLine(Color(0xFF757575), start = Offset(cellSize*0.25f, yPos), end = Offset(cellSize*0.75f, yPos), strokeWidth = 1.5f)
                                }
                            }
                            ComponentType.PIEZO_SENSOR -> {
                                drawRect(Color(0xFF004D40), topLeft = Offset(cellSize*0.1f, cellSize*0.1f), size = Size(cellSize*0.8f, cellSize*0.8f))
                                drawRect(Color(0xFF009688), topLeft = Offset(cellSize*0.25f, cellSize*0.25f), size = Size(cellSize*0.5f, cellSize*0.5f))
                                if (component.logicState) {
                                    drawCircle(Color(0xFFEEFF41), radius = cellSize * 0.12f, center = Offset(cx, cy))
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
                                drawRect(metalShineBrush, size = Size(cellSize, cellSize))
                            }
                        }

                        if (type == ComponentType.INFINITE_WATER || 
                            type == ComponentType.INFINITE_LAVA ||
                            type == ComponentType.INFINITE_OIL ||
                            type == ComponentType.INFINITE_ACID ||
                            type == ComponentType.INFINITE_SLIME ||
                            type == ComponentType.INFINITE_GASOLINE ||
                            type == ComponentType.INFINITE_LIQUID_NITROGEN ||
                            type == ComponentType.INFINITE_STEAM) {
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
