package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the configuration and operational switching logic for open/closed switches, SPDT relays, and multi-pole DIP switches.
 */
object Switch {
    fun isSwitch(type: ComponentType): Boolean {
        return type == ComponentType.SWITCH_OPEN ||
               type == ComponentType.SWITCH_CLOSED ||
               type == ComponentType.DIP_SWITCH ||
               type == ComponentType.RELAY
    }

    /**
     * Determines the default conductivity matching the manual contact point's state.
     */
    fun isConducting(type: ComponentType, userState: Boolean): Boolean {
        return when (type) {
            ComponentType.SWITCH_CLOSED -> true
            ComponentType.SWITCH_OPEN -> false
            else -> userState
        }
    }
}
