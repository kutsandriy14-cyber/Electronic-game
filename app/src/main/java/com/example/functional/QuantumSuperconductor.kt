package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * QuantumSuperconductor displays zero electrical resistance and triggers infinite current loops
 * when cooled below -150 Celsius. Acts as regular cable otherwise.
 */
object QuantumSuperconductor {
    fun isQuantumSuperconductor(type: ComponentType): Boolean = type == ComponentType.QUANTUM_SUPERCONDUCTOR

    fun conduct(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int) {
        val isSuperconductive = comp.temperature < -150f
        val currentVoltage = comp.voltage

        if (currentVoltage > 0f) {
            val dxs = intArrayOf(0, 0, -1, 1)
            val dys = intArrayOf(-1, 1, 0, 0)
            for (i in 0..3) {
                val nx = x + dxs[i]
                val ny = y + dys[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val neighbor = grid[nx][ny]
                    if (neighbor.type == ComponentType.COPPER || 
                        neighbor.type == ComponentType.STEEL || 
                        neighbor.type == ComponentType.QUANTUM_SUPERCONDUCTOR) {
                        
                        val drop = if (isSuperconductive) 0f else 0.5f
                        val targetVoltage = Math.max(0f, currentVoltage - drop)
                        if (neighbor.voltage < targetVoltage) {
                            grid[nx][ny] = neighbor.copy(isPowered = true, voltage = targetVoltage)
                        }
                    }
                }
            }
        }
    }
}
