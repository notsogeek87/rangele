#!/usr/bin/env bash
# Lance ktlint sans passer par Gradle.
#
# Raison d'être : dans une session Claude Code sur le web, `dl.google.com` (Google Maven) est
# bloqué par la politique réseau, donc le plugin Android Gradle ne peut pas être résolu et
# AUCUNE tâche Gradle ne peut tourner — `./gradlew ktlintCheck` y est impossible. Le CLI ktlint,
# lui, est sur Maven Central, qui est accessible. Ce script reproduit le verdict de
# `./gradlew ktlintCheck` (mêmes sources, même version, même style) pour qu'une violation soit
# détectée avant le push plutôt que par la CI.
#
# Usage :
#   scripts/ktlint.sh              # vérifie (sortie 1 si violation)
#   scripts/ktlint.sh -F           # corrige automatiquement ce qui peut l'être
#   scripts/ktlint.sh --install-only   # télécharge le jar sans rien vérifier
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Version alignée sur celle que le plugin Gradle utilise (`ktlint { version.set("…") }`),
# pour que ce script et la CI ne puissent pas diverger.
VERSION="$(sed -n 's/.*version\.set("\([0-9][0-9.]*\)").*/\1/p' app/build.gradle.kts | head -1)"
if [ -z "$VERSION" ]; then
    echo "ktlint.sh : version ktlint introuvable dans app/build.gradle.kts" >&2
    exit 2
fi

CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/ktlint"
JAR="$CACHE_DIR/ktlint-cli-$VERSION-all.jar"

if [ ! -s "$JAR" ]; then
    echo "ktlint.sh : téléchargement de ktlint $VERSION depuis Maven Central…" >&2
    mkdir -p "$CACHE_DIR"
    curl -sSL --fail --max-time 300 -o "$JAR.part" \
        "https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-cli/$VERSION/ktlint-cli-$VERSION-all.jar"
    mv "$JAR.part" "$JAR"
fi

if [ "${1:-}" = "--install-only" ]; then
    echo "$JAR"
    exit 0
fi

if ! command -v java >/dev/null 2>&1; then
    echo "ktlint.sh : java introuvable" >&2
    exit 2
fi

# Mêmes source sets que `ktlintCheck` : main, test et les scripts Kotlin. Le style reste celui
# par défaut : `android.set(true)` côté plugin Gradle ne bascule PAS ktlint 1.x en style
# android_studio (vérifié — ce style rejette du code que la CI accepte), donc ne pas ajouter
# `ktlint_code_style` au .editorconfig.
exec java -jar "$JAR" --relative "$@" \
    "app/src/main/**/*.kt" \
    "app/src/test/**/*.kt" \
    "**/*.kts" \
    "!**/build/**"
