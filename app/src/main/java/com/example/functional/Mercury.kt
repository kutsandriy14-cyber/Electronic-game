package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Mercury (Hg) is a heavy liquid metal at room temperature.
 * It behaves like a fluid, has extreme density, and conducts electric current/voltage.
 */
object Mercury {
    fun isMercury(type: ComponentType): Boolean = type == ComponentType.MERCURY

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int, moved: Array<BooleanArray>) {
        if (moved[x][y]) return
        val current = grid[x][y]

        // Falls down rapidly due to high density, otherwise spreads to the sides
        val downY = y + 1
        if (downY < height) {
            val bottom = grid[x][downY]
            if (bottom.type == ComponentType.EMPTY) {
                grid[x][downY] = current.copy()
                grid[x][y] = GridComponent(ComponentType.EMPTY)
                moved[x][downY] = true
                return
            } else if (Fluid.isFluid(bottom.type) && bottom.type != ComponentType.MERCURY) {
                // Displaces lighter fluids upward!
                grid[x][downY] = current.copy()
                grid[x][y] = bottom.copy()
                moved[x][downY] = true
                return
            }
        }

        // Try left or right randomly
        val sideDir = if (Math.random() < 0.5) -1 else 1
        val sideX1 = x + sideDir
        val sideX2 = x - sideDir

        if (sideX1 in 0 until width && grid[sideX1][y].type == ComponentType.EMPTY) {
            grid[sideX1][y] = current.copy()
            grid[x][y] = GridComponent(ComponentType.EMPTY)
            moved[sideX1][y] = true
        } else if (sideX2 in 0 until width && grid[sideX2][y].type == ComponentType.EMPTY) {
            grid[sideX2][y] = current.copy()
            grid[x][y] = GridComponent(ComponentType.EMPTY)
            moved[sideX2][y] = true
        }
    }
}
