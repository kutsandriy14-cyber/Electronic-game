package com.example.functional

import com.example.model.ComponentType
import com.example.model.GridComponent

/**
 * PcmCell (Phase Change Material Cell) liquefies or solidifies to maintain a balanced temperature,
 * absorbing huge amounts of thermal energy without letting the temperature of adjacent blocks rise
 * or drop past a stable room temp.
 */
object PcmCell {
    fun isPcmCell(type: ComponentType): Boolean = type == ComponentType.PCM_CELL

    fun bufferTemperature(grid: Array<Array<GridComponent>>, x: Int, y: Int, comp: GridComponent, width: Int, height: Int): GridComponent {
        val ambientRating = 20f // Room temperature balance
        val tempDiff = comp.temperature - ambientRating
        
        // Dampen thermal spike! Absorbs or emits thermal loops to restore rating.
        val adjustedTemp = comp.temperature - (tempDiff * 0.15f)
        return comp.copy(temperature = adjustedTemp)
    }
}
