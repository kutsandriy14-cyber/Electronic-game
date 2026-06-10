package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates electrical charge storage capacity, dielectric strength, and leakage rates for Capacitors.
 */
object Capacitor {
    fun isCapacitor(type: ComponentType): Boolean = type == ComponentType.CAPACITOR

    fun getDefaultCapacitanceFarads(): Float = 0.0001f // 100 uF
}
