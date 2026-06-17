package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Microphone senses local vibration waves or dynamic fluid movement, registering high DB levels.
 */
object Microphone {
    fun isMicrophone(type: ComponentType): Boolean = type == ComponentType.MICROPHONE

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var loudVibration = false
        val range = 2
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val t = grid[nx][ny].type
                    if (t == ComponentType.STEAM || t == ComponentType.PLASMA || t == ComponentType.SPEAKER || t == ComponentType.BUZZER) {
                        loudVibration = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = loudVibration, voltage = if (loudVibration) 4.9f else 0.1f, logicState = loudVibration)
    }
}
