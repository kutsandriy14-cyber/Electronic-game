package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * LightningRod acts as a high-voltage power collector.
 * Exposed to upper environment levels, it periodically redirects extreme voltages to adjacent conductors.
 */
object LightningRod {
    fun isLightningRod(type: ComponentType): Boolean = type == ComponentType.LIGHTNING_ROD

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int) {
        val skyExposed = y == 0 || (1 until y).all { grid[x][it].type == ComponentType.EMPTY }
        if (skyExposed && Math.random() < 0.05) {
            // A strike hits! Extreme charge input
            grid[x][y] = comp.copy(isPowered = true, voltage = 24f)
        } else if (comp.voltage > 0f) {
            // Dissipate charge to neighboring conductors
            val dxs = intArrayOf(0, 0, -1, 1)
            val dys = intArrayOf(-1, 1, 0, 0)
            for (i in 0..3) {
                val nx = x + dxs[i]
                val ny = y + dys[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val neighbor = grid[nx][ny]
                    if (neighbor.type == ComponentType.COPPER || neighbor.type == ComponentType.STEEL) {
                        grid[nx][ny] = neighbor.copy(isPowered = true, voltage = comp.voltage)
                    }
                }
            }
            grid[x][y] = comp.copy(voltage = comp.voltage - 2f)
        }
    }
}
