package com.example.engine

import com.example.functional.Battery
import com.example.functional.Conveyor
import com.example.functional.Fluid
import com.example.functional.Generator
import com.example.functional.Physical
import com.example.functional.Piston
import com.example.functional.Uranium
import com.example.functional.WaterPump
import com.example.functional.Fan
import com.example.functional.Motor
import com.example.functional.Heater
import com.example.functional.Cooler
import com.example.functional.Peltier
import com.example.functional.Magnet
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent

/**
 * Main coordinator handling block-by-block operations for advanced machine items
 * such as water pumps, fans, motors, pistons, conveyor belts, heaters, and geothermal/nuclear elements.
 * Fully refactored to decouple components into their specific target files under package `com.example.functional`.
 */
object FunctionalEngine {

    /**
     * Determines whether a component of certain type can be freely moved or acted upon as dynamic particle.
     */
    fun isMobileParticle(type: ComponentType): Boolean {
        return Fluid.isFluid(type) ||
               type == ComponentType.STEAM ||
               type == ComponentType.FIRE ||
               type == ComponentType.SAND ||
               type == ComponentType.DIRT ||
               type == ComponentType.MAGIC_DUST ||
               type == ComponentType.ICE
    }

    /**
     * Performs water pump simulation. Delegates to WaterPump object.
     */
    fun simulateWaterPump(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        WaterPump.simulate(grid, x, y, comp, width, height)
    }

    /**
     * Simulates fan blowing wind. Delegates to Fan object.
     */
    fun simulateFan(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        Fan.simulate(grid, x, y, comp, width, height)
    }

    /**
     * Simulates motor rotation torque. Delegates to Motor object.
     */
    fun simulateMotor(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        Motor.simulate(grid, x, y, comp, width, height)
    }

    /**
     * Simulates conveyor belt pushing solids. Delegates to Conveyor object.
     */
    fun simulateConveyorBelt(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        Conveyor.simulate(grid, x, y, comp, width, height, moved)
    }

    /**
     * Simulates Uranium heat cycles. Delegates to Uranium object.
     */
    fun simulateUranium(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        Uranium.simulate(grid, x, y, comp, width, height, moved)
    }

    /**
     * Simulates pistons extending or pulling blocks.
     */
    fun simulatePiston(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        val moveX = Physical.getDx(comp.direction)
        val moveY = Physical.getDy(comp.direction)

        if (comp.isPowered && !comp.logicState) {
            val pushX = x + moveX
            val pushY = y + moveY
            var length = 0
            var currentPushX = pushX
            var currentPushY = pushY
            while (currentPushX in 0 until width && currentPushY in 0 until height &&
                   grid[currentPushX][currentPushY].type.category == ComponentCategory.MATERIALS && length < Piston.MAX_PUSH_LENGTH) {
                length++
                currentPushX += moveX
                currentPushY += moveY
            }
            if (length > 0) {
                val endEmptyX = pushX + length * moveX
                val endEmptyY = pushY + length * moveY
                if (endEmptyX in 0 until width && endEmptyY in 0 until height && grid[endEmptyX][endEmptyY].type == ComponentType.EMPTY) {
                    for (i in length downTo 1) {
                        val fromX = pushX + (i - 1) * moveX
                        val fromY = pushY + (i - 1) * moveY
                        val toX = pushX + i * moveX
                        val toY = pushY + i * moveY
                        grid[toX][toY] = grid[fromX][fromY]
                    }
                    grid[pushX][pushY] = GridComponent(ComponentType.EMPTY)
                }
            }
            grid[x][y] = comp.copy(logicState = true)
        } else if (!comp.isPowered && comp.logicState) {
            val retractX = x + moveX
            val retractY = y + moveY
            if (retractX in 0 until width && retractY in 0 until height) {
                val pullX = retractX + moveX
                val pullY = retractY + moveY
                if (pullX in 0 until width && pullY in 0 until height && grid[retractX][retractY].type == ComponentType.EMPTY) {
                    grid[retractX][retractY] = grid[pullX][pullY]
                    grid[pullX][pullY] = GridComponent(ComponentType.EMPTY)
                }
            }
            grid[x][y] = comp.copy(logicState = false)
        }
    }
}
