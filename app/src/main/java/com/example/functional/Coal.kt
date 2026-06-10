package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Coal {
    fun isCoal(type: ComponentType): Boolean = type == ComponentType.COAL
    
    fun getFlameDuration(): Int = 80 // ticks
}
