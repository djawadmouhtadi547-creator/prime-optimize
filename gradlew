#!/usr/bin/env sh
# Lightweight Gradle 8.7 bootstrap wrapper for environments without a wrapper JAR.
set -eu
GRADLE_VERSION="8.7"
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/prime-wrapper/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME_DIR/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME_DIR")"
  ZIP_FILE="${TMPDIR:-/tmp}/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_FILE" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Error: curl or wget is required to download Gradle ${GRADLE_VERSION}." >&2
    exit 1
  fi
  mkdir -p "$GRADLE_HOME_DIR"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP_FILE" -d "$(dirname "$GRADLE_HOME_DIR")"
    # Distribution extracts directly to the expected gradle-8.7 directory.
  else
    echo "Error: unzip is required to install Gradle." >&2
    exit 1
  fi
fi
exec "$GRADLE_BIN" "$@"
