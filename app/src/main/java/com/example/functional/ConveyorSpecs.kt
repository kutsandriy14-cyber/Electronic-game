package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Mechanical conveyor speed parameters, static load coefficients, and direction routing rules.
 */
object ConveyorSpecs {
    fun getStandardBeltVelocity(): Float = 2.0f // units per tick
}
