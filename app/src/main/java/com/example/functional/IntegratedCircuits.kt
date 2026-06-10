package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates multi-pin integrated circuits including the 555 Timer, Operational Amplifiers (OP-AMPs), ADC/DAC converters, and H-bridge motor drivers.
 */
object IntegratedCircuits {
    fun isIntegratedCircuit(type: ComponentType): Boolean {
        return type == ComponentType.TIMER_555 ||
               type == ComponentType.OP_AMP ||
               type == ComponentType.ADC ||
               type == ComponentType.DAC ||
               type == ComponentType.COMPARATOR ||
               type == ComponentType.VOLTAGE_REGULATOR ||
               type == ComponentType.AMPLIFIER ||
               type == ComponentType.BUFFER ||
               type == ComponentType.IC_7400_NAND ||
               type == ComponentType.IC_7402_NOR ||
               type == ComponentType.IC_7404_NOT ||
               type == ComponentType.IC_7408_AND ||
               type == ComponentType.IC_7432_OR ||
               type == ComponentType.IC_7486_XOR ||
               type == ComponentType.IC_7447_DECODER ||
               type == ComponentType.IC_CD4017_DECADE ||
               type == ComponentType.IC_LM358_OPAMP ||
               type == ComponentType.IC_LM324_OPAMP ||
               type == ComponentType.IC_LM317_REG ||
               type == ComponentType.IC_L298N_MOTOR ||
               type == ComponentType.IC_ULN2003
    }

    /**
     * Resolves theoretical pin count of an IC package.
     */
    fun getPinCount(type: ComponentType): Int {
        return when (type) {
            ComponentType.TIMER_555 -> 8
            ComponentType.IC_L298N_MOTOR -> 15
            else -> 14 // Standard DIP-14 logic package
        }
    }
}
