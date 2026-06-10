package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates transistor gate threshold voltages, gain amplification indexes (hFE), and RDS(on) resistances of BJTs and MOSFETs.
 */
object Transistor {
    fun isTransistor(type: ComponentType): Boolean {
        return type == ComponentType.TRANSISTOR || type == ComponentType.MOSFET
    }

    fun getGateThresholdVoltage(): Float = 2.0f // Volts
}
