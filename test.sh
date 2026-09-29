#!/usr/bin/env bash
# Runs the automated tests. Needs a JDK 17+ and a Gson jar (the JSON library Minecraft itself ships;
# Prism keeps one at <Prism data folder>/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar).
#   GSON_JAR=/path/to/gson-2.10.1.jar ./test.sh
# Optional: ARPHEX_JAR=/path/to/ArPhEx-5.0.2-neoforge-1.21.1.jar also checks a real mod is left alone
# by the GeckoLib scan. To check the mixins against the real mods, see tools/verify_targets.py.
set -euo pipefail
cd "$(dirname "$0")"
: "${GSON_JAR:?Set GSON_JAR to the path of a Gson 2.10+ jar (or classes folder)}"
unset JAVA_TOOL_OPTIONS

./build.sh > /dev/null
rm -rf build/test && mkdir -p build/test
# Test doubles (src/test/java) replace the compile-time stubs for the classes they cover.
javac --release 17 -encoding UTF-8 -cp "build/stubs:build/classes:$GSON_JAR" -d build/test $(find src/test/java -name '*.java' | sort)
CP="build/test:build/classes:$GSON_JAR:build/stubs"

echo "== GeckoLib folder scan"
java -cp "$CP" io.github.mojolowjo.ninefix.ScanTest

echo; echo "== Logic tests (default settings)"
empty=$(mktemp -d)
java -Dninefix.configDir="$empty" -cp "$CP" io.github.mojolowjo.ninefix.LogicTest build/classes

echo; echo "== Logic tests (some fixes switched off in the settings file)"
cfg=$(mktemp -d)
cat > "$cfg/ninefix-startup.toml" <<'TOML'
#The Obsessed: stop the network flood (server): ...
obsessedNetworkFix = false
#FancyMenu / SpiffyHUD: remember screen lookups: ...
fancymenuScreenCache = false
arphexSphereCache = true
TOML
java -Dninefix.configDir="$cfg" -cp "$CP" io.github.mojolowjo.ninefix.LogicTest build/classes switched-off
rm -rf "$empty" "$cfg"
