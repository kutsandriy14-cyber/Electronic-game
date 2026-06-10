package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates voltage regulation, reverse avalanche breakdown levels, and clamping features of Zener Diodes.
 */
object ZenerDiode {
    fun isZenerDiode(type: ComponentType): Boolean = type == ComponentType.ZENER_DIODE

    fun getBreakdownVoltage(): Float = 5.1f // Standard zener breakdown voltage
}
