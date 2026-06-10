package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates segment/pixel matrices, refresh rates, resolution limits, and graphics scaling for display components (OLED, TFT, 7-Segment, LCD).
 */
object Displays {
    fun isDisplay(type: ComponentType): Boolean {
        return type == ComponentType.SEVEN_SEGMENT ||
               type == ComponentType.FOURTEEN_SEGMENT ||
               type == ComponentType.LCD_DISPLAY_16X2 ||
               type == ComponentType.MONITOR_OLED ||
               type == ComponentType.CRT_MONITOR ||
               type == ComponentType.DISPLAY_PIXEL ||
               type == ComponentType.DISPLAY_7SEG_4DIGIT ||
               type == ComponentType.DISPLAY_OLED_128X64 ||
               type == ComponentType.DISPLAY_TFT_24 ||
               type == ComponentType.E_PAPER_DISPLAY
    }

    /**
     * Resolves default display aspect ratio representation.
     */
    fun getAspectRatio(type: ComponentType): Pair<Int, Int> {
        return when (type) {
            ComponentType.MONITOR_OLED -> Pair(16, 9)
            ComponentType.CRT_MONITOR -> Pair(4, 3)
            else -> Pair(1, 1)
        }
    }
}
