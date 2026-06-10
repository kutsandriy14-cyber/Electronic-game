package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

object Rubber {
    fun isRubber(type: ComponentType): Boolean = type == ComponentType.RUBBER
    
    fun isInsulator(): Boolean = true
}
