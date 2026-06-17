package com.example.engine

import com.example.model.ComponentCategory
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

    private class PathNode(
        val x: Int, 
        val y: Int, 
        val resistance: Float, 
        val isAcBranch: Boolean = false,
        val frequency: Float = 0f, 
        val phase: Float = 0f
    ) : Comparable<PathNode> {
        override fun compareTo(other: PathNode): Int {
            return this.resistance.compareTo(other.resistance)
        }
    }

    private fun canPassPower(component: GridComponent, fromX: Int, fromY: Int, toX: Int, toY: Int, instantaneousV: Float = 1f): Boolean {
        val type = component.type
        if (type == ComponentType.EMPTY || type.category == ComponentCategory.TOOLS) return false
        if (FiberOptic.isFiberOptic(type)) return false
        
        if (PushButton.isPushButton(type) && !component.logicState) return false
        if (PressurePad.isMechanicalSensor(type) && !component.logicState) return false
        if (Sensors.isSensor(type) && type == ComponentType.PRESSURE_SENSOR && !component.logicState) return false
        
        if (Switch.isSwitch(type) && !Switch.isConducting(type, component.logicState)) return false
        if (Transistor.isTransistor(type) && !component.logicState) return false
        
        // Rectification and forward-bias logic
        if (Diode.isDiode(type) || ZenerDiode.isZenerDiode(type)) {
            val dir = component.direction
            val isForward = when (dir) {
                Direction.RIGHT -> fromX < toX
                Direction.LEFT -> fromX > toX
                Direction.DOWN -> fromY < toY
                Direction.UP -> fromY > toY
            }
            
            if (isForward) {
                // Diode conducts forwards if instantaneous voltage is positive
                return instantaneousV >= 0.1f
            } else {
                // Zener diode conducts backwards in reverse breakdown
                if (ZenerDiode.isZenerDiode(type) && instantaneousV < -ZenerDiode.getBreakdownVoltage()) {
                    return true
                }
                return false
            }
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
            val comp = grid[cx][cy]
            
            if (isPowerSource(comp.type) && comp.type != ComponentType.BATTERY && comp.type != ComponentType.BATTERY_PACK && comp.type != ComponentType.COIN_CELL) {
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
                } else if (comp.type == ComponentType.WIND_TURBINE) {
                    val windSpeed = WindTurbine.getWindSpeedAt(grid, cx, cy, width, height)
                    isActive = windSpeed >= WindTurbine.getCutInWindSpeed() && windSpeed <= WindTurbine.getCutOutWindSpeed()
                } else {
                    isActive = true // AC Source, etc.
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
        val isAcPath = Array(width) { BooleanArray(height) { false } }
        val pathFrequency = Array(width) { FloatArray(height) { 0f } }
        val pathPhase = Array(width) { FloatArray(height) { 0f } }
        
        val localTime = (System.currentTimeMillis() % 10000000) / 1000f

        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)

        val propsCache = Array(width) { x -> Array(height) { y -> CircuitUtils.parseProps(grid[x][y].extraData) } }
        
        // Dynamic AC frequency, default 50Hz, configurable via "f" parameter in power source props
        val resistanceCache = Array(width) { x -> Array(height) { y -> 
            val p = propsCache[x][y]
            val type = grid[x][y].type
            addDynamicResistanceForComponent(grid, x, y, type, p) 
        }}

        // 1. Initial scan for active power sources & calculate instant voltages (AC/DC)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (isPowerSource(comp.type)) {
                    val maxCap = RenderEngine.getMaxCap(comp)
                    val isBatteryType = Battery.isBattery(comp.type)
                    val currentCharge = if (isBatteryType) {
                        if (comp.charge < 0f || comp.charge.isNaN()) maxCap else comp.charge
                    } else {
                        -1f
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
                    } else if (comp.type == ComponentType.WIND_TURBINE) {
                        val windSpeed = WindTurbine.getWindSpeedAt(grid, x, y, width, height)
                        isValidSource = windSpeed >= WindTurbine.getCutInWindSpeed() && windSpeed <= WindTurbine.getCutOutWindSpeed()
                    } else if (isBatteryType) {
                        val connectedGen = isConnectedToGenerator(grid, x, y, width, height)
                        if (connectedGen == null && currentCharge > 0.05f) {
                            isValidSource = true
                        }
                    } else {
                        isValidSource = true
                    }
                    
                    if (isValidSource) {
                        val maxV = CircuitUtils.propFloat(propsCache[x][y], "v", getDefaultVoltage(comp.type))
                        
                        // Parse AC characteristics and frequency
                        val isAcSource = comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INVERTER || CircuitUtils.propFloat(propsCache[x][y], "ac", 0f) > 0.5f
                        val frequency = CircuitUtils.propFloat(propsCache[x][y], "f", 50f) // 50 Hz default
                        val basePhase = CircuitUtils.propFloat(propsCache[x][y], "phase", 0f)

                        var currentV = 0f
                        if (!comp.isOverloaded) {
                            val previousCurrentMa = comp.current.coerceAtLeast(0f)
                            val maxI = getRatedMaxCurrentMa(comp.type)
                            val sagFactor = if (maxI > 0) (previousCurrentMa / maxI).coerceIn(0f, 1.2f) else 0f
                            val baseV = if (isBatteryType) {
                                maxV * (currentCharge / maxCap).coerceIn(0f, 1f)
                            } else if (comp.type == ComponentType.WIND_TURBINE) {
                                val windSpeed = WindTurbine.getWindSpeedAt(grid, x, y, width, height)
                                maxV * WindTurbine.getEfficiency(windSpeed)
                            } else {
                                maxV
                            }
                            val sagReduction = if (isBatteryType) 0.25f else 0.45f
                            var actualPeak = baseV * (1f - sagReduction * sagFactor)
                            
                            // Load Regulator Modes: "ECO", "BOOST", "SURGE"
                            val mode = propsCache[x][y]["mode"] ?: "NORMAL"
                            when (mode.uppercase()) {
                                "ECO" -> {
                                    actualPeak *= 0.7f // Saves power but outputs less voltage
                                }
                                "BOOST" -> {
                                    actualPeak *= 1.35f // Boost output but causes self heating!
                                    grid[x][y] = grid[x][y].copy(temperature = grid[x][y].temperature + 4.5f)
                                }
                                "SURGE" -> {
                                    // Periodic spikes
                                    val spike = if ((localTime * 2f).toInt() % 4 == 0) 1.6f else 0.9f
                                    actualPeak *= spike
                                }
                            }

                            currentV = if (isAcSource) {
                                // Dynamic instant sinusoidal AC calculation
                                val angle = (2f * Math.PI * frequency * localTime + basePhase).toFloat()
                                actualPeak * kotlin.math.sin(angle)
                            } else {
                                actualPeak
                            }
                        }
                        
                        minResistance[x][y] = 0f
                        primarySource[x][y] = Pair(x, y)
                        isAcPath[x][y] = isAcSource
                        pathFrequency[x][y] = frequency
                        pathPhase[x][y] = basePhase
                        pq.add(PathNode(x, y, 0f, isAcSource, frequency, basePhase))
                    }
                }
            }
        }

        // 2. Run Dijkstra's path-finding algorithm using reactive impedance (Z)
        while (pq.isNotEmpty()) {
            val curr = pq.poll()
            val cx = curr.x
            val cy = curr.y
            
            if (curr.resistance > minResistance[cx][cy]) continue
            if (grid[cx][cy].isOverloaded) continue

            val activeVInstant = if (curr.isAcBranch) {
                val s = primarySource[cx][cy]
                if (s != null) {
                    val angle = (2f * Math.PI * curr.frequency * localTime + curr.phase).toFloat()
                    kotlin.math.sin(angle)
                } else 1f
            } else {
                1f
            }

            for (i in 0..3) {
                val nx = cx + dxs[i]
                val ny = cy + dys[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val nComp = grid[nx][ny]
                    if (nComp.isOverloaded) continue
                    if (canPassPower(nComp, cx, cy, nx, ny, activeVInstant)) {
                        
                        // Calculate dynamic impedance based on category
                        var rNeigh = resistanceCache[nx][ny]
                        if (curr.isAcBranch && curr.frequency > 0.1f) {
                            if (nComp.type == ComponentType.CAPACITOR) {
                                // Reactance Xc = 1 / (2*pi*f*C). C is capacitive rating.
                                val cValue = CircuitUtils.propFloat(propsCache[nx][ny], "c_val", 100f).coerceAtLeast(0.1f) // in microFarad
                                val reactanceC = 1000000f / (2f * Math.PI.toFloat() * curr.frequency * cValue)
                                rNeigh = (0.2f + reactanceC).coerceAtMost(100000f)
                            } else if (nComp.type == ComponentType.INDUCTOR) {
                                // Reactance Xl = 2 * pi * f * L. L is inductance rating.
                                val lValue = CircuitUtils.propFloat(propsCache[nx][ny], "l_val", 10f).coerceAtLeast(0.1f) // in milliHenry
                                val reactanceL = 2f * Math.PI.toFloat() * curr.frequency * lValue / 1000f
                                rNeigh = (0.1f + reactanceL).coerceAtMost(100000f)
                            }
                        } else if (!curr.isAcBranch) {
                            // DC Mode blocks capacitor
                            if (nComp.type == ComponentType.CAPACITOR) {
                                rNeigh = 1000000f // blocks DC
                            }
                        }

                        val newR = curr.resistance + rNeigh
                        if (newR < minResistance[nx][ny]) {
                            minResistance[nx][ny] = newR
                            primarySource[nx][ny] = primarySource[cx][cy]
                            isAcPath[nx][ny] = curr.isAcBranch
                            pathFrequency[nx][ny] = curr.frequency
                            pathPhase[nx][ny] = curr.phase
                            
                            var mult = voltageMultiplier[cx][cy]
                            if (grid[cx][cy].type == ComponentType.STEP_DOWN_CONVERTER) mult *= CircuitUtils.propFloat(propsCache[cx][cy], "r", 0.5f)
                            if (grid[cx][cy].type == ComponentType.STEP_UP_CONVERTER) mult *= CircuitUtils.propFloat(propsCache[cx][cy], "r", 2.0f)
                            
                            // AC Transformers only operate under oscillating AC.
                            // Blocks DC completely or converts it into high heating!
                            if (grid[cx][cy].type == ComponentType.TRANSFORMER) {
                                if (curr.isAcBranch && curr.frequency > 5.0f) {
                                    val turnsRatio = CircuitUtils.propFloat(propsCache[cx][cy], "turns", 2.0f)
                                    mult *= turnsRatio
                                } else {
                                    mult *= 0.01f // Highly chokes DC
                                    // Generate catastrophic heat due to electromagnetic core saturation on DC!
                                    grid[cx][cy] = grid[cx][cy].copy(temperature = grid[cx][cy].temperature + 15.0f)
                                }
                            }

                            voltageMultiplier[nx][ny] = mult
                            pq.add(PathNode(nx, ny, newR, curr.isAcBranch, curr.frequency, curr.phase))
                        }
                    }
                }
            }
        }

        // 3. Evaluate local voltages, currents, thermal power dissipation, and melting thresholds
        var totalCurrentDrawnSum = 0f
        var isShortCircuit = false
        var totalActiveSourcesCount = 0
        var sourceVoltagesSum = 0f

        // Thermal calculations: surrounding elements affect ambient temperature (Environment convergence)
        val ambientGrid = Array(width) { FloatArray(height) { 25f } } // Default ambient room temp 25 C
        for (x in 0 until width) {
            for (y in 0 until height) {
                val adjType = grid[x][y].type
                var localAmbientContribution = 25f
                if (adjType == ComponentType.LAVA || adjType == ComponentType.INFINITE_LAVA) {
                    localAmbientContribution = 1200f
                } else if (adjType == ComponentType.FIRE) {
                    localAmbientContribution = 600f
                } else if (adjType == ComponentType.ICE) {
                    localAmbientContribution = 0f
                } else if (adjType == ComponentType.LIQUID_NITROGEN || adjType == ComponentType.INFINITE_LIQUID_NITROGEN) {
                    localAmbientContribution = -196f
                } else if (adjType == ComponentType.HEATER) {
                    localAmbientContribution = if (grid[x][y].isPowered) 180f else 25f
                } else if (adjType == ComponentType.COOLER) {
                    localAmbientContribution = if (grid[x][y].isPowered) -30f else 25f
                }
                
                if (localAmbientContribution != 25f) {
                    // Spread ambient thermal zone radials to adjacent cells
                    ambientGrid[x][y] = maxOf(ambientGrid[x][y], localAmbientContribution)
                    for (i in 0..3) {
                        val nx = x + dxs[i]; val ny = y + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            ambientGrid[nx][ny] = (ambientGrid[nx][ny] * 0.4f) + (localAmbientContribution * 0.6f)
                        }
                    }
                }
            }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.EMPTY || comp.type.category == ComponentCategory.TOOLS) continue

                if (minResistance[x][y] < Float.MAX_VALUE && minResistance[x][y] >= 0f) {
                    val src = primarySource[x][y]
                    var sourcePeakV = 0f
                    if (src != null) {
                        val sComp = grid[src.first][src.second]
                        if (!sComp.isOverloaded) {
                            val sMaxV = CircuitUtils.propFloat(propsCache[src.first][src.second], "v", getDefaultVoltage(sComp.type))
                            val sMaxCharge = RenderEngine.getMaxCap(sComp)
                            val sIsBatteryType = Battery.isBattery(sComp.type)
                            val sCharge = if (sIsBatteryType) {
                                if (sComp.charge < 0f || sComp.charge.isNaN()) sMaxCharge else sComp.charge
                            } else {
                                sMaxCharge
                            }
                            val previousCurrentMa = sComp.current.coerceAtLeast(0f)
                            val maxI = getRatedMaxCurrentMa(sComp.type)
                            val sagFactor = if (maxI > 0) (previousCurrentMa / maxI).coerceIn(0f, 1.2f) else 0f
                            val baseSourceV = if (sIsBatteryType) {
                                sMaxV * (sCharge / sMaxCharge).coerceIn(0f, 1f)
                            } else if (sComp.type == ComponentType.WIND_TURBINE) {
                                val windSpeed = WindTurbine.getWindSpeedAt(grid, src.first, src.second, width, height)
                                sMaxV * WindTurbine.getEfficiency(windSpeed)
                            } else {
                                sMaxV
                            }
                            val sagReduction = if (sIsBatteryType) 0.25f else 0.45f
                            var actualPeak = baseSourceV * (1f - sagReduction * sagFactor)
                            
                            val mode = propsCache[src.first][src.second]["mode"] ?: "NORMAL"
                            if (mode.uppercase() == "ECO") actualPeak *= 0.7f
                            if (mode.uppercase() == "BOOST") actualPeak *= 1.35f
                            if (mode.uppercase() == "SURGE" && (localTime * 2f).toInt() % 4 == 0) actualPeak *= 1.6f

                            sourcePeakV = actualPeak * voltageMultiplier[x][y]
                        }
                    }

                    val isAc = isAcPath[x][y]
                    val frequency = pathFrequency[x][y]
                    val phase = pathPhase[x][y]
                    
                    val currentPeakV = if (isAc && frequency > 0.1f) {
                        val angle = (2f * Math.PI * frequency * localTime + phase).toFloat()
                        sourcePeakV * kotlin.math.sin(angle)
                    } else {
                        sourcePeakV
                    }

                    val rTotal = minResistance[x][y] + 0.12f 
                    val currentMa = (currentPeakV / rTotal) * 1000f
                    val vLocal = currentPeakV - (currentMa / 1000f) * minResistance[x][y]
                    
                    if (kotlin.math.abs(currentMa) > PhysicsConstants.SHORT_CIRCUIT_CURRENT_MA) {
                        isShortCircuit = true
                    }

                    var finalOverloaded = comp.isOverloaded

                    // Wire maximum current limitation
                    if (WireAny.isWire(comp.type)) {
                        val maxI = WireAny.getMaxCurrentAmps(comp.type) * 1000f
                        if (kotlin.math.abs(currentMa) > maxI) {
                            finalOverloaded = true
                        }
                    }

                    // Fuse current breakdown rating
                    if (Fuse.isFuse(comp.type)) {
                        if (kotlin.math.abs(currentMa) > Fuse.getMaxCurrentRatingAmps() * 1000f) {
                            finalOverloaded = true
                        }
                    }

                    if (isLoad(comp.type)) {
                        totalCurrentDrawnSum += kotlin.math.abs(currentMa)
                    }
                    
                    // High voltage damage
                    if (kotlin.math.abs(vLocal) > 420f && !WireAny.isWire(comp.type) && !Fuse.isFuse(comp.type) && comp.type != ComponentType.HIGH_VOLTAGE_CABLE) {
                        val overvoltageRatio = (kotlin.math.abs(vLocal) / 420f).coerceAtLeast(1f)
                        val burnProb = (1f - 1f / overvoltageRatio).coerceIn(0f, 1f)
                        if (rand() < burnProb) finalOverloaded = true
                    }

                    // --- JOULE HEATING THERMODYNAMIC CONVERGENCE ---
                    // Dissipated heat power P = I^2 * R. Convert current to Amperes, R to Ohms.
                    val iAmps = currentMa / 1000f
                    val rComponent = resistanceCache[x][y].coerceAtLeast(0.005f)
                    val powerWatts = (iAmps * iAmps * rComponent).coerceIn(0f, 50000f)
                    
                    // Thermal capacity updates
                    val localAmbient = ambientGrid[x][y]
                    val heatGain = powerWatts * 1.5f // degrees celsius per watt per tick
                    val thermalMassFactor = getThermalMassCoefficient(comp.type)
                    val nextTemp = comp.temperature + (heatGain / thermalMassFactor)
                    // Thermal dissipation decay to surrounding ambient
                    val decayCoeff = 0.045f * getThermalDissipationFactor(comp.type)
                    var thermalDischarge = nextTemp - (nextTemp - localAmbient) * decayCoeff
                    if (thermalDischarge.isNaN()) {
                        thermalDischarge = localAmbient
                    }
                    
                    // Melt/Burn threshold check
                    val meltTemp = getComponentMeltingTemperature(comp.type)
                    if (thermalDischarge >= meltTemp) {
                        finalOverloaded = true
                        // Toxic melt smoke logs
                        isShortCircuit = true
                    }

                    // Specific states
                    val cl = comp.clone()
                    if (comp.type == ComponentType.CAPACITOR) {
                        val capCharge = if (comp.charge < 0f || comp.charge.isNaN()) 0f else comp.charge
                        val newCapCharge = (capCharge + currentMa * 0.001f).coerceIn(-vLocal * 1000f, vLocal * 1000f)
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal, current = currentMa, resistance = rComponent, isOverloaded = finalOverloaded, charge = newCapCharge, temperature = thermalDischarge)
                    } else if (comp.type == ComponentType.INDUCTOR) {
                        val indCurrent = if (comp.current < currentMa) (comp.current + 10f).coerceAtMost(currentMa) else currentMa
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal, current = indCurrent, resistance = rComponent, isOverloaded = finalOverloaded, temperature = thermalDischarge)
                    } else if (comp.type == ComponentType.STEP_DOWN_CONVERTER) {
                        val ratio = CircuitUtils.propFloat(propsCache[x][y], "r", 0.5f)
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal * ratio, current = currentMa, resistance = rComponent, isOverloaded = finalOverloaded, temperature = thermalDischarge)
                    } else if (comp.type == ComponentType.STEP_UP_CONVERTER) {
                        val ratio = CircuitUtils.propFloat(propsCache[x][y], "r", 2.0f)
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal * ratio, current = currentMa, resistance = rComponent, isOverloaded = finalOverloaded, temperature = thermalDischarge)
                    } else if (comp.type == ComponentType.INVERTER) {
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal, current = currentMa, logicState = !comp.logicState, resistance = rComponent, isOverloaded = finalOverloaded, temperature = thermalDischarge)
                    } else {
                        grid[x][y] = cl.copy(isPowered = true, voltage = vLocal, current = currentMa, resistance = rComponent, isOverloaded = finalOverloaded, temperature = thermalDischarge)
                    }
                } else if (!isPowerSource(comp.type)) {
                    var finalCharge = comp.charge
                    if (comp.type == ComponentType.CAPACITOR && comp.charge > 0f) {
                        finalCharge = (comp.charge - 10f).coerceAtLeast(0f)
                    }
                    
                    // Cool down inactive elements to historical surrounding local ambient
                    val localAmbient = ambientGrid[x][y]
                    val decayCoeff = 0.045f * getThermalDissipationFactor(comp.type)
                    var thermalDischarge = comp.temperature - (comp.temperature - localAmbient) * decayCoeff
                    if (thermalDischarge.isNaN()) {
                        thermalDischarge = localAmbient
                    }

                    if (comp.isPowered || comp.voltage > 0f || comp.current > 0f || comp.charge != finalCharge || comp.temperature != thermalDischarge || (comp.type == ComponentType.INDUCTOR && comp.current > 0f)) {
                         grid[x][y] = comp.copy(isPowered = false, voltage = 0f, current = if (comp.type == ComponentType.INDUCTOR) (comp.current - 10f).coerceAtLeast(0f) else 0f, resistance = resistanceCache[x][y], charge = finalCharge, temperature = thermalDischarge)
                    }
                }
            }
        }
        
        // 4. Smart Power Balancer Charging & Dynamic Source Heat Dissipation
        val timeScale = 15f 
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                val type = comp.type
                
                if (isPowerSource(type)) {
                    var totalDischargeCurrentMa = 0f
                    for (lx in 0 until width) {
                        for (ly in 0 until height) {
                            val cLoad = grid[lx][ly]
                            if (primarySource[lx][ly] == Pair(x, y) && isLoad(cLoad.type)) {
                                totalDischargeCurrentMa += kotlin.math.abs(cLoad.current)
                            }
                        }
                    }
                    
                    val maxI = getRatedMaxCurrentMa(type)
                    var isOverloadedNow = comp.isOverloaded
                    
                    val ratingThreshold = if (propsCache[x][y]["mode"]?.uppercase() == "BOOST") maxI * 1.8f else maxI * 1.5f
                    if (totalDischargeCurrentMa > ratingThreshold && type != ComponentType.INFINITE_BATTERY) {
                        isOverloadedNow = true
                    }
                    
                    // Generate dynamic thermal heating inside current sources!
                    val srcAmps = totalDischargeCurrentMa / 1000f
                    val srcR = if (Battery.isBattery(type)) 0.15f else 0.45f
                    val srcWatts = srcAmps * srcAmps * srcR
                    val localAmbient = ambientGrid[x][y]
                    val srcHeatGain = srcWatts * 2.2f
                    var srcNextTemp = comp.temperature + srcHeatGain
                    
                    // ECO cools down generator because of throttled load levels
                    if (propsCache[x][y]["mode"]?.uppercase() == "ECO") {
                        srcNextTemp -= 1.5f
                    }
                    
                    var srcThermalDischarge = srcNextTemp - (srcNextTemp - localAmbient) * 0.05f
                    if (srcThermalDischarge.isNaN()) srcThermalDischarge = localAmbient
                    if (srcThermalDischarge >= getComponentMeltingTemperature(type) && type != ComponentType.INFINITE_BATTERY) {
                        isOverloadedNow = true
                    }

                    if (Battery.isBattery(type)) {
                        val maxCap = RenderEngine.getMaxCap(comp)
                        val maxV = CircuitUtils.propFloat(propsCache[x][y], "v", getDefaultVoltage(type))
                        val currentCharge = if (comp.charge < 0f || comp.charge.isNaN()) maxCap else comp.charge
                        
                        val connectedGen = isConnectedToGenerator(grid, x, y, width, height)
                        if (connectedGen != null && !isOverloadedNow) {
                            val vReceiving = if (minResistance[x][y] < Float.MAX_VALUE) grid[x][y].voltage else 0f
                            val batteryCurrentV = maxV * (currentCharge / maxCap).coerceIn(0.1f, 1f)
                            
                            val isOverPotential = vReceiving > batteryCurrentV
                            if ((isOverPotential || vReceiving > 0.5f) && currentCharge < maxCap) {
                                val activeChargingV = maxOf(vReceiving, batteryCurrentV + 1.2f)
                                val internalRes = resistanceCache[x][y].coerceAtLeast(0.1f)
                                val chargeCurrentMa = (((activeChargingV - batteryCurrentV) / internalRes) * 1000f).coerceIn(200f, when(type) {
                                    ComponentType.COIN_CELL -> 500f
                                    ComponentType.BATTERY -> 3000f
                                    else -> 8000f
                                })
                                val chargeGainTick = (chargeCurrentMa / 72000f) * timeScale * 1.5f
                                val newCharge = (currentCharge + chargeGainTick).coerceAtMost(maxCap)
                                
                                grid[x][y] = comp.copy(
                                    charge = newCharge,
                                    isPowered = true,
                                    voltage = vReceiving,
                                    current = -chargeCurrentMa,
                                    isOverloaded = isOverloadedNow,
                                    temperature = srcThermalDischarge
                                )
                            } else {
                                grid[x][y] = comp.copy(
                                    charge = currentCharge,
                                    isPowered = true,
                                    voltage = vReceiving,
                                    current = 0f,
                                    isOverloaded = isOverloadedNow,
                                    temperature = srcThermalDischarge
                                )
                            }
                        } else {
                            val batteryV = if (isOverloadedNow) 0f else (maxV * (currentCharge / maxCap).coerceIn(0f, 1f))
                            val sagFactor = if (maxI > 0) (totalDischargeCurrentMa / maxI).coerceIn(0f, 1f) else 0f
                            val actualBatteryV = batteryV * (1f - 0.25f * sagFactor)
                            
                            val chargeLossTick = (totalDischargeCurrentMa / 288000f) * timeScale
                            val newCharge = (currentCharge - chargeLossTick).coerceAtLeast(0f)
                            
                            grid[x][y] = comp.copy(
                                charge = newCharge,
                                isPowered = newCharge > 0.05f && !isOverloadedNow,
                                voltage = if (newCharge > 0.05f) actualBatteryV else 0f,
                                current = if (newCharge > 0.05f) totalDischargeCurrentMa else 0f,
                                isOverloaded = isOverloadedNow,
                                temperature = srcThermalDischarge
                              )
                        }
                    } else {
                        val sMaxV = CircuitUtils.propFloat(propsCache[x][y], "v", getDefaultVoltage(type))
                        val sagFactor = if (maxI > 0) (totalDischargeCurrentMa / maxI).coerceIn(0f, 1.2f) else 0f
                        var actualGenV = if (isOverloadedNow) 0f else sMaxV * (1f - 0.45f * sagFactor)
                        
                        val mode = propsCache[x][y]["mode"] ?: "NORMAL"
                        if (mode.uppercase() == "ECO") actualGenV *= 0.7f
                        if (mode.uppercase() == "BOOST") actualGenV *= 1.35f
                        if (mode.uppercase() == "SURGE" && (localTime * 2f).toInt() % 4 == 0) actualGenV *= 1.6f

                        grid[x][y] = comp.copy(
                            isPowered = actualGenV > 0.1f,
                            voltage = actualGenV,
                            current = totalDischargeCurrentMa,
                            isOverloaded = isOverloadedNow,
                            temperature = srcThermalDischarge
                        )
                    }
                    
                    if (grid[x][y].isPowered) {
                        totalActiveSourcesCount++
                        sourceVoltagesSum += grid[x][y].voltage
                    }
                }
            }
        }

        val maxObservedVoltage = maxOf(if (totalActiveSourcesCount > 0) sourceVoltagesSum / totalActiveSourcesCount else 0f, 
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

        // Thermal expansion resistance scaling coefficients (e.g. metals increase resistance when hot)
        val tempCoeff = when (type) {
            ComponentType.COPPER, ComponentType.ALUMINUM, ComponentType.STEEL, ComponentType.GOLD -> 0.0039f // pure metals
            ComponentType.SUPERCONDUCTOR -> -0.001f // high stability
            else -> 0f
        }
        val currentTemp = grid[x][y].temperature
        val tempModifier = if (tempCoeff != 0f) (1f + tempCoeff * (currentTemp - 25f)).coerceAtLeast(0.001f) else 1f

        return when {
            WireAny.isWire(type) -> {
                val wireBase = when (type) {
                    ComponentType.SUPERCONDUCTOR -> 0.001f
                    ComponentType.HIGH_VOLTAGE_CABLE -> 0.02f
                    else -> 0.05f
                }
                wireBase * tempModifier
            }
            FiberOptic.isFiberOptic(type) -> Float.MAX_VALUE / 2
            
            Resistor.isResistor(type) -> Resistor.getDefaultResistanceOhms()
            Potentiometer.isPotentiometer(type) -> {
                val valPct = props["p"]?.toFloatOrNull() ?: 0.5f
                Potentiometer.getResistanceOhms(valPct)
            }
            Diode.isDiode(type) || ZenerDiode.isZenerDiode(type) -> 10f
            Transformer.isTransformer(type) -> 15f
            
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

    fun getRatedMaxCurrentMa(type: ComponentType): Float {
        return when (type) {
            ComponentType.NUCLEAR_REACTOR -> 35000f
            ComponentType.GENERATOR -> 15000f
            ComponentType.GEOTHERMAL_GENERATOR -> 10000f
            ComponentType.HYDRO_GENERATOR -> 6000f
            ComponentType.WIND_TURBINE -> 4000f
            ComponentType.SOLAR_PANEL -> 2500f
            ComponentType.COIN_CELL -> 600f
            ComponentType.BATTERY -> 4000f
            ComponentType.BATTERY_PACK -> 12000f
            else -> 100000f
        }
    }

    // Material Thermodynamic Melting/Breakdown limit specifications
    private fun getComponentMeltingTemperature(type: ComponentType): Float {
        return when (type) {
            ComponentType.GLASS -> 600f
            ComponentType.WOOD -> 230f
            ComponentType.PLASTIC -> 160f
            ComponentType.ICE -> 0f
            ComponentType.SLIME -> 90f
            ComponentType.SPONGE -> 140f
            ComponentType.RUBBER -> 180f
            
            ComponentType.COPPER -> 1085f
            ComponentType.GOLD -> 1064f
            ComponentType.ALUMINUM -> 660f
            ComponentType.STEEL -> 1510f
            
            ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL -> 130f
            ComponentType.FUSE -> 95f
            ComponentType.WIRE_ANY -> 250f
            ComponentType.HIGH_VOLTAGE_CABLE -> 380f
            ComponentType.SUPERCONDUCTOR -> 90f // Needs cryogenic chilling to stay superconducting! Excellent mechanical balance!
            
            else -> 1200f
        }
    }

    private fun getThermalMassCoefficient(type: ComponentType): Float {
        return when (type) {
            ComponentType.COPPER -> 8.5f
            ComponentType.GOLD -> 12.0f
            ComponentType.ALUMINUM -> 5.0f
            ComponentType.STEEL -> 10.0f
            ComponentType.THERMISTOR -> 2.0f
            ComponentType.PELTIER_MODULE -> 18.0f // Heavy dissipator
            else -> 6.0f
        }
    }

    private fun getThermalDissipationFactor(type: ComponentType): Float {
        return when (type) {
            ComponentType.COPPER -> 1.8f // High thermal dissipation
            ComponentType.GOLD -> 1.5f
            ComponentType.ALUMINUM -> 2.2f // Great heat dissipator
            ComponentType.STEEL -> 0.8f
            ComponentType.WOOD -> 0.05f // Insulator
            ComponentType.PLASTIC -> 0.08f // Insulator
            else -> 1.0f
        }
    }

    fun isPowerSource(type: ComponentType): Boolean {
        return Battery.isBattery(type) || Generator.isGenerator(type) || type == ComponentType.INFINITE_BATTERY || type == ComponentType.NUCLEAR_REACTOR || type == ComponentType.AC_SOURCE || type == ComponentType.SOLAR_PANEL || type == ComponentType.WIND_TURBINE || type == ComponentType.GEOTHERMAL_GENERATOR || type == ComponentType.HYDRO_GENERATOR || type == ComponentType.THERMOELECTRIC_GENERATOR
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
            ComponentType.AC_SOURCE -> 220f
            ComponentType.SOLAR_PANEL -> 18f
            ComponentType.WIND_TURBINE -> 24f
            ComponentType.GEOTHERMAL_GENERATOR -> 48f
            ComponentType.HYDRO_GENERATOR -> 36f
            ComponentType.THERMOELECTRIC_GENERATOR -> 5f
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
