package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates asynchronous bi-stable flip-flop state machines, Set inputs, Reset inputs, and undefined race conditions.
 */
object SRLatch {
    fun isSRLatch(type: ComponentType): Boolean = type == ComponentType.LATCH_SR

    fun isRaceCondition(set: Boolean, reset: Boolean): Boolean = set && reset
}
