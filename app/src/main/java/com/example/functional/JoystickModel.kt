package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Modeling specifications of coordinate systems, resistance ranges, and dead-zones of Joystick controls.
 */
object JoystickModel {
    /**
     * Resolves x, y position with optional deadzone trimming.
     */
    fun applyDeadzone(raw: Float, deadzone: Float = 0.05f): Float {
        val shifted = raw - 2.5f // Shift center to 0
        return if (Math.abs(shifted) < deadzone) {
            2.5f
        } else {
            raw
        }
    }
}
