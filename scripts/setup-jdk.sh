#!/usr/bin/env bash
set -euo pipefail

JDK_VERSION="21.0.2+13"
JDK_ARCHIVE="OpenJDK21U-jdk_x64_linux_hotspot_21.0.2_13.tar.gz"
DOWNLOAD_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.2%2B13/${JDK_ARCHIVE}"
JDK_DIR=".jdks/jdk-${JDK_VERSION}"

if [[ -d "${JDK_DIR}" ]]; then
  echo "JDK already present at ${JDK_DIR}"
  exit 0
fi

mkdir -p .jdks
cd .jdks

if [[ ! -f "${JDK_ARCHIVE}" ]]; then
  echo "Downloading Temurin JDK ${JDK_VERSION}..."
  curl -L -o "${JDK_ARCHIVE}" "${DOWNLOAD_URL}"
fi

echo "Extracting ${JDK_ARCHIVE}..."
tar -xzf "${JDK_ARCHIVE}"
rm -f "${JDK_ARCHIVE}"

echo "JDK ready in ${JDK_DIR}. Use it by running:\n"
echo "    export JAVA_HOME=\"$(pwd)/jdk-${JDK_VERSION}\""
echo "    export PATH=\"$JAVA_HOME/bin:\$PATH\""
