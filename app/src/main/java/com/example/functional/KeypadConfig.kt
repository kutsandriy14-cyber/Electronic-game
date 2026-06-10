package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Technical specification config parameters for the 4x4 dynamic tactile keypad matrix.
 */
object KeypadConfig {
    /**
     * Resolves layout coordinates of numbers on keypad.
     */
    fun getKeyLabel(row: Int, col: Int): String {
        val keys = arrayOf(
            arrayOf("1", "2", "3", "A"),
            arrayOf("4", "5", "6", "B"),
            arrayOf("7", "8", "9", "C"),
            arrayOf("*", "0", "#", "D")
        )
        if (row in 0..3 && col in 0..3) {
            return keys[row][col]
        }
        return ""
    }
}
