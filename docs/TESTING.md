# Testes automatizados

O sistema tem duas suites, uma por servico.

```powershell
.\mvnw.cmd test
```

```powershell
cd alertas-service
.\mvnw.cmd test
```

Total: **89 testes** — 31 no servico principal (1 ignorado sem Docker) e 58 no microsservico (5 ignorados sem Docker).

## Servico principal

### Persistencia e dominio

- `GestanteRepositoryTest`: pesquisa paginada por nome e busca por e-mail.
- `ProcedimentoRepositoryTest`: filtros por ativo, tipo, trimestre e semana.
- `ItemChecklistGestanteRepositoryTest`: constraint de duplicidade e filtro por status.
- `ConsultaPreNatalRepositoryTest`: ordenacao desc por data e integridade de peso.
- `AuditoriaIntegrationTest`: cria e atualiza gestante, depois consulta revisoes Envers.
- `MigrationIntegrationTest`: sobe PostgreSQL com Testcontainers, aplica Flyway e valida seed.

### Integracao com o microsservico

- `AlertaIntegracaoServiceTest`: valida o snapshot enviado ao microsservico (dados da gestante,
  status do item, janelas do procedimento) e garante que a variante tolerante a falha nao
  propaga erro.
- `ChecklistAlteradoListenerTest`: prova que gerar checklist, marcar item como realizado e
  remover gestante disparam a sincronizacao **apos o commit**, e que uma falha do microsservico
  nao impede a operacao de checklist.
- `AlertaControllerTest`: cobre a fachada REST, incluindo 404 para gestante inexistente, 503
  quando uma acao da usuaria nao pode ser concluida e degradacao para lista vazia nas leituras.
- `AlertaClientFallbackFactoryTest`: documenta a degradacao assimetrica do circuit breaker
  (leituras vazias, acoes explicitas com erro visivel).
- `AlertaClientHttpTest`: exercita o cliente Feign **sobre HTTP real**, contra um servidor stub
  em memoria, verificando o verbo efetivamente trafegado em cada operacao.

O `AlertaClientHttpTest` existe por um motivo concreto. Os demais testes de integracao
substituem o `AlertaClient` por um mock, o que valida a traducao do dominio mas deixa a camada
de transporte descoberta — e foi por ali que passou um defeito real: o cliente HTTP padrao do
Feign (`HttpURLConnection`) nao suporta o verbo `PATCH`, entao as transicoes de estado do alerta
falhavam com "Invalid HTTP method: PATCH" apenas com os dois servicos no ar. A correcao foi
trocar o cliente por Apache HttpClient 5 (`feign-hc5`); o teste falha sem ela e passa com ela.

## Microsservico de alertas

- `MotorDeRegrasAlertaTest` (16 testes, sem Spring e sem banco): estimativa da semana
  gestacional por DUM e por DPP, limites fisiologicos, classificacao em atrasado / pendente /
  janela proxima, efeito da prioridade para itens nao obrigatorios, ordenacao por severidade,
  conversao de semana em data e o efeito de mudar os parametros configuraveis.
- `AlertaRepositoryTest`: constraint `uk_alerta_origem_tipo`, ordenacao por severidade real
  (e nao alfabetica), exclusao de alertas encerrados da lista de ativos, contagem agrupada por
  prioridade, filtro paginado e remocao em massa por gestante.
- `AlertaServiceTest`: reconciliacao — idempotencia, resolucao ao concluir o item, reabertura
  ao voltar a ficar pendente, preservacao do status de leitura e troca de tipo do alerta.
- `AlertaControllerTest`: contrato HTTP da API, validacao de payload, resumo agregado,
  transicoes de leitura/resolucao e 404.
- `AlertasServiceApplicationTests`: contexto sobe e os parametros do motor sao carregados.

## Notas de ambiente

`MigrationIntegrationTest` usa `@Testcontainers(disabledWithoutDocker = true)`, entao a suite
continua executavel sem Docker — a evidencia PostgreSQL aparece quando Docker esta disponivel.

Os testes de escrita em entidades `@Audited` aplicam
[`src/test/resources/db/envers-test-schema.sql`](../src/test/resources/db/envers-test-schema.sql)
via `@Sql`. O profile `test` usa `ddl-auto=create-drop`, que cria apenas as tabelas mapeadas
por entidades; as tabelas `*_aud` e `revinfo` do Envers precisam ser declaradas a parte.

Os testes de integracao substituem o `AlertaClient` por um mock (`@MockitoBean`): nenhuma suite
depende do microsservico estar no ar. A sincronizacao automatica fica desligada no profile
`test` (`gestarafeto.alertas.integracao-automatica=false`) e e reativada apenas em
`ChecklistAlteradoListenerTest`, que existe justamente para exercita-la.

## Mensageria e arquitetura de eventos (TP4)

### Servico principal (produtor)

- `AlertaIntegracaoServiceTest`: conteudo da mensagem publicada — estado completo do checklist
  (para o consumidor nao precisar chamar de volta), envelope de rastreamento com `eventoId`,
  `tipo`, `versao` e `origem`, e a garantia de que gestante inexistente falha antes de publicar.
- `ChecklistAlteradoListenerTest`: gerar checklist, marcar item como realizado e remover
  gestante publicam os eventos correspondentes em `AFTER_COMMIT`; uma falha na publicacao nao
  impede a operacao de checklist ja concluida.
- `AlertaControllerTest`: a reavaliacao responde **202 Accepted** e publica o evento; gestante
  inexistente retorna 404 sem publicar nada.

### Microsservico (consumidor)

- `ChecklistAlteradoConsumerTest`: traducao da mensagem para o contrato interno, idempotencia
  (consumir duas vezes nao duplica alertas), resolucao ao concluir o item e rejeicao direta
  para a DLQ quando o payload e invalido.
- `GestanteRemovidaConsumerTest`: limpeza dos dados derivados e tolerancia a reentrega.
- `EventoRabbitMqIntegrationTest`: **broker real via Testcontainers**. Verifica que a topologia
  e declarada no broker, que a mensagem publicada no exchange chega ao consumidor e gera o
  alerta, que uma routing key nao vinculada nao chega as filas do servico, e que uma mensagem
  invalida termina na fila de dead letter.

O teste com broker real publica as mensagens como **mapas**, sem nenhuma classe do servico
produtor no classpath. Isso prova que o contrato entre os dois lados e o JSON, e nao uma classe
Java compartilhada — o que so funciona porque o conversor do consumidor usa precedencia de tipo
`INFERRED` em vez do cabecalho `__TypeId__` enviado pelo produtor.

Como os demais testes de infraestrutura, e ignorado automaticamente quando nao ha Docker.
