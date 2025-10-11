# Plano de Testes - CashWise

## Informações do Sistema

| **Nome do Sistema** |
|:---:|
| **CashWise** - Mais controle, menos desperdício |

| **Resumo do Sistema** |
|:---:|
| A **Plataforma de Controle Financeiro Pessoal – CashWise** é um sistema web com objetivo de registrar e analisar receitas e despesas, com categorização e relatórios visuais para apoiar decisões financeiras pessoais. |

| **Objetivos do Plano de Teste** |
|:---|
| Garantir que as regras de negócio do CashWise estejam sendo aplicadas corretamente, com foco na validação dos campos obrigatórios, integridade referencial, geração automática de lançamentos recorrentes e fluxo correto das operações de CRUD. |

---

## Casos de Teste

### CT001 - Cadastro de Categoria com Sucesso

**Pré-condições**: Sistema iniciado e conectado ao banco de dados

**Valores de entrada**: 
- Nome: "Alimentação"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição POST com CategoriaRequest válido | Status 201 Created |
| 2 | Verificar resposta contém id gerado e nome informado | CategoriaResponse com id e nome "Alimentação" |
| 3 | Consultar categoria no banco de dados | Categoria persistida com metadados criado_em e atualizado_em |

**Resultados esperados**: Categoria cadastrada com sucesso e metadados de auditoria registrados

**Pós-condições**: Categoria "Alimentação" existe no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT002 - Cadastro de Categoria com Nome Inválido

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Nome: "" (vazio)

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição POST com nome vazio | Status 400 Bad Request |
| 2 | Verificar mensagem de validação | Erro indicando campo obrigatório |
| 3 | Confirmar que nenhum registro foi criado | Banco sem alterações |

**Resultados esperados**: Validação @NotBlank impede cadastro

**Pós-condições**: Nenhuma categoria criada

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT003 - Exclusão de Categoria sem Vínculo

**Pré-condições**: Categoria cadastrada sem lançamentos vinculados

**Valores de entrada**: 
- ID da categoria: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição DELETE para categoria id=1 | Status 204 No Content |
| 2 | Verificar inexistência no banco | Categoria removida |
| 3 | Tentar consultar categoria excluída | Status 404 Not Found |

**Resultados esperados**: Categoria excluída com sucesso

**Pós-condições**: Categoria não existe mais no sistema

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT004 - Bloqueio de Exclusão de Categoria com Lançamentos

**Pré-condições**: Categoria com lançamento vinculado

**Valores de entrada**: 
- ID da categoria: 1 (com lançamentos)

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar DELETE na categoria com vínculos | Status 400 Bad Request |
| 2 | Verificar mensagem de erro | Erro indicando impossibilidade por vínculo |
| 3 | Confirmar categoria ainda existe | Categoria permanece no banco |

**Resultados esperados**: Exclusão bloqueada por integridade referencial

**Pós-condições**: Categoria e lançamentos permanecem intactos

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT005 - Cadastro de Lançamento Único com Sucesso

**Pré-condições**: Categoria cadastrada (id=1)

**Valores de entrada**:
- tipo: "RECEITA"
- categoriaId: 1
- valor: 1500.00
- data: "2025-10-11"
- descricao: "Salário"
- recorrencia: "UNICA"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar POST com LancamentoRequest | Status 201 Created |
| 2 | Validar resposta | LancamentoResponse com todos os dados |
| 3 | Verificar no banco | Lançamento único criado sem recorrências futuras |

**Resultados esperados**: Lançamento único registrado corretamente

**Pós-condições**: Um lançamento no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT006 - Geração Automática de Lançamentos Recorrentes Mensais

**Pré-condições**: Categoria cadastrada (id=1)

**Valores de entrada**:
- tipo: "DESPESA"
- categoriaId: 1
- valor: 500.00
- data: "2025-10-11"
- descricao: "Aluguel"
- recorrencia: "MENSAL"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Cadastrar lançamento com recorrência MENSAL | Status 201 Created |
| 2 | Executar job de recorrência | Lançamentos futuros gerados (11/nov, 11/dez, etc) |
| 3 | Validar datas e valores | Mesmos dados, datas incrementadas mensalmente |

**Resultados esperados**: Lançamentos recorrentes gerados automaticamente

**Pós-condições**: Múltiplos lançamentos mensais no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT007 - Validação de Valor Negativo em Lançamento

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- valor: -100.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar cadastrar com valor negativo | Status 400 Bad Request |
| 2 | Verificar mensagem de validação | Erro @Positive indicando valor inválido |
| 3 | Confirmar não criação | Banco sem alterações |

**Resultados esperados**: Validação impede valor negativo/zero

**Pós-condições**: Nenhum lançamento criado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT008 - Filtro de Lançamentos por Período

**Pré-condições**: Lançamentos cadastrados em datas diversas

**Valores de entrada**:
- dataInicio: "2025-10-01"
- dataFim: "2025-10-31"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar GET com filtro de período | Status 200 OK |
| 2 | Validar resultados retornados | Apenas lançamentos dentro do período |
| 3 | Verificar paginação | Resposta paginada conforme configuração |

**Resultados esperados**: Lista filtrada corretamente

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT009 - Filtro por Tipo e Categoria

**Pré-condições**: Lançamentos de tipos e categorias variados

**Valores de entrada**:
- tipo: "DESPESA"
- categoriaId: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Aplicar filtro combinado | Status 200 OK |
| 2 | Validar resultados | Apenas despesas da categoria 1 |
| 3 | Verificar ordenação | Dados ordenados por data |

**Resultados esperados**: Filtros múltiplos funcionando

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT010 - Cálculo de Saldo no Período

**Pré-condições**: Lançamentos cadastrados (receitas e despesas)

**Valores de entrada**:
- inicio: "2025-10-01"
- fim: "2025-10-31"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar relatório de saldo | Status 200 OK |
| 2 | Validar cálculo | Saldo = Σ receitas - Σ despesas |
| 3 | Verificar precisão decimal | Valores corretos em BRL com 2 casas |

**Resultados esperados**: Saldo calculado corretamente

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT011 - Distribuição por Categoria

**Pré-condições**: Lançamentos em categorias diversas

**Valores de entrada**:
- inicio: "2025-01-01"
- fim: "2025-12-31"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar distribuição por categoria | Status 200 OK |
| 2 | Validar agrupamento | Soma por categoria correta |
| 3 | Verificar formato de resposta | Map com categoria e valor total |

**Resultados esperados**: Distribuição agrupada corretamente

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT012 - Validação de Campos Obrigatórios em Lançamento

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- LancamentoRequest sem descrição

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar request incompleto | Status 400 Bad Request |
| 2 | Verificar validação @Valid | Erro nos campos obrigatórios faltantes |
| 3 | Confirmar não persistência | Nenhum dado salvo |

**Resultados esperados**: Validação bloqueia cadastro incompleto

**Pós-condições**: Banco inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

## Estratégias de Teste

### 🔬 Teste de Unidade

| **Aspecto** | **Descrição** |
|:---|:---|
| **Objetivo** | Assegurar correção e robustez da lógica de negócio na camada de serviço, verificando validações, processamento de dados e interações com repositórios |
| **Técnica** | • Teste de Caixa-Branca<br>• Mocking com Mockito para simular dependências<br>• Asserções JUnit para validar resultados<br>• Verificação de interações (verify, never) |
| **Critério de Finalização** | • 100% dos casos de teste executam com sucesso<br>• Cobertura mínima de 80% nas classes de serviço |
| **Considerações Especiais** | • Setup (@Before) para inicializar mocks<br>• Validação de estado e interações<br>• Ambiente isolado por teste |

---

## Matriz de Rastreabilidade

| Requisito | Casos de Teste |
|:---|:---|
| RF01 - CRUD Categorias | CT001, CT002, CT003, CT004 |
| RF02 - CRUD Lançamentos | CT005, CT007, CT012 |
| RF03 - Recorrência Automática | CT006 |
| RF04 - Listagem com Filtros | CT008, CT009 |
| RF05 - Relatórios Visuais | CT010, CT011 |
| RF07 - Validação @Valid | CT002, CT007, CT012 |

---

## Resumo Executivo

| Métrica | Valor |
|:---|:---:|
| **Total de Casos de Teste** | 12 |
| **Cobertura de Requisitos Funcionais** | 100% |
| **Requisitos Funcionais Testados** | 6 de 6 |
| **Técnicas Aplicadas** | Caixa-Branca, Mocking |
| **Frameworks** | JUnit, Mockito |

---

**Elaborado em**: 11/10/2025
**Versão do Documento**: 1.0
**Status**: Em Revisão
