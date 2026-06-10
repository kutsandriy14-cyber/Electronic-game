package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates hydrodynamic fluid drain ports, flow absorption indices, and discharge velocities.
 */
object FluidDrain {
    fun isFluidDrain(type: ComponentType): Boolean = type == ComponentType.FLUID_DRAIN

    fun getDrainCapacityLiterSec(): Float = 24.5f
}
