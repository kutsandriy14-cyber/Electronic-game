package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Barometer measures fluid and gaseous pressure or surrounding barometric altitude forces.
 */
object Barometer {
    fun isBarometer(type: ComponentType): Boolean = type == ComponentType.BAROMETER

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var highestLocalPressure = comp.pressure
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                highestLocalPressure = maxOf(highestLocalPressure, grid[nx][ny].pressure)
            }
        }
        val triggered = highestLocalPressure > 0.8f
        return comp.copy(isPowered = triggered, pressure = highestLocalPressure, voltage = (highestLocalPressure * 5f).coerceIn(0f, 5f), logicState = triggered)
    }
}
