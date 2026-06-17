package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Thermistor modifies resistance dramatically depending on temperature.
 */
object Thermistor {
    fun isThermistor(type: ComponentType): Boolean = type == ComponentType.THERMISTOR

    fun calculateResistance(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int): Float {
        var maxTemp = grid[x][y].temperature
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]; val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                maxTemp = maxOf(maxTemp, grid[nx][ny].temperature)
            }
        }
        val tC = maxTemp.coerceAtLeast(-273f)
        val tK = tC + 273.15f
        val t0K = 25f + 273.15f
        val b = 3950f
        val baseRes = 10000f
        return (baseRes * Math.exp((b * (1f / tK - 1f / t0K)).toDouble())).toFloat().coerceIn(10f, 1000000f)
    }
}
