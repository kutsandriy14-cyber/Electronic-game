package com.example.functional

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Definition and configuration for the Piston component.
 */
object Piston {
    /**
     * Checks if component type is a Piston.
     */
    fun isPiston(type: ComponentType): Boolean = type == ComponentType.PISTON

    /**
     * Maximum push limit or steps for the piston action.
     */
    const val MAX_PUSH_LENGTH = 3
}
