#!/usr/bin/env bash
# Builds, verifies and (optionally) publishes a release. See docs/RELEASING.md.
#
#   scripts/release.sh           build + verify into dist/
#   scripts/release.sh --publish also tag, push and create the GitHub Release
set -euo pipefail

cd "$(dirname "$0")/.."

prop() { grep -E "^$1=" gradle.properties | cut -d= -f2-; }
VERSION_NAME="$(prop app.versionName)"
VERSION_CODE="$(prop app.versionCode)"
TAG="v${VERSION_NAME}"
APK="dist/SecretSanta-${VERSION_NAME}.apk"
PUBLISH=false
[[ "${1:-}" == "--publish" ]] && PUBLISH=true

die() { echo "error: $*" >&2; exit 1; }

echo "==> Releasing ${VERSION_NAME} (versionCode ${VERSION_CODE})"

[[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$ ]] \
    || die "app.versionName '$VERSION_NAME' is not MAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]"
grep -q "^## \[${VERSION_NAME}\]" CHANGELOG.md \
    || die "CHANGELOG.md has no '## [${VERSION_NAME}]' section"
if $PUBLISH; then
    [[ -z "$(git status --porcelain)" ]] || die "working tree is not clean; commit first"
    ! git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null || die "tag ${TAG} already exists"
    command -v gh >/dev/null || die "GitHub CLI (gh) is not installed"
fi
grep -q "^secretsanta.storeFile=" "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" 2>/dev/null \
    || die "release signing is not configured (see docs/RELEASING.md)"

echo "==> Tests, lint and release build"
./gradlew --no-configuration-cache clean testDebugUnitTest lintRelease assembleRelease

mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk "$APK"

BUILD_TOOLS="$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/* | sort -V | tail -1)"
"$BUILD_TOOLS/apksigner" verify "$APK" || die "APK signature verification failed"
(cd dist && sha256sum "$(basename "$APK")" > "$(basename "$APK").sha256")
echo "==> Built $APK"
cat "$APK.sha256"

if $PUBLISH; then
    NOTES="$(mktemp)"
    # The changelog section for this version becomes the release notes.
    awk -v v="## [${VERSION_NAME}]" '
        index($0, v) == 1 { on = 1; next }
        on && /^## \[/ { exit }
        on { print }
    ' CHANGELOG.md > "$NOTES"

    PRERELEASE=()
    [[ "$VERSION_NAME" == *-* ]] && PRERELEASE=(--prerelease)

    echo "==> Tagging ${TAG} and publishing the GitHub Release"
    git tag -a "$TAG" -m "Secret Santa ${VERSION_NAME}"
    git push origin "$TAG"
    gh release create "$TAG" "$APK" "$APK.sha256" \
        --title "Secret Santa ${VERSION_NAME}" \
        --notes-file "$NOTES" \
        "${PRERELEASE[@]}"
    rm -f "$NOTES"
fi
