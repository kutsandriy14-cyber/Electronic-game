package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates truth tables, propagation delays, and gate thresholds of integrated Logic Gates (AND, OR, NOT, NAND, NOR, XOR, XNOR).
 */
object LogicGate {
    fun isLogicGate(type: ComponentType): Boolean {
        return type == ComponentType.LOGIC_AND ||
               type == ComponentType.LOGIC_OR ||
               type == ComponentType.LOGIC_NOT ||
               type == ComponentType.LOGIC_NAND ||
               type == ComponentType.LOGIC_NOR ||
               type == ComponentType.LOGIC_XOR ||
               type == ComponentType.LOGIC_XNOR
    }

    /**
     * Resolves logic gate outputs based on input state pins.
     */
    fun evaluate(type: ComponentType, in1: Boolean, in2: Boolean): Boolean {
        return when (type) {
            ComponentType.LOGIC_AND -> in1 && in2
            ComponentType.LOGIC_OR -> in1 || in2
            ComponentType.LOGIC_NOT -> !in1
            ComponentType.LOGIC_NAND -> !(in1 && in2)
            ComponentType.LOGIC_NOR -> !(in1 || in2)
            ComponentType.LOGIC_XOR -> in1 xor in2
            ComponentType.LOGIC_XNOR -> !(in1 xor in2)
            else -> false
        }
    }
}
