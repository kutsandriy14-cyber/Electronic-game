package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * PhSensor outputs high voltage when immersed in Acid (pH < 3) or Slime nodes.
 */
object PhSensor {
    fun isPhSensor(type: ComponentType): Boolean = type == ComponentType.PH_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var acidAround = false
        val dx = intArrayOf(-1, 1, 0, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1, 0)
        for (i in 0..4) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val t = grid[nx][ny].type
                if (t == ComponentType.ACID || t == ComponentType.INFINITE_ACID) {
                    acidAround = true
                    break
                }
            }
        }
        val voltage = if (acidAround) 4.5f else 1.2f // baseline pH 7 converts to ~1.2v
        return comp.copy(isPowered = acidAround, voltage = voltage, logicState = acidAround)
    }
}
