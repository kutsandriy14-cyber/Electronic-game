package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates total internal reflection indices, optical transmission bands, and light speed velocities of Fiber Optic waveguides.
 */
object FiberOptic {
    fun isFiberOptic(type: ComponentType): Boolean = type == ComponentType.FIBER_OPTIC

    fun getRefractiveIndex(): Float = 1.45f // Silica glass core index
}
