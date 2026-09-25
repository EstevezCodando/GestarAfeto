# GestarAfeto - Controle de Versao

## v4.0.0 - TP4/PB - Arquitetura orientada a eventos

### Implementado

- RabbitMQ 3.13 como broker de mensagens, subindo junto no `compose.yaml` com painel em `:15672`.
- Integracao via Spring AMQP 4 (`spring-boot-starter-amqp`) nos dois servicos.
- Exchange topic `gestarafeto.eventos` e exchange de dead letter `gestarafeto.eventos.dlx`.
- Routing keys `checklist.alterado` e `gestante.removida`.
- Filas duraveis `alertas.checklist-alterado` e `alertas.gestante-removida`, uma por evento,
  cada uma com a propria DLQ. Topologia declarada em codigo e criada na subida.
- Mensagens em JSON com envelope de rastreamento: `eventoId`, `tipo`, `versao`, `ocorridoEm`
  e `origem`. O evento de checklist carrega o estado completo, de modo que o consumidor
  processa sem nenhuma chamada de volta ao produtor.
- `EventoPublisher` no servico principal, com publisher confirms, returns e repeticao.
- `ChecklistAlteradoConsumer` e `GestanteRemovidaConsumer` com `@RabbitListener`, repeticao
  com espera crescente e rejeicao direta para DLQ em payload invalido.
- Refatoracao do acoplamento: `avaliar` e `removerPorGestante` sairam do cliente Feign e
  viraram eventos. `POST /api/gestantes/{id}/alertas/avaliar` passou a responder **202**.
- Leituras (listar, resumo, marcar lido, resolver) permanecem sincronas por decisao de projeto.
- Front-end ajustado para o 202, com atualizacao posterior refletindo a consistencia eventual.
- 89 testes no total, incluindo um ponta a ponta com RabbitMQ real via Testcontainers.
- Documentacao: `docs/ARQUITETURA_EVENTOS.md` e atualizacoes em README, TESTING e API_EXAMPLES.

### Observacoes

- Consistencia eventual assumida: os alertas podem ficar defasados por instantes apos uma
  alteracao, o que e aceitavel porque nenhuma decisao clinica depende disso em tempo real.
- Permanece uma janela de perda: se o broker estiver fora do ar no instante exato da
  publicacao, a mensagem se perde. O estado se recompoe na alteracao seguinte, porque o
  consumo e idempotente. Um outbox transacional eliminaria a janela e fica como evolucao.
- Entrega ao menos uma vez: o consumo foi desenhado para ser idempotente.

## v3.0.0 - TP3/PB - Microsservico de alertas

### Implementado

- Microsservico `gestarafeto-alertas` (Spring Boot 4, Java 21) em `alertas-service/`, com
  processo, porta (8081) e banco (`gestarafeto_alertas`, porta 5433) proprios.
- Modelo de dominio atualizado: novo bounded context `Alerta`, com referencias logicas a
  gestante e ao item de checklist, sem chave estrangeira entre bancos.
- `MotorDeRegrasAlerta`: estima a semana gestacional (DUM ou DPP) e classifica itens em
  `PROCEDIMENTO_ATRASADO`, `PROCEDIMENTO_PENDENTE`, `JANELA_PROXIMA` e `REVISAO_SOLICITADA`,
  com prioridade ALTA, MEDIA ou BAIXA.
- Reavaliacao idempotente: reconciliacao por `(origemId, tipo)` que cria, atualiza, reabre ou
  resolve alertas preservando o status de leitura.
- Repositorio dedicado `AlertaRepository`, com filtros, paginacao, contagem agregada e
  ordenacao por severidade feita no banco.
- API REST do microsservico em `/api/alertas` e fachada no servico principal em
  `/api/gestantes/{id}/alertas` e `/api/alertas/{id}/...`.
- Spring Cloud 2025.1.2: OpenFeign para o cliente declarativo, CircuitBreaker Resilience4j
  para resiliencia e spring-cloud-context/config para configuracao distribuida recarregavel
  via `/actuator/refresh`.
- Fallback com degradacao assimetrica: leituras seguem vazias, acoes explicitas da usuaria
  retornam 503.
- Sincronizacao automatica por eventos de dominio (`ChecklistAlteradoEvent`,
  `GestanteRemovidaEvent`) entregues em `AFTER_COMMIT`, mantendo checklist e gestante sem
  conhecimento do microsservico.
- Front-end: aba **Alertas** com painel de contagens por prioridade, filtros e acoes de
  leitura/resolucao (`AlertasPage`, `AlertaCard`, `alertaService`, `types/Alerta`).
- `compose.yaml` com um banco por servico e volumes independentes.
- 44 testes no microsservico e 14 novos no servico principal (71 no total).
- Documentacao: `docs/MICROSSERVICO_ALERTAS.md` e atualizacoes em `ARCHITECTURE.md`,
  `PERSISTENCE.md`, `TESTING.md`, `API_EXAMPLES.md` e README.

### Observacoes

- Os alertas sao eventualmente consistentes: uma indisponibilidade do microsservico atrasa a
  atualizacao ate a proxima reavaliacao, que reconcilia tudo de uma vez.
- A integracao e sincrona (REST). Uma fila de mensagens seria o proximo passo natural.
- O cliente de Config Server esta configurado, porem desligado por padrao
  (`GESTARAFETO_CONFIG_ENABLED=false`); nao ha Config Server no `compose.yaml`.
- O servico principal continua operando normalmente com o microsservico desligado.

## v2.0.0 - TP2/PB - Camada de persistencia real

### Implementado

- PostgreSQL como banco real de desenvolvimento e demonstracao.
- `compose.yaml` com volume Docker persistente.
- Profiles `dev`, `dev-h2`, `test` e `prod`.
- Flyway com migrations para tabelas principais, constraints, indices, seed de procedimentos e tabelas Envers.
- Entidades JPA revisadas com `@Version`, `@Audited`, `@EntityListeners`, `@CreatedDate` e `@LastModifiedDate`.
- Repositories Spring Data com filtros, paginacao e `@EntityGraph`.
- Validacoes de integridade em services e constraints/checks no banco.
- Historico consultavel em `/api/auditoria/{entidade}/{id}`.
- Tratamento global para violacao de integridade e conflito otimista.
- Testes de repositories, integridade, auditoria e migrations com Testcontainers.
- README na raiz com evidencias das rubricas e links para diagramas.
- Documentos `docs/ARCHITECTURE.md`, `docs/PERSISTENCE.md`, `docs/TESTING.md` e `docs/API_EXAMPLES.md`.

### Observacoes

- H2 permanece apenas para desenvolvimento rapido e testes locais.
- Autenticacao do usuario que realizou cada revisao ainda nao foi implementada.
