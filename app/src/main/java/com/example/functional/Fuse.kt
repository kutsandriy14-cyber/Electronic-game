package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates overcurrent protection, thermal melting points, and sacrificial interruption ratings for Fuses.
 */
object Fuse {
    fun isFuse(type: ComponentType): Boolean = type == ComponentType.FUSE

    fun getMaxCurrentRatingAmps(): Float = 5.0f // Blows above 5 Amps
}
