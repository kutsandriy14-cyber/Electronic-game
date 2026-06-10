package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates structural fission containment thresholds of Nuclear Reactors.
 */
object NuclearReactor {
    fun isNuclearReactor(type: ComponentType): Boolean = type == ComponentType.NUCLEAR_REACTOR

    fun getMeltdownTemperature(): Float = 2500f // Celsius
}
