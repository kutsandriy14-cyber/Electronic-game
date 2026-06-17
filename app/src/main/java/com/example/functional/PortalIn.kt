package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * An energetic wormhole entry point that scans adjacent squares for mobile particles/solids
 * and instantly teleports them to any active exit Portal (PortalOut).
 */
object PortalIn {
    fun isPortalIn(type: ComponentType): Boolean = type == ComponentType.PORTAL_IN

    fun handleTeleportation(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int) {
        val dxs = intArrayOf(0, 0, -1, 1)
        val dys = intArrayOf(-1, 1, 0, 0)
        for (i in 0..3) {
            val nx = x + dxs[i]
            val ny = y + dys[i]
            if (nx in 0 until width && ny in 0 until height) {
                val candidate = grid[nx][ny]
                val canTeleport = candidate.type != ComponentType.EMPTY && 
                                  candidate.type != ComponentType.PORTAL_IN && 
                                  candidate.type != ComponentType.PORTAL_OUT && 
                                  candidate.type != ComponentType.BEDROCK
                if (canTeleport) {
                    var targetX = -1
                    var targetY = -1
                    outer@ for (tx in 0 until width) {
                        for (ty in 0 until height) {
                            if (grid[tx][ty].type == ComponentType.PORTAL_OUT) {
                                for (j in 0..3) {
                                    val ox = tx + dxs[j]
                                    val oy = ty + dys[j]
                                    if (ox in 0 until width && oy in 0 until height && grid[ox][oy].type == ComponentType.EMPTY) {
                                        targetX = ox
                                        targetY = oy
                                        break@outer
                                    }
                                }
                            }
                        }
                    }
                    if (targetX != -1 && targetY != -1) {
                        grid[targetX][targetY] = candidate.copy()
                        grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                    }
                }
            }
        }
    }
}
