#!/bin/bash
# Lint do Jenkinsfile via endpoint do plugin Pipeline: Declarative.
# Uso: bash scripts/validate_jenkinsfile.sh   (JENKINS_URL padrao: http://localhost:8180)
# Se o Jenkins exigir autenticacao: JENKINS_USER e JENKINS_TOKEN (API token ou senha).

cd "$(dirname "$0")/.." || exit 1

JENKINS_URL="${JENKINS_URL:-http://localhost:8180}"
AUTH=()
COOKIES=$(mktemp)
trap 'rm -f "$COOKIES"' EXIT
[ -n "$JENKINS_USER" ] && AUTH=(-u "$JENKINS_USER:$JENKINS_TOKEN")

# CSRF crumb (retorna vazio se o Jenkins nao exigir)
HEADERS=()
CRUMB=$(curl -fsS -c "$COOKIES" "${AUTH[@]}" "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,%22:%22,//crumb)" 2>/dev/null)
[ -n "$CRUMB" ] && HEADERS=(-H "$CRUMB")

RESULT=$(curl -sS -b "$COOKIES" "${AUTH[@]}" "${HEADERS[@]}" -X POST -F "jenkinsfile=<Jenkinsfile" \
  "$JENKINS_URL/pipeline-model-converter/validate")
echo "$RESULT"

if echo "$RESULT" | grep -q "successfully validated"; then
  echo "Jenkinsfile VALIDO."
  exit 0
fi
echo "Jenkinsfile INVALIDO (ou Jenkins inacessivel/sem autenticacao)."
exit 1
