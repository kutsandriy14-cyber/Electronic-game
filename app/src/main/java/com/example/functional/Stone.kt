package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Stone {
    fun isStone(type: ComponentType): Boolean = type == ComponentType.STONE
    
    fun getHardness(): Float = 6.5f // Mohs scale
}
