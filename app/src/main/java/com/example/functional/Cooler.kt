package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates operations and heat sink dynamics for the Cooler actuator.
 */
object Cooler {
    /**
     * Checks if component type is a Cooler.
     */
    fun isCooler(type: ComponentType): Boolean = type == ComponentType.COOLER

    /**
     * Simulates heat removal/freezing from the cooler to adjacent items.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        if (!comp.isPowered) return

        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
            if (nx in 0 until width && ny in 0 until height) {
                val target = grid[nx][ny]
                if (target.type == ComponentType.WATER) {
                    grid[nx][ny] = GridComponent(ComponentType.ICE)
                } else if (target.type == ComponentType.STEAM) {
                    grid[nx][ny] = GridComponent(ComponentType.WATER)
                }
                grid[nx][ny] = grid[nx][ny].copy(temperature = (grid[nx][ny].temperature - 5f).coerceAtLeast(-273f))
            }
        }
    }
}
