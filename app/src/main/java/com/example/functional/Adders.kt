package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates carry out lines, boolean sums, and cascade delays of Half Adders and Full Adders.
 */
object Adders {
    fun isAdder(type: ComponentType): Boolean {
        return type == ComponentType.HALF_ADDER || type == ComponentType.FULL_ADDER
    }

    fun getCarryOutDelayNs(): Int = 5 // Fast carry propagation times
}
