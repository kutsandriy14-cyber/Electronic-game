package com.example.engine

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.Telemetry
import java.util.PriorityQueue

object EnergyEngine {

    data class EnergyResult(val grid: Array<Array<GridComponent>>, val telemetry: Telemetry)

    private class PathNode(val x: Int, val y: Int, val resistance: Float) : Comparable<PathNode> {
        override fun compareTo(other: PathNode): Int {
            return this.resistance.compareTo(other.resistance)
        }
    }

    fun parseProps(data: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (data.isBlank()) return map
        data.split("|").forEach {
            val parts = it.split("=")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    private fun canPassPower(component: GridComponent, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val type = component.type
        if (type == ComponentType.EMPTY || type == ComponentType.SWITCH_OPEN || type.category.name == "TOOLS") return false
        
        if (type == ComponentType.PUSH_BUTTON && !component.logicState) return false
        
        if (type == ComponentType.RELAY && !component.logicState) return false
        if (type == ComponentType.TRANSISTOR && !component.logicState) return false
        
        if (type == ComponentType.DIODE) {
            val dir = component.direction
            if (fromX < toX && dir == Direction.RIGHT) return true
            if (fromX > toX && dir == Direction.LEFT) return true
            if (fromY < toY && dir == Direction.DOWN) return true
            if (fromY > toY && dir == Direction.UP) return true
            return false
        }
        
        return true
    }

    fun simulateEnergyFlow(grid: Array<Array<GridComponent>>, width: Int, height: Int, activeScriptsCount: Int = 0): EnergyResult {
        // No thread-unsafe shared buffers anymore. We initialize everything locally.
        val pq = PriorityQueue<PathNode>()
        val minResistance = Array(width) { FloatArray(height) { Float.MAX_VALUE } }
        val primarySource = Array(width) { Array<Pair<Int, Int>?>(height) { null } }
        
        val batteries = mutableListOf<Pair<Int, Int>>()
        var totalSources = 0
        var totalSourceVoltageSum = 0f

        // 1. Initial scan for active power sources
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (isPowerSource(comp.type)) {
                    var isValidSource = false
                    if (comp.type == ComponentType.GENERATOR) {
                        // Check if adjacent to steam
                        val dxs = intArrayOf(-1, 1, 0, 0)
                        val dys = intArrayOf(0, 0, -1, 1)
                        for (i in 0..3) {
                            val nx = x + dxs[i]; val ny = y + dys[i]
                            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.STEAM) {
                                isValidSource = true; break
                            }
                        }
                    } else {
                        if (comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.SOLAR_PANEL || comp.type == ComponentType.WIND_TURBINE || comp.type == ComponentType.GEOTHERMAL_GENERATOR || comp.type == ComponentType.HYDRO_GENERATOR || comp.type == ComponentType.THERMOELECTRIC_GENERATOR || comp.type == ComponentType.INFINITE_BATTERY || comp.charge > 0) {
                            isValidSource = true
                        }
                    }
                    
                    if (isValidSource) {
                        batteries.add(Pair(x, y))
                        totalSources++
                        
                        val props = parseProps(comp.extraData)
                        val maxV = props["v"]?.toFloatOrNull() ?: getDefaultVoltage(comp.type)
                        val maxCharge = props["c"]?.toFloatOrNull() ?: getDefaultCapacity(comp.type)
                        
                        val currentV = if (comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INFINITE_BATTERY) maxV else maxV * (comp.charge / maxCharge).coerceIn(0f, 1f)
                        if (currentV > 0.1f) {
                            totalSourceVoltageSum += currentV
                            // Set up starting Dijkstra nodes
                            minResistance[x][y] = 0f
                            primarySource[x][y] = Pair(x, y)
                            pq.add(PathNode(x, y, 0f))
                        }
                    }
                } else if (comp.type == ComponentType.PULSE_GENERATOR && comp.logicState) {
                    batteries.add(Pair(x, y)) 
                    totalSources++
                    minResistance[x][y] = 0f
                    primarySource[x][y] = Pair(x, y)
                    pq.add(PathNode(x, y, 0f))
                }
            }
        }

        // 2. Run Dijkstra's path-finding algorithm for cumulative path resistance
        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)

        while (pq.isNotEmpty()) {
            val curr = pq.poll()
            val cx = curr.x
            val cy = curr.y
            
            if (curr.resistance > minResistance[cx][cy]) continue
            if (grid[cx][cy].isOverloaded) continue

            for (i in 0..3) {
                val nx = cx + dxs[i]
                val ny = cy + dys[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val nComp = grid[nx][ny]
                    if (nComp.isOverloaded) continue
                    if (canPassPower(nComp, cx, cy, nx, ny)) {
                        val properties = parseProps(nComp.extraData)
                        val rNeigh = addDynamicResistanceForComponent(nComp.type, properties)
                        val newR = curr.resistance + rNeigh
                        if (newR < minResistance[nx][ny]) {
                            minResistance[nx][ny] = newR
                            primarySource[nx][ny] = primarySource[cx][cy]
                            pq.add(PathNode(nx, ny, newR))
                        }
                    }
                }
            }
        }

        // 3. Evaluate local voltage, current, and load metrics for all grid nodes
        var totalCurrentDrawnSum = 0f
        var hasPoweredWires = false
        val averagePowerSourceVoltage = if (totalSources > 0) totalSourceVoltageSum / totalSources else 9f

        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                val pathR = minResistance[x][y]
                val wasPowered = pathR < Float.MAX_VALUE
                
                var vLocal = 0f
                var currentMa = 0f
                val rLocal = addDynamicResistanceForComponent(comp.type, parseProps(comp.extraData))
                
                if (wasPowered) {
                    if (comp.type.category == com.example.model.ComponentCategory.CONDUCTORS || comp.type.category == com.example.model.ComponentCategory.SWITCHES) {
                        hasPoweredWires = true
                    }
                    val sourceCoords = primarySource[x][y]
                    if (sourceCoords != null) {
                        val sComp = grid[sourceCoords.first][sourceCoords.second]
                        val sProps = parseProps(sComp.extraData)
                        val maxV = sProps["v"]?.toFloatOrNull() ?: getDefaultVoltage(sComp.type)
                        val maxCharge = sProps["c"]?.toFloatOrNull() ?: getDefaultCapacity(sComp.type)
                        val sourceV = if (sComp.type == ComponentType.AC_SOURCE || sComp.type == ComponentType.INFINITE_BATTERY) maxV else maxV * (sComp.charge / maxCharge).coerceIn(0f, 1f)
                        
                        val rPath = (pathR - rLocal).coerceAtLeast(0f)
                        val rSeries = (rPath + rLocal).coerceAtLeast(0.1f)
                        
                        currentMa = (sourceV / rSeries) * 1000f
                        vLocal = sourceV * (rLocal / rSeries)
                        
                        // Accumulate current if the component is an active load
                        if (isLoad(comp.type) || comp.type == ComponentType.RESISTOR || comp.type.category == com.example.model.ComponentCategory.SENSORS || comp.type.category == com.example.model.ComponentCategory.LOGIC || comp.type.category == com.example.model.ComponentCategory.ANALOG_ICS || comp.type.category == com.example.model.ComponentCategory.ADVANCED) {
                            totalCurrentDrawnSum += currentMa
                        }
                    }
                }

                // 4. Update the logic states, charge decay, and overloaded/burned conditions
                var finalLogicState = comp.logicState
                var finalCharge = comp.charge
                var finalOverloaded = comp.isOverloaded
                
                if (comp.type == ComponentType.RELAY) {
                     finalLogicState = wasPowered
                }
                
                // Realistic dynamic overload/burning based on exact physics
                if (wasPowered && !finalOverloaded) {
                    if (comp.type == ComponentType.LED || comp.type == ComponentType.RGB_LED) {
                        if (vLocal > 3.8f || currentMa > 60f) {
                            if (Math.random() < 0.2) finalOverloaded = true
                        }
                    } else if (comp.type == ComponentType.MICROCONTROLLER || comp.type.category == com.example.model.ComponentCategory.LOGIC || comp.type.category == com.example.model.ComponentCategory.ANALOG_ICS) {
                        if (vLocal > 6.0f) {
                            if (Math.random() < 0.25) finalOverloaded = true
                        }
                    } else if (comp.type == ComponentType.RESISTOR) {
                        val powerMw = vLocal * currentMa
                        if (powerMw > 2000f) { // 2W max on tiny component resistor
                            if (Math.random() < 0.1) finalOverloaded = true
                        }
                    } else if (comp.type == ComponentType.BULB) {
                        if (vLocal > 150f) {
                            if (Math.random() < 0.15) finalOverloaded = true
                        }
                    } else if (comp.type == ComponentType.MOTOR || comp.type == ComponentType.FAN || comp.type == ComponentType.HEATER || comp.type == ComponentType.COOLER) {
                        if (vLocal > 30f) {
                            if (Math.random() < 0.1) finalOverloaded = true
                        }
                    } else {
                        // Standard component cabling / wires
                        if (currentMa > 12000f) { // 12 Amps limit for standard lines
                            if (Math.random() < 0.05) finalOverloaded = true
                        }
                    }
                }
                
                // Battery drain decay
                if (wasPowered && comp.charge > 0f) {
                    if (comp.type == ComponentType.BATTERY || comp.type == ComponentType.BATTERY_PACK || comp.type == ComponentType.COIN_CELL) {
                        val drainShare = if (totalSources > 0) totalCurrentDrawnSum / totalSources else totalCurrentDrawnSum
                        val mA_drain = drainShare * 0.000001f
                        finalCharge = (comp.charge - mA_drain).coerceAtLeast(0f)
                    }
                }
                
                if (comp.isPowered != (wasPowered && !finalOverloaded) || 
                    comp.logicState != finalLogicState || 
                    comp.charge != finalCharge || 
                    comp.isOverloaded != finalOverloaded ||
                    comp.voltage != vLocal ||
                    comp.current != currentMa ||
                    comp.resistance != rLocal) {
                    grid[x][y] = comp.copy(
                        isPowered = wasPowered && !finalOverloaded, 
                        logicState = finalLogicState, 
                        charge = finalCharge, 
                        isOverloaded = finalOverloaded,
                        voltage = if (wasPowered && !finalOverloaded) vLocal else 0f,
                        current = if (wasPowered && !finalOverloaded) currentMa else 0f,
                        resistance = rLocal
                    )
                }
            }
        }
        
        // 5. Short circuit check & Telemetry metrics building
        var isShort = false
        if (totalSources > 0) {
            // Highly realistic short circuit check: if current exceeds 100 Amps, OR we have powered conductors but zero active loads
            if (totalCurrentDrawnSum > 100000f || (hasPoweredWires && totalCurrentDrawnSum < 0.1f)) {
                isShort = true
                if (totalCurrentDrawnSum < 0.1f) {
                    totalCurrentDrawnSum = (averagePowerSourceVoltage / 0.1f) * 1000f // max short circuit Amps
                }
            }
        }
        
        val totalPowerWatts = (averagePowerSourceVoltage * (totalCurrentDrawnSum / 1000f))
        
        val telemetry = Telemetry(
            totalVoltage = averagePowerSourceVoltage, 
            totalCurrent = totalCurrentDrawnSum, 
            totalPower = totalPowerWatts, 
            isShortCircuit = isShort, 
            runningScripts = activeScriptsCount
        )
        return EnergyResult(grid, telemetry)
    }
    
    fun addDynamicResistanceForComponent(type: ComponentType, props: Map<String, String>): Float {
        return when (type) {
            ComponentType.WIRE_ANY, ComponentType.SUPERCONDUCTOR, ComponentType.HIGH_VOLTAGE_CABLE, ComponentType.FIBER_OPTIC -> 0.05f 
            ComponentType.RESISTOR -> props["r"]?.toFloatOrNull() ?: 330f
            ComponentType.POTENTIOMETER -> props["r"]?.toFloatOrNull() ?: 1000f
            ComponentType.BULB -> props["r"]?.toFloatOrNull() ?: 100f
            ComponentType.LED, ComponentType.RGB_LED -> 100f
            ComponentType.SEVEN_SEGMENT, ComponentType.DISPLAY_PIXEL -> 150f
            ComponentType.LASER_DIODE, ComponentType.HEATER, ComponentType.FIRE -> 20f
            ComponentType.COOLER, ComponentType.WATER_PUMP, ComponentType.FAN -> 30f
            ComponentType.MOTOR, ComponentType.SERVO_MOTOR, ComponentType.STEPPER_MOTOR -> 40f
            ComponentType.VIBRATION_MOTOR, ComponentType.LINEAR_ACTUATOR, ComponentType.SOLENOID -> 45f
            ComponentType.SPEAKER, ComponentType.BUZZER, ComponentType.AMPLIFIER -> 15f
            ComponentType.DISPLAY_7SEG_4DIGIT, ComponentType.DISPLAY_OLED_128X64, ComponentType.DISPLAY_TFT_24, ComponentType.E_PAPER_DISPLAY -> 60f
            ComponentType.MICROCONTROLLER, ComponentType.MEMORY_RAM, ComponentType.MEMORY_ROM -> 300f
            ComponentType.IC_7400_NAND, ComponentType.IC_7408_AND, ComponentType.IC_7432_OR -> 400f
            else -> 0.5f 
        }
    }

    fun isPowerSource(type: ComponentType): Boolean {
        return type in listOf(
            ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, 
            ComponentType.AC_SOURCE, ComponentType.WIND_TURBINE,  
            ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, 
            ComponentType.THERMOELECTRIC_GENERATOR, ComponentType.INFINITE_BATTERY,
            ComponentType.GENERATOR, ComponentType.SOLAR_PANEL
        )
    }
    
    fun isLoad(type: ComponentType): Boolean {
        return type in listOf(
            ComponentType.BULB, ComponentType.LED, ComponentType.RGB_LED, ComponentType.MOTOR, ComponentType.HEATER,
            ComponentType.COOLER, ComponentType.SPEAKER, ComponentType.BUZZER, ComponentType.LASER_DIODE,
            ComponentType.WATER_PUMP, ComponentType.FAN, ComponentType.DISPLAY_PIXEL
        )
    }

    fun getDefaultVoltage(type: ComponentType): Float {
        return when (type) {
            ComponentType.COIN_CELL -> 3f
            ComponentType.BATTERY -> 9f
            ComponentType.BATTERY_PACK -> 12f
            ComponentType.AC_SOURCE -> 120f
            ComponentType.SOLAR_PANEL -> 3f
            ComponentType.GENERATOR -> 12f
            ComponentType.WIND_TURBINE -> 6f
            ComponentType.GEOTHERMAL_GENERATOR -> 24f
            ComponentType.HYDRO_GENERATOR -> 18f
            ComponentType.THERMOELECTRIC_GENERATOR -> 3f
            ComponentType.INFINITE_BATTERY -> 220f
            ComponentType.NUCLEAR_REACTOR -> 0f
            else -> 9f
        }
    }

    fun getDefaultCapacity(type: ComponentType): Float {
        return when (type) {
            ComponentType.COIN_CELL -> 100f
            ComponentType.BATTERY -> 1000f
            ComponentType.BATTERY_PACK -> 5000f
            ComponentType.GENERATOR -> 2000f
            ComponentType.SOLAR_PANEL -> 20f
            ComponentType.WIND_TURBINE -> 50f
            ComponentType.NUCLEAR_REACTOR -> 0f
            ComponentType.GEOTHERMAL_GENERATOR -> 1000f
            ComponentType.HYDRO_GENERATOR -> 500f
            ComponentType.THERMOELECTRIC_GENERATOR -> 10f
            ComponentType.INFINITE_BATTERY -> 9999999f
            else -> 1000f
        }
    }
}
