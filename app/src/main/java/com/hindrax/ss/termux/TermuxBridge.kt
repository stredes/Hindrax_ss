package com.hindrax.ss.termux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object TermuxBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"
    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    private const val HINDRAX_WORKDIR = "$TERMUX_HOME/.hindrax_ss"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun executeScript(context: Context, scriptName: String, arguments: Array<String>): Boolean {
        if (!isTermuxInstalled(context)) {
            Log.e("TermuxBridge", "Termux is not installed")
            return false
        }

        val scriptPath = "$HINDRAX_WORKDIR/scripts/$scriptName"
        return executePath(context, scriptPath, arguments, HINDRAX_WORKDIR)
    }

    fun executeCommand(context: Context, command: String, arguments: Array<String> = emptyArray()): Boolean {
        if (!isTermuxInstalled(context)) {
            Log.e("TermuxBridge", "Termux is not installed")
            return false
        }

        if (!isSafeCommandName(command)) {
            Log.e("TermuxBridge", "Unsafe command name rejected: $command")
            return false
        }

        val commandPath = "$TERMUX_PREFIX/bin/$command"
        return executePath(context, commandPath, arguments, HINDRAX_WORKDIR)
    }

    private fun executePath(
        context: Context,
        commandPath: String,
        arguments: Array<String>,
        workdir: String
    ): Boolean {
        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(TERMUX_PACKAGE, "$TERMUX_PACKAGE.app.RunCommandService")
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_PATH", commandPath)
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_ARGUMENTS", arguments)
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_WORKDIR", workdir)
            putExtra("$TERMUX_PACKAGE.RUN_COMMAND_BACKGROUND", true)
        }

        try {
            context.startService(intent)
            return true
        } catch (e: Exception) {
            Log.e("TermuxBridge", "Failed to start Termux service: ${e.message}")
            return false
        }
    }

    private fun isSafeCommandName(command: String): Boolean {
        return command.matches(Regex("[A-Za-z0-9._+-]+"))
    }
}
