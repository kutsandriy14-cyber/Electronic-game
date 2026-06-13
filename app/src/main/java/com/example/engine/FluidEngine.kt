package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.functional.Fluid
import java.util.LinkedList

object FluidEngine {

    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    private fun isPassable(type: ComponentType): Boolean {
        return Fluid.isPassable(type)
    }

    private fun isSolidWall(type: ComponentType): Boolean {
        return Fluid.isSolidWall(type)
    }

    fun calculatePressureAndLeaks(
        grid: Array<Array<GridComponent>>,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        val nextPressure = Array(width) { FloatArray(height) { 0f } }

        val visited = Array(width) { BooleanArray(height) }
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!visited[x][y] && isPassable(grid[x][y].type)) {
                    val queue = LinkedList<Pair<Int, Int>>()
                    val roomCells = mutableListOf<Pair<Int, Int>>()
                    var isClosed = true
                    var sourceCount = 0

                    queue.add(Pair(x, y))
                    visited[x][y] = true

                    while (queue.isNotEmpty()) {
                        val curr = queue.poll()
                        roomCells.add(curr)

                        if ((curr.first == 0 || curr.first == width - 1 || curr.second == 0 || curr.second == height - 1) && grid[curr.first][curr.second].type == ComponentType.EMPTY) {
                            isClosed = false
                        }

                        val compType = grid[curr.first][curr.second].type
                        if (compType == ComponentType.INFINITE_WATER || 
                            compType == ComponentType.INFINITE_LAVA ||
                            compType == ComponentType.INFINITE_OIL ||
                            compType == ComponentType.INFINITE_ACID ||
                            compType == ComponentType.INFINITE_SLIME ||
                            compType == ComponentType.INFINITE_GASOLINE ||
                            compType == ComponentType.INFINITE_LIQUID_NITROGEN ||
                            compType == ComponentType.INFINITE_STEAM) {
                            sourceCount++
                        }

                        for (i in 0..3) {
                            val nx = curr.first + dx[i]
                            val ny = curr.second + dy[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val neighborType = grid[nx][ny].type
                                if (!visited[nx][ny] && isPassable(neighborType)) {
                                    visited[nx][ny] = true
                                    queue.add(Pair(nx, ny))
                                }
                            }
                        }
                    }

                    if (isClosed && roomCells.isNotEmpty()) {
                        val fluidInCellCount = roomCells.count { 
                            val t = grid[it.first][it.second].type
                            Fluid.isFluid(t)
                        }
                        
                        val ratio = if (roomCells.size > 0) fluidInCellCount.toFloat() / roomCells.size else 0f
                        var roomPressure = ratio * 120f
                        if (sourceCount > 0) {
                            roomPressure += (sourceCount * 30f)
                        }

                        for (cell in roomCells) {
                            nextPressure[cell.first][cell.second] = roomPressure.coerceAtLeast(0f)
                        }

                        if (roomPressure > 100f && rand() < 0.1) {
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
                                val leakedWall = boundingWalls.random(kotlin.random.Random(rng.get()!!.nextLong()))
                                grid[leakedWall.first][leakedWall.second] = GridComponent(ComponentType.EMPTY)
                            }
                        }
                    }
                }
            }
        }

        // Pipe direction convention: the direction indicates FLOW DIRECTION (output side).
        // INPUT comes from the opposite side. Direction.RIGHT: input from left (inDx=-1),
        // output to right (outDx=+1).
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
                        val isFluidConnected = Fluid.isFluid(inType) || inType == ComponentType.PIPE
                        
                        if (isFluidConnected) {
                            var currOutX = x + outDx
                            var currOutY = y + outDy
                            var pipeLength = 1
                            var isBlocked = true
                            
                            val visitedPipes = mutableSetOf<Pair<Int, Int>>()
                            visitedPipes.add(Pair(x, y))
                            while (currOutX in 0 until width && currOutY in 0 until height) {
                                val outPos = Pair(currOutX, currOutY)
                                if (visitedPipes.contains(outPos)) {
                                    break
                                }
                                visitedPipes.add(outPos)
                                
                                val nextType = grid[currOutX][currOutY].type
                                if (nextType == ComponentType.PIPE) {
                                    val pipeDir = grid[currOutX][currOutY].direction
                                    val nextOutDx = when(pipeDir) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                                    val nextOutDy = when(pipeDir) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                                    if (nextOutDx == 0 && nextOutDy == 0) {
                                        break
                                    }
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

                            if (pipePressure > 100f && rand() < 0.15) {
                                val inFluidType = if (inX in 0 until width && inY in 0 until height) grid[inX][inY].type else ComponentType.WATER
                                val spiltFluid = if (Fluid.isFluid(inFluidType)) inFluidType else ComponentType.WATER
                                grid[x][y] = GridComponent(ComponentType.EMPTY) 
                                val adjacentX = x + outDx
                                val adjacentY = y + outDy
                                if (adjacentX in 0 until width && adjacentY in 0 until height && grid[adjacentX][adjacentY].type == ComponentType.EMPTY) {
                                    grid[adjacentX][adjacentY] = GridComponent(spiltFluid)
                                    moved[adjacentX][adjacentY] = true
                                }
                            }
                        }
                    }
                }
            }
        }

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
                        pressure = maxSeenPressure
                    )
                }
            }
        }

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
