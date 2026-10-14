#!/usr/bin/env bash
# Fails when a feature module reaches into another feature, or when :core or :ui depend on a feature.
# Features may depend on :core and :ui only; the app (:composeApp) and :shared wire them together.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
bad=0

for dir in feature-*/; do
  name="${dir%/}"
  other_deps="$(grep -E 'project\(":feature-' "$name/build.gradle.kts" | grep -v "\":$name\"" || true)"
  if [ -n "$other_deps" ]; then echo "$name depends on another feature in its build file:"; echo "$other_deps"; bad=1; fi
  pkg="${name#feature-}"
  for other in feature-*/; do
    other_pkg="${other%/}"; other_pkg="${other_pkg#feature-}"
    [ "$other_pkg" = "$pkg" ] && continue
    hits="$(grep -rnE "^import dev\.athar\.$other_pkg[.;]|dev\.athar\.$other_pkg\.[A-Za-z]" "$name/src" --include='*.kt' || true)"
    if [ -n "$hits" ]; then echo "$name uses the $other_pkg feature in source:"; echo "$hits"; bad=1; fi
  done
done

for base in core ui; do
  if grep -qE 'project\(":(feature-|shared|composeApp)' "$base/build.gradle.kts"; then
    echo ":$base must not depend on a feature, :shared or :composeApp"; bad=1
  fi
done

if [ "$bad" -ne 0 ]; then echo "module rules broken"; exit 1; fi
echo "module rules ok"
