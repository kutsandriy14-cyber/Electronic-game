package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates parallel-in serial-out (PISO), Serial-in parallel-out (SIPO), and shift cascade properties of Shift Registers.
 */
object ShiftRegister {
    fun isShiftRegister(type: ComponentType): Boolean = type == ComponentType.SHIFT_REGISTER

    fun getBitSize(): Int = 8 // Standard serial register size
}
