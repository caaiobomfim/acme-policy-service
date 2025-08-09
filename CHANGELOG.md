# Changelog

## [0.3.0] - 2025-08-08
### Visão Geral
Nesta release, evoluímos o projeto para integrar com o **Amazon DynamoDB** utilizando o DynamoDB **Enhanced Client** do **AWS SDK v2**.
Foi implementada toda a camada de persistência (`PolicyDynamoRepository`) para salvar e consultar apólices de seguros no DynamoDB, mapeando o domínio para um modelo de armazenamento (`PolicyItem`).
Também introduzimos o **MapStruct** para automatizar o mapeamento entre `Policy` (domínio) e `PolicyItem` (persistência), reduzindo código boilerplate e centralizando as conversões.

### Stack Técnico e Padrões
- **AWS SDK v2** – **DynamoDB Enhanced Client**.
- **MapStruct** para conversões entre objetos.
- **Lombok** para geração de getters/setters.
- **Maven** com configuração de `annotationProcessorPaths` para MapStruct e Lombok.
- **LocalStack** para emular serviços AWS (DynamoDB).

### Aprendizados
- **Repository Pattern** aplicado para encapsular o acesso ao DynamoDB (`PolicyDynamoRepository`).
- Uso de **@DynamoDbBean**, **@DynamoDbPartitionKey** e **@DynamoDbSecondaryPartitionKey** para simplificar o mapeamento objeto-tabela.
- Separação de responsabilidades com um **Mapper dedicado** (`PolicyItemMapper`) para manter conversões isoladas e testáveis.
- Configuração do **DynamoDbEnhancedClient** centralizada para garantir reuso e consistência.
- Aprendizado sobre **problemas com ClassCastException** quando o Spring DevTools recarrega classes usadas pelo **Enhanced Client**, reforçando boas práticas para evitar conflitos de classloader.
- Melhoria no `pom.xml` para suportar **annotation processors** do Lombok e MapStruct em conjunto (ordem correta no `annotationProcessorPaths`).

### Adicionado
- Integração com **Amazon DynamoDB** utilizando **DynamoDB Enhanced Client**.
- Implementação do `PolicyDynamoRepository` para persistência e consulta de políticas no DynamoDB.
- Criação do `PolicyItem` anotado com `@DynamoDbBean`, incluindo chave primária (`@DynamoDbPartitionKey`) e chave secundária global (`@DynamoDbSecondaryPartitionKey`).
- Mapeamento entre `Policy` e `PolicyItem` utilizando **MapStruct**, com métodos auxiliares para conversão de tipos complexos (BigDecimal, OffsetDateTime, Map e List).
- Configuração do `DynamoDbEnhancedClient` e da injeção do `DynamoDbTable<PolicyItem>` no repositório.

### Alterado
- Extração de lógica de mapeamento de atributos para uma classe Mapper dedicada (`PolicyItemMapper`), separando conversões utilitárias.
- Ajustes no `pom.xml` para incluir dependências do **MapStruct**, **Lombok** e **AWS SDK DynamoDB Enhanced** com suporte a annotation processors.

### Corrigido
- Problema de `ClassCastException` causado pelo `Spring DevTools` e o carregamento de classes do DynamoDB Enhanced Client.

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

