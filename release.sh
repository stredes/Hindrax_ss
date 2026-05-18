#!/bin/bash

# Configuración
VERSION=$1
MESSAGE=$2

if [ -z "$VERSION" ] || [ -z "$MESSAGE" ]; then
    echo "Uso: ./release.sh [version] [mensaje]"
    echo "Ejemplo: ./release.sh 1.0.6 'Mejora en sistema de emparejamiento'"
    exit 1
fi

echo "[*] Preparando release v$VERSION..."

# 1. Añadir y comprometer cambios
git add .
git commit -m "Release v$VERSION: $MESSAGE"

# 2. Crear etiqueta de versión
git tag -a "v$VERSION" -m "Version $VERSION"

# 3. Empujar a GitHub (esto dispara el GitHub Action)
git push origin main
git push origin "v$VERSION"

echo "[+] Cambios subidos. GitHub Actions compilará la APK y creará el release automáticamente."
