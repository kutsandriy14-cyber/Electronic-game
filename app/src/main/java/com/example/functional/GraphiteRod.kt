package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * GraphiteRod dampens active fission reactions.
 * When adjacent to uranium, it dampens runaway heat cycles to prevent critical thermal failures.
 */
object GraphiteRod {
    fun isGraphiteRod(type: ComponentType): Boolean = type == ComponentType.GRAPHITE_ROD

    fun dampenNuclearHeat(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)
        var adjacentUraniums = 0
        for (i in 0..3) {
            val nx = x + dxs[i]
            val ny = y + dys[i]
            if (nx in 0 until width && ny in 0 until height) {
                if (grid[nx][ny].type == ComponentType.URANIUM) {
                    adjacentUraniums++
                }
            }
        }

        // Dissipates extreme surrounding heat efficiently!
        val targetTemp = if (comp.temperature > 100f) comp.temperature * 0.85f else comp.temperature
        return comp.copy(temperature = targetTemp, logicState = adjacentUraniums > 0)
    }
}
