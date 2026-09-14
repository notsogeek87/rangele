#!/usr/bin/env bash
# État de la CI GitHub Actions pour un commit, en ligne de commande.
#
# Raison d'être : dans une session Claude Code sur le web, Gradle est inutilisable (dl.google.com
# bloqué), donc la compilation et les tests ne sont validés QUE par la CI. Avant de merger une
# branche de travail dans `staging`/`main`, il faut donc pouvoir répondre à « est-ce que la CI de
# cette branche est verte ? » sans ouvrir le navigateur.
#
# Usage :
#   scripts/ci-status.sh                  # état du HEAD local
#   scripts/ci-status.sh <branche|sha>    # état d'un commit précis
#   scripts/ci-status.sh --wait [ref]     # attend la fin du run (30 min max)
#
# Sortie : 0 = run réussi, 1 = run en échec, 2 = aucun run trouvé, 3 = run encore en cours,
#          4 = API injoignable.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

WAIT=0
if [ "${1:-}" = "--wait" ]; then
    WAIT=1
    shift
fi

REF="${1:-HEAD}"
SHA="$(git rev-parse "$REF" 2>/dev/null)" || {
    echo "ci-status.sh : référence git inconnue : $REF" >&2
    exit 2
}

REMOTE_URL="$(git remote get-url origin 2>/dev/null)"
SLUG="$(printf '%s' "$REMOTE_URL" | sed -E 's#(git@github\.com:|https://[^/]*github\.com/)##; s#\.git$##')"
if [ -z "$SLUG" ]; then
    echo "ci-status.sh : impossible de déduire owner/repo depuis '$REMOTE_URL'" >&2
    exit 2
fi

auth_header=()
TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"
[ -n "$TOKEN" ] && auth_header=(-H "Authorization: Bearer $TOKEN")

query_run() {
    curl -sS --max-time 30 "${auth_header[@]}" \
        -H "Accept: application/vnd.github+json" \
        "https://api.github.com/repos/$SLUG/actions/runs?head_sha=$SHA&per_page=20" 2>/dev/null |
        python3 -c '
import json, sys

try:
    runs = json.load(sys.stdin).get("workflow_runs", [])
except Exception:
    print("API_ERROR")
    sys.exit(0)

if not runs:
    print("NO_RUN")
    sys.exit(0)

run = sorted(runs, key=lambda r: r.get("run_number", 0))[-1]
print("\t".join([run.get("status") or "", run.get("conclusion") or "", run.get("html_url") or ""]))
'
}

deadline=$(( $(date +%s) + 1800 ))
while :; do
    result="$(query_run)"
    case "$result" in
        API_ERROR | "")
            echo "ci-status.sh : API GitHub injoignable (réseau ou quota)." >&2
            exit 4
            ;;
        NO_RUN)
            if [ "$WAIT" = "1" ] && [ "$(date +%s)" -lt "$deadline" ]; then
                sleep 20
                continue
            fi
            echo "Aucun run CI pour ${SHA:0:7} — ce commit n'a jamais été vérifié."
            exit 2
            ;;
    esac

    status="$(printf '%s' "$result" | cut -f1)"
    conclusion="$(printf '%s' "$result" | cut -f2)"
    url="$(printf '%s' "$result" | cut -f3)"

    if [ "$status" != "completed" ]; then
        if [ "$WAIT" = "1" ] && [ "$(date +%s)" -lt "$deadline" ]; then
            sleep 20
            continue
        fi
        echo "CI en cours pour ${SHA:0:7} ($status) — $url"
        exit 3
    fi

    if [ "$conclusion" = "success" ]; then
        printf '\033[32m✓ CI verte\033[0m pour %s — %s\n' "${SHA:0:7}" "$url"
        exit 0
    fi
    printf '\033[31m✗ CI %s\033[0m pour %s — %s\n' "$conclusion" "${SHA:0:7}" "$url"
    exit 1
done
