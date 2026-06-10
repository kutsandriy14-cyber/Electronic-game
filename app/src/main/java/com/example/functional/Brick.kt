package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates properties and high temperature refractory resistance of Brick.
 */
object Brick {
    fun isBrick(type: ComponentType): Boolean = type == ComponentType.BRICK

    fun getMeltingPoint(): Float = 1400f // Incredibly high melting point
}
