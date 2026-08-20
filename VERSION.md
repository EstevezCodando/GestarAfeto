# GestarAfeto - Controle de Versao

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
