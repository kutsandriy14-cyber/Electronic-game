package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates responsive physical sensor components, transducing thresholds, and environment readings.
 */
object Sensors {
    fun isSensor(type: ComponentType): Boolean {
        return type == ComponentType.PHOTORESISTOR ||
               type == ComponentType.THERMISTOR ||
               type == ComponentType.TEMPERATURE_SENSOR ||
               type == ComponentType.LIGHT_SENSOR ||
               type == ComponentType.PROXIMITY_SENSOR ||
               type == ComponentType.ULTRASONIC_SENSOR ||
               type == ComponentType.SOUND_SENSOR ||
               type == ComponentType.VIBRATION_SENSOR ||
               type == ComponentType.GAS_SENSOR ||
               type == ComponentType.MOISTURE_SENSOR ||
               type == ComponentType.HALL_EFFECT_SENSOR ||
               type == ComponentType.PIR_MOTION_SENSOR ||
               type == ComponentType.ACCELEROMETER ||
               type == ComponentType.GYROSCOPE ||
               type == ComponentType.MAGNETOMETER ||
               type == ComponentType.BAROMETER ||
               type == ComponentType.PRESSURE_SENSOR ||
               type == ComponentType.HUMIDITY_SENSOR ||
               type == ComponentType.COLOR_SENSOR ||
               type == ComponentType.FINGERPRINT_SCANNER ||
               type == ComponentType.CAMERA_MODULE ||
               type == ComponentType.UV_SENSOR ||
               type == ComponentType.PH_SENSOR
    }

    /**
     * Standard sensitivity parameters.
     */
    fun getTransductionCoefficient(type: ComponentType): Float = 0.85f
}
