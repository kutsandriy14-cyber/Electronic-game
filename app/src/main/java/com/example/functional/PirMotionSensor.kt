package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * PirMotionSensor registers passive infrared motion of moving fluid and dynamic grains.
 */
object PirMotionSensor {
    fun isPirMotionSensor(type: ComponentType): Boolean = type == ComponentType.PIR_MOTION_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int, moved: Array<BooleanArray>): GridComponent {
        var motionPassed = false
        val range = 3
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    if (moved[nx][ny] && grid[nx][ny].type != ComponentType.EMPTY) {
                        motionPassed = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = motionPassed, voltage = if (motionPassed) 5f else 0f, logicState = motionPassed)
    }
}
