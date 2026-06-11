package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates thermal physical state reactions for Water and Eternal/Infinite Water.
 */
object Water {
    /**
     * Checks if component type is regular Water or Infinite Water.
     */
    fun isWater(type: ComponentType): Boolean = type == ComponentType.WATER || type == ComponentType.INFINITE_WATER

    /**
     * Simulates interactions with adjacent block types (Lava, Sponge).
     */
    fun interact(
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
}
