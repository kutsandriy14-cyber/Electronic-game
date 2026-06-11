package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates the operations for the directional fluid induction WaterPump actuator.
 */
object WaterPump {
    /**
     * Checks if component type is a WaterPump.
     */
    fun isWaterPump(type: ComponentType): Boolean = type == ComponentType.WATER_PUMP

    /**
     * Simulates water pump taking in fluid from the intake and outputting it.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int
    ) {
        if (!comp.isPowered) return

        val inDx = -Physical.getDx(comp.direction)
        val inDy = -Physical.getDy(comp.direction)
        val outDx = Physical.getDx(comp.direction)
        val outDy = Physical.getDy(comp.direction)

        val inX = x + inDx
        val inY = y + inDy
        val outX = x + outDx
        val outY = y + outDy

        if (inX in 0 until width && inY in 0 until height && outX in 0 until width && outY in 0 until height) {
            val inComp = grid[inX][inY]
            if (Fluid.isFluid(inComp.type) && grid[outX][outY].type == ComponentType.EMPTY) {
                grid[outX][outY] = inComp
                grid[inX][inY] = GridComponent(ComponentType.EMPTY)
            }
        }
    }
}
