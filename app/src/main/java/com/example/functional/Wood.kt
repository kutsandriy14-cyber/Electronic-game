package com.example.functional

import com.example.model.ComponentType

object Wood {
    fun isWood(type: ComponentType): Boolean = type == ComponentType.WOOD
    
    fun isFlammable(): Boolean = true
}
