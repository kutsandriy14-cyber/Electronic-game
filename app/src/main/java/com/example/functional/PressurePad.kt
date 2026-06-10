package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates mechanical contact thresholds, elastic forces, reed proximity sensors, and electric pressure response structures.
 */
object PressurePad {
    fun isMechanicalSensor(type: ComponentType): Boolean {
        return type == ComponentType.PRESSURE_PAD ||
               type == ComponentType.REED_SWITCH ||
               type == ComponentType.LIMIT_SWITCH ||
               type == ComponentType.MAGNETIC_CONTACT
    }

    /**
     * Determines the mechanical force trigger threshold in Newtons.
     */
    fun getTriggerForceNewtons(type: ComponentType): Float {
        return when (type) {
            ComponentType.PRESSURE_PAD -> 15.0f
            else -> 1.5f // Tactile micro-switch limits
        }
    }
}
