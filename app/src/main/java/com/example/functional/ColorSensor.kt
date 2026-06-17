package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * ColorSensor detects distinct colorful elements from adjacent places (e.g. gold, copper, led colors).
 */
object ColorSensor {
    fun isColorSensor(type: ComponentType): Boolean = type == ComponentType.COLOR_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var goldOrCopperNearby = false
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val t = grid[nx][ny].type
                if (t == ComponentType.GOLD || t == ComponentType.COPPER || t == ComponentType.RGB_LED) {
                    goldOrCopperNearby = true
                    break
                }
            }
        }
        return comp.copy(isPowered = goldOrCopperNearby, voltage = if (goldOrCopperNearby) 5f else 0f, logicState = goldOrCopperNearby)
    }
}
