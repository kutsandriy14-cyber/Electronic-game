package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates setup hold timings, propagation delays, and logic inputs of classic JK, T, and D flip flops.
 */
object FlipFlopConfigs {
    fun isFlipFlop(type: ComponentType): Boolean {
        return type == ComponentType.D_FLIP_FLOP ||
               type == ComponentType.T_FLIP_FLOP ||
               type == ComponentType.JK_FLIP_FLOP
    }

    fun getClockPropagationDelayNs(): Int = 8
}
