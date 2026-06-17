package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * TeslaCoil wirelessly transmits power to all conductive objects in a designated 3-cell radius,
 * ionizing adjacent particles and generating visual discharge.
 */
object TeslaCoil {
    fun isTeslaCoil(type: ComponentType): Boolean = type == ComponentType.TESLA_COIL

    fun transmitWirelessPower(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int) {
        val range = 3
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val neighbor = grid[nx][ny]
                    if (neighbor.type != ComponentType.EMPTY && neighbor.type != ComponentType.TESLA_COIL) {
                        grid[nx][ny] = neighbor.copy(isPowered = true, voltage = 12f)
                    }
                }
            }
        }
    }
}
