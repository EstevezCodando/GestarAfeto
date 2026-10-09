#!/usr/bin/env bash
# Sobe (ou atualiza) o ambiente no Kubernetes: namespaces, segredos, aplicacao e observabilidade.
#
# Idempotente: pode ser executado quantas vezes for preciso. Os segredos so sao gerados se ainda
# nao existirem, o que e essencial: trocar a senha de um banco ja inicializado quebraria o login.
#
# Variaveis:
#   KUBE_CONTEXT  contexto do kubectl (padrao: docker-desktop)
#   OVERLAY       overlay Kustomize (padrao: k8s/overlays/local)
#   IMAGE_TAG     tag das imagens (padrao: conteudo de .image-tag, gravado por k8s-build-load.sh)
#   IMAGE_PREFIX  prefixo do nome das imagens, ex.: ghcr.io/dono/gestarafeto- (padrao: gestarafeto/)
#   SO_APLICAR    se definida, nao espera os rollouts terminarem
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
CTX="${KUBE_CONTEXT:-docker-desktop}"
OVERLAY="${OVERLAY:-$RAIZ/k8s/overlays/local}"
IMAGE_PREFIX="${IMAGE_PREFIX:-gestarafeto/}"
K=(kubectl --context "$CTX")

IMAGE_TAG="${IMAGE_TAG:-$(cat "$RAIZ/.image-tag" 2>/dev/null || true)}"
[ -n "$IMAGE_TAG" ] || { echo "Defina IMAGE_TAG ou rode scripts/k8s-build-load.sh antes." >&2; exit 1; }

segredo_aleatorio() { head -c 18 /dev/urandom | od -An -tx1 | tr -d ' \n'; }

echo "== Contexto: $CTX"
"${K[@]}" cluster-info >/dev/null 2>&1 || { echo "Cluster inacessivel no contexto '$CTX'." >&2; exit 1; }

echo "== Namespaces"
"${K[@]}" apply -f "$RAIZ/k8s/base/namespace.yaml" -f "$RAIZ/observability/namespace.yaml"

echo "== Segredos (gerados apenas se ausentes)"
if ! "${K[@]}" -n gestarafeto-prod get secret gestarafeto-secrets >/dev/null 2>&1; then
  "${K[@]}" -n gestarafeto-prod create secret generic gestarafeto-secrets \
    --from-literal=core-db-password="$(segredo_aleatorio)" \
    --from-literal=alertas-db-password="$(segredo_aleatorio)" \
    --from-literal=rabbitmq-password="$(segredo_aleatorio)"
else
  echo "  gestarafeto-secrets ja existe, mantido."
fi
if ! "${K[@]}" -n gestarafeto-obs get secret grafana-admin >/dev/null 2>&1; then
  GRAFANA_SENHA="$(segredo_aleatorio)"
  "${K[@]}" -n gestarafeto-obs create secret generic grafana-admin \
    --from-literal=user=admin --from-literal=password="$GRAFANA_SENHA"
  echo "  Grafana: usuario 'admin', senha '$GRAFANA_SENHA' (guarde; para ler depois, veja docs/KUBERNETES.md)"
else
  echo "  grafana-admin ja existe, mantido."
fi

# Overlay temporario so para fixar a tag; o overlay versionado fica intocado.
TMP="$RAIZ/k8s/overlays/.gerado"
rm -rf "$TMP"; mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/kustomization.yaml" <<YAML
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../$(basename "$OVERLAY")
images:
  - {name: gestarafeto/app, newName: "${IMAGE_PREFIX}app", newTag: "$IMAGE_TAG"}
  - {name: gestarafeto/alertas, newName: "${IMAGE_PREFIX}alertas", newTag: "$IMAGE_TAG"}
  - {name: gestarafeto/frontend, newName: "${IMAGE_PREFIX}frontend", newTag: "$IMAGE_TAG"}
YAML

echo "== Aplicando manifests (overlay $OVERLAY, imagens :$IMAGE_TAG)"
"${K[@]}" apply -k "$TMP"

if [ -z "${SO_APLICAR:-}" ]; then
  echo "== Aguardando rollouts"
  for r in statefulset/postgres-core statefulset/postgres-alertas statefulset/rabbitmq \
           deployment/gestarafeto-app deployment/gestarafeto-alertas deployment/gestarafeto-frontend; do
    "${K[@]}" -n gestarafeto-prod rollout status "$r" --timeout=420s
  done
  for r in deployment/jaeger statefulset/loki deployment/grafana; do
    "${K[@]}" -n gestarafeto-obs rollout status "$r" --timeout=300s
  done
fi

echo "== Estado"
"${K[@]}" -n gestarafeto-prod get pods,hpa
"${K[@]}" -n gestarafeto-obs get pods
