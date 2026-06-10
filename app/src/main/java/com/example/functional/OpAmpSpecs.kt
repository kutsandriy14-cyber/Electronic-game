package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Technical constants, open-loop gain coefficients, and voltage supply clipping for Operational Amplifiers (Op-Amps).
 */
object OpAmpSpecs {
    fun getOpenLoopGain(): Float = 200000f // 106 dB open-loop gain

    fun getSlewRateVperUs(): Float = 0.5f // Standard LM358 slew rate limit
}
