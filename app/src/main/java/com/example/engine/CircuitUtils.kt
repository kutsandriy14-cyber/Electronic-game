package com.example.engine

object CircuitUtils {
    fun parseProps(data: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (data.isEmpty()) return map
        val parts = data.split("|")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2) {
                map[kv[0].trim()] = kv[1].trim()
            }
        }
        return map
    }

    fun propFloat(data: Map<String, String>, key: String, default: Float): Float {
        return data[key]?.toFloatOrNull() ?: default
    }
}
