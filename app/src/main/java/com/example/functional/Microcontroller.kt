package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates microcontrollers (MCU), RAM blocks, ROM architectures, clock speeds, and logic execution boundaries.
 */
object Microcontroller {
    fun isMicrocontrollerOrMemory(type: ComponentType): Boolean {
        return type == ComponentType.MICROCONTROLLER ||
               type == ComponentType.MEMORY_RAM ||
               type == ComponentType.MEMORY_ROM
    }

    /**
     * Represents typical memory storage capacity in bytes.
     */
    fun getCapacityBytes(type: ComponentType): Int {
        return when (type) {
            ComponentType.MEMORY_ROM -> 32768 // 32kB ROM
            ComponentType.MEMORY_RAM -> 2048  // 2kB RAM
            else -> 1024 // MCU internal EEPROM
        }
    }
}
