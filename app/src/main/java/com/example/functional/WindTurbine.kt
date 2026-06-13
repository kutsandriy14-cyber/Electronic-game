package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates rotor surface area and wind yield conversion mechanics of Wind Turbines.
 */
object WindTurbine {
    fun isWindTurbine(type: ComponentType): Boolean = type == ComponentType.WIND_TURBINE

    fun getCutInWindSpeed(): Float = 3.0f // m/s
    fun getCutOutWindSpeed(): Float = 25.0f // m/s

    /**
     * Calculates local wind speed in m/s based on:
     * - Global atmospheric cycle (time-based)
     * - Height (wind speed is higher at the top of the screen)
     * - Solid obstacles (blocks shield wind)
     * - Active Fans blowing wind directly towards this coordinate
     */
    fun getWindSpeedAt(
        grid: Array<Array<com.example.model.GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Float {
        // Base environmental wind with minor natural variation over time
        val timeSecs = System.currentTimeMillis() / 1000f
        val globalWind = 5.0f + 4.0f * kotlin.math.sin(timeSecs / 15f) // Fluctuates between 1.0 and 9.0 m/s
        
        // Height factor: higher altitude (lower y index) has stronger wind
        // At y=0 (top of map): max height multiplier = 1.5
        // At y=height-1 (bottom of map): min height multiplier = 0.2
        val heightRatio = (height - 1 - y).toFloat() / maxOf(1, height - 1)
        val heightMultiplier = 0.2f + 1.3f * heightRatio
        var localWind = globalWind * heightMultiplier

        // Block shielding: check if there are nearby solid blocks horizontally or above that block breeze
        var obstacleShield = 0f
        val checkRange = 2
        for (dx in -checkRange..checkRange) {
            for (dy in -checkRange..0) { // check same level or above block
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val targetComp = grid[nx][ny]
                    if (targetComp.type != ComponentType.EMPTY && 
                        targetComp.type != ComponentType.WIND_TURBINE && 
                        !Fluid.isFluid(targetComp.type)) {
                        obstacleShield += 0.15f
                    }
                }
            }
        }
        localWind = (localWind - (localWind * obstacleShield.coerceAtMost(0.8f))).coerceAtLeast(0.5f)

        // Fan wind boost: check if there are power-connected active fans blowing directly at our turbine
        // Search in horizontal and vertical directions up to 6 blocks away
        val dirX = intArrayOf(-1, 1, 0, 0)
        val dirY = intArrayOf(0, 0, -1, 1)
        for (d in 0..3) {
            val stepX = dirX[d]
            val stepY = dirY[d]
            for (dist in 1..6) {
                val fx = x + stepX * dist
                val fy = y + stepY * dist
                if (fx in 0 until width && fy in 0 until height) {
                    val compAt = grid[fx][fy]
                    if (compAt.type == ComponentType.FAN && compAt.isPowered) {
                        // The fan must be facing our turbine's direction
                        // If we are to the right of the fan, the fan must face RIGHT to blow on us
                        val fanDirDx = Physical.getDx(compAt.direction)
                        val fanDirDy = Physical.getDy(compAt.direction)
                        // This vector from fan to turbine is (-stepX, -stepY)
                        if (fanDirDx == -stepX && fanDirDy == -stepY) {
                            // Boost based on proximity
                            val boost = 15.0f / dist
                            localWind += boost
                        }
                    }
                } else {
                    break
                }
            }
        }

        return localWind
    }

    /**
     * Map wind speed to voltage factor (0.0 to 1.0)
     */
    fun getEfficiency(windSpeed: Float): Float {
        if (windSpeed < getCutInWindSpeed()) return 0f
        if (windSpeed > getCutOutWindSpeed()) return 0f // High-wind safety cutoff
        
        // Linear scaling from cut-in to rated speed (e.g. 12 m/s rated)
        val ratedSpeed = 12.0f
        if (windSpeed >= ratedSpeed) return 1f
        return (windSpeed - getCutInWindSpeed()) / (ratedSpeed - getCutInWindSpeed())
    }
}
