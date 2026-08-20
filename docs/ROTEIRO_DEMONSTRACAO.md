# Roteiro de demonstração — Microsserviço de Alertas

Demonstração prática da operação integrada entre o serviço principal (GestarAfeto) e o
microsserviço `gestarafeto-alertas`. Duração estimada: **10 a 12 minutos**.

A ideia central que o roteiro sustenta: **os dois serviços são independentes, mas operam como
um só sistema — e o principal continua funcionando quando o microsserviço cai.**

---

## Antes de começar

### 1. Subir o ambiente (fazer antes da apresentação)

```powershell
docker compose up -d
```

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

```powershell
cd alertas-service
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

```powershell
cd frontend
npm run dev
```

### 2. Preparar os dados

```powershell
.\demo\reset-demo.ps1
```

O script apaga tudo e recria duas gestantes **sem checklist gerado**:

| Gestante | Semana | Perfil |
|---|---|---|
| Maria Silva | 26 | Iniciou o pré-natal tarde — vários procedimentos atrasados |
| Joana Pereira | 8 | Acompanhamento em dia — nenhum atraso |

> Rode o script novamente entre ensaios para repetir a demonstração do zero.

### 3. Janelas a deixar abertas

| Janela | Para quê |
|---|---|
| Navegador — `http://localhost:5173` | A demonstração acontece aqui |
| Navegador (2ª aba) — `http://localhost:8081/api/alertas?size=5` | Provar que o microsserviço é um serviço à parte |
| Terminal do serviço principal | Mostrar o log do *fallback* no Ato 4 |
| Terminal livre | Comandos `curl` e parar/subir o microsserviço |

### Checagem rápida (30 s antes de começar)

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

Ambos devem responder `"status":"UP"`.

---

## Ato 1 — Dois serviços, dois bancos (1,5 min)

**Objetivo:** estabelecer que existem de fato dois serviços autônomos.

### O que fazer

Mostre os dois *health checks* respondendo em portas diferentes, cada um com seu PostgreSQL:

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

Depois, mostre que os bancos não compartilham nenhuma tabela:

```powershell
docker exec gestarafeto-postgres psql -U gestarafeto -d gestarafeto -c "\dt"
```

```powershell
docker exec gestarafeto-postgres-alertas psql -U gestarafeto -d gestarafeto_alertas -c "\dt"
```

### O que dizer

> "São dois processos Spring Boot independentes, nas portas 8080 e 8081, cada um com seu
> próprio banco PostgreSQL, em containers e volumes separados.
>
> Repare que o banco de alertas tem uma única tabela: `alertas`. Ele não enxerga `gestantes`
> nem `itens_checklist_gestante`. Não existe *foreign key*, *join* ou transação distribuída
> entre os dois — e nenhum dos serviços tem credencial para o banco do outro. Isso é
> proposital: **força** a integração a acontecer pela API REST, em vez de alguém tomar um
> atalho e ler a tabela do vizinho."

---

## Ato 2 — Integração automática (3 min)

**Objetivo:** o momento mais forte da demonstração — os alertas aparecem sozinhos.

### O que fazer

1. Abra a aba **Checklist**, selecione **Maria Silva** e clique em **Gerar Checklist**.
   Aparecem 23 itens, todos `PENDENTE`.
2. **Não faça mais nada no checklist.** Vá direto para a aba **Alertas** e selecione
   **Maria Silva**.

### O que mostrar

O painel já vem preenchido, sem nenhuma ação extra:

| Ativos | Alta | Média | Baixa |
|---|---|---|---|
| 22 | 14 | 6 | 2 |

Role a lista e leia em voz alta um alerta de cada prioridade:

- **Alta** — *"Atrasado: Exame de Urina EAS — a janela recomendada terminou na semana 13 e a
  gestação está na semana 26 (13 semanas de atraso)."*
- **Média** — *"Pendente agora: Avaliação de Peso — a janela recomendada (semanas 1 a 40) está
  em curso e o item continua pendente."*
- **Baixa** — *"Em breve: Vacina dTpa — a janela recomendada começa na semana 27, em 1 semana."*

### O que dizer

> "Eu não pedi alertas em nenhum momento — só gerei o checklist. O `ChecklistService` publica um
> evento de domínio, e um *listener* reage **depois do commit** chamando o microsserviço. O
> domínio de checklist não sabe que alertas existem; ele só anuncia que algo mudou.
>
> E repare que o microsserviço não recebeu só 'gerou checklist'. Ele recebeu a DUM da gestante e
> as janelas recomendadas de cada procedimento, estimou que ela está na **semana 26** e
> classificou item por item. Um exame cuja janela era até a semana 13 vira prioridade **alta**;
> um que só começa na semana 27 vira **baixa**, com o aviso de que falta uma semana."

### Prova de que é outro serviço

Na segunda aba do navegador, abra:

```
http://localhost:8081/api/alertas?size=5
```

> "Esses alertas estão na porta 8081, no banco do microsserviço. A tela que acabamos de ver
> consome isso através do serviço principal, que atua como fachada — assim o navegador fala com
> uma origem só."

---

## Ato 3 — Ida e volta do estado (2,5 min)

**Objetivo:** mostrar que a integração é bidirecional e idempotente.

### 3.1 Concluir um item resolve o alerta

1. Volte à aba **Checklist** (Maria Silva).
2. Clique em **✓ Realizado** em **Hemograma Completo**.
3. Volte à aba **Alertas** e selecione Maria novamente.

O painel agora mostra **21 ativos**, **13 alta**, e **1 resolvido**. O alerta do hemograma sumiu
da lista.

> "Concluí um item na tela de checklist e o alerta correspondente se resolveu sozinho no outro
> serviço. E ele não foi apagado — virou `RESOLVIDO`. O rastro do que já foi sinalizado à equipe
> continua consultável."

### 3.2 Marcar como lido, e a idempotência

1. Clique em **Marcar lido** no primeiro alerta. O contador **Não lidos** cai de 21 para 20.
2. Clique em **Reavaliar Alertas** — duas vezes seguidas.

> "Reavaliei duas vezes e o total continua 21: nenhum registro duplicado. A reconciliação usa a
> chave *(item de origem, tipo do alerta)*, então reavaliar é idempotente.
>
> E o alerta que eu marquei como lido **continua lido**. Se a reavaliação recriasse tudo do zero,
> a equipe perderia o controle do que já conferiu a cada mudança no checklist."

### 3.3 Contraste entre perfis (opcional, se houver tempo)

Gere o checklist da **Joana Pereira** (semana 8) e abra os alertas dela: nenhum atraso, apenas
prioridades médias e baixas.

> "Mesma regra, mesmo catálogo de procedimentos, resultado completamente diferente — porque a
> semana gestacional é outra."

---

## Ato 4 — Resiliência (3,5 min)

**Objetivo:** o argumento de arquitetura mais importante. Vale investir tempo aqui.

### O que fazer

**Derrube o microsserviço** — `Ctrl+C` no terminal dele, ou:

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

Agora, **com o microsserviço fora do ar**, faça nesta ordem:

**1. O sistema principal continua intacto.**
Navegue por Gestantes, Procedimentos e Checklist. Tudo funciona normalmente.

**2. Marque outro item do checklist como realizado.**
Funciona. Nenhum erro na tela.

> "Essa é a parte que mais importa. O microsserviço está morto e a operação de checklist —
> que é o núcleo do sistema — concluiu normalmente. O *listener* que sincroniza alertas engole
> a falha e registra um aviso. Salvar o checklist com sucesso nunca pode virar erro para a
> usuária por causa de um serviço auxiliar."

Mostre a linha no terminal do serviço principal:

```
WARN AlertaClientFallbackFactory : Microsservico de alertas indisponivel;
     aplicando fallback. Causa: Connection refused
```

**3. Abra a aba Alertas.**
A tela carrega, HTTP 200, painel zerado e lista vazia. Sem tela de erro.

**4. Tente marcar um alerta como lido.**

Como a lista está vazia, não há botão para clicar — faça pelo terminal (o circuito está aberto,
então qualquer id serve; a chamada nem chega ao microsserviço):

```powershell
curl -X PATCH http://localhost:8080/api/alertas/1/leitura
```

Retorna **503** com mensagem clara:

```json
{"status":503,"erro":"Servico indisponivel",
 "mensagem":"O servico de alertas esta indisponivel no momento. Tente novamente em instantes."}
```

### O que dizer

> "Repare que a degradação é **assimétrica**, e isso foi uma decisão de projeto.
>
> **Leitura** degrada em silêncio: o painel de alertas fica vazio, mas o pré-natal continua
> funcionando. Alerta é recurso complementar.
>
> **Ação explícita da usuária** falha de forma **visível**, com 503. Se eu devolvesse 200 aqui, a
> interface mostraria 'lido' para um alerta que o microsserviço nunca registrou. Mentir para a
> usuária é pior do que dizer que o serviço caiu.
>
> Quem faz esse controle é o circuit breaker do Resilience4j: depois de 50% de falhas na janela,
> ele abre por 10 segundos e nem tenta a chamada — vai direto para o *fallback*."

### Recuperação

Suba o microsserviço de novo:

```powershell
cd alertas-service
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Volte à aba **Alertas**, selecione Maria e clique em **Reavaliar Alertas**.

> "Os alertas voltam, e já reconciliados: o item que concluí enquanto o serviço estava fora
> aparece resolvido, e o alerta que eu tinha marcado como lido continua lido.
>
> Os alertas são **eventualmente consistentes** — essa é a troca que aceitei ao separar o
> serviço. Em compensação, o cadastro de gestantes nunca fica indisponível por causa dos
> alertas."

---

## Ato 5 — Fechamento técnico (1,5 min)

**Objetivo:** amarrar a demonstração ao código.

### O que mostrar

Abra `docs.html` em `http://localhost:8080/docs.html`, seções 20 a 25.

Ou mostre os três trechos de código que sustentam tudo que foi demonstrado:

**1. O cliente Feign** — comunicação distribuída declarativa:

```java
@FeignClient(name = "gestarafeto-alertas",
             url = "${gestarafeto.alertas.url:http://localhost:8081}",
             fallbackFactory = AlertaClientFallbackFactory.class)
public interface AlertaClient {
    @PostMapping("/api/alertas/avaliacoes")
    List<AlertaResponse> avaliar(@RequestBody AvaliarChecklistRequest request);
}
```

**2. O desacoplamento por evento** — o checklist não conhece alertas:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void aoAlterarChecklist(ChecklistAlteradoEvent evento) {
    alertaIntegracaoService.reavaliarSemPropagarErro(evento.gestanteId());
}
```

**3. A degradação assimétrica** — o *fallback*:

```java
@Override
public List<AlertaResponse> listarPorGestante(Long gestanteId) {
    return List.of();          // leitura degrada
}

@Override
public AlertaResponse marcarLido(Long id) {
    throw indisponivel();      // acao da usuaria falha visivelmente
}
```

### Números para fechar

> "São 76 testes no total: 44 no microsserviço e 32 no serviço principal.
>
> E vale contar um detalhe: quando subi os dois serviços pela primeira vez, o `PATCH` falhava. O
> cliente HTTP padrão do Feign usa `HttpURLConnection`, que não suporta esse verbo. Nenhum teste
> pegou, porque todos mockavam o cliente — validavam a tradução do domínio, mas não o
> transporte. Troquei o cliente por Apache HttpClient 5 e escrevi o `AlertaClientHttpTest`, que
> exercita o Feign real contra um servidor HTTP em memória. Confirmei que ele falha sem a
> correção e passa com ela."

---

## Plano B

| Problema | O que fazer |
|---|---|
| Front-end não carrega | Demonstre por `curl` — todos os comandos estão neste roteiro |
| Alertas vazios no Ato 2 | Clique em **Reavaliar Alertas**; o evento pode ter falhado se o microsserviço subiu depois |
| Estado confuso após um ensaio | Rode `.\demo\reset-demo.ps1` e recomece do Ato 2 |
| Docker indisponível | Suba os dois serviços com `-Dspring-boot.run.profiles=dev-h2` (banco em memória) |
| Porta ocupada | `Get-NetTCPConnection -LocalPort 8081 -State Listen` para achar o processo |

---

## Perguntas prováveis

**"Por que alertas, e não outro domínio?"**
> Foi o único candidato que atendia aos três critérios: tem dados próprios (o alerta tem ciclo de
> vida que o checklist não modela), a regra é volátil (limiares mudam com protocolos clínicos) e a
> carga é assimétrica (reavaliação em lote, muito mais frequente que as escritas de cadastro). O
> cadastro de gestantes eu mantive de propósito no serviço principal — é o núcleo transacional e
> precisa de integridade forte.

**"E se os dois bancos ficarem inconsistentes?"**
> Ficam mesmo, temporariamente. A reavaliação é idempotente e reconcilia tudo de uma vez, então a
> inconsistência se resolve na próxima sincronização. Nenhuma decisão clínica depende do alerta
> estar atualizado no mesmo segundo.

**"Por que REST e não fila de mensagens?"**
> Fila seria melhor e está no plano de evolução — resolveria a janela de indisponibilidade sem
> depender de nova reavaliação. Para esta entrega, REST síncrono com circuit breaker cobre o
> requisito de comunicação distribuída com menos infraestrutura.

**"Onde está o Spring Cloud?"**
> Em três pontos: OpenFeign para o cliente declarativo, CircuitBreaker com Resilience4j para a
> resiliência que acabamos de ver, e spring-cloud-context para a configuração distribuída — os
> parâmetros do motor de regras podem ser recarregados em runtime com um
> `POST /actuator/refresh`, sem reiniciar o serviço.

**"Os serviços compartilham código?"**
> Não. Os DTOs do contrato são replicados nos dois lados de propósito. Um *jar* compartilhado
> acoplaria o ciclo de release dos dois serviços. Pelo mesmo motivo, os enums trafegam como texto:
> um valor novo de um lado não quebra a desserialização do outro.

---

## Resumo dos tempos

| Ato | Conteúdo | Tempo |
|---|---|---|
| 1 | Dois serviços, dois bancos | 1,5 min |
| 2 | Integração automática por evento | 3 min |
| 3 | Resolução, leitura e idempotência | 2,5 min |
| 4 | Resiliência e degradação assimétrica | 3,5 min |
| 5 | Fechamento técnico | 1,5 min |
| | **Total** | **12 min** |

Se o tempo apertar, corte o item 3.3 (contraste entre perfis) e encurte o Ato 5 para apenas os
números. **Não corte o Ato 4** — é o argumento de arquitetura mais forte da entrega.
