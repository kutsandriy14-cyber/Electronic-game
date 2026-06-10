package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates dynamic mechanical sweep properties, logarithmic/linear taper, and variable resistance of Potentiometers.
 */
object Potentiometer {
    fun isPotentiometer(type: ComponentType): Boolean = type == ComponentType.POTENTIOMETER

    fun getResistanceOhms(wiperPercent: Float): Float {
        return wiperPercent.coerceIn(0f, 1f) * 10000f // 10k Ohm max sweep
    }
}
