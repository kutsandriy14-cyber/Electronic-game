package com.example.engine

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.Telemetry

object EnergyEngine {

    data class EnergyResult(val grid: Array<Array<GridComponent>>, val telemetry: Telemetry)

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

    private val queueBuffer = ArrayDeque<Pair<Int, Int>>()
    private val enqueuedBuffer = mutableSetOf<Pair<Int, Int>>()
    private val batteriesBuffer = mutableListOf<Pair<Int, Int>>()
    private val poweredBuffer = mutableSetOf<Pair<Int, Int>>()

    fun simulateEnergyFlow(grid: Array<Array<GridComponent>>, width: Int, height: Int, activeScriptsCount: Int = 0): EnergyResult {
        queueBuffer.clear()
        enqueuedBuffer.clear()
        batteriesBuffer.clear()
        poweredBuffer.clear()

        val powered = poweredBuffer
        val batteries = batteriesBuffer
        var totalSources = 0
        var calculatedTotalVoltage = 0f
        var calculatedTotalResistance = 1f 

        // Initial scan for sources and voltage sum
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
                            calculatedTotalVoltage += currentV
                        }
                    }
                } else if (comp.type == ComponentType.PULSE_GENERATOR && comp.logicState) {
                    batteries.add(Pair(x, y)) 
                    totalSources++
                }
            }
        }

        val queue = queueBuffer
        val enqueued = enqueuedBuffer
        queue.addAll(batteries)
        enqueued.addAll(batteries)
        
        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if(powered.contains(curr)) continue
            
            if(grid[curr.first][curr.second].isOverloaded) continue
            
            powered.add(curr)
            
            val type = grid[curr.first][curr.second].type
            val extraProps = parseProps(grid[curr.first][curr.second].extraData)
            calculatedTotalResistance += addDynamicResistanceForComponent(type, extraProps)

            for (i in 0..3) {
                val nx = curr.first + dxs[i]
                val ny = curr.second + dys[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val nComp = grid[nx][ny]
                    if (nComp.isOverloaded) continue 
                    val n = Pair(nx, ny)
                    if (canPassPower(nComp, curr.first, curr.second, nx, ny) && !enqueued.contains(n)) {
                        enqueued.add(n)
                        queue.add(n)
                    }
                }
            }
        }
        
        var totalConductance = 0f
        
        for (p in powered) {
             val comp = grid[p.first][p.second]
             val props = parseProps(comp.extraData)
             val r = addDynamicResistanceForComponent(comp.type, props)
             if (r > 5f) {
                 totalConductance += 1f / r
             }
             if (comp.type == ComponentType.RESISTOR) {
                 val rResistor = props["r"]?.toFloatOrNull() ?: 330f
                 totalConductance += 1f / rResistor
             }
             if (comp.type == ComponentType.MICROCONTROLLER || comp.type == ComponentType.MEMORY_RAM || comp.type == ComponentType.MEMORY_ROM) {
                 val cores = props["cores"]?.toIntOrNull() ?: 1
                 val freq = props["mhz"]?.toFloatOrNull() ?: 16f
                 val mem = props["mem_kb"]?.toIntOrNull() ?: 2
                 val loadR = 1000f / (cores * (freq / 16f) * (mem / 2f).coerceAtLeast(1f))
                 totalConductance += 1f / loadR.coerceAtLeast(10f)
             }
        }


        val wireResistance = 0.5f // Base wire and internal resistance
        val effectiveR = if (totalConductance > 0f) (1f / totalConductance) + wireResistance else wireResistance
        
        var current = 0f
        var power = 0f
        var shortCircuit = false
        
        if (totalSources > 0) {
            current = (calculatedTotalVoltage / effectiveR) * 1000f
            if (totalConductance == 0f && powered.size > totalSources) {
                shortCircuit = true
            }
            power = calculatedTotalVoltage * (current / 1000f) 
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val wasPowered = powered.contains(Pair(x, y))
                val comp = grid[x][y]
                
                var finalLogicState = comp.logicState
                var finalCharge = comp.charge
                var finalOverloaded = comp.isOverloaded
                
                if (comp.type == ComponentType.RELAY) {
                     finalLogicState = wasPowered
                }
                
                if (wasPowered && current > 500000f) {
                    val burnChance = (current - 500000f) / 1000000f
                    if (Math.random() < burnChance) {
                        finalOverloaded = true 
                    }
                }
                
                // Battery Drain
                if (wasPowered && comp.charge > 0f) {
                    if (comp.type == ComponentType.BATTERY || comp.type == ComponentType.BATTERY_PACK || comp.type == ComponentType.COIN_CELL) {
                        val drainShare = if (totalSources > 0) current / totalSources else current
                        val mA_drain = drainShare * 0.0001f // Reduced drain as requested
                        finalCharge = (comp.charge - mA_drain).coerceAtLeast(0f)
                    }
                }
                
                if (comp.isPowered != (wasPowered && !finalOverloaded) || 
                    comp.logicState != finalLogicState || 
                    comp.charge != finalCharge || 
                    comp.isOverloaded != finalOverloaded) {
                    grid[x][y] = comp.copy(isPowered = wasPowered && !finalOverloaded, logicState = finalLogicState, charge = finalCharge, isOverloaded = finalOverloaded)
                }
            }
        }
        
        val telemetry = Telemetry(calculatedTotalVoltage, current, power, shortCircuit, activeScriptsCount)
        return EnergyResult(grid, telemetry)
    }
    
    fun addDynamicResistanceForComponent(type: ComponentType, props: Map<String, String>): Float {
        return when (type) {
            ComponentType.WIRE_ANY, ComponentType.SUPERCONDUCTOR, ComponentType.HIGH_VOLTAGE_CABLE, ComponentType.FIBER_OPTIC -> 0.01f 
            ComponentType.BULB, ComponentType.LED, ComponentType.RGB_LED, ComponentType.SEVEN_SEGMENT, ComponentType.DISPLAY_PIXEL -> props["r"]?.toFloatOrNull() ?: 50f
            ComponentType.LASER_DIODE, ComponentType.HEATER, ComponentType.FIRE -> 20f
            ComponentType.COOLER, ComponentType.WATER_PUMP, ComponentType.FAN -> 30f
            ComponentType.MOTOR, ComponentType.SERVO_MOTOR, ComponentType.STEPPER_MOTOR -> 40f
            ComponentType.VIBRATION_MOTOR, ComponentType.LINEAR_ACTUATOR, ComponentType.SOLENOID -> 45f
            ComponentType.SPEAKER, ComponentType.BUZZER, ComponentType.AMPLIFIER -> 15f
            ComponentType.DISPLAY_7SEG_4DIGIT, ComponentType.DISPLAY_OLED_128X64, ComponentType.DISPLAY_TFT_24, ComponentType.E_PAPER_DISPLAY -> 60f
            ComponentType.MICROCONTROLLER, ComponentType.MEMORY_RAM, ComponentType.MEMORY_ROM -> 100f
            ComponentType.IC_7400_NAND, ComponentType.IC_7408_AND, ComponentType.IC_7432_OR -> 80f
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
            ComponentType.INFINITE_BATTERY -> 999f
            ComponentType.NUCLEAR_REACTOR -> 0f // not a direct source anymore
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
