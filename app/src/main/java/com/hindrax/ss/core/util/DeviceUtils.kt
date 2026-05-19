package com.hindrax.ss.core.util

import android.os.Build

object DeviceUtils {
    /**
     * Detecta si el dispositivo es una Samsung Galaxy Tab A 8.0
     * Modelos comunes: SM-T290 (Wi-Fi), SM-T295 (LTE), SM-P200, SM-P205
     */
    fun isSamsungTabA8(model: String = Build.MODEL): Boolean {
        return model.contains("SM-T29", ignoreCase = true) || 
               model.contains("SM-P20", ignoreCase = true) ||
               model.contains("GT-P35", ignoreCase = true)
    }
}
