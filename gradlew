#!/usr/bin/env sh
# Small Gradle 8.7 bootstrap wrapper. This repository omits the binary wrapper JAR,
# so it downloads the official distribution and executes Gradle directly.
set -eu
GRADLE_VERSION="8.7"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_ROOT="$GRADLE_USER_HOME/wrapper/dists/prime-optimize/gradle-$GRADLE_VERSION-bin"
GRADLE_BIN="$DIST_ROOT/gradle-$GRADLE_VERSION/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_ROOT"
  ZIP="$DIST_ROOT/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required to bootstrap Gradle." >&2
      exit 1
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$DIST_ROOT"
  else
    echo "unzip is required to bootstrap Gradle." >&2
    exit 1
  fi
fi
exec "$GRADLE_BIN" "$@"
