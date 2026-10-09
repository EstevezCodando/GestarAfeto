#!/usr/bin/env bash
# Remove o ambiente do Kubernetes.
#
#   ./scripts/k8s-down.sh           remove workloads; PRESERVA volumes (PVC), segredos e namespaces
#   ./scripts/k8s-down.sh --tudo    remove os namespaces e, com eles, todos os dados
set -euo pipefail

CTX="${KUBE_CONTEXT:-docker-desktop}"
K=(kubectl --context "$CTX")

if [ "${1:-}" = "--tudo" ]; then
  echo "Removendo namespaces gestarafeto-prod e gestarafeto-obs (dados incluidos)..."
  "${K[@]}" delete namespace gestarafeto-prod gestarafeto-obs --ignore-not-found
else
  echo "Removendo workloads (volumes e segredos preservados)..."
  for ns in gestarafeto-prod gestarafeto-obs; do
    "${K[@]}" -n "$ns" delete deployment,statefulset,service,hpa,pdb,networkpolicy,configmap,job,pod \
      -l app.kubernetes.io/part-of=gestarafeto --ignore-not-found
    # ConfigMaps gerados pelo Kustomize ganham sufixo de hash e nao levam o rotulo acima.
    "${K[@]}" -n "$ns" delete configmap --all --ignore-not-found
  done
  echo "Para apagar tambem os dados: ./scripts/k8s-down.sh --tudo"
fi
