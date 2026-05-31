package com.example.engine

import android.util.Base64
import com.example.model.ComponentType
import com.example.model.ComponentCategory
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.SimulationResult
import com.example.model.Telemetry

class CircuitEngine {

    private fun runScripts(grid: Array<Array<GridComponent>>, width: Int, height: Int): List<String> {
        val logs = mutableListOf<String>()
        val originalGrid = Array(width) { x -> Array(height) { y -> grid[x][y].copy() } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    val lines = comp.extraData.split('\n')
                    for (line in lines) {
                        var parsedLine = line.trim()
                        if (parsedLine.isEmpty() || parsedLine.startsWith("--") || parsedLine.startsWith("//")) continue
                        
                        // Replace 'in(pin)' with '1' or '0'
                        for (p in 0..3) {
                            if (parsedLine.contains("in($p)")) {
                                val nx = when(p) { 0 -> x; 1 -> x + 1; 2 -> x; 3 -> x - 1; else -> -1 }
                                val ny = when(p) { 0 -> y - 1; 1 -> y; 2 -> y + 1; 3 -> y; else -> -1 }
                                val v = if (nx in 0 until width && ny in 0 until height && originalGrid[nx][ny].logicState) "1" else "0"
                                parsedLine = parsedLine.replace("in($p)", v)
                            }
                        }
                        
                        // Handle simple 'if ... then'
                        var shouldExecute = true
                        var commandPart = parsedLine
                        
                        if (parsedLine.startsWith("if")) {
                            var cond = parsedLine.substringAfter("if").substringBefore("then")
                            if (cond == parsedLine.substringAfter("if")) {
                                cond = parsedLine.substringAfter("if").substringBefore("out")
                            }
                            cond = cond.trim()
                            
                            // Basic false condition check (like if 0, if false)
                            // Evaluates simple equality `1 == 1` -> let's just cheat
                            if (cond.contains("0 == 1") || cond.contains("1 == 0") || cond == "0" || cond == "false" || cond == "!1" || cond.contains("0==") || cond.contains("==0")) {
                                shouldExecute = false
                            }
                            
                            if (parsedLine.contains("then")) {
                                commandPart = parsedLine.substringAfter("then").trim()
                            } else if (parsedLine.contains("out(")) {
                                commandPart = "out(" + parsedLine.substringAfter("out(")
                            } else if (parsedLine.contains("log(")) {
                                commandPart = "log(" + parsedLine.substringAfter("log(")
                            }
                        }
                        
                        if (shouldExecute) {
                            if (commandPart.startsWith("log(")) {
                                val msg = commandPart.substringAfter("log(").substringBeforeLast(")")
                                logs.add("[MCU $x,$y]: $msg")
                            } else if (commandPart.startsWith("out(")) {
                                val outPart = commandPart.substringAfter("out(").substringBefore(")")
                                val args = outPart.split(",")
                                if (args.size == 2) {
                                    val pin = args[0].trim().toIntOrNull() ?: 0
                                    // eval simple expression like '1' or '1 == 1'
                                    val expr = args[1].trim()
                                    val isHigh = expr == "1" || expr == "true" || expr == "1==1" || expr == "1 == 1"
                                    
                                    val nx = when(pin) { 0 -> x; 1 -> x + 1; 2 -> x; 3 -> x - 1; else -> -1 }
                                    val ny = when(pin) { 0 -> y - 1; 1 -> y; 2 -> y + 1; 3 -> y; else -> -1 }
                                    
                                    if (nx in 0 until width && ny in 0 until height) {
                                        grid[nx][ny] = grid[nx][ny].copy(logicState = isHigh)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return logs
    }

    fun calculatePower(grid: Array<Array<GridComponent>>, width: Int, height: Int, tick: Long = 0): SimulationResult {
        var activeScriptsCount = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                var newLogicState = comp.logicState
                
                if (comp.type == ComponentType.PULSE_GENERATOR) {
                    val rate = comp.extraData.toLongOrNull() ?: 1L // tick rate
                    if (tick % rate == 0L) {
                        newLogicState = !newLogicState
                    }
                }
                
                val newCharge = if (EnergyEngine.isPowerSource(comp.type) && comp.charge == -1f) {
                     val props = EnergyEngine.parseProps(comp.extraData)
                     props["c"]?.toFloatOrNull() ?: EnergyEngine.getDefaultCapacity(comp.type)
                } else {
                     comp.charge
                }
                
                if (comp.logicState != newLogicState || comp.charge != newCharge) {
                    grid[x][y] = comp.copy(logicState = newLogicState, charge = newCharge)
                }
                
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    activeScriptsCount++
                }
            }
        }
        
        val logs = runScripts(grid, width, height)

        val energyResult = EnergyEngine.simulateEnergyFlow(grid, width, height, activeScriptsCount)
        PhysicsEngine.simulateMaterials(grid, width, height, energyResult.telemetry.totalVoltage)

        return SimulationResult(grid, energyResult.telemetry, logs)
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
