package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * CameraModule scans the horizontal field of view in front of it for custom active patterns.
 */
object CameraModule {
    fun isCameraModule(type: ComponentType): Boolean = type == ComponentType.CAMERA_MODULE

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        var opticalActivity = false
        val range = 6
        val scanDir = when (comp.direction) {
            com.example.model.Direction.RIGHT -> 1
            com.example.model.Direction.LEFT -> -1
            else -> 0
        }
        if (scanDir != 0) {
            for (i in 1..range) {
                val tx = x + scanDir * i
                if (tx in 0 until width) {
                    val target = grid[tx][y]
                    if (target.type != ComponentType.EMPTY && target.isPowered) {
                        opticalActivity = true
                        break
                    }
                }
            }
        }
        return comp.copy(isPowered = opticalActivity, voltage = if (opticalActivity) 5f else 0.5f, logicState = opticalActivity)
    }
}
