package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * StepDownConverter scales input voltages downwards (e.g., from 12V down to 5V or 3.3V) with high thermal efficiency.
 */
object StepDownConverter {
    fun isStepDownConverter(type: ComponentType): Boolean = type == ComponentType.STEP_DOWN_CONVERTER

    fun convert(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        var maxInputVolt = 0f
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val neighbor = grid[nx][ny]
                if (neighbor.type != ComponentType.STEP_DOWN_CONVERTER) {
                    maxInputVolt = maxOf(maxInputVolt, neighbor.voltage)
                }
            }
        }
        val outputVolt = if (maxInputVolt > 0.5f) {
            (maxInputVolt * 0.41f).coerceIn(0f, 5f)
        } else 0f
        return comp.copy(isPowered = outputVolt > 0f, voltage = outputVolt)
    }
}
