# Desafio Seguradora ACME

Este repositório contém o código do projeto desenvolvido para o desafio **Seguradora ACME**. 

O objetivo deste desafio é criar um **microsserviço orientado a eventos** para gerenciar **solicitações de apólice de seguros**, integrando com **API de Fraudes**, aplicando **regras de negócio** e publicando **eventos de mudanças de estado**.

## Tecnologias Utilizadas

- **Java** - Linguagem de programação utilizada para o desenvolvimento do projeto.
- **Spring Boot** - Framework utilizado para construção da aplicação.
- **Feign** - Cliente HTTP para integração com APIs externas.

## Funcionalidades

...

## Estrutura do Projeto

O projeto está organizado da seguinte forma:

- **src/main/java** - Código-fonte da aplicação.
- **src/test/java** - Testes unitários e de integração.
- **src/main/resources** - Arquivos de configuração (por exemplo, `application.properties` ou `application.yml`).

## Como Rodar o Projeto

Para rodar a aplicação localmente, siga os seguintes passos:

### Requisitos

- **Java 11 ou superior** - Certifique-se de que o Java está instalado em sua máquina.
- **Maven ou Gradle** - Ferramentas de gerenciamento de dependências e build.

### Passos

1. Clone o repositório:
```bash
git clone https://github.com/usuario/repo.git
```

2. Navegue até o diretório do projeto:
```bash
cd desafio
```

3. Compile o projeto com o Maven:
```bash
mvn clean install
```

4. Execute a aplicação:
```bash
mvn spring-boot:run
```

A aplicação estará rodando localmente no `http://localhost:8080`.

### Como Testar
Descreva como rodar os testes do projeto.

1. Execute os testes unitários:
```bash
mvn test
```

2. (Opcional) Se o projeto possui testes de integração, explique como executá-los.

### Contribuindo
Se você deseja contribuir para este projeto, siga as etapas abaixo:

1. Fork este repositório.
2. Crie uma branch para sua modificação:
```bash
git checkout -b minha-modificacao
```
3. Faça as alterações necessárias.
4. Commit e push:
```bash
git commit -am "Descrição das alterações"
git push origin minha-modificacao
```
5. Abra um Pull Request.

### Licença
Este projeto está sob a licença [nome da licença]. Veja o arquivo LICENSE para mais informações.

### Contato
Se tiver dúvidas ou sugestões, entre em contato:
- **E-mail**: [seu-email@dominio.com]
- **LinkedIn**: [seu-linkedin]

### Explicações de Seções:
- **Objetivo do Desafio**: Aqui você pode explicar rapidamente o que é o projeto e o que ele pretende resolver ou alcançar.
- **Tecnologias Utilizadas**: Liste as tecnologias que você utilizou no projeto.
- **Funcionalidades**: Descreva brevemente as funcionalidades que a aplicação oferece.
- **Estrutura do Projeto**: Mostre a estrutura do projeto para facilitar a navegação.
- **Como Rodar o Projeto**: Explicação detalhada sobre como configurar e rodar o projeto localmente.
- **Como Testar**: Mostre como os testes são executados (unitários, integração, etc.).
- **Contribuindo**: Se você deseja permitir contribuições, inclua o processo para isso.
- **Licença**: Especifique a licença do projeto (se houver).
- **Contato**: Ofereça formas de entrar em contato.