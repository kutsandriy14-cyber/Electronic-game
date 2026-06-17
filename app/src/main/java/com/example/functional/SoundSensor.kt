package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * SoundSensor detects acoustic waves from adjacent speaker/buzzer nodes and turns on signal pins.
 */
object SoundSensor {
    fun isSoundSensor(type: ComponentType): Boolean = type == ComponentType.SOUND_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var acousticDetected = false
        val range = 4
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val candidate = grid[nx][ny]
                    if (candidate.isPowered && (candidate.type == ComponentType.SPEAKER || candidate.type == ComponentType.BUZZER)) {
                        acousticDetected = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = acousticDetected, voltage = if (acousticDetected) 5f else 0f, logicState = acousticDetected)
    }
}
