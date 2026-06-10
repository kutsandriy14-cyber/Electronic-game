package com.example.functional

import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.engine.FunctionalEngine
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Definition and configuration for the Conveyor Belt component.
 */
object Conveyor {
    /**
     * Checks if component type is a conveyor belt.
     */
    fun isConveyor(type: ComponentType): Boolean = type == ComponentType.CONVEYOR_BELT

    /**
     * Simulates conveyor belt pushing solids and liquids resting on top of it.
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
        if (!comp.isPowered) return

        // Push target item: usually item directly above the belt
        val itemX = when (comp.direction) {
            Direction.RIGHT -> x - 1
            Direction.LEFT -> x + 1
            else -> x
        }
        val itemY = when (comp.direction) {
            Direction.UP -> y + 1
            Direction.DOWN -> y - 1
            else -> y - 1 // Default to above
        }

        val moveX = Physical.getDx(comp.direction)
        val moveY = Physical.getDy(comp.direction)

        if (itemX in 0 until width && itemY in 0 until height) {
            val itemComp = grid[itemX][itemY]
            if (FunctionalEngine.isMobileParticle(itemComp.type) && !moved[itemX][itemY]) {
                val destX = itemX + moveX
                val destY = itemY + moveY
                if (destX in 0 until width && destY in 0 until height) {
                    if (grid[destX][destY].type == ComponentType.EMPTY) {
                        grid[destX][destY] = itemComp
                        grid[itemX][itemY] = GridComponent()
                        moved[itemX][itemY] = true
                        moved[destX][destY] = true
                    }
                }
            }
        }
    }
}

