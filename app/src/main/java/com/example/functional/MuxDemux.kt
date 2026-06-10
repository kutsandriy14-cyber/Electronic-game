package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates selector lines, multiplexing pins, and switching velocities of multiplexers and demultiplexers.
 */
object MuxDemux {
    fun isMuxDemux(type: ComponentType): Boolean {
        return type == ComponentType.MULTIPLEXER || type == ComponentType.DEMULTIPLEXER
    }

    fun getSelectorPinCount(): Int = 3 // Standard 8:1 addressable bits
}
