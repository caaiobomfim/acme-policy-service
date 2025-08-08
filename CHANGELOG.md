# Changelog

## [0.2.0] - 2025-08-08
### Visão Geral
Implementação da integração com a **API de Fraudes** para análise de risco durante a criação de solicitações de apólice, utilizando **OpenFeign** e mock de resposta com **WireMock** para testes locais.

### Stack Técnico e Padrões
- OpenFeign para consumo de API externa.
- WireMock com `mappings` e `__files` para simulação de respostas da API de Fraudes.
- Resposta dinâmica usando **Response Template** no WireMock.
- Propriedades customizadas no `application.yml` para configuração de URL e timeouts da integração.

### Aprendizados
- Configuração e uso de **Feign Clients** com interceptors customizados.
- Parametrização de timeouts de conexão e leitura via `application.yml`.
- Organização de stubs WireMock com `bodyFileName` e arquivos JSON no diretório `__files`.
- Boas práticas para desacoplamento usando **Portas e Adaptadores (Hexagonal Architecture)**.

### Adicionado
- DTO `FraudAnalysisResponse` e record `Occurrence` para mapear a resposta da API de Fraudes.
- Interface `FraudGateway` para abstração da integração.
- Implementação `FraudGatewayFeignAdapter` usando Feign Client.
- Configuração `FraudFeignConfig` com interceptors e logger level customizado.
- Arquivo `wiremock/mappings/fraud-analysis.json` e `wiremock/__files/fraud-analysis-body.json` para simulação de resposta mockada.

### Alterado
- Atualização do `application.yml` para incluir propriedades de `policy.fraud.base-url`, `connectTimeout` e `readTimeout`.
- Ajustes na camada de serviço para invocar a API de Fraudes durante a criação de apólices.

### Removido
- Não houve remoções nesta versão.

### Notas
- Atualmente, a integração utiliza dados mockados pelo WireMock.

## [0.1.0] - 2025-08-07
### Visão Geral
Versão inicial do microsserviço de solicitações de apólice, contemplando configuração base, endpoints de criação e consulta, e estrutura inicial de camadas.

### Stack Técnico e Padrões
- Java 17.
- Spring Boot 3.5.4.
- Maven.
- Jackson para serialização/deserialização JSON.
- Jakarta Validation para validação de dados de entrada.
- Estrutura de pacotes alinhada ao Clean Architecture.

### Aprendizados
- Padronização de commits usando Conventional Commits + Gitmoji.
- Estruturação inicial de um projeto com records e DTOs imutáveis.
- Configuração de `application.yml` com propriedades customizadas.

---

### Adicionado
- Criação dos DTOs `PolicyRequestDto` e `PolicyResponseDto` para definição do contrato da API, utilizando Java records e `@JsonProperty` para mapeamento JSON.
- Definição da interface `PolicyService` com métodos principais para manipulação de solicitações de apólice.
- Implementação inicial de `PolicyServiceImpl` com lógica em memória retornando dados mockados para criação e consulta de solicitações.
- Criação do `PolicyController` com endpoints:
    - `POST /policies` – criação de nova solicitação de apólice.
    - `GET /policies/{id}` – consulta por ID.
    - `GET /policies?customerId=` – consulta por ID de cliente.
- Conversão do arquivo de configuração de `application.properties` para `application.yml` e inclusão do `server.port` padrão.

### Alterado
- Ajuste da estrutura de pacotes para melhor organização e manutenção.

### Removido
- Remoção das dependências do Testcontainers não utilizadas no `pom.xml`.

### Notas
- A implementação atual retorna dados mockados.
- As anotações `@JsonProperty` garantem o mapeamento correto em snake_case conforme o contrato da API.
- Foram realizadas chamadas de teste no Insomnia para validar o comportamento das três rotas implementadas (`POST /policies`, `GET /policies/{id}`, `GET /policies?customerId=`).

