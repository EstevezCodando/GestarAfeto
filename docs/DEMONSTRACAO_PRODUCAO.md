# Roteiro de demonstracao operacional (v5.0.0)

Duracao estimada: 25–30 minutos. Todos os comandos usam o contexto `docker-desktop`; com kind,
troque por `kind-<cluster>`. Os "resultados esperados" sao os observados nas execucoes reais
registradas em [`RASTREABILIDADE.md`](RASTREABILIDADE.md) (numeros como latencias variam).

Dica: abra 3 janelas — **terminal**, **Grafana/Jaeger** no navegador e **`kubectl get pods -w`**.

## Preparacao (antes da banca)

```bash
docker info > /dev/null && echo "Docker ok"
kubectl --context docker-desktop get nodes            # STATUS Ready
bash scripts/k8s-build-load.sh                        # imagens na tag do commit atual
bash scripts/k8s-up.sh                                # ~3 min na primeira vez; guarde a senha do Grafana
```

Abra os acessos (cada um em um terminal; reabra se um pod for substituido):

```bash
kubectl --context docker-desktop -n gestarafeto-prod port-forward svc/gestarafeto-frontend 13080:80
kubectl --context docker-desktop -n gestarafeto-prod port-forward svc/gestarafeto-app 18080:8080
kubectl --context docker-desktop -n gestarafeto-obs  port-forward svc/grafana 23000:3000
kubectl --context docker-desktop -n gestarafeto-obs  port-forward svc/jaeger 26686:16686
```

---

## 1. Arquitetura geral (2 min)

**Mostrar:** o diagrama de [`PRODUCAO.md`](PRODUCAO.md#1-arquitetura-final).

**Dizer:** tres servicos (front-end, principal, alertas), dois bancos independentes, RabbitMQ
entre os servicos, observabilidade em namespace proprio. Principal e alertas conversam de duas
formas: **eventos** (RabbitMQ) para o que muda o estado e **HTTP com circuit breaker** para
leituras.

## 2. Construcao dos conteineres (3 min)

```bash
docker build -t gestarafeto/app:demo .
docker history gestarafeto/app:demo --format '{{.Size}}\t{{.CreatedBy}}' | head -8
docker run --rm --entrypoint id gestarafeto/app:demo     # usuario efetivo do container
```

**Esperado:** build multi-etapa (cache acelera o segundo build); a imagem final nao contem
Maven nem JDK; `uid=1001(spring)` — nao e root. Mostrar `Dockerfile` e `.dockerignore`.

Em Docker Compose (alternativa rapida, sem Kubernetes):

```bash
docker compose --profile app up -d --build && docker compose --profile app ps
```

**Esperado:** todos os servicos `(healthy)`.

## 3. Implantacao Kubernetes (4 min)

```bash
kubectl --context docker-desktop -n gestarafeto-prod get pods,svc,hpa,pvc
kubectl --context docker-desktop -n gestarafeto-obs get pods
```

**Esperado:** `gestarafeto-app` e `gestarafeto-alertas` com 2/2 `Running`; frontend 2/2; tres
StatefulSets (`postgres-core-0`, `postgres-alertas-0`, `rabbitmq-0`) com PVC `Bound`; HPAs com
`cpu: x%/70%` (nao `<unknown>`).

Mostrar um manifest (`k8s/base/app.yaml`) apontando: probes, `requests/limits`,
`securityContext`, HPA. Mostrar que **nao ha senha** nos YAMLs:

```bash
grep -rn "kind: Secret" k8s/ observability/ || echo "nenhum Secret versionado"
kubectl --context docker-desktop -n gestarafeto-prod get secret gestarafeto-secrets
```

## 4. Funcionamento e comunicacao entre os servicos (4 min)

```bash
APP_URL=http://localhost:18080 FRONT_URL=http://localhost:13080 bash scripts/smoke.sh
```

**Esperado:**

```
[ok] servico principal pronto
[ok] gestante id=N
[ok] 46 itens no checklist
[ok] 22 alertas ativos calculados pelo microsservico
[ok] proxy do Nginx encaminhou a requisicao
== Fumaca concluida com sucesso
```

**Explicar:** os 22 alertas foram calculados pelo *outro* servico, que recebeu o evento
`checklist.alterado` pelo RabbitMQ. Painel (opcional):
`kubectl ... port-forward svc/rabbitmq 15672:15672` → Exchanges → `gestarafeto.eventos`.

Abrir http://localhost:13080 e navegar na aplicacao (cadastrar gestante, gerar checklist, aba Alertas).

## 5. Disponibilidade e auto-recuperacao (4 min)

**a) Pod morre durante o uso**

```bash
kubectl --context docker-desktop -n gestarafeto-prod get pods -l app.kubernetes.io/name=gestarafeto-app
kubectl --context docker-desktop -n gestarafeto-prod delete pod <um-dos-pods>
kubectl --context docker-desktop -n gestarafeto-prod get pods -w
```

**Esperado:** o ReplicaSet cria outro pod na hora; o outro continua atendendo (observado:
30/30 requisicoes OK durante a troca).

**b) O processo Java cai dentro do container**

```bash
kubectl --context docker-desktop -n gestarafeto-prod exec <pod-alertas> -- kill 1
kubectl --context docker-desktop -n gestarafeto-prod get pod <pod-alertas>     # REINICIOS 1, motivo Error
```

**Esperado:** o kubelet reinicia o container (`RESTARTS` sobe para 1) sem intervencao.

**c) Atualizacao sem indisponibilidade** (opcional)

```bash
kubectl --context docker-desktop -n gestarafeto-prod rollout restart deployment/gestarafeto-app
kubectl --context docker-desktop -n gestarafeto-prod rollout status deployment/gestarafeto-app
```

**Esperado:** pods trocados um a um (`maxUnavailable: 0`); medido de dentro do cluster:
120/120 requisicoes OK durante o rollout.

**d) Dados sobrevivem ao banco**

```bash
kubectl --context docker-desktop -n gestarafeto-prod delete pod postgres-core-0
# apos Ready: a gestante criada antes continua la (GET /api/gestantes/{id} → 200)
```

## 6. Escalabilidade (5 min)

**a) Manual**

```bash
kubectl --context docker-desktop -n gestarafeto-prod scale deployment/gestarafeto-frontend --replicas=4
kubectl --context docker-desktop -n gestarafeto-prod get deploy gestarafeto-frontend
kubectl --context docker-desktop -n gestarafeto-prod scale deployment/gestarafeto-frontend --replicas=2
```

**b) Automatica (HPA) sob carga**

```bash
kubectl --context docker-desktop -n gestarafeto-prod get hpa -w &        # janela separada
kubectl --context docker-desktop apply -f k8s/load-test/load-generator.yaml
```

**Esperado (observado):** em ~20 s o alvo passa de `2%/70%` para acima de 100% e o HPA
escala `2 → 3 → 4 → 5 → 6` em cerca de 2 min (`SuccessfulRescale … above target`), limitado
por `maxReplicas: 6`. Ao remover a carga:

```bash
kubectl --context docker-desktop delete -f k8s/load-test/load-generator.yaml
```

o HPA volta a `2` apos a janela de estabilizacao de 60 s. Evidencia: `kubectl describe hpa gestarafeto-app`.

## 7. Logs centralizados (3 min)

Grafana → http://localhost:23000 (usuario `admin`; senha impressa pelo `k8s-up.sh` ou lida do
Secret, ver `PRODUCAO.md`) → dashboard **GestarAfeto - Logs e erros**.

Para provocar um erro **real** e achar por servico:

```bash
kubectl --context docker-desktop -n gestarafeto-prod scale statefulset/rabbitmq --replicas=0
# crie uma gestante e gere o checklist pela interface ou:
curl -si -X POST localhost:18080/api/gestantes/<id>/checklist/gerar | grep -i x-trace-id
kubectl --context docker-desktop -n gestarafeto-prod scale statefulset/rabbitmq --replicas=1
```

No Grafana → Explore → Loki:

```logql
{service_name=~".+"} | severity_text="ERROR"
```

**Esperado:** `Falha ao publicar evento … UnknownHostException: rabbitmq` no servico
`GestarAfeto` (e reconexoes no `gestarafeto-alertas`), cada linha com `trace_id`. A resposta
HTTP continua **201**: a falha do broker nao derruba a operacao do usuario (decisao do TP4).

## 8. Rastreamento de transacoes (3 min)

```bash
curl -si -X POST localhost:18080/api/gestantes/<id>/checklist/gerar | grep -i x-trace-id
```

Jaeger → http://localhost:26686 → buscar pelo **Trace ID** do cabecalho (ou servico
`GestarAfeto`, operacao `http post /api/gestantes/{gestanteId}/checklist/gerar`).

**Esperado:** um unico trace com spans dos **dois servicos**:

```
GestarAfeto         http post …/checklist/gerar
GestarAfeto           gestarafeto.eventos/checklist.alterado send
gestarafeto-alertas   alertas.checklist-alterado receive
```

Em seguida, no Grafana, colar o mesmo id: `{service_name=~".+"} | trace_id="<id>"` — os logs
dos dois servicos aparecem juntos. Apontar a duracao de cada span (gargalos).

## 9. Testes (2 min)

```bash
./mvnw -B verify                      # servico principal
cd alertas-service && ./mvnw -B verify
```

**Esperado:** `Tests run: 72, Failures: 0, Errors: 0, Skipped: 0` e `Tests run: 74, …`;
`0 Checkstyle violations`; relatorios de cobertura em `target/site/jacoco/index.html`.
Mostrar `docs/TESTING.md` para a organizacao (unitarios, integracao, API, mensageria com
RabbitMQ real, manifests Kubernetes, erros HTTP).

## 10. CI/CD (2 min)

Mostrar `.github/workflows/ci.yml` e `cd.yml` e a aba **Actions** do GitHub com as execucoes
(**so possivel apos enviar a branch**; ver [`CICD.md`](CICD.md#4-como-validar-de-fato-passo-que-falta)).
Pontos: jobs em paralelo, `mvn verify` como portao, build de imagens, kind efemero com
`smoke.sh`, publicacao no GHCR e manifests anexados ao Release.

---

## Encerramento

```bash
kill %1 %2 2>/dev/null; # port-forwards
bash scripts/k8s-down.sh            # preserva dados
# bash scripts/k8s-down.sh --tudo   # remove tudo, inclusive dados
```

## Plano B (se algo falhar ao vivo)

| Problema | Acao |
|---|---|
| Rede/Docker Hub lento | As imagens ja estao no no; nao rode `docker pull` na banca |
| `port-forward` caiu apos matar pod | Reabra o comando (afeta so o encaminhamento) |
| HPA nao escala | `kubectl top pods` deve mostrar CPU; confira se o `load-generator` esta `Running` |
| Cluster lento | Reduza o gerador de carga para 1 replica ou use `k8s-down.sh` e suba novamente |
| Sem rede para o GitHub | Mostre os workflows e o resultado local de cada job (`CICD.md` §3) |
