package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Formula definitions, duty cycle multipliers, and resistor/capacitor network timers of 555 Timer IC.
 */
object Timer555Specs {
    /**
     * Calculates the frequency of stable astable operations.
     */
    fun calculateAstableFrequencyHz(r1: Float, r2: Float, c: Float): Float {
        if (r1 + 2 * r2 <= 0f || c <= 0f) return 0f
        return 1.44f / ((r1 + 2 * r2) * c)
    }
}
