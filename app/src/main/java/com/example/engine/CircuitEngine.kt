package com.example.engine

import android.util.Base64
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.SimulationResult
import com.example.model.Telemetry
import com.example.functional.*

class CircuitEngine {

    // FIXED: BUG 1.2 (pulse), BUG 1.3 (MCU logic), BUG 1.4 (dead code removal)
    // ADDED: FEATURE 6.1 (logic gates), FEATURE 6.2 (flip-flops), FEATURE 6.3/6.4 (Sensors)

    private fun evaluateCondition(cond: String): Boolean {
        val trimmed = cond.trim()
        return when {
            trimmed.contains("!=") -> {
                val parts = trimmed.split("!=")
                if (parts.size == 2) parts[0].trim() != parts[1].trim() else false
            }
            trimmed.contains("==") -> {
                val parts = trimmed.split("==")
                if (parts.size == 2) parts[0].trim() == parts[1].trim() else false
            }
            trimmed.contains(">=") -> {
                val parts = trimmed.split(">=")
                if (parts.size == 2) (parts[0].trim().toFloatOrNull() ?: 0f) >= (parts[1].trim().toFloatOrNull() ?: 0f) else false
            }
            trimmed.contains(">") -> {
                val parts = trimmed.split(">")
                if (parts.size == 2) (parts[0].trim().toFloatOrNull() ?: 0f) > (parts[1].trim().toFloatOrNull() ?: 0f) else false
            }
            trimmed == "0" || trimmed == "false" -> false
            trimmed == "1" || trimmed == "true" -> true
            else -> trimmed.isNotEmpty() && trimmed != "0"
        }
    }

    private fun runScripts(grid: Array<Array<GridComponent>>, width: Int, height: Int): List<String> {
        val logs = mutableListOf<String>()
        val originalGrid = Array(width) { x -> Array(height) { y -> grid[x][y].copy() } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    val lines = comp.extraData.split('\n')
                    for (line in lines) {
                        var parsedLine = line.trim()
                        if (parsedLine.isEmpty() || parsedLine.startsWith("--") || parsedLine.startsWith("//")) continue
                        
                        for (p in 0..3) {
                            if (parsedLine.contains("in($p)")) {
                                val nx = when(p) { 0 -> x; 1 -> x + 1; 2 -> x; 3 -> x - 1; else -> -1 }
                                val ny = when(p) { 0 -> y - 1; 1 -> y; 2 -> y + 1; 3 -> y; else -> -1 }
                                val v = if (nx in 0 until width && ny in 0 until height && originalGrid[nx][ny].logicState) "1" else "0"
                                parsedLine = parsedLine.replace("in($p)", v)
                            }
                        }
                        
                        var shouldExecute = true
                        var commandPart = parsedLine
                        
                        if (parsedLine.startsWith("if")) {
                            var cond = parsedLine.substringAfter("if").substringBefore("then")
                            if (cond == parsedLine.substringAfter("if")) {
                                cond = parsedLine.substringAfter("if").substringBefore("out")
                            }
                            if (cond == parsedLine.substringAfter("if")) {
                                cond = parsedLine.substringAfter("if").substringBefore("log")
                            }
                            
                            shouldExecute = evaluateCondition(cond)
                            
                            if (parsedLine.contains("then")) {
                                commandPart = parsedLine.substringAfter("then").trim()
                            } else if (parsedLine.contains("out(")) {
                                commandPart = "out(" + parsedLine.substringAfter("out(")
                            } else if (parsedLine.contains("log(")) {
                                commandPart = "log(" + parsedLine.substringAfter("log(")
                            }
                        }
                        
                        if (shouldExecute) {
                            if (commandPart.startsWith("log(")) {
                                val msg = commandPart.substringAfter("log(").substringBeforeLast(")")
                                logs.add("[MCU $x,$y]: $msg")
                            } else if (commandPart.startsWith("out(")) {
                                val outPart = commandPart.substringAfter("out(").substringBefore(")")
                                val args = outPart.split(",")
                                if (args.size == 2) {
                                    val pin = args[0].trim().toIntOrNull() ?: 0
                                    val expr = args[1].trim()
                                    val isHigh = evaluateCondition(expr)
                                    
                                    val nx = when(pin) { 0 -> x; 1 -> x + 1; 2 -> x; 3 -> x - 1; else -> -1 }
                                    val ny = when(pin) { 0 -> y - 1; 1 -> y; 2 -> y + 1; 3 -> y; else -> -1 }
                                    
                                    if (nx in 0 until width && ny in 0 until height) {
                                        grid[nx][ny] = grid[nx][ny].copy(logicState = isHigh)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return logs
    }

    fun calculatePower(inputGrid: Array<Array<GridComponent>>, width: Int, height: Int, tick: Long = 0, ramGb: Int = 4, cores: Int = 4): SimulationResult {
        // Deep copy of GridComponent array.
        val grid = Array(width) { x -> inputGrid[x].map { it.copy() }.toTypedArray() }
        var activeScriptsCount = 0

        // Process Logic Gates
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                
                when (comp.type) {
                    ComponentType.LOGIC_AND -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = leftState && rightState
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.LOGIC_OR -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = leftState || rightState
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.LOGIC_NOT -> {
                        val inputSide = when(comp.direction) {
                            Direction.RIGHT -> if (x > 0) inputGrid[x-1][y].logicState else false
                            Direction.LEFT -> if (x < width-1) inputGrid[x+1][y].logicState else false
                            Direction.DOWN -> if (y > 0) inputGrid[x][y-1].logicState else false
                            Direction.UP -> if (y < height-1) inputGrid[x][y+1].logicState else false
                        }
                        val out = !inputSide
                        grid[x][y] = comp.copy(logicState = out)
                        val outputSide = when(comp.direction) {
                            Direction.RIGHT -> if (x < width-1) Pair(x+1, y) else null
                            Direction.LEFT -> if (x > 0) Pair(x-1, y) else null
                            Direction.DOWN -> if (y < height-1) Pair(x, y+1) else null
                            Direction.UP -> if (y > 0) Pair(x, y-1) else null
                        }
                        outputSide?.let { (ox, oy) ->
                            grid[ox][oy] = grid[ox][oy].copy(logicState = out)
                        }
                    }
                    ComponentType.LOGIC_XOR -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = leftState xor rightState
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.LOGIC_NAND -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = !(leftState && rightState)
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.LOGIC_NOR -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = !(leftState || rightState)
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.LOGIC_XNOR -> {
                        val leftState = if (x > 0) inputGrid[x-1][y].logicState else false
                        val rightState = if (x < width-1) inputGrid[x+1][y].logicState else false
                        val out = !(leftState xor rightState)
                        grid[x][y] = comp.copy(logicState = out)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = out)
                    }
                    ComponentType.D_FLIP_FLOP -> {
                        val clockWasHigh = if (comp.charge.isNaN()) false else comp.charge > 0.5f
                        val clockIsHigh = if (x > 0) inputGrid[x-1][y].logicState else false
                        val dInput = if (x < width-1) inputGrid[x+1][y].logicState else false
                        var qOut = comp.logicState
                        if (!clockWasHigh && clockIsHigh) {
                            qOut = dInput
                        }
                        grid[x][y] = comp.copy(logicState = qOut, charge = if (clockIsHigh) 1f else 0f)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = qOut)
                    }
                    ComponentType.T_FLIP_FLOP -> {
                        val clockWasHigh = if (comp.charge.isNaN()) false else comp.charge > 0.5f
                        val clockIsHigh = if (x > 0) inputGrid[x-1][y].logicState else false
                        var qOut = comp.logicState
                        if (!clockWasHigh && clockIsHigh) {
                            qOut = !qOut
                        }
                        grid[x][y] = comp.copy(logicState = qOut, charge = if (clockIsHigh) 1f else 0f)
                        if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = qOut)
                    }
                    ComponentType.JK_FLIP_FLOP -> {
                         val clockWasHigh = if (comp.charge.isNaN()) false else comp.charge > 0.5f
                         val clockIsHigh = if (x > 0) inputGrid[x-1][y].logicState else false
                         val jInput = if (y < height-1) inputGrid[x][y+1].logicState else false
                         val kInput = if (x < width-1) inputGrid[x+1][y].logicState else false
                         var qOut = comp.logicState
                         if (!clockWasHigh && clockIsHigh) {
                             if (jInput && kInput) qOut = !qOut
                             else if (jInput) qOut = true
                             else if (kInput) qOut = false
                         }
                         grid[x][y] = comp.copy(logicState = qOut, charge = if (clockIsHigh) 1f else 0f)
                         if (y > 0) grid[x][y-1] = grid[x][y-1].copy(logicState = qOut)
                    }
                    else -> {}
                }
            }
        }

        // Process Sensors + Pulse generator updates
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                var newLogicState = comp.logicState
                
                if (comp.type == ComponentType.PULSE_GENERATOR) {
                    val rate = CircuitUtils.propFloat(CircuitUtils.parseProps(comp.extraData), "rate", 1f).toLong()
                    val phase = CircuitUtils.propFloat(CircuitUtils.parseProps(comp.extraData), "phase", 0f).toLong()
                    newLogicState = (((tick + phase) / rate) % 2L) == 0L
                    
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = x + dxs[i]; val ny = y + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            grid[nx][ny] = grid[nx][ny].copy(logicState = newLogicState)
                        }
                    }
                } else if (comp.type == ComponentType.TEMPERATURE_SENSOR) {
                    val threshold = CircuitUtils.propFloat(CircuitUtils.parseProps(comp.extraData), "t", 50f)
                    var maxTemp = 0f
                    val dxs = intArrayOf(-1, 1, 0, 0)
                    val dys = intArrayOf(0, 0, -1, 1)
                    for (i in 0..3) {
                        val nx = x + dxs[i]; val ny = y + dys[i]
                        if (nx in 0 until width && ny in 0 until height) {
                            maxTemp = maxOf(maxTemp, grid[nx][ny].temperature)
                        }
                    }
                    newLogicState = maxTemp > threshold
                } else if (comp.type == ComponentType.LIGHT_SENSOR) {
                    newLogicState = LightSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.HUMIDITY_SENSOR) {
                    newLogicState = HumiditySensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.SOUND_SENSOR) {
                    newLogicState = SoundSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.GAS_SENSOR) {
                    newLogicState = GasSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.MOISTURE_SENSOR) {
                    newLogicState = MoistureSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.ACCELEROMETER) {
                    newLogicState = Accelerometer.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.GYROSCOPE) {
                    newLogicState = Gyroscope.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.MAGNETOMETER) {
                    newLogicState = Magnetometer.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.BAROMETER) {
                    newLogicState = Barometer.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.PRESSURE_SENSOR) {
                    newLogicState = PressureSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.PROXIMITY_SENSOR) {
                    newLogicState = ProximitySensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.ULTRASONIC_SENSOR) {
                    newLogicState = UltrasonicSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.PIR_MOTION_SENSOR) {
                    val dummyMoved = Array(width) { BooleanArray(height) { true } }
                    newLogicState = PirMotionSensor.simulate(grid, x, y, comp, width, height, dummyMoved).logicState
                } else if (comp.type == ComponentType.HALL_EFFECT_SENSOR) {
                    newLogicState = HallEffectSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.PH_SENSOR) {
                    newLogicState = PhSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.UV_SENSOR) {
                    newLogicState = UvSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.COLOR_SENSOR) {
                    newLogicState = ColorSensor.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.FINGERPRINT_SCANNER) {
                    newLogicState = FingerprintScanner.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.CAMERA_MODULE) {
                    newLogicState = CameraModule.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.MICROPHONE) {
                    newLogicState = Microphone.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.IC_7408_AND) {
                    newLogicState = Ic7408And.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.IC_7432_OR) {
                    newLogicState = Ic7432Or.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.IC_7404_NOT) {
                    newLogicState = Ic7404Not.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.IC_CD4017_DECADE) {
                    newLogicState = IcCd4017Decade.simulate(grid, x, y, comp, width, height).logicState
                } else if (comp.type == ComponentType.IC_L298N_MOTOR) {
                    newLogicState = IcL298nMotor.simulate(grid, x, y, comp, width, height).logicState
                }
                
                val newCharge = if (EnergyEngine.isPowerSource(comp.type) && (comp.charge < 0f || comp.charge.isNaN())) {
                     CircuitUtils.propFloat(CircuitUtils.parseProps(comp.extraData), "c", RenderEngine.getMaxCap(comp))
                } else {
                     comp.charge
                }
                
                if (comp.logicState != newLogicState || comp.charge != newCharge) {
                    grid[x][y] = comp.copy(logicState = newLogicState, charge = newCharge)
                }
                
                if (comp.type == ComponentType.MICROCONTROLLER && comp.extraData.isNotEmpty()) {
                    activeScriptsCount++
                }
            }
        }
        
        val logs = runScripts(grid, width, height)

        val energyResult = EnergyEngine.simulateEnergyFlow(grid, width, height, activeScriptsCount)
        
        // Link double doors so that if either end is powered, both are powered
        for (tx in 0 until width) {
            for (ty in 0 until height) {
                val comp = grid[tx][ty]
                if (comp.type == ComponentType.DOUBLE_DOOR) {
                    val dx = Physical.getDx(comp.direction)
                    val dy = Physical.getDy(comp.direction)
                    val nx = tx + dx
                    val ny = ty + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        val other = grid[nx][ny]
                        if (other.type == ComponentType.DOUBLE_DOOR) {
                            if (comp.isPowered || other.isPowered) {
                                if (!comp.isPowered || !other.isPowered) {
                                    grid[tx][ty] = comp.copy(isPowered = true)
                                    grid[nx][ny] = other.copy(isPowered = true)
                                }
                            }
                        }
                    }
                }
            }
        }

        PhysicsEngine.simulateMaterials(grid, width, height, energyResult.telemetry.totalVoltage, ramGb)

        return SimulationResult(grid, energyResult.telemetry, logs)
    }

    // Grid serialization methods left untouched as per instructions (or assumes they are elsewhere, but prompt says unchanged signatures).
    // The prompt says "CircuitEngine.serializeGrid/deserializeGrid — unchanged". I need to include them.
    // I noticed they were missing from the file I viewed (they are probably lower down). I will append them.
    
    fun serializeGrid(grid: Array<Array<GridComponent>>, width: Int, height: Int): String {
        val bytes = mutableListOf<Byte>()
        bytes.add(width.toByte())
        bytes.add(height.toByte())
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type != ComponentType.EMPTY) {
                   bytes.add(x.toByte())
                   bytes.add(y.toByte())
                   val typeIdx = ComponentType.values().indexOf(comp.type)
                   bytes.add((typeIdx shr 8).toByte())
                   bytes.add((typeIdx and 0xFF).toByte())
                   bytes.add(comp.direction.ordinal.toByte())
                }
            }
        }
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun deserializeGrid(data: String, targetWidth: Int, targetHeight: Int): Array<Array<GridComponent>> {
        val grid = Array(targetWidth) { Array(targetHeight) { GridComponent(ComponentType.EMPTY) } }
        try {
            val bytes = Base64.decode(data, Base64.NO_WRAP or Base64.URL_SAFE)
            if (bytes.size >= 2) {
                if (bytes.size % 5 == 2) {
                    val dataPts = (bytes.size - 2) / 5
                    for (i in 0 until dataPts) {
                        val base = 2 + (i * 5)
                        val x = bytes[base].toInt() and 0xFF
                        val y = bytes[base+1].toInt() and 0xFF
                        val typeH = bytes[base+2].toInt() and 0xFF
                        val typeL = bytes[base+3].toInt() and 0xFF
                        val typeIdx = (typeH shl 8) or typeL
                        val dirIdx = bytes[base+4].toInt() and 0xFF
                        
                        if (x in 0 until targetWidth && y in 0 until targetHeight) {
                            val types = ComponentType.values()
                            val tType = if (typeIdx in types.indices) types[typeIdx] else ComponentType.EMPTY
                            val dir = if (dirIdx in Direction.values().indices) Direction.values()[dirIdx] else Direction.UP
                            grid[x][y] = GridComponent(type = tType, direction = dir)
                        }
                    }
                }
            }
        } catch(e: Exception) {}
        return grid
    }
}
