#!/usr/bin/env bash
# Prépare une session Claude Code sur le web pour que le lint soit exécutable localement.
#
# `./gradlew` ne peut pas fonctionner dans cet environnement : la politique réseau y refuse
# `dl.google.com`, donc le plugin Android Gradle n'est jamais résolu. Le CLI ktlint vient de
# Maven Central (accessible) et permet au moins de vérifier le formatage avant de pousser,
# ce qui est la cause des échecs CI les plus fréquents sur ce dépôt.
set -euo pipefail

# Sur une machine de dev normale, `./gradlew ktlintCheck` marche : rien à préparer.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

JAR="$("$PROJECT_DIR/scripts/ktlint.sh" --install-only)"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    echo "export KTLINT_JAR=\"$JAR\"" >> "$CLAUDE_ENV_FILE"
fi

echo "ktlint prêt : $JAR"
echo "Vérifier le formatage avant tout push : scripts/ktlint.sh (corriger : scripts/ktlint.sh -F)"
