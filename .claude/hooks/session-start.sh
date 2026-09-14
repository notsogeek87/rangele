#!/usr/bin/env bash
# Prépare une session Claude Code : garde-fou `pre-push` installé, lint exécutable localement.
#
# `./gradlew` ne peut pas fonctionner dans cet environnement : la politique réseau y refuse
# `dl.google.com`, donc le plugin Android Gradle n'est jamais résolu. Le CLI ktlint vient de
# Maven Central (accessible) et permet au moins de vérifier le formatage avant de pousser,
# ce qui est la cause des échecs CI les plus fréquents sur ce dépôt.
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

# Garde-fou avant push (ktlint + vérifications statiques + contrôle de CI verte pour staging/main).
# `core.hooksPath` est une config locale au clone : elle doit être (re)posée dans chaque session.
git -C "$PROJECT_DIR" config core.hooksPath .githooks 2>/dev/null || true

# Sur une machine de dev normale, `./gradlew ktlintCheck` marche : rien d'autre à préparer.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

JAR="$("$PROJECT_DIR/scripts/ktlint.sh" --install-only)"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    echo "export KTLINT_JAR=\"$JAR\"" >> "$CLAUDE_ENV_FILE"
fi

echo "ktlint prêt : $JAR"
echo "Avant tout push : scripts/verify.sh (le hook pre-push le lance automatiquement)"
echo "État de la CI d'une branche : scripts/ci-status.sh [--wait] [branche]"
