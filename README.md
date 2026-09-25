# GestarAfeto

GestarAfeto e um sistema Spring Boot + React para acompanhamento pre-natal. Na entrega TP2/PB o foco foi a camada de persistencia real: PostgreSQL, migrations Flyway, mapeamento JPA revisado, repositories Spring Data, auditoria Envers, testes e documentacao rastreavel. Na entrega **TP3/PB**, o calculo de alertas foi extraido para um **microsservico proprio**, com banco e repositorio dedicados, integrado por REST com Spring Cloud OpenFeign e circuit breaker Resilience4j. Na entrega **TP4/PB**, a sincronizacao entre os dois servicos foi refatorada para uma **arquitetura orientada a eventos** com RabbitMQ e Spring AMQP: o servico principal publica fatos e segue, e o microsservico consome quando puder.

O sistema e composto por dois servicos autonomos e um broker de mensagens:

| Componente | Porta | Banco | Responsabilidade |
|---|---|---|---|
| `GestarAfeto` (principal) | 8080 | `gestarafeto` | Gestantes, procedimentos, checklist, consultas, auditoria |
| `gestarafeto-alertas` | 8081 | `gestarafeto_alertas` | Priorizacao do checklist em alertas acionaveis |
| `rabbitmq` | 5672 / 15672 | — | Transporte dos eventos entre os dois servicos |

## Indice rapido

- [Arquitetura orientada a eventos](docs/ARQUITETURA_EVENTOS.md) — documento principal do TP4
- [Microsservico de alertas](docs/MICROSSERVICO_ALERTAS.md)
- [Arquitetura e diagramas](docs/ARCHITECTURE.md)
- [Design da persistencia](docs/PERSISTENCE.md)
- [Testes automatizados](docs/TESTING.md)
- [Exemplos de API](docs/API_EXAMPLES.md)
- [Historico por API](#historico-de-alteracoes)
- [Evidencias das rubricas](#evidencias-das-rubricas)

## Diagramas de arquitetura

Os diagramas estao escritos em Mermaid e sao **renderizados automaticamente pelo GitHub** (basta abrir o arquivo). Eles se dividem em dois documentos: os de mensageria em `ARQUITETURA_EVENTOS.md` e os de estrutura e persistencia em `ARCHITECTURE.md`.

Ha tambem uma versao interativa em `src/main/resources/static/docs.html`, servida em `http://localhost:8080/docs.html` quando a aplicacao esta no ar — ela cobre ate o TP3 e nao inclui a camada de eventos.

**Mensageria e eventos (TP4)** — em [`docs/ARQUITETURA_EVENTOS.md`](docs/ARQUITETURA_EVENTOS.md):

- [Visao geral da arquitetura](docs/ARQUITETURA_EVENTOS.md#2-visão-geral-da-arquitetura) · [topologia RabbitMQ](docs/ARQUITETURA_EVENTOS.md#3-topologia-rabbitmq)
- Fluxos: [checklist alterado](docs/ARQUITETURA_EVENTOS.md#61-checklist-alterado) · [reavaliacao sob demanda (202)](docs/ARQUITETURA_EVENTOS.md#62-reavaliação-sob-demanda-http-202) · [gestante removida](docs/ARQUITETURA_EVENTOS.md#63-gestante-removida) · [falha e dead letter](docs/ARQUITETURA_EVENTOS.md#64-falha-e-dead-letter)

**Estrutura e persistencia** — em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md):

- [Visao geral dos servicos](docs/ARCHITECTURE.md#visao-geral-dos-servicos)
- [Diagrama de componentes](docs/ARCHITECTURE.md#diagrama-de-componentes)
- [Diagrama de camadas](docs/ARCHITECTURE.md#diagrama-de-camadas)
- [Modelo ER](docs/ARCHITECTURE.md#modelo-er) · [modelo do microsservico](docs/ARCHITECTURE.md#modelo-do-microsservico-de-alertas)
- Sequencia: [cadastro de gestante](docs/ARCHITECTURE.md#sequencia-cadastro-de-gestante) · [geracao de checklist](docs/ARCHITECTURE.md#sequencia-geracao-de-checklist) · [atualizar status do checklist](docs/ARCHITECTURE.md#sequencia-atualizar-status-do-checklist) · [historico de auditoria](docs/ARCHITECTURE.md#sequencia-historico-de-auditoria) · [concorrencia com lock otimista](docs/ARCHITECTURE.md#sequencia-concorrencia-com-lock-otimista)

> As sequencias de integracao em `ARCHITECTURE.md` ([sincronizacao automatica](docs/ARCHITECTURE.md#sequencia-sincronizacao-automatica-apos-mudanca-no-checklist) e [circuit breaker](docs/ARCHITECTURE.md#sequencia-microsservico-indisponivel-circuit-breaker)) descrevem a integracao **sincrona do TP3**, mantida como registro historico. O comportamento atual da sincronizacao esta em `ARQUITETURA_EVENTOS.md`.

## Tecnologias

Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, Hibernate, Hibernate Envers, Flyway, PostgreSQL, H2 para testes rapidos, Testcontainers, Maven, React, Vite e TypeScript.

Na camada distribuida: **Spring Cloud 2025.1.2** — OpenFeign (cliente REST declarativo), CircuitBreaker com Resilience4j (resiliencia) e spring-cloud-context/config (configuracao distribuida e recarregavel).

Na mensageria: **RabbitMQ 3.13** com **Spring AMQP 4** (`spring-boot-starter-amqp`) — exchange topic, filas duraveis, bindings por routing key, dead letter queues, publisher confirms e repeticao com espera crescente.

## Execucao com PostgreSQL

1. Crie um `.env` a partir de `.env.example`.
2. Inicie a infraestrutura (dois bancos e o broker):

```powershell
docker compose up -d
```

3. Inicie o servico principal com profile PostgreSQL:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Em outro terminal, inicie o microsservico de alertas (consumidor das filas):

```powershell
cd alertas-service
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Os volumes `gestarafeto_postgres_data` e `gestarafeto_alertas_postgres_data` preservam os dados entre reinicios. Cada servico tem suas proprias migrations Flyway (`src/main/resources/db/migration`) e usa `ddl-auto=validate` nos perfis PostgreSQL.

Para uma execucao rapida sem PostgreSQL, ambos aceitam o profile `dev-h2`:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

O `dev-h2` dispensa apenas o banco: o **RabbitMQ continua necessario** para a sincronizacao por eventos. Sem ele, o sistema sobe e opera normalmente, mas os alertas nao sao atualizados.

O servico principal **funciona com o microsservico desligado**, e desde o TP4 isso vale tambem para as escritas:

- **Escritas** (mudancas de checklist, remocao de gestante) sao publicadas no RabbitMQ. Com o consumidor fora do ar, as mensagens **ficam na fila** e sao processadas quando ele volta — nada se perde.
- **Leituras** (listar alertas, resumo) degradam para vazio pelo circuit breaker, e a aba Alertas aparece sem conteudo ate o microsservico retornar.

O broker, por outro lado, e necessario para a sincronizacao: sem ele, a publicacao falha e a mensagem se perde, embora a operacao do usuario conclua normalmente. Ver [limitacoes](#limitacoes-conhecidas).

## Testes

```powershell
.\mvnw.cmd test
```

```powershell
cd alertas-service
.\mvnw.cmd test
```

Sao 31 testes no servico principal e 58 no microsservico (89 no total). A suite cobre repositories, integridade, ordenacao, auditoria, regras de alerta, publicacao e consumo de eventos, e a integracao entre os servicos.

Duas classes de teste usam **Testcontainers** e sao ignoradas automaticamente quando nao ha Docker disponivel:

- `MigrationIntegrationTest` — aplica as migrations Flyway em um PostgreSQL real.
- `EventoRabbitMqIntegrationTest` — sobe um **RabbitMQ real** e percorre o caminho completo da mensagem: topologia declarada, publicacao, roteamento, fila, consumo, routing key nao vinculada e mensagem invalida terminando na DLQ.

## Historico de alteracoes

As entidades `Gestante`, `ProcedimentoPreNatal`, `ItemChecklistGestante` e `ConsultaPreNatal` usam `@Audited`. O historico pode ser consultado por API:

```http
GET /api/auditoria/gestante/1
GET /api/auditoria/procedimento/1
GET /api/auditoria/checklist/1
GET /api/auditoria/consulta/1
```

Cada resposta traz numero da revisao, data, tipo de alteracao (`ADD`, `MOD`, `DEL`) e snapshot dos dados.

## Exemplos de uso dos repositories

Controllers nao acessam repositories diretamente. O fluxo e sempre:

```text
Controller -> Service -> Repository -> PostgreSQL
```

Exemplos reais:

- `GestanteService.cadastrar()` usa `GestanteRepository.save()` para persistir cadastro validado.
- `GestanteRepository.findByNomeContainingIgnoreCase(nome, pageable)` pagina pesquisa por nome.
- `ProcedimentoRepository.findByAtivoTrue(Pageable)` lista catalogo ativo.
- `ItemChecklistGestanteRepository.findByGestanteIdAndStatus(...)` filtra checklist por relacionamento e status.
- `ItemChecklistGestanteRepository.findById(...)` usa `@EntityGraph` para carregar gestante e procedimento sem N+1.
- `ConsultaPreNatalRepository.findByGestanteIdOrderByDataConsultaDesc(...)` retorna historico clinico em ordem temporal.
- `AuditoriaService.historico(...)` usa Envers para consultar revisoes.

## Alertas do pre-natal

O microsservico `gestarafeto-alertas` le o checklist de uma gestante, estima a semana gestacional (a partir da DUM ou da DPP) e classifica cada item pendente:

| Situacao | Tipo de alerta | Prioridade |
|---|---|---|
| Janela recomendada ja passou | `PROCEDIMENTO_ATRASADO` | ALTA se obrigatorio |
| Janela recomendada em curso | `PROCEDIMENTO_PENDENTE` | MEDIA se obrigatorio |
| Janela comeca em ate 2 semanas | `JANELA_PROXIMA` | BAIXA |
| Item marcado para revisao | `REVISAO_SOLICITADA` | ALTA |

A reavaliacao e **idempotente**: alertas ja lidos preservam esse estado, alertas cuja condicao desapareceu viram `RESOLVIDO` (nunca sao apagados) e itens que voltam a ficar pendentes reabrem o alerta original. Essa propriedade e o que torna seguro o reprocessamento de mensagens reentregues pelo broker.

```http
POST   /api/gestantes/1/alertas/avaliar   -> 202 Accepted (assincrono, publica evento)
GET    /api/gestantes/1/alertas           -> 200 (sincrono)
GET    /api/gestantes/1/alertas/resumo    -> 200 (sincrono)
PATCH  /api/alertas/5/leitura             -> 200 (sincrono)
PATCH  /api/alertas/5/resolucao           -> 200 (sincrono)
```

Detalhes de arquitetura, contratos e decisoes em [`docs/MICROSSERVICO_ALERTAS.md`](docs/MICROSSERVICO_ALERTAS.md).

## Arquitetura orientada a eventos (TP4)

A sincronizacao entre os servicos deixou de ser uma chamada HTTP sincrona e passou a ser troca de mensagens no RabbitMQ. O servico principal publica o fato e retorna; o microsservico consome quando puder.

| Routing key | Publicada quando | Consumida por |
|---|---|---|
| `checklist.alterado` | Checklist gerado ou item com status alterado | `alertas.checklist-alterado` |
| `gestante.removida` | Gestante excluida | `alertas.gestante-removida` |

Topologia: exchange **topic** `gestarafeto.eventos`, filas duraveis por evento, bindings por routing key e uma DLQ por fila via `gestarafeto.eventos.dlx`. Tudo declarado em codigo e criado na subida, sem passo manual no broker.

O que **continua sincrono**, de proposito: listar alertas, resumo, marcar como lido e resolver. A regra aplicada foi *quem provoca trabalho vira evento; quem precisa de resposta agora continua REST*.

```http
POST /api/gestantes/1/alertas/avaliar   -> 202 Accepted (publica o evento)
GET  /api/gestantes/1/alertas           -> 200 (consulta sincrona)
```

Com o microsservico fora do ar, as operacoes continuam respondendo normalmente e as mensagens se acumulam na fila, sendo processadas quando ele volta. Detalhes, diagramas e decisoes em [`docs/ARQUITETURA_EVENTOS.md`](docs/ARQUITETURA_EVENTOS.md).

## Evidencias das rubricas

| Rubrica | Evidencia no codigo | Evidencia documental | Teste |
|---|---|---|---|
| Modelagem e isolamento | entidades em `gestante`, `procedimento`, `checklist`, `consulta` | `docs/PERSISTENCE.md` | repositories |
| JPA e Spring Data | `@Entity`, `@Version`, `@Audited`, repositories | `docs/PERSISTENCE.md` | `*RepositoryTest` |
| Persistencia real | `compose.yaml`, profiles PostgreSQL, Flyway | README + `docs/PERSISTENCE.md` | `MigrationIntegrationTest` |
| Integridade | constraints, checks, unique checklist | `docs/PERSISTENCE.md` | checklist/consulta tests |
| Historico | Envers + `/api/auditoria/{entidade}/{id}` | README + arquitetura | `AuditoriaIntegrationTest` |
| Testes | suites em `src/test/java` e `alertas-service/src/test/java` | `docs/TESTING.md` | `mvn test` |
| Documentacao | README na raiz + diagramas Mermaid | `docs/ARCHITECTURE.md` | revisao de links |
| **Modelo de dominio atualizado** | contexto `Alerta` com referencias logicas, sem FK entre bancos | `docs/MICROSSERVICO_ALERTAS.md` | `AlertaRepositoryTest` |
| **Endpoints REST do microsservico** | `alertas-service` `AlertaController` + fachada `alerta/controller` | `docs/API_EXAMPLES.md` | `AlertaControllerTest` (ambos) |
| **Microsservico com Spring Boot** | modulo `alertas-service/` | `docs/MICROSSERVICO_ALERTAS.md` | `AlertasServiceApplicationTests` |
| **Spring Cloud** | OpenFeign, CircuitBreaker Resilience4j, config/context | `docs/MICROSSERVICO_ALERTAS.md#uso-do-spring-cloud` | `AlertaClientFallbackFactoryTest` |
| **Repositorio dedicado** | `AlertaRepository` sobre banco proprio | `docs/MICROSSERVICO_ALERTAS.md` | `AlertaRepositoryTest` |
| **Front-end do microsservico** | `AlertasPage`, `AlertaCard`, `alertaService` | README + arquitetura | `npm run build` |
| **Testes do microsservico** | 58 testes em `alertas-service` | `docs/TESTING.md` | `mvn test` |
| **Arquitetura orientada a eventos** | `mensageria/` nos dois servicos | `docs/ARQUITETURA_EVENTOS.md` | `EventoRabbitMqIntegrationTest` |
| **Mensagens e fluxos de eventos** | `mensageria/evento/*Message.java` | `docs/ARQUITETURA_EVENTOS.md#4-estrutura-das-mensagens` | `AlertaIntegracaoServiceTest` |
| **Exchanges, filas, bindings, routing keys** | `RabbitConsumidorConfig`, `TopologiaEventos` | `docs/ARQUITETURA_EVENTOS.md#3-topologia-rabbitmq` | `EventoRabbitMqIntegrationTest` |
| **Produtores e consumidores** | `EventoPublisher`, `*Consumer` | `docs/ARQUITETURA_EVENTOS.md` | `ChecklistAlteradoConsumerTest` |
| **Spring Boot + Spring AMQP** | `RabbitProdutorConfig`, `@RabbitListener` | `docs/ARQUITETURA_EVENTOS.md#5-integracao-com-spring-boot-e-spring-amqp` | suites dos dois servicos |
| **Reducao de acoplamento** | `AlertaClient` encolhido; escrita virou evento | `docs/ARQUITETURA_EVENTOS.md#1-o-que-mudou-e-por-que` | `ChecklistAlteradoListenerTest` |

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

A SPA tem quatro abas: Gestantes, Procedimentos, Checklist e **Alertas**. A aba Alertas consome o microsservico atraves do servico principal, exibindo contagens por prioridade, filtros e acoes de leitura/resolucao.

O botao **Reavaliar Alertas** reflete a natureza assincrona da operacao: a chamada recebe **202 Accepted**, a tela confirma que a solicitacao foi aceita e em seguida consulta a lista algumas vezes ate o resultado chegar. E a consistencia eventual aparecendo na interface, e nao um defeito.

O backend continua expondo as rotas REST anteriores, com rotas adicionais para paginacao, auditoria e alertas.

## Limitacoes conhecidas

- **Consistencia eventual.** Os alertas podem ficar defasados por instantes apos uma alteracao do checklist. Aceito por decisao de projeto: nenhuma decisao clinica depende de o alerta estar atualizado no mesmo segundo, e a interface informa que a atualizacao vem "em instantes".
- **Janela de perda na publicacao.** O evento e publicado depois do commit da operacao. Se o broker estiver fora do ar nesse instante exato, a mensagem se perde — a operacao do usuario ja foi concluida e nao pode ser revertida por um efeito colateral. O estado se recompoe na alteracao seguinte, porque o consumo e idempotente. Fechar a janela exigiria um **outbox transacional**, registrado como evolucao.
- **Entrega ao menos uma vez.** O RabbitMQ pode reentregar a mesma mensagem. O consumo foi desenhado para ser idempotente por construcao, mas isso e uma propriedade a preservar em qualquer consumidor novo.
- **Ordem nao garantida.** Duas alteracoes em sequencia rapida podem ser processadas fora de ordem. Sem impacto aqui, porque cada mensagem carrega o estado completo e a ultima processada estabelece o resultado final.
- **Autenticacao nas revisoes Envers.** As revisoes registram o que mudou e quando, mas nao quem alterou.
- **Config Server ausente.** O cliente esta configurado, porem desligado por padrao; nao ha um Config Server no `compose.yaml`.
