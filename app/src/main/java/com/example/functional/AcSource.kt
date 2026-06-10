package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the alternating voltage thresholds and frequency of the AC Source.
 */
object AcSource {
    fun isAcSource(type: ComponentType): Boolean = type == ComponentType.AC_SOURCE

    fun getFrequencyHz(): Float = 50f // 50 Hz standard frequency
}
