package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Steel {
    fun isSteel(type: ComponentType): Boolean = type == ComponentType.STEEL
    
    fun getDensity(): Float = 7.85f // g/cm^3
}
