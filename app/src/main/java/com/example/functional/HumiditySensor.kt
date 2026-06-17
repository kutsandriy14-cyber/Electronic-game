package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * HumiditySensor outputs analog voltages based on moisture of local surrounding fluids (e.g. steam/water).
 */
object HumiditySensor {
    fun isHumiditySensor(type: ComponentType): Boolean = type == ComponentType.HUMIDITY_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var moistureCount = 0
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val adj = grid[nx][ny].type
                if (adj == ComponentType.WATER || adj == ComponentType.STEAM || adj == ComponentType.SLIME) {
                    moistureCount++
                }
            }
        }
        val outputVoltage = (moistureCount * 1.5f).coerceAtMost(5f)
        return comp.copy(isPowered = outputVoltage > 0f, voltage = outputVoltage, logicState = outputVoltage > 2.5f)
    }
}
