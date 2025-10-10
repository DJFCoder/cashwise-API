# 💰 CashWise API

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8+-blue?style=flat&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.8+-red?style=flat&logo=apache-maven)
![Status](https://img.shields.io/badge/Status-MVP-success?style=flat)

**Mais controle, menos desperdício.**

API REST para controle financeiro pessoal, permitindo gerenciar categorias, registrar receitas e despesas com recorrência automática, e visualizar relatórios financeiros detalhados.

---

## 📋 Sobre o Projeto

**CashWise** é uma plataforma de gestão financeira pessoal que oferece:
- Cadastro de categorias personalizadas
- Registro de lançamentos (receitas e despesas)
- Sistema de recorrência automática
- Relatórios visuais com análises financeiras
- Filtros avançados por período, tipo e categoria

---

## 🚀 Funcionalidades

- ✅ **CRUD de Categorias** - Gerenciamento completo com validação de vínculo
- ✅ **CRUD de Lançamentos** - Receitas e despesas com recorrência
- ✅ **Geração Automática** - Lançamentos recorrentes criados automaticamente
- ✅ **Filtros Avançados** - Busca por período, tipo e categoria com paginação
- ✅ **Relatórios Financeiros** - Saldo, evolução mensal e distribuição por categoria
- ✅ **Validações Robustas** - Regras de negócio aplicadas via Bean Validation

---

## 🛠️ Tecnologias

- **Java 17+**
- **Spring Boot 3.x**
  - Spring Data JPA
  - Spring Web
  - Bean Validation
- **MySQL 8+**
- **Maven**
- **Hibernate** (geração automática de schema)

---

## 📦 Pré-requisitos

- JDK 17 ou superior
- MySQL 8+
- Maven 3.8+

---

## ⚙️ Configuração e Execução

### 1. Clone o repositório
```bash
git clone https://github.com/DJFCoder/cashwise-API.git
cd cashwise
```

### 2. Configure o banco de dados

Execute o script SQL:
```bash
mysql -u root -p < src/main/resources/db/V1__cashwise_db.sql
```

Ou crie manualmente:
```sql
CREATE DATABASE IF NOT EXISTS cashwise_db;
```

### 3. Configure as credenciais

Edite `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cashwise_db
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update
```

### 4. Execute a aplicação
```bash
./mvnw spring-boot:run
```

A API estará disponível em: `http://localhost:8080`

---

## 📡 Endpoints da API

### Categorias

#### Cadastrar categoria
```http
POST /api/categorias
Content-Type: application/json

{
  "nome": "Alimentação"
}
```

#### Listar categorias
```http
GET /api/categorias
```

#### Excluir categoria
```http
DELETE /api/categorias/{id}
```
⚠️ Apenas categorias sem lançamentos vinculados podem ser excluídas.

---

### Lançamentos

#### Cadastrar lançamento
```http
POST /api/lancamentos
Content-Type: application/json

{
  "tipo": "DESPESA",
  "categoriaId": 1,
  "valor": 150.00,
  "data": "2025-10-10",
  "descricao": "Compras do mês",
  "recorrencia": "MENSAL"
}
```

**Tipos válidos:** `RECEITA`, `DESPESA`

**Recorrências válidas:** `UNICA`, `DIARIA`, `SEMANAL`, `MENSAL`, `TRIMESTRAL`, `ANUAL`

💡 Recorrências diferentes de `UNICA` geram automaticamente lançamentos futuros.

#### Listar lançamentos com filtros
```http
GET /api/lancamentos?dataInicio=2025-01-01&dataFim=2025-12-31&tipo=DESPESA&categoriaId=1&page=0&size=20
```

#### Excluir lançamento
```http
DELETE /api/lancamentos/{id}
```

---

### Relatórios

#### Saldo no período
```http
GET /api/relatorios/saldo?dataInicio=2025-01-01&dataFim=2025-12-31
```

#### Evolução mensal
```http
GET /api/relatorios/evolucao?ano=2025
```

#### Distribuição por categoria
```http
GET /api/relatorios/distribuicao?dataInicio=2025-01-01&dataFim=2025-12-31
```

---

## 📂 Estrutura do Projeto

```
br.com.devjf.cashwise/
├── CashwiseApplication.java
├── domain/
│   ├── dto/
│   │   ├── category/
│   │   │   ├── CategoryRequest.java
│   │   │   └── CategoryResponse.java
│   │   └── transaction/
│   │       ├── TransactionRequest.java
│   │       ├── TransactionRequestFilter.java
│   │       └── TransactionResponse.java
│   ├── entity/
│   │   ├── Category.java
│   │   ├── Transaction.java
│   │   ├── TransactionType.java
│   │   └── RecurrencyType.java
│   └── mapper/
│       ├── CategoryMapper.java
│       └── TransactionMapper.java
├── exception/
│   └── BusinessException.java
├── repository/
│   ├── CategoryRepository.java
│   └── TransactionRepository.java
└── service/
    ├── CategoryService.java
    ├── TransactionService.java
    └── ReportService.java
```

---

## 🎯 Regras de Negócio

- ✔️ Todo lançamento deve estar vinculado a uma categoria existente
- ✔️ Valores devem ser maiores que zero
- ✔️ Recorrência é obrigatória e controlada pelo sistema
- ✔️ Lançamentos recorrentes geram automaticamente entradas futuras
- ✔️ Categorias só podem ser excluídas se não houver lançamentos vinculados
- ✔️ Lançamentos não podem ser editados (apenas excluídos)
- ✔️ Todos os valores são em Real (BRL)
- ✔️ Validações ocorrem via `@Valid` nos DTOs

---

## 🗄️ Modelo de Dados

### Categoria
- `id` (BIGINT, PK)
- `nome` (VARCHAR 120, NOT NULL)
- `criadoEm` (DATETIME, NOT NULL)
- `atualizadoEm` (DATETIME, NOT NULL)

### Lançamento
- `id` (BIGINT, PK)
- `categoriaId` (BIGINT, FK, NOT NULL)
- `tipo` (ENUM: RECEITA, DESPESA)
- `recorrencia` (ENUM: UNICA, DIARIA, SEMANAL, MENSAL, TRIMESTRAL, ANUAL)
- `data` (DATE, NOT NULL)
- `valor` (DECIMAL 15,2, NOT NULL)
- `descricao` (VARCHAR 255, NOT NULL)
- `criadoEm` (DATETIME, NOT NULL)
- `atualizadoEm` (DATETIME, NOT NULL)

---

## 🧪 Testes

Execute os testes:
```bash
./mvnw test
```

---

## 📝 Licença

Este projeto está sob a licença MIT.

---

## 👨‍💻 Autor

Desenvolvido com ☕ por [DevJF](https://github.com/DJFCoder)

---

## 📞 Suporte

Para reportar bugs ou sugerir melhorias, abra uma [issue](https://github.com/DJFCoder/cashwise-API/issues).