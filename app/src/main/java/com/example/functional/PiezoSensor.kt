package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * PiezoSensor translates physical compression force from pistons/explosions/pressures
 * into high voltage triggers.
 */
object PiezoSensor {
    fun isPiezoSensor(type: ComponentType): Boolean = type == ComponentType.PIEZO_SENSOR

    fun processPressure(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)
        var highPressureAdjacent = false
        for (i in 0..3) {
            val nx = x + dxs[i]
            val ny = y + dys[i]
            if (nx in 0 until width && ny in 0 until height) {
                val neighbor = grid[nx][ny]
                if (neighbor.type == ComponentType.PISTON || neighbor.pressure > 1.5f || neighbor.temperature > 400f) {
                    highPressureAdjacent = true
                    break
                }
            }
        }

        return if (highPressureAdjacent) {
            comp.copy(isPowered = true, voltage = 12f, logicState = true)
        } else {
            // Decay charge quickly
            val nextVolt = Math.max(0f, comp.voltage - 3f)
            comp.copy(isPowered = nextVolt > 0f, voltage = nextVolt, logicState = nextVolt > 0f)
        }
    }
}
