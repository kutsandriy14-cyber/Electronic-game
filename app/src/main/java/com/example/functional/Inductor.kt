package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates magnetic flux loading, inductance values, and saturation limits for Inductors.
 */
object Inductor {
    fun isInductor(type: ComponentType): Boolean = type == ComponentType.INDUCTOR

    fun getDefaultInductanceHenry(): Float = 0.01f // 10 mH
}
