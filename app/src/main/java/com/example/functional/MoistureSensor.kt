package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * MoistureSensor registers logic high if placed directly into or adjacent to any fluid (water, acid, lava).
 */
object MoistureSensor {
    fun isMoistureSensor(type: ComponentType): Boolean = type == ComponentType.MOISTURE_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var isWet = false
        val dx = intArrayOf(0, 0, -1, 1, 0)
        val dy = intArrayOf(-1, 1, 0, 0, 0)
        for (i in 0 until dx.size) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val t = grid[nx][ny].type
                if (t == ComponentType.WATER || t == ComponentType.INFINITE_WATER || t == ComponentType.ACID || t == ComponentType.INFINITE_ACID || t == ComponentType.OIL || t == ComponentType.INFINITE_OIL || t == ComponentType.MERCURY) {
                    isWet = true
                    break
                }
            }
        }
        return comp.copy(isPowered = isWet, voltage = if (isWet) 5f else 0f, logicState = isWet)
    }
}
