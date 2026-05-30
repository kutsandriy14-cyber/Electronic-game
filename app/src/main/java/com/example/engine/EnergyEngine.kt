package com.example.engine

import com.example.model.ComponentType
import com.example.model.GridComponent

object EnergyEngine {

    class CircuitProps(
        var voltage: Float = 0f,
        var resistance: Float = 1f, // Default base resistance to prevent arithmetic issues
        var totalSources: Int = 0,
        var totalLoads: Int = 0
    )

    fun calculateInitialProperties(grid: Array<Array<GridComponent>>, width: Int, height: Int): CircuitProps {
        val props = CircuitProps()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.EMPTY) continue
                
                val parsedProps = parseExtraData(comp.extraData)
                
                // Add Voltage Sources
                if (isPowerSource(comp.type)) {
                    val maxV = parsedProps["v"]?.toFloatOrNull() ?: getDefaultVoltage(comp.type)
                    val maxCharge = parsedProps["c"]?.toFloatOrNull() ?: getDefaultCapacity(comp.type)
                    val charge = if (comp.charge < 0f) maxCharge else comp.charge
                    
                    if (charge > 0 || comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INFINITE_BATTERY) {
                        val currentV = if (comp.type == ComponentType.AC_SOURCE || comp.type == ComponentType.INFINITE_BATTERY) maxV else maxV * (charge / maxCharge)
                        if (currentV > 0.1f) {
                            props.voltage += currentV
                            props.totalSources++
                        }
                    }
                }
                
                // Static Resistance (components that add resistance even if not structurally analyzed perfectly yet)
                if (comp.type == ComponentType.RESISTOR) {
                    props.resistance += parsedProps["r"]?.toFloatOrNull() ?: 330f
                }
                
                // Load components
                if (isLoad(comp.type)) {
                    props.totalLoads++
                }
            }
        }
        
        return props
    }
    
    fun addDynamicResistanceForComponent(type: ComponentType, props: Map<String, String>): Float {
        return when (type) {
            ComponentType.WIRE_ANY, ComponentType.SUPERCONDUCTOR, ComponentType.HIGH_VOLTAGE_CABLE, ComponentType.FIBER_OPTIC -> 0.01f // Almost zero resistance for wires
            ComponentType.BULB, ComponentType.LED, ComponentType.RGB_LED, ComponentType.SEVEN_SEGMENT -> props["r"]?.toFloatOrNull() ?: 50f
            ComponentType.LASER_DIODE, ComponentType.HEATER, ComponentType.FIRE -> 20f
            ComponentType.COOLER, ComponentType.WATER_PUMP, ComponentType.FAN -> 30f
            ComponentType.MOTOR, ComponentType.SERVO_MOTOR, ComponentType.STEPPER_MOTOR -> 40f
            ComponentType.VIBRATION_MOTOR, ComponentType.LINEAR_ACTUATOR, ComponentType.SOLENOID -> 45f
            ComponentType.SPEAKER, ComponentType.BUZZER, ComponentType.AMPLIFIER -> 15f
            ComponentType.DISPLAY_7SEG_4DIGIT, ComponentType.DISPLAY_OLED_128X64, ComponentType.DISPLAY_TFT_24, ComponentType.E_PAPER_DISPLAY -> 60f
            ComponentType.MICROCONTROLLER, ComponentType.MEMORY_RAM, ComponentType.MEMORY_ROM, ComponentType.ESP32, ComponentType.ARDUINO_UNO -> 100f
            ComponentType.IC_7400_NAND, ComponentType.IC_7408_AND, ComponentType.IC_7432_OR -> 80f
            else -> 0.5f // Default small resistance
        }
    }

    private fun parseExtraData(data: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (data.isEmpty()) return map
        data.split("|").forEach {
            val parts = it.split("=")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    fun isPowerSource(type: ComponentType): Boolean {
        return type in listOf(
            ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, 
            ComponentType.AC_SOURCE, ComponentType.WIND_TURBINE, ComponentType.NUCLEAR_REACTOR, 
            ComponentType.GEOTHERMAL_GENERATOR, ComponentType.HYDRO_GENERATOR, 
            ComponentType.THERMOELECTRIC_GENERATOR, ComponentType.INFINITE_BATTERY,
            ComponentType.GENERATOR, ComponentType.SOLAR_PANEL
        )
    }
    
    // Check if it's something that consumes power
    fun isLoad(type: ComponentType): Boolean {
        return type in listOf(
            ComponentType.BULB, ComponentType.LED, ComponentType.RGB_LED, ComponentType.MOTOR, ComponentType.HEATER,
            ComponentType.COOLER, ComponentType.SPEAKER, ComponentType.BUZZER, ComponentType.LASER_DIODE,
            ComponentType.WATER_PUMP, ComponentType.FAN
        )
    }

    fun getDefaultVoltage(type: ComponentType): Float {
        return when (type) {
            ComponentType.COIN_CELL -> 3f
            ComponentType.BATTERY_PACK -> 12f
            ComponentType.NUCLEAR_REACTOR -> 1000f
            ComponentType.INFINITE_BATTERY -> 9000f
            else -> 9f
        }
    }

    fun getDefaultCapacity(type: ComponentType): Float {
        return when (type) {
            ComponentType.COIN_CELL -> 100f
            ComponentType.BATTERY_PACK -> 10000f
            ComponentType.NUCLEAR_REACTOR -> 1000000f
            ComponentType.INFINITE_BATTERY -> 9999999f
            else -> 2500f
        }
    }
}
