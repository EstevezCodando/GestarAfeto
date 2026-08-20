# GestarAfeto

GestarAfeto e um sistema Spring Boot + React para acompanhamento pre-natal. Na entrega TP2/PB o foco foi a camada de persistencia real: PostgreSQL, migrations Flyway, mapeamento JPA revisado, repositories Spring Data, auditoria Envers, testes e documentacao rastreavel. Na entrega **TP3/PB**, o calculo de alertas foi extraido para um **microsservico proprio**, com banco e repositorio dedicados, integrado por REST com Spring Cloud OpenFeign e circuit breaker Resilience4j.

O sistema passa a ter dois servicos:

| Servico | Porta | Banco | Responsabilidade |
|---|---|---|---|
| `GestarAfeto` (principal) | 8080 | `gestarafeto` | Gestantes, procedimentos, checklist, consultas, auditoria |
| `gestarafeto-alertas` | 8081 | `gestarafeto_alertas` | Priorizacao do checklist em alertas acionaveis |

## Indice rapido

- [Microsservico de alertas](docs/MICROSSERVICO_ALERTAS.md)
- [Arquitetura e diagramas](docs/ARCHITECTURE.md)
- [Design da persistencia](docs/PERSISTENCE.md)
- [Testes automatizados](docs/TESTING.md)
- [Exemplos de API](docs/API_EXAMPLES.md)
- [Historico por API](#historico-de-alteracoes)
- [Evidencias das rubricas](#evidencias-das-rubricas)

## Diagramas de arquitetura

Todos os diagramas estao em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), escritos em Mermaid e **renderizados automaticamente pelo GitHub** (basta abrir o arquivo). Ha tambem uma versao interativa em `src/main/resources/static/docs.html`, servida em `http://localhost:8080/docs.html` quando a aplicacao esta no ar.

- [Visao geral dos servicos](docs/ARCHITECTURE.md#visao-geral-dos-servicos)
- [Diagrama de componentes](docs/ARCHITECTURE.md#diagrama-de-componentes)
- [Diagrama de camadas](docs/ARCHITECTURE.md#diagrama-de-camadas)
- [Modelo ER](docs/ARCHITECTURE.md#modelo-er) · [modelo do microsservico](docs/ARCHITECTURE.md#modelo-do-microsservico-de-alertas)
- Sequencia: [cadastro de gestante](docs/ARCHITECTURE.md#sequencia-cadastro-de-gestante) · [geracao de checklist](docs/ARCHITECTURE.md#sequencia-geracao-de-checklist) · [atualizar status do checklist](docs/ARCHITECTURE.md#sequencia-atualizar-status-do-checklist) · [historico de auditoria](docs/ARCHITECTURE.md#sequencia-historico-de-auditoria) · [concorrencia com lock otimista](docs/ARCHITECTURE.md#sequencia-concorrencia-com-lock-otimista)
- Sequencia (microsservico): [avaliacao de alertas](docs/ARCHITECTURE.md#sequencia-avaliacao-de-alertas-no-microsservico) · [sincronizacao automatica](docs/ARCHITECTURE.md#sequencia-sincronizacao-automatica-apos-mudanca-no-checklist) · [circuit breaker](docs/ARCHITECTURE.md#sequencia-microsservico-indisponivel-circuit-breaker)

## Tecnologias

Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, Hibernate, Hibernate Envers, Flyway, PostgreSQL, H2 para testes rapidos, Testcontainers, Maven, React, Vite e TypeScript.

Na camada distribuida: **Spring Cloud 2025.1.2** — OpenFeign (cliente REST declarativo), CircuitBreaker com Resilience4j (resiliencia) e spring-cloud-context/config (configuracao distribuida e recarregavel).

## Execucao com PostgreSQL

1. Crie um `.env` a partir de `.env.example`.
2. Inicie os dois bancos (um por servico):

```powershell
docker compose up -d
```

3. Inicie o servico principal com profile PostgreSQL:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Em outro terminal, inicie o microsservico de alertas:

```powershell
cd alertas-service
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Os volumes `gestarafeto_postgres_data` e `gestarafeto_alertas_postgres_data` preservam os dados entre reinicios. Cada servico tem suas proprias migrations Flyway (`src/main/resources/db/migration`) e usa `ddl-auto=validate` nos perfis PostgreSQL.

Para uma execucao rapida sem PostgreSQL, ambos aceitam o profile `dev-h2`:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

O servico principal **funciona com o microsservico desligado**: a aba Alertas fica vazia e o restante do sistema segue normal, graças ao circuit breaker e ao fallback.

## Testes

```powershell
.\mvnw.cmd test
```

```powershell
cd alertas-service
.\mvnw.cmd test
```

Sao 27 testes no servico principal e 44 no microsservico. A suite inclui testes de repositories, integridade, ordenacao, auditoria, regras de alerta, integracao entre servicos e migrations com PostgreSQL via Testcontainers. O teste de Testcontainers e desabilitado automaticamente se Docker nao estiver disponivel.

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

A reavaliacao e **idempotente**: alertas ja lidos preservam esse estado, alertas cuja condicao desapareceu viram `RESOLVIDO` (nunca sao apagados) e itens que voltam a ficar pendentes reabrem o alerta original.

```http
POST   /api/gestantes/1/alertas/avaliar
GET    /api/gestantes/1/alertas
GET    /api/gestantes/1/alertas/resumo
PATCH  /api/alertas/5/leitura
PATCH  /api/alertas/5/resolucao
```

Detalhes de arquitetura, contratos e decisoes em [`docs/MICROSSERVICO_ALERTAS.md`](docs/MICROSSERVICO_ALERTAS.md).

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
| **Testes do microsservico** | 44 testes em `alertas-service` | `docs/TESTING.md` | `mvn test` |

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

A SPA tem quatro abas: Gestantes, Procedimentos, Checklist e **Alertas**. A aba Alertas consome o microsservico atraves do servico principal, exibindo contagens por prioridade, filtros e acoes de leitura/resolucao.

O backend continua expondo as rotas REST anteriores, com rotas adicionais para paginacao, auditoria e alertas.

## Limitacoes conhecidas

- Autenticacao de usuario nas revisoes Envers ainda nao foi implementada. As revisoes registram o que mudou e quando mudou, mas nao quem alterou.
- A integracao com o microsservico e sincrona (REST). Uma fila de mensagens tornaria a sincronizacao resiliente a janelas de indisponibilidade sem depender de nova reavaliacao.
- Os alertas sao eventualmente consistentes: se o microsservico estiver fora do ar durante uma mudanca de checklist, eles so se atualizam na proxima reavaliacao.
- O cliente de Config Server esta configurado mas desligado por padrao; nao ha um Config Server no `compose.yaml`.
