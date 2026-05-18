package com.hindrax.ss.termux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object TermuxBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun executeScript(context: Context, scriptName: String, arguments: Array<String>) {
        if (!isTermuxInstalled(context)) {
            Log.e("TermuxBridge", "Termux is not installed")
            return
        }

        val scriptPath = "/data/data/com.termux/files/home/.hindrax_ss/scripts/$scriptName"
        
        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(TERMUX_PACKAGE, "$TERMUX_PACKAGE.app.RunCommandService")
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_PATH", scriptPath)
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_ARGUMENTS", arguments)
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home/.hindrax_ss")
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_BACKGROUND", true)
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("TermuxBridge", "Failed to start Termux service: ${e.message}")
        }
    }
}
