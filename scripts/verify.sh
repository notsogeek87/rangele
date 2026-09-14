#!/usr/bin/env bash
# Vérification avant push : lance TOUT ce qui est exécutable dans l'environnement courant.
#
# Pourquoi : plusieurs builds KO d'affilée sur `staging`/`main` (formatage ktlint, import manquant,
# tests jamais exécutés). Ce script est le premier filet ; il est branché sur le hook `pre-push`
# (voir `.githooks/pre-push`), donc il tourne automatiquement à chaque `git push`.
#
# Usage :
#   scripts/verify.sh            # tout ce qui est possible ici (statique + Gradle si disponible)
#   scripts/verify.sh --fast     # seulement les vérifications statiques (pas de Gradle)
#
# Variables d'environnement :
#   RANGELE_SKIP_GRADLE=1        # équivaut à --fast
#
# Sortie 0 = rien à signaler. Sortie 1 = au moins une vérification a échoué : ne pas pousser.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

FAST=0
[ "${1:-}" = "--fast" ] && FAST=1
[ "${RANGELE_SKIP_GRADLE:-}" = "1" ] && FAST=1

failures=0
skipped_gradle=0

ok() { printf '  \033[32m✓\033[0m %s\n' "$1"; }
ko() { printf '  \033[31m✗\033[0m %s\n' "$1"; failures=$((failures + 1)); }
warn() { printf '  \033[33m!\033[0m %s\n' "$1"; }
title() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# ---------------------------------------------------------------------------
title "1/5 Formatage Kotlin (ktlint)"
if out="$(scripts/ktlint.sh 2>&1)"; then
    ok "ktlint : aucune violation"
else
    echo "$out" | grep -v 'Picked up JAVA_TOOL_OPTIONS' | head -40
    ko "ktlint : violations ci-dessus (corriger avec : scripts/ktlint.sh -F)"
fi

# ---------------------------------------------------------------------------
title "2/5 Marqueurs de conflit de merge non résolus"
# Les merges récents vers staging/main ont eu des conflits : un marqueur oublié casse la compilation.
if conflicts="$(git grep -n -E '^(<<<<<<< |>>>>>>> )' -- '*.kt' '*.kts' '*.xml' '*.toml' '*.md' '*.yml' 2>/dev/null)"; then
    echo "$conflicts" | head -20
    ko "marqueurs de conflit présents dans des fichiers suivis"
else
    ok "aucun marqueur de conflit"
fi

# ---------------------------------------------------------------------------
title "3/5 Cohérence des migrations Room"
python3 - <<'PY'
import re, sys, pathlib

db = next(pathlib.Path('app/src/main').rglob('AppDatabase.kt'), None)
mig = next(pathlib.Path('app/src/main').rglob('Migrations.kt'), None)
if db is None or mig is None:
    print("  ! AppDatabase.kt ou Migrations.kt introuvable — vérification ignorée")
    sys.exit(0)

db_src, mig_src = db.read_text(), mig.read_text()
m = re.search(r'version\s*=\s*(\d+)', db_src)
if not m:
    print("  ! version de la base introuvable dans AppDatabase.kt — vérification ignorée")
    sys.exit(0)

version = int(m.group(1))
missing = [
    f"MIGRATION_{v}_{v + 1}"
    for v in range(1, version)
    if not re.search(rf'\bMIGRATION_{v}_{v + 1}\b', mig_src)
]
if missing:
    print(f"  ✗ base en version {version} mais migrations absentes de Migrations.kt : {', '.join(missing)}")
    sys.exit(1)

# Une migration écrite mais jamais branchée sur le builder Room ne sert à rien : à l'installation
# d'une mise à jour, Room lève IllegalStateException au lieu de migrer.
declared = set(re.findall(r'val\s+(MIGRATION_\d+_\d+)', mig_src))
wired = {
    name
    for path in pathlib.Path('app/src/main').rglob('*.kt')
    if path != mig
    for name in re.findall(r'\b(MIGRATION_\d+_\d+)\b', path.read_text())
}
orphans = sorted(declared - wired)
if orphans:
    print(f"  ✗ migrations déclarées mais jamais passées à addMigrations(...) : {', '.join(orphans)}")
    sys.exit(1)

print(f"  ✓ base en version {version}, migrations 1→{version} présentes et branchées")
PY
[ $? -ne 0 ] && failures=$((failures + 1))

# ---------------------------------------------------------------------------
title "4/5 Références au version catalog (libs.versions.toml)"
python3 - <<'PY'
import re, sys, pathlib

toml = pathlib.Path('gradle/libs.versions.toml')
if not toml.exists():
    print("  ! gradle/libs.versions.toml introuvable — vérification ignorée")
    sys.exit(0)

section, keys = None, set()
for line in toml.read_text().splitlines():
    line = line.strip()
    if line.startswith('['):
        section = line.strip('[]')
        continue
    m = re.match(r'([A-Za-z0-9_.\-]+)\s*=', line)
    if not m or section is None:
        continue
    alias = m.group(1).replace('-', '.').replace('_', '.')
    prefix = {'versions': 'versions.', 'plugins': 'plugins.', 'bundles': 'bundles.'}.get(section, '')
    keys.add(prefix + alias)

unknown = []
for path in list(pathlib.Path('.').glob('*.gradle.kts')) + list(pathlib.Path('app').glob('*.gradle.kts')):
    for ref in re.finditer(r'\blibs\.([A-Za-z0-9_.]+)(\()?', path.read_text()):
        if ref.group(2):  # libs.findLibrary("…") & co : pas un accesseur d'alias
            continue
        accessor = ref.group(1).rstrip('.')
        # Un accesseur peut désigner un groupe intermédiaire (libs.androidx.room → androidx.room.ktx).
        if accessor in keys or any(k.startswith(accessor + '.') for k in keys):
            continue
        unknown.append(f"{path}: libs.{accessor}")

if unknown:
    for u in unknown:
        print(f"  ✗ alias inconnu → {u}")
    sys.exit(1)

print(f"  ✓ tous les alias libs.* référencés existent ({len(keys)} alias déclarés)")
PY
[ $? -ne 0 ] && failures=$((failures + 1))

# ---------------------------------------------------------------------------
title "5/5 Compilation et tests unitaires (Gradle)"
gradle_reachable() {
    # Google Maven (dl.google.com) est nécessaire pour résoudre le plugin Android Gradle. Il est
    # bloqué par la politique réseau des sessions Claude Code sur le web : Gradle y est inutilisable.
    curl -s -o /dev/null --max-time 10 "https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml"
}

if [ "$FAST" = "1" ]; then
    warn "Gradle ignoré (--fast) : compilation et tests NON vérifiés ici"
    skipped_gradle=1
elif [ ! -x ./gradlew ] && [ ! -f ./gradlew ]; then
    warn "gradlew introuvable — compilation et tests NON vérifiés ici"
    skipped_gradle=1
elif ! gradle_reachable; then
    warn "dl.google.com injoignable (politique réseau) : Gradle ne peut pas résoudre le plugin Android."
    warn "Compilation et tests unitaires NON vérifiés ici — c'est la CI de la branche qui les valide."
    warn "Pour lever la limite : autoriser dl.google.com dans la politique réseau de l'environnement."
    skipped_gradle=1
else
    if ./gradlew --no-daemon ktlintCheck testStagingDebugUnitTest testProductionDebugUnitTest assembleDebug; then
        ok "Gradle : lint, tests unitaires (staging + production) et APK debug OK"
    else
        ko "Gradle : voir la sortie ci-dessus"
    fi
fi

# ---------------------------------------------------------------------------
echo
if [ "$failures" -gt 0 ]; then
    printf '\033[31m✗ %s vérification(s) en échec — ne pas pousser en l'"'"'état.\033[0m\n' "$failures"
    exit 1
fi

if [ "$skipped_gradle" = "1" ]; then
    printf '\033[33m✓ Vérifications statiques OK, mais compilation/tests non exécutés ici.\033[0m\n'
    printf '  Pousser la branche de travail d'"'"'abord, puis attendre le vert :\n'
    printf '    scripts/ci-status.sh --wait\n'
    printf '  et ne merger dans staging/main qu'"'"'une fois la CI verte.\n'
else
    printf '\033[32m✓ Tout est vert.\033[0m\n'
fi
