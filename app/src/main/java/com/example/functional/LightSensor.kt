package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.functional.Leds

/**
 * LightSensor detects active luminous blocks nearby and outputs logic triggers.
 */
object LightSensor {
    fun isLightSensor(type: ComponentType): Boolean = type == ComponentType.LIGHT_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var lightFound = false
        val dx = intArrayOf(-2, -1, 1, 2, 0, 0, 0, 0)
        val dy = intArrayOf(0, 0, 0, 0, -2, -1, 1, 2)
        for (i in 0 until dx.size) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val adj = grid[nx][ny]
                if (adj.isPowered && Leds.isLuminous(adj.type)) {
                    lightFound = true
                    break
                }
            }
        }
        return comp.copy(isPowered = lightFound, voltage = if (lightFound) 5f else 0f, logicState = lightFound)
    }
}
