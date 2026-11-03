#!/bin/bash
# Smoke test dos endpoints REST da aplicacao gamification.
# Uso: scripts/integration_tests-endpoints.sh [BASE_URL]   (padrao: http://localhost:8080)
# Sai com codigo != 0 se qualquer endpoint responder fora de 2xx.

BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
FAILED=0

# req <descricao> <metodo> <caminho> [corpo-json]
req() {
  local desc="$1" method="$2" path="$3" body="${4:-}"
  local args=(-s -o /tmp/smoke_body.$$ -w "%{http_code}" -X "$method" "$BASE_URL$path")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")

  local code
  code=$(curl "${args[@]}")
  echo "== $desc: $method $path -> HTTP $code"
  cat /tmp/smoke_body.$$ 2>/dev/null; echo

  if [[ ! "$code" =~ ^2 ]]; then
    echo "FALHA: esperado 2xx, recebido $code"
    FAILED=1
  fi
}
trap 'rm -f /tmp/smoke_body.$$' EXIT

req "Lista alunos"               GET  /api/alunos
req "Cria assinatura (e aluno)"  POST /api/assinaturas/Luan
req "Consulta aluno"             GET  /api/alunos/Luan
req "Consulta assinatura"        GET  /api/assinaturas/Luan
req "Registra pagamento"         POST /api/assinaturas/Luan/pagamento
req "Registra contribuicao"      POST /api/meses/set-2026/contribuicoes '{"alunoNome":"Luan","peso":1.0}'
req "Consulta ranking"           GET  /api/meses/set-2026/ranking
req "Conclui curso (media 8.5)"  POST /api/alunos/Luan/cursos '{"cursoNome":"DevOps","media":8.5}'
req "Actuator health"            GET  /actuator/health
req "Actuator prometheus"        GET  /actuator/prometheus

if [ "$FAILED" -ne 0 ]; then
  echo "Smoke test FALHOU."
  exit 1
fi
echo "Smoke test OK."
