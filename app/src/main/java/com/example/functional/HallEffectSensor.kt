package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * HallEffectSensor measures high current flow in adjacent conductors or raw magnet presence.
 */
object HallEffectSensor {
    fun isHallEffectSensor(type: ComponentType): Boolean = type == ComponentType.HALL_EFFECT_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var highCurrentDetected = false
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val adj = grid[nx][ny]
                if (adj.current > 1500f || adj.type == ComponentType.MAGNET || adj.type == ComponentType.TESLA_COIL) {
                    highCurrentDetected = true
                    break
                }
            }
        }
        return comp.copy(isPowered = highCurrentDetected, voltage = if (highCurrentDetected) 5f else 0f, logicState = highCurrentDetected)
    }
}
