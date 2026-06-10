package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates behaviors, thermodynamics, and solidifying limits of molten metal/rock Lava.
 */
object Lava {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    fun isLava(type: ComponentType): Boolean = type == ComponentType.LAVA || type == ComponentType.INFINITE_LAVA

    fun getMeltingPoint(): Float = 1200f // Celsius

    /**
     * Cools down and reacts to surrounding materials.
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
                val targetT = grid[nx][ny].type
                if (targetT == ComponentType.WATER || targetT == ComponentType.INFINITE_WATER) {
                    grid[x][y] = GridComponent(ComponentType.STONE, temperature = 400f)
                    grid[nx][ny] = GridComponent(ComponentType.STEAM, temperature = 200f)
                } else if (targetT == ComponentType.ICE) {
                    grid[nx][ny] = GridComponent(ComponentType.WATER, temperature = 80f)
                }
            }
        }
    }
}
