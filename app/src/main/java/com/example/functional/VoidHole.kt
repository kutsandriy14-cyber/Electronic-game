package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the complete annihilation fields of Void Holes and selective Fluid Drains.
 */
object VoidHole {
    /**
     * Checks if component type is a Void Hole or Fluid Drain.
     */
    fun isWormholeOrDrain(type: ComponentType): Boolean = type == ComponentType.VOID_HOLE || type == ComponentType.FLUID_DRAIN

    /**
     * Simulates vacuum absorption of particles and fluid draining logic.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        val isDrainOnly = comp.type == ComponentType.FLUID_DRAIN
        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        for (dx in -1..1) {
            for (dy in -1..1) {
                if (x + dx in 0 until width && y + dy in 0 until height) {
                    val adj = grid[x + dx][y + dy].type
                    if (adj != ComponentType.EMPTY &&
                        adj != ComponentType.VOID_HOLE &&
                        adj != ComponentType.FLUID_DRAIN &&
                        adj != ComponentType.BEDROCK
                    ) {
                        if (!isDrainOnly || (Fluid.isFluid(adj))) {
                            grid[x + dx][y + dy] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                }
            }
        }
        for (i in 0..3) {
            val nx = x + dxs4[i]
            val ny = y + dys4[i]
            if (nx in 0 until width && ny in 0 until height &&
                grid[nx][ny].type != ComponentType.VOID_HOLE &&
                grid[nx][ny].type != ComponentType.BEDROCK &&
                !isDrainOnly
            ) {
                grid[nx][ny] = GridComponent(ComponentType.EMPTY)
            }
        }
    }
}
