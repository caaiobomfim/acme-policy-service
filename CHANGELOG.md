# Changelog

## [0.1.0] - 2025-08-07
### Adicionado
- Criação dos DTOs `PolicyRequestDto` e `PolicyResponseDto` para definição do contrato da API, utilizando Java records e `@JsonProperty` para mapeamento JSON.
- Definição da interface `PolicyService` com métodos principais para manipulação de solicitações de apólice.
- Implementação inicial de `PolicyServiceImpl` com lógica em memória retornando dados mockados para criação e consulta de solicitações.
- Criação do `PolicyController` com endpoints:
    - `POST /policies` – criação de nova solicitação de apólice
    - `GET /policies/{id}` – consulta por ID
    - `GET /policies?customerId=` – consulta por ID de cliente
- Configuração base do projeto com Spring Boot 3.5.4, Java 17 e Maven.
- Conversão do arquivo de configuração de `application.properties` para `application.yml` e inclusão do `server.port` padrão.

### Alterado
- Ajuste da estrutura de pacotes para melhor organização e manutenção.

### Removido
- Remoção das dependências do Testcontainers não utilizadas no `pom.xml`.

### Notas
- A implementação atual retorna dados mockados.
- As anotações `@JsonProperty` garantem o mapeamento correto em snake_case conforme o contrato da API.
