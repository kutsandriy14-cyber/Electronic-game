package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates Seebeck effect conversion coefficients for Thermoelectric Generators.
 */
object ThermoelectricGenerator {
    fun isThermoelectricGenerator(type: ComponentType): Boolean = type == ComponentType.THERMOELECTRIC_GENERATOR

    fun getSeebeckCoefficient(): Float = 0.0002f // V/K
}
