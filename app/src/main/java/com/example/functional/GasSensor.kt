package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * GasSensor triggers whenever specialized gases or steam particles reside on top of or next to it.
 */
object GasSensor {
    fun isGasSensor(type: ComponentType): Boolean = type == ComponentType.GAS_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var gasDetected = false
        val dx = intArrayOf(0, 0, -1, 1, 0)
        val dy = intArrayOf(-1, 1, 0, 0, 0) // check top, down, sides and center
        for (i in 0 until dx.size) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val t = grid[nx][ny].type
                if (t == ComponentType.HELIUM || t == ComponentType.HYDROGEN || t == ComponentType.METHANE || t == ComponentType.CARBON_DIOXIDE || t == ComponentType.STEAM) {
                    gasDetected = true
                    break
                }
            }
        }
        return comp.copy(isPowered = gasDetected, voltage = if (gasDetected) 5f else 0f, logicState = gasDetected)
    }
}
