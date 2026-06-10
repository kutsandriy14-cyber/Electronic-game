package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates thermal baking, firing, and mechanical transformation details of Clay.
 */
object Clay {
    fun isClay(type: ComponentType): Boolean = type == ComponentType.CLAY

    fun getBakingTemperature(): Float = 600f // Hardens into brick at this point
}
