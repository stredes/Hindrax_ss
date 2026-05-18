# Hindrax SS

Hindrax SS es una aplicación Android nativa diseñada para el estudio de pentesting, auditoría defensiva y diagnóstico técnico en entornos autorizados. Funciona como un panel visual que organiza tareas de auditoría, valida objetivos y se integra opcionalmente con Termux para ejecución de scripts avanzados.

## Características (MVP)

*   **Dashboard:** Estado de red, IP local y detección de Termux.
*   **Safety Gate:** Sistema de validación de objetivos para prevenir escaneos no autorizados en redes públicas.
*   **Reconocimiento de Red:** Ping y Escaneo de puertos comunes (22, 80, 443, etc.).
*   **DNS Lookup:** Resolución de registros A para dominios.
*   **Análisis de APK:** Extracción de metadatos (Package Name, Versión) y cálculo de Hash SHA-256.
*   **Análisis Web:** Verificación de cabeceras de seguridad (CSP, HSTS, X-Frame-Options).
*   **OSINT Básico:** Descubrimiento de subdominios y recolección de información pública.
*   **Gestión de Reportes:** Historial de auditorías con exportación en formato Markdown.
*   **Termux Bridge:** Integración mediante `RUN_COMMAND` para ejecutar scripts validados.

## Stack Tecnológico

*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Arquitectura:** MVVM
*   **Base de Datos:** Room
*   **Concurrencia:** Coroutines & Flow
*   **Red:** OkHttp

## Configuración de Termux

Para habilitar las funciones avanzadas, instale Termux y siga estos pasos:

1.  Cree el directorio de scripts:
    ```bash
    mkdir -p ~/.hindrax_ss/scripts
    ```
2.  Otorgue permisos a la aplicación para ejecutar comandos en Termux (ajustes de Android -> Aplicaciones -> Hindrax SS -> Permisos).

## Advertencia Legal

Esta herramienta ha sido creada exclusivamente con fines educativos y para auditorías en entornos donde el usuario tiene autorización explícita. El uso de esta aplicación contra objetivos sin permiso es ilegal y responsabilidad del usuario final.
