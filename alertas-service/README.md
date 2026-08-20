# gestarafeto-alertas

Microsservico que transforma o checklist pre-natal de uma gestante em uma lista priorizada de
alertas. Faz parte do sistema [GestarAfeto](../README.md).

A documentacao completa — decisoes de arquitetura, contratos, diagramas e uso do Spring Cloud —
esta em [`docs/MICROSSERVICO_ALERTAS.md`](../docs/MICROSSERVICO_ALERTAS.md).

## Execucao

Com PostgreSQL (o banco sobe pelo `compose.yaml` da raiz, na porta 5433):

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Sem Docker, com banco em memoria:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

O servico sobe em `http://localhost:8081`.

## Testes

```powershell
.\mvnw.cmd test
```

## Configuracao

| Variavel | Padrao | Para que |
|---|---|---|
| `GESTARAFETO_ALERTAS_PORT` | `8081` | Porta HTTP |
| `GESTARAFETO_ALERTAS_DB_URL` | `jdbc:postgresql://localhost:5433/gestarafeto_alertas` | Banco proprio |
| `GESTARAFETO_ALERTAS_DB_USER` | `gestarafeto` | Usuario do banco |
| `GESTARAFETO_ALERTAS_DB_PASSWORD` | `gestarafeto` | Senha do banco |
| `GESTARAFETO_CONFIG_ENABLED` | `false` | Habilita o cliente de Config Server |
| `GESTARAFETO_CONFIG_URI` | `http://localhost:8888` | Endereco do Config Server |

Parametros do motor de regras, recarregaveis via `POST /actuator/refresh`:

```properties
gestarafeto.alertas.regras.antecedencia-semanas=2
gestarafeto.alertas.regras.semanas-gestacao-total=40
gestarafeto.alertas.regras.alertar-nao-obrigatorios=true
```
