package com.example.functional

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.functional.Physical

/**
 * UltrasonicSensor fires a sonar pulse in its pointing direction to measure obstacle distance.
 */
object UltrasonicSensor {
    fun isUltrasonicSensor(type: ComponentType): Boolean = type == ComponentType.ULTRASONIC_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val dx = Physical.getDx(comp.direction)
        val dy = Physical.getDy(comp.direction)
        var dist = 0
        var foundObstacle = false
        for (i in 1..8) {
            val tx = x + dx * i
            val ty = y + dy * i
            if (tx in 0 until width && ty in 0 until height) {
                if (grid[tx][ty].type != ComponentType.EMPTY) {
                    dist = i
                    foundObstacle = true
                    break
                }
            } else {
                dist = i
                foundObstacle = true
                break
            }
        }
        val outputVolt = if (foundObstacle) {
            ((8 - dist) * (5f / 8f)).coerceIn(0f, 5f)
        } else 0f
        return comp.copy(isPowered = foundObstacle, voltage = outputVolt, logicState = foundObstacle && dist <= 3)
    }
}
