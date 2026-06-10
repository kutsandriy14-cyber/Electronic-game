package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Mechanical specifications and quadrature resolution signals for relative rotary encoders.
 */
object EncoderSpecs {
    /**
     * Translates encoder step directions to integer offsets (+1, -1).
     */
    fun parseQuadratureChange(pinA: Boolean, pinB: Boolean, prevA: Boolean, prevB: Boolean): Int {
        if (!prevA && pinA) { // Rising edge A
            return if (!pinB) 1 else -1
        }
        return 0
    }
}
