# Desafio Seguradora ACME

Este repositório contém o código do projeto desenvolvido para o desafio **Seguradora ACME**. 

O objetivo deste desafio é criar um **microsserviço orientado a eventos** para gerenciar **solicitações de apólice de seguros**, integrando com **API de Fraudes**, aplicando **regras de negócio** e publicando **eventos de mudanças de estado**.

## Tecnologias, Frameworks, Bibliotecas e Padrões Utilizados
- **Java 17**
- **Spring Boot 3.5.4**
- **Spring Cloud OpenFeign** – Integração com APIs externas.
- **WireMock** – Simulação de API de Fraudes com stubs e arquivos em `__files`.
- **Jackson** – Serialização/deserialização JSON.
- **Clean Architecture** – Organização de pacotes e separação de responsabilidades.
- **Maven** – Gerenciamento de dependências e build.
- **LocalStack** – Emulação local de serviços AWS (DynamoDB) para desenvolvimento e testes.
- **AWS SDK v2 – DynamoDB Enhanced Client** – Persistência no DynamoDB.
- **MapStruct** – Mapeamento entre o domínio e modelos de persistência.
- **Lombok** – Redução de boilerplate com geração automática de getters/setters.

## Funcionalidades
- Criar solicitação de apólice (`POST /policies`)
- Consultar apólice por ID (`GET /policies/{id}`)
- Consultar apólices por Customer ID (`GET /policies?customerId=...`)
- Realizar análise de fraude durante a criação da apólice (simulada com WireMock)
- Persistência de apólices no Amazon DynamoDB.
- Consulta de apólices usando índice secundário global (`GSI`).

## Estrutura do Projeto

O projeto está organizado da seguinte forma:

- **src/main/java** - Código-fonte da aplicação.
- **src/test/java** - Testes unitários e de integração.
- **src/main/resources** - Arquivos de configuração (por exemplo, `application.properties` ou `application.yml`).
- **infra/dynamodb** – Contém a implementação de repositório (`PolicyDynamoRepository`) e o modelo de persistência (`PolicyItem`).
- **infra/dynamodb/mapper** – Contém o `PolicyItemMapper` (MapStruct) para conversão de objetos.

## Como Rodar o Projeto

Para rodar a aplicação localmente, siga os seguintes passos:

### Requisitos

- **Java 17** - Certifique-se de que o Java está instalado em sua máquina.
- **Maven** - Ferramentas de gerenciamento de dependências e build.

### Passos

1. Clone o repositório:
```bash
git clone https://github.com/caaiobomfim/acme-policy-service.git
```

2. Navegue até o diretório do projeto:
```bash
cd acme-policy-service
```

3. Suba o ambiente de mock de fraudes e localstack:
```bash
docker compose up -d
```

4. Compile e execute o projeto:
```bash
mvn clean install
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080` e o mock de fraudes em `http://localhost:8089/v1/fraud_analysis`.

### Como Testar
Descreva como rodar os testes do projeto.

1. Execute os testes unitários:
```bash
mvn test
```

2. (Opcional) Se o projeto possui testes de integração, explique como executá-los.

### Testes Manuais com Insomnia

As três rotas implementadas nesta versão foram validadas manualmente utilizando o Insomnia.

Endpoints testados:
1. Criar solicitação (`POST /policies`)
- Método: POST
- URL: http://localhost:8080/policies
- Body (JSON):
```bash
{
	"customer_id": "adc56d77-348c-4bf0-908f-22d40e2e715c",
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
}
```

2. Consultar por ID (`GET /policies/{id}`)
- Método: GET
- URL: http://localhost:8080/policies/{id}
```bash
{
	"id": "00c67838-291a-4d77-8ec1-08523d1532d7",
	"customer_id": "adc56d77-348c-4bf0-908f-22d40e2e715c",
	"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
	"category": "AUTO",
	"salesChannel": "MOBILE",
	"paymentMethod": "CREDIT_CARD",
	"status": "RECEIVED",
	"createdAt": "2025-08-08T22:27:05.109762-03:00",
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
			"timestamp": "2025-08-08T22:27:05.109762-03:00"
		}
	]
}
```

3. Consultar por Customer ID (`GET /policies?customerId=`)
- Método: GET
- URL: http://localhost:8080/policies?customerId={uuid}
```bash
[
	{
		"id": "01aa42cf-c920-4e77-a10e-8b4cece9e1c3",
		"customer_id": "adc56d77-348c-4bf0-908f-22d40e2e715c",
		"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
		"category": "AUTO",
		"salesChannel": "MOBILE",
		"paymentMethod": "CREDIT_CARD",
		"status": "RECEIVED",
		"createdAt": "2025-08-08T22:26:28.7888718-03:00",
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
				"timestamp": "2025-08-08T22:26:28.7888718-03:00"
			}
		]
	},
	{
		"id": "00c67838-291a-4d77-8ec1-08523d1532d7",
		"customer_id": "adc56d77-348c-4bf0-908f-22d40e2e715c",
		"product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
		"category": "AUTO",
		"salesChannel": "MOBILE",
		"paymentMethod": "CREDIT_CARD",
		"status": "RECEIVED",
		"createdAt": "2025-08-08T22:27:05.109762-03:00",
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
				"timestamp": "2025-08-08T22:27:05.109762-03:00"
			}
		]
	}
]
```

#### Fraud API (mockada)
- `GET /v1/fraud_analysis?orderId=123&customerId=456`

### Histórico de mudanças
Consulte o arquivo [CHANGELOG.md](./CHANGELOG.md) para ver as alterações de cada versão.

## 📄 Desafio
Para mais detalhes sobre o escopo e requisitos do desafio, consulte o arquivo [DESAFIO.md](./DESAFIO.md).