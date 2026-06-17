package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * ProximitySensor checks if non-empty blocks occupy adjacent spots, outputting logic signals.
 */
object ProximitySensor {
    fun isProximitySensor(type: ComponentType): Boolean = type == ComponentType.PROXIMITY_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var obstacleNearby = false
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                if (grid[nx][ny].type != ComponentType.EMPTY && grid[nx][ny].type != ComponentType.LIMIT_SWITCH) {
                    obstacleNearby = true
                    break
                }
            }
        }
        return comp.copy(isPowered = obstacleNearby, voltage = if (obstacleNearby) 5f else 0f, logicState = obstacleNearby)
    }
}
