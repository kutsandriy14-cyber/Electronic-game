package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.functional.Leds

/**
 * Photoresistor decreases resistance in high-light environments.
 */
object Photoresistor {
    fun isPhotoresistor(type: ComponentType): Boolean = type == ComponentType.PHOTORESISTOR

    fun calculateResistance(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int): Float {
        var illuminated = false
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val adj = grid[nx][ny]
                if (adj.isPowered && Leds.isLuminous(adj.type)) {
                    illuminated = true
                }
            }
        }
        return if (illuminated) 330f else 1000000f
    }
}
