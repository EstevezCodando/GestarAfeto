#!/usr/bin/env bash
# Constroi as tres imagens e as carrega no no do cluster local.
#
# Por que carregar: o Kubernetes local (Docker Desktop/kind) tem um containerd PROPRIO, separado do
# daemon Docker do host. Uma imagem recem-construida no host nao e visivel para os pods ate ser
# importada no no. Com imagePullPolicy IfNotPresent, tags reaproveitadas fariam o cluster rodar uma
# imagem antiga; por isso cada build recebe uma tag nova, gravada em .image-tag para o k8s-up.sh.
#
# Variaveis:
#   IMAGE_TAG   tag a usar (padrao: 5.0.0-<sha curto>[-dirty-<hora>])
#   KIND_NODE   container do no (padrao: desktop-control-plane; para kind: <cluster>-control-plane)
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"

VERSAO_BASE="5.0.0"
SHA="$(git rev-parse --short HEAD 2>/dev/null || echo nogit)"
SUFIXO=""
[ -n "$(git status --porcelain 2>/dev/null)" ] && SUFIXO="-dirty-$(date +%H%M%S)"
IMAGE_TAG="${IMAGE_TAG:-${VERSAO_BASE}-${SHA}${SUFIXO}}"
KIND_NODE="${KIND_NODE:-desktop-control-plane}"

echo "== Construindo imagens com a tag $IMAGE_TAG"
docker build -t "gestarafeto/app:$IMAGE_TAG" -f Dockerfile .
docker build -t "gestarafeto/alertas:$IMAGE_TAG" -f alertas-service/Dockerfile alertas-service
docker build -t "gestarafeto/frontend:$IMAGE_TAG" -f frontend/Dockerfile frontend

echo "== Carregando no no '$KIND_NODE'"
docker inspect "$KIND_NODE" >/dev/null 2>&1 || { echo "No '$KIND_NODE' nao encontrado (defina KIND_NODE)." >&2; exit 1; }
for img in app alertas frontend; do
  # --platform evita importar as atestacoes de proveniencia, que o containerd do no nao precisa.
  docker save "gestarafeto/$img:$IMAGE_TAG" \
    | docker exec -i "$KIND_NODE" ctr -n k8s.io images import --platform linux/amd64 - >/dev/null
  echo "  gestarafeto/$img:$IMAGE_TAG carregada"
done

echo "$IMAGE_TAG" > "$RAIZ/.image-tag"
echo "== Pronto. Tag gravada em .image-tag: $IMAGE_TAG"
