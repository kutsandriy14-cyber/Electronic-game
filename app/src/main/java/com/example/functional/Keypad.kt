package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the 4x4 matrix keyboard scanner with bounce filtering and debounce intervals.
 */
object Keypad {
    fun isKeypad(type: ComponentType): Boolean = type == ComponentType.KEYPAD_4X4

    fun getDebounceMs(): Int = 20
}
