package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Plasma is a superheated, highly ionized state of matter.
 * It carries extreme thermal energy, flows upwards (gaseous fluid), and converts
 * nearby solids like stone or metals into lava, and organic matter into fire.
 */
object Plasma {
    fun isPlasma(type: ComponentType): Boolean = 
        type == ComponentType.PLASMA || type == ComponentType.INFINITE_PLASMA

    fun getCoreTemperature(): Float = 3000f // Extremely high temperature

    fun interactWithSurroundings(grid: Array<Array<GridComponent>>, x: Int, y: Int, width: Int, height: Int) {
        val dxs = intArrayOf(-1, 1, 0, 0)
        val dys = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dxs[i]
            val ny = y + dys[i]
            if (nx in 0 until width && ny in 0 until height) {
                val neighbor = grid[nx][ny]
                when (neighbor.type) {
                    ComponentType.WATER, ComponentType.INFINITE_WATER -> {
                        grid[x][y] = GridComponent(ComponentType.STEAM, temperature = 200f)
                    }
                    ComponentType.STONE, ComponentType.DIRT, ComponentType.CLAY, ComponentType.OBSIDIAN -> {
                        if (Math.random() < 0.1) {
                            grid[nx][ny] = GridComponent(ComponentType.LAVA, temperature = 1100f)
                        }
                    }
                    ComponentType.STEEL, ComponentType.COPPER, ComponentType.ALUMINUM, ComponentType.GOLD -> {
                        if (Math.random() < 0.05) {
                            grid[nx][ny] = GridComponent(ComponentType.LAVA, temperature = 1400f)
                        }
                    }
                    ComponentType.WOOD, ComponentType.PLASTIC, ComponentType.GASOLINE, ComponentType.OIL, ComponentType.METHANE -> {
                        grid[nx][ny] = GridComponent(ComponentType.FIRE, temperature = 800f)
                    }
                    else -> {}
                }
            }
        }
    }
}
