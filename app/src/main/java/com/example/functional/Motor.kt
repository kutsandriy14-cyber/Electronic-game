package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.FunctionalEngine
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates behaviors and mechanics of the motorized rotary torque actuator.
 */
object Motor {
    /**
     * Checks if component type is a Motor, Servo, or Stepper motor.
     */
    fun isMotor(type: ComponentType): Boolean {
        return type == ComponentType.MOTOR ||
               type == ComponentType.SERVO_MOTOR ||
               type == ComponentType.STEPPER_MOTOR
    }

    /**
     * Simulates motor rotation driving nearby movable particles forward in its facing direction.
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

        val outDx = Physical.getDx(comp.direction)
        val outDy = Physical.getDy(comp.direction)

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = x + dx
                val ty = y + dy
                if (tx in 0 until width && ty in 0 until height) {
                    if (FunctionalEngine.isMobileParticle(grid[tx][ty].type)) {
                        val destX = tx + outDx
                        val destY = ty + outDy
                        if (destX in 0 until width && destY in 0 until height && grid[destX][destY].type == ComponentType.EMPTY) {
                            grid[destX][destY] = grid[tx][ty]
                            grid[tx][ty] = GridComponent(ComponentType.EMPTY)
                        }
                    }
                }
            }
        }
    }
}
