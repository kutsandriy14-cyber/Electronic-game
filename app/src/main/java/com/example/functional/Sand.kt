package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Sand {
    fun isSand(type: ComponentType): Boolean = type == ComponentType.SAND
    
    fun getDensity(): Float = 1.6f // g/cm^3
}
