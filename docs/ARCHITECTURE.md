# Arquitetura do GestarAfeto

O GestarAfeto e um servico principal em camadas organizado por dominio, acompanhado de um
microsservico dedicado a alertas. A entrega TP2/PB fortaleceu a persistencia com PostgreSQL,
Flyway, Spring Data JPA, Envers e Testcontainers. A entrega TP3/PB extraiu o calculo de
alertas para um servico proprio, integrado por REST com Spring Cloud OpenFeign e circuit
breaker Resilience4j.

O detalhamento do microsservico esta em [`MICROSSERVICO_ALERTAS.md`](MICROSSERVICO_ALERTAS.md).

## Visao geral dos servicos

```mermaid
flowchart LR
    U["Usuario"] --> FE["React SPA"]
    FE -->|"HTTP :8080"| MAIN["Servico principal<br/>GestarAfeto"]
    MAIN -->|"REST via Feign :8081"| ALERTAS["Microsservico<br/>gestarafeto-alertas"]
    MAIN --> DB[("gestarafeto")]
    ALERTAS --> ADB[("gestarafeto_alertas")]
```

Cada servico e dono do seu banco. Nao ha FK, join ou transacao distribuida entre eles: o
microsservico guarda apenas referencias logicas (`gestante_id`, `origem_id`).

## Diagrama de componentes

```mermaid
flowchart LR
    U["Usuario"] --> FE["React SPA"]
    FE --> API["Controllers REST"]

    subgraph Spring["Servico principal - Spring Boot"]
        API --> GS["GestanteService"]
        API --> PS["ProcedimentoService"]
        API --> CS["ChecklistService"]
        API --> QS["ConsultaService"]
        API --> AS["AuditoriaService"]
        API --> AIS["AlertaIntegracaoService"]

        GS --> GR["GestanteRepository"]
        PS --> PR["ProcedimentoRepository"]
        CS --> IR["ItemChecklistRepository"]
        QS --> QR["ConsultaRepository"]
        AS --> EV["Hibernate Envers"]

        CS -. "ChecklistAlteradoEvent" .-> EL["ChecklistAlteradoListener"]
        GS -. "GestanteRemovidaEvent" .-> EL
        EL --> AIS
        AIS --> FC["AlertaClient (Feign)"]
        FC -. "circuito aberto" .-> FB["Fallback"]

        GR & PR & IR & QR --> JPA["Spring Data JPA"]
        EV --> JPA
        EH["GlobalExceptionHandler"] -.-> API
    end

    subgraph Alertas["Microsservico gestarafeto-alertas"]
        MC["AlertaController"] --> MS["AlertaService"]
        MS --> MR["MotorDeRegrasAlerta"]
        MS --> AR["AlertaRepository"]
        AR --> ADB[("gestarafeto_alertas")]
    end

    FC -->|HTTP| MC

    JPA --> DB[("PostgreSQL")]
    FW["Flyway migrations"] --> DB
    FWA["Flyway migrations"] --> ADB
    TC["Testcontainers"] -. "testes" .-> DB
```

## Diagrama de camadas

```mermaid
flowchart TD
    C["Controller: HTTP, validacao e DTOs"]
    S["Service: regras, transacoes e orquestracao"]
    R["Repository: Spring Data JPA"]
    D["Domain: entidades JPA auditadas"]
    P[("PostgreSQL")]
    A["Envers: tabelas *_aud + revinfo"]

    C --> S --> R --> D
    R --> P
    D --> A --> P
```

## Modelo ER

```mermaid
erDiagram
    GESTANTES ||--o{ ITENS_CHECKLIST_GESTANTE : possui
    GESTANTES ||--o{ CONSULTAS_PRENATAL : realiza
    PROCEDIMENTOS_PRENATAL ||--o{ ITENS_CHECKLIST_GESTANTE : compoe
    REVINFO ||--o{ GESTANTES_AUD : revisa
    REVINFO ||--o{ PROCEDIMENTOS_PRENATAL_AUD : revisa
    REVINFO ||--o{ ITENS_CHECKLIST_GESTANTE_AUD : revisa
    REVINFO ||--o{ CONSULTAS_PRENATAL_AUD : revisa

    GESTANTES {
        bigint id PK
        bigint version
        varchar nome
        varchar email
        date data_provavel_parto
        timestamp data_cadastro
    }

    PROCEDIMENTOS_PRENATAL {
        bigint id PK
        bigint version
        varchar nome
        varchar tipo
        varchar trimestre_recomendado
        boolean ativo
    }

    ITENS_CHECKLIST_GESTANTE {
        bigint id PK
        bigint gestante_id FK
        bigint procedimento_id FK
        varchar status
        date data_realizacao
    }

    CONSULTAS_PRENATAL {
        bigint id PK
        bigint gestante_id FK
        date data_consulta
        integer semana_gestacional
        numeric peso
    }
```

Constraints principais: `UNIQUE (gestante_id, procedimento_id)` no checklist, checks de semana gestacional, peso positivo, intervalo de semanas do procedimento e data obrigatoria quando status e `REALIZADO`.

## Modelo do microsservico de alertas

Banco separado, sem relacionamento fisico com o modelo acima. A linha tracejada indica uma
referencia **logica**, resolvida por id na aplicacao e nunca por join.

```mermaid
erDiagram
    ALERTAS {
        bigint id PK
        bigint version
        bigint gestante_id "referencia logica"
        varchar gestante_nome "replicado na avaliacao"
        bigint origem_id "item de checklist, referencia logica"
        varchar tipo
        varchar prioridade
        varchar status
        varchar titulo
        text mensagem
        date data_referencia
        integer semana_gestacional_referencia
        timestamp data_criacao
        timestamp data_leitura
        timestamp data_resolucao
    }
```

Constraints: `UNIQUE (gestante_id, origem_id, tipo)` — que torna a reavaliacao idempotente —
alem de checks para os valores de `tipo`, `prioridade` e `status`, e para a coerencia entre
status e as datas de leitura/resolucao.

## Sequencia: cadastro de gestante

```mermaid
sequenceDiagram
    actor Usuario
    participant FE as React
    participant C as GestanteController
    participant S as GestanteService
    participant R as GestanteRepository
    participant DB as PostgreSQL
    participant AUD as Envers

    Usuario->>FE: preenche cadastro
    FE->>C: POST /api/gestantes
    C->>S: cadastrar(request)
    S->>S: valida DPP ou DUM
    S->>R: save(gestante)
    R->>DB: INSERT gestantes
    AUD->>DB: INSERT gestantes_aud
    S-->>C: GestanteResponse
    C-->>FE: 201 Created
```

## Sequencia: geracao de checklist

```mermaid
sequenceDiagram
    participant C as ChecklistController
    participant S as ChecklistService
    participant G as GestanteService
    participant P as ProcedimentoService
    participant R as ItemChecklistRepository
    participant DB as PostgreSQL

    C->>S: gerarChecklist(gestanteId)
    S->>R: existsByGestanteId
    S->>G: buscarEntidade
    S->>P: listarAtivos
    S->>R: saveAll(itens)
    R->>DB: INSERT itens_checklist_gestante
    DB-->>R: constraint unique garante nao duplicidade
    S-->>C: lista de itens
```

## Sequencia: historico de auditoria

```mermaid
sequenceDiagram
    participant C as AuditoriaController
    participant S as AuditoriaService
    participant E as AuditReader Envers
    participant DB as PostgreSQL

    C->>S: historico("gestante", id)
    S->>E: forRevisionsOfEntity(Gestante)
    E->>DB: SELECT revinfo + gestantes_aud
    DB-->>E: revisoes
    S-->>C: AuditoriaRevisionResponse[]
```

## Sequencia: atualizar status do checklist

```mermaid
sequenceDiagram
    actor Usuario
    participant C as ChecklistController
    participant S as ChecklistService
    participant R as ItemChecklistRepository
    participant DB as PostgreSQL
    participant AUD as Envers

    Usuario->>C: PATCH /api/checklist/{itemId}/realizar
    C->>S: marcarRealizado(itemId)
    S->>R: findById(itemId)
    R->>DB: SELECT item
    alt item nao encontrado
        S-->>C: RecursoNaoEncontradoException (404)
    else item encontrado
        S->>S: status = REALIZADO, dataRealizacao = now
        S->>R: save(item)
        R->>DB: UPDATE itens_checklist_gestante ... version = version + 1
        AUD->>DB: INSERT itens_checklist_gestante_aud (revtype = MOD)
        S-->>C: ItemChecklistResponse (200 OK)
    end
```

## Sequencia: concorrencia com lock otimista

```mermaid
sequenceDiagram
    actor A as Usuaria A
    actor B as Usuaria B
    participant S as GestanteService
    participant DB as PostgreSQL

    Note over A,B: Ambas carregam a mesma gestante (version = 5)
    A->>S: atualizar(id, dadosA)
    S->>DB: UPDATE gestantes ... version = 6 WHERE id = ? AND version = 5
    DB-->>S: 1 linha afetada (ok, agora version = 6)
    S-->>A: 200 OK
    B->>S: atualizar(id, dadosB) [ainda com version = 5]
    S->>DB: UPDATE gestantes ... version = 6 WHERE id = ? AND version = 5
    DB-->>S: 0 linhas afetadas
    S-->>B: OptimisticLockingFailureException -> 409 Conflict
    Note over B: Recarregar os dados atualizados e repetir a edicao
```

## Sequencia: avaliacao de alertas no microsservico

```mermaid
sequenceDiagram
    actor U as Usuaria
    participant FE as React (AlertasPage)
    participant AC as AlertaController (:8080)
    participant IS as AlertaIntegracaoService
    participant FC as AlertaClient (Feign)
    participant MS as Microsservico (:8081)
    participant DB as gestarafeto_alertas

    U->>FE: clica em "Reavaliar Alertas"
    FE->>AC: POST /api/gestantes/1/alertas/avaliar
    AC->>IS: reavaliar(1)
    IS->>IS: monta snapshot (gestante + itens + janelas)
    IS->>FC: avaliar(snapshot)
    FC->>MS: POST /api/alertas/avaliacoes
    MS->>MS: estima semana gestacional e classifica itens
    MS->>DB: reconcilia por (origem, tipo)
    MS-->>FC: alertas ativos ordenados por severidade
    FC-->>AC: List<AlertaResponse>
    AC-->>FE: 200 OK
```

## Sequencia: sincronizacao automatica apos mudanca no checklist

```mermaid
sequenceDiagram
    participant CS as ChecklistService
    participant DB as PostgreSQL
    participant EV as ApplicationEventPublisher
    participant L as ChecklistAlteradoListener
    participant MS as Microsservico de alertas

    CS->>DB: UPDATE item (status = REALIZADO)
    CS->>EV: publish(ChecklistAlteradoEvent)
    DB-->>CS: commit
    Note over EV,L: @TransactionalEventListener(AFTER_COMMIT)
    EV->>L: aoAlterarChecklist(evento)
    L->>MS: POST /api/alertas/avaliacoes
    alt microsservico fora do ar
        MS--xL: falha
        L->>L: registra aviso, nao propaga
        Note over L: o checklist ja foi salvo:<br/>a usuaria nao ve erro
    end
```

## Sequencia: microsservico indisponivel (circuit breaker)

```mermaid
sequenceDiagram
    participant FE as React
    participant AC as AlertaController
    participant CB as CircuitBreaker (Resilience4j)
    participant FB as AlertaClientFallbackFactory
    participant MS as Microsservico

    FE->>AC: GET /api/gestantes/1/alertas
    AC->>CB: listarPorGestante(1)
    CB->>MS: GET /api/alertas/gestante/1
    MS--xCB: connection refused
    CB->>FB: create(causa)
    FB-->>AC: lista vazia (degradacao)
    AC-->>FE: 200 OK com []
    Note over FE: painel vazio; o resto do<br/>sistema segue funcionando

    FE->>AC: PATCH /api/alertas/9/resolucao
    AC->>CB: resolver(9)
    CB->>FB: create(causa)
    FB--xAC: ServicoIndisponivelException
    AC-->>FE: 503 Service Unavailable
    Note over FE: acao explicita falha de forma<br/>visivel, sem fingir sucesso
```
