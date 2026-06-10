package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the operations, evaporation, and freezing parameters of cryogenic Liquid Nitrogen.
 */
object LiquidNitrogen {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is Liquid Nitrogen.
     */
    fun isLiquidNitrogen(type: ComponentType): Boolean = type == ComponentType.LIQUID_NITROGEN

    /**
     * Simulates cryogenic rapid evaporation characteristics on exposure.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int
    ) {
        if (rand() < 0.1) {
            grid[x][y] = GridComponent(ComponentType.EMPTY)
        }
    }
}
