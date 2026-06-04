package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent

object PhysicsEngine {
    // FIXED: BUG 3.1 (static material), BUG 3.2 (Math.random), BUG 3.3 (uranium temp & logic),
    //        BUG 3.4 (void hole bedrock), BUG 3.5 (conveyor vertical), BUG 3.6 (piston retract),
    //        BUG 3.7 (magic dust), BUG 3.8 (slime viscosity) BUG 3.9 (pipe comment mismatch)
    // ADDED: FEATURE 6.5 (motor/fan/pump), FEATURE 6.6 (heater temp), FEATURE 6.7 (peltier module)
    // QUALITY 7.1, 7.3, 7.4 (Double buffering for grid mutations)

    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()
    private fun randFloat() = rng.get()!!.nextFloat()

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

    private fun isMobileParticle(type: ComponentType): Boolean {
        return type == ComponentType.WATER || type == ComponentType.LAVA ||
               type == ComponentType.OIL || type == ComponentType.ACID ||
               type == ComponentType.SLIME || type == ComponentType.GASOLINE ||
               type == ComponentType.LIQUID_NITROGEN || type == ComponentType.STEAM ||
               type == ComponentType.FIRE || type == ComponentType.SAND ||
               type == ComponentType.DIRT || type == ComponentType.MAGIC_DUST ||
               type == ComponentType.ICE
    }

    private fun canSqueeze(grid: Array<Array<GridComponent>>, x1: Int, y1: Int, x2: Int, y2: Int, width: Int, height: Int): Boolean {
        if (x1 == x2 || y1 == y2) return true
        val s1 = x2 in 0 until width && !isPassable(grid[x2][y1].type)
        val s2 = y2 in 0 until height && !isPassable(grid[x1][y2].type)
        return !(s1 && s2)
    }

    fun simulateMaterials(grid: Array<Array<GridComponent>>, width: Int, height: Int, voltage: Float) {
        val moved = Array(width) { BooleanArray(height) }
        val prevGrid = Array(width) { x -> grid[x].copyOf() }

        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        // Movement Phase
        for (y in height - 1 downTo 0) {
            val goRight = rand() < 0.5
            val startX = if (goRight) 0 else width - 1
            val endX = if (goRight) width - 1 else 0
            val stepX = if (goRight) 1 else -1
            
            var x = startX
            while (true) {
                if (!moved[x][y]) {
                    val comp = prevGrid[x][y]
                    
                    if (isMobileParticle(comp.type)) {
                        var newX = x
                        var newY = y
                        var didMove = false
                        
                        val goesUp = comp.type == ComponentType.STEAM || comp.type == ComponentType.FIRE || comp.type == ComponentType.LIQUID_NITROGEN
                        val dirY = if (goesUp) -1 else 1
                        val flowChance = when(comp.type) {
                            ComponentType.LAVA -> 0.4
                            ComponentType.SLIME -> 0.25 // Very viscous
                            ComponentType.OIL -> 0.7
                            else -> 0.95
                        }

                        if (y + dirY in 0 until height && grid[x][y + dirY].type == ComponentType.EMPTY) {
                            newY = y + dirY
                            didMove = true
                        } else if (y + dirY in 0 until height) {
                            val isFluid = comp.type == ComponentType.WATER || comp.type == ComponentType.LAVA || comp.type == ComponentType.OIL || comp.type == ComponentType.ACID || comp.type == ComponentType.SLIME || comp.type == ComponentType.GASOLINE || comp.type == ComponentType.LIQUID_NITROGEN
                            val isPowder = comp.type == ComponentType.SAND || comp.type == ComponentType.DIRT || comp.type == ComponentType.MAGIC_DUST || comp.type == ComponentType.ICE
                            
                            val blockBelow = grid[x][y + dirY].type
                            val belowIsFluid = blockBelow == ComponentType.WATER || blockBelow == ComponentType.LAVA || blockBelow == ComponentType.OIL || blockBelow == ComponentType.ACID || blockBelow == ComponentType.GASOLINE || blockBelow == ComponentType.SLIME
                            
                            if (isPowder && belowIsFluid) {
                                grid[x][y] = grid[x][y + dirY]
                                grid[x][y + dirY] = comp
                                moved[x][y + dirY] = true
                                moved[x][y] = true
                            } else if (isFluid && rand() < flowChance) {
                                val goLeftFirst = rand() < 0.5
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
                            } else if (isPowder) {
                                val goLeftFirst = rand() < 0.5
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
                            grid[x][y] = GridComponent(ComponentType.EMPTY)
                            moved[newX][newY] = true
                        }
                    } else if (comp.type == ComponentType.INFINITE_WATER) {
                        if (y + 1 in 0 until height && grid[x][y+1].type == ComponentType.EMPTY) {
                             grid[x][y+1] = GridComponent(ComponentType.WATER)
                             moved[x][y+1] = true
                        }
                    } else if (comp.type == ComponentType.INFINITE_LAVA) {
                        if (y + 1 in 0 until height && grid[x][y+1].type == ComponentType.EMPTY) {
                             grid[x][y+1] = GridComponent(ComponentType.LAVA)
                             moved[x][y+1] = true
                        }
                    }
                }
                
                if (x == endX) break
                x += stepX
            }
        }

        // Feature / Interactions
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.EMPTY) continue

                when(comp.type) {
                    ComponentType.WATER, ComponentType.INFINITE_WATER -> {
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val targetT = grid[nx][ny].type
                                if (targetT == ComponentType.LAVA || targetT == ComponentType.INFINITE_LAVA) {
                                    grid[x][y] = GridComponent(ComponentType.STONE)
                                    grid[nx][ny] = GridComponent(ComponentType.STONE)
                                } else if (targetT == ComponentType.SPONGE) {
                                    grid[x][y] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                        }
                    }
                    ComponentType.FIRE -> {
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                val adj = grid[nx][ny].type
                                if (adj == ComponentType.WOOD || adj == ComponentType.COAL) {
                                    if (rand() < 0.05) {
                                        grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                    }
                                } else if (adj == ComponentType.WATER) {
                                    grid[x][y] = GridComponent(ComponentType.STEAM)
                                } else if (adj == ComponentType.OIL || adj == ComponentType.GASOLINE) {
                                    grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                }
                            }
                        }
                        if (rand() < PhysicsConstants.FIRE_DEATH_PROBABILITY) {
                            grid[x][y] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                    ComponentType.STEAM -> {
                        if (rand() < PhysicsConstants.STEAM_CONDENSATION_PROBABILITY) {
                            grid[x][y] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                    ComponentType.LIQUID_NITROGEN -> {
                        if (rand() < 0.1) {
                            grid[x][y] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                    ComponentType.ACID -> {
                        if (y + 1 in 0 until height) {
                            val adj = grid[x][y + 1].type
                            if (adj != ComponentType.EMPTY && adj != ComponentType.GLASS && adj != ComponentType.ACID && adj != ComponentType.OBSIDIAN && adj != ComponentType.BEDROCK) {
                                if (rand() < 0.2) {
                                    grid[x][y + 1] = GridComponent(ComponentType.EMPTY)
                                    if (rand() < 0.5) {
                                        grid[x][y] = GridComponent(ComponentType.EMPTY)
                                    }
                                }
                            }
                        }
                    }
                    ComponentType.MAGIC_DUST -> {
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height) {
                                if (grid[nx][ny].type == ComponentType.WATER) {
                                    grid[x][y] = GridComponent(ComponentType.SLIME)
                                    grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                                } else if (grid[nx][ny].type == ComponentType.FIRE) {
                                    grid[nx][ny] = GridComponent(ComponentType.STEAM)
                                    if (rand() < 0.3) grid[x][y] = GridComponent(ComponentType.EMPTY)
                                } else if (grid[nx][ny].type == ComponentType.LAVA) {
                                    grid[x][y] = GridComponent(ComponentType.GLASS)
                                }
                            }
                        }
                    }
                    ComponentType.VOID_HOLE, ComponentType.FLUID_DRAIN -> {
                        val isDrainOnly = comp.type == ComponentType.FLUID_DRAIN
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                if (x + dx in 0 until width && y + dy in 0 until height) {
                                    val adj = grid[x + dx][y + dy].type
                                    if (adj != ComponentType.EMPTY && adj != ComponentType.VOID_HOLE && adj != ComponentType.FLUID_DRAIN && adj != ComponentType.BEDROCK) {
                                        if (!isDrainOnly || (adj == ComponentType.WATER || adj == ComponentType.LAVA || adj == ComponentType.OIL || adj == ComponentType.ACID || adj == ComponentType.SLIME || adj == ComponentType.GASOLINE)) {
                                            grid[x + dx][y + dy] = GridComponent(ComponentType.EMPTY)
                                        }
                                    }
                                }
                            }
                        }
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type != ComponentType.VOID_HOLE && grid[nx][ny].type != ComponentType.BEDROCK && !isDrainOnly) {
                                grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                            }
                        }
                    }
                    ComponentType.HEATER -> {
                        if (comp.isPowered) {
                            for (i in 0..3) {
                                val nx = x + dxs4[i]; val ny = y + dys4[i]
                                if (nx in 0 until width && ny in 0 until height) {
                                    val target = grid[nx][ny]
                                    if (target.type == ComponentType.WATER) {
                                        grid[nx][ny] = GridComponent(ComponentType.STEAM)
                                    } else if (target.type == ComponentType.ICE) {
                                        grid[nx][ny] = GridComponent(ComponentType.WATER)
                                    }
                                    grid[nx][ny] = target.copy(temperature = (target.temperature + 5f).coerceAtMost(500f))
                                }
                            }
                        }
                    }
                    ComponentType.COOLER -> {
                        if (comp.isPowered) {
                            for (i in 0..3) {
                                val nx = x + dxs4[i]; val ny = y + dys4[i]
                                if (nx in 0 until width && ny in 0 until height) {
                                    val target = grid[nx][ny]
                                    if (target.type == ComponentType.WATER) {
                                        grid[nx][ny] = GridComponent(ComponentType.ICE)
                                    } else if (target.type == ComponentType.STEAM) {
                                        grid[nx][ny] = GridComponent(ComponentType.WATER)
                                    }
                                    grid[nx][ny] = target.copy(temperature = (target.temperature - 5f).coerceAtLeast(-273f))
                                }
                            }
                        }
                    }
                    ComponentType.PELTIER_MODULE -> {
                        if (comp.isPowered) {
                            val heatX = when(comp.direction) { Direction.RIGHT -> x + 1; Direction.LEFT -> x - 1; else -> x }
                            val heatY = when(comp.direction) { Direction.DOWN -> y + 1; Direction.UP -> y - 1; else -> y }
                            val coolX = when(comp.direction) { Direction.RIGHT -> x - 1; Direction.LEFT -> x + 1; else -> x }
                            val coolY = when(comp.direction) { Direction.DOWN -> y - 1; Direction.UP -> y + 1; else -> y }
                            
                            var tempDiff = 0f
                            if (heatX in 0 until width && heatY in 0 until height) {
                                val target = grid[heatX][heatY]
                                tempDiff += target.temperature
                                grid[heatX][heatY] = target.copy(temperature = (target.temperature + 10f).coerceAtMost(500f))
                            }
                            if (coolX in 0 until width && coolY in 0 until height) {
                                val target = grid[coolX][coolY]
                                tempDiff -= target.temperature
                                grid[coolX][coolY] = target.copy(temperature = (target.temperature - 10f).coerceAtLeast(-273f))
                            }
                            
                            if (tempDiff > 100f) {
                                grid[x][y] = comp.copy(logicState = true)
                            }
                        } else {
                            grid[x][y] = comp.copy(logicState = false)
                        }
                    }
                    ComponentType.CONVEYOR_BELT -> {
                        if (comp.isPowered) {
                            val itemX = when(comp.direction) { Direction.RIGHT -> x - 1; Direction.LEFT -> x + 1; else -> x }
                            val itemY = when(comp.direction) { Direction.UP -> y + 1; Direction.DOWN -> y - 1; else -> y - 1 }
                            
                            val moveX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                            val moveY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                            
                            if (itemX in 0 until width && itemY in 0 until height) {
                                val item = grid[itemX][itemY]
                                val isPowderSlime = isMobileParticle(item.type)
                                if (isPowderSlime && !moved[itemX][itemY]) {
                                    val destX = itemX + moveX; val destY = itemY + moveY
                                    if (destX in 0 until width && destY in 0 until height) {
                                        if (grid[destX][destY].type == ComponentType.EMPTY) {
                                            grid[destX][destY] = item
                                            grid[itemX][itemY] = GridComponent()
                                            moved[itemX][itemY] = true
                                            moved[destX][destY] = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ComponentType.PISTON -> {
                        val moveX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                        val moveY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                        
                        if (comp.isPowered && !comp.logicState) {
                            val pushX = x + moveX; val pushY = y + moveY
                            var length = 0
                            var currentPushX = pushX; var currentPushY = pushY
                            while(currentPushX in 0 until width && currentPushY in 0 until height && 
                                  grid[currentPushX][currentPushY].type.category == ComponentCategory.MATERIALS && length < 3) {
                                length++
                                currentPushX += moveX; currentPushY += moveY
                            }
                            if (length > 0) {
                                val endEmptyX = pushX + length * moveX; val endEmptyY = pushY + length * moveY
                                if (endEmptyX in 0 until width && endEmptyY in 0 until height && grid[endEmptyX][endEmptyY].type == ComponentType.EMPTY) {
                                    for (i in length downTo 1) {
                                        val fromX = pushX + (i-1)*moveX; val fromY = pushY + (i-1)*moveY
                                        val toX = pushX + i*moveX; val toY = pushY + i*moveY
                                        grid[toX][toY] = grid[fromX][fromY]
                                    }
                                    grid[pushX][pushY] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                            grid[x][y] = comp.copy(logicState = true)
                        } else if (!comp.isPowered && comp.logicState) {
                            val retractX = x + moveX; val retractY = y + moveY
                            if (retractX in 0 until width && retractY in 0 until height) {
                                val target = grid[retractX][retractY]
                                val emptyX = x; val emptyY = y // But PISTON is there. Retract actually brings it closer? The real logic pulls (retractX+moveX) to retractX
                                val pullX = retractX + moveX; val pullY = retractY + moveY
                                if (pullX in 0 until width && pullY in 0 until height && grid[retractX][retractY].type == ComponentType.EMPTY) {
                                    grid[retractX][retractY] = grid[pullX][pullY]
                                    grid[pullX][pullY] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                            grid[x][y] = comp.copy(logicState = false)
                        }
                    }
                    ComponentType.URANIUM -> {
                        var currentTemp = comp.temperature
                        var uNeighbors = 0
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.URANIUM) {
                                uNeighbors++
                            }
                        }
                        if (uNeighbors >= 2) currentTemp += 150f
                        
                        var cooled = false
                        for (i in 0..3) {
                            val nx = x + dxs4[i]; val ny = y + dys4[i]
                            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.WATER) {
                                currentTemp -= 200f
                                cooled = true
                                grid[nx][ny] = GridComponent(ComponentType.STEAM)
                            }
                        }
                        
                        currentTemp = currentTemp.coerceIn(20f, PhysicsConstants.URANIUM_MAX_TEMP)
                        grid[x][y] = comp.copy(temperature = currentTemp)

                        if (!cooled && currentTemp > 200f) {
                            val heatRatio = ((currentTemp - 200f) / 2300f).coerceIn(0f, 1f)
                            val fireRadius = (1 + (heatRatio * 2).toInt()).coerceIn(1, 3)
                            val fireChance = heatRatio * 0.15f
                            for (dx in -fireRadius..fireRadius) {
                                for (dy in -fireRadius..fireRadius) {
                                    val nx = x + dx; val ny = y + dy
                                    if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY) {
                                        if (rand() < fireChance) {
                                            grid[nx][ny] = GridComponent(ComponentType.FIRE)
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (currentTemp > 1000f) {
                            val belowY = y + 1
                            if (belowY in 0 until height) {
                                val belowT = grid[x][belowY].type
                                if (belowT == ComponentType.STONE || belowT == ComponentType.GLASS || belowT == ComponentType.OBSIDIAN || belowT == ComponentType.BRICK) {
                                    if (rand() < 0.1) {
                                        grid[x][belowY] = comp.copy(temperature = currentTemp)
                                        grid[x][y] = GridComponent(belowT)
                                        moved[x][belowY] = true
                                        moved[x][y] = true
                                        continue
                                    }
                                }
                            }
                        }
                    }
                    ComponentType.WATER_PUMP -> {
                        if (comp.isPowered) {
                            val inX = when(comp.direction) { Direction.RIGHT -> x - 1; Direction.LEFT -> x + 1; else -> x }
                            val inY = when(comp.direction) { Direction.UP -> y + 1; Direction.DOWN -> y - 1; else -> y - 1 }
                            val outX = when(comp.direction) { Direction.RIGHT -> x + 1; Direction.LEFT -> x - 1; else -> x }
                            val outY = when(comp.direction) { Direction.DOWN -> y + 1; Direction.UP -> y - 1; else -> y }
                            
                            if (inX in 0 until width && inY in 0 until height && outX in 0 until width && outY in 0 until height) {
                                val inType = grid[inX][inY].type
                                if ((inType == ComponentType.WATER || inType == ComponentType.OIL || inType == ComponentType.LAVA || inType == ComponentType.ACID || inType == ComponentType.SLIME || inType == ComponentType.GASOLINE) && grid[outX][outY].type == ComponentType.EMPTY) {
                                    grid[outX][outY] = grid[inX][inY]
                                    grid[inX][inY] = GridComponent(ComponentType.EMPTY)
                                }
                            }
                        }
                    }
                    ComponentType.FAN -> {
                        if (comp.isPowered) {
                            val outX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                            val outY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                            for (len in 1..4) {
                                val tx = x + (outX * len); val ty = y + (outY * len)
                                if (tx in 0 until width && ty in 0 until height) {
                                    val tType = grid[tx][ty].type
                                    if (tType == ComponentType.STEAM || tType == ComponentType.FIRE || tType == ComponentType.LIQUID_NITROGEN) {
                                        val destX = tx + outX; val destY = ty + outY
                                        if (destX in 0 until width && destY in 0 until height && grid[destX][destY].type == ComponentType.EMPTY) {
                                            grid[destX][destY] = grid[tx][ty]
                                            grid[tx][ty] = GridComponent(ComponentType.EMPTY)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ComponentType.MOTOR -> {
                        if (comp.isPowered) {
                            val outX = when(comp.direction) { Direction.RIGHT -> 1; Direction.LEFT -> -1; else -> 0 }
                            val outY = when(comp.direction) { Direction.DOWN -> 1; Direction.UP -> -1; else -> 0 }
                            val inX = when(comp.direction) { Direction.RIGHT -> -1; Direction.LEFT -> 1; else -> 0 }
                            val inY = when(comp.direction) { Direction.DOWN -> -1; Direction.UP -> 1; else -> 0 }
                            for (dx in -1..1) {
                                for (dy in -1..1) {
                                    var tx = x + dx; var ty = y + dy
                                    // Move items orthogonally like a rotating gear
                                    if(tx in 0 until width && ty in 0 until height) {
                                        if (isMobileParticle(grid[tx][ty].type)) {
                                            val destX = tx + outX; val destY = ty + outY
                                            if (destX in 0 until width && destY in 0 until height && grid[destX][destY].type == ComponentType.EMPTY) {
                                                grid[destX][destY] = grid[tx][ty]
                                                grid[tx][ty] = GridComponent(ComponentType.EMPTY)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
