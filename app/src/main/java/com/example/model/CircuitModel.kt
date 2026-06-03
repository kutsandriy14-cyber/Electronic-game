package com.example.model

enum class ComponentCategory(val title: String) { 
    TOOLS("Tools & Modes"), 
    POWER("Power Sources"), 
    CONDUCTORS("Conveyance"), 
    SWITCHES("Switches & Inputs"), 
    SENSORS("Sensors"),
    OUTPUTS("Outputs & Actuators"), 
    LOGIC("Logic Gates"), 
    ANALOG_ICS("Analog ICs"),
    ADVANCED("Advanced & Memory"),
    MATERIALS("Materials & Fluids") 
}

enum class ComponentType(val category: ComponentCategory) {
    EMPTY(ComponentCategory.TOOLS), 
    PAN(ComponentCategory.TOOLS), 
    ROTATE(ComponentCategory.TOOLS), 
    INSPECT(ComponentCategory.TOOLS),
    MULTIMETER(ComponentCategory.TOOLS),

    BATTERY(ComponentCategory.POWER), 
    BATTERY_PACK(ComponentCategory.POWER),
    COIN_CELL(ComponentCategory.POWER),
    GENERATOR(ComponentCategory.POWER), 
    SOLAR_PANEL(ComponentCategory.POWER),
    AC_SOURCE(ComponentCategory.POWER),
    WIND_TURBINE(ComponentCategory.POWER),
    NUCLEAR_REACTOR(ComponentCategory.POWER),
    GEOTHERMAL_GENERATOR(ComponentCategory.POWER),
    HYDRO_GENERATOR(ComponentCategory.POWER),
    THERMOELECTRIC_GENERATOR(ComponentCategory.POWER),
    
    WIRE_ANY(ComponentCategory.CONDUCTORS), 
    RESISTOR(ComponentCategory.CONDUCTORS), 
    CAPACITOR(ComponentCategory.CONDUCTORS), 
    INDUCTOR(ComponentCategory.CONDUCTORS),
    DIODE(ComponentCategory.CONDUCTORS),
    ZENER_DIODE(ComponentCategory.CONDUCTORS),
    FUSE(ComponentCategory.CONDUCTORS),
    TRANSFORMER(ComponentCategory.CONDUCTORS),
    SUPERCONDUCTOR(ComponentCategory.CONDUCTORS),
    HIGH_VOLTAGE_CABLE(ComponentCategory.CONDUCTORS),
    FIBER_OPTIC(ComponentCategory.CONDUCTORS),
    
    SWITCH_OPEN(ComponentCategory.SWITCHES), 
    SWITCH_CLOSED(ComponentCategory.SWITCHES), 
    PUSH_BUTTON(ComponentCategory.SWITCHES),
    DIP_SWITCH(ComponentCategory.SWITCHES),
    REED_SWITCH(ComponentCategory.SWITCHES),
    LIMIT_SWITCH(ComponentCategory.SWITCHES),
    RELAY(ComponentCategory.SWITCHES),
    TRANSISTOR(ComponentCategory.SWITCHES),
    MOSFET(ComponentCategory.SWITCHES),
    POTENTIOMETER(ComponentCategory.SWITCHES),
    MAGNETIC_CONTACT(ComponentCategory.SWITCHES),
    PRESSURE_PAD(ComponentCategory.SWITCHES),
    
    PHOTORESISTOR(ComponentCategory.SENSORS),
    THERMISTOR(ComponentCategory.SENSORS),
    TEMPERATURE_SENSOR(ComponentCategory.SENSORS),
    LIGHT_SENSOR(ComponentCategory.SENSORS),
    PROXIMITY_SENSOR(ComponentCategory.SENSORS),
    ULTRASONIC_SENSOR(ComponentCategory.SENSORS),
    SOUND_SENSOR(ComponentCategory.SENSORS),
    VIBRATION_SENSOR(ComponentCategory.SENSORS),
    GAS_SENSOR(ComponentCategory.SENSORS),
    MOISTURE_SENSOR(ComponentCategory.SENSORS),
    HALL_EFFECT_SENSOR(ComponentCategory.SENSORS),
    PIR_MOTION_SENSOR(ComponentCategory.SENSORS),
    
    BULB(ComponentCategory.OUTPUTS), 
    LED(ComponentCategory.OUTPUTS), 
    RGB_LED(ComponentCategory.OUTPUTS),
    SEVEN_SEGMENT(ComponentCategory.OUTPUTS),
    FOURTEEN_SEGMENT(ComponentCategory.OUTPUTS),
    LCD_DISPLAY_16X2(ComponentCategory.OUTPUTS),
    MOTOR(ComponentCategory.OUTPUTS), 
    SERVO_MOTOR(ComponentCategory.OUTPUTS),
    STEPPER_MOTOR(ComponentCategory.OUTPUTS),
    SPEAKER(ComponentCategory.OUTPUTS),
    BUZZER(ComponentCategory.OUTPUTS),
    LASER_DIODE(ComponentCategory.OUTPUTS),
    HEATER(ComponentCategory.OUTPUTS),
    COOLER(ComponentCategory.OUTPUTS),
    WATER_PUMP(ComponentCategory.OUTPUTS),
    FAN(ComponentCategory.OUTPUTS),
    MONITOR_OLED(ComponentCategory.OUTPUTS),
    CRT_MONITOR(ComponentCategory.OUTPUTS),
    DISPLAY_PIXEL(ComponentCategory.OUTPUTS),
    
    LOGIC_AND(ComponentCategory.LOGIC), 
    LOGIC_OR(ComponentCategory.LOGIC), 
    LOGIC_NOT(ComponentCategory.LOGIC),
    LOGIC_NAND(ComponentCategory.LOGIC),
    LOGIC_NOR(ComponentCategory.LOGIC),
    LOGIC_XOR(ComponentCategory.LOGIC),
    LOGIC_XNOR(ComponentCategory.LOGIC),
    PULSE_GENERATOR(ComponentCategory.LOGIC),
    D_FLIP_FLOP(ComponentCategory.LOGIC),
    T_FLIP_FLOP(ComponentCategory.LOGIC),
    JK_FLIP_FLOP(ComponentCategory.LOGIC),
    MULTIPLEXER(ComponentCategory.LOGIC),
    DEMULTIPLEXER(ComponentCategory.LOGIC),
    SHIFT_REGISTER(ComponentCategory.LOGIC),
    HALF_ADDER(ComponentCategory.LOGIC),
    FULL_ADDER(ComponentCategory.LOGIC),
    LATCH_SR(ComponentCategory.LOGIC),
    
    TIMER_555(ComponentCategory.ANALOG_ICS),
    OP_AMP(ComponentCategory.ANALOG_ICS),
    ADC(ComponentCategory.ANALOG_ICS),
    DAC(ComponentCategory.ANALOG_ICS),
    COMPARATOR(ComponentCategory.ANALOG_ICS),
    VOLTAGE_REGULATOR(ComponentCategory.ANALOG_ICS),
    AMPLIFIER(ComponentCategory.ANALOG_ICS),
    BUFFER(ComponentCategory.ANALOG_ICS),
    
    MICROCONTROLLER(ComponentCategory.ADVANCED),
    MEMORY_RAM(ComponentCategory.ADVANCED),
    MEMORY_ROM(ComponentCategory.ADVANCED),
    
    WATER(ComponentCategory.MATERIALS),
    LAVA(ComponentCategory.MATERIALS),
    OIL(ComponentCategory.MATERIALS),
    ACID(ComponentCategory.MATERIALS),
    SAND(ComponentCategory.MATERIALS),
    DIRT(ComponentCategory.MATERIALS),
    STONE(ComponentCategory.MATERIALS),
    GLASS(ComponentCategory.MATERIALS),
    WOOD(ComponentCategory.MATERIALS),
    FIRE(ComponentCategory.MATERIALS),
    ICE(ComponentCategory.MATERIALS),
    STEAM(ComponentCategory.MATERIALS),
    SLIME(ComponentCategory.MATERIALS),
    RUBBER(ComponentCategory.MATERIALS),
    DIAMOND(ComponentCategory.MATERIALS),
    COAL(ComponentCategory.MATERIALS),
    SPONGE(ComponentCategory.MATERIALS),
    GASOLINE(ComponentCategory.MATERIALS),
    LIQUID_NITROGEN(ComponentCategory.MATERIALS),
    URANIUM(ComponentCategory.MATERIALS),
    MAGIC_DUST(ComponentCategory.MATERIALS),
    PIPE(ComponentCategory.MATERIALS),
    
    INFINITE_BATTERY(ComponentCategory.POWER),
    INFINITE_WATER(ComponentCategory.MATERIALS),
    INFINITE_LAVA(ComponentCategory.MATERIALS),
    FLUID_DRAIN(ComponentCategory.MATERIALS),
    VOID_HOLE(ComponentCategory.MATERIALS),
    CONVEYOR_BELT(ComponentCategory.OUTPUTS),
    MAGNET(ComponentCategory.OUTPUTS),
    PISTON(ComponentCategory.OUTPUTS),
    
    // Additional requested components
    KEYPAD_4X4(ComponentCategory.SWITCHES),
    PUSH_BUTTON_UP(ComponentCategory.SWITCHES),
    PUSH_BUTTON_DOWN(ComponentCategory.SWITCHES),
    PUSH_BUTTON_LEFT(ComponentCategory.SWITCHES),
    PUSH_BUTTON_RIGHT(ComponentCategory.SWITCHES),
    JOYSTICK(ComponentCategory.SWITCHES),
    ENCODER(ComponentCategory.SWITCHES),
    
    STEP_DOWN_CONVERTER(ComponentCategory.POWER),
    STEP_UP_CONVERTER(ComponentCategory.POWER),
    INVERTER(ComponentCategory.POWER),
    
    ACCELEROMETER(ComponentCategory.SENSORS),
    GYROSCOPE(ComponentCategory.SENSORS),
    MAGNETOMETER(ComponentCategory.SENSORS),
    BAROMETER(ComponentCategory.SENSORS),
    PRESSURE_SENSOR(ComponentCategory.SENSORS),
    HUMIDITY_SENSOR(ComponentCategory.SENSORS),
    COLOR_SENSOR(ComponentCategory.SENSORS),
    FINGERPRINT_SCANNER(ComponentCategory.SENSORS),
    CAMERA_MODULE(ComponentCategory.SENSORS),
    MICROPHONE(ComponentCategory.SENSORS),
    UV_SENSOR(ComponentCategory.SENSORS),
    PH_SENSOR(ComponentCategory.SENSORS),
    
    DISPLAY_7SEG_4DIGIT(ComponentCategory.OUTPUTS),
    DISPLAY_OLED_128X64(ComponentCategory.OUTPUTS),
    DISPLAY_TFT_24(ComponentCategory.OUTPUTS),
    E_PAPER_DISPLAY(ComponentCategory.OUTPUTS),
    VIBRATION_MOTOR(ComponentCategory.OUTPUTS),
    SOLENOID(ComponentCategory.OUTPUTS),
    LINEAR_ACTUATOR(ComponentCategory.OUTPUTS),
    RELAY_MODULE_4CH(ComponentCategory.OUTPUTS),
    PELTIER_MODULE(ComponentCategory.OUTPUTS),
    
    IC_7400_NAND(ComponentCategory.LOGIC),
    IC_7402_NOR(ComponentCategory.LOGIC),
    IC_7404_NOT(ComponentCategory.LOGIC),
    IC_7408_AND(ComponentCategory.LOGIC),
    IC_7432_OR(ComponentCategory.LOGIC),
    IC_7486_XOR(ComponentCategory.LOGIC),
    IC_7447_DECODER(ComponentCategory.LOGIC),
    IC_CD4017_DECADE(ComponentCategory.LOGIC),
    
    IC_LM358_OPAMP(ComponentCategory.ANALOG_ICS),
    IC_LM324_OPAMP(ComponentCategory.ANALOG_ICS),
    IC_LM317_REG(ComponentCategory.ANALOG_ICS),
    IC_L298N_MOTOR(ComponentCategory.ANALOG_ICS),
    IC_ULN2003(ComponentCategory.ANALOG_ICS),
    
    STEEL(ComponentCategory.MATERIALS),
    COPPER(ComponentCategory.MATERIALS),
    GOLD(ComponentCategory.MATERIALS),
    ALUMINUM(ComponentCategory.MATERIALS),
    PLASTIC(ComponentCategory.MATERIALS),
    CLAY(ComponentCategory.MATERIALS),
    BRICK(ComponentCategory.MATERIALS),
    OBSIDIAN(ComponentCategory.MATERIALS),
    BEDROCK(ComponentCategory.MATERIALS)
}

enum class Direction { 
    UP, RIGHT, DOWN, LEFT; 
    fun next() = values()[(ordinal + 1) % 4] 
}

data class GridComponent(
    val type: ComponentType = ComponentType.EMPTY,
    val direction: Direction = Direction.UP,
    val isPowered: Boolean = false,
    val logicState: Boolean = false,
    val extraData: String = "", // For Lua scripts, properties, etc.
    val charge: Float = -1f,
    val isOverloaded: Boolean = false,
    val voltage: Float = 0f,
    val current: Float = 0f, // in mA
    val resistance: Float = 0f, // in Ohms
    val temperature: Float = 0f, // in Celsius / heat indicator
    val pressure: Float = 0f // pipe fluid pressure / atmosphere pressure
) {
    fun clone() = GridComponent(type, direction, isPowered, logicState, extraData, charge, isOverloaded, voltage, current, resistance, temperature, pressure)
}

data class Telemetry(
    val totalVoltage: Float = 0f,
    val totalCurrent: Float = 0f, // mA
    val totalPower: Float = 0f, // W
    val isShortCircuit: Boolean = false,
    val runningScripts: Int = 0
)

data class SimulationResult(
    val grid: Array<Array<GridComponent>>,
    val telemetry: Telemetry,
    val logs: List<String> = emptyList()
)

