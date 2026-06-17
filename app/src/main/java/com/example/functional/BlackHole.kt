package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * A gravitational singularity that pulls and consumes all physical particles
 * and components around it.
 */
object BlackHole {
    fun isBlackHole(type: ComponentType): Boolean = type == ComponentType.BLACK_HOLE

    fun applyGravity(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int) {
        val radius = 2
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val neighbor = grid[nx][ny]
                    if (neighbor.type != ComponentType.EMPTY && 
                        neighbor.type != ComponentType.BEDROCK && 
                        neighbor.type != ComponentType.BLACK_HOLE) {
                        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                            grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                        } else {
                            val stepX = if (dx < 0) 1 else if (dx > 0) -1 else 0
                            val stepY = if (dy < 0) 1 else if (dy > 0) -1 else 0
                            val tx = nx + stepX
                            val ty = ny + stepY
                            if (tx in 0 until width && ty in 0 until height && grid[tx][ty].type == ComponentType.EMPTY) {
                                grid[tx][ty] = neighbor.copy()
                                grid[nx][ny] = GridComponent(ComponentType.EMPTY)
                            }
                        }
                    }
                }
            }
        }
    }
}
