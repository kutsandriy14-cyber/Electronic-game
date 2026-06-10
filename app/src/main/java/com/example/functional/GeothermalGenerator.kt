package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the geothermal efficiency of volcanic/magmatic elements.
 */
object GeothermalGenerator {
    fun isGeothermalGenerator(type: ComponentType): Boolean = type == ComponentType.GEOTHERMAL_GENERATOR

    fun getIdealOperatingTemp(): Float = 350f // Celsius
}
