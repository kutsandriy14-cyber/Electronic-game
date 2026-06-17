package com.example.functional

import com.example.model.ComponentType

/**
 * An exit node of a space-time bridge where teleprinted elements are discharged.
 */
object PortalOut {
    fun isPortalOut(type: ComponentType): Boolean = type == ComponentType.PORTAL_OUT
}
