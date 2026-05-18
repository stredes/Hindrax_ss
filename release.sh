#!/bin/bash
# Hindrax SS - Sistema de Autoprogresión TOTAL CI/CD (SEGURO)
TOKEN_FILE="github.token"
GRADLE_FILE="app/build.gradle.kts"

# 0. Validar existencia del token local
if [ ! -f "$TOKEN_FILE" ]; then
    echo "❌ Error: Archivo $TOKEN_FILE no encontrado."
    echo "Por seguridad, ejecuta primero: echo 'TU_TOKEN' > github.token"
    exit 1
fi

TOKEN=$(cat "$TOKEN_FILE" | tr -d '\r\n ')
# Mensaje automático si no se provee uno por comando
MESSAGE="Update node core, security optimizations and mission sync"

echo "[*] 🤖 Iniciando ciclo de auto-release..."

# 1. Obtener versión actual de Gradle
OLD_CODE=$(grep "versionCode =" $GRADLE_FILE | awk '{print $3}')
OLD_NAME=$(grep "versionName =" $GRADLE_FILE | cut -d'"' -f2)

if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
    echo "❌ Error: No se pudo leer la versión en $GRADLE_FILE"
    exit 1
fi

# 2. Calcular nueva versión (Autoprogresión)
# Incrementa el código en 1 y la versión en 0.1
NEW_CODE=$((OLD_CODE + 1))
NEW_NAME=$(echo $OLD_NAME | awk -F. '{$NF = $NF + 1;} 1' OFS=.)

echo "[*] 📈 Evolución detectada: v$OLD_NAME ($OLD_CODE) -> v$NEW_NAME ($NEW_CODE)"

# 3. Aplicar cambios en build.gradle.kts
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" $GRADLE_FILE
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" $GRADLE_FILE

# 4. Configurar autorización segura con GitHub
echo "[*] 🔐 Validando credenciales..."
# Extraer el path del repositorio (stredes/Hindrax_ss)
REPO_PATH=$(git remote get-url origin | sed 's|.*github.com/||' | sed 's|.git||')
git remote set-url origin "https://$TOKEN@github.com/$REPO_PATH.git"

# 5. Git Push & Tag
echo "[*] 📦 Sincronizando repositorio..."
git add .
git commit -m "Automated Release v$NEW_NAME (Build $NEW_CODE): $MESSAGE"
git tag -a "v$NEW_NAME" -m "Auto-generated release for Hindrax Node Sync"

echo "[*] 🚀 Subiendo al servidor principal..."
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
git push origin "$CURRENT_BRANCH"
git push origin "v$NEW_NAME"

echo "--------------------------------------------------------"
echo "✅ [SUCCESS] Versión $NEW_NAME desplegada correctamente."
echo "⚙️  GitHub Actions está compilando tu nueva APK."
echo "📲 Tus nodos detectarán v$NEW_NAME y se actualizarán solos."
echo "--------------------------------------------------------"
