package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Dirt {
    fun isDirt(type: ComponentType): Boolean = type == ComponentType.DIRT
    
    fun getMoistureRetention(): Float = 0.4f
}
