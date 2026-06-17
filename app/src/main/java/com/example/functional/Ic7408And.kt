package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Ic7408And simulates a 7408 Quad 2-input AND gate package on the sandbox grid.
 */
object Ic7408And {
    fun isIc7408(type: ComponentType): Boolean = type == ComponentType.IC_7408_AND

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val in1 = if (x > 0) grid[x-1][y].logicState else false
        val in2 = if (x < width-1) grid[x+1][y].logicState else false
        val out = in1 && in2
        return comp.copy(isPowered = out, voltage = if (out) 5f else 0f, logicState = out)
    }
}
