package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.GridComponent

object PhysicsEngine {

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

    private fun canSqueeze(grid: Array<Array<GridComponent>>, x1: Int, y1: Int, x2: Int, y2: Int, width: Int, height: Int): Boolean {
        if (x1 == x2 || y1 == y2) return true
        val s1 = x2 in 0 until width && !isPassable(grid[x2][y1].type)
        val s2 = y2 in 0 until height && !isPassable(grid[x1][y2].type)
        return !(s1 && s2)
    }

    fun simulateMaterials(grid: Array<Array<GridComponent>>, width: Int, height: Int, voltage: Float) {
        val moved = Array(width) { BooleanArray(height) }
        
        // Steam and Fire go up, gravity things go down
        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        // Iterate bottom-up for gravity
        for (y in height - 1 downTo 0) {
            val goRight = Math.random() < 0.5
            val startX = if (goRight) 0 else width - 1
            val endX = if (goRight) width - 1 else 0
            val stepX = if (goRight) 1 else -1
            
            var x = startX
            while (true) {
                if (!moved[x][y]) {
                    val comp = grid[x][y]
                    
                    // Fluids, powders, gases fall or move
                    if (comp.type.category == ComponentCategory.MATERIALS && 
                        comp.type != ComponentType.GLASS && 
                        comp.type != ComponentType.STONE && 
                        comp.type != ComponentType.WOOD && 
                        comp.type != ComponentType.RUBBER && 
                        comp.type != ComponentType.DIAMOND && 
                        comp.type != ComponentType.COAL && 
                        comp.type != ComponentType.SPONGE && 
                        comp.type != ComponentType.URANIUM && 
                        comp.type != ComponentType.INFINITE_WATER && 
                        comp.type != ComponentType.INFINITE_LAVA && 
                        comp.type != ComponentType.VOID_HOLE && 
                        comp.type != ComponentType.FLUID_DRAIN && 
                        comp.type != ComponentType.STEEL && 
                        comp.type != ComponentType.COPPER && 
                        comp.type != ComponentType.GOLD && 
                        comp.type != ComponentType.ALUMINUM && 
                        comp.type != ComponentType.PLASTIC && 
                        comp.type != ComponentType.CLAY && 
                        comp.type != ComponentType.BRICK && 
                        comp.type != ComponentType.OBSIDIAN && 
                        comp.type != ComponentType.BEDROCK &&
                        comp.type != ComponentType.PIPE) {
                        
                        var newX = x
                        var newY = y
                        var didMove = false
                        
                        val goesUp = comp.type == ComponentType.STEAM || comp.type == ComponentType.FIRE || comp.type == ComponentType.LIQUID_NITROGEN
                        val dirY = if (goesUp) -1 else 1
                        
                        // Try falling/rising orthagonally
                        if (y + dirY in 0 until height && grid[x][y + dirY].type == ComponentType.EMPTY) {
                            newY = y + dirY
                            didMove = true
                        } else if (y + dirY in 0 until height) {
                            val isFluid = comp.type == ComponentType.WATER || comp.type == ComponentType.LAVA || comp.type == ComponentType.OIL || comp.type == ComponentType.ACID || comp.type == ComponentType.SLIME || comp.type == ComponentType.GASOLINE || comp.type == ComponentType.LIQUID_NITROGEN
                            val isPowder = comp.type == ComponentType.SAND || comp.type == ComponentType.DIRT || comp.type == ComponentType.MAGIC_DUST || comp.type == ComponentType.ICE
                            
                            val blockBelow = grid[x][y + dirY].type
                            val belowIsFluid = blockBelow == ComponentType.WATER || blockBelow == ComponentType.LAVA || blockBelow == ComponentType.OIL || blockBelow == ComponentType.ACID || blockBelow == ComponentType.GASOLINE
                            
                            // Powders slide through fluids
                            if (isPowder && belowIsFluid) {
                                grid[x][y] = grid[x][y + dirY]
                                grid[x][y + dirY] = comp
                                moved[x][y + dirY] = true
                                moved[x][y] = true
                                continue
                            } else if (isFluid) {
                                val goLeftFirst = Math.random() < 0.5
                                val dir1 = if (goLeftFirst) -1 else 1
                                val dir2 = if (goLeftFirst) 1 else -1
                                var sideMoved = false
                                
                                // Slide sideways
                                if (x + dir1 in 0 until width && grid[x + dir1][y].type == ComponentType.EMPTY) {
                                    newX = x + dir1
                                    didMove = true
                                    sideMoved = true
                                } else if (x + dir2 in 0 until width && grid[x + dir2][y].type == ComponentType.EMPTY) {
                                    newX = x + dir2
                                    didMove = true
                                    sideMoved = true
                                }
                                
                                // Slide diagonally ONLY IF we can squeeze through corner!
                                if (!sideMoved) {
                                    if (x + dir1 in 0 until width && y + dirY in 0 until height && 
                                        grid[x + dir1][y + dirY].type == ComponentType.EMPTY &&
                                        canSqueeze(grid, x, y, x + dir1, y + dirY, width, height)) {
                                        newX = x + dir1
                                        newY = y + dirY
                                        didMove = true
                                    } else if (x + dir2 in 0 until width && y + dirY in 0 until height && 
                                        grid[x + dir2][y + dirY].type == ComponentType.EMPTY &&
                                        canSqueeze(grid, x, y, x + dir2, y + dirY, width, height)) {
                                        newX = x + dir2
                                        newY = y + dirY
                                        didMove = true
                                    }
                                }
                            } else {
                                // Slide diagonally (for powders) if can squeeze corner
                                val goLeftFirst = Math.random() < 0.5
                                val dir1 = if (goLeftFirst) -1 else 1
                                val dir2 = if (goLeftFirst) 1 else -1
                                if (x + dir1 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir1][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir1, y + dirY, width, height)) {
                                    newX = x + dir1
                                    newY = y + dirY
                                    didMove = true
                                } else if (x + dir2 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir2][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir2, y + dirY, width, height)) {
                                    newX = x + dir2
                                    newY = y + dirY
                                    didMove = true
                                }
                            }
                        }
                        
                        if (didMove) {
                            grid[newX][newY] = comp
                            grid[x][y] = GridComponent(type = ComponentType.EMPTY)
                            moved[newX][newY] = true
                        }
                        
                        // Chemical/physical reactions on the moved element
                        val currentX = if (didMove) newX else x
                        val currentY = if (didMove) newY else y
                        val movedComp = grid[currentX][currentY]
                        
                        // Water interaction
                        if (movedComp.type == ComponentType.WATER || movedComp.type == ComponentType.INFINITE_WATER) {
                            for (dx in -1..1) {
                                for (dy in -1..1) {
                                    if (currentX + dx in 0 until width && currentY + dy in 0 until height) {
                                        val targetT = grid[currentX + dx][currentY + dy].type
                                        if (targetT == ComponentType.LAVA || targetT == ComponentType.INFINITE_LAVA) {
                                            grid[currentX][currentY] = GridComponent(type = ComponentType.STONE)
                                            grid[currentX + dx][currentY + dy] = GridComponent(type = ComponentType.STONE)
                                        } else if (targetT == ComponentType.SPONGE) {
                                            grid[currentX][currentY] = GridComponent(type = ComponentType.EMPTY)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Fire interaction
                        if (movedComp.type == ComponentType.FIRE) {
                            for (dx in -1..1) {
                                for (dy in -1..1) {
                                    if (currentX + dx in 0 until width && currentY + dy in 0 until height) {
                                        val adj = grid[currentX + dx][currentY + dy].type
                                        if (adj == ComponentType.WOOD || adj == ComponentType.COAL) {
                                            if (Math.random() < 0.05) {
                                                grid[currentX + dx][currentY + dy] = GridComponent(type = ComponentType.FIRE)
                                            }
                                        } else if (adj == ComponentType.WATER) {
                                            grid[currentX][currentY] = GridComponent(type = ComponentType.STEAM)
                                        } else if (adj == ComponentType.OIL || adj == ComponentType.GASOLINE) {
                                            grid[currentX + dx][currentY + dy] = GridComponent(type = ComponentType.FIRE)
                                        }
                                    }
                                }
                            }
                            if (Math.random() < 0.1) {
                                grid[currentX][currentY] = GridComponent(type = ComponentType.EMPTY)
                            }
                        } else if (movedComp.type == ComponentType.STEAM && Math.random() < 0.05) {
                            grid[currentX][currentY] = GridComponent(type = ComponentType.EMPTY) 
                        } else if (movedComp.type == ComponentType.LIQUID_NITROGEN && Math.random() < 0.1) {
                            grid[currentX][currentY] = GridComponent(type = ComponentType.EMPTY)
                        } else if (movedComp.type == ComponentType.ACID) {
                            if (currentY + 1 in 0 until height) {
                                val adj = grid[currentX][currentY + 1].type
                                if (adj != ComponentType.EMPTY && adj != ComponentType.GLASS && adj != ComponentType.ACID && adj != ComponentType.OBSIDIAN && adj != ComponentType.BEDROCK) {
                                    if (Math.random() < 0.2) {
                                        grid[currentX][currentY + 1] = GridComponent(type = ComponentType.EMPTY)
                                        if (Math.random() < 0.5) {
                                            grid[currentX][currentY] = GridComponent(type = ComponentType.EMPTY)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (comp.type == ComponentType.INFINITE_WATER) {
                        if (y + 1 in 0 until height && grid[x][y+1].type == ComponentType.EMPTY) {
                             grid[x][y+1] = GridComponent(type = ComponentType.WATER)
                             moved[x][y+1] = true
                        }
                    } else if (comp.type == ComponentType.INFINITE_LAVA) {
                        if (y + 1 in 0 until height && grid[x][y+1].type == ComponentType.EMPTY) {
                             grid[x][y+1] = GridComponent(type = ComponentType.LAVA)
                             moved[x][y+1] = true
                        }
                    } else if (comp.type == ComponentType.VOID_HOLE || comp.type == ComponentType.FLUID_DRAIN) {
                        val isDrainOnly = comp.type == ComponentType.FLUID_DRAIN
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                if (x + dx in 0 until width && y + dy in 0 until height) {
                                    val adj = grid[x + dx][y + dy]
                                    if (adj.type != ComponentType.EMPTY && adj.type != ComponentType.VOID_HOLE && adj.type != ComponentType.FLUID_DRAIN && adj.type != ComponentType.BEDROCK) {
                                        if (!isDrainOnly || (adj.type == ComponentType.WATER || adj.type == ComponentType.LAVA || adj.type == ComponentType.OIL || adj.type == ComponentType.ACID || adj.type == ComponentType.SLIME || adj.type == ComponentType.GASOLINE)) {
                                            grid[x + dx][y + dy] = GridComponent(type = ComponentType.EMPTY)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (x == endX) break
                x += stepX
            }
        }
        
        // Element generation and interactions
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                
                // Generators
                if (comp.type == ComponentType.INFINITE_WATER) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY && Math.random() < 0.2) {
                            grid[nx][ny] = GridComponent(ComponentType.WATER)
                        }
                    }
                } else if (comp.type == ComponentType.INFINITE_LAVA) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY && Math.random() < 0.1) {
                            grid[nx][ny] = GridComponent(ComponentType.LAVA)
                        }
                    }
                } else if (comp.type == ComponentType.FLUID_DRAIN) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type.category == ComponentCategory.MATERIALS && grid[nx][ny].type != ComponentType.INFINITE_WATER && grid[nx][ny].type != ComponentType.INFINITE_LAVA && grid[nx][ny].type != ComponentType.VOID_HOLE) {
                            grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                } else if (comp.type == ComponentType.VOID_HOLE) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type != ComponentType.VOID_HOLE) {
                            grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                } else if (comp.type == ComponentType.SPONGE) {
                    for (i in -3..3) {
                        for (j in -3..3) {
                            if (x+i in 0 until width && y+j in 0 until height && grid[x+i][y+j].type == ComponentType.WATER) {
                                grid[x+i][y+j] = GridComponent(ComponentType.EMPTY)
                            }
                        }
                    }
                }
                
                // Active hydraulic pipeline pumps
                if (comp.type == ComponentType.PIPE && !moved[x][y]) {
                    val inDx = when(comp.direction) { com.example.model.Direction.RIGHT -> -1; com.example.model.Direction.LEFT -> 1; else -> 0 }
                    val inDy = when(comp.direction) { com.example.model.Direction.DOWN -> -1; com.example.model.Direction.UP -> 1; else -> 0 }
                    val outDx = when(comp.direction) { com.example.model.Direction.RIGHT -> 1; com.example.model.Direction.LEFT -> -1; else -> 0 }
                    val outDy = when(comp.direction) { com.example.model.Direction.DOWN -> 1; com.example.model.Direction.UP -> -1; else -> 0 }
                    
                    val inX = x + inDx
                    val inY = y + inDy
                    
                    if (inX in 0 until width && inY in 0 until height) {
                        val inType = grid[inX][inY].type
                        val isInFluid = inType == ComponentType.WATER || inType == ComponentType.LAVA || inType == ComponentType.OIL || inType == ComponentType.ACID || inType == ComponentType.GASOLINE || inType == ComponentType.SLIME || inType == ComponentType.LIQUID_NITROGEN
                        
                        if (isInFluid) {
                            // Find the outlet of the continuous pipelines
                            var currOutX = x + outDx
                            var currOutY = y + outDy
                            var foundOutput = false
                            var targetX = -1
                            var targetY = -1
                            
                            while (currOutX in 0 until width && currOutY in 0 until height) {
                                val nextType = grid[currOutX][currOutY].type
                                if (nextType == ComponentType.PIPE) {
                                    val pipeDir = grid[currOutX][currOutY].direction
                                    val nextOutDx = when(pipeDir) { com.example.model.Direction.RIGHT -> 1; com.example.model.Direction.LEFT -> -1; else -> 0 }
                                    val nextOutDy = when(pipeDir) { com.example.model.Direction.DOWN -> 1; com.example.model.Direction.UP -> -1; else -> 0 }
                                    currOutX += nextOutDx
                                    currOutY += nextOutDy
                                } else if (nextType == ComponentType.EMPTY) {
                                    foundOutput = true
                                    targetX = currOutX
                                    targetY = currOutY
                                    break
                                } else {
                                    break
                                }
                            }
                            
                            if (foundOutput && targetX in 0 until width && targetY in 0 until height) {
                                grid[targetX][targetY] = grid[inX][inY]
                                grid[inX][inY] = GridComponent(ComponentType.EMPTY)
                                moved[targetX][targetY] = true
                            }
                        }
                    }
                }
                
                // Uranium heat, decay and chain reaction physics
                if (comp.type == ComponentType.URANIUM && !moved[x][y]) {
                    var currentTemp = comp.temperature
                    if (currentTemp <= 0f) {
                        currentTemp = 20f
                    }

                    var uNeighbors = 0
                    val waterNeighbors = mutableListOf<Pair<Int, Int>>()
                    val iceNeighbors = mutableListOf<Pair<Int, Int>>()
                    val woodNeighbors = mutableListOf<Pair<Int, Int>>()
                    val otherSolids = mutableListOf<Pair<Int, Int>>()
                    var hasCooling = false

                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nt = grid[nx][ny].type
                            if (nt == ComponentType.URANIUM) {
                                uNeighbors++
                            } else if (nt == ComponentType.WATER || nt == ComponentType.INFINITE_WATER) {
                                waterNeighbors.add(Pair(nx, ny))
                                hasCooling = true
                            } else if (nt == ComponentType.LIQUID_NITROGEN || nt == ComponentType.COOLER) {
                                hasCooling = true
                            } else if (nt == ComponentType.ICE) {
                                iceNeighbors.add(Pair(nx, ny))
                            } else if (nt == ComponentType.WOOD || nt == ComponentType.COAL) {
                                woodNeighbors.add(Pair(nx, ny))
                            } else if (nt == ComponentType.STONE || nt == ComponentType.CLAY || nt == ComponentType.BRICK || nt == ComponentType.SAND || nt == ComponentType.STEEL || nt == ComponentType.COPPER || nt == ComponentType.GOLD || nt == ComponentType.ALUMINUM || nt == ComponentType.PLASTIC || nt == ComponentType.OBSIDIAN) {
                                otherSolids.add(Pair(nx, ny))
                            }
                        }
                    }

                    // Heat generation
                    if (uNeighbors >= 2) {
                        currentTemp += 150f // 5x faster critical thermal buildup
                    } else if (uNeighbors == 1) {
                        currentTemp += 25f  // Moderate warming
                    } else {
                        currentTemp -= 0.5f  // Slower ambient dispersal
                    }

                    // Cooling logic
                    if (hasCooling) {
                        val coolRate = if (currentTemp > 1000f) 5f else 50f // requires massive water supply when molten
                        currentTemp -= coolRate * (waterNeighbors.size + 1)
                        
                        // Boil water adjacent to the Uranium
                        for (w in waterNeighbors) {
                            if (Math.random() < 0.3) {
                                grid[w.first][w.second] = GridComponent(ComponentType.STEAM)
                            }
                        }
                    }

                    for (ic in iceNeighbors) {
                        currentTemp -= 30f
                        if (Math.random() < 0.5) {
                            grid[ic.first][ic.second] = GridComponent(ComponentType.WATER)
                        }
                    }

                    currentTemp = currentTemp.coerceIn(20f, 2500f)

                    // Melt things on its path if melted (temperature > 1000f)
                    val isMelting = currentTemp > 1000f
                    if (isMelting) {
                        // Melt solids beside and below
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val nt = grid[nx][ny].type
                                if (nt != ComponentType.BEDROCK && (nt == ComponentType.STONE || nt == ComponentType.CLAY || nt == ComponentType.BRICK || nt == ComponentType.SAND || nt == ComponentType.STEEL || nt == ComponentType.COPPER || nt == ComponentType.GOLD || nt == ComponentType.ALUMINUM || nt == ComponentType.PLASTIC || nt == ComponentType.OBSIDIAN || nt == ComponentType.GLASS || nt == ComponentType.WOOD)) {
                                    if (Math.random() < 0.4) {
                                        grid[nx][ny] = GridComponent(ComponentType.LAVA) // melt to lava!
                                    }
                                }
                            }
                        }
                        
                        // Gravitational dropping of molten Uranium through empty spaces or lava
                        val belowY = y + 1
                        if (belowY in 0 until height) {
                            val belowT = grid[x][belowY].type
                            if (belowT == ComponentType.EMPTY || belowT == ComponentType.LAVA || belowT == ComponentType.WATER || belowT == ComponentType.STEAM) {
                                // Swap Uranium down, representing it burning through floors!
                                grid[x][belowY] = comp.copy(temperature = currentTemp)
                                grid[x][y] = GridComponent(belowT)
                                moved[x][belowY] = true
                                continue
                            }
                        }
                    }

                    // Fire risk / chain reaction effects (50x heightened fire and heat emission sparks)
                    if (currentTemp > 200f) {
                        for (wd in woodNeighbors) {
                            if (Math.random() < 0.95) {
                                grid[wd.first][wd.second] = GridComponent(ComponentType.FIRE)
                            }
                        }
                        // Spark fire in empty / combustible spaces in a 2-block radius!
                        for (dx in -2..2) {
                            for (dy in -2..2) {
                                if (dx == 0 && dy == 0) continue
                                val nx = x + dx
                                val ny = y + dy
                                if (nx in 0 until width && ny in 0 until height) {
                                    val targetType = grid[nx][ny].type
                                    if (targetType == ComponentType.EMPTY && Math.random() < 0.3) {
                                        grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                    } else if (targetType == ComponentType.COAL || targetType == ComponentType.GASOLINE || targetType == ComponentType.WOOD) {
                                        if (Math.random() < 0.7) {
                                            grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save the updated temperature to the grid cell
                    grid[x][y] = comp.copy(temperature = currentTemp)
                }
                
                // Reactions
                if (comp.type == ComponentType.LAVA) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nType = grid[nx][ny].type
                            if (nType == ComponentType.WATER) {
                                grid[x][y] = GridComponent(ComponentType.STONE)
                                grid[nx][ny] = GridComponent(ComponentType.STEAM)
                                break
                            } else if (nType == ComponentType.WOOD || nType == ComponentType.COAL || nType == ComponentType.OIL || nType == ComponentType.GASOLINE) {
                                grid[nx][ny] = GridComponent(ComponentType.FIRE)
                            } else if (nType == ComponentType.ICE) {
                                grid[nx][ny] = GridComponent(ComponentType.WATER)
                                grid[x][y] = GridComponent(ComponentType.STONE)
                            }
                        }
                    }
                } else if (comp.type == ComponentType.FIRE) {
                    if (Math.random() < 0.1) { 
                        grid[x][y] = GridComponent(ComponentType.EMPTY)
                    } else {
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val nType = grid[nx][ny].type
                                if (nType == ComponentType.WOOD || nType == ComponentType.COAL || nType == ComponentType.OIL || nType == ComponentType.GASOLINE) {
                                    if (Math.random() < 0.2) grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                } else if (nType == ComponentType.WATER) {
                                    grid[x][y] = GridComponent(ComponentType.STEAM)
                                } else if (nType == ComponentType.ICE) {
                                    grid[nx][ny] = GridComponent(ComponentType.WATER)
                                    grid[x][y] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                        }
                    }
                } else if (comp.type == ComponentType.ACID) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val target = grid[nx][ny]
                            if (target.type != ComponentType.EMPTY && target.type != ComponentType.ACID && target.type != ComponentType.GLASS && target.type != ComponentType.DIAMOND && target.type != ComponentType.VOID_HOLE) {
                                if (Math.random() < 0.1) {
                                    grid[nx][ny] = GridComponent(ComponentType.ACID)
                                    grid[x][y] = GridComponent(ComponentType.EMPTY)
                                    break
                                }
                            }
                        }
                    }
                } else if (comp.type == ComponentType.LIQUID_NITROGEN) {
                    if (Math.random() < 0.2) grid[x][y] = GridComponent(ComponentType.EMPTY)
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nType = grid[nx][ny].type
                            if (nType == ComponentType.WATER) grid[nx][ny] = GridComponent(ComponentType.ICE)
                            if (nType == ComponentType.FIRE) grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                } else if (comp.type == ComponentType.STEAM) {
                    if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.WATER)
                    else if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.EMPTY)
                }
                
                // Nuclear Reactor logic
                if (comp.type == ComponentType.NUCLEAR_REACTOR) {
                    var hasWater = false
                    var hasUranium = false
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            if (grid[nx][ny].type == ComponentType.WATER) hasWater = true
                            if (grid[nx][ny].type == ComponentType.URANIUM) hasUranium = true
                        }
                    }
                    if (hasWater && hasUranium) {
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                  if (grid[nx][ny].type == ComponentType.WATER && Math.random() < 0.3) {
                                      grid[nx][ny] = GridComponent(ComponentType.STEAM)
                                  }
                                  if (grid[nx][ny].type == ComponentType.URANIUM && Math.random() < 0.01) {
                                      grid[nx][ny] = GridComponent(ComponentType.EMPTY) 
                                  }
                            }
                        }
                    }
                }
                
                // Component Interactions
                if (comp.isPowered && comp.type == ComponentType.HEATER) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nt = grid[nx][ny].type
                            if (nt == ComponentType.WATER) grid[nx][ny] = GridComponent(ComponentType.STEAM)
                            if (nt == ComponentType.ICE) grid[nx][ny] = GridComponent(ComponentType.WATER)
                        }
                    }
                }
                if (comp.isPowered && comp.type == ComponentType.COOLER) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nt = grid[nx][ny].type
                            if (nt == ComponentType.WATER) grid[nx][ny] = GridComponent(ComponentType.ICE)
                            if (nt == ComponentType.STEAM) grid[nx][ny] = GridComponent(ComponentType.WATER)
                        }
                    }
                }
                
                // Conveyor Belt
                if (comp.isPowered && comp.type == ComponentType.CONVEYOR_BELT) {
                    val moveX = when(comp.direction) { com.example.model.Direction.RIGHT -> 1; com.example.model.Direction.LEFT -> -1; else -> 0 }
                    val moveY = when(comp.direction) { com.example.model.Direction.DOWN -> 1; com.example.model.Direction.UP -> -1; else -> 0 }
                    val topY = y - 1
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
                    val pulls = listOf(com.example.model.Direction.UP, com.example.model.Direction.DOWN, com.example.model.Direction.LEFT, com.example.model.Direction.RIGHT)
                    for (dir in pulls) {
                        for (dist in 1..radius) {
                            val dx = when(dir) { com.example.model.Direction.RIGHT -> dist; com.example.model.Direction.LEFT -> -dist; else -> 0 }
                            val dy = when(dir) { com.example.model.Direction.DOWN -> dist; com.example.model.Direction.UP -> -dist; else -> 0 }
                            val sx = x + dx
                            val sy = y + dy
                            if (sx in 0 until width && sy in 0 until height) {
                                val t = grid[sx][sy]
                                if (t.type.category == ComponentCategory.MATERIALS && !moved[sx][sy]) {
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
                    val moveX = when(comp.direction) { com.example.model.Direction.RIGHT -> 1; com.example.model.Direction.LEFT -> -1; else -> 0 }
                    val moveY = when(comp.direction) { com.example.model.Direction.DOWN -> 1; com.example.model.Direction.UP -> -1; else -> 0 }
                    var currentPushX = x + moveX
                    var currentPushY = y + moveY
                    var length = 0
                    while(currentPushX in 0 until width && currentPushY in 0 until height && grid[currentPushX][currentPushY].type.category == ComponentCategory.MATERIALS && length < 3) {
                        length++
                        currentPushX += moveX
                        currentPushY += moveY
                    }
                    if (currentPushX in 0 until width && currentPushY in 0 until height && grid[currentPushX][currentPushY].type == ComponentType.EMPTY && length > 0) {
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
                        grid[x + moveX][y + moveY] = GridComponent()
                    }
                }
            }
        }
        FluidEngine.calculatePressureAndLeaks(grid, width, height, moved)
    }
}
