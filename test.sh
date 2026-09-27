#!/usr/bin/env bash
# Runs the automated tests for the GeckoLib folder scan.
# Needs a JDK 17+ and a Gson jar (the JSON library Minecraft itself ships). Prism keeps one at
#   <Prism data folder>/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
# Usage:  GSON_JAR=/path/to/gson-2.10.1.jar ./test.sh
# Optional: ARPHEX_JAR=/path/to/ArPhEx-5.0.2-neoforge-1.21.1.jar also checks a real mod is left alone.
set -euo pipefail
cd "$(dirname "$0")"
: "${GSON_JAR:?Set GSON_JAR to the path of a Gson 2.10+ jar (or classes folder)}"

./build.sh > /dev/null
rm -rf build/test && mkdir -p build/test
javac --release 17 -encoding UTF-8 -cp "build/stubs:build/classes:$GSON_JAR" -d build/test $(find src/test/java -name '*.java' | sort)
# Test doubles come first on the classpath, stubs last (only the SLF4J Logger interface is used from them).
java -cp "build/test:build/classes:$GSON_JAR:build/stubs" io.github.mojolowjo.ninefix.ScanTest
