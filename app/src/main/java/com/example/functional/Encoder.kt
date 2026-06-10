package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the relative rotary encoders with gray code pulses (A and B channels) and shaft click buttons.
 */
object Encoder {
    fun isEncoder(type: ComponentType): Boolean = type == ComponentType.ENCODER

    fun getPulsesPerRevolution(): Int = 24
}
