package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * StirlingEngine converts a thermal gradient (high heat underneath, cold above, or viceversa)
 * into a solid electrical voltage and mechanical pressure.
 */
object StirlingEngine {
    fun isStirlingEngine(type: ComponentType): Boolean = type == ComponentType.STIRLING_ENGINE

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var hotTemp = 0f
        var coldTemp = 0f

        if (y + 1 < height) {
            hotTemp = grid[x][y + 1].temperature
        }
        if (y - 1 >= 0) {
            coldTemp = grid[x][y - 1].temperature
        }

        val delta = Math.abs(hotTemp - coldTemp)
        return if (delta > 100f) {
            val inducedVoltage = Math.min(12f, delta / 50f)
            comp.copy(isPowered = true, voltage = inducedVoltage, logicState = true)
        } else {
            comp.copy(isPowered = false, voltage = 0f, logicState = false)
        }
    }
}
