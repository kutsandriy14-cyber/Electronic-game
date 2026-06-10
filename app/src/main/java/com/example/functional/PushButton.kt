package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates tactile momentary push buttons, spring return coefficients, and directional vector inputs.
 */
object PushButton {
    fun isPushButton(type: ComponentType): Boolean {
        return type == ComponentType.PUSH_BUTTON ||
               type == ComponentType.PUSH_BUTTON_UP ||
               type == ComponentType.PUSH_BUTTON_DOWN ||
               type == ComponentType.PUSH_BUTTON_LEFT ||
               type == ComponentType.PUSH_BUTTON_RIGHT
    }

    /**
     * Translates directional button elements to unit vector offsets.
     */
    fun getDirectionDelta(type: ComponentType): Pair<Int, Int> {
        return when (type) {
            ComponentType.PUSH_BUTTON_UP -> Pair(0, -1)
            ComponentType.PUSH_BUTTON_DOWN -> Pair(0, 1)
            ComponentType.PUSH_BUTTON_LEFT -> Pair(-1, 0)
            ComponentType.PUSH_BUTTON_RIGHT -> Pair(1, 0)
            else -> Pair(0, 0)
        }
    }
}
