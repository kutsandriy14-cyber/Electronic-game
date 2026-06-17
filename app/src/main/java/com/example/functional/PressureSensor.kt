package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * PressureSensor outputs logic HIGH if heavy materials or solid blocks settle on top of it.
 */
object PressureSensor {
    fun isPressureSensor(type: ComponentType): Boolean = type == ComponentType.PRESSURE_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val topY = y - 1
        var pressurePressed = false
        if (topY in 0 until height) {
            val topComp = grid[x][topY].type
            if (topComp != ComponentType.EMPTY && topComp != ComponentType.STEAM && topComp != ComponentType.HELIUM && topComp != ComponentType.HYDROGEN) {
                pressurePressed = true
            }
        }
        return comp.copy(isPowered = pressurePressed, voltage = if (pressurePressed) 5f else 0f, logicState = pressurePressed)
    }
}
