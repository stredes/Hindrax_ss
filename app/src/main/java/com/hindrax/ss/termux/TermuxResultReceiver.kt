package com.hindrax.ss.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hindrax.ss.HindraxApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recibe resultados desde Termux mediante:
 * am broadcast -a com.hindrax.ss.RESULT --el sessionId <ID> --es result_json '<JSON>'
 */
class TermuxResultReceiver : BroadcastReceiver() {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.hindrax.ss.RESULT") {
            val sessionId = intent.getLongExtra("sessionId", -1)
            val jsonResult = intent.getStringExtra("result_json") ?: return

            if (sessionId == -1L) return

            val app = context.applicationContext as HindraxApplication
            val auditRepository = app.auditRepository

            repositoryScope.launch {
                val resultEntity = TermuxResultParser.parseJsonResult(sessionId, jsonResult)
                if (resultEntity != null) {
                    auditRepository.saveResult(resultEntity)
                    Log.d("TermuxReceiver", "Resultado guardado para la sesión $sessionId")
                }
            }
        }
    }
}
