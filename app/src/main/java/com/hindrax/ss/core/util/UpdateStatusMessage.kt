package com.hindrax.ss.core.util

object UpdateStatusMessage {
    fun human(status: String): String {
        return when {
            status.contains("SIGNATURE_MISMATCH") ->
                "Conflicto de firma: la app instalada no fue firmada con la clave release actual. Desinstala esa build y luego instala el release."
            status.contains("INSTALL_PERMISSION_REQUIRED") ->
                "Permite instalar apps desconocidas para Hindrax y vuelve a presionar Actualizar."
            status.startsWith("DOWNLOAD_FAILED") ->
                "Fallo la descarga. Revisa internet/espacio y vuelve a intentar."
            status.contains("VERSION_NOT_NEWER") ->
                "El APK descargado no es mas nuevo que la version instalada."
            status.contains("PACKAGE_MISMATCH") ->
                "El APK descargado no pertenece a este paquete de Hindrax."
            else -> status
        }
    }
}
