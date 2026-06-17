package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Accelerometer registers mechanical vibration and movement spikes.
 */
object Accelerometer {
    fun isAccelerometer(type: ComponentType): Boolean = type == ComponentType.ACCELEROMETER

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var movementDetected = false
        val range = 2
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val candidate = grid[nx][ny]
                    if (candidate.type == ComponentType.MOTOR || candidate.type == ComponentType.FAN || candidate.pressure > 1.0f) {
                        movementDetected = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = movementDetected, voltage = if (movementDetected) 5f else 0f, logicState = movementDetected)
    }
}
