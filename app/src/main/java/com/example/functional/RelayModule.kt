package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates multi-channel relay modules, safe mechanical isolations, and coil drive thresholds.
 */
object RelayModule {
    fun isRelayModule(type: ComponentType): Boolean = type == ComponentType.RELAY_MODULE_4CH

    fun getTriggerCurrentMA(): Float = 5.0f // Minimal optoisolator current
}
