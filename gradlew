#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="9.5.0"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_USER_HOME/sample-pack-creator/gradle-$GRADLE_VERSION"
GRADLE_BIN="$INSTALL_DIR/gradle-$GRADLE_VERSION/bin/gradle"
ARCHIVE="$INSTALL_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$INSTALL_DIR"
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 "$URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$ARCHIVE" "$URL"
    else
        echo "É necessário curl ou wget para descarregar o Gradle." >&2
        exit 1
    fi
    if ! command -v unzip >/dev/null 2>&1; then
        echo "É necessário unzip para instalar o Gradle." >&2
        exit 1
    fi
    unzip -q -o "$ARCHIVE" -d "$INSTALL_DIR"
    rm -f "$ARCHIVE"
fi

exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
