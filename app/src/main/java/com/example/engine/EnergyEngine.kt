package com.example.engine

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.Telemetry
import com.example.functional.*
import java.util.PriorityQueue

object EnergyEngine {

    data class EnergyResult(val grid: Array<Array<GridComponent>>, val telemetry: Telemetry)

    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()
    private fun randFloat() = rng.get()!!.nextFloat()

    private class PathNode(val x: Int, val y: Int, val resistance: Float) : Comparable<PathNode> {
        override fun compareTo(other: PathNode): Int {
            return this.resistance.compareTo(other.resistance)
        }
    }

    private fun canPassPower(component: GridComponent, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val type = component.type
        if (type == ComponentType.EMPTY || type.category.name == "TOOLS") return false
        if (FiberOptic.isFiberOptic(type)) return false
        
        if (PushButton.isPushButton(type) && !component.logicState) return false
        if (PressurePad.isMechanicalSensor(type) && !component.logicState) return false
        if (Sensors.isSensor(type) && type == ComponentType.PRESSURE_SENSOR && !component.logicState) return false
        
        if (Switch.isSwitch(type) && !Switch.isConducting(type, component.logicState)) return false
        if (Transistor.isTransistor(type) && !component.logicState) return false
        
        if (Diode.isDiode(type) || ZenerDiode.isZenerDiode(type)) {
            val dir = component.direction
            if (fromX < toX && dir == Direction.RIGHT) return true
            if (fromX > toX && dir == Direction.LEFT) return true
            if (fromY < toY && dir == Direction.DOWN) return true
            if (fromY > toY && dir == Direction.UP) return true
            
            // If reverse voltage on Zener diode is above breakdown, it conducts backwards
            if (ZenerDiode.isZenerDiode(type) && component.voltage > ZenerDiode.getBreakdownVoltage()) {
                return true
            }
            return false
        }
        
        return true
    }

    private fun isConnectedToGenerator(grid: Array<Array<GridComponent>>, startX: Int, startY: Int, width: Int, height: Int): Pair<Int, Int>? {
        val queue = java.util.ArrayDeque<Pair<Int, Int>>()
        val visited = Array(width) { BooleanArray(height) }
        
        queue.add(Pair(startX, startY))
        visited[startX][startY] = true
        
        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.poll()
            
            // If we found an active generator (which is not a battery or coin cell)
            val comp = grid[cx][cy]
            if (Generator.isGenerator(comp.type) || NuclearReactor.isNuclearReactor(comp.type)) {
                // Verify if it is active
                var isActive = false
                if (comp.type == ComponentType.GENERATOR) {
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = cx + dxs[i]; val ny = cy + dys[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.STEAM) {
                            isActive = true; break
                        }
                    }
                } else if (NuclearReactor.isNuclearReactor(comp.type)) {
                    var hasWater = false; var hasUranium = false
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = cx + dxs[i]; val ny = cy + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            if (grid[nx][ny].type == ComponentType.WATER) hasWater = true
                            if (grid[nx][ny].type == ComponentType.URANIUM) hasUranium = true
                        }
                    }
                    isActive = hasWater && hasUranium && comp.temperature < NuclearReactor.getMeltdownTemperature()
                } else if (GeothermalGenerator.isGeothermalGenerator(comp.type)) {
                    var hasHeat = comp.temperature > 100f
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = cx + dxs[i]; val ny = cy + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val adj = grid[nx][ny].type
                            if (Lava.isLava(adj) || adj == ComponentType.FIRE || grid[nx][ny].temperature >= GeothermalGenerator.getIdealOperatingTemp()) {
                                        hasHeat = true
                            }
                        }
                    }
                    isActive = hasHeat
                } else if (HydroGenerator.isHydroGenerator(comp.type)) {
                    var hasWater = false
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = cx + dxs[i]; val ny = cy + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val adj = grid[nx][ny].type
                            if (adj == ComponentType.WATER || adj == ComponentType.INFINITE_WATER) {
                                hasWater = true
                            }
                        }
                    }
                    isActive = hasWater
                } else if (SolarPanel.isSolarPanel(comp.type)) {
                    var hasLight = true
                    if (cy > height * 0.75) {
                        hasLight = false
                        val dxs = intArrayOf(-1, 1, 0, 0)
                        val dys = intArrayOf(0, 0, -1, 1)
                        for (i in 0..3) {
                            val nx = cx + dxs[i]; val ny = cy + dys[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val adj = grid[nx][ny]
                                if (adj.isPowered && Leds.isLuminous(adj.type)) {
                                    hasLight = true
                                }
                            }
                        }
                    }
                    isActive = hasLight
                } else {
                    isActive = true // AC source, Wind Turbine etc always active when placed
                }
                
                if (isActive) {
                    return Pair(cx, cy)
                }
            }
            
            val dxs = intArrayOf(-1, 1, 0, 0)
            val dys = intArrayOf(0, 0, -1, 1)
            for (i in 0..3) {
                val nx = cx + dxs[i]
                val ny = cy + dys[i]
                if (nx in 0 until width && ny in 0 until height && !visited[nx][ny]) {
                    if (canPassPower(grid[nx][ny], cx, cy, nx, ny)) {
                        visited[nx][ny] = true
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }
        return null
    }

    fun simulateEnergyFlow(grid: Array<Array<GridComponent>>, width: Int, height: Int, activeScriptsCount: Int = 0): EnergyResult {
        val pq = PriorityQueue<PathNode>()
        val minResistance = Array(width) { FloatArray(height) { Float.MAX_VALUE } }
        val primarySource = Array(width) { Array<Pair<Int, Int>?>(height) { null } }
        val voltageMultiplier = Array(width) { FloatArray(height) { 1f } }
        
        val batteries = mutableListOf<Pair<Int, Int>>()
        var totalSources = 0
        var totalSourceVoltageSum = 0f

        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)

        val propsCache = Array(width) { x -> Array(height) { y -> CircuitUtils.parseProps(grid[x][y].extraData) } }
        val resistanceCache = Array(width) { x -> Array(height) { y -> 
            addDynamicResistanceForComponent(grid, x, y, grid[x][y].type, propsCache[x][y]) 
        }}

        // 1. Initial scan for active power sources
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (isPowerSource(comp.type)) {
                    val maxCap = RenderEngine.getMaxCap(comp)
                    val isBatteryType = Battery.isBattery(comp.type)
                    val currentCharge = if (isBatteryType) {
                        if (comp.charge < 0f || comp.charge.isNaN()) maxCap else comp.charge
                    } else {
                        -1f // Generators don't have finite accumulative charge
                    }
                    if (comp.charge != currentCharge) {
                        grid[x][y] = comp.copy(charge = currentCharge)
                    }
                    
                    var isValidSource = false
                    if (comp.type == ComponentType.GENERATOR) {
                        for (i in 0..3) {
                            val nx = x + dxs[i]; val ny = y + dys[i]
                            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.STEAM) {
                                isValidSource = true; break
                            }
                        }
                    } else if (NuclearReactor.isNuclearReactor(comp.type)) {
                        var hasWater = false; var hasUranium = false
                        for (i in 0..3) {
                            val nx = x + dxs[i]; val ny = y + dys[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                if (grid[nx][ny].type == ComponentType.WATER) hasWater = true
                                if (grid[nx][ny].type == ComponentType.URANIUM) hasUranium = true
                            }
                        }
                        isValidSource = hasWater && hasUranium && comp.temperature < NuclearReactor.getMeltdownTemperature()
                        if (isValidSource) totalSourceVoltageSum += 240f
                    } else if (GeothermalGenerator.isGeothermalGenerator(comp.type)) {
                        var hasHeat = comp.temperature > 100f
                        for (i in 0..3) {
                            val nx = x + dxs[i]; val ny = y + dys[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val adj = grid[nx][ny].type
                                if (Lava.isLava(adj) || adj == ComponentType.FIRE || grid[nx][ny].temperature >= GeothermalGenerator.getIdealOperatingTemp()) {
                                    hasHeat = true
                                }
                            }
                        }
                        isValidSource = hasHeat
                    } else if (HydroGenerator.isHydroGenerator(comp.type)) {
                        var hasWater = false
                        for (i in 0..3) {
                            val nx = x + dxs[i]; val ny = y + dys[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val adj = grid[nx][ny].type
                                if (adj == ComponentType.WATER || adj == ComponentType.INFINITE_WATER) {
                                    hasWater = true
                                }
                            }
                        }
                        isValidSource = hasWater
                    } else if (SolarPanel.isSolarPanel(comp.type)) {
                        var hasLight = true
                        if (y > height * 0.75) {
                            hasLight = false
                            for (i in 0..3) {
                                val nx = x + dxs[i]; val ny = y + dys[i]
                                if (nx in 0 until width && ny in 0 until height) {
                                    val adj = grid[nx][ny]
                                    if (adj.isPowered && Leds.isLuminous(adj.type)) {
                                        hasLight = true
                                    }
                                }
                            }
                        }
                        isValidSource = hasLight
                    } else if (isBatteryType) {
                        val connectedGen = isConnectedToGenerator(grid, x, y, width, height)
                        if (connectedGen == null && currentCharge > 0.05f) {
                            isValidSource = true
                        }
                    } else {
                        isValidSource = true
                    }
                    
                    if (isValidSource) {
                        batteries.add(Pair(x, y))
                        totalSources++
                        
                        val maxV = CircuitUtils.propFloat(propsCache[x][y], "v", getDefaultVoltage(comp.type))
                        val currentV = if (isBatteryType) {
                            maxV * (currentCharge / maxCap).coerceIn(0f, 1f)
                        } else {
                            maxV
                        }
                        
                        if (currentV > 0.1f && comp.type != ComponentType.NUCLEAR_REACTOR) {
                            totalSourceVoltageSum += currentV
                        }
                        if (currentV > 0.1f) {
                            minResistance[x][y] = 0f
                            primarySource[x][y] = Pair(x, y)
                            pq.add(PathNode(x, y, 0f))
                        }
                    }
                }
            }
        }

        // 2. Run Dijkstra's path-finding algorithm for cumulative path resistance
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
                        val rNeigh = resistanceCache[nx][ny]
                        val newR = curr.resistance + rNeigh
                        if (newR < minResistance[nx][ny]) {
                            minResistance[nx][ny] = newR
                            primarySource[nx][ny] = primarySource[cx][cy]
                            
                            var mult = voltageMultiplier[cx][cy]
                            if (grid[cx][cy].type == ComponentType.STEP_DOWN_CONVERTER) mult *= CircuitUtils.propFloat(propsCache[cx][cy], "r", 0.5f)
                            if (grid[cx][cy].type == ComponentType.STEP_UP_CONVERTER) mult *= CircuitUtils.propFloat(propsCache[cx][cy], "r", 2.0f)
                            voltageMultiplier[nx][ny] = mult
                            
                            pq.add(PathNode(nx, ny, newR))
                        }
                    }
                }
            }
        }

        // 3. Evaluate local voltage, current, and load metrics for all grid nodes
        var totalCurrentDrawnSum = 0f
        var hasPoweredWires = false
        var isShortCircuit = false
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.EMPTY || comp.type.category.name == "TOOLS") continue

                if (minResistance[x][y] < Float.MAX_VALUE && minResistance[x][y] >= 0f) {
                    val src = primarySource[x][y]
                    var sourceV = 0f
                    if (src != null) {
                        val sComp = grid[src.first][src.second]
                        val sMaxV = CircuitUtils.propFloat(propsCache[src.first][src.second], "v", getDefaultVoltage(sComp.type))
                        val sMaxCharge = RenderEngine.getMaxCap(sComp)
                        val sIsBatteryType = Battery.isBattery(sComp.type)
                        val sCharge = if (sIsBatteryType) {
                            if (sComp.charge < 0f || sComp.charge.isNaN()) sMaxCharge else sComp.charge
                        } else {
                            sMaxCharge
                        }
                        sourceV = if (sIsBatteryType) {
                            sMaxV * (sCharge / sMaxCharge).coerceIn(0f, 1f)
                        } else {
                            sMaxV
                        }
                        sourceV *= voltageMultiplier[x][y]
                    }

                    val rTotal = minResistance[x][y] + 0.1f 
                    val currentMa = (sourceV / rTotal) * 1000f
                    val vLocal = sourceV - (currentMa / 1000f) * minResistance[x][y]
                    
                    if (currentMa > PhysicsConstants.SHORT_CIRCUIT_CURRENT_MA) isShortCircuit = true

                    var finalOverloaded = comp.isOverloaded

                    if (WireAny.isWire(comp.type)) {
                        hasPoweredWires = true
                        if (currentMa > WireAny.getMaxCurrentAmps(comp.type) * 1000f) {
                            finalOverloaded = true
                        }
                    }

                    if (Fuse.isFuse(comp.type)) {
                        if (currentMa > Fuse.getMaxCurrentRatingAmps() * 1000f) {
                            finalOverloaded = true
                        }
                    }

                    if (isLoad(comp.type)) {
                        totalCurrentDrawnSum += currentMa  
                    }
                    
                    if (vLocal > 3.8f && !WireAny.isWire(comp.type) && !Fuse.isFuse(comp.type)) {
                        val overvoltageRatio = (vLocal / 3.8f).coerceAtLeast(1f)
                        val burnProb = (1f - 1f / overvoltageRatio).coerceIn(0f, 1f)
                        if (rand() < burnProb) finalOverloaded = true
                    }

                    if (comp.type == ComponentType.CAPACITOR) {
                        val capCharge = if (comp.charge < 0f || comp.charge.isNaN()) 0f else comp.charge
                        val newCapCharge = (capCharge + currentMa * 0.001f).coerceAtMost(vLocal * 1000f)
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal, current = currentMa, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded, charge = newCapCharge)
                    } else if (comp.type == ComponentType.INDUCTOR) {
                        val indCurrent = if (comp.current < currentMa) (comp.current + 10f).coerceAtMost(currentMa) else currentMa
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal, current = indCurrent, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded)
                    } else if (comp.type == ComponentType.STEP_DOWN_CONVERTER) {
                        val ratio = CircuitUtils.propFloat(propsCache[x][y], "r", 0.5f)
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal * ratio, current = currentMa, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded)
                    } else if (comp.type == ComponentType.STEP_UP_CONVERTER) {
                        val ratio = CircuitUtils.propFloat(propsCache[x][y], "r", 2.0f)
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal * ratio, current = currentMa, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded)
                    } else if (comp.type == ComponentType.INVERTER) {
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal, current = currentMa, logicState = !comp.logicState, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded)
                    } else {
                        grid[x][y] = comp.copy(isPowered = true, voltage = vLocal, current = currentMa, resistance = resistanceCache[x][y], isOverloaded = finalOverloaded)
                    }
                } else if (!isPowerSource(comp.type)) {
                    var finalCharge = comp.charge
                    if (comp.type == ComponentType.CAPACITOR && comp.charge > 0f) {
                        finalCharge = (comp.charge - 10f).coerceAtLeast(0f)
                    }
                    if (comp.isPowered || comp.voltage > 0f || comp.current > 0f || comp.charge != finalCharge || (comp.type == ComponentType.INDUCTOR && comp.current > 0f)) {
                         grid[x][y] = comp.copy(isPowered = false, voltage = 0f, current = if (comp.type == ComponentType.INDUCTOR) (comp.current - 10f).coerceAtLeast(0f) else 0f, resistance = resistanceCache[x][y], charge = finalCharge)
                    }
                }
            }
        }
        
        // 4. Smart AI Power Balancer & Charging Pass
        // Time Scale for balanced gameplay/simulator: 15x real-time so charging/draining is visible and satisfying
        val timeScale = 15f 
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.BATTERY || comp.type == ComponentType.BATTERY_PACK || comp.type == ComponentType.COIN_CELL) {
                    val maxCap = RenderEngine.getMaxCap(comp)
                    val maxV = CircuitUtils.propFloat(propsCache[x][y], "v", getDefaultVoltage(comp.type))
                    val currentCharge = if (comp.charge < 0f || comp.charge.isNaN()) maxCap else comp.charge
                    
                    val connectedGen = isConnectedToGenerator(grid, x, y, width, height)
                    if (connectedGen != null) {
                        val vReceiving = if (minResistance[x][y] < Float.MAX_VALUE) grid[x][y].voltage else 0f
                        val batteryCurrentV = maxV * (currentCharge / maxCap).coerceIn(0.1f, 1f)
                        
                        if (vReceiving > batteryCurrentV && currentCharge < maxCap) {
                            val internalRes = resistanceCache[x][y].coerceAtLeast(0.1f)
                            val chargeCurrentMa = (((vReceiving - batteryCurrentV) / internalRes) * 1000f).coerceIn(100f, when(comp.type) {
                                ComponentType.COIN_CELL -> 200f
                                ComponentType.BATTERY -> 1500f
                                else -> 4000f // BATTERY_PACK
                            })
                            val chargeGainTick = (chargeCurrentMa / 72000f) * timeScale * 0.85f // 85% charging efficiency
                            val newCharge = (currentCharge + chargeGainTick).coerceAtMost(maxCap)
                            
                            grid[x][y] = comp.copy(
                                charge = newCharge,
                                isPowered = true,
                                voltage = vReceiving,
                                current = -chargeCurrentMa // Negative current represents input charging!
                            )
                        } else {
                            grid[x][y] = comp.copy(
                                charge = currentCharge,
                                isPowered = true,
                                voltage = vReceiving,
                                current = 0f
                            )
                        }
                    } else {
                        var totalBatteryDischargeCurrentMa = 0f
                        for (lx in 0 until width) {
                            for (ly in 0 until height) {
                                val cLoad = grid[lx][ly]
                                if (primarySource[lx][ly] == Pair(x, y) && isLoad(cLoad.type)) {
                                    totalBatteryDischargeCurrentMa += cLoad.current
                                }
                            }
                        }
                        
                        val chargeLossTick = (totalBatteryDischargeCurrentMa / 72000f) * timeScale
                        val newCharge = (currentCharge - chargeLossTick).coerceAtLeast(0f)
                        
                        grid[x][y] = comp.copy(
                            charge = newCharge,
                            isPowered = newCharge > 0.05f,
                            voltage = if (newCharge > 0.05f) maxV * (newCharge / maxCap).coerceIn(0f, 1f) else 0f,
                            current = if (newCharge > 0.05f) totalBatteryDischargeCurrentMa else 0f
                        )
                    }
                }
            }
        }

        val maxObservedVoltage = maxOf(if (totalSources > 0) totalSourceVoltageSum / totalSources else 0f, 
            (0 until width).flatMap { x -> (0 until height).map { y -> grid[x][y].voltage } }.maxOrNull() ?: 0f)

        val totalPowerWatts = maxObservedVoltage * (totalCurrentDrawnSum / 1000f)

        return EnergyResult(
            grid = grid,
            telemetry = Telemetry(
                totalVoltage = maxObservedVoltage,
                totalCurrent = totalCurrentDrawnSum,
                totalPower = totalPowerWatts,
                isShortCircuit = isShortCircuit,
                runningScripts = activeScriptsCount
            )
        )
    }

    private fun addDynamicResistanceForComponent(grid: Array<Array<GridComponent>>, x: Int, y: Int, type: ComponentType, props: Map<String, String>): Float {
        val baseR = props["r"]?.toFloatOrNull()
        if (baseR != null) return baseR

        return when {
            WireAny.isWire(type) -> {
                when (type) {
                    ComponentType.SUPERCONDUCTOR -> 0.001f
                    ComponentType.HIGH_VOLTAGE_CABLE -> 0.02f
                    else -> 0.05f
                }
            }
            FiberOptic.isFiberOptic(type) -> Float.MAX_VALUE / 2
            
            Resistor.isResistor(type) -> Resistor.getDefaultResistanceOhms()
            Potentiometer.isPotentiometer(type) -> {
                val valPct = props["p"]?.toFloatOrNull() ?: 0.5f
                Potentiometer.getResistanceOhms(valPct)
            }
            Diode.isDiode(type) || ZenerDiode.isZenerDiode(type) -> 10f
            Transformer.isTransformer(type) -> 50f
            
            Leds.isLuminous(type) -> {
                when (type) {
                    ComponentType.LASER_DIODE -> 50f
                    ComponentType.BULB -> 100f
                    else -> 220f
                }
            }
            
            Motors.isMotorType(type) -> {
                when (type) {
                    ComponentType.SERVO_MOTOR, ComponentType.STEPPER_MOTOR -> 80f
                    else -> 50f
                }
            }
            
            type == ComponentType.HEATER -> 15f
            type == ComponentType.COOLER -> 20f
            type == ComponentType.PELTIER_MODULE -> 25f
            
            type == ComponentType.PHOTORESISTOR -> {
                var illuminated = false
                val dx = intArrayOf(-1, 1, 0, 0)
                val dy = intArrayOf(0, 0, -1, 1)
                for (i in 0..3) {
                    val nx = x + dx[i]; val ny = y + dy[i]
                    if (nx in 0 until grid.size && ny in 0 until grid[0].size) {
                        val adj = grid[nx][ny]
                        if (adj.isPowered && Leds.isLuminous(adj.type)) {
                            illuminated = true
                        }
                    }
                }
                if (illuminated) 330f else 1000000f
            }
            type == ComponentType.THERMISTOR -> {
                var maxTemp = grid[x][y].temperature
                val dx = intArrayOf(-1, 1, 0, 0)
                val dy = intArrayOf(0, 0, -1, 1)
                for (i in 0..3) {
                    val nx = x + dx[i]; val ny = y + dy[i]
                    if (nx in 0 until grid.size && ny in 0 until grid[0].size) {
                        maxTemp = maxOf(maxTemp, grid[nx][ny].temperature)
                    }
                }
                val tC = maxTemp.coerceAtLeast(-273f)
                val tK = tC + 273.15f
                val t0K = 25f + 273.15f
                val b = CircuitUtils.propFloat(props, "B", 3950f)
                val baseRes = CircuitUtils.propFloat(props, "R", 10000f)
                (baseRes * Math.exp((b * (1/tK - 1/t0K)).toDouble())).toFloat().coerceIn(10f, 1000000f)
            }
            SpeakerBuzzer.isAcoustic(type) -> 8f
            
            Switch.isSwitch(type) || PushButton.isPushButton(type) -> 0.1f
            type == ComponentType.STEP_DOWN_CONVERTER || type == ComponentType.STEP_UP_CONVERTER || type == ComponentType.INVERTER -> 5f
            
            LogicGate.isLogicGate(type) -> 1000f
            
            Microcontroller.isMicrocontrollerOrMemory(type) -> 5000f
            
            else -> 0.5f
        }
    }

    fun isPowerSource(type: ComponentType): Boolean {
        return Battery.isBattery(type) || Generator.isGenerator(type) || type == ComponentType.INFINITE_BATTERY || type == ComponentType.NUCLEAR_REACTOR
    }

    fun isLoad(type: ComponentType): Boolean {
        return type.category == com.example.model.ComponentCategory.OUTPUTS || 
               type.category == com.example.model.ComponentCategory.LOGIC || 
               type.category == com.example.model.ComponentCategory.ADVANCED
    }

    fun getDefaultVoltage(type: ComponentType): Float {
        if (Battery.isBattery(type)) return Battery.getDefaultVoltage(type)
        if (Generator.isGenerator(type)) return Generator.getDefaultVoltage(type)
        return when (type) {
            ComponentType.INFINITE_BATTERY -> 220f
            ComponentType.NUCLEAR_REACTOR -> 240f
            else -> 9f
        }
    }

    fun getDefaultCapacity(type: ComponentType): Float {
        if (Battery.isBattery(type)) return Battery.getMaxCapacity(type)
        return when (type) {
            ComponentType.GENERATOR -> 500f
            ComponentType.SOLAR_PANEL -> 20f
            ComponentType.AC_SOURCE -> Float.MAX_VALUE
            ComponentType.WIND_TURBINE -> 50f
            ComponentType.GEOTHERMAL_GENERATOR -> 1000f
            ComponentType.HYDRO_GENERATOR -> 500f
            ComponentType.THERMOELECTRIC_GENERATOR -> 10f
            ComponentType.INFINITE_BATTERY -> Float.MAX_VALUE
            else -> 1000f
        }
    }
}
