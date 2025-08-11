# Changelog

## [0.6.0] - 2025-08-11
### Visão Geral
Refatoração grande para **portas e casos de uso** (Clean/Hexagonal), formalização da **máquina de estados** e ajustes REST. Também habilitamos toggles de mensageria e estabilizamos build/testes.

### Stack Técnico e Padrões
- **Use Case / Query pattern**: `CreatePolicyUseCase`, `CancelPolicyUseCase`, `GetPolicyByIdQuery`, `ListPoliciesByCustomerQuery` (interfaces com `execute(...)`) + implementações.
- **Ports & Adapters**: `PolicyRepository`, `PolicyRequestPublisher`, `FraudGateway`, `ApiPolicyMapper`.
- **State Pattern**: `PolicyStateMachine` + `PolicyStatus` (enum) controlando transições.
- **Feature toggles** com `@ConditionalOnProperty` (publisher/listeners SQS).
- **@ConfigurationProperties**: `AppProps` (aws/sqs/dynamo).
- **@EnableScheduling** via `SchedulingConfig`.

### Adicionado
- **Endpoint de cancelamento** de solicitação retornando **204 No Content**.
- **`Location` header** no `@PostMapping` de criação (201 com URI do recurso).
- `InMemoryCorrelationStore` para correlacionar pagamento/subscrição que chegam fora de ordem.

### Alterado
- Controller deixa de chamar `PolicyServiceImpl` diretamente e passa a delegar para **UseCases/Queries**.
- **`Policy.status`** de `String` → `PolicyStatus` (**enum**) com transições mediadas por `PolicyStateMachine`. **Breaking**: atualize contratos/mapeamentos que esperavam `String`.
- Serviços de aplicação reescritos para depender de **ports**:  
  `CreatePolicyService` (usa `PolicyRepository`, `PolicyRequestPublisher`, `FraudGateway`, `ApiPolicyMapper`, `PolicyStateMachine`),  
  `GetPolicyByIdService` e `ListPoliciesByCustomerService` (usam `PolicyRepository` + `ApiPolicyMapper`),  
  `CancelPolicyService` (usa `PolicyRepository`, `PolicyStateMachine`, `ApiPolicyMapper`).
- `PolicyStateMachine` passa a interagir com `PolicyRepository`, `PolicyRequestPublisher` e `InMemoryCorrelationStore`.
- `PolicyDynamoRepository` refatorado para utilizar **mappers de conversão**.

### Corrigido
- Falha no `mvn clean install` por ausência do bean `PolicyRequestPublisher` em testes: resolvido com **publisher no-op** via `MessagingNoopConfig`.

### Config & Infra
- Reorganização do **`application.yml`** (grupos aws/sqs/dynamo) e criação do **`application-test.yml`**.
- **Docker Compose**: container da aplicação na **8080**, variáveis de ambiente e **network bridge**.
- **Dockerfile** multi-stage (builder).
- **Maven**: `maven-surefire-plugin` configurando `spring.profiles.active=test`.

### Notas
- Reestruturei o módulo para casos de uso, isolando regras de domínio da camada web.
- Modelei a máquina de estados com `PolicyStateMachine` + `PolicyStatus`, deixando as transições explícitas e testáveis.
- Tratei a correlação assíncrona (pagamento/subscrição) com o `InMemoryCorrelationStore` para aprovar somente quando ambos chegarem.
- Mantive o build estável em ambientes sem SQS com toggles e publisher no-op.

## [0.5.0] - 2025-08-10
### Visão Geral
Nesta release, o microsserviço evoluiu o fluxo assíncrono de integração com a adição de dois novos **consumidores SQS**: `PaymentResultsConsumer` e `SubscriptionResultsConsumer`.  
Esses consumidores processam eventos de pagamento e subscrição, aplicando regras de negócio para alteração do status da apólice, registro de históricos e publicação de eventos de mudança de status.  
Foi implementada a detecção de combinações de marcadores (`PAYMENT_CONFIRMED` e `SUBSCRIPTION_AUTHORIZED`) para aprovação automática, bem como a finalização do ciclo da apólice (`finishedAt`) nos casos de aprovação ou rejeição.

### Stack Técnico e Padrões
- **Spring Cloud AWS SQS** para consumo de mensagens com `@SqsListener`.
- **LocalStack** para emulação local das filas de pagamento e subscrição.
- Métodos imutáveis no agregado `Policy` para:
  - Adicionar entradas de histórico (`withHistoryEntry`).
  - Alterar status e registrar histórico (`withStatusAndHistory`).
  - Encerrar ciclo (`withFinishedAt`).
  - Verificar presença de marcadores (`hasHistory`).
- Marcadores e status tratados com `equalsIgnoreCase` para robustez.
- Logs estruturados para rastreabilidade dos eventos recebidos e decisões tomadas.

### Aprendizados
- Boas práticas para consumidores SQS desacoplados, com responsabilidades claras e reutilização de lógica no domínio (`Policy`).
- Estratégias para correlacionar múltiplos eventos assíncronos e disparar mudanças de estado somente quando todas as condições forem atendidas.
- Benefícios de manter o domínio imutável e centralizar alterações de estado no agregado.

### Adicionado
- `PaymentResultsConsumer`:
  - Processamento de eventos `PaymentResultEvent`.
  - Registro de marcador `PAYMENT_CONFIRMED` no histórico.
  - Alteração para `REJECTED` em pagamentos negados.
  - Aprovação automática (`APPROVED`) quando `PAYMENT_CONFIRMED` e `SUBSCRIPTION_AUTHORIZED` estão presentes no histórico.
- `SubscriptionResultsConsumer`:
  - Processamento de eventos `SubscriptionResultEvent`.
  - Registro de marcador `SUBSCRIPTION_AUTHORIZED` no histórico.
  - Alteração para `REJECTED` em subscrições negadas.
  - Aprovação automática (`APPROVED`) quando ambos os marcadores estão presentes.
- Métodos no agregado `Policy`:
  - `withHistoryEntry` e `withHistoryEntryNow`.
  - `hasHistory` para verificação de marcadores.
  - Ajustes em `withStatusAndHistory` para uso consistente.
  - `withFinishedAt` para registro do encerramento do ciclo.

### Alterado
- Refatoração do `PaymentResultsConsumer` para utilizar métodos específicos do agregado `Policy` em vez de manipulação direta da lista de histórico.
- Alinhamento do `SubscriptionResultsConsumer` com o padrão de implementação do `PaymentResultsConsumer`.

### Notas
- Todo o fluxo pode ser testado localmente com LocalStack e mensagens mockadas no SQS.
- A decisão de aprovação automática é sensível à ordem de chegada dos eventos, sendo persistida a informação de cada marcador no histórico até que a combinação necessária seja atingida.

## [0.4.0] - 2025-08-09
### Visão Geral
Nesta release, o microsserviço passou a publicar eventos de solicitações de apólice em uma fila **Amazon SQS** após a persistência no **DynamoDB**, simulando o fluxo assíncrono de integração entre sistemas.
Também foi implementada a aplicação de regras de negócio com base na **classificação de risco** retornada pela API de Fraudes, ajustando o status da apólice e registrando o histórico de alterações.

### Stack Técnico e Padrões
- **AWS SDK v2 + Spring Cloud AWS** para integração com o SQS.
- **LocalStack** para emulação local do SQS.
- **Publisher dedicado** (`PolicyRequestPublisher`) para envio de mensagens.
- **Domain Events** (`PolicyRequestCreatedEvent`, `PolicyRequestStatusChangedEvent`) para representar mudanças de estado no domínio.
- Aplicação de **regras de negócio** via `FraudRules` e `FraudClassification`.
- Uso consistente de **OffsetDateTime UTC** para garantir coerência temporal em persistência e publicação.

### Aprendizados
- Integração assíncrona com SQS utilizando `SqsTemplate` e boas práticas de isolamento em adapters.
- Controle de estado de domínio e histórico utilizando métodos imutáveis (`withStatusAndHistory`) no agregado `Policy`.
- Estratégias para manter timestamps consistentes entre persistência no DynamoDB e publicação de eventos.
- Simulação de cenários de classificação de fraude via WireMock com respostas dinâmicas.

### Adicionado
- Integração com **Amazon SQS** para publicação de eventos de criação e mudança de status de solicitações.
- Implementação de `PolicyRequestPublisher` com métodos para publicar `PolicyRequestCreatedEvent` e `PolicyRequestStatusChangedEvent`.
- Regras de aprovação/reprovação com base na classificação de risco (`FraudClassification`) e categoria/valor da apólice (`FraudRules`).
- Ajuste do fluxo no `PolicyServiceImpl` para:
  - Persistir a apólice no DynamoDB.
  - Publicar evento de criação no SQS.
  - Consultar a **API de Fraudes**.
  - Alterar o status para `VALIDATED`/`PENDING` ou `REJECTED` conforme regra.
  - Publicar eventos de mudança de status no SQS.

### Alterado
- Atualização do `docker-compose.yml` para incluir o serviço do **LocalStack** com suporte ao SQS.
- Ajustes no `application.yml` para configurar propriedades de AWS e endpoint do SQS.
- Adequação da integração com API de Fraudes para permitir simulação de múltiplas classificações.

### Notas
- Todo o fluxo de persistência e publicação pode ser testado localmente com **LocalStack** e **WireMock**.
- A fila utilizada (`orders-topic`) é não-FIFO e configurada no ambiente local.
- Os timestamps de status e eventos são mantidos em UTC para evitar inconsistências entre persistência e mensageria.

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

