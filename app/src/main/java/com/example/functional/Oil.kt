package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates properties, flash point, and viscosity parameters of hydrocarbon Oil.
 */
object Oil {
    fun isOil(type: ComponentType): Boolean = type == ComponentType.OIL

    fun getFlashPoint(): Float = 210f // Celsius

    fun getDensity(): Float = 0.88f // g/cm^3
}
