package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.GridComponent

object PhysicsEngine {

    private var movedBuffer: Array<BooleanArray>? = null

    private fun getMovedBuffer(width: Int, height: Int): Array<BooleanArray> {
        val b = movedBuffer
        if (b != null && b.size == width && b[0].size == height) {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    b[x][y] = false
                }
            }
            return b
        }
        val newB = Array(width) { BooleanArray(height) }
        movedBuffer = newB
        return newB
    }

    fun simulateMaterials(grid: Array<Array<GridComponent>>, width: Int, height: Int, voltage: Float) {
        val moved = getMovedBuffer(width, height)
        
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
                if (comp.type.category == ComponentCategory.MATERIALS && comp.type != ComponentType.GLASS && comp.type != ComponentType.STONE && comp.type != ComponentType.WOOD && comp.type != ComponentType.RUBBER && comp.type != ComponentType.DIAMOND && comp.type != ComponentType.COAL && comp.type != ComponentType.SPONGE && comp.type != ComponentType.URANIUM && comp.type != ComponentType.INFINITE_WATER && comp.type != ComponentType.INFINITE_LAVA && comp.type != ComponentType.VOID_HOLE && comp.type != ComponentType.FLUID_DRAIN && comp.type != ComponentType.STEEL && comp.type != ComponentType.COPPER && comp.type != ComponentType.GOLD && comp.type != ComponentType.ALUMINUM && comp.type != ComponentType.PLASTIC && comp.type != ComponentType.CLAY && comp.type != ComponentType.BRICK && comp.type != ComponentType.OBSIDIAN && comp.type != ComponentType.BEDROCK) {
                    
                    var newX = x
                    var newY = y
                    var didMove = false
                    
                    val goesUp = comp.type == ComponentType.STEAM || comp.type == ComponentType.FIRE || comp.type == ComponentType.LIQUID_NITROGEN
                    val dirY = if (goesUp) -1 else 1
                    
                    if (y + dirY in 0 until height && grid[x][y + dirY].type == ComponentType.EMPTY) {
                        newY = y + dirY
                        didMove = true
                    } else if (y + dirY in 0 until height) {
                        val isFluid = comp.type == ComponentType.WATER || comp.type == ComponentType.LAVA || comp.type == ComponentType.OIL || comp.type == ComponentType.ACID || comp.type == ComponentType.SLIME || comp.type == ComponentType.GASOLINE || comp.type == ComponentType.LIQUID_NITROGEN
                        val isPowder = comp.type == ComponentType.SAND || comp.type == ComponentType.DIRT || comp.type == ComponentType.MAGIC_DUST || comp.type == ComponentType.ICE
                        
                        val blockBelow = grid[x][y + dirY].type
                        val belowIsFluid = blockBelow == ComponentType.WATER || blockBelow == ComponentType.LAVA || blockBelow == ComponentType.OIL || blockBelow == ComponentType.ACID || blockBelow == ComponentType.GASOLINE
                        
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
                            if (x + dir1 in 0 until width && grid[x + dir1][y].type == ComponentType.EMPTY) {
                                newX = x + dir1
                                didMove = true
                                sideMoved = true
                            } else if (x + dir2 in 0 until width && grid[x + dir2][y].type == ComponentType.EMPTY) {
                                newX = x + dir2
                                didMove = true
                                sideMoved = true
                            }
                            if (!sideMoved) {
                                if (x + dir1 in 0 until width && y + dirY in 0 until height && grid[x + dir1][y + dirY].type == ComponentType.EMPTY) {
                                    newX = x + dir1
                                    newY = y + dirY
                                    didMove = true
                                } else if (x + dir2 in 0 until width && y + dirY in 0 until height && grid[x + dir2][y + dirY].type == ComponentType.EMPTY) {
                                    newX = x + dir2
                                    newY = y + dirY
                                    didMove = true
                                }
                            }
                        } else {
                            val goLeftFirst = Math.random() < 0.5
                            val dir1 = if (goLeftFirst) -1 else 1
                            val dir2 = if (goLeftFirst) 1 else -1
                            if (x + dir1 in 0 until width && y + dirY in 0 until height && grid[x + dir1][y + dirY].type == ComponentType.EMPTY) {
                                newX = x + dir1
                                newY = y + dirY
                                didMove = true
                            } else if (x + dir2 in 0 until width && y + dirY in 0 until height && grid[x + dir2][y + dirY].type == ComponentType.EMPTY) {
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
                    
                    if (comp.type == ComponentType.WATER || comp.type == ComponentType.INFINITE_WATER) {
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                if (newX + dx in 0 until width && newY + dy in 0 until height) {
                                    if (grid[newX + dx][newY + dy].type == ComponentType.LAVA || grid[newX + dx][newY + dy].type == ComponentType.INFINITE_LAVA) {
                                        grid[newX][newY] = GridComponent(type = ComponentType.STONE)
                                        grid[newX + dx][newY + dy] = GridComponent(type = ComponentType.STONE)
                                    } else if (grid[newX + dx][newY + dy].type == ComponentType.SPONGE) {
                                        grid[newX][newY] = GridComponent(type = ComponentType.EMPTY)
                                    }
                                }
                            }
                        }
                    }
                    if (comp.type == ComponentType.FIRE) {
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                if (newX + dx in 0 until width && newY + dy in 0 until height) {
                                    val adj = grid[newX + dx][newY + dy].type
                                    if (adj == ComponentType.WOOD || adj == ComponentType.COAL) {
                                        if (Math.random() < 0.05) {
                                            grid[newX + dx][newY + dy] = GridComponent(type = ComponentType.FIRE)
                                        }
                                    } else if (adj == ComponentType.WATER) {
                                        grid[newX][newY] = GridComponent(type = ComponentType.STEAM)
                                    } else if (adj == ComponentType.OIL || adj == ComponentType.GASOLINE) {
                                        grid[newX + dx][newY + dy] = GridComponent(type = ComponentType.FIRE)
                                    }
                                }
                            }
                        }
                        if (Math.random() < 0.1) { // Fire dies out
                            grid[newX][newY] = GridComponent(type = ComponentType.EMPTY)
                        }
                    } else if (comp.type == ComponentType.STEAM && Math.random() < 0.05) {
                        grid[newX][newY] = GridComponent(type = ComponentType.EMPTY) 
                    } else if (comp.type == ComponentType.LIQUID_NITROGEN) {
                         if (Math.random() < 0.1) {
                            grid[newX][newY] = GridComponent(type = ComponentType.EMPTY)
                        }
                    } else if (comp.type == ComponentType.ACID) {
                        if (y + 1 in 0 until height) {
                            val adj = grid[newX][newY + 1].type
                            if (adj != ComponentType.EMPTY && adj != ComponentType.GLASS && adj != ComponentType.ACID && adj != ComponentType.OBSIDIAN && adj != ComponentType.BEDROCK) {
                                if (Math.random() < 0.2) {
                                    grid[newX][newY + 1] = GridComponent(type = ComponentType.EMPTY)
                                    if (Math.random() < 0.5) {
                                        grid[newX][newY] = GridComponent(type = ComponentType.EMPTY)
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
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY && Math.random() < 0.2) grid[nx][ny] = GridComponent(ComponentType.WATER)
                    }
                } else if (comp.type == ComponentType.INFINITE_LAVA) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY && Math.random() < 0.1) grid[nx][ny] = GridComponent(ComponentType.LAVA)
                    }
                } else if (comp.type == ComponentType.FLUID_DRAIN) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type.category == ComponentCategory.MATERIALS && grid[nx][ny].type != ComponentType.INFINITE_WATER && grid[nx][ny].type != ComponentType.INFINITE_LAVA && grid[nx][ny].type != ComponentType.VOID_HOLE) grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                    }
                } else if (comp.type == ComponentType.VOID_HOLE) {
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type != ComponentType.VOID_HOLE) grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                    }
                } else if (comp.type == ComponentType.SPONGE) {
                    for (i in -3..3) for (j in -3..3) if (x+i in 0 until width && y+j in 0 until height && grid[x+i][y+j].type == ComponentType.WATER) grid[x+i][y+j] = GridComponent(ComponentType.EMPTY)
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
                        grid[x][y] = GridComponent(ComponentType.EMPTY) // Fire dies eventually
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
                    if (Math.random() < 0.2) grid[x][y] = GridComponent(ComponentType.EMPTY) // Evaporates
                    for (i in 0..3) {
                        val nx = x + dxs4[i]; val ny = y + dys4[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val nType = grid[nx][ny].type
                            if (nType == ComponentType.WATER) grid[nx][ny] = GridComponent(ComponentType.ICE)
                            if (nType == ComponentType.FIRE) grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                } else if (comp.type == ComponentType.STEAM) {
                    if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.WATER) // Condense
                    else if (Math.random() < 0.05) grid[x][y] = GridComponent(ComponentType.EMPTY) // Dissipate
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
                        // Consume water, make steam, consume uranium slowly
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
                
                // Component Interactions (Powered Heaters, Coolers)
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
                        grid[x + moveX][y + moveY] = GridComponent() // Clear first spot
                    }
                }
            }
        }
    }
}
