package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates operations for the fan propulsion and wind actuator.
 */
object Fan {
    /**
     * Checks if component type is a Fan.
     */
    fun isFan(type: ComponentType): Boolean = type == ComponentType.FAN

    /**
     * Simulates fan blowing gaseous and volatile particles forward.
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

        val outDx = Physical.getDx(comp.direction)
        val outDy = Physical.getDy(comp.direction)

        for (len in 1..4) {
            val tx = x + (outDx * len)
            val ty = y + (outDy * len)
            if (tx in 0 until width && ty in 0 until height) {
                val tType = grid[tx][ty].type
                if (tType == ComponentType.STEAM || tType == ComponentType.FIRE || tType == ComponentType.LIQUID_NITROGEN) {
                    val destX = tx + outDx
                    val destY = ty + outDy
                    if (destX in 0 until width && destY in 0 until height && grid[destX][destY].type == ComponentType.EMPTY) {
                        grid[destX][destY] = grid[tx][ty]
                        grid[tx][ty] = GridComponent(ComponentType.EMPTY)
                    }
                }
            }
        }
    }
}
