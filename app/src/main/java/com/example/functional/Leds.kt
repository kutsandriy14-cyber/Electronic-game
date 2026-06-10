package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates luminous emitting surfaces, dynamic light intensities, and spectral characteristics of bulbs, LEDs, and Laser Diodes.
 */
object Leds {
    fun isLuminous(type: ComponentType): Boolean {
        return type == ComponentType.BULB ||
               type == ComponentType.LED ||
               type == ComponentType.RGB_LED ||
               type == ComponentType.LASER_DIODE
    }

    /**
     * Determines the optimal operating current for maximum brightness.
     */
    fun getOptimalCurrentMA(type: ComponentType): Float {
        return when (type) {
            ComponentType.LASER_DIODE -> 80f
            ComponentType.BULB -> 300f
            else -> 20f // Standard modern LED
        }
    }
}
