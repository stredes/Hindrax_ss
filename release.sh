#!/bin/bash
set -euo pipefail

# Hindrax SS - release protocol
# Bumps Android version, validates the project, pushes a Git tag, and lets
# GitHub Actions compile/upload the APK to the GitHub Release.

TOKEN_FILE="github.token"
GRADLE_FILE="app/build.gradle.kts"
MESSAGE="${1:-Update node core, security optimizations and mission sync}"

if [ ! -f "$TOKEN_FILE" ]; then
    echo "ERROR: $TOKEN_FILE not found."
    echo "Create it with a GitHub token that can push tags/releases."
    exit 1
fi

TOKEN=$(tr -d '\r\n ' < "$TOKEN_FILE")
OLD_REMOTE=$(git remote get-url origin)
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
REPO_PATH=$(echo "$OLD_REMOTE" | sed 's|.*github.com[:/]||' | sed 's|.git$||')
HAS_WORKTREE_CHANGES=false
if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
    HAS_WORKTREE_CHANGES=true
fi

restore_remote() {
    git remote set-url origin "$OLD_REMOTE" >/dev/null 2>&1 || true
}
trap restore_remote EXIT

echo "[1/7] Reading current Android version..."
if [ "$HAS_WORKTREE_CHANGES" = true ]; then
    echo "      Pending changes detected; they will be included in this release commit."
fi
OLD_CODE=$(grep "versionCode =" "$GRADLE_FILE" | awk '{print $3}')
OLD_NAME=$(grep "versionName =" "$GRADLE_FILE" | cut -d'"' -f2)

if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
    echo "ERROR: could not read versionCode/versionName from $GRADLE_FILE"
    exit 1
fi

NEW_CODE=$((OLD_CODE + 1))
NEW_NAME=$(echo "$OLD_NAME" | awk -F. '{$NF = $NF + 1;} 1' OFS=.)
TAG="v$NEW_NAME"

echo "[2/7] Bumping v$OLD_NAME ($OLD_CODE) -> $TAG ($NEW_CODE)..."
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"

echo "[3/7] Running verification protocol..."
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}" ./gradlew testDebugUnitTest assembleRelease

echo "[4/7] Preparing git commit and tag..."
git add -A
git commit -m "Automated Release $TAG (Build $NEW_CODE): $MESSAGE"
git tag -d "$TAG" 2>/dev/null || true
git tag -a "$TAG" -m "Hindrax SS $TAG"

echo "[5/7] Configuring authenticated remote..."
git remote set-url origin "https://$TOKEN@github.com/$REPO_PATH.git"

echo "[6/7] Pushing branch and release tag..."
git push origin "$CURRENT_BRANCH"
git push origin "$TAG"

echo "[7/7] Release tag pushed."
echo "GitHub Actions will now:"
echo "- run unit tests"
echo "- compile the APK"
echo "- attach hindrax-$TAG.apk to the GitHub Release"
echo
echo "The app updater will unlock the yellow ACTUALIZAR button after GitHub publishes the APK."
