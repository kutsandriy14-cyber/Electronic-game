package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * UvSensor measures solar radiation levels based on vertical height exposure (closer to air levels is higher UV).
 */
object UvSensor {
    fun isUvSensor(type: ComponentType): Boolean = type == ComponentType.UV_SENSOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        // High UV near top boundary
        val exposure = (1.0f - (y.toFloat() / height.toFloat())).coerceIn(0f, 1f)
        val trigger = exposure > 0.6f
        return comp.copy(isPowered = trigger, voltage = exposure * 5f, logicState = trigger)
    }
}
