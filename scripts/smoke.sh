#!/usr/bin/env bash
# Teste de fumaca ponta a ponta: exercita os dois servicos, o banco e o RabbitMQ.
#
# Fluxo: cria gestante -> gera checklist (servico principal) -> evento checklist.alterado
# no RabbitMQ -> microsservico de alertas calcula alertas -> servico principal le os alertas
# por HTTP (Feign). Se qualquer elo falhar, o script termina com codigo != 0.
#
# Variaveis: APP_URL (padrao http://localhost:8080), FRONT_URL (opcional; se definida,
# repete a leitura passando pelo proxy do Nginx), TIMEOUT_EVENTO (segundos, padrao 45).
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8080}"
FRONT_URL="${FRONT_URL:-}"
TIMEOUT_EVENTO="${TIMEOUT_EVENTO:-45}"

ok()   { printf '  [ok]   %s\n' "$*"; }
fail() { printf '  [FAIL] %s\n' "$*" >&2; exit 1; }
json_id() { sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -n1; }

echo "== Saude"
curl -fsS "$APP_URL/actuator/health/readiness" | grep -q '"UP"' || fail "servico principal nao esta pronto"
ok "servico principal pronto"

echo "== Criar gestante"
EMAIL="smoke-$(date +%s)-$RANDOM@example.com"
# Data provavel de parto daqui a ~14 semanas => gestacao na semana ~26 (itens atrasados).
DPP="$(date -d '+98 days' +%F 2>/dev/null || date -v+98d +%F)"
RESP="$(curl -fsS -X POST "$APP_URL/api/gestantes" -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Smoke Test\",\"email\":\"$EMAIL\",\"dataProvavelParto\":\"$DPP\"}")"
ID="$(printf '%s' "$RESP" | json_id)"
[ -n "$ID" ] || fail "resposta sem id: $RESP"
ok "gestante id=$ID"

cleanup() { curl -s -X DELETE "$APP_URL/api/gestantes/$ID" >/dev/null || true; }
trap cleanup EXIT

echo "== Gerar checklist"
curl -fsS -X POST "$APP_URL/api/gestantes/$ID/checklist/gerar" >/dev/null || fail "falha ao gerar checklist"
ITENS="$(curl -fsS "$APP_URL/api/gestantes/$ID/checklist" | grep -o '"id":' | wc -l)"
[ "$ITENS" -gt 0 ] || fail "checklist vazio"
ok "$ITENS itens no checklist"

echo "== Evento -> microsservico de alertas (assincrono, ate ${TIMEOUT_EVENTO}s)"
fim=$((SECONDS + TIMEOUT_EVENTO))
TOTAL=0
while [ $SECONDS -lt $fim ]; do
  RESUMO="$(curl -fsS "$APP_URL/api/gestantes/$ID/alertas/resumo" || true)"
  TOTAL="$(printf '%s' "$RESUMO" | sed -n 's/.*"totalAtivos":\([0-9]*\).*/\1/p')"
  [ "${TOTAL:-0}" -gt 0 ] && break
  sleep 2
done
[ "${TOTAL:-0}" -gt 0 ] || fail "nenhum alerta apareceu: o evento nao chegou ao microsservico (resumo=$RESUMO)"
ok "$TOTAL alertas ativos calculados pelo microsservico"

if [ -n "$FRONT_URL" ]; then
  echo "== Front-end (Nginx -> API)"
  curl -fsS "$FRONT_URL/api/gestantes/$ID/alertas/resumo" | grep -q '"totalAtivos"' || fail "proxy do front-end nao alcancou a API"
  ok "proxy do Nginx encaminhou a requisicao"
fi

echo "== Fumaca concluida com sucesso"
