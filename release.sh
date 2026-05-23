#!/usr/bin/env bash
set -euo pipefail

# Hindrax SS release protocol
# - Bumps Android versionCode/versionName.
# - Runs fast local verification by default.
# - Pushes branch + tag.
# - Creates/updates the GitHub Release immediately.
# - Lets GitHub Actions build and upload hindrax-vX.Y.apk.

TOKEN_FILE="${TOKEN_FILE:-github.token}"
GRADLE_FILE="${GRADLE_FILE:-app/build.gradle.kts}"
MESSAGE="${1:-Update Hindrax core and release APK}"
VERIFY_MODE="${VERIFY_MODE:-unit}" # unit | full | skip
WAIT_FOR_RELEASE_ASSET="${WAIT_FOR_RELEASE_ASSET:-true}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-600}"
WAIT_INTERVAL_SECONDS="${WAIT_INTERVAL_SECONDS:-10}"

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

echo "[3/8] Running local verification: $VERIFY_MODE..."
case "$VERIFY_MODE" in
    unit)
        JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew --build-cache --parallel testDebugUnitTest
        ;;
    full)
        JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew --build-cache --parallel testDebugUnitTest assembleRelease
        ;;
    skip)
        echo "      Local verification skipped. GitHub Actions will still build the release."
        ;;
    *)
        echo "ERROR: VERIFY_MODE must be one of: unit, full, skip"
        exit 1
        ;;
esac

echo "[4/8] Creating release commit and tag..."
git add -A
git commit -m "release: $TAG build $NEW_CODE - $MESSAGE"
git tag -a "$TAG" -m "Hindrax SS $TAG"

echo "[5/8] Pushing branch and tag..."
git remote set-url origin "https://$TOKEN@github.com/$REPO_PATH.git"
git push origin "$CURRENT_BRANCH" "$TAG"

echo "[6/8] Creating/updating GitHub Release shell..."
if command -v gh >/dev/null 2>&1; then
    export GH_TOKEN="$TOKEN"
    if gh release view "$TAG" --repo "$REPO_PATH" >/dev/null 2>&1; then
        gh release edit "$TAG" \
            --repo "$REPO_PATH" \
            --title "Release $TAG" \
            --notes "Hindrax SS automated release. GitHub Actions uploads $ASSET_NAME." \
            --latest
    else
        gh release create "$TAG" \
            --repo "$REPO_PATH" \
            --verify-tag \
            --title "Release $TAG" \
            --notes "Hindrax SS automated release. GitHub Actions uploads $ASSET_NAME." \
            --latest
    fi
else
    echo "      gh CLI not installed; GitHub Actions will create the release."
fi

echo "[7/8] Waiting for APK asset: $ASSET_NAME..."
if [ "$WAIT_FOR_RELEASE_ASSET" = "true" ] && command -v gh >/dev/null 2>&1; then
    elapsed=0
    while [ "$elapsed" -lt "$WAIT_TIMEOUT_SECONDS" ]; do
        if gh release view "$TAG" --repo "$REPO_PATH" --json assets \
            | grep -q "$ASSET_NAME"; then
            echo "      APK asset is available."
            break
        fi
        sleep "$WAIT_INTERVAL_SECONDS"
        elapsed=$((elapsed + WAIT_INTERVAL_SECONDS))
        echo "      still waiting... ${elapsed}s"
    done

    if [ "$elapsed" -ge "$WAIT_TIMEOUT_SECONDS" ]; then
        echo "      Timeout waiting for APK. Check GitHub Actions for $TAG."
    fi
else
    echo "      Asset wait disabled or gh CLI unavailable."
fi

echo "[8/8] Release protocol finished."
echo "Release: https://github.com/$REPO_PATH/releases/tag/$TAG"
echo "Expected APK: $ASSET_NAME"
echo
echo "Fast modes:"
echo "- VERIFY_MODE=unit ./release.sh \"message\"      # default"
echo "- VERIFY_MODE=full ./release.sh \"message\"      # local tests + release build"
echo "- WAIT_FOR_RELEASE_ASSET=false ./release.sh     # return immediately after push"
