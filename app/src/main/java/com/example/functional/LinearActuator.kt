package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates motorized high-torque linear actuators, stroke limits, and feedback potentiometers.
 */
object LinearActuator {
    fun isLinearActuator(type: ComponentType): Boolean = type == ComponentType.LINEAR_ACTUATOR

    fun getStrokeLimitMm(): Float = 150f
}
