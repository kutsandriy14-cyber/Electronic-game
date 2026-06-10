package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates pressure holding and leak thresholds of dynamic hydraulic Pipes.
 */
object Pipe {
    fun isPipe(type: ComponentType): Boolean = type == ComponentType.PIPE

    fun getMaxPressure(): Float = 200f // psi / bar indicator
}
