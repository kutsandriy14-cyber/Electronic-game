package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Ic7404Not simulates a 7404 Hex Inverter NOT logic gate package on the sandbox grid.
 */
object Ic7404Not {
    fun isIc7404(type: ComponentType): Boolean = type == ComponentType.IC_7404_NOT

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val inputState = if (x > 0) grid[x-1][y].logicState else false
        val out = !inputState
        return comp.copy(isPowered = out, voltage = if (out) 5f else 0f, logicState = out)
    }
}
