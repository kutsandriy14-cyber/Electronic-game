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
 * Centered direction vectors, component rotation handlers, and physical properties.
 */
object Physical {
    /**
     * Determines if a given component type supports directional rotation.
     */
    fun canRotate(type: ComponentType): Boolean {
        return type != ComponentType.EMPTY &&
               type != ComponentType.PAN &&
               type != ComponentType.ROTATE &&
               type != ComponentType.INSPECT &&
               type != ComponentType.MULTIMETER
    }

    /**
     * Rotates a component to its next sequential clock-wise direction.
     */
    fun rotate(component: GridComponent): GridComponent {
        return component.copy(direction = component.direction.next())
    }

    /**
     * Translates a Direction enum to a horizontal delta step (X axis).
     */
    fun getDx(direction: Direction): Int {
        return when (direction) {
            Direction.RIGHT -> 1
            Direction.LEFT -> -1
            else -> 0
        }
    }

    /**
     * Translates a Direction enum to a vertical delta step (Y axis).
     */
    fun getDy(direction: Direction): Int {
        return when (direction) {
            Direction.DOWN -> 1
            Direction.UP -> -1
            else -> 0
        }
    }
}
