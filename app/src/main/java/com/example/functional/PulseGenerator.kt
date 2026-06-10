package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates adjustable duty cycle triggers, square wave oscillators, and precise system clocks.
 */
object PulseGenerator {
    fun isPulseGenerator(type: ComponentType): Boolean = type == ComponentType.PULSE_GENERATOR

    fun getDefaultPulseIntervalTicks(): Int = 10
}
