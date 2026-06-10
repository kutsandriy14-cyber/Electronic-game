package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates gelatinous Slime properties, viscosity index, and high-temperature evaporation.
 */
object Slime {
    fun isSlime(type: ComponentType): Boolean = type == ComponentType.SLIME

    fun getBoilingPoint(): Float = 150f // Drying/hardening point

    fun getViscosityMultiplier(): Float = 4.5f
}
