#!/usr/bin/env bash
set -euo pipefail

# Hindrax SS release protocol
# - Bumps Android versionCode/versionName.
# - Builds a local APK artifact.
# - Pushes branch + tag.
# - Creates/updates the GitHub Release.
# - Uploads hindrax-vX.Y.apk so the in-app updater can detect it immediately.

TOKEN_FILE="${TOKEN_FILE:-github.token}"
GRADLE_FILE="${GRADLE_FILE:-app/build.gradle.kts}"
MESSAGE="${1:-Update Hindrax core and release APK}"
VERIFY_MODE="${VERIFY_MODE:-unit}" # unit | full | skip
API_HINDRAX_ENV_FILE="${API_HINDRAX_ENV_FILE:-/home/gian/Escritorio/API_HINDRAX/.env}"
API_HINDRAX_RELEASE_BASE_URL="${API_HINDRAX_RELEASE_BASE_URL:-https://api-hindrax.vercel.app}"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERROR: required command not found: $1"
        exit 1
    fi
}

read_gradle_value() {
    local key="$1"
    grep "$key =" "$GRADLE_FILE" | head -1
}

restore_remote() {
    git remote set-url origin "$OLD_REMOTE" >/dev/null 2>&1 || true
}

require_command git
require_command sed
require_command awk
require_command gh
require_command keytool
require_command sha256sum

KEYSTORE_DIR="${KEYSTORE_DIR:-keystore}"
KEYSTORE_FILE="${HINDRAX_KEYSTORE_PATH:-$KEYSTORE_DIR/hindrax-release.jks}"
KEYSTORE_PROPS="${KEYSTORE_PROPS:-$KEYSTORE_DIR/hindrax-release.properties}"

random_secret() {
    local seed
    seed="$(date +%s%N)-$RANDOM-$(hostname)"
    printf '%s' "$seed" | sha256sum | awk '{print $1}'
}

read_property() {
    local key="$1"
    if [ -f "$KEYSTORE_PROPS" ]; then
        grep "^$key=" "$KEYSTORE_PROPS" | tail -1 | cut -d= -f2-
    fi
}

read_env_property() {
    local file="$1"
    local key="$2"
    if [ -f "$file" ]; then
        grep "^$key=" "$file" | tail -1 | cut -d= -f2-
    fi
}

prepare_api_hindrax_release_config() {
    local api_token_from_env_file
    api_token_from_env_file="$(read_env_property "$API_HINDRAX_ENV_FILE" "API_TOKEN")"

    export API_HINDRAX_BASE_URL="${API_HINDRAX_BASE_URL:-$API_HINDRAX_RELEASE_BASE_URL}"
    export API_HINDRAX_TOKEN="${API_HINDRAX_TOKEN:-$api_token_from_env_file}"

    if [ -n "$API_HINDRAX_TOKEN" ]; then
        export API_HINDRAX_ENABLED="${API_HINDRAX_ENABLED:-true}"
        echo "[api] API_HINDRAX enabled for release: $API_HINDRAX_BASE_URL"
    else
        export API_HINDRAX_ENABLED="${API_HINDRAX_ENABLED:-false}"
        echo "[api] WARNING: API_HINDRAX_TOKEN missing. Release APK will not auto-connect to API_HINDRAX."
        echo "[api] Expected token in env API_HINDRAX_TOKEN or $API_HINDRAX_ENV_FILE as API_TOKEN."
    fi
}

prepare_release_signing() {
    if [ -n "${HINDRAX_KEYSTORE_PATH:-}" ] || [ -n "${HINDRAX_KEYSTORE_PASSWORD:-}" ] || [ -n "${HINDRAX_KEY_ALIAS:-}" ] || [ -n "${HINDRAX_KEY_PASSWORD:-}" ]; then
        if [ -z "${HINDRAX_KEYSTORE_PATH:-}" ] || [ -z "${HINDRAX_KEYSTORE_PASSWORD:-}" ] || [ -z "${HINDRAX_KEY_ALIAS:-}" ] || [ -z "${HINDRAX_KEY_PASSWORD:-}" ]; then
            echo "ERROR: incomplete signing env. Set HINDRAX_KEYSTORE_PATH, HINDRAX_KEYSTORE_PASSWORD, HINDRAX_KEY_ALIAS and HINDRAX_KEY_PASSWORD."
            exit 1
        fi
        if [ ! -f "$HINDRAX_KEYSTORE_PATH" ]; then
            echo "ERROR: HINDRAX_KEYSTORE_PATH does not exist: $HINDRAX_KEYSTORE_PATH"
            exit 1
        fi
        echo "[signing] Using release keystore from env: $HINDRAX_KEYSTORE_PATH"
        return
    fi

    mkdir -p "$KEYSTORE_DIR"
    if [ ! -f "$KEYSTORE_PROPS" ]; then
        local store_password key_password key_alias
        store_password="$(random_secret)"
        key_password="$store_password"
        key_alias="hindrax_release"
        cat > "$KEYSTORE_PROPS" <<EOF
storeFile=$KEYSTORE_FILE
storePassword=$store_password
keyAlias=$key_alias
keyPassword=$key_password
EOF
        chmod 600 "$KEYSTORE_PROPS"
    fi

    export HINDRAX_KEYSTORE_PATH
    export HINDRAX_KEYSTORE_PASSWORD
    export HINDRAX_KEY_ALIAS
    export HINDRAX_KEY_PASSWORD
    HINDRAX_KEYSTORE_PATH="$(read_property storeFile)"
    HINDRAX_KEYSTORE_PASSWORD="$(read_property storePassword)"
    HINDRAX_KEY_ALIAS="$(read_property keyAlias)"
    HINDRAX_KEY_PASSWORD="$(read_property keyPassword)"

    if [ -z "$HINDRAX_KEYSTORE_PATH" ] || [ -z "$HINDRAX_KEYSTORE_PASSWORD" ] || [ -z "$HINDRAX_KEY_ALIAS" ] || [ -z "$HINDRAX_KEY_PASSWORD" ]; then
        echo "ERROR: invalid keystore properties at $KEYSTORE_PROPS"
        exit 1
    fi

    if [ ! -f "$HINDRAX_KEYSTORE_PATH" ]; then
        echo "[signing] Creating persistent local release keystore: $HINDRAX_KEYSTORE_PATH"
        keytool -genkeypair \
            -v \
            -keystore "$HINDRAX_KEYSTORE_PATH" \
            -storepass "$HINDRAX_KEYSTORE_PASSWORD" \
            -keypass "$HINDRAX_KEY_PASSWORD" \
            -alias "$HINDRAX_KEY_ALIAS" \
            -keyalg RSA \
            -keysize 4096 \
            -validity 10000 \
            -dname "CN=Hindrax SS, OU=Hindrax, O=Hindrax, L=Santiago, ST=RM, C=CL"
        chmod 600 "$HINDRAX_KEYSTORE_PATH"
    else
        echo "[signing] Using persistent local release keystore: $HINDRAX_KEYSTORE_PATH"
    fi
}

if [ ! -f "$TOKEN_FILE" ]; then
    echo "ERROR: $TOKEN_FILE not found."
    echo "Create it with a GitHub token that can push tags/releases."
    exit 1
fi

if [ ! -f "$GRADLE_FILE" ]; then
    echo "ERROR: $GRADLE_FILE not found."
    exit 1
fi

TOKEN="$(tr -d '\r\n ' < "$TOKEN_FILE")"
if [ -z "$TOKEN" ]; then
    echo "ERROR: $TOKEN_FILE is empty."
    exit 1
fi

OLD_REMOTE="$(git remote get-url origin)"
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
REPO_PATH="$(echo "$OLD_REMOTE" | sed 's|.*github.com[:/]||' | sed 's|.git$||')"
trap restore_remote EXIT

if [ "$CURRENT_BRANCH" = "HEAD" ]; then
    echo "ERROR: detached HEAD. Checkout a branch before releasing."
    exit 1
fi

echo "[1/8] Reading Android version..."
OLD_CODE="$(read_gradle_value "versionCode" | awk '{print $3}')"
OLD_NAME="$(read_gradle_value "versionName" | cut -d'"' -f2)"

if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
    echo "ERROR: could not read versionCode/versionName from $GRADLE_FILE"
    exit 1
fi

NEW_CODE=$((OLD_CODE + 1))
NEW_NAME="$(echo "$OLD_NAME" | awk -F. '{$NF = $NF + 1;} 1' OFS=.)"
TAG="v$NEW_NAME"
ASSET_NAME="hindrax-$TAG.apk"
LOCAL_RELEASE_DIR="build/release-artifacts/$TAG"
LOCAL_APK="$LOCAL_RELEASE_DIR/$ASSET_NAME"
SHA_FILE="$LOCAL_RELEASE_DIR/SHA256SUMS.txt"

if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "ERROR: local tag already exists: $TAG"
    exit 1
fi

if git ls-remote --tags origin "refs/tags/$TAG" | grep -q "$TAG"; then
    echo "ERROR: remote tag already exists: $TAG"
    exit 1
fi

echo "[2/8] Bumping v$OLD_NAME ($OLD_CODE) -> $TAG ($NEW_CODE)..."
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"

prepare_release_signing
prepare_api_hindrax_release_config

echo "[3/8] Running local verification/build: $VERIFY_MODE..."
case "$VERIFY_MODE" in
    unit)
        JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew --build-cache --parallel testDebugUnitTest assembleRelease
        ;;
    full)
        JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew --build-cache --parallel check assembleRelease
        ;;
    skip)
        JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew --build-cache --parallel assembleRelease
        ;;
    *)
        echo "ERROR: VERIFY_MODE must be one of: unit, full, skip"
        exit 1
        ;;
esac

echo "[4/8] Preparing local APK artifact..."
mkdir -p "$LOCAL_RELEASE_DIR"
if [ ! -f app/build/outputs/apk/release/app-release.apk ]; then
    echo "ERROR: release APK was not generated at app/build/outputs/apk/release/app-release.apk"
    exit 1
fi
cp app/build/outputs/apk/release/app-release.apk "$LOCAL_APK"
(cd "$LOCAL_RELEASE_DIR" && sha256sum "$ASSET_NAME" > "$(basename "$SHA_FILE")")
ls -lh "$LOCAL_RELEASE_DIR"

echo "[5/8] Creating release commit and tag..."
git add -A
git commit -m "release: $TAG build $NEW_CODE - $MESSAGE"
git tag -a "$TAG" -m "Hindrax SS $TAG"

echo "[6/8] Pushing repository changes and tag..."
git remote set-url origin "https://$TOKEN@github.com/$REPO_PATH.git"
git push origin "$CURRENT_BRANCH" "$TAG"

echo "[7/8] Creating/updating GitHub Release and uploading APK..."
export GH_TOKEN="$TOKEN"
RELEASE_NOTES="$(cat <<EOF
Hindrax SS automated release.

Protocol:
- Repository changes pushed to $CURRENT_BRANCH.
- Android version bumped to $TAG build $NEW_CODE.
- Local APK uploaded as $ASSET_NAME.
- SHA256SUMS.txt attached for integrity checks.
- In-app updater detects this release through GitHub Releases.
EOF
)"

if gh release view "$TAG" --repo "$REPO_PATH" >/dev/null 2>&1; then
    gh release edit "$TAG" \
        --repo "$REPO_PATH" \
        --title "Release $TAG" \
        --notes "$RELEASE_NOTES" \
        --latest
else
    gh release create "$TAG" \
        --repo "$REPO_PATH" \
        --verify-tag \
        --title "Release $TAG" \
        --notes "$RELEASE_NOTES" \
        --latest
fi

gh release upload "$TAG" \
    "$LOCAL_APK#$ASSET_NAME" \
    "$SHA_FILE#SHA256SUMS.txt" \
    --repo "$REPO_PATH" \
    --clobber

echo "[8/8] Release protocol finished."
echo "Release: https://github.com/$REPO_PATH/releases/tag/$TAG"
echo "APK: $ASSET_NAME"
echo "Local artifact: $LOCAL_APK"
echo
echo "Fast modes:"
echo "- ./release.sh \"message\"                 # tests + release APK + upload"
echo "- VERIFY_MODE=skip ./release.sh \"message\" # release APK + upload, no local tests"
echo "- VERIFY_MODE=full ./release.sh \"message\" # Gradle check + release APK + upload"
