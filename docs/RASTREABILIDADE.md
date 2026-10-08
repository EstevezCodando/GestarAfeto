# Rastreabilidade: requisitos de implantacao e manutencao em producao (v5.0.0)

Registro do que foi pedido, do que existia, do que foi feito e **de que forma foi comprovado**.
Estados permitidos: Pendente · Em implementacao · Implementado, nao validado · Validado ·
Bloqueado. "Validado" so aparece quando ha execucao observada; configuracao criada nao conta.

Execucoes de 08/10/2026 em Windows 10, Docker Desktop 29.6 (Kubernetes v1.36, no unico
`desktop-control-plane`, 8 CPUs / 16 GB), JDK 21, Node 24.

## 1. Matriz de rastreabilidade

| ID | Requisito | Estado | Evidencia | Validacao |
|---|---|---|---|---|
| R1 | Docker | **Validado** | 3 Dockerfiles multi-etapa, `.dockerignore` por contexto, usuario nao root, `HEALTHCHECK`; `compose.yaml` (profile `app`) | `docker compose --profile app up -d --build` → 9 servicos `healthy`; `smoke.sh` OK (incl. proxy Nginx); `id` = `uid=1001(spring)`; `hadolint` sem avisos nos 3 arquivos |
| R2 | Kubernetes | **Validado** | `k8s/base` + `observability` + overlay (39 recursos); namespaces, ConfigMap, Secrets gerados, PVCs, probes, requests/limits, HPA, PDB, NetworkPolicy | Deploy em Docker Desktop; `smoke.sh` OK; pod removido (30/30 OK); `kill 1` → reinicio do container; rollout 120/120 OK; dados do PostgreSQL sobrevivem; **HPA 2→6** sob carga; NetworkPolicy bloqueou pod intruso; `k8s-down`/`k8s-up` preservam dados |
| R3 | Logs e rastreamento | **Validado** | OpenTelemetry + Jaeger + Loki + Grafana; `X-Trace-Id`; dashboard provisionado | Trace unico atravessando HTTP → RabbitMQ → alertas (4 spans, 2 servicos) em Compose **e** Kubernetes; logs dos dois servicos no Loki filtrados por `trace_id`; erro real (RabbitMQ parado) achado por servico; 0 ocorrencias de `password/secret/senha` nos logs |
| R4 | Git/GitHub | **Validado** (publicacao pendente, ver §5) | Branch `feature/devops-producao`, 5 commits descritivos sobre `main`; `.gitignore`; `.env.example`; `CHANGELOG.md`; `VERSION.md` | `git log`; `gitleaks detect` em 6 commits: *no leaks found*. O `push` da branch nao foi feito (depende de credenciais do usuario) |
| R5 | GitHub Actions | **Implementado, nao validado** | `ci.yml` (7 jobs) e `cd.yml` (5 jobs); permissoes minimas; GHCR via `GITHUB_TOKEN` | `actionlint`: sem erros. **Nenhuma execucao no GitHub ainda.** Cada comando dos jobs foi rodado localmente com sucesso (§3) |
| R6 | Testes | **Validado** | 146 testes automatizados (72 + 74), JaCoCo, Checkstyle; testes novos em 6 areas | `mvn verify` nos dois modulos: **146 executados, 146 aprovados, 0 falhas, 0 erros, 0 ignorados**; 0 violacoes Checkstyle |
| R7 | Codigo adaptado | **Validado** | Mudancas em codigo, Dockerfiles, manifests, scripts, workflows (§4) | Imagens construidas a partir dele e executadas; testes verdes |
| R8 | Documentacao | **Validado** | README, `PRODUCAO.md`, `OBSERVABILIDADE.md`, `CICD.md`, `DEMONSTRACAO_PRODUCAO.md`, este arquivo, `TESTING.md`, `CHANGELOG.md` | Comandos de build, deploy, escala, observabilidade, down/up e testes foram executados contra os arquivos reais |
| R9 | Demonstracao operacional | **Implementado, nao validado** | `docs/DEMONSTRACAO_PRODUCAO.md` (10 etapas com comandos e resultados esperados) | Os comandos foram executados individualmente (§3), mas o roteiro **nao foi ensaiado de ponta a ponta** em sequencia, e o passo 10 depende das execucoes no GitHub |

## 2. Diagnostico inicial (antes das mudancas)

| Item | Estado encontrado |
|---|---|
| Componentes | 2 servicos Spring Boot 4 / Java 21 (`GestarAfeto` :8080 e `gestarafeto-alertas` :8081), front-end React/Vite, 2 PostgreSQL, RabbitMQ |
| Comunicacao | HTTP via Feign + circuit breaker (leituras/acoes) e eventos RabbitMQ (`checklist.alterado`, `gestante.removida`) |
| Docker | Somente `compose.yaml` com **infraestrutura** (2 bancos + RabbitMQ). Nenhuma aplicacao conteinerizada |
| Kubernetes | Nenhum manifest no repositorio (havia, no cluster local, um namespace `gestarafeto` de um experimento anterior, nao versionado; foi preservado e nao reutilizado) |
| Observabilidade | Nenhuma: so logs de console; sem trace, sem correlacao |
| CI/CD | Nenhum workflow |
| Testes | 89 testes (31 + 58); **6 eram ignorados silenciosamente** neste ambiente porque o Testcontainers 1.21.3 nao conecta ao Docker 29 |
| Riscos encontrados | Senha gerada do Spring Security no log; erros 4xx do cliente respondendo 500; `show-details=always` no perfil base; credenciais padrao em `compose.yaml`/`.env.example` (aceitaveis para desenvolvimento, nao para producao); ausencia de probes de saude |

## 3. Evidencias (saidas observadas)

**Compose (R1).** `docker compose --profile app ps`: `frontend`, `gestarafeto-app`,
`gestarafeto-alertas`, `postgres`, `postgres-alertas`, `rabbitmq` → `(healthy)`; `grafana`,
`jaeger`, `loki` → `Up`. `scripts/smoke.sh`:

```
[ok] servico principal pronto   [ok] gestante id=1   [ok] 46 itens no checklist
[ok] 22 alertas ativos calculados pelo microsservico
[ok] proxy do Nginx encaminhou a requisicao
```

**Kubernetes (R2).** Pods `Running` (2× app, 2× alertas, 2× frontend, 3 StatefulSets), PVCs
`Bound`, HPAs lendo `cpu: 8%/70%`.

- *Pod removido:* 30 requisicoes durante a substituicao → **30 OK, 0 falhas**.
- *Processo Java morto (`kill 1`):* `RESTARTS 1`, ultimo estado `Error`, pod volta `Ready`.
- *Rolling restart do servico principal, medido de dentro do cluster:* **120/120 OK**.
- *Banco reiniciado:* `GET /api/gestantes/3` → **200** depois de recriar `postgres-core-0`.
- *HPA sob carga (`load-generator`, 2 replicas × 8 laços):* alvo foi a `100%/70%` e as replicas
  subiram `2 → 3 → 4 → 5 → 6` em ~2 min; eventos `SuccessfulRescale … cpu resource utilization
  (percentage of request) above target`. Teto `maxReplicas: 6` atingido.
- *Escala manual:* `kubectl scale deployment/gestarafeto-app --replicas=4` resultou em 3/3 — o
  HPA (min 2) retomou o controle. Por isso a escala manual e documentada no frontend (sem HPA).
- *NetworkPolicy (pod sem rotulo permitido):* `postgres-core:5432` **BLOQUEADO**,
  `rabbitmq:5672` **BLOQUEADO**, `gestarafeto-app:8080` **BLOQUEADO**, `gestarafeto-frontend:80`
  **ABERTO** (unica porta publica por desenho). O CNI do Docker Desktop aplica a politica.
- *`k8s-down.sh` seguido de `k8s-up.sh`:* PVCs e Secrets preservados, segredos nao regerados.

**Observabilidade (R3).** Trace real do cluster:

```
GestarAfeto          http post /api/gestantes/{gestanteId}/checklist/gerar   644 ms
GestarAfeto            gestarafeto.eventos/checklist.alterado send             10 ms
gestarafeto-alertas    alertas.checklist-alterado receive                     234 ms
```

Logs correlacionados: `{service_name=~".+"} | trace_id="f8e37992…"` devolveu, em ordem, o
"Evento publicado" (GestarAfeto) e o "Evento recebido/processado" (gestarafeto-alertas).
Falha induzida (RabbitMQ com 0 replicas): o log `Falha ao publicar evento … UnknownHostException:
rabbitmq` apareceu no Loki como `severity_text=ERROR`, `service_name=GestarAfeto`, com o mesmo
`trace_id` do `X-Trace-Id` devolvido na resposta HTTP (que continuou 201 por desenho). A consulta
de erros por servico retornou series para os dois servicos.

**Testes e analise (R6).**

| Modulo | Testes | Aprovados | Falhas/Erros | Ignorados | Cobertura de linhas / ramos |
|---|---|---|---|---|---|
| Servico principal | 72 | 72 | 0 | 0 | 77,0 % / 48,7 % |
| Alertas | 74 | 74 | 0 | 0 | 88,7 % / 71,4 % |
| **Total** | **146** | **146** | **0** | **0** | — |

Medidos com JaCoCo 0.8.13 (`target/site/jacoco`). A cobertura de ramos do servico principal e
baixa: camadas de servico/controladores de dominio so sao exercitadas pelos fluxos principais.

**Verificacoes estaticas rodadas localmente:** `hadolint` (3 Dockerfiles), `actionlint`,
`kubeconform -strict` (39 recursos validos, 0 invalidos), `gitleaks` (6 commits, sem vazamentos),
`tsc --noEmit`, `vite build`, Checkstyle (0 violacoes).

## 4. Defeitos reais encontrados e corrigidos

| # | Defeito | Como apareceu | Correcao |
|---|---|---|---|
| 1 | Testcontainers 1.21.3 nao conecta ao Docker 29: 6 testes (Rabbit/PostgreSQL reais) ficavam **ignorados** sem aviso | Execucao dos testes com Docker ligado | Testcontainers 1.21.4 → os 6 passam |
| 2 | `RabbitTemplate` criado manualmente ignorava `observation-enabled`: o trace nao atravessava o broker | Jaeger mostrava dois traces separados | `setObservationEnabled(true)` em `RabbitProdutorConfig` |
| 3 | O Boot criava o exportador OTLP de logs mas nao ligava o Logback a ele: Loki vazio | `label/service_name` sem valores | Ponte `OpenTelemetryAppender.install` em `ObservabilidadeConfig` |
| 4 | Spring Security **gravava a senha gerada no log** | Busca por `password` no Loki | `UserDetailsService` vazio em `SecurityConfig` + teste |
| 5 | Service links do Kubernetes (`GESTARAFETO_ALERTAS_PORT=tcp://…`) quebravam `server.port` | `CrashLoopBackOff` no primeiro deploy | `enableServiceLinks: false` + teste de politica |
| 6 | Cluster rodava imagem antiga (tags `5.0.x` ja no cache do no) | Logs JSON/Zipkin de outra versao | Tag unica por build e carga explicita no no (`k8s-build-load.sh`) |
| 7 | Rota inexistente, metodo errado, JSON malformado e id invalido respondiam **500**, e o 500 nao era logado | Teste de tratamento de erros | `GlobalExceptionHandler` mapeia para 404/405/415/400 e registra o erro inesperado |
| 8 | Dashboard filtrava `detected_level="error"`; o Loki grava `ERROR` | Consulta contra o Loki real nao casava | `severity_text="ERROR"` |
| 9 | `container_name` fixo no compose conflitava com containers antigos e impedia escalar | `docker compose up` falhou | Removido |
| 10 | `HEALTHCHECK` em forma de shell (hadolint DL3025) | `hadolint` | Forma exec com `wget --spider` |

## 5. Pendencias e limitacoes

**Pendencias (dependem do usuario ou de acesso externo):**

1. **Enviar a branch ao GitHub e executar os workflows.** `gh` nao esta autenticado e o push foi
   deixado para o usuario. Sem isso, R5 e o passo 10 do roteiro (R9) nao podem ser "Validado".
2. Ensaiar o roteiro de ponta a ponta.

**Limitacoes assumidas (nao sao defeitos escondidos):**

- Cluster de uma maquina, um no: nao ha teste de falha de no nem de alta disponibilidade real.
- PostgreSQL e RabbitMQ com **uma replica**; Jaeger com armazenamento em memoria.
- Autenticacao: a API do servico principal e publica (`permitAll`), como no projeto original;
  nao foi alterada por estar fora do escopo.
- Sem TLS/Ingress; acesso por `port-forward`. Sem varredura de CVE das imagens nem assinatura.
- Sem metricas de aplicacao (Prometheus) nem alertas automaticos.
- As imagens `5.0.0` sao construidas localmente; so o CD (nao executado) publica em registry.
- A chave `management.endpoint.health.show-details=always` permanece no perfil **base** (dev);
  o perfil `prod` (usado nas imagens) a sobrescreve para `never`, coberto por teste.
- Os filtros de listagem de procedimentos (`tipo`, `trimestre`, `nome`) sao mutuamente
  exclusivos no servico original; mantido como estava.
- Sem teste automatizado de carga no CI; o HPA foi demonstrado manualmente.

## 6. Plano executado, por sprint

| Sprint | Objetivo | Arquivos principais | Criterio de aceite | Resultado |
|---|---|---|---|---|
| 1 — Diagnostico, organizacao e Docker | Conteinerizar os servicos | `Dockerfile` (×3), `.dockerignore` (×3), `frontend/nginx.conf`, `compose.yaml`, `scripts/smoke.sh`, probes do Actuator | Servicos `healthy` e fluxo ponta a ponta no Compose | Atendido (commit `8e77803`) |
| 3 — Observabilidade (feita antes do K8s por afetar a imagem) | Traces e logs correlacionados | `pom.xml` ×2, `logback-spring.xml` ×2, `ObservabilidadeConfig` ×2, `observability/*`, `RabbitProdutorConfig`, `SecurityConfig` | Trace unico entre servicos e logs por `trace_id` | Atendido (commit `17c6e23`) |
| 2 — Kubernetes | Orquestrar, escalar e recuperar | `k8s/base/*`, `k8s/overlays/local`, `observability/*.yaml`, `scripts/k8s-*.sh`, `k8s/load-test` | Pods prontos, HPA escalando, recuperacao de falhas | Atendido (commit `956f1b9`) |
| 4 — Testes e CI/CD | Portao de qualidade automatico | `.github/workflows/*`, `config/checkstyle.xml`, JaCoCo, 6 novas classes de teste + `FluxoApiPrincipalTest` | 146 testes verdes, workflows validos | Testes atendidos; execucao no GitHub **pendente** (commit `993dc08`) |
| 5 — Validacao e documentacao | Fechar a matriz e documentar | `docs/*.md`, `README.md`, `CHANGELOG.md`, `VERSION.md` | Documentacao reproduzivel | Atendido, com as pendencias da §5 |

A ordem 1 → 3 → 2 foi escolhida porque a instrumentacao altera as dependencias e portanto a
imagem; fazer o Kubernetes primeiro obrigaria a reconstruir e reimplantar para validar a
observabilidade.
