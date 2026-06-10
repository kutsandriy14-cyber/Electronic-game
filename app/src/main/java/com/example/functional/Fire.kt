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
 * Encapsulates the combustion, spread, and extinguishing logic for Fire particles.
 */
object Fire {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is active burning Fire.
     */
    fun isFire(type: ComponentType): Boolean = type == ComponentType.FIRE

    /**
     * Simulates fire spread, consumption of wood/coal, and thermal decay.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
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
}
