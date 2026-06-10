package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the operations and transmutational reaction pathways of Magic Dust.
 */
object MagicDust {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is Magic Dust.
     */
    fun isMagicDust(type: ComponentType): Boolean = type == ComponentType.MAGIC_DUST

    /**
     * Performs magical transmutational reactions with water (slime), fire (steam), or lava (glass).
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
}
