package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the infinite resistance, invincibility, and immovable boundaries of Bedrock.
 */
object Bedrock {
    fun isBedrock(type: ComponentType): Boolean = type == ComponentType.BEDROCK

    fun isIndestructible(): Boolean = true
}
