#!/usr/bin/env bash
# Builds build/libs/ninefix-<version>.jar using only a JDK (17 or newer).
# No Gradle and no downloads: the external APIs are compile-time stubs (see stubs/README.md).
# The jar is reproducible: the same sources + JDK give a byte-identical jar.
set -euo pipefail
cd "$(dirname "$0")"

VERSION=$(sed -n 's/^version="\(.*\)"$/\1/p' src/main/resources/META-INF/neoforge.mods.toml | head -n 1)
BUILD_DATE="2026-10-03T03:00:00Z"   # fixed timestamp for reproducible jars; bump with each release
JAR="build/libs/ninefix-${VERSION}.jar"

rm -rf build
mkdir -p build/stubs build/classes build/libs

javac --release 17 -encoding UTF-8 -d build/stubs $(find stubs -name '*.java' | sort)
javac --release 17 -encoding UTF-8 -Xlint:all -cp build/stubs -d build/classes $(find src/main/java -name '*.java' | sort)
cp -R src/main/resources/. build/classes/
cp LICENSE build/classes/LICENSE

# Guard: in Mixin 0.8.7 (what NeoForge 21.1 ships) @ModifyArg and @Redirect take ONE @At.
# Storing a list there can crash the game while loading (MixinExtras casts it to a single value),
# so refuse to build if any mixin does that.
for cls in $(cd build/classes && find . -path '*/mixin/*.class' | sed 's#^\./##; s#\.class$##; s#/#.#g'); do
  if javap -v -p -cp build/classes "$cls" \
      | awk '/injection\.(ModifyArg|Redirect)\(/ { f = 1 } f && / at=/ { print; f = 0 }' \
      | grep -q 'at=\[@'; then
    echo "ERROR: $cls: @ModifyArg/@Redirect 'at' must be a single @At for Mixin 0.8.7" >&2
    exit 1
  fi
done

cat > build/MANIFEST.MF <<MF
Manifest-Version: 1.0
Automatic-Module-Name: io.github.mojolowjo.ninefix
Implementation-Title: NINE Performance Fixes
Implementation-Version: ${VERSION}
MF

jar --create --file "$JAR" --date="$BUILD_DATE" --manifest build/MANIFEST.MF -C build/classes .

echo "Built $JAR"
if command -v sha1sum >/dev/null; then sha1sum "$JAR"; elif command -v shasum >/dev/null; then shasum -a 1 "$JAR"; fi
