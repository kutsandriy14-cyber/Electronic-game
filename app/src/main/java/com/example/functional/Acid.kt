package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates corrosion, material digestion rates, and physical dissolving logic for Acid.
 */
object Acid {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is Acid.
     */
    fun isAcid(type: ComponentType): Boolean = type == ComponentType.ACID

    /**
     * Simulates downward and sideways industrial/corrosive dissolution of vulnerable solid walls.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        if (y + 1 in 0 until height) {
            val adj = grid[x][y + 1].type
            if (adj != ComponentType.EMPTY &&
                adj != ComponentType.GLASS &&
                adj != ComponentType.ACID &&
                adj != ComponentType.OBSIDIAN &&
                adj != ComponentType.BEDROCK
            ) {
                if (rand() < 0.2) {
                    grid[x][y + 1] = GridComponent(ComponentType.EMPTY)
                    if (rand() < 0.5) {
                        grid[x][y] = GridComponent(ComponentType.EMPTY)
                    }
                }
            }
        }
    }
}
