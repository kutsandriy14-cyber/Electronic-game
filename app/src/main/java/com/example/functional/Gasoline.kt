package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates highly volatile, low flash point combustion behaviors of Gasoline.
 */
object Gasoline {
    fun isGasoline(type: ComponentType): Boolean = type == ComponentType.GASOLINE

    fun getFlashPoint(): Float = -43f // Flash point is below room temp!

    fun getAutoIgnitionTemp(): Float = 280f // Celsius

    fun getExplosionRadius(): Int = 2
}
