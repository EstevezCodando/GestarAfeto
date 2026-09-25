# Exemplos de API

## Cadastrar gestante

```http
POST /api/gestantes
Content-Type: application/json

{
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "dataProvavelParto": "2026-11-20",
  "observacoes": "Acompanhamento inicial"
}
```

## Paginar gestantes por nome

```http
GET /api/gestantes/paginado?nome=maria&page=0&size=10
```

## Listar procedimentos ativos paginados

```http
GET /api/procedimentos/paginado?tipo=EXAME&page=0&size=10
```

## Gerar checklist

```http
POST /api/gestantes/1/checklist/gerar
```

## Filtrar checklist por status

```http
GET /api/gestantes/1/checklist?status=REALIZADO
```

## Registrar consulta

```http
POST /api/gestantes/1/consultas
Content-Type: application/json

{
  "dataConsulta": "2026-07-17",
  "semanaGestacional": 20,
  "peso": 68.50,
  "pressaoArterial": "110/70"
}
```

## Consultar historico

```http
GET /api/auditoria/gestante/1
```

## Alertas

As rotas abaixo sao expostas pelo servico principal (porta 8080), que encaminha ao
microsservico de alertas. Ver [`MICROSSERVICO_ALERTAS.md`](MICROSSERVICO_ALERTAS.md) para a
API direta do microsservico (porta 8081).

### Reavaliar alertas de uma gestante (assincrono)

```http
POST /api/gestantes/1/alertas/avaliar
```

Responde **202 Accepted**: o servico publicou o evento `checklist.alterado` no RabbitMQ e
retornou. O calculo acontece no microsservico, de forma assincrona.

```json
{
  "gestanteId": 1,
  "status": "ACEITO",
  "mensagem": "Reavaliacao solicitada. Os alertas serao atualizados em instantes."
}
```

A lista atualizada vem de uma consulta posterior, nao desta chamada.

### Listar alertas ativos

```http
GET /api/gestantes/1/alertas
```

### Resumo agregado

```http
GET /api/gestantes/1/alertas/resumo
```

```json
{
  "gestanteId": 1,
  "totalAtivos": 5,
  "alta": 2,
  "media": 2,
  "baixa": 1,
  "naoLidos": 4,
  "resolvidos": 7
}
```

### Marcar como lido e resolver

```http
PATCH /api/alertas/12/leitura
```

```http
PATCH /api/alertas/12/resolucao
```

Se o microsservico estiver indisponivel, estas duas rotas respondem **503**:

```json
{
  "timestamp": "2026-08-19T21:20:11",
  "status": 503,
  "erro": "Servico indisponivel",
  "mensagem": "O servico de alertas esta indisponivel no momento. Tente novamente em instantes.",
  "path": "/api/alertas/12/resolucao"
}
```

As rotas de leitura (`GET .../alertas` e `GET .../alertas/resumo`) degradam para lista vazia e
contagens zeradas em vez de erro.

### Chamada direta ao microsservico

```http
POST http://localhost:8081/api/alertas/avaliacoes
Content-Type: application/json

{
  "gestanteId": 1,
  "gestanteNome": "Maria Silva",
  "dataUltimaMenstruacao": "2026-01-20",
  "dataProvavelParto": null,
  "itens": [
    {
      "itemId": 34,
      "procedimentoNome": "Ultrassonografia Morfologica",
      "procedimentoTipo": "ULTRASSONOGRAFIA",
      "status": "PENDENTE",
      "obrigatorio": true,
      "semanaInicialRecomendada": 20,
      "semanaFinalRecomendada": 24,
      "dataRealizacao": null
    }
  ]
}
```

### Recarregar as regras sem reiniciar

```http
POST http://localhost:8081/actuator/refresh
```
