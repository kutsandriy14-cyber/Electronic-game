package com.example.engine

import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.functional.*

object PhysicsEngine {
    // FIXED: BUG 3.1 (static material), BUG 3.2 (Math.random), BUG 3.3 (uranium temp & logic),
    //        BUG 3.4 (void hole bedrock), BUG 3.5 (conveyor vertical), BUG 3.6 (piston retract),
    //        BUG 3.7 (magic dust), BUG 3.8 (slime viscosity) BUG 3.9 (pipe comment mismatch)
    // ADDED: FEATURE 6.5 (motor/fan/pump), FEATURE 6.6 (heater temp), FEATURE 6.7 (peltier module)
    // QUALITY 7.1, 7.3, 7.4 (Double buffering for grid mutations)

    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()
    private fun randFloat() = rng.get()!!.nextFloat()

    private fun isPassable(type: ComponentType): Boolean {
        return Fluid.isPassable(type)
    }

    private fun isMobileParticle(comp: GridComponent): Boolean {
        if (comp.type == ComponentType.URANIUM && comp.temperature > 1000f) {
            return true
        }
        return FunctionalEngine.isMobileParticle(comp.type)
    }

    private fun canSqueeze(grid: Array<Array<GridComponent>>, x1: Int, y1: Int, x2: Int, y2: Int, width: Int, height: Int): Boolean {
        if (x1 == x2 || y1 == y2) return true
        val s1 = x2 in 0 until width && !isPassable(grid[x2][y1].type)
        val s2 = y2 in 0 until height && !isPassable(grid[x1][y2].type)
        return !(s1 && s2)
    }

    fun simulateMaterials(grid: Array<Array<GridComponent>>, width: Int, height: Int, voltage: Float, ramGb: Int = 4) {
        val moved = Array(width) { BooleanArray(height) }
        val prevGrid = Array(width) { x -> grid[x].copyOf() }

        // Recalculate fluid network pressures, flow direction pipe leaks, and pressure sensors
        FluidEngine.calculatePressureAndLeaks(grid, width, height, moved)

        val dxs4 = intArrayOf(-1, 1, 0, 0)
        val dys4 = intArrayOf(0, 0, -1, 1)

        var particlesSimulated = 0
        val maxParticles = when {
            ramGb <= 1 -> 32
            ramGb <= 2 -> 96
            ramGb <= 4 -> 256
            ramGb <= 8 -> 800
            else -> 99999
        }

        // Movement Phase
        for (y in height - 1 downTo 0) {
            val goRight = rand() < 0.5
            val startX = if (goRight) 0 else width - 1
            val endX = if (goRight) width - 1 else 0
            val stepX = if (goRight) 1 else -1
            
            var x = startX
            while (true) {
                if (!moved[x][y]) {
                    val comp = prevGrid[x][y]
                    
                    if (isMobileParticle(comp)) {
                        particlesSimulated++
                        if (particlesSimulated > maxParticles) {
                            // Throttled by simulated device memory limit
                            x += stepX
                            if (x == endX + stepX) break
                            continue
                        }
                        var newX = x
                        var newY = y
                        var didMove = false
                        
                        val goesUp = Fluid.goesUp(comp.type)
                        val dirY = if (goesUp) -1 else 1
                        val flowChance = if (comp.type == ComponentType.URANIUM) 0.5 else Fluid.getFlowChance(comp.type).toDouble()

                        var testY = y + dirY
                        while (testY in 0 until height && 
                               grid[x][testY].type == ComponentType.DOUBLE_DOOR && 
                               grid[x][testY].isPowered) {
                            testY += dirY
                        }
                        if (testY in 0 until height && grid[x][testY].type == ComponentType.EMPTY) {
                            newY = testY
                            didMove = true
                        } else if (y + dirY in 0 until height) {
                            val isFluid = Fluid.isFluid(comp.type) || (comp.type == ComponentType.URANIUM && comp.temperature > 1000f)
                            val isPowder = comp.type == ComponentType.SAND || comp.type == ComponentType.DIRT || comp.type == ComponentType.MAGIC_DUST || comp.type == ComponentType.ICE
                            
                            val blockBelow = grid[x][y + dirY].type
                            val belowIsFluid = Fluid.isFluid(blockBelow)
                            
                            if (isPowder && belowIsFluid) {
                                grid[x][y] = grid[x][y + dirY]
                                grid[x][y + dirY] = comp
                                moved[x][y + dirY] = true
                                moved[x][y] = true
                            } else if (isFluid && rand() < flowChance) {
                                val goLeftFirst = rand() < 0.5
                                val dir1 = if (goLeftFirst) -1 else 1
                                val dir2 = if (goLeftFirst) 1 else -1
                                var diagonalMoved = false
                                
                                // Try diagonal-down first to roll off slopes naturally and realistically
                                if (x + dir1 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir1][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir1, y + dirY, width, height)) {
                                    newX = x + dir1
                                    newY = y + dirY
                                    didMove = true
                                    diagonalMoved = true
                                } else if (x + dir2 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir2][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir2, y + dirY, width, height)) {
                                    newX = x + dir2
                                    newY = y + dirY
                                    didMove = true
                                    diagonalMoved = true
                                }
                                
                                // If diagonal-down is blocked, spread flat horizontally
                                if (!diagonalMoved) {
                                    var testX2 = x + dir1
                                    while (testX2 in 0 until width && 
                                           grid[testX2][y].type == ComponentType.DOUBLE_DOOR && 
                                           grid[testX2][y].isPowered) {
                                        testX2 += dir1
                                    }
                                    if (testX2 in 0 until width && grid[testX2][y].type == ComponentType.EMPTY) {
                                        newX = testX2
                                        didMove = true
                                    } else {
                                        var testX3 = x + dir2
                                        while (testX3 in 0 until width && 
                                               grid[testX3][y].type == ComponentType.DOUBLE_DOOR && 
                                               grid[testX3][y].isPowered) {
                                            testX3 += dir2
                                        }
                                        if (testX3 in 0 until width && grid[testX3][y].type == ComponentType.EMPTY) {
                                            newX = testX3
                                            didMove = true
                                        }
                                    }
                                }
                            } else if (isPowder) {
                                val goLeftFirst = rand() < 0.5
                                val dir1 = if (goLeftFirst) -1 else 1
                                val dir2 = if (goLeftFirst) 1 else -1
                                if (x + dir1 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir1][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir1, y + dirY, width, height)) {
                                    newX = x + dir1
                                    newY = y + dirY
                                    didMove = true
                                } else if (x + dir2 in 0 until width && y + dirY in 0 until height && 
                                    grid[x + dir2][y + dirY].type == ComponentType.EMPTY &&
                                    canSqueeze(grid, x, y, x + dir2, y + dirY, width, height)) {
                                    newX = x + dir2
                                    newY = y + dirY
                                    didMove = true
                                }
                            }
                        }
                        
                        if (didMove) {
                            grid[newX][newY] = comp
                            grid[x][y] = GridComponent(ComponentType.EMPTY)
                            moved[newX][newY] = true
                        }
                    } else {
                        val infiniteSpawnType = when (comp.type) {
                            ComponentType.INFINITE_WATER -> ComponentType.WATER
                            ComponentType.INFINITE_LAVA -> ComponentType.LAVA
                            ComponentType.INFINITE_OIL -> ComponentType.OIL
                            ComponentType.INFINITE_ACID -> ComponentType.ACID
                            ComponentType.INFINITE_SLIME -> ComponentType.SLIME
                            ComponentType.INFINITE_GASOLINE -> ComponentType.GASOLINE
                            ComponentType.INFINITE_LIQUID_NITROGEN -> ComponentType.LIQUID_NITROGEN
                            ComponentType.INFINITE_STEAM -> ComponentType.STEAM
                            ComponentType.INFINITE_PLASMA -> ComponentType.PLASMA
                            else -> null
                        }
                        if (infiniteSpawnType != null) {
                            val spawnDirY = if (Fluid.goesUp(infiniteSpawnType)) -1 else 1
                            if (y + spawnDirY in 0 until height && grid[x][y + spawnDirY].type == ComponentType.EMPTY) {
                                val spawnTemp = when (infiniteSpawnType) {
                                    ComponentType.STEAM -> 150f
                                    ComponentType.LAVA -> 1200f
                                    ComponentType.LIQUID_NITROGEN -> -196f
                                    ComponentType.PLASMA -> 3000f
                                    else -> 20f
                                }
                                grid[x][y + spawnDirY] = GridComponent(infiniteSpawnType, temperature = spawnTemp)
                                moved[x][y + spawnDirY] = true
                            }
                        }
                    }
                }
                
                if (x == endX) break
                x += stepX
            }
        }

        // Feature / Interactions
        for (x in 0 until width) {
            for (y in 0 until height) {
                val comp = grid[x][y]
                if (comp.type == ComponentType.EMPTY) continue

                when(comp.type) {
                    ComponentType.PLASMA -> {
                        Plasma.interactWithSurroundings(grid, x, y, width, height)
                    }
                    ComponentType.BLACK_HOLE -> {
                        BlackHole.applyGravity(grid, x, y, width, height)
                    }
                    ComponentType.PORTAL_IN -> {
                        PortalIn.handleTeleportation(grid, x, y, width, height)
                    }
                    ComponentType.PORTAL_OUT -> {
                        // Handled by PortalIn
                    }
                    ComponentType.TESLA_COIL -> {
                        TeslaCoil.transmitWirelessPower(grid, x, y, width, height)
                    }
                    ComponentType.MERCURY -> {
                        Mercury.simulate(grid, x, y, width, height, moved)
                    }
                    ComponentType.LIGHTNING_ROD -> {
                        LightningRod.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.STIRLING_ENGINE -> {
                        grid[x][y] = StirlingEngine.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.QUANTUM_SUPERCONDUCTOR -> {
                        QuantumSuperconductor.conduct(grid, x, y, comp, width, height)
                    }
                    ComponentType.PCM_CELL -> {
                        grid[x][y] = PcmCell.bufferTemperature(grid, x, y, comp, width, height)
                    }
                    ComponentType.LASER_RECEIVER -> {
                        grid[x][y] = LaserReceiver.processLaserDetection(grid, x, y, comp, width, height)
                    }
                    ComponentType.GRAPHITE_ROD -> {
                        grid[x][y] = GraphiteRod.dampenNuclearHeat(grid, x, y, comp, width, height)
                    }
                    ComponentType.PIEZO_SENSOR -> {
                        grid[x][y] = PiezoSensor.processPressure(grid, x, y, comp, width, height)
                    }
                    ComponentType.WATER, ComponentType.INFINITE_WATER -> {
                        Water.interact(grid, x, y, width, height)
                    }
                    ComponentType.FIRE -> {
                        Fire.simulate(grid, x, y, width, height)
                    }
                    ComponentType.STEAM -> {
                        Steam.simulate(grid, x, y)
                    }
                    ComponentType.HELIUM, ComponentType.HYDROGEN, ComponentType.METHANE, ComponentType.CARBON_DIOXIDE -> {
                        Gas.simulate(grid, x, y, width, height)
                    }
                    ComponentType.LIQUID_NITROGEN -> {
                        LiquidNitrogen.simulate(grid, x, y)
                    }
                    ComponentType.ACID -> {
                        Acid.simulate(grid, x, y, width, height)
                    }
                    ComponentType.MAGIC_DUST -> {
                        MagicDust.simulate(grid, x, y, width, height)
                    }
                    ComponentType.VOID_HOLE, ComponentType.FLUID_DRAIN -> {
                        VoidHole.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.HEATER -> {
                        Heater.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.COOLER -> {
                        Cooler.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.PELTIER_MODULE -> {
                        grid[x][y] = Peltier.simulate(grid, x, y, comp, width, height)
                    }
                    ComponentType.MAGNET -> {
                        Magnet.simulate(grid, x, y, comp, width, height, moved)
                    }
                    ComponentType.CONVEYOR_BELT -> {
                        FunctionalEngine.simulateConveyorBelt(grid, x, y, comp, width, height, moved)
                    }
                    ComponentType.PISTON -> {
                        FunctionalEngine.simulatePiston(grid, x, y, comp, width, height)
                    }
                    ComponentType.URANIUM -> {
                        FunctionalEngine.simulateUranium(grid, x, y, comp, width, height, moved)
                    }
                    ComponentType.WATER_PUMP -> {
                        FunctionalEngine.simulateWaterPump(grid, x, y, comp, width, height)
                    }
                    ComponentType.FAN -> {
                        FunctionalEngine.simulateFan(grid, x, y, comp, width, height)
                    }
                    ComponentType.MOTOR -> {
                        FunctionalEngine.simulateMotor(grid, x, y, comp, width, height)
                    }
                    else -> {
                        MaterialEngine.simulate(grid, x, y, comp, width, height, moved)
                    }
                }
            }
        }
    }
}
