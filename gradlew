#!/usr/bin/env sh
# Placeholder gradlew - scaffold only. Does NOT include gradle-wrapper.jar.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$SCRIPT_DIR/gradlew" ]; then
  "$SCRIPT_DIR/gradlew" "$@"
  exit $?
fi
if command -v gradle >/dev/null 2>&1; then
  echo "Gradle found on PATH. Generating wrapper..."
  gradle wrapper
  echo "Run $SCRIPT_DIR/gradlew build"
  exit 0
else
  echo "Gradle is not installed on this machine."
  echo "Please either:"
  echo "  1) Install Gradle and run: gradle wrapper"
  echo "  2) Run 'gradle wrapper' on another machine and copy gradle/wrapper/gradle-wrapper.jar into this project."
  echo "See BUILD_INSTRUCTIONS.md for details."
  exit 1
fi