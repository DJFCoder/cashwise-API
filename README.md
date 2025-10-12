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

### /api/categoria


| Método     | URI       | Descrição                   | Corpo da Requisição         | Resposta de Sucesso               | Erros Possíveis                                                                     |
| ---------- | --------- | --------------------------- | --------------------------- | --------------------------------- | ----------------------------------------------------------------------------------- |
| **POST**   | `/`       | Cadastrar nova categoria    | `{ "name": "Alimentação" }` | `201 Created` – CategoryResponse  | `400` – nome inválido/duplicado                                                     |
| **GET**    | `/listar` | Listar todas categorias     | —                           | `200 OK` – List<CategoryResponse> | —                                                                                   |
| **GET**    | `/{id}`   | Buscar categoria por ID     | —                           | `200 OK` – CategoryResponse       | `404` – categoria não encontrada                                                    |
| **PUT**    | `/{id}`   | Atualizar nome da categoria | `{ "name": "Viagem" }`      | `200 OK` – CategoryResponse       | `400` – nome inválido/duplicado<br>`404` – categoria não encontrada                 |
| **DELETE** | `/{id}`   | Excluir categoria\*         | —                           | `204 No Content`                  | `404` – categoria não encontrada<br>`409` – categoria possui lançamentos vinculados |



⚠️ Apenas categorias sem lançamentos vinculados podem ser excluídas.

#### Exemplo de corpo de resposta (CategoryResponse):

```json
{
  "id": 3,
  "name": "Alimentação"
}
```

---

### /api/lancamento

| Método     | URI       | Descrição                              | Corpo da Requisição       | Resposta de Sucesso                  | Erros Possíveis                                             |
| ---------- | --------- | -------------------------------------- | ------------------------- | ------------------------------------ | ----------------------------------------------------------- |
| **POST**   | `/`       | Cadastrar novo lançamento              | TransactionRequest (JSON) | `201 Created` – TransactionResponse  | `400` – dados inválidos<br>`404` – categoria não encontrada |
| **GET**    | `/listar` | Listar com **filtros** e **paginação** | — (query params)          | `200 OK` – Page<TransactionResponse> | `400` – parâmetros inválidos                                |
| **GET**    | `/{id}`   | Buscar lançamento por ID               | —                         | `200 OK` – TransactionResponse       | `404` – lançamento não encontrado                           |
| **DELETE** | `/{id}`   | Excluir lançamento\*                   | —                         | `204 No Content`                     | `404` – lançamento não encontrado                           |

⚠️ Apenas exclusão física – não há edição no MVP.

**Tipos válidos:** `RECEITA`, `DESPESA`

**Recorrências válidas:** `UNICA`, `DIARIA`, `SEMANAL`, `MENSAL`, `TRIMESTRAL`, `ANUAL`

💡 Recorrências diferentes de `UNICA` geram automaticamente lançamentos futuros.

#### Exemplo de corpo de requisição (TransactionRequest):

```json
{
  "type": "Receita",
  "categoryId": 1,
  "amount": 2500.00,
  "description": "Salário mensal",
  "recurrency": "MONTHLY"
}
```

#### Exemplo de corpo de resposta (TransactionResponse):

```json
{
  "id": 10,
  "type": "Receita",
  "category": { "id": 1, "name": "Salário" },
  "amount": 2500.00,
  "date": "2025-06-01",
  "description": "Salário mensal",
  "recurrency": "MONTHLY"
}
```

#### Parâmetros de consulta (GET /listar):

| Parâmetro       | Tipo      | Obrigatório | Descrição                                 | Exemplo      |
| --------------- | --------- | ----------- | ----------------------------------------- | ------------ |
| `startDate`     | LocalDate | Não         | Data inicial do período                   | `2025-01-01` |
| `endDate`       | LocalDate | Não         | Data final do período                     | `2025-06-30` |
| `type`          | String    | Não         | Filtrar por tipo (`Receita` ou `Despesa`) | `Despesa`    |
| `categoryId`    | Long      | Não         | Filtrar por categoria                     | `2`          |
| `page`          | int       | Não         | Número da página (0-based)                | `0`          |
| `size`          | int       | Não         | Tamanho da página (padrão 20)             | `10`         |
| `sortBy`        | String    | Não         | Campo de ordenação (padrão: createdAt)    | `amount`     |
| `sortDirection` | String    | Não         | `asc` ou `desc` (padrão: desc)            | `asc`        |


#### Exemplo de URL completa:
```http
GET /api/lancamento/listar?startDate=2025-01-01&endDate=2025-06-30&type=Despesa&page=0&size=10&sortBy=amount&sortDirection=desc
```

---

### /api/recorrencia

| Método   | URI                | Descrição                          | Corpo da Requisição | Resposta de Sucesso                    | Erros Possíveis                                                 |
| -------- | ------------------ | ---------------------------------- | ------------------- | -------------------------------------- | --------------------------------------------------------------- |
| **POST** | `/{id}/desativar`  | Desativa geração de filhos         | —                   | `200 OK` – RecurrencyOperationResponse | `404` – lançamento não encontrado<br>`400` – lançamento é filho |
| **POST** | `/{id}/ativar`     | Reativa geração de filhos          | —                   | `200 OK` – RecurrencyOperationResponse | `404` – não encontrado<br>`400` – filho ou `UNIQUE`             |
| **POST** | `/{id}/data-final` | Define/remove data limite          | EndDateRequest      | `200 OK` – RecurrencyOperationResponse | `400` – data inválida ou filho<br>`404` – não encontrado        |
| **GET**  | `/{id}/children`   | Lista lançamentos filhos gerados   | —                   | `200 OK` – List<TransactionResponse>   | `404` – original não encontrado                                 |
| **GET**  | `/{id}/count`      | Conta quantidade de filhos gerados | —                   | `200 OK` – ChildCountResponse          | `404` – original não encontrado                                 |

#### Exemplo de corpo de requisição (EndDateRequest):

```json
{ "endDate": "2025-12-31" }
```

Envie null para remover a data de término (recorrência infinita).

#### Exemplo de corpo de resposta (RecurrencyOperationResponse):

```json
{
  "transactionId": 5,
  "operation": "DEACTIVATED",
  "message": "Recorrência desativada com sucesso. Novos lançamentos não serão mais gerados automaticamente."
}
```

#### Exemplo de corpo de resposta (ChildCountResponse):

```json
{
  "parentTransactionId": 5,
  "childCount": 3,
  "message": "Lançamentos filhos encontrados"
}
```

---

### /api/relatorio

| Método  | URI                | Descrição                   | Parâmetros                            | Resposta de Sucesso                 | Erros Possíveis          |
| ------- | ------------------ | --------------------------- | ------------------------------------- | ----------------------------------- | ------------------------ |
| **GET** | `/balancete`       | Saldo (receitas - despesas) | `startDate`, `endDate` (obrigatórios) | `200 OK` – BalanceResponse          | `400` – período inválido |
| **GET** | `/distribuicao`    | Gastos por categoria        | `startDate`, `endDate` (obrigatórios) | `200 OK` – DistributionResponse     | `400` – período inválido |
| **GET** | `/evolucao-mensal` | Evolução mensal             | `year` (obrigatório)                  | `200 OK` – MonthlyEvolutionResponse | `400` – ano inválido     |

#### Exemplo de resposta (BalanceResponse):

```json
{
  "startDate": "2025-01-01",
  "endDate": "2025-06-30",
  "revenues": 15000.00,
  "expenses": 8000.00,
  "balance": 7000.00,
  "status": "SUPERAVIT"
}
```

#### Exemplo de resposta (DistributionResponse):

```json
{
  "startDate": "2025-01-01",
  "endDate": "2025-06-30",
  "distribution": {
    "Alimentação": 2500.00,
    "Transporte": 1200.00,
    "Lazer": 800.00
  },
  "totalCategories": 3
}
```

#### Exemplo de resposta (MonthlyEvolutionResponse):

```json
{
  "year": 2025,
  "monthlyData": [
    {
      "month": 1,
      "year": 2025,
      "revenues": 5000.00,
      "expenses": 3000.00
    },
    {
      "month": 2,
      "year": 2025,
      "revenues": 5500.00,
      "expenses": 3200.00
    }
  ],
  "totalMonths": 2
}
```

---

## 📂 Estrutura do Projeto

```
br.com.devjf.cashwise/
├── controller/     ← REST endpoints
├── domain/         ← entidades, enums, DTOs, mappers
├── service/        ← regras de negócio
├── repository/     ← acesso a dados
└── job/            ← RecurrencyJob (01:00 diário)
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
- ✔️ Job gera no máximo 2 filhos/dia por lançamento original
- ✔️ Relatórios somente-leitura, sem exportação no MVP

---

## 🗄️ Modelo de Dados

### Tabela `category`
| Campo         | Tipo              | Restrições              | Descrição                          |
|---------------|-------------------|-------------------------|------------------------------------|
| id            | BIGINT            | PK, AUTO_INCREMENT      | Identificador único               |
| name          | VARCHAR(120)      | NOT NULL, UNIQUE        | Nome da categoria                 |
| created_at    | DATETIME          | NOT NULL                | Data/hora de criação              |
| updated_at    | DATETIME          | NOT NULL                | Data/hora da última alteração     |


### Tabela `transaction`
| Campo                  | Tipo              | Restrições                         | Descrição                                                     |
|------------------------|-------------------|------------------------------------|---------------------------------------------------------------|
| id                     | BIGINT            | PK, AUTO_INCREMENT                 | Identificador único                                          |
| category_id            | BIGINT            | FK → category(id), NOT NULL        | Categoria vinculada                                          |
| type                   | ENUM              | NOT NULL                           | RECEITA ou DESPESA                                           |
| amount                 | DECIMAL(15,2)     | NOT NULL, > 0                      | Valor monetário (até 9999999999999.99)                       |
| description            | VARCHAR(255)      | NOT NULL                           | Descrição livre                                              |
| recurrency             | ENUM              | NOT NULL                           | UNIQUE, DAILY, WEEKLY, MONTHLY, QUARTERLY, ANUAL             |
| recurrency_active      | BOOLEAN           | DEFAULT TRUE                       | Indica se a recorrência está ativa (apenas para originais)    |
| recurrency_end_date    | DATE              | NULL                               | Data limite da recorrência (NULL = infinita)                 |
| parent_transaction_id  | BIGINT            | FK → transaction(id), NULL         | Referência para o lançamento original (filhos)               |
| created_at             | DATETIME          | NOT NULL                           | Data/hora de criação                                         |
| updated_at             | DATETIME          | NOT NULL                           | Data/hora da última alteração                                |


**Índices recomendados:**
```sql
CREATE INDEX idx_transaction_created_at ON transaction(created_at);
CREATE INDEX idx_transaction_category_id ON transaction(category_id);
CREATE INDEX idx_transaction_type ON transaction(type);
CREATE INDEX idx_transaction_parent_id ON transaction(parent_transaction_id);
```

---

## 🧪 Testes

Execute os testes:
```bash
./mvnw test
```

---

## 👨‍💻 Autor

Desenvolvido com ☕ por [DevJF](https://github.com/DJFCoder)

---

## 📞 Suporte

Para reportar bugs ou sugerir melhorias, abra uma [issue](https://github.com/DJFCoder/cashwise-API/issues).