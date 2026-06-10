package com.example.functional

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates solid-state thermoelectric heat pump dynamics for Peltier modules.
 */
object Peltier {
    /**
     * Checks if component type is a Peltier module.
     */
    fun isPeltier(type: ComponentType): Boolean = type == ComponentType.PELTIER_MODULE

    /**
     * Simulates thermodynamic differential pumping and power generation/logic feedback.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ): GridComponent {
        if (!comp.isPowered) {
            return comp.copy(logicState = false)
        }

        val heatX = when (comp.direction) {
            Direction.RIGHT -> x + 1
            Direction.LEFT -> x - 1
            else -> x
        }
        val heatY = when (comp.direction) {
            Direction.DOWN -> y + 1
            Direction.UP -> y - 1
            else -> y
        }
        val coolX = when (comp.direction) {
            Direction.RIGHT -> x - 1
            Direction.LEFT -> x + 1
            else -> x
        }
        val coolY = when (comp.direction) {
            Direction.DOWN -> y - 1
            Direction.UP -> y + 1
            else -> y
        }

        var tempDiff = 0f
        if (heatX in 0 until width && heatY in 0 until height) {
            val target = grid[heatX][heatY]
            tempDiff += target.temperature
            grid[heatX][heatY] = target.copy(temperature = (target.temperature + 10f).coerceAtMost(500f))
        }
        if (coolX in 0 until width && coolY in 0 until height) {
            val target = grid[coolX][coolY]
            tempDiff -= target.temperature
            grid[coolX][coolY] = target.copy(temperature = (target.temperature - 10f).coerceAtLeast(-273f))
        }

        return if (tempDiff > 100f) {
            comp.copy(logicState = true)
        } else {
            comp.copy(logicState = false)
        }
    }
}
