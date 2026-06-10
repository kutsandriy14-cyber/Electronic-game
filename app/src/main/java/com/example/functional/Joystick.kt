package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates dual-axis analog potentiometers and integrated momentary select buttons of Joysticks.
 */
object Joystick {
    fun isJoystick(type: ComponentType): Boolean = type == ComponentType.JOYSTICK

    fun getDefaultCenterValue(): Float = 2.5f // 2.5V center on a 5V scale
}
