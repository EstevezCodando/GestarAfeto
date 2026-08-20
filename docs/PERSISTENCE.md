# Design da camada de persistencia

## Decisoes principais

- PostgreSQL e o banco de desenvolvimento e demonstracao.
- H2 fica restrito ao profile `dev-h2` e aos testes rapidos.
- Flyway cria e evolui o schema em `src/main/resources/db/migration`.
- Hibernate usa `ddl-auto=validate` nos perfis PostgreSQL.
- Envers registra historico em `revinfo` e tabelas `*_aud`.
- `@Version` protege entidades mutaveis contra atualizacao concorrente.

## Entidades e responsabilidades

- `Gestante`: entidade central do acompanhamento pre-natal.
- `ProcedimentoPreNatal`: catalogo independente de procedimentos.
- `ItemChecklistGestante`: associacao entre gestante e procedimento, com status e datas.
- `ConsultaPreNatal`: registro longitudinal de consultas de uma gestante.

## Integridade

As migrations criam FKs, checks e indices. A constraint `uk_checklist_gestante_procedimento` impede dois itens iguais para a mesma gestante. Checks validam semana gestacional, peso positivo, intervalo de semanas recomendado e data obrigatoria para checklist realizado.

## Performance

Relacionamentos `@ManyToOne` usam `LAZY`. Consultas que retornam DTOs com dados relacionados usam `@EntityGraph`, como `ItemChecklistGestanteRepository.findById` e filtros de checklist. Listagens que podem crescer possuem rotas paginadas.

## Repositories

Exemplos reais:

```java
Page<Gestante> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
Page<ProcedimentoPreNatal> findByTipoAndAtivoTrue(TipoProcedimento tipo, Pageable pageable);
List<ItemChecklistGestante> findByGestanteIdAndStatus(Long gestanteId, StatusChecklist status);
List<ConsultaPreNatal> findByGestanteIdOrderByDataConsultaDesc(Long gestanteId);
```

Os controllers acessam services, nunca repositories diretamente.

## Auditoria

`AuditoriaService` usa `AuditReaderFactory` para consultar revisoes. O endpoint `GET /api/auditoria/{entidade}/{id}` suporta `gestante`, `procedimento`, `checklist` e `consulta`.

## Profiles

- `dev`: PostgreSQL local via Docker Compose.
- `dev-h2`: H2 em memoria para desenvolvimento rapido.
- `test`: H2 em memoria, schema criado pelo Hibernate para testes rapidos.
- `prod`: PostgreSQL por variaveis de ambiente.

## Persistencia do microsservico de alertas

O microsservico `gestarafeto-alertas` tem uma camada de persistencia propria e independente,
seguindo o padrao **database-per-service**.

### Separacao fisica

O banco `gestarafeto_alertas` roda em um container e volume distintos (porta 5433). Nenhum dos
dois servicos tem credenciais para o banco do outro, o que torna impossivel um join acidental
entre os modelos e forca toda a integracao a passar pela API REST.

### Referencias logicas

`alertas.gestante_id` e `alertas.origem_id` apontam para registros do servico principal, mas
**nao sao chaves estrangeiras**. Consequencias assumidas:

- o banco nao impede um alerta orfao (gestante removida no outro servico);
- a limpeza e feita pela aplicacao, via `GestanteRemovidaEvent` → `DELETE /api/alertas/gestante/{id}`;
- em troca, os servicos sobem, migram e escalam de forma totalmente independente.

O nome da gestante e **replicado** em `gestante_nome` no momento da avaliacao. E uma
desnormalizacao deliberada: permite listar alertas sem uma chamada de volta ao servico
principal, ao custo de o nome ficar defasado ate a proxima reavaliacao — aceitavel para um
rotulo de exibicao.

### Integridade

A migration `V1__create_alertas_table.sql` cria:

- `UNIQUE (gestante_id, origem_id, tipo)` — a base da idempotencia da reavaliacao: cada par
  item/motivo tem no maximo uma linha, entao reavaliar nunca duplica;
- checks de dominio para `tipo`, `prioridade` e `status`;
- check de coerencia temporal: alerta `RESOLVIDO`/`CANCELADO` exige `data_resolucao`, e alerta
  `LIDO` exige `data_leitura`;
- indices em `gestante_id` e `(gestante_id, status)`, que sao os filtros reais da API.

`@Version` protege contra atualizacao concorrente, como nas entidades do servico principal.

### Ordenacao por severidade

`prioridade` e persistida como texto (`EnumType.STRING`), legivel no banco. Isso significa que
um `ORDER BY prioridade DESC` ordenaria alfabeticamente — `MEDIA`, `BAIXA`, `ALTA` — que e
exatamente o oposto do desejado.

`AlertaRepository.findAtivosOrdenadosPorSeveridade` resolve com um `CASE` que traduz cada valor
para o peso do enum, mantendo a ordenacao no banco em vez de trazer tudo para memoria.
`AlertaRepositoryTest.listaAtivosOrdenadosDaMaiorParaAMenorPrioridade` trava esse
comportamento.

### Auditoria

O microsservico nao usa Envers. O proprio ciclo de vida do alerta ja registra o historico
relevante (`data_criacao`, `data_leitura`, `data_resolucao`) e alertas encerrados sao marcados
como `RESOLVIDO` em vez de apagados, preservando o rastro do que ja foi sinalizado a equipe.
