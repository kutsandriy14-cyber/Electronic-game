package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * IcL298nMotor simulates an L298N dual H-bridge driver stepping outputs corresponding to high input direction lines.
 */
object IcL298nMotor {
    fun isL298N(type: ComponentType): Boolean = type == ComponentType.IC_L298N_MOTOR

    fun simulate(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val in1 = if (x > 0) grid[x-1][y].logicState else false
        val in2 = if (x < width-1) grid[x+1][y].logicState else false
        val outActive = in1 xor in2
        return comp.copy(isPowered = outActive, voltage = if (outActive) 12f else 0f, logicState = outActive)
    }
}
