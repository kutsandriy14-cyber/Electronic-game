package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates unidirectional current flow, PN junction forward voltage drops, and blocking characteristics of Diodes.
 */
object Diode {
    fun isDiode(type: ComponentType): Boolean = type == ComponentType.DIODE

    fun getForwardVoltageDrop(): Float = 0.7f // Silicon standard drop
}
