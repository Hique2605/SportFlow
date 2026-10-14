# SportFlow API

API REST para gerenciamento e agendamento de quadras esportivas. Esta versão entrega a base em camadas, cinco CRUDs completos, validações, tratamento centralizado de erros, migrations Flyway e autenticação/autorização com JWT. A documentação Swagger será adicionada em uma etapa posterior.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- Spring Security
- Auth0 Java JWT
- Bean Validation
- PostgreSQL e Flyway
- H2 nos testes de integração

## Pré-requisitos

1. JDK 21 configurado em `JAVA_HOME`.
2. PostgreSQL em execução.
3. Banco vazio chamado `sportflow`.
4. Variáveis de ambiente baseadas em `.env.example`.

No PowerShell, um exemplo para a sessão atual é:

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.2\jbr"
$env:DB_PASSWORD = "sua-senha-do-postgres"
$env:JWT_SECRET = "um-segredo-longo-com-pelo-menos-32-caracteres"
```

Nunca envie senhas reais ou o segredo JWT para o Git.

## Executar

```powershell
.\mvnw.cmd spring-boot:run
```

O Flyway cria e atualiza o schema automaticamente. A aplicação usa `jdbc:postgresql://localhost:5432/sportflow` e o usuário PostgreSQL `postgres`.

## Autenticação

Cadastro público:

```http
POST /api/auth/register
Content-Type: application/json

{
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "senha": "SenhaSegura123",
  "telefone": "48999990000"
}
```

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "maria@example.com",
  "senha": "SenhaSegura123"
}
```

Envie o token retornado nas rotas protegidas:

```http
Authorization: Bearer SEU_TOKEN
```

Todo cadastro público recebe o perfil `CLIENTE`. Operações de manutenção de esportes, quadras e horários exigem `FUNCIONARIO` ou `ADMIN`; exclusão de usuários exige `ADMIN`.

## Endpoints implementados

| Recurso | Base | Operações |
|---|---|---|
| Autenticação | `/api/auth` | registro e login |
| Usuários | `/api/usuarios` | CRUD |
| Estabelecimentos | `/api/estabelecimentos` | CRUD |
| Quadras | `/api/quadras` | CRUD |
| Esportes | `/api/esportes` | CRUD |
| Horários | `/api/horarios` | CRUD e bloqueio de sobreposição |
| Avaliações | `/api/avaliacoes` | CRUD preparado para agendamentos |

As consultas de estabelecimentos, quadras, esportes, horários e avaliações são públicas. As operações de escrita exigem autenticação e, quando aplicável, o perfil correto.

## Arquitetura

O fluxo principal é:

```text
Controller -> DTO validado -> Service -> Repository -> Entity -> PostgreSQL
```

- `Controller`: protocolo HTTP e códigos de resposta.
- `dto`: contratos de entrada e saída; senhas nunca aparecem nas respostas.
- `Service`: regras de negócio, normalização e transações.
- `Repository`: persistência com Spring Data JPA.
- `Entity`: mapeamento do schema relacional.
- `exception`: erros padronizados por `@RestControllerAdvice`.
- `security`: geração e validação do JWT.
- `config`: política stateless do Spring Security.

O DER atualizado está em `docs/der-sportflow.png`.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes cobrem:

- cadastro, hash BCrypt, login e autenticação JWT;
- bloqueio de rota sem token;
- autorização por perfil;
- validações e e-mail duplicado;
- cinco CRUDs com relacionamentos;
- conflito de horários sobrepostos;
- execução das migrations Flyway e validação do schema pelo Hibernate.

## Próxima etapa

Adicionar Swagger/OpenAPI com autenticação Bearer sem alterar os contratos atuais da API.
