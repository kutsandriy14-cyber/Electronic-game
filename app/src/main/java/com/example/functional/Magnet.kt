package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Encapsulates magnetic flux and gravity pulling of metals (Steel, Copper, Gold, Aluminum)
 * towards active powered electromagnets.
 */
object Magnet {
    private val rng = ThreadLocal.withInitial { java.util.Random() }
    private fun rand() = rng.get()!!.nextDouble()

    /**
     * Checks if component type is a Magnet.
     */
    fun isMagnet(type: ComponentType): Boolean = type == ComponentType.MAGNET

    /**
     * Determines whether a material is metallic and susceptible to electromagnetic fields.
     */
    fun isMagneticMaterial(type: ComponentType): Boolean {
        return type == ComponentType.STEEL ||
               type == ComponentType.COPPER ||
               type == ComponentType.GOLD ||
               type == ComponentType.ALUMINUM ||
               type == ComponentType.MAGNETIC_CONTACT
    }

    /**
     * Simulates magnetic attraction pull on nearby solid metal components.
     */
    fun simulate(
        grid: Array<Array<GridComponent>>,
        x: Int,
        y: Int,
        comp: GridComponent,
        width: Int,
        height: Int,
        moved: Array<BooleanArray>
    ) {
        if (!comp.isPowered) return

        // 3x3 search area around electromagnet
        for (dx in -3..3) {
            for (dy in -3..3) {
                // Skip self or center
                if (dx == 0 && dy == 0) continue
                
                val tx = x + dx
                val ty = y + dy
                if (tx in 0 until width && ty in 0 until height) {
                    val target = grid[tx][ty]
                    if (isMagneticMaterial(target.type) && !moved[tx][ty]) {
                        // Calculate direction vector towards the magnet
                        val stepX = when {
                            dx > 0 -> -1
                            dx < 0 -> 1
                            else -> 0
                        }
                        val stepY = when {
                            dy > 0 -> -1
                            dy < 0 -> 1
                            else -> 0
                        }

                        val destX = tx + stepX
                        val destY = ty + stepY
                        if (destX in 0 until width && destY in 0 until height) {
                            if (grid[destX][destY].type == ComponentType.EMPTY) {
                                grid[destX][destY] = target
                                grid[tx][ty] = GridComponent(ComponentType.EMPTY)
                                moved[destX][destY] = true
                                moved[tx][ty] = true
                                return // Pull one item per tick to keep simulation balanced
                            }
                        }
                    }
                }
            }
        }
    }
}
