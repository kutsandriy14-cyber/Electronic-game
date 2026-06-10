package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates step-down buck converters, step-up boost converters, and DC-AC Inverters.
 */
object PowerConverters {
    fun isPowerConverter(type: ComponentType): Boolean {
        return type == ComponentType.STEP_DOWN_CONVERTER ||
               type == ComponentType.STEP_UP_CONVERTER ||
               type == ComponentType.INVERTER
    }

    /**
     * Retrieves the standard efficiency rating.
     */
    fun getEfficiency(): Float = 0.88f // 88% average buck/boost conversion efficiency
}
