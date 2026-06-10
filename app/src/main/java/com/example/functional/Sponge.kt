package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Sponge {
    fun isSponge(type: ComponentType): Boolean = type == ComponentType.SPONGE
    
    fun getAbsorbingRadius(): Int = 2
}
