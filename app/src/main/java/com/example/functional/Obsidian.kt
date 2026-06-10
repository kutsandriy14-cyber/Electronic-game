package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the rapid cooling results of Lava meeting Water, forming ultra-durable Obsidian.
 */
object Obsidian {
    fun isObsidian(type: ComponentType): Boolean = type == ComponentType.OBSIDIAN

    fun getMeltingPoint(): Float = 1300f

    fun getHardness(): Float = 5.5f // Mohs scale
}
