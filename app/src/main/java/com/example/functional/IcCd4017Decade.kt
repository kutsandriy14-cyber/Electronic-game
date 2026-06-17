package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * IcCd4017Decade simulates the CD4017 Johnson Decade Counter which cycles through 10 decimal outputs on clock triggers.
 */
object IcCd4017Decade {
    fun isCd4017(type: ComponentType): Boolean = type == ComponentType.IC_CD4017_DECADE

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val clockWasHigh = if (comp.charge.isNaN()) false else comp.charge > 0.5f
        val clockIsHigh = if (x > 0) grid[x-1][y].logicState else false
        var count = if (comp.extraData.isEmpty()) 0 else {
            try { comp.extraData.toInt() } catch(e: Exception) { 0 }
        }

        if (!clockWasHigh && clockIsHigh) {
            count = (count + 1) % 10
        }

        val outState = count > 0 // simulates counter operational state
        return comp.copy(isPowered = outState, voltage = if (outState) 5f else 0f, logicState = outState, charge = if (clockIsHigh) 1f else 0f, extraData = count.toString())
    }
}
