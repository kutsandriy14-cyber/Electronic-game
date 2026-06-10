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
 * Encapsulates behaviors, condensation thresholds, and buoyancy vector logic for steam vapor.
 */
object Steam {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is Steam.
     */
    fun isSteam(type: ComponentType): Boolean = type == ComponentType.STEAM

    /**
     * Simulates steam vapor condensation checks over time.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int
    ) {
        if (rand() < PhysicsConstants.STEAM_CONDENSATION_PROBABILITY) {
            grid[x][y] = GridComponent(ComponentType.EMPTY)
        }
    }
}
