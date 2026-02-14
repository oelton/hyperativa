# API de Cadastro e Consulta de Cartões

Este projeto implementa uma API REST para:
- Autenticação de usuários via JWT
- Inserção de números de cartão (unitário e em arquivo TXT)
- Consulta da existência de um cartão, retornando seu identificador interno
- Log das requisições e respostas
- Auditoria automática de criação e modificação de entidades

Atenção: números de cartão são dados sensíveis. Nesta solução eles são:
- Armazenados com segurança: hash SHA-256 para busca e cifra simétrica (Spring Security Encryptors) para preservação do valor completo de forma confidencial.

## Stack
- Java 17, Spring Boot (Web, Security, Validation, JPA)
- MySQL (Docker via compose.yaml)
- JWT (JJWT)
- Lombok para redução de boilerplate
- Swagger/OpenAPI para documentação da API

## Como executar

1) Subir o MySQL com Docker Compose:

```
docker compose up -d
```

O compose cria o DB `mydatabase` com usuário `myuser` e senha `secret` na porta 3306.

2) Configurar variáveis de ambiente (recomendado):

Crie um arquivo `.env` na raiz do projeto (use `.env.example` como referência):

- `JWT_SECRET`: segredo HMAC para assinar tokens JWT (mínimo 32 caracteres/256 bits para HS256)
- `JWT_EXPIRATION`: tempo de expiração do token em milissegundos (padrão: 3600000 = 1 hora)
- `JWT_ISSUER`: emissor do token (padrão: hyperativa-api)
- `JWT_AUDIENCE`: audiência do token (padrão: hyperativa-web)

⚠️ **Importante**: Os valores padrões no `application.yaml` são apenas para desenvolvimento.

3) Rodar a aplicação:

```
./mvnw spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

## Endpoints

Autenticação:
- `POST /auth/register` — cria usuário
  - Body:
    ```json
    {"username": "user", "password": "pass"}
    ```
  - 200 em sucesso; 400 se já existir.

- `POST /auth/login` — autentica e retorna token JWT
  - Body: igual ao register
  - Resposta:
    ```json
    {"token":"<jwt>"}
    ```

Cartões (exige Bearer Token):
- `POST /cards` — cadastra cartão unitário
  - Body:
    ```json
    {"cardNumber":"4111111111111111"}
    ```
  - Resposta:
    ```json
    {"id": 123}
    ```

- `POST /cards/upload` — upload de TXT (multipart) com um cartão por linha
  - Form field `file`: arquivo `.txt`
  - Resposta: texto com a quantidade processada

- `GET /cards/exists?number=4111111111111111` — verifica existência
  - 200 e `{ "id": 123 }` se encontrado
  - 404 se não encontrado

## Documentação e Testes (Swagger & Postman)
- **Swagger UI**: Disponível em `http://localhost:8080/swagger-ui.html` após iniciar a aplicação.
- **Postman Collection**: O arquivo `Hyperativa_CRUD_API.postman_collection.json` na raiz do projeto pode ser importado no Postman para facilitar os testes.
  - A collection inclui scripts para salvar automaticamente o token JWT após o login bem-sucedido.
  - Certifique-se de definir a variável `base_url` no ambiente do Postman (padrão `http://localhost:8080`).

## Segurança e Armazenamento
- **Busca por cartão**: via `SHA-256` do número (campo `cardNumberHash`, index único)
- **Persistência do número completo**: cifrado com `Encryptors.text` (chave derivada de `api.security.token.secret` — em produção, use segredo e salt distintos e seguros)
- **Autenticação**: JWT (stateless) com validação de emissor e audiência, filtro adiciona autenticação no contexto
- **Autorização**: `/auth/**` público; demais endpoints exigem Bearer token
- **Tratamento de exceções customizado**: `TokenException` para erros relacionados a JWT, `HashGenerationException` e `FileProcessingException` para outras operações
- **Auditoria**: Entidades auditáveis com campos `createdAt`, `updatedAt` e `createdBy` usando JPA Auditing

## Logs
Todas as requisições são logadas (método, caminho, usuário/autenticado ou anônimo, status HTTP, duração em ms).

## Variáveis de Configuração (application.yaml)
- `spring.datasource.*`: parâmetros do MySQL
- `spring.jpa.hibernate.ddl-auto`: `update` para dev; em produção, gerencie migrações com Flyway/Liquibase
- `spring.jpa.properties.hibernate.globally_quoted_identifiers`: true para compatibilidade com palavras reservadas
- `api.security.token.secret`: chave HMAC para JWT (mínimo 256 bits)
- `api.security.token.expiration`: tempo de vida do token (ms)
- `api.security.token.issuer`: emissor do token
- `api.security.token.audience`: audiência do token

## Build/Run alternativo
```bash
./mvnw clean package
java -jar target/crud-0.0.1-SNAPSHOT.jar
```

## Estrutura do Projeto
```
src/main/java/com/hyperativa/crud/
├── config/              # Configurações (JPA Auditing, Security)
├── controller/          # Endpoints REST
├── domain/
│   ├── model/          # Entidades JPA (User, Card, Auditable)
│   └── repository/     # Repositórios Spring Data JPA
├── dto/                # DTOs de requisição/resposta
├── exception/          # Exceções customizadas (TokenException, etc)
├── filter/             # Filtros de segurança (JwtAuthenticationFilter)
├── security/           # Serviços de segurança (TokenService)
└── service/            # Lógica de negócio
```