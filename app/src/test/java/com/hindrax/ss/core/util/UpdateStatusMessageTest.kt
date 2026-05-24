package com.hindrax.ss.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStatusMessageTest {
    @Test
    fun signatureMismatchExplainsDebugReleaseConflict() {
        val message = UpdateStatusMessage.human(
            "APK_PREFLIGHT_ERROR: SIGNATURE_MISMATCH uninstall_debug_build_then_install_release"
        )

        assertEquals(
            "Conflicto de firma: desinstala la build instalada desde Android Studio/debug y luego instala el release.",
            message
        )
    }
}
