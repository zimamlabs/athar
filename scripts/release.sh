#!/usr/bin/env bash
# Build the release files, name them, write a checksum next to each, and optionally publish.
#
#   scripts/release.sh            build into dist/ (signed Android APKs need the keystore variables below)
#   scripts/release.sh --publish  also create the GitHub release for the version in composeApp/build.gradle.kts
#
# Signing: ATHAR_KEYSTORE (path), ATHAR_KEYSTORE_PASSWORD, optional ATHAR_KEY_ALIAS.
# Publishing needs the GitHub CLI, logged in with a token that can write to this repository.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' composeApp/build.gradle.kts | head -1)"
: "${VERSION:?could not read versionName}"
[ -n "${ATHAR_KEYSTORE:-}" ] || echo "note: ATHAR_KEYSTORE is not set, the APKs will be unsigned" >&2

./gradlew --no-daemon :shared:desktopTest :feature-notes:desktopTest :feature-journal:desktopTest :feature-clipboard:desktopTest :composeApp:assembleRelease

OUT="dist/$VERSION"
rm -rf "$OUT"; mkdir -p "$OUT"
for abi in arm64-v8a armeabi-v7a x86_64; do
  cp "composeApp/build/outputs/apk/release/composeApp-$abi-release.apk" "$OUT/Athar_${VERSION}_Android_$abi.apk" 2>/dev/null || true
done

# One checksum file per release file, named after it, so `sha256sum -c <file>.sha256` works.
(cd "$OUT" && for f in Athar_*; do case "$f" in *.sha256) ;; *) sha256sum -- "$f" > "$f.sha256" ;; esac; done)
ls -l "$OUT"

if [ "${1:-}" = "--publish" ]; then
  NOTES="$(awk -v v="## $VERSION" '$0==v{p=1;next} /^## /{p=0} p' CHANGELOG.md)"
  gh release create "v$VERSION" "$OUT"/* --title "Athar $VERSION" --notes "${NOTES:-Athar $VERSION}"
fi
