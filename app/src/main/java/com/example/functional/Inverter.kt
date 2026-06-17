package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Inverter translates a flat DC input into an oscillating, alternating current (AC) signal.
 */
object Inverter {
    fun isInverter(type: ComponentType): Boolean = type == ComponentType.INVERTER

    fun convert(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        var maxInputVolt = 0f
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val neighbor = grid[nx][ny]
                if (neighbor.type != ComponentType.INVERTER) {
                    maxInputVolt = maxOf(maxInputVolt, neighbor.voltage)
                }
            }
        }

        // Oscillate output voltage to mimic AC inversion cycles
        val timeBase = System.currentTimeMillis() % 1000
        val sinFactor = kotlin.math.sin(timeBase / 1000f * 2.0 * java.lang.Math.PI).toFloat()
        val outputVolt = if (maxInputVolt > 0.5f) {
            maxInputVolt * sinFactor
        } else 0f

        return comp.copy(isPowered = Math.abs(outputVolt) > 0.5f, voltage = outputVolt, logicState = outputVolt > 0f)
    }
}
