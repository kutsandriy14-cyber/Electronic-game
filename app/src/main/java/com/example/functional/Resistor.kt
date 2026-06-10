package com.example.functional

import com.example.model.ComponentType

/**
 * Encapsulates behaviors, property limits, and electrical resistance coefficients of the Resistor component.
 */
object Resistor {
    fun isResistor(type: ComponentType): Boolean = type == ComponentType.RESISTOR

    /**
     * Retrieves standard resistance of a resistor.
     */
    fun getDefaultResistanceOhms(): Float = 1000f // 1k Ohm
}
