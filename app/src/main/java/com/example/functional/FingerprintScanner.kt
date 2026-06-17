package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * FingerprintScanner acts as a high-security biometrically verified gate/trigger.
 */
object FingerprintScanner {
    fun isFingerprintScanner(type: ComponentType): Boolean = type == ComponentType.FINGERPRINT_SCANNER

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        // Toggle based logic or proximity touch detection
        val topY = y - 1
        var isAuthorized = false
        if (topY in 0 until height) {
            val topType = grid[x][topY].type
            if (topType == ComponentType.SLIME || topType == ComponentType.MAGIC_DUST) {
                isAuthorized = true
            }
        }
        return comp.copy(isPowered = isAuthorized, voltage = if (isAuthorized) 5f else 0f, logicState = isAuthorized)
    }
}
