package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates audio frequencies, electromagnetic coil impedance, decibel outputs, and microphone/sound pickups.
 */
object SpeakerBuzzer {
    fun isAcoustic(type: ComponentType): Boolean {
        return type == ComponentType.SPEAKER ||
               type == ComponentType.BUZZER ||
               type == ComponentType.SOUND_SENSOR ||
               type == ComponentType.MICROPHONE
    }

    /**
     * Standard frequency in Hertz.
     */
    fun getDefaultFrequencyHz(type: ComponentType): Float {
        return when (type) {
            ComponentType.BUZZER -> 2500f // 2.5 kHz resonant frequency
            else -> 1000f // 1 kHz standard sine reference
        }
    }
}
