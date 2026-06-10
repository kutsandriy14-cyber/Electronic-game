package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates solar intensity converting properties of Solar Panels.
 */
object SolarPanel {
    fun isSolarPanel(type: ComponentType): Boolean = type == ComponentType.SOLAR_PANEL

    fun getOutputPowerRating(): Float = 2.5f // Watts per light unit
}
