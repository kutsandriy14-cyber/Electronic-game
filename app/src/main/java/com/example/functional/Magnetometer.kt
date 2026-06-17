package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Magnetometer senses magnetic fields and orientation vectors.
 */
object Magnetometer {
    fun isMagnetometer(type: ComponentType): Boolean = type == ComponentType.MAGNETOMETER

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var hasMagneticFields = false
        val range = 4
        for (dx in -range..range) {
            for (dy in -range..range) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val candidate = grid[nx][ny]
                    if (candidate.type == ComponentType.MAGNET || candidate.type == ComponentType.TESLA_COIL || candidate.type == ComponentType.TRANSFORMER) {
                        hasMagneticFields = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = hasMagneticFields, voltage = if (hasMagneticFields) 4.8f else 0.05f, logicState = hasMagneticFields)
    }
}
