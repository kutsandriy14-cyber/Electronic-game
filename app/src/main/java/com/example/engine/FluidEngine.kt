package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import java.util.LinkedList

object FluidEngine {

    private fun isPassable(type: ComponentType): Boolean {
        return type == ComponentType.EMPTY ||
               type == ComponentType.WATER ||
               type == ComponentType.LAVA ||
               type == ComponentType.OIL ||
               type == ComponentType.ACID ||
               type == ComponentType.GASOLINE ||
               type == ComponentType.SLIME ||
               type == ComponentType.LIQUID_NITROGEN ||
               type == ComponentType.STEAM ||
               type == ComponentType.FIRE
    }

    private fun isSolidWall(type: ComponentType): Boolean {
        return type == ComponentType.STONE ||
               type == ComponentType.STEEL ||
               type == ComponentType.COPPER ||
               type == ComponentType.GOLD ||
               type == ComponentType.ALUMINUM ||
               type == ComponentType.PLASTIC ||
               type == ComponentType.CLAY ||
               type == ComponentType.BRICK ||
               type == ComponentType.OBSIDIAN ||
               type == ComponentType.BEDROCK ||
               type == ComponentType.GLASS ||
               type == ComponentType.WOOD
    }

    fun calculatePressureAndLeaks(
        grid: Array<Array<GridComponent>>,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        // Reset and recalculate pressure for all cells.
        // Atmospheric or base pressure for empty is 0. Pipes usually have local pressure.
        val nextPressure = Array(width) { FloatArray(height) { 0f } }

        // Find rooms (connected groups of passable elements)
        val visited = Array(width) { BooleanArray(height) }
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!visited[x][y] && isPassable(grid[x][y].type)) {
                    // BFS to find the whole room
                    val queue = LinkedList<Pair<Int, Int>>()
                    val roomCells = mutableListOf<Pair<Int, Int>>()
                    var isClosed = true
                    var sourceCount = 0

                    queue.add(Pair(x, y))
                    visited[x][y] = true

                    while (queue.isNotEmpty()) {
                        val curr = queue.poll()
                        roomCells.add(curr)

                        // If at the boundary, this room is open to the atmosphere
                        if (curr.first == 0 || curr.first == width - 1 || curr.second == 0 || curr.second == height - 1) {
                            isClosed = false
                        }

                        // Count active sources or high pressure nodes
                        val compType = grid[curr.first][curr.second].type
                        if (compType == ComponentType.INFINITE_WATER || compType == ComponentType.INFINITE_LAVA) {
                            sourceCount++
                        }

                        for (i in 0..3) {
                            val nx = curr.first + dx[i]
                            val ny = curr.second + dy[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val neighborType = grid[nx][ny].type
                                if (neighborType == ComponentType.INFINITE_WATER || neighborType == ComponentType.INFINITE_LAVA) {
                                    sourceCount++
                                }
                                if (!visited[nx][ny] && isPassable(neighborType)) {
                                    visited[nx][ny] = true
                                    queue.add(Pair(nx, ny))
                                }
                            }
                        }
                    }

                    // Enclosed rooms build up pressure if they have sources or infinite inflows
                    if (isClosed && roomCells.isNotEmpty()) {
                        // Base pressure on number of active fluid streams and room size
                        // Smaller rooms with active fluid inflows experience higher pressure
                        val fluidInCellCount = roomCells.count { 
                            val t = grid[it.first][it.second].type
                            t == ComponentType.WATER || t == ComponentType.LAVA || t == ComponentType.OIL || t == ComponentType.ACID 
                        }
                        
                        val ratio = if (roomCells.size > 0) fluidInCellCount.toFloat() / roomCells.size else 0f
                        var roomPressure = ratio * 120f
                        if (sourceCount > 0) {
                            roomPressure += (sourceCount * 30f)
                        }

                        // Apply room pressure to all cells inside this closed room
                        for (cell in roomCells) {
                            nextPressure[cell.first][cell.second] = roomPressure.coerceAtLeast(0f)
                        }

                        // Leaks trigger on extremely high room pressure (> 100)
                        if (roomPressure > 100f && Math.random() < 0.1) {
                            // Find solid walls bounding this room and crack them!
                            val boundingWalls = mutableListOf<Pair<Int, Int>>()
                            for (cell in roomCells) {
                                for (i in 0..3) {
                                    val wx = cell.first + dx[i]
                                    val wy = cell.second + dy[i]
                                    if (wx in 0 until width && wy in 0 until height) {
                                        val wallT = grid[wx][wy].type
                                        if (isSolidWall(wallT) && wallT != ComponentType.BEDROCK) {
                                            boundingWalls.add(Pair(wx, wy))
                                        }
                                    }
                                }
                            }
                            if (boundingWalls.isNotEmpty()) {
                                val leakedWall = boundingWalls.random()
                                // Turn solid wall to empty (cracked leak!)
                                grid[leakedWall.first][leakedWall.second] = GridComponent(ComponentType.EMPTY)
                            }
                        }
                    }
                }
            }
        }

        // --- Pipe Pressure Simulation ---
        // Let's identify pipe lines and track pressure
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.PIPE) {
                    val inDx = when(comp.direction) { Direction.RIGHT -> -1; Direction.LEFT -> 1; else -> 0 }
                    val inDy = when(comp.direction) { Direction.DOWN -> -1; Direction.UP -> 1; else -> 0 }
                    val outDx = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                    val outDy = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                    
                    val inX = x + inDx
                    val inY = y + inDy
                    
                    if (inX in 0 until width && inY in 0 until height) {
                        val inType = grid[inX][inY].type
                        val isFluidConnected = inType == ComponentType.WATER || inType == ComponentType.LAVA || inType == ComponentType.OIL || inType == ComponentType.ACID || inType == ComponentType.GASOLINE || inType == ComponentType.PIPE
                        
                        if (isFluidConnected) {
                            // Find output blockage to build pressure
                            var currOutX = x + outDx
                            var currOutY = y + outDy
                            var pipeLength = 1
                            var isBlocked = true
                            
                            while (currOutX in 0 until width && currOutY in 0 until height) {
                                val nextType = grid[currOutX][currOutY].type
                                if (nextType == ComponentType.PIPE) {
                                    val pipeDir = grid[currOutX][currOutY].direction
                                    val nextOutDx = when(pipeDir) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                                    val nextOutDy = when(pipeDir) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                                    currOutX += nextOutDx
                                    currOutY += nextOutDy
                                    pipeLength++
                                } else if (nextType == ComponentType.EMPTY || nextType == ComponentType.FLUID_DRAIN || nextType == ComponentType.VOID_HOLE) {
                                    isBlocked = false
                                    break
                                } else {
                                    break
                                }
                            }
                            
                            var pipePressure = 15f * pipeLength
                            if (isBlocked) {
                                pipePressure += 70f
                            }
                            nextPressure[x][y] = pipePressure.coerceIn(0f, 150f)

                            // Break pipe if pressure is too high! (Utečka / leak)
                            if (pipePressure > 100f && Math.random() < 0.15) {
                                grid[x][y] = GridComponent(ComponentType.EMPTY) // pipe burst!
                                // turn into water/liquid
                                val adjacentX = x + outDx
                                val adjacentY = y + outDy
                                if (adjacentX in 0 until width && adjacentY in 0 until height && grid[adjacentX][adjacentY].type == ComponentType.EMPTY) {
                                    grid[adjacentX][adjacentY] = GridComponent(ComponentType.WATER)
                                    moved[adjacentX][adjacentY] = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Pressure Sensor Check ---
        // Pressure sensors output logicState = true if they detect pressure > 35f in adjacent cells
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.PRESSURE_SENSOR) {
                    var detectsPressure = false
                    var maxSeenPressure = 0f
                    for (i in 0..3) {
                        val nx = x + dx[i]
                        val ny = y + dy[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val p = nextPressure[nx][ny]
                            if (p > maxSeenPressure) {
                                maxSeenPressure = p
                            }
                        }
                    }
                    if (maxSeenPressure > 35f) {
                        detectsPressure = true
                    }
                    
                    grid[x][y] = comp.copy(
                        logicState = detectsPressure,
                        isPowered = detectsPressure,
                        voltage = if (detectsPressure) 5f else 0f,
                        current = if (detectsPressure) 20f else 0f,
                        pressure = maxSeenPressure
                    )
                }
            }
        }

        // Write back calculated pressures to grid
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (grid[x][y].type != ComponentType.PRESSURE_SENSOR) {
                    val p = nextPressure[x][y]
                    grid[x][y] = grid[x][y].copy(pressure = p)
                }
            }
        }
    }
}
