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

        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
            if (nx in 0 until width && ny in 0 until height && grid[nx][ny].type == ComponentType.URANIUM) {
                uNeighbors++
            }
        }

        if (uNeighbors >= 2) currentTemp += 150f

        var cooled = false
        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
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
                    }
                }
            }
        }
    }
}
