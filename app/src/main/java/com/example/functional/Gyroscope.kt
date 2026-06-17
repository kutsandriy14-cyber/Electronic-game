package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Gyroscope measures angular tilt and rotation based on turbine operations.
 */
object Gyroscope {
    fun isGyroscope(type: ComponentType): Boolean = type == ComponentType.GYROSCOPE

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var gyroTilt = false
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val t = grid[nx][ny].type
                if (t == ComponentType.WIND_TURBINE || t == ComponentType.STIRLING_ENGINE || t == ComponentType.STEPPER_MOTOR) {
                    gyroTilt = true
                    break
                }
            }
        }
        return comp.copy(isPowered = gyroTilt, voltage = if (gyroTilt) 5f else 0.2f, logicState = gyroTilt)
    }
}
