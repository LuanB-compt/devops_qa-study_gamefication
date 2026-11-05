#!/bin/bash
# Valida a infraestrutura: compose (dev/homol/prod), properties, Jenkins e ambiente HOMOL.
# Uso: bash scripts/validate_infra.sh [--down]
#   --down  derruba Jenkins e HOMOL (containers e redes; volumes preservados) e sai.
# Executar na maquina host (usa localhost e as portas publicadas).

cd "$(dirname "$0")/.." || exit 1

JENKINS_URL="${JENKINS_URL:-http://localhost:8180}"
HOMOL_FILE=docker-compose.homol.yml
JENKINS_FILE=docker-compose.jenkins.yml
FAILED=0

ok()   { echo "  [OK]   $1"; }
fail() { echo "  [FALHA] $1"; FAILED=1; }

if [ "$1" = "--down" ]; then
  docker compose -f "$HOMOL_FILE" down --remove-orphans
  docker compose -f "$JENKINS_FILE" down --remove-orphans
  exit 0
fi

# wait_until <descricao> <tentativas> <intervalo> <comando...>
wait_until() {
  local desc="$1" tries="$2" delay="$3"; shift 3
  for _ in $(seq 1 "$tries"); do
    if "$@" >/dev/null 2>&1; then ok "$desc"; return 0; fi
    sleep "$delay"
  done
  fail "$desc"; return 1
}

http_200() { [ "$(curl -s -o /dev/null -w '%{http_code}' "$1")" = "200" ]; }
health_of() { docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null; }
is_healthy() { [ "$(health_of "$1")" = "healthy" ]; }

echo "== 1. Arquivos Compose e properties =="
for env in dev homol prod; do
  if docker compose -f "docker-compose.$env.yml" config -q 2>/dev/null; then
    ok "docker-compose.$env.yml valido"
  else
    fail "docker-compose.$env.yml invalido (docker compose config -q)"
  fi
  [ -f "src/main/resources/application-$env.properties" ] \
    && ok "application-$env.properties existe" \
    || fail "application-$env.properties ausente"
done
docker compose -f "$JENKINS_FILE" config -q 2>/dev/null \
  && ok "$JENKINS_FILE valido" || fail "$JENKINS_FILE invalido"

echo "== 2. Jenkins =="
docker compose -f "$JENKINS_FILE" up -d --build || fail "falha ao subir o Jenkins"
wait_until "container jenkins healthy" 40 5 is_healthy jenkins
wait_until "Jenkins /login responde 200" 6 5 http_200 "$JENKINS_URL/login"

echo "== 3. HOMOL =="
IMAGE_TAG="${IMAGE_TAG:-validate}" docker compose -f "$HOMOL_FILE" up -d --build || fail "falha ao subir o HOMOL"
PG_CID=$(docker compose -f "$HOMOL_FILE" ps -q postgres)
wait_until "postgres healthy" 30 5 is_healthy "$PG_CID"
wait_until "app /actuator/health = UP" 24 5 \
  bash -c "curl -fsS http://localhost:8081/actuator/health | grep -q '\"status\":\"UP\"'"
wait_until "app /actuator/prometheus responde 200" 6 5 http_200 http://localhost:8081/actuator/prometheus
wait_until "pgadmin responde 200"    24 5 http_200 http://localhost:5051/misc/ping
wait_until "prometheus responde 200" 12 5 http_200 http://localhost:9091/-/ready
wait_until "grafana responde 200"    12 5 http_200 http://localhost:3001/api/health
wait_until "frontend responde 200"   12 5 http_200 http://localhost:5174/

echo
if [ "$FAILED" -eq 0 ]; then echo "Infraestrutura VALIDADA."; else echo "Validacao FALHOU."; fi
exit "$FAILED"
