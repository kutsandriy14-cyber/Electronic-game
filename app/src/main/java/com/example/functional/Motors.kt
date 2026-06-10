package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates dynamic rotary velocities, stepper counts, and torque multipliers of Servo, Stepper, and Vibration Motors.
 */
object Motors {
    fun isMotorType(type: ComponentType): Boolean {
        return type == ComponentType.MOTOR ||
               type == ComponentType.SERVO_MOTOR ||
               type == ComponentType.STEPPER_MOTOR ||
               type == ComponentType.VIBRATION_MOTOR
    }

    /**
     * Determines the maximum operating rotational rate in RPM.
     */
    fun getMaxSpeedRPM(type: ComponentType): Float {
        return when (type) {
            ComponentType.STEPPER_MOTOR -> 300f
            ComponentType.SERVO_MOTOR -> 60f // Slow, high precision sweep
            else -> 6000f // High speed miniature motor
        }
    }
}
