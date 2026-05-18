# Informe Técnico: Hindrax SS (v1.1.0-CYD-Connect)

Este documento describe la arquitectura, seguridad y capacidades técnicas de la aplicación Hindrax SS, incluyendo la nueva integración de hardware externo.

## 1. Visión General
Hindrax SS es una plataforma Android nativa diseñada para la educación en ciberseguridad, auditoría defensiva y diagnóstico técnico. Su objetivo es proporcionar un entorno controlado y seguro para profesionales y estudiantes, integrando capacidades nativas con la potencia de dispositivos de hardware embebido (CYD/ESP32) y el ecosistema Linux vía Termux.

## 2. Arquitectura del Sistema
La aplicación utiliza una arquitectura reactiva basada en **MVVM** y **Clean Architecture** con Inyección de Dependencias (**Hilt**):

*   **UI (Jetpack Compose):** Interfaz declarativa con Material 3 y estética "Cyber-Lab".
*   **Domain Layer:** Casos de uso para orquestación de tareas de red, OSINT y control de hardware.
*   **Data Layer (Multi-Transport):** Repositorios que gestionan la persistencia (Room) y múltiples canales de comunicación (HTTP, WebSocket, USB Serial, BLE).
*   **Hardware Bridge:** Módulo "CYD Connect" para la interacción con el firmware Bruce (ESP32).

## 3. Capacidades de Auditoría y Hardware

### 3.1 Integración de Hardware (CYD Connect)
*   **Multi-Channel Discovery:** Detección automática de dispositivos en la red local (Subnet Scan) y vía USB OTG (Serial).
*   **Bruce Firmware Orchestration:** Ejecución remota de módulos físicos:
    *   **Sub-GHz (CC1101):** Escaneo y captura de radiofrecuencia.
    *   **2.4GHz (nRF24):** Diagnóstico de redes inalámbricas y dispositivos IoT.
    *   **NFC (PN532):** Lectura y análisis de etiquetas de proximidad.
    *   **Infrared (IR):** Captura y emulación de señales infrarrojas.
*   **Real-time Terminal:** Acceso directo a la shell serie/web del dispositivo con streaming de logs vía WebSocket.
*   **Remote File Manager:** Gestión de archivos en la tarjeta SD del hardware (Upload de scripts/Download de capturas).

### 3.2 Reconocimiento de Red (Network)
*   **Ping & Net Discovery:** Mapeo de hosts activos en LAN.
*   **Port Scanner:** Escaneo TCP asíncrono gestionado por WorkManager.
*   **Banner Grabbing:** Identificación de servicios mediante sockets crudos.

### 3.3 OSINT (Inteligencia Abierta)
*   **Subdomain Enumeration:** Integración con la API de certificados `crt.sh`.
*   **WHOIS/RDAP:** Consulta de datos de registro de activos.
*   **EXIF Extractor:** Análisis de metadatos geográficos y técnicos en imágenes.

## 4. Eje de Seguridad: Safety Gate
Hindrax SS implementa un control de acceso estricto:
1.  **Clasificación de Objetivos:** Validación automática de rangos de IP (Privadas vs Públicas).
2.  **Hardware Trust:** Requerimiento de emparejamiento y registro de dispositivos CYD antes de habilitar funciones de RF.
3.  **Auditoría de Acciones:** Registro de cada comando enviado al hardware en el historial de sesiones.

## 5. Integración con Termux
La aplicación actúa como una "cabeza visual" para el motor Linux:
*   **RUN_COMMAND Bridge:** Ejecución de herramientas complejas (Nmap, Python) de forma transparente.
*   **JSON Result Parser:** Procesamiento de hallazgos devueltos por Termux para su inclusión en reportes.

## 6. Stack Tecnológico
*   **Lenguaje:** Kotlin 2.0 (Coroutines, Flow, Serialization).
*   **DI:** Hilt (Dagger) para gestión de dependencias de red y hardware.
*   **Comunicación Serie:** `usb-serial-for-android` (Controladores CP210x, CH340).
*   **Red:** OkHttp 4.12.0 + WebSocket dinámico.
*   **Persistencia:** Room Database con indexación avanzada.
*   **Reportes:** Motor dinámico en Markdown con soporte de exportación vía SAF.

---
**Desarrollado por:** Arquitecto Senior Hindrax SS
**Fecha de Actualización:** Mayo 2024
