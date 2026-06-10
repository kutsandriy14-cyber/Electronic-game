package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates rotor surface area and wind yield conversion mechanics of Wind Turbines.
 */
object WindTurbine {
    fun isWindTurbine(type: ComponentType): Boolean = type == ComponentType.WIND_TURBINE

    fun getCutInWindSpeed(): Float = 3.0f // m/s
}
