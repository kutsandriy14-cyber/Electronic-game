package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates conductivity and load currents of common metallic wire conductors.
 */
object WireAny {
    fun isWire(type: ComponentType): Boolean {
        return type == ComponentType.WIRE_ANY ||
               type == ComponentType.HIGH_VOLTAGE_CABLE ||
               type == ComponentType.SUPERCONDUCTOR
    }

    fun getMaxCurrentAmps(type: ComponentType): Float {
        return when (type) {
            ComponentType.SUPERCONDUCTOR -> Float.MAX_VALUE
            ComponentType.HIGH_VOLTAGE_CABLE -> 500f
            else -> 10f // Standard copper wire limit
        }
    }
}
