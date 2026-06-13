package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

object Gas {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int) {
        val comp = grid[x][y]
        val type = comp.type

        // Dissipation: gases eventually dissipate, especially at the top boundary
        val isAtTop = y <= 2
        val dissipateChance = if (isAtTop) 0.08 else 0.01
        if (rand() < dissipateChance) {
            grid[x][y] = GridComponent(ComponentType.EMPTY)
            return
        }

        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)

        when (type) {
            ComponentType.HYDROGEN, ComponentType.METHANE -> {
                // Check if adjacent to FIRE or highly hot components
                var nearFireOrHeat = false
                for (i in 0..3) {
                    val nx = x + dx[i]
                    val ny = y + dy[i]
                    if (nx in 0 until width && ny in 0 until height) {
                        val adj = grid[nx][ny]
                        if (adj.type == ComponentType.FIRE || adj.temperature > 250f) {
                            nearFireOrHeat = true
                            break
                        }
                    }
                }
                if (nearFireOrHeat) {
                    // Ignite! Turn into fire and propagate
                    grid[x][y] = GridComponent(ComponentType.FIRE, temperature = 800f)
                    for (i in 0..3) {
                        val nx = x + dx[i]
                        val ny = y + dy[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            val adj = grid[nx][ny]
                            if (adj.type == ComponentType.HYDROGEN || adj.type == ComponentType.METHANE) {
                                grid[nx][ny] = GridComponent(ComponentType.FIRE, temperature = 800f)
                            }
                        }
                    }
                }
            }
            ComponentType.CARBON_DIOXIDE -> {
                // Extinguishes fire
                for (i in 0..3) {
                    val nx = x + dx[i]
                    val ny = y + dy[i]
                    if (nx in 0 until width && ny in 0 until height) {
                        val adj = grid[nx][ny]
                        if (adj.type == ComponentType.FIRE) {
                            grid[nx][ny] = GridComponent(ComponentType.EMPTY) // Cool down and snuff out
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
