package com.example.functional

import com.example.model.ComponentType
import com.example.engine.CircuitEngine
import com.example.engine.EnergyEngine
import com.example.engine.FluidEngine
import com.example.engine.PhysicsEngine
import com.example.engine.MaterialEngine

/**
 * Definition and physics configuration for fluids and fluid flow simulation.
 */
object Fluid {
    /**
     * Checks if a type behaves as a fluid (e.g. falls and flows sideways).
     */
    fun isFluid(type: ComponentType): Boolean {
        return type == ComponentType.WATER ||
               type == ComponentType.LAVA ||
               type == ComponentType.OIL ||
               type == ComponentType.ACID ||
               type == ComponentType.SLIME ||
               type == ComponentType.GASOLINE ||
               type == ComponentType.LIQUID_NITROGEN ||
               type == ComponentType.STEAM ||
               type == ComponentType.HELIUM ||
               type == ComponentType.HYDROGEN ||
               type == ComponentType.METHANE ||
               type == ComponentType.CARBON_DIOXIDE
    }

    /**
     * Checks if a type can be passed through by other moving/flowing particles.
     */
    fun isPassable(type: ComponentType): Boolean {
        return type == ComponentType.EMPTY ||
               type == ComponentType.WATER ||
               type == ComponentType.LAVA ||
               type == ComponentType.OIL ||
               type == ComponentType.ACID ||
               type == ComponentType.GASOLINE ||
               type == ComponentType.SLIME ||
               type == ComponentType.LIQUID_NITROGEN ||
               type == ComponentType.STEAM ||
               type == ComponentType.FIRE ||
               type == ComponentType.HELIUM ||
               type == ComponentType.HYDROGEN ||
               type == ComponentType.METHANE ||
               type == ComponentType.CARBON_DIOXIDE
    }

    /**
     * Checks if a type acts as a solid retaining wall for fluid containment.
     */
    fun isSolidWall(type: ComponentType): Boolean {
        return type == ComponentType.STONE ||
               type == ComponentType.STEEL ||
               type == ComponentType.COPPER ||
               type == ComponentType.GOLD ||
               type == ComponentType.ALUMINUM ||
               type == ComponentType.PLASTIC ||
               type == ComponentType.CLAY ||
               type == ComponentType.BRICK ||
               type == ComponentType.OBSIDIAN ||
               type == ComponentType.BEDROCK ||
               type == ComponentType.GLASS ||
               type == ComponentType.WOOD
    }

    /**
     * Retrieves the chance of flow propagation per tick due to gravity/viscosity.
     */
    fun getFlowChance(type: ComponentType): Double {
        return when (type) {
            ComponentType.LAVA -> 0.4
            ComponentType.SLIME -> 0.25 // Viscous
            ComponentType.OIL -> 0.7
            else -> 0.95
        }
    }

    /**
     * Determines whether a volatile fluid or gas goes upwards rather than downwards.
     */
    fun goesUp(type: ComponentType): Boolean {
        // FIXED: LIQUID_NITROGEN should fall as it is a liquid, not rise like gas. Only steam/fire rise.
        return type == ComponentType.STEAM || 
               type == ComponentType.FIRE ||
               type == ComponentType.HELIUM ||
               type == ComponentType.HYDROGEN ||
               type == ComponentType.METHANE
    }
}
