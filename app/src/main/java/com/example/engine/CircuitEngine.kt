package com.example.engine

import android.util.Base64
import com.example.model.ComponentType
import com.example.model.ComponentCategory
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.SimulationResult
import com.example.model.Telemetry

class CircuitEngine {

    private fun canPassPower(component: GridComponent, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val type = component.type
        if (type == ComponentType.EMPTY || type == ComponentType.SWITCH_OPEN || type.category.name == "TOOLS") return false
        
        // Push button acts like an open switch until pressed (we'll assume they stay open unless handled specifically, normally they require active input, but we don't have mouse down. We can use logicState).
        if (type == ComponentType.PUSH_BUTTON && !component.logicState) return false
        
        // Relays and Transistors only pass power if their logicState is true
        if (type == ComponentType.RELAY && !component.logicState) return false
        if (type == ComponentType.TRANSISTOR && !component.logicState) return false
        
        // Diode Logic: Allows flow only in the direction it points
        if (type == ComponentType.DIODE) {
            val dir = component.direction
            // Determine direction of flow
            if (fromX < toX && dir == Direction.RIGHT) return true
            if (fromX > toX && dir == Direction.LEFT) return true
            if (fromY < toY && dir == Direction.DOWN) return true
            if (fromY > toY && dir == Direction.UP) return true
            return false // Blocked
        }
        
        return true
    }

    fun parseProps(data: String): Map<String, String> {
        if (data.isBlank()) return emptyMap()
        return data.split("|").mapNotNull { 
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    private fun runScripts(grid: Array<Array<GridComponent>>, width: Int, height: Int) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    // Very simple parser for proof of concept
                    // command format: out(pin, 1/0)
                    val lines = comp.extraData.split('\n')
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("out(") && trimmed.endsWith(")")) {
                            try {
                                val args = trimmed.substring(4, trimmed.length - 1).split(",")
                                if (args.size == 2) {
                                    val pin = args[0].trim().toIntOrNull() ?: 0
                                    val v = args[1].trim().toIntOrNull() ?: 0
                                    
                                    val nx = when(pin) { 0 -> x; 1 -> x + 1; 2 -> x; 3 -> x - 1; else -> -1 }
                                    val ny = when(pin) { 0 -> y - 1; 1 -> y; 2 -> y + 1; 3 -> y; else -> -1 }
                                    
                                    if (nx in 0 until width && ny in 0 until height) {
                                        grid[nx][ny] = grid[nx][ny].copy(logicState = (v > 0))
                                    }
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }
            }
        }
    }

    fun calculatePower(grid: Array<Array<GridComponent>>, width: Int, height: Int, tick: Long = 0): SimulationResult {
        val powered = mutableSetOf<Pair<Int, Int>>()
        val batteries = mutableListOf<Pair<Int, Int>>()
        var activeScriptsCount = 0
        
        var totalSources = 0
        var totalLoads = 0
        
        var calculatedTotalVoltage = 0f
        var calculatedTotalResistance = 1f // Base wire resistance

        // Prep the grid for state changes this tick (like PULSE_GENERATOR)
        val prepGrid = Array(width) { x ->
            Array(height) { y ->
                val comp = grid[x][y]
                var newLogicState = comp.logicState
                var newCharge = comp.charge
                var newOverloaded = comp.isOverloaded
                
                if (comp.type == ComponentType.PULSE_GENERATOR) {
                    val rate = comp.extraData.toLongOrNull() ?: 1L // tick rate
                    if (tick % rate == 0L) {
                        newLogicState = !newLogicState
                    }
                }
                
                val props = parseProps(comp.extraData)
                if (EnergyEngine.isPowerSource(comp.type)) {
                     val maxV = props["v"]?.toFloatOrNull() ?: EnergyEngine.getDefaultVoltage(comp.type)
                     val maxCharge = props["c"]?.toFloatOrNull() ?: EnergyEngine.getDefaultCapacity(comp.type)
                     
                     // Initialize charge
                     if (newCharge == -1f) {
                         newCharge = maxCharge
                     }
                     
                     if (newCharge > 0 || comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INFINITE_BATTERY) {
                        val currentV = if (comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INFINITE_BATTERY) maxV else maxV * (newCharge / maxCharge)
                        if (currentV > 0.1f) {
                            calculatedTotalVoltage += currentV
                        } else {
                            newCharge = 0f // Dead
                        }
                     }
                }
                if (comp.type == ComponentType.RESISTOR) {
                     val r = props["r"]?.toFloatOrNull() ?: 330f
                     calculatedTotalResistance += r
                }
                
                // CPU logic and memory load
                if (comp.type == ComponentType.MICROCONTROLLER || comp.type == ComponentType.MEMORY_RAM || comp.type == ComponentType.MEMORY_ROM) {
                     val cores = props["cores"]?.toIntOrNull() ?: 1
                     val freq = props["mhz"]?.toFloatOrNull() ?: 16f
                     val mem = props["mem_kb"]?.toIntOrNull() ?: 2
                     
                     // more cores / freq / mem = less resistance (higher current draw)
                     val loadR = 1000f / (cores * (freq / 16f) * (mem / 2f).coerceAtLeast(1f))
                     calculatedTotalResistance += loadR.coerceAtLeast(10f)
                }
                
                comp.copy(logicState = newLogicState, charge = newCharge)
            }
        }
        
        runScripts(prepGrid, width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = prepGrid[x][y]
                if (EnergyEngine.isPowerSource(comp.type)) {
                    if (comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.GENERATOR || comp.type == ComponentType.SOLAR_PANEL || comp.type == ComponentType.WIND_TURBINE || comp.type == ComponentType.NUCLEAR_REACTOR || comp.type == ComponentType.GEOTHERMAL_GENERATOR || comp.type == ComponentType.HYDRO_GENERATOR || comp.type == ComponentType.THERMOELECTRIC_GENERATOR || comp.type == ComponentType.INFINITE_BATTERY || comp.charge > 0) {
                        batteries.add(Pair(x, y))
                        totalSources++
                    }
                } else if (comp.type == ComponentType.PULSE_GENERATOR && comp.logicState) {
                    batteries.add(Pair(x, y)) // Acts as a source when high
                    totalSources++
                }
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    activeScriptsCount++
                }
            }
        }

        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addAll(batteries)
        
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if(powered.contains(curr)) continue
            
            // Overloaded components break circuit
            if(prepGrid[curr.first][curr.second].isOverloaded) continue
            
            powered.add(curr)
            
            val type = prepGrid[curr.first][curr.second].type
            val extraProps = parseProps(prepGrid[curr.first][curr.second].extraData)
            calculatedTotalResistance += EnergyEngine.addDynamicResistanceForComponent(type, extraProps)
            
            if (EnergyEngine.isLoad(type)) {
                totalLoads++
            }

            val neighbors = listOf(
                Pair(curr.first - 1, curr.second),
                Pair(curr.first + 1, curr.second),
                Pair(curr.first, curr.second - 1),
                Pair(curr.first, curr.second + 1)
            )
            
            for (n in neighbors) {
                if (n.first in 0 until width && n.second in 0 until height) {
                    val nComp = prepGrid[n.first][n.second]
                    if (nComp.isOverloaded) continue // Skip broken elements
                    
                    // Special logic for switches/relays: they can only conduct if conditions match
                    var canConduct = canPassPower(nComp, curr.first, curr.second, n.first, n.second)
                    
                    if (canConduct && !powered.contains(n)) {
                        queue.add(n)
                    }
                }
            }
        }
        
        // --- Ohm's Law Simulation ---
        // Rather than simple series addition which causes voltage to drop in half when connecting 2 loads,
        // we'll approximate parallel loads: Total Current = Sum of individual load currents.
        val voltage = calculatedTotalVoltage
        var totalConductance = 0f // 1/R
        
        // Let's re-calculate conductance for the connected graph
        for (p in powered) {
             val comp = prepGrid[p.first][p.second]
             val props = parseProps(comp.extraData)
             val r = EnergyEngine.addDynamicResistanceForComponent(comp.type, props)
             // Only add significant loads to conductance, wires are just wires
             if (r > 5f) {
                 totalConductance += 1f / r
             }
        }
        
        // Add globally static resistors to conductance
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = prepGrid[x][y]
                if (comp.type == ComponentType.RESISTOR) {
                     val r = parseProps(comp.extraData)["r"]?.toFloatOrNull() ?: 330f
                     totalConductance += 1f / r
                }
                if (comp.type == ComponentType.MICROCONTROLLER || comp.type == ComponentType.MEMORY_RAM || comp.type == ComponentType.MEMORY_ROM) {
                     val props = parseProps(comp.extraData)
                     val cores = props["cores"]?.toIntOrNull() ?: 1
                     val freq = props["mhz"]?.toFloatOrNull() ?: 16f
                     val mem = props["mem_kb"]?.toIntOrNull() ?: 2
                     val loadR = 1000f / (cores * (freq / 16f) * (mem / 2f).coerceAtLeast(1f))
                     totalConductance += 1f / loadR.coerceAtLeast(10f)
                }
            }
        }

        // If nothing has resistance, default effective R is low (wire only), otherwise 1/Conductance
        val effectiveR = if (totalConductance > 0f) (1f / totalConductance).coerceAtLeast(5f) else 5f
        val resistance = effectiveR
        
        var current = 0f
        var power = 0f
        var shortCircuit = false
        
        if (totalSources > 0) {
            current = (voltage / effectiveR) * 1000f // mA
            
            if (totalConductance == 0f && powered.size > totalSources) {
                // Short circuit (only wires, no loads)
                shortCircuit = true
            }
            power = voltage * (current / 1000f) // W = V * I(A)
        }

        // Final pass to update grid states based on power and handle overloads/drains
        val newGrid = Array(width) { x ->
            Array(height) { y ->
                val wasPowered = powered.contains(Pair(x, y))
                val comp = prepGrid[x][y]
                
                var finalLogicState = comp.logicState
                var finalCharge = comp.charge
                var finalOverloaded = comp.isOverloaded
                
                // Relays toggle when powered
                if (comp.type == ComponentType.RELAY) {
                     if (wasPowered) finalLogicState = true else finalLogicState = false
                }
                
                // Wiring Overload Logic
                if (wasPowered && current > 500000f) { // Raised safe current to 500A so components don't randomly burn for basic circuits
                    val burnChance = (current - 500000f) / 1000000f
                    if (Math.random() < burnChance) {
                        finalOverloaded = true 
                    }
                }
                
                // Battery Drain
                if (wasPowered && comp.charge > 0f) {
                    if (comp.type == ComponentType.BATTERY || comp.type == ComponentType.BATTERY_PACK || comp.type == ComponentType.COIN_CELL) {
                        // Realistic-ish drain, slow down significantly so it doesn't drain in 1 second
                        val drainShare = if (totalSources > 0) current / totalSources else current
                        val mA_drain = drainShare * 0.00005f 
                        finalCharge = (comp.charge - mA_drain).coerceAtLeast(0f)
                    }
                }
                
                comp.copy(isPowered = wasPowered && !finalOverloaded, logicState = finalLogicState, charge = finalCharge, isOverloaded = finalOverloaded)
            }
        }
        
        // Apply Material and Physics Automata
        PhysicsEngine.simulateMaterials(newGrid, width, height, voltage)

        val telemetry = Telemetry(voltage, current, power, shortCircuit, activeScriptsCount)
        
        return SimulationResult(newGrid, telemetry)
    }

    /* private fun simulateMaterials(grid: Array<Array<GridComponent>>, width: Int, height: Int, voltage: Float) {
        val moved = Array(width) { BooleanArray(height) }
        
        // Steam and Fire go up, gravity things go down
        // Iterate bottom-up for gravity
        for (y in height - 1 downTo 0) {
            val xs = (0 until width).shuffled()
            for (x in xs) {
                if (moved[x][y]) continue
                
                val comp = grid[x][y]
                if (comp.type.category == ComponentCategory.MATERIALS && comp.type != ComponentType.GLASS && comp.type != ComponentType.STONE && comp.type != ComponentType.WOOD && comp.type != ComponentType.RUBBER && comp.type != ComponentType.DIAMOND && comp.type != ComponentType.COAL && comp.type != ComponentType.SPONGE && comp.type != ComponentType.URANIUM && comp.type != ComponentType.INFINITE_WATER && comp.type != ComponentType.INFINITE_LAVA && comp.type != ComponentType.VOID_HOLE && comp.type != ComponentType.FLUID_DRAIN) {
                    
                    var newX = x
                    var newY = y
                    var didMove = false
                    
                    val goesUp = comp.type == ComponentType.STEAM || comp.type == ComponentType.FIRE || comp.type == ComponentType.LIQUID_NITROGEN // Nitrogen boils fast
                    
                    val dirY = if (goesUp) -1 else 1
                    
                    // Try to move
                    if (y + dirY in 0 until height && grid[x][y + dirY].type == ComponentType.EMPTY) {
                        newY = y + dirY
                        didMove = true
                    } else if (y + dirY in 0 until height) {
                        val isFluid = comp.type == ComponentType.WATER || comp.type == ComponentType.LAVA || comp.type == ComponentType.OIL || comp.type == ComponentType.ACID || comp.type == ComponentType.SLIME || comp.type == ComponentType.GASOLINE || comp.type == ComponentType.LIQUID_NITROGEN
                        val isPowder = comp.type == ComponentType.SAND || comp.type == ComponentType.DIRT || comp.type == ComponentType.MAGIC_DUST || comp.type == ComponentType.ICE
                        
                        // Heavier powders push fluids up
                        val blockBelow = grid[x][y + dirY].type
                        val belowIsFluid = blockBelow == ComponentType.WATER || blockBelow == ComponentType.LAVA || blockBelow == ComponentType.OIL || blockBelow == ComponentType.ACID || blockBelow == ComponentType.GASOLINE
                        
                        if (isPowder && belowIsFluid && dirY == 1) {
                            // Swap
                            grid[x][y + 1] = comp
                            grid[x][y] = GridComponent(blockBelow)
                            moved[x][y + 1] = true
                            continue
                        }
                        
                        if (isFluid || isPowder || goesUp) {
                            // Try diagonal
                            val canLeft = x > 0 && grid[x - 1][y + dirY].type == ComponentType.EMPTY
                            val canRight = x < width - 1 && grid[x + 1][y + dirY].type == ComponentType.EMPTY
                            
                            if (canLeft && canRight) {
                                newX = if (Math.random() > 0.5) x - 1 else x + 1
                                newY = y + dirY
                                didMove = true
                            } else if (canLeft) {
                                newX = x - 1
                                newY = y + dirY
                                didMove = true
                            } else if (canRight) {
                                newX = x + 1
                                newY = y + dirY
                                didMove = true
                            } else if (isFluid || goesUp) {
                                // Liquids can spread horizontally
                                val canHLeft = x > 0 && grid[x - 1][y].type == ComponentType.EMPTY
                                val canHRight = x < width - 1 && grid[x + 1][y].type == ComponentType.EMPTY
                                
                                val flowChance = if (comp.type == ComponentType.LAVA || comp.type == ComponentType.SLIME) 0.2 else 0.8
                                
                                if (Math.random() < flowChance) {
                                    if (canHLeft && canHRight) {
                                        newX = if (Math.random() > 0.5) x - 1 else x + 1
                                        didMove = true
                                    } else if (canHLeft) {
                                        newX = x - 1
                                        didMove = true
                                    } else if (canHRight) {
                                        newX = x + 1
                                        didMove = true
                                    }
                                }
                            }
                        }
                    }
                    
                    if (didMove) {
                        grid[newX][newY] = comp
                        grid[x][y] = GridComponent()
                        moved[newX][newY] = true
                    }
                }
            }
        }
        
        // Element generation and interactions
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                val neighbors = listOf(Pair(x-1,y), Pair(x+1,y), Pair(x,y-1), Pair(x,y+1))
                
                // Generators
                if (comp.type == ComponentType.INFINITE_WATER) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height && grid[n.first][n.second].type == ComponentType.EMPTY && Math.random() < 0.2) grid[n.first][n.second] = GridComponent(ComponentType.WATER)
                } else if (comp.type == ComponentType.INFINITE_LAVA) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height && grid[n.first][n.second].type == ComponentType.EMPTY && Math.random() < 0.1) grid[n.first][n.second] = GridComponent(ComponentType.LAVA)
                } else if (comp.type == ComponentType.FLUID_DRAIN) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height && grid[n.first][n.second].type.category == ComponentCategory.MATERIALS && grid[n.first][n.second].type != ComponentType.INFINITE_WATER && grid[n.first][n.second].type != ComponentType.INFINITE_LAVA && grid[n.first][n.second].type != ComponentType.VOID_HOLE) grid[n.first][n.second] = GridComponent(ComponentType.EMPTY)
                } else if (comp.type == ComponentType.VOID_HOLE) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height && grid[n.first][n.second].type != ComponentType.VOID_HOLE) grid[n.first][n.second] = GridComponent(ComponentType.EMPTY)
                } else if (comp.type == ComponentType.SPONGE) {
                    for (i in -3..3) for (j in -3..3) if (x+i in 0 until width && y+j in 0 until height && grid[x+i][y+j].type == ComponentType.WATER) grid[x+i][y+j] = GridComponent(ComponentType.EMPTY)
                }
                
                // Reactions
                if (comp.type == ComponentType.LAVA) {
                    for (n in neighbors) {
                        if (n.first in 0 until width && n.second in 0 until height) {
                            val nType = grid[n.first][n.second].type
                            if (nType == ComponentType.WATER) {
                                grid[x][y] = GridComponent(ComponentType.STONE)
                                grid[n.first][n.second] = GridComponent(ComponentType.STEAM)
                                break
                            } else if (nType == ComponentType.WOOD || nType == ComponentType.COAL || nType == ComponentType.OIL || nType == ComponentType.GASOLINE) {
                                grid[n.first][n.second] = GridComponent(ComponentType.FIRE)
                            } else if (nType == ComponentType.ICE) {
                                grid[n.first][n.second] = GridComponent(ComponentType.WATER)
                                grid[x][y] = GridComponent(ComponentType.STONE)
                            }
                        }
                    }
                } else if (comp.type == ComponentType.FIRE) {
                    if (Math.random() < 0.1) { 
                        grid[x][y] = GridComponent(ComponentType.EMPTY) // Fire dies eventually
                    } else {
                        for (n in neighbors) {
                            if (n.first in 0 until width && n.second in 0 until height) {
                                val nType = grid[n.first][n.second].type
                                if (nType == ComponentType.WOOD || nType == ComponentType.COAL || nType == ComponentType.OIL || nType == ComponentType.GASOLINE) {
                                    if (Math.random() < 0.2) grid[n.first][n.second] = GridComponent(ComponentType.FIRE)
                                } else if (nType == ComponentType.WATER) {
                                    grid[x][y] = GridComponent(ComponentType.STEAM)
                                } else if (nType == ComponentType.ICE) {
                                    grid[n.first][n.second] = GridComponent(ComponentType.WATER)
                                    grid[x][y] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                        }
                    }
                } else if (comp.type == ComponentType.ACID) {
                    for (n in neighbors) {
                        if (n.first in 0 until width && n.second in 0 until height) {
                            val target = grid[n.first][n.second]
                            if (target.type != ComponentType.EMPTY && target.type != ComponentType.ACID && target.type != ComponentType.GLASS && target.type != ComponentType.DIAMOND && target.type != ComponentType.VOID_HOLE) {
                                if (Math.random() < 0.1) { // 10% chance to melt block per tick
                                    grid[n.first][n.second] = GridComponent(ComponentType.ACID)
                                    grid[x][y] = GridComponent(ComponentType.EMPTY) // Acid gets consumed slightly
                                    break // One reaction at a time
                                }
                            }
                        }
                    }
                } else if (comp.type == ComponentType.LIQUID_NITROGEN) {
                    if (Math.random() < 0.2) grid[x][y] = GridComponent(ComponentType.EMPTY) // Evaporates
                    for (n in neighbors) {
                        if (n.first in 0 until width && n.second in 0 until height) {
                            val nType = grid[n.first][n.second].type
                            if (nType == ComponentType.WATER) grid[n.first][n.second] = GridComponent(ComponentType.ICE)
                            if (nType == ComponentType.FIRE) grid[n.first][n.second] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                } else if (comp.type == ComponentType.STEAM) {
                    if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.WATER) // Condense
                    else if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.EMPTY) // Dissipate
                }
                
                // Component Interactions (Powered Heaters, Coolers)
                if (comp.isPowered && comp.type == ComponentType.HEATER) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height) {
                        val nt = grid[n.first][n.second].type
                        if (nt == ComponentType.WATER) grid[n.first][n.second] = GridComponent(ComponentType.STEAM)
                        if (nt == ComponentType.ICE) grid[n.first][n.second] = GridComponent(ComponentType.WATER)
                    }
                }
                if (comp.isPowered && comp.type == ComponentType.COOLER) {
                    for (n in neighbors) if (n.first in 0 until width && n.second in 0 until height) {
                        val nt = grid[n.first][n.second].type
                        if (nt == ComponentType.WATER) grid[n.first][n.second] = GridComponent(ComponentType.ICE)
                        if (nt == ComponentType.STEAM) grid[n.first][n.second] = GridComponent(ComponentType.WATER)
                    }
                }
                
                // Conveyor Belt
                if (comp.isPowered && comp.type == ComponentType.CONVEYOR_BELT) {
                    val moveX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                    val moveY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                    val topY = y - 1 // Item on top
                    if (topY >= 0 && grid[x][topY].type.category == ComponentCategory.MATERIALS && !moved[x][topY]) {
                        val destX = x + moveX
                        val destY = topY + moveY
                        if (destX in 0 until width && destY in 0 until height && grid[destX][destY].type == ComponentType.EMPTY) {
                            grid[destX][destY] = grid[x][topY]
                            grid[x][topY] = GridComponent()
                            moved[destX][destY] = true
                        }
                    }
                }
                
                // Magnet
                if (comp.isPowered && comp.type == ComponentType.MAGNET) {
                    val radius = java.lang.Math.max(1, (voltage / 5).toInt().coerceIn(1, 5))
                    // Pull materials towards it
                    val pulls = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                    for (dir in pulls) {
                        for (dist in 1..radius) {
                            val dx = when(dir) { Direction.RIGHT -> dist; Direction.LEFT -> -dist; else -> 0 }
                            val dy = when(dir) { Direction.DOWN -> dist; Direction.UP -> -dist; else -> 0 }
                            val sx = x + dx
                            val sy = y + dy
                            if (sx in 0 until width && sy in 0 until height) {
                                val t = grid[sx][sy]
                                if (t.type.category == ComponentCategory.MATERIALS && !moved[sx][sy]) {
                                    // Move one step closer
                                    val nx = sx - Integer.signum(dx)
                                    val ny = sy - Integer.signum(dy)
                                    if (grid[nx][ny].type == ComponentType.EMPTY) {
                                        grid[nx][ny] = t
                                        grid[sx][sy] = GridComponent()
                                        moved[nx][ny] = true
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Piston
                if (comp.isPowered && comp.type == ComponentType.PISTON) {
                    val moveX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                    val moveY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                    var currentPushX = x + moveX
                    var currentPushY = y + moveY
                    // Push up to 3 blocks
                    var length = 0
                    while(currentPushX in 0 until width && currentPushY in 0 until height && grid[currentPushX][currentPushY].type.category == ComponentCategory.MATERIALS && length < 3) {
                        length++
                        currentPushX += moveX
                        currentPushY += moveY
                    }
                    if (currentPushX in 0 until width && currentPushY in 0 until height && grid[currentPushX][currentPushY].type == ComponentType.EMPTY && length > 0) {
                        // We can push. Go backwards from currentPushX, currentPushY
                        var pullX = currentPushX
                        var pullY = currentPushY
                        for (i in 0 until length) {
                            val nextPullX = pullX - moveX
                            val nextPullY = pullY - moveY
                            grid[pullX][pullY] = grid[nextPullX][nextPullY]
                            moved[pullX][pullY] = true
                            pullX = nextPullX
                            pullY = nextPullY
                        }
                        grid[x + moveX][y + moveY] = GridComponent() // Clear first spot
                    }
                }
            }
        }
    } */
    
    fun serializeGrid(grid: Array<Array<GridComponent>>, width: Int, height: Int): String {
        val sb = StringBuilder()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type != ComponentType.EMPTY && comp.type.category.name != "TOOLS") {
                    val ext = if (comp.extraData.isNotEmpty()) {
                        Base64.encodeToString(comp.extraData.toByteArray(), Base64.NO_WRAP)
                    } else ""
                    sb.append("$x,$y,${comp.type.name},${comp.direction.name},$ext;")
                }
            }
        }
        return sb.toString()
    }
    
    fun deserializeGrid(data: String, width: Int, height: Int): Array<Array<GridComponent>> {
        val newGrid = Array(width) { Array(height) { GridComponent() } }
        if (data.isEmpty()) return newGrid
        
        val parts = data.split(";")
        for (part in parts) {
            if (part.isNotBlank()) {
                val tokens = part.split(",")
                if (tokens.size >= 4) {
                    val x = tokens[0].toIntOrNull() ?: continue
                    val y = tokens[1].toIntOrNull() ?: continue
                    val type = try { ComponentType.valueOf(tokens[2]) } catch(e: Exception) { ComponentType.EMPTY }
                    val dir = try { Direction.valueOf(tokens[3]) } catch(e: Exception) { Direction.UP }
                    val ext = if (tokens.size > 4 && tokens[4].isNotEmpty()) {
                        try { String(Base64.decode(tokens[4], Base64.NO_WRAP)) } catch (e: Exception) { "" }
                    } else ""
                    
                    if (x in 0 until width && y in 0 until height && type.category.name != "TOOLS") {
                        newGrid[x][y] = GridComponent(type = type, direction = dir, extraData = ext)
                    }
                }
            }
        }
        return newGrid
    }
}
