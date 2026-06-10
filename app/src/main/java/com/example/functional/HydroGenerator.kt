package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the hydraulic efficiency and kinetic flow response of Hydro Generators.
 */
object HydroGenerator {
    fun isHydroGenerator(type: ComponentType): Boolean = type == ComponentType.HYDRO_GENERATOR

    fun getFlowRequirementM3S(): Float = 1.2f
}
