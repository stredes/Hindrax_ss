package com.hindrax.ss.features.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.hindrax.ss.domain.nfc.NfcHceEmulator

class NfcLabHostApduService : HostApduService() {
    private var emulator: NfcHceEmulator? = null

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val current = emulator ?: newEmulator().also { emulator = it }
        return current.process(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        emulator = null
    }

    private fun newEmulator(): NfcHceEmulator {
        return NfcHceEmulator(
            payloadText = NfcLabHceProfileStore.payload(this),
            enabled = NfcLabHceProfileStore.isEnabled(this)
        )
    }
}
