package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Copper {
    fun isCopper(type: ComponentType): Boolean = type == ComponentType.COPPER
    
    fun getConductivity(): Float = 59.6f // MS/m
}
