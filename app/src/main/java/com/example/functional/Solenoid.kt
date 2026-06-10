package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates high speed linear electromagnetic solenoids, holding forces, and duty cycle indices.
 */
object Solenoid {
    fun isSolenoid(type: ComponentType): Boolean = type == ComponentType.SOLENOID

    fun getHoldingForceNewtons(): Float = 25f
}
