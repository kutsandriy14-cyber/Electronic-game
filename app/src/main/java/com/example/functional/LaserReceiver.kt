package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * LaserReceiver scans standard vertical and horizontal axes for active LASER_DIODE components,
 * switching states and emitting high output powers when aligned with light beams.
 */
object LaserReceiver {
    fun isLaserReceiver(type: ComponentType): Boolean = type == ComponentType.LASER_RECEIVER

    fun processLaserDetection(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var beamDetected = false
        
        // Scan horizontally & vertically to verify laser proximity and alignment
        val dirs = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )
        for (dir in dirs) {
            var step = 1
            while (step < 12) {
                val checkX = x + dir.first * step
                val checkY = y + dir.second * step
                if (checkX in 0 until width && checkY in 0 until height) {
                    val candidate = grid[checkX][checkY]
                    if (candidate.type == ComponentType.LASER_DIODE) {
                        beamDetected = true
                        break
                    } else if (candidate.type != ComponentType.EMPTY && candidate.type != ComponentType.GLASS) {
                        // Solid blocks interrupt laser pathing
                        break
                    }
                } else {
                    break
                }
                step++
            }
        }

        return if (beamDetected) {
            comp.copy(isPowered = true, voltage = 5f, logicState = true)
        } else {
            comp.copy(isPowered = false, voltage = 0f, logicState = false)
        }
    }
}
