# ACME Policy Service - Desafio Seguradora

Microsserviço orientado a eventos para gerenciar solicitações de apólice de seguros com integrações, regras de negócio e fluxo assíncrono (SQS). Foco em arquitetura limpa, testes e observabilidade (metrics, traces e logs).

## Jornada & Decisões

A evolução foi organizada em releases curtas, cada uma com um tema principal. Em linhas gerais:
- **0.1.0**: API inicial (criação/consulta), contratos em records, validação básica, estrutura alinhada a Clean Architecture.
- **0.2.0**: Integração com **API de Fraudes via OpenFeign** e **WireMock** (stubs com response templating) para facilitar testes locais.
- **0.3.0**: Persistência no **DynamoDB** (AWS SDK v2 Enhanced Client), mapeamentos com **MapStruct**.
- **0.4.0**: Publicação de **eventos** (SQS) após persistência; formalização de regras de negócio com Domain Events.
- **0.5.0**: **Consumers** de resultados (pagamento e subscrição), combinação de marcadores para aprovação automática; histórico/estado robustos.
- **0.6.0**: **Ports & UseCases** consolidados (Hexagonal), **máquina de estados** explícita, endpoints REST lapidados (201+Location; 204 No Content), **InMemoryCorrelationStore**.
- **0.7.0**: **Validações** monetárias (`@Positive`, `@Digits`), padronização de erros com **RFC 7807/ProblemDetail**, exceções 404/409, **gate de cobertura 90% (JaCoCo)**.
- **0.8.0**: **Observabilidade ponta a ponta** — Actuator/Micrometer/Prometheus, **OpenTelemetry** (agent + collector) e **Jaeger** para traces; configuração de logs com `logback-spring.xml`.

## Tecnologias por Release
| Release   | Foco                       | Principais tecnologias e padrões                                                                                      |
|-----------|----------------------------|-----------------------------------------------------------------------------------------------------------------------|
| **0.1.0** | API base                   | Java 17, Spring Boot, Records/DTOs, Validation, Clean Architecture                                                    |
| **0.2.0** | Fraudes                    | OpenFeign, WireMock (mappings/`__files`, response template), timeouts/config em `application.yml`                     |
| **0.3.0** | Persistência               | AWS SDK v2 DynamoDB Enhanced, MapStruct, LocalStack                                                                   |
| **0.4.0** | Eventos                    | SQS (publisher), Domain Events, logs estruturados                                                                     |
| **0.5.0** | Consumers                  | `PaymentResultsConsumer`, `SubscriptionResultsConsumer`, marcadores/history, aprovação automática                     |
| **0.6.0** | Design/Qualidade           | Ports/UseCases, `PolicyStateMachine` + `PolicyStatus` (enum), 201+Location, 204 No Content, correlation store, testes |
| **0.7.0** | Padronização de erros & QA | `@Positive`/`@Digits`, `ProblemDetail` (RFC 7807), exceções 404/409, Surefire/Failsafe, JaCoCo (≥90%)                 |
| **0.8.0** | Observabilidade            | Actuator + Micrometer Prometheus, OpenTelemetry (agent + collector), Jaeger, `logback-spring.xml`                     |

## Como Executar

### Requisitos
- Java 17, Docker, Docker Compose, Maven.

### Subir infraestrutura
```bash
docker compose up -d --build
# Dica: aguarde alguns segundos para Jaeger/Collector/LocalStack ficarem up
```

### Build, testes e cobertura
```bash
mvn clean verify
# Relatório JaCoCo: target/site/jacoco/index.html
```

### Rodar a aplicação
Você pode executar via Maven ou Docker (dependendo de como o compose está configurado):
```bash
# via Maven (perfil de testes já configurado no surefire)
mvn spring-boot:run

# ou usando a imagem gerada pelo compose (se aplicável)
docker compose ps
```

## Como Usar (passo a passo)
1. **Emitir solicitação**
```bash
curl -sS -X POST http://localhost:8080/policies \
  -H 'Content-Type: application/json' \
  -d '{
	"customer_id": "8d86546c-f580-40a9-ad2c-a6049b908f5b",
	"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
	"category": "AUTO",
	"salesChannel": "MOBILE",
	"paymentMethod": "CREDIT_CARD",
	"total_monthly_premium_amount": 75.25,
	"insured_amount": 275000.50,
	"coverages": {
		"Roubo": 100000.25,
		"Perda Total": 100000.25,
		"Colisão com Terceiros": 75000.00
	},
	"assistances": [
		"Guincho até 250km",
		"Troca de Óleo",
		"Chaveiro 24h"
	]
}'
```

Exemplo (200 OK):

```bash
{
	"id": "33b6db63-064b-4e8e-a918-ddbdb1a72e07",
	"customer_id": "8d86546c-f580-40a9-ad2c-a6049b908f5b",
	"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
	"category": "AUTO",
	"salesChannel": "MOBILE",
	"paymentMethod": "CREDIT_CARD",
	"status": "PENDING",
	"createdAt": "2025-08-13T00:54:11.566335758Z",
	"finishedAt": null,
	"total_monthly_premium_amount": 75.25,
	"insured_amount": 275000.50,
	"coverages": {
		"Roubo": 100000.25,
		"Perda Total": 100000.25,
		"Colisão com Terceiros": 75000.00
	},
	"assistances": [
		"Guincho até 250km",
		"Troca de Óleo",
		"Chaveiro 24h"
	],
	"history": [
		{
			"status": "RECEIVED",
			"timestamp": "2025-08-13T00:54:11.566335758Z"
		},
		{
			"status": "VALIDATED",
			"timestamp": "2025-08-13T00:54:11.622899456Z"
		},
		{
			"status": "PENDING",
			"timestamp": "2025-08-13T00:54:11.658210003Z"
		}
	]
}
```

2. **Consultar por ID**
```bash
curl -sS http://localhost:8080/policies/{id}
```

Exemplo (200 OK):

```bash
{
	"id": "33b6db63-064b-4e8e-a918-ddbdb1a72e07",
	"customer_id": "8d86546c-f580-40a9-ad2c-a6049b908f5b",
	"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
	"category": "AUTO",
	"salesChannel": "MOBILE",
	"paymentMethod": "CREDIT_CARD",
	"status": "PENDING",
	"createdAt": "2025-08-13T00:54:11.566335758Z",
	"finishedAt": null,
	"total_monthly_premium_amount": 75.25,
	"insured_amount": 275000.50,
	"coverages": {
		"Roubo": 100000.25,
		"Perda Total": 100000.25,
		"Colisão com Terceiros": 75000.00
	},
	"assistances": [
		"Guincho até 250km",
		"Troca de Óleo",
		"Chaveiro 24h"
	],
	"history": [
		{
			"status": "RECEIVED",
			"timestamp": "2025-08-13T00:54:11.566335758Z"
		},
		{
			"status": "VALIDATED",
			"timestamp": "2025-08-13T00:54:11.622899456Z"
		},
		{
			"status": "PENDING",
			"timestamp": "2025-08-13T00:54:11.658210003Z"
		}
	]
}
```

Exemplo (404 Not Found — RFC 7807):

```bash
{
	"type": "https://api.acme.com/errors/policy-not-found",
	"title": "Not Found",
	"status": 404,
	"detail": "Policy not found",
	"instance": "/policies/1b60970b-006d-4581-9b42-059085341f68",
	"policyId": "1b60970b-006d-4581-9b42-059085341f68",
	"timestamp": "2025-08-13T00:56:22.981708770Z"
}
```

3. **Consultar por Customer ID**
```bash
curl -sS "http://localhost:8080/policies?customerId={customer_id}"
```

Exemplo (200 OK):

```bash
[
    {
        "id": "33b6db63-064b-4e8e-a918-ddbdb1a72e07",
        "customer_id": "8d86546c-f580-40a9-ad2c-a6049b908f5b",
        "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
        "category": "AUTO",
        "salesChannel": "MOBILE",
        "paymentMethod": "CREDIT_CARD",
        "status": "PENDING",
        "createdAt": "2025-08-13T00:54:11.566335758Z",
        "finishedAt": null,
        "total_monthly_premium_amount": 75.25,
        "insured_amount": 275000.50,
        "coverages": {
            "Roubo": 100000.25,
            "Perda Total": 100000.25,
            "Colisão com Terceiros": 75000.00
        },
        "assistances": [
            "Guincho até 250km",
            "Troca de Óleo",
            "Chaveiro 24h"
        ],
        "history": [
            {
                "status": "RECEIVED",
                "timestamp": "2025-08-13T00:54:11.566335758Z"
            },
            {
                "status": "VALIDATED",
                "timestamp": "2025-08-13T00:54:11.622899456Z"
            },
            {
                "status": "PENDING",
                "timestamp": "2025-08-13T00:54:11.658210003Z"
            }
        ]
    }
]
```

Exemplo (404 Not Found — RFC 7807):

```bash
{
	"type": "https://api.acme.com/errors/customer-policies-not-found",
	"title": "Not Found",
	"status": 404,
	"detail": "No policy was found for this customer",
	"instance": "/policies",
	"customerId": "8d86546c-f580-40a9-ad2c-a6049b908f5b",
	"timestamp": "2025-08-12T20:23:28.742279807Z"
}
```

4. **Cancelar por ID**
```bash
curl -sS -X PATCH http://localhost:8080/policies/{id}/cancel
```

Exemplo (204 No Content):
```bash
HTTP/1.1 204 No Content
```

Exemplo (404 Not Found — RFC 7807):
```bash
{
	"type": "https://api.acme.com/errors/policy-not-found",
	"title": "Not Found",
	"status": 404,
	"detail": "Policy not found",
	"instance": "/policies/48ffd5c1-4f9d-4bcc-9111-83bd49814c07/cancel",
	"policyId": "48ffd5c1-4f9d-4bcc-9111-83bd49814c07",
	"timestamp": "2025-08-12T20:23:43.884029791Z"
}
```

Exemplo (409 Conflict — RFC 7807):
```bash
{
	"type": "https://api.acme.com/errors/policy-cancel-conflict",
	"title": "Policy cancel conflict",
	"status": 409,
	"detail": "It is not possible to cancel a policy with a final status: CANCELLED",
	"instance": "/policies/9a87aba6-ec14-4429-8286-d4d70402acc6/cancel",
	"policyId": "9a87aba6-ec14-4429-8286-d4d70402acc6",
	"currentStatus": "CANCELLED",
	"timestamp": "2025-08-13T01:00:32.054432082Z"
}
```

### Collection do Insomnia
A coleção [acme-policy-service-collection.json](./docs/acme-policy-service-collection.json) está disponível no repositório e contém chamadas prontas para:
- `POST /policies` (emissão)
- `GET /policies/{id}` (consulta por id)
- `GET /policies?customerId={customer_id}` (consulta por cliente)
- `PATCH /policies/{id}/cancel` (cancelamento)

## Comportamento da Aplicação
- **API de Fraudes (WireMock)**: stubs com **response templating** permitem simular respostas variadas; opcionalmente, pode-se **randomizar a classificação de risco** para aproximar do mundo real (documente os templates usados).
- **Regras de negócio**: a classificação recebida ativa **regras** que alteram o status da solicitação (e.g., `REJECTED`, `VALIDATED`/`PENDING`).
- **Fluxo assíncrono (SQS)**: após validação, a solicitação pode ficar **PENDING** até que **pagamento** e **subscrição** retornem **APROVADOS** — combinação que leva a **APPROVED**; eventos contrários levam a **REJECTED**.
- **Histórico e estado**: mudanças são registradas em histórico; estados finais encerram o ciclo.

## Observabilidade
- **Actuator**: `GET /actuator`, `GET /actuator/health`, `GET /actuator/metrics`, `GET /actuator/prometheus`.
- **Traces (OpenTelemetry + Jaeger)**:
  - O agente Java do OTel é injetado via `-javaagent:`;
  - O **Collector** recebe/exporta via OTLP;
  - A UI do **Jaeger** permite buscar pelo `OTEL_SERVICE_NAME` configurado.
- **Logs**: `logback-spring.xml` define formato/níveis; parâmetros de log podem ser ajustados por pacote.
> Dica: gere algumas requisições (POST/GET/PATCH) e observe os spans no Jaeger (latência, dependências, tags).

## DynamoDB (LocalStack) & NoSQL Workbench
- Listar tabelas (LocalStack):
```bash
awslocal dynamodb list-tables
```

- **Ver itens por chave** (ajuste `--table-name` e chaves conforme sua modelagem):
```bash
awslocal dynamodb get-item \
  --table-name PolicyRequests \
  --key '{"id":{"S":"<ID-DA-POLICY>"}}'
```

- **NoSQL Workbench**: aponte para `http://localhost:4566` (ou o endpoint LocalStack configurado) para inspecionar entidades e GSI.

## Cenários de Teste (manuais)

### Cenário A — REJECTED por regra de negócio
1. Envie `POST /policies` com payload que viole os limites de aprovação para a classificação do cliente (simulada pelo WireMock).
2. Verifique `status: REJECTED` ao consultar `GET /policies/{id}`.

### Cenário B — PENDING → APPROVED via eventos
1. Envie `POST /policies` e verifique `status: PENDING`.
2. **Envie evento de pagamento aprovado** para a fila de pagamento.
3. **Envie evento de subscrição autorizada** para a fila de subscrição.
4. Consulte `GET /policies/{id}` e espere `status: APPROVED`.

**Scripts de Mensageria (SQS/LocalStack)**

Os scripts abaixo publicam eventos nas filas **payment-topic** e **insurance-subscriptions-topic** para **confirmar** ou **negar** as etapas do fluxo. Eles usam o endpoint do **LocalStack** e as configurações do projeto

| Script                                 | Ação                     | Fila alvo (padrão)              | Observação                                               |
|----------------------------------------|--------------------------|---------------------------------|----------------------------------------------------------|
| `tools/send-payment-approved.sh`       | Confirma **pagamento**   | `payment-topic`                 | Define `status=APPROVED` para o `requestId` informado    |
| `tools/send-payment-denied.sh`         | Nega **pagamento**       | `payment-topic`                 | Define `status=DENIED` para o `requestId` informado      |
| `tools/send-subscription-approved.sh`  | Autoriza **subscrição**  | `insurance-subscriptions-topic` | Define `status=AUTHORIZED` para o `requestId` informado  |
| `tools/send-subscription-denied.sh`    | Nega **subscrição**      | `insurance-subscriptions-topic` | Define `status=DENIED` para o `requestId` informado      |

Uso rápido:

```bash
# Passe o ID da solicitação (requestId) via variável de ambiente
REQUEST_ID=<UUID-DA-POLICY> sh tools/scripts/send-payment-approved.sh
REQUEST_ID=<UUID-DA-POLICY> sh tools/scripts/send-payment-denied.sh
REQUEST_ID=<UUID-DA-POLICY> sh tools/scripts/send-subscription-approved.sh
REQUEST_ID=<UUID-DA-POLICY> sh tools/scripts/send-subscription-denied.sh
```

Exemplo:

```bash
REQUEST_ID=8d86546c-f580-40a9-ad2c-a6049b908f5b sh tools/scripts/send-payment-approved.sh
```

### Histórico de mudanças
Consulte o arquivo [CHANGELOG.md](./CHANGELOG.md) para ver as alterações de cada versão.

## Escopo do Desafio
Os requisitos e critérios de avaliação estão documentados no arquivo de [DESAFIO.md](./DESAFIO.md). Este README traz o racional das decisões e instruções para execução e validação da solução.