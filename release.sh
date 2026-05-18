#!/bin/bash
# Hindrax SS - Sistema de Autoprogresión TOTAL CI/CD
TOKEN="REDACTED_TOKEN"
GRADLE_FILE="app/build.gradle.kts"

# Definir mensaje automático si no se provee uno por comando
MESSAGE=${1:-"Update node core and security optimizations"}

echo "[*] 🤖 Iniciando ciclo de auto-release..."

# 1. Obtener versión actual de Gradle
OLD_CODE=$(grep "versionCode =" $GRADLE_FILE | awk '{print $3}')
OLD_NAME=$(grep "versionName =" $GRADLE_FILE | cut -d'"' -f2)

if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
    echo "❌ Error: No se pudo leer la versión en $GRADLE_FILE"
    exit 1
fi

# 2. Calcular nueva versión (Autoprogresión)
# Incrementa el código en 1 (Ej: 1 -> 2)
NEW_CODE=$((OLD_CODE + 1))
# Incrementa el último decimal del nombre (Ej: 1.0 -> 1.1)
NEW_NAME=$(echo $OLD_NAME | awk -F. '{$NF = $NF + 1;} 1' OFS=.)

echo "[*] 📈 Evolución detectada: v$OLD_NAME ($OLD_CODE) -> v$NEW_NAME ($NEW_CODE)"

# 3. Aplicar cambios en build.gradle.kts
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" $GRADLE_FILE
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" $GRADLE_FILE

# 4. Configurar autorización con Token en el remoto
echo "[*] 🔐 Validando credenciales con GitHub..."
REPO_PATH=$(git remote get-url origin | sed 's|.*github.com/||' | sed 's|.git||')
git remote set-url origin "https://$TOKEN@github.com/$REPO_PATH.git"

# 5. Git Push & Tag
echo "[*] 📦 Sincronizando repositorio..."
git add .
git commit -m "Automated Release v$NEW_NAME (Build $NEW_CODE): $MESSAGE"
git tag -a "v$NEW_NAME" -m "Auto-generated release: $MESSAGE"

echo "[*] 🚀 Subiendo al servidor principal..."
# Detectar rama actual automáticamente
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
git push origin "$CURRENT_BRANCH"
git push origin "v$NEW_NAME"

echo "--------------------------------------------------------"
echo "✅ [SUCCESS] Versión $NEW_NAME desplegada correctamente."
echo "⚙️  GitHub Actions está compilando tu nueva APK."
echo "📲 La aplicación detectará v$NEW_NAME y pedirá actualizar."
echo "--------------------------------------------------------"
