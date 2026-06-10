package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates magnetic winding ratios, induction, impedance matching, and voltage/current scaling factors of Transformers.
 */
object Transformer {
    fun isTransformer(type: ComponentType): Boolean = type == ComponentType.TRANSFORMER

    fun getTurnsRatio(): Float = 0.1f // Default to 10:1 step-down ratio
}
