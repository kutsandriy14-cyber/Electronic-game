package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.PhysicsConstants
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates behaviors, radioactivity, and heat generation for the Uranium material.
 */
object Uranium {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is Uranium.
     */
    fun isUranium(type: ComponentType): Boolean = type == ComponentType.URANIUM

    /**
     * Performs step-by-step Uranium temperature chain reaction and meltdown simulation.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        var currentTemp = comp.temperature
        var uNeighbors = 0
        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        // Count adjacent uranium blocks to build cluster density (critical mass booster)
        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.URANIUM) {
                uNeighbors++
            }
        }

        // Clustering increases thermal production significantly
        if (uNeighbors >= 1) currentTemp += 40f
        if (uNeighbors >= 2) currentTemp += 180f
        if (uNeighbors >= 3) currentTemp += 450f

        val isCritical = currentTemp > 1200f

        // Critical status: runaway self-heating up to extreme limits
        if (isCritical) {
            currentTemp += 150f + (uNeighbors * 80f)
        }

        // Coolant checks with protective steam screen logic (Leidenfrost shield)
        var cooled = false
        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
            if (nx in 0 until width && ny in 0 until height) {
                val adjType = grid[nx][ny].type
                if (adjType == ComponentType.WATER || adjType == ComponentType.INFINITE_WATER) {
                    if (isCritical) {
                        // Leidenfrost protective steam shield:
                        // Water turns to steam instantly BUT does not cool the runaway block!
                        grid[nx][ny] = GridComponent(ComponentType.STEAM)
                    } else {
                        // Standard water cooling before runaway
                        currentTemp -= 220f
                        cooled = true
                        grid[nx][ny] = GridComponent(ComponentType.STEAM)
                    }
                } else if (adjType == ComponentType.LIQUID_NITROGEN) {
                    // Cryogenic nitrogen is strong enough to pierce the shield and absorb immense heat
                    currentTemp -= 600f
                    cooled = true
                    grid[nx][ny] = GridComponent(ComponentType.EMPTY) // Nitrogen boils away completely
                }
            }
        }

        // Cap temperature (runaway melts up to 100,000 degrees!)
        val maxUraniumTemp = if (isCritical) 100000f else PhysicsConstants.URANIUM_MAX_TEMP
        currentTemp = currentTemp.coerceIn(20f, maxUraniumTemp)
        grid[x][y] = comp.copy(temperature = currentTemp)

        // 32x32 liquid evaporation in vicinity under extreme heat
        if (currentTemp > 1000f) {
            val evapRadius = 16
            for (dx in -evapRadius..evapRadius) {
                for (dy in -evapRadius..evapRadius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        val victimType = grid[nx][ny].type
                        if (victimType == ComponentType.WATER || victimType == ComponentType.INFINITE_WATER) {
                            grid[nx][ny] = GridComponent(ComponentType.STEAM, temperature = currentTemp)
                        } else if (victimType == ComponentType.ACID || victimType == ComponentType.SLIME || 
                                   victimType == ComponentType.GASOLINE || victimType == ComponentType.LIQUID_NITROGEN) {
                            grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                }
            }
        }

        // 10x10 Corium/Elephant's Foot meltdown propagation
        // Solid materials (metals, concrete, stones) mix with fuel and melt into super-heated flowing corium
        if (isCritical) {
            val meltRadius = 5
            for (dx in -meltRadius..meltRadius) {
                for (dy in -meltRadius..meltRadius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        val neighbor = grid[nx][ny]
                        val nType = neighbor.type
                        if (nType != ComponentType.EMPTY && nType != ComponentType.URANIUM && nType != ComponentType.BEDROCK) {
                            if (rand() < 0.20) {
                                // Melt nearby structure into active flowing molten Uranium/Corium slag
                                grid[nx][ny] = GridComponent(
                                    type = ComponentType.URANIUM,
                                    temperature = currentTemp
                                )
                                moved[nx][ny] = true
                            }
                        }
                    }
                }
            }
        }

        // Classic fire breeding around hot non-cooled elements
        if (!cooled && currentTemp > 200f) {
            val heatRatio = ((currentTemp - 200f) / 2300f).coerceIn(0f, 1f)
            val fireRadius = (1 + (heatRatio * 2).toInt()).coerceIn(1, 3)
            val fireChance = heatRatio * 0.15f
            for (dx in -fireRadius..fireRadius) {
                for (dy in -fireRadius..fireRadius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.EMPTY) {
                        if (rand() < fireChance) {
                            grid[nx][ny] = GridComponent(ComponentType.FIRE)
                        }
                    }
                }
            }
        }

        // Sinking/Melt-through mechanism
        if (currentTemp > 1000f) {
            val belowY = y + 1
            if (belowY in 0 until height) {
                val belowT = grid[x][belowY].type
                if (belowT == ComponentType.STONE || belowT == ComponentType.GLASS || belowT == ComponentType.OBSIDIAN || belowT == ComponentType.BRICK) {
                    if (rand() < 0.25) {
                        grid[x][belowY] = comp.copy(temperature = currentTemp)
                        grid[x][y] = GridComponent(belowT)
                        moved[x][belowY] = true
                        moved[x][y] = true
                    }
                }
            }
        }
    }
}
