# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento
semantico. O historico por entrega (TP2, TP3, TP4) esta em [`VERSION.md`](VERSION.md).

## [5.0.0] - 2026-10-08

Implantacao e manutencao em producao simulada: Docker, Kubernetes, observabilidade, testes
ampliados e CI/CD. Detalhes e evidencias em [`docs/RASTREABILIDADE.md`](docs/RASTREABILIDADE.md).

### Adicionado
- **Docker:** Dockerfiles multi-etapa para o servico principal, o microsservico de alertas e o
  front-end (Nginx sem privilegios), com `.dockerignore`, usuario nao root e `HEALTHCHECK`.
- **Docker Compose:** profile `app` com os tres servicos e a pilha de observabilidade; o
  `docker compose up` sem profile continua subindo so a infraestrutura.
- **Kubernetes (Kustomize):** namespaces `gestarafeto-prod` e `gestarafeto-obs`; Deployments,
  StatefulSets com PVC, Services, ConfigMap, probes (startup/readiness/liveness), requests/limits,
  HPA (app 2–6, alertas 2–5), PodDisruptionBudgets, NetworkPolicies e contexto de seguranca restrito.
- **Observabilidade:** OpenTelemetry (traces e logs por OTLP), Jaeger, Loki e Grafana com
  datasources e dashboard provisionados; cabecalho `X-Trace-Id` em todas as respostas HTTP;
  o trace atravessa HTTP, Feign e RabbitMQ.
- **Scripts:** `scripts/smoke.sh` (ponta a ponta), `k8s-build-load.sh`, `k8s-up.sh`, `k8s-down.sh`.
- **CI/CD (GitHub Actions):** `ci.yml` (gitleaks, testes, Checkstyle, JaCoCo, tsc, hadolint,
  kubeconform, build de imagens, implantacao em kind) e `cd.yml` (publicacao no GHCR, implantacao
  verificada, manifests anexados ao Release).
- **Testes:** +57 (de 89 para 146): observabilidade, actuator/exposicao, configuracao de producao,
  politica dos manifests Kubernetes, tratamento de erros HTTP e fluxo completo da API.
- **Analise estatica e cobertura:** Checkstyle (`config/checkstyle.xml`) e JaCoCo nos dois modulos.
- **Documentacao:** `docs/PRODUCAO.md`, `OBSERVABILIDADE.md`, `CICD.md`, `DEMONSTRACAO_PRODUCAO.md`,
  `RASTREABILIDADE.md`.

### Alterado
- Testcontainers 1.21.3 → 1.21.4 (a versao anterior nao conectava ao Docker 29 e 6 testes eram
  ignorados em silencio).
- `compose.yaml`: removido `container_name` fixo (conflitava com containers antigos e impedia escalar).
- `docs/ARQUITETURA_EVENTOS.md`: comandos `docker exec gestarafeto-rabbitmq …` trocados por
  `docker compose exec rabbitmq …`.
- `RabbitTemplate` do servico principal passa a gerar spans de publicacao.

### Corrigido
- Erros do cliente (rota inexistente, metodo nao suportado, JSON malformado, id invalido) respondiam
  500; agora 404/405/415/400. O 500 inesperado passa a ser registrado no log.
- A senha gerada do Spring Security era escrita no log da aplicacao; agora nao ha usuario padrao.
- Os logs nao chegavam ao Loki: faltava ligar o appender do Logback ao SDK do OpenTelemetry.
- Os pods do microsservico de alertas nao subiam no Kubernetes por causa de variaveis de service
  link colidindo com `server.port` (`enableServiceLinks: false`).

### Seguranca
- Nenhum segredo versionado: senhas do cluster sao geradas por `k8s-up.sh`; `gitleaks` no CI.
- Contêineres sem root, sem privilegios, sistema de arquivos somente leitura e sem token de ServiceAccount.

## [4.0.0] - TP4/PB
Arquitetura orientada a eventos com RabbitMQ. Ver [`VERSION.md`](VERSION.md).

## [3.0.0] - TP3/PB
Microsservico de alertas, OpenFeign e circuit breaker. Ver [`VERSION.md`](VERSION.md).
