package com.example.engine

import com.example.functional.*
import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * Custom core engine for ordinary materials. Handles phase transformations, melting points,
 * spontaneous hot auto-ignitions, baking of clays, and other advanced thermodynamics.
 */
object MaterialEngine {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Simulates thermodynamic phase transfers, combustion thresholds, and properties of ordinary items.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        val type = comp.type
        var temp = comp.temperature

        // 1. Spontaneous Thermal Conduction / Environmental cooling
        // Slowly align with normal background temperature (e.g., 20 °C)
        if (temp > 20f) {
            temp = (temp - 0.5f).coerceAtLeast(20f)
        } else if (temp < 20f) {
            temp = (temp + 0.5f).coerceAtMost(20f)
        }

        // 2. Specific item behaviors
        when {
            // Ice melts to water
            Ice.isIce(type) -> {
                if (temp > Ice.getMeltingPoint()) {
                    grid[x][y] = GridComponent(ComponentType.WATER, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Clay bakes into brick
            Clay.isClay(type) -> {
                if (temp >= Clay.getBakingTemperature()) {
                    grid[x][y] = GridComponent(ComponentType.BRICK, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Wood catches fire
            Wood.isWood(type) -> {
                if (temp >= 250f || isAdjacentToFire(grid, x, y, width, height)) {
                    if (rand() < 0.08) {
                        grid[x][y] = GridComponent(ComponentType.FIRE, temperature = temp + 50f)
                        moved[x][y] = true
                        return
                    }
                }
            }

            // Coal burns
            Coal.isCoal(type) -> {
                if (temp >= 350f || isAdjacentToFire(grid, x, y, width, height)) {
                    if (rand() < 0.05) {
                        grid[x][y] = GridComponent(ComponentType.FIRE, temperature = temp + 100f)
                        moved[x][y] = true
                        return
                    }
                }
            }

            // Stone melts to Lava at melting point
            Stone.isStone(type) -> {
                if (temp >= Lava.getMeltingPoint()) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Glass melts to Lava
            Glass.isGlass(type) -> {
                if (temp >= 1000f) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Obsidian melts to Lava
            Obsidian.isObsidian(type) -> {
                if (temp >= Obsidian.getMeltingPoint()) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Steel melts to Lava
            Steel.isSteel(type) -> {
                if (temp >= 1500f) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Copper melts to Lava
            Copper.isCopper(type) -> {
                if (temp >= 1085f) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Gold melts to Lava
            Gold.isGold(type) -> {
                if (temp >= 1064f) {
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Aluminum melts to Lava
            Aluminum.isAluminum(type) -> {
                if (temp >= Aluminum.getWeightRatio() * 1900f) { // Melts around 660 C
                    grid[x][y] = GridComponent(ComponentType.LAVA, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Plastic melts and burns
            Plastic.isPlastic(type) -> {
                if (temp >= 150f) {
                    if (rand() < 0.15) {
                        grid[x][y] = GridComponent(ComponentType.FIRE, temperature = temp + 20f)
                        moved[x][y] = true
                        return
                    }
                }
            }

            // Gasoline auto-ignites
            Gasoline.isGasoline(type) -> {
                if (temp >= Gasoline.getAutoIgnitionTemp() || isAdjacentToFire(grid, x, y, width, height)) {
                    grid[x][y] = GridComponent(ComponentType.FIRE, temperature = temp + 300f)
                    moved[x][y] = true
                    triggerExplosion(grid, x, y, width, height, Gasoline.getExplosionRadius())
                    return
                }
            }

            // Oil ignites
            Oil.isOil(type) -> {
                if (temp >= Oil.getFlashPoint() || isAdjacentToFire(grid, x, y, width, height)) {
                    if (rand() < 0.2) {
                        grid[x][y] = GridComponent(ComponentType.FIRE, temperature = temp + 150f)
                        moved[x][y] = true
                        return
                    }
                }
            }

            // Slime dries up
            Slime.isSlime(type) -> {
                if (temp >= Slime.getBoilingPoint()) {
                    grid[x][y] = GridComponent(ComponentType.DIRT, temperature = temp)
                    moved[x][y] = true
                    return
                }
            }

            // Lava cools or flows, and sets nearby items on fire
            Lava.isLava(type) -> {
                Lava.simulate(grid, x, y, width, height)
                heatAdjacent(grid, x, y, width, height, 80f)
            }
        }

        // Apply updated temperature to grid cell
        grid[x][y] = comp.copy(temperature = temp)
    }

    private fun isAdjacentToFire(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Boolean {
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]
            val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                if (grid[nx][ny].type == ComponentType.FIRE) return true
            }
        }
        return false
    }

    private fun heatAdjacent(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        intensity: Float
    ) {
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)
        for (i in 0..3) {
            val nx = x + dx[i]
            val ny = y + dy[i]
            if (nx in 0 until width && ny in 0 until height) {
                val target = grid[nx][ny]
                if (target.type != ComponentType.EMPTY) {
                    grid[nx][ny] = target.copy(temperature = (target.temperature + intensity).coerceAtMost(2500f))
                }
            }
        }
    }

    private fun triggerExplosion(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int
    ) {
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    if (grid[nx][ny].type != ComponentType.BEDROCK) {
                        grid[nx][ny] = GridComponent(ComponentType.FIRE, temperature = 800f)
                    }
                }
            }
        }
    }
}
