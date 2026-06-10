package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Aluminum {
    fun isAluminum(type: ComponentType): Boolean = type == ComponentType.ALUMINUM
    
    fun getWeightRatio(): Float = 0.35f
}
