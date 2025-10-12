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

## MÓDULO 1: CATEGORIAS

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

### CT002 - Validação de Nome Antes de Cadastrar

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Nome: "Transporte"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Verificar se nome já existe no repositório | Chamada ao método existsByName |
| 2 | Enviar requisição POST | Status 201 Created |
| 3 | Confirmar validação prévia | Repositório consultado antes do save |

**Resultados esperados**: Nome validado antes da persistência

**Pós-condições**: Categoria cadastrada após validação

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT003 - Cadastro com Categoria Nula

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Categoria: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição com categoria nula | Status 400 Bad Request |
| 2 | Verificar mensagem de erro | "Categoria não pode ser nula" |
| 3 | Confirmar não persistência | Nenhuma interação com repositório |

**Resultados esperados**: IllegalArgumentException lançada

**Pós-condições**: Banco inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT004 - Cadastro com Nome Vazio

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Nome: ""

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição POST com nome vazio | Status 400 Bad Request |
| 2 | Verificar mensagem de validação | "Nome da categoria é obrigatório" |
| 3 | Confirmar não persistência | Banco sem alterações |

**Resultados esperados**: Validação impede cadastro

**Pós-condições**: Nenhuma categoria criada

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT005 - Cadastro com Nome Apenas Espaços

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Nome: "   "

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição com nome em branco | Status 400 Bad Request |
| 2 | Verificar mensagem | "Nome da categoria é obrigatório" |
| 3 | Confirmar rejeição | Repositório não invocado |

**Resultados esperados**: Validação de string em branco funciona

**Pós-condições**: Banco inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT006 - Cadastro com Nome Nulo

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- Nome: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição com nome nulo | Status 400 Bad Request |
| 2 | Verificar mensagem de validação | "Nome da categoria é obrigatório" |
| 3 | Confirmar não criação | Nenhum registro no banco |

**Resultados esperados**: Campo obrigatório validado

**Pós-condições**: Sistema sem alterações

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT007 - Cadastro com Nome Duplicado

**Pré-condições**: Categoria "Alimentação" já cadastrada

**Valores de entrada**: 
- Nome: "Alimentação"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar cadastrar categoria duplicada | Status 400 Bad Request |
| 2 | Verificar BusinessException | "Já existe uma categoria com o nome" |
| 3 | Confirmar unicidade | Verificação existsByName realizada |

**Resultados esperados**: Bloqueio de duplicação

**Pós-condições**: Apenas uma categoria "Alimentação" no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT008 - Exclusão de Categoria sem Vínculo

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

### CT009 - Exclusão de Categoria Inexistente

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- ID: 999

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar excluir categoria inexistente | Status 404 Not Found |
| 2 | Verificar EntityNotFoundException | "não encontrada" |
| 3 | Confirmar não execução | deleteById não invocado |

**Resultados esperados**: Exceção lançada para ID inexistente

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT010 - Bloqueio de Exclusão com Lançamentos

**Pré-condições**: Categoria com lançamento vinculado

**Valores de entrada**: 
- ID da categoria: 1 (com lançamentos)

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar DELETE na categoria com vínculos | Status 400 Bad Request |
| 2 | Verificar mensagem de erro | "existem lançamentos vinculados" |
| 3 | Confirmar categoria ainda existe | Categoria permanece no banco |

**Resultados esperados**: Exclusão bloqueada por integridade referencial

**Pós-condições**: Categoria e lançamentos permanecem intactos

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT011 - Validação de Integridade Referencial

**Pré-condições**: Categoria com vínculos

**Valores de entrada**: 
- ID: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Verificar existência da categoria | existsById retorna true |
| 2 | Verificar lançamentos vinculados | existsByCategoryId retorna true |
| 3 | Lançar BusinessException | Exclusão bloqueada |

**Resultados esperados**: Ordem de validações respeitada

**Pós-condições**: Integridade preservada

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT012 - Listagem de Todas as Categorias

**Pré-condições**: Categorias cadastradas no sistema

**Valores de entrada**: 
- Nenhum filtro

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição GET /categorias | Status 200 OK |
| 2 | Verificar lista retornada | Todas as categorias presentes |
| 3 | Validar estrutura de resposta | Array de CategoriaResponse |

**Resultados esperados**: Lista completa retornada

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT013 - Listagem Vazia de Categorias

**Pré-condições**: Banco sem categorias

**Valores de entrada**: 
- Nenhum filtro

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Requisitar lista de categorias | Status 200 OK |
| 2 | Verificar array vazio | Lista sem elementos |
| 3 | Confirmar resposta válida | Estrutura JSON correta |

**Resultados esperados**: Array vazio retornado

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT014 - Busca de Categoria por ID

**Pré-condições**: Categoria id=1 cadastrada

**Valores de entrada**: 
- ID: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar GET /categorias/1 | Status 200 OK |
| 2 | Validar dados retornados | Categoria com id=1 |
| 3 | Verificar completude | Todos os campos preenchidos |

**Resultados esperados**: Categoria específica retornada

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT015 - Busca de Categoria Inexistente

**Pré-condições**: Sistema iniciado

**Valores de entrada**: 
- ID: 999

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar buscar categoria inexistente | Status 404 Not Found |
| 2 | Verificar EntityNotFoundException | "não encontrada" |
| 3 | Confirmar consulta ao repositório | findById invocado |

**Resultados esperados**: Exceção lançada corretamente

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

## MÓDULO 2: LANÇAMENTOS

### CT016 - Cadastro de Lançamento Único (RECEITA)

**Pré-condições**: Categoria cadastrada (id=1)

**Valores de entrada**:
- tipo: "RECEITA"
- categoriaId: 1
- valor: 1500.00
- descrição: "Salário"
- recorrência: "UNIQUE"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar POST com LançamentoRequest | Status 201 Created |
| 2 | Validar resposta | LançamentoResponse com todos os dados |
| 3 | Verificar no banco | Lançamento único criado |

**Resultados esperados**: Receita registrada corretamente

**Pós-condições**: Um lançamento no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT017 - Cadastro de Lançamento Único (DESPESA)

**Pré-condições**: Categoria cadastrada

**Valores de entrada**:
- tipo: "DESPESA"
- valor: 500.00
- descrição: "Aluguel"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar POST de despesa | Status 201 Created |
| 2 | Verificar tipo | TransactionType.EXPENSE |
| 3 | Confirmar persistência | Registro salvo no banco |

**Resultados esperados**: Despesa cadastrada com sucesso

**Pós-condições**: Lançamento despesa no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT018 - Definição Automática de Data de Criação

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- createdAt: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Cadastrar sem informar createdAt | Data atual atribuída |
| 2 | Verificar campo preenchido | createdAt não nulo |
| 3 | Validar timestamp | Data/hora atual |

**Resultados esperados**: Sistema preenche automaticamente

**Pós-condições**: Metadado de auditoria presente

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT019 - Validação de Valor Negativo

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- valor: -100.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar cadastrar com valor negativo | Status 400 Bad Request |
| 2 | Verificar mensagem de validação | "deve ser positivo" |
| 3 | Confirmar não criação | Banco sem alterações |

**Resultados esperados**: Validação impede valor negativo

**Pós-condições**: Nenhum lançamento criado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT020 - Validação de Valor Zero

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- valor: 0.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar cadastrar com valor zero | Status 400 Bad Request |
| 2 | Verificar IllegalArgumentException | "deve ser positivo" |
| 3 | Confirmar rejeição | Repositório não invocado |

**Resultados esperados**: Zero não permitido

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT021 - Validação de Valor Nulo

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- valor: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar requisição com valor nulo | Status 400 Bad Request |
| 2 | Verificar mensagem | "deve ser positivo" |
| 3 | Confirmar não persistência | Nenhum save executado |

**Resultados esperados**: Campo obrigatório validado

**Pós-condições**: Banco inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT022 - Validação de Lançamento Nulo

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- transaction: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar registerTransaction(null) | IllegalArgumentException |
| 2 | Verificar mensagem | "Lançamento não pode ser nulo" |
| 3 | Confirmar não execução | Nenhuma operação realizada |

**Resultados esperados**: Parâmetro nulo rejeitado

**Pós-condições**: Sistema sem alterações

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT023 - Validação de Tipo Nulo

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- tipo: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Cadastrar sem informar tipo | Status 400 Bad Request |
| 2 | Verificar erro | "Tipo de lançamento é obrigatório" |
| 3 | Confirmar rejeição | Save não invocado |

**Resultados esperados**: Tipo obrigatório validado

**Pós-condições**: Nenhum registro criado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT024 - Validação de Categoria Nula

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- categoria: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar lançamento sem categoria | Status 400 Bad Request |
| 2 | Verificar mensagem | "Categoria é obrigatória" |
| 3 | Confirmar bloqueio | Repositório não chamado |

**Resultados esperados**: Categoria obrigatória

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT025 - Validação de Descrição Nula

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- descrição: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Cadastrar sem descrição | Status 400 Bad Request |
| 2 | Verificar erro | "Descrição é obrigatória" |
| 3 | Confirmar não criação | Banco sem alterações |

**Resultados esperados**: Descrição obrigatória validada

**Pós-condições**: Nenhum lançamento criado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT026 - Validação de Descrição Vazia

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- descrição: ""

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar descrição vazia | Status 400 Bad Request |
| 2 | Verificar IllegalArgumentException | "Descrição é obrigatória" |
| 3 | Confirmar rejeição | Save não executado |

**Resultados esperados**: String vazia não permitida

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT027 - Validação de Descrição em Branco

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- descrição: "   "

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Cadastrar com espaços em branco | Status 400 Bad Request |
| 2 | Verificar mensagem | "Descrição é obrigatória" |
| 3 | Confirmar bloqueio | Repositório não invocado |

**Resultados esperados**: Validação de blank funciona

**Pós-condições**: Nenhum registro criado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT028 - Filtro por Período

**Pré-condições**: Lançamentos cadastrados em datas diversas

**Valores de entrada**:
- dataInicio: "2025-10-01"
- dataFim: "2025-10-31"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar GET com filtro de período | Status 200 OK |
| 2 | Validar resultados retornados | Apenas lançamentos dentro do período |
| 3 | Verificar paginação | Resposta paginada |

**Resultados esperados**: Lista filtrada corretamente

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT029 - Listagem sem Filtro de Período

**Pré-condições**: Lançamentos cadastrados

**Valores de entrada**:
- dataInicio: null
- dataFim: null

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Requisitar todos os lançamentos | Status 200 OK |
| 2 | Verificar método invocado | findAll(pageable) |
| 3 | Validar resposta completa | Todos os lançamentos retornados |

**Resultados esperados**: Lista completa quando sem filtro

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT030 - Página Vazia no Período

**Pré-condições**: Sem lançamentos no período especificado

**Valores de entrada**:
- dataInicio: "2025-11-01"
- dataFim: "2025-11-30"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Filtrar período sem dados | Status 200 OK |
| 2 | Verificar resposta | Page vazia |
| 3 | Validar totalElements | Valor 0 |

**Resultados esperados**: Página vazia retornada

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT031 - Filtro por Tipo (DESPESA)

**Pré-condições**: Lançamentos de tipos variados

**Valores de entrada**:
- tipo: "EXPENSE"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Aplicar filtro de tipo | Status 200 OK |
| 2 | Validar resultados | Apenas despesas |
| 3 | Verificar método | findByType invocado |

**Resultados esperados**: Filtro de tipo funciona

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT032 - Filtro por Categoria

**Pré-condições**: Lançamentos em categorias diversas

**Valores de entrada**:
- categoriaId: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Filtrar por categoria específica | Status 200 OK |
| 2 | Validar lançamentos retornados | Todos da categoria 1 |
| 3 | Verificar busca de categoria | findCategoryById invocado |

**Resultados esperados**: Filtro por categoria funcional

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT033 - Filtro Combinado (Tipo + Categoria)

**Pré-condições**: Lançamentos variados

**Valores de entrada**:
- tipo: "EXPENSE"
- categoriaId: 1
- período: 2025-10

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Aplicar múltiplos filtros | Status 200 OK |
| 2 | Validar intersecção | Apenas despesas da categoria 1 no período |
| 3 | Verificar método | findByCreatedAtBetweenAndTypeAndCategory |

**Resultados esperados**: Filtros múltiplos funcionam

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT034 - Filtro por Período e Tipo

**Pré-condições**: Lançamentos cadastrados

**Valores de entrada**:
- período: 2025-10
- tipo: "REVENUE"

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Combinar filtros período + tipo | Status 200 OK |
| 2 | Validar resultados | Receitas de outubro |
| 3 | Verificar método correto | findByCreatedAtBetweenAndType |

**Resultados esperados**: Combinação período/tipo funciona

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT035 - Exclusão de Lançamento

**Pré-condições**: Lançamento id=1 cadastrado

**Valores de entrada**:
- ID: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Enviar DELETE /lancamentos/1 | Status 204 No Content |
| 2 | Verificar exclusão | existsById retorna false |
| 3 | Confirmar remoção | deleteById invocado |

**Resultados esperados**: Lançamento excluído com sucesso

**Pós-condições**: Lançamento não existe mais

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT036 - Exclusão de Lançamento Inexistente

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- ID: 999

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar excluir ID inexistente | Status 404 Not Found |
| 2 | Verificar EntityNotFoundException | "não encontrada" |
| 3 | Confirmar não execução | deleteById não invocado |

**Resultados esperados**: Exceção para ID inexistente

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

## MÓDULO 3: RECORRÊNCIAS

### CT037 - Processar Recorrência Ativa e Gerar 1 Filho

**Pré-condições**: Lançamento original com recorrência ativa

**Valores de entrada**:
- recurrency: DAILY
- recurrencyActive: true

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Executar processamento diário | Job executa com sucesso |
| 2 | Verificar geração de filho | 1 filho criado |
| 3 | Validar data do filho | Data = hoje + 1 dia |

**Resultados esperados**: 1 filho gerado por execução

**Pós-condições**: Filho criado com parentTransactionId preenchido

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT038 - Usar Último Filho como Fonte

**Pré-condições**: Original com filho já gerado ontem

**Valores de entrada**:
- Último filho: ontem

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Buscar último filho | findLastChildTransaction |
| 2 | Calcular próxima data | Base = data do último filho |
| 3 | Gerar novo filho | Data = hoje |

**Resultados esperados**: Próximo filho usa último como base

**Pós-condições**: Sequência de datas correta

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT039 - Respeitar Limite de 2 Filhos por Dia

**Pré-condições**: 2 filhos já gerados hoje

**Valores de entrada**:
- Filhos hoje: 2

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Verificar contagem | countByParentAndCreatedAtAfter = 2 |
| 2 | Avaliar limite | Processamento interrompido |
| 3 | Confirmar não geração | Nenhum save executado |

**Resultados esperados**: Limite de 2/dia respeitado

**Pós-condições**: Máximo 2 filhos por dia mantido

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT040 - Calcular Próxima Data para Cada Tipo

**Pré-condições**: Originais de cada tipo cadastrados

**Valores de entrada**:
- Base: 2025-10-12

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | DAILY | Próxima = 2025-10-13 |
| 2 | WEEKLY | Próxima = 2025-10-19 |
| 3 | MONTHLY | Próxima = 2025-11-12 |
| 4 | QUARTERLY | Próxima = 2026-01-12 |
| 5 | ANNUAL | Próxima = 2026-10-12 |

**Resultados esperados**: Cálculo correto para cada tipo

**Pós-condições**: Filhos gerados com datas corretas

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT041 - Desativar Recorrência com Sucesso

**Pré-condições**: Lançamento com recorrência ativa

**Valores de entrada**:
- ID: 1
- recurrencyActive: true

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar deactivateRecurrency(1) | Execução sem exceção |
| 2 | Verificar flag | recurrencyActive = false |
| 3 | Confirmar persistência | save invocado |

**Resultados esperados**: Recorrência desativada

**Pós-condições**: Flag atualizada no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT042 - Falha ao Desativar Lançamento Inexistente

**Pré-condições**: Sistema iniciado

**Valores de entrada**:
- ID: 999

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar desativar ID inexistente | EntityNotFoundException |
| 2 | Verificar mensagem | Erro de não encontrado |
| 3 | Confirmar não persistência | save não invocado |

**Resultados esperados**: Exceção lançada corretamente

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT043 - Ativar Recorrência com Sucesso

**Pré-condições**: Lançamento com recorrência inativa

**Valores de entrada**:
- ID: 1
- recurrencyActive: false

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar activateRecurrency(1) | Execução sem exceção |
| 2 | Verificar flag | recurrencyActive = true |
| 3 | Confirmar save | Atualização persistida |

**Resultados esperados**: Recorrência ativada

**Pós-condições**: Flag true no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT044 - Falha ao Ativar Recorrência de Filho

**Pré-condições**: Lançamento filho cadastrado

**Valores de entrada**:
- parentTransactionId: não nulo

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar ativar filho | IllegalArgumentException |
| 2 | Verificar validação | Filho não pode ser ativado |
| 3 | Confirmar não execução | save não invocado |

**Resultados esperados**: Bloqueio de ativação de filho

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT045 - Definir Data de Término com Sucesso

**Pré-condições**: Lançamento recorrente cadastrado

**Valores de entrada**:
- ID: 1
- endDate: 2025-12-31

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar setRecurrencyEndDate | Execução sem exceção |
| 2 | Verificar campo | recurrencyEndDate = 2025-12-31 |
| 3 | Confirmar persistência | save executado |

**Resultados esperados**: Data de término definida

**Pós-condições**: Campo atualizado no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT046 - Falha ao Definir Data Anterior ao Original

**Pré-condições**: Lançamento criado em 2025-10-12

**Valores de entrada**:
- endDate: 2025-09-01

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Tentar definir data passada | IllegalArgumentException |
| 2 | Verificar validação | Data deve ser posterior |
| 3 | Confirmar não persistência | save não invocado |

**Resultados esperados**: Validação de data funciona

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT047 - Buscar Lançamentos Filhos

**Pré-condições**: Original com filhos gerados

**Valores de entrada**:
- parentId: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar findChildTransactions(1) | Lista retornada |
| 2 | Verificar conteúdo | Todos os filhos presentes |
| 3 | Validar parentTransactionId | Todos apontam para 1 |

**Resultados esperados**: Lista de filhos retornada

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT048 - Contar Lançamentos Filhos

**Pré-condições**: Original com 5 filhos

**Valores de entrada**:
- parentId: 1

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Invocar countChildTransactions(1) | Long retornado |
| 2 | Verificar valor | count = 5 |
| 3 | Validar consulta | countChildTransactions executado |

**Resultados esperados**: Contagem correta

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT049 - Desativação Mantém Último Filho Atual

**Pré-condições**: Original com filho gerado hoje

**Valores de entrada**:
- Filho criado: hoje

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Desativar recorrência | recurrencyActive = false |
| 2 | Verificar filho existente | Filho permanece |
| 3 | Validar data do filho | Data = hoje |

**Resultados esperados**: Filho não é removido

**Pós-condições**: Filhos existentes preservados

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT050 - UNIQUE Não Gera Filhos

**Pré-condições**: Lançamento com recorrência UNIQUE

**Valores de entrada**:
- recurrency: UNIQUE
- recurrencyActive: true

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Executar job de processamento | Job executa |
| 2 | Verificar geração | Nenhum filho criado |
| 3 | Confirmar não persistência | save não invocado |

**Resultados esperados**: UNIQUE não gera filhos

**Pós-condições**: Apenas lançamento original existe

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT051 - Respeitar Data de Término

**Pré-condições**: Original com endDate = ontem

**Valores de entrada**:
- recurrencyEndDate: 2025-10-11

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Executar processamento | Job verifica data |
| 2 | Avaliar término | Geração bloqueada |
| 3 | Confirmar não criação | Nenhum filho gerado |

**Resultados esperados**: Data de término respeitada

**Pós-condições**: Nenhum filho após término

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

## MÓDULO 4: RELATÓRIOS

### CT052 - Calcular Saldo com Receitas e Despesas

**Pré-condições**: Lançamentos cadastrados

**Valores de entrada**:
- Receitas: 5000.00
- Despesas: 3000.00
- Período: 2025-10

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar relatório de saldo | Status 200 OK |
| 2 | Validar cálculo | Saldo = 5000 - 3000 = 2000 |
| 3 | Verificar estrutura | Map com revenues, expenses, balance |

**Resultados esperados**: Saldo calculado corretamente

**Pós-condições**: Nenhuma alteração no banco

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT053 - Calcular Saldo Positivo

**Pré-condições**: Receitas > Despesas

**Valores de entrada**:
- Receitas: 10000.00
- Despesas: 4500.50

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = 5499.50 |
| 2 | Verificar sinal | balance > 0 |
| 3 | Validar precisão | 2 casas decimais |

**Resultados esperados**: Saldo positivo correto

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT054 - Calcular Saldo Negativo

**Pré-condições**: Despesas > Receitas

**Valores de entrada**:
- Receitas: 2000.00
- Despesas: 3500.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = -1500.00 |
| 2 | Verificar sinal | balance < 0 |
| 3 | Validar formato | Número negativo |

**Resultados esperados**: Saldo negativo correto

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT055 - Calcular Saldo Zero

**Pré-condições**: Receitas = Despesas

**Valores de entrada**:
- Receitas: 5000.00
- Despesas: 5000.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = 0.00 |
| 2 | Verificar comparação | compareTo(ZERO) = 0 |
| 3 | Validar equilíbrio | Receitas = Despesas |

**Resultados esperados**: Saldo zero quando equilibrado

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT056 - Calcular Saldo Sem Receitas

**Pré-condições**: Apenas despesas cadastradas

**Valores de entrada**:
- Receitas: 0.00
- Despesas: 1500.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = -1500.00 |
| 2 | Verificar revenues | revenues = 0.00 |
| 3 | Validar expenses | expenses = 1500.00 |

**Resultados esperados**: Saldo negativo sem receitas

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT057 - Calcular Saldo Sem Despesas

**Pré-condições**: Apenas receitas cadastradas

**Valores de entrada**:
- Receitas: 3000.00
- Despesas: 0.00

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = 3000.00 |
| 2 | Verificar revenues | revenues = 3000.00 |
| 3 | Validar expenses | expenses = 0.00 |

**Resultados esperados**: Saldo positivo sem despesas

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT058 - Manter Precisão Decimal BRL

**Pré-condições**: Lançamentos com centavos

**Valores de entrada**:
- Receitas: 1234.56
- Despesas: 789.12

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular saldo | balance = 445.44 |
| 2 | Verificar scale | scale = 2 |
| 3 | Validar precisão | Casas decimais preservadas |

**Resultados esperados**: Precisão de 2 casas mantida

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT059 - Retornar Distribuição por Categoria

**Pré-condições**: Lançamentos em múltiplas categorias

**Valores de entrada**:
- Período: 2025-10

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar distribuição | Status 200 OK |
| 2 | Verificar agrupamento | Map<String, BigDecimal> |
| 3 | Validar categorias | Alimentação: 1500, Transporte: 800, Lazer: 500 |

**Resultados esperados**: Distribuição correta por categoria

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT060 - Retornar Mapa Vazio Sem Lançamentos

**Pré-condições**: Período sem lançamentos

**Valores de entrada**:
- Período: 2025-11

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar distribuição | Status 200 OK |
| 2 | Verificar resposta | Map vazio |
| 3 | Validar estrutura | isEmpty() = true |

**Resultados esperados**: Mapa vazio retornado

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT061 - Agrupar Valores por Categoria

**Pré-condições**: Múltiplos lançamentos por categoria

**Valores de entrada**:
- Alimentação: 3 lançamentos
- Saúde: 2 lançamentos

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Calcular totais | Soma por categoria |
| 2 | Verificar agrupamento | Alimentação: 2500.75, Saúde: 1200.50 |
| 3 | Validar precisão | 2 casas decimais |

**Resultados esperados**: Agrupamento correto

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT062 - Retornar Distribuição Anual

**Pré-condições**: Lançamentos ao longo de 2025

**Valores de entrada**:
- Período: 2025-01-01 a 2025-12-31

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar distribuição anual | Status 200 OK |
| 2 | Verificar período | Ano completo processado |
| 3 | Validar categorias | Moradia: 12000, Educação: 8000 |

**Resultados esperados**: Distribuição anual correta

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT063 - Retornar Evolução Mensal

**Pré-condições**: Lançamentos em diversos meses

**Valores de entrada**:
- Ano: 2025

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar evolução mensal | Status 200 OK |
| 2 | Verificar estrutura | List<Map<String, Object>> |
| 3 | Validar dados | Janeiro: R:5000 D:3000, Fevereiro: R:5500 D:3200 |

**Resultados esperados**: Evolução mensal completa

**Pós-condições**: Sistema inalterado

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

### CT064 - Retornar Lista Vazia Sem Dados do Ano

**Pré-condições**: Ano sem lançamentos

**Valores de entrada**:
- Ano: 2024

| Etapa | Descrição | Resultado Esperado |
|:---:|:---|:---|
| 1 | Solicitar evolução de 2024 | Status 200 OK |
| 2 | Verificar resposta | Lista vazia |
| 3 | Validar estrutura | isEmpty() = true |

**Resultados esperados**: Lista vazia para ano sem dados

**Pós-condições**: Nenhuma alteração

**Estado**: ✅ Aprovado / ⬜ Reprovado

---

## Estratégias de Teste

### 🔬 Teste de Unidade

| **Aspecto** | **Descrição** |
|:---|:---|
| **Objetivo** | Assegurar correção e robustez da lógica de negócio na camada de serviço, verificando validações, processamento de dados e interações com repositórios |
| **Técnica** | • Teste de Caixa-Branca<br>• Mocking com Mockito para simular dependências<br>• Asserções JUnit para validar resultados<br>• Verificação de interações (verify, never) |
| **Critério de Finalização** | • 100% dos casos de teste executam com sucesso<br>• Cobertura mínima de 80% nas classes de serviço |
| **Considerações Especiais** | • Setup (@BeforeEach) para inicializar mocks<br>• Validação de estado e interações<br>• Ambiente isolado por teste<br>• Testes organizados por módulo funcional |

---

## Matriz de Rastreabilidade

| Requisito | Casos de Teste | Quantidade |
|:---|:---|:---:|
| **RF01 - CRUD Categorias** | CT001-CT015 | 15 |
| **RF02 - CRUD Lançamentos** | CT016-CT036 | 21 |
| **RF03 - Recorrência Automática** | CT037-CT051 | 15 |
| **RF04 - Listagem com Filtros** | CT028-CT034 | 7 |
| **RF05 - Relatórios Visuais** | CT052-CT064 | 13 |
| **RF06 - Metadados de Auditoria** | CT001, CT016, CT018 | 3 |
| **RF07 - Validação @Valid** | CT003-CT007, CT019-CT027 | 14 |
| **RN01 - Vínculo Obrigatório** | CT024 | 1 |
| **RN02 - Validação de Valor** | CT019-CT021 | 3 |
| **RN03 - Recorrência Obrigatória** | CT037-CT051 | 15 |
| **RN04 - Não Edição** | - | 0 |
| **RN05 - Exclusão com Integridade** | CT008-CT011 | 4 |
| **RN06 - Filhos com Parent** | CT037-CT038, CT047-CT048 | 4 |
| **RN07 - Limite 2 Filhos/Dia** | CT039 | 1 |

---

## Resumo Executivo

| Métrica | Valor |
|:---|:---:|
| **Total de Casos de Teste** | 64 |
| **Cobertura de Requisitos Funcionais** | 100% |
| **Requisitos Funcionais Testados** | 7 de 7 |
| **Regras de Negócio Testadas** | 7 de 7 |
| **Módulos de Teste** | 4 |
| **- Categorias** | 15 casos |
| **- Lançamentos** | 21 casos |
| **- Recorrências** | 15 casos |
| **- Relatórios** | 13 casos |
| **Técnicas Aplicadas** | Caixa-Branca, Mocking |
| **Frameworks** | JUnit 5, Mockito, AssertJ |
| **Cobertura de Código Esperada** | ≥ 80% |

### Distribuição de Casos por Tipo

| Tipo de Teste | Quantidade | Percentual |
|:---|:---:|:---:|
| **Validação de Entrada** | 22 | 34.4% |
| **Lógica de Negócio** | 24 | 37.5% |
| **Integridade Referencial** | 8 | 12.5% |
| **Filtros e Consultas** | 10 | 15.6% |

### Critérios de Aceitação

✅ **Aprovação do Plano**: Todos os 64 casos de teste devem executar com sucesso  
✅ **Cobertura Mínima**: 80% de cobertura de código nas camadas de serviço  
✅ **Zero Regressões**: Nenhum teste existente pode falhar após mudanças  
✅ **Tempo de Execução**: Suite completa deve executar em < 30 segundos  

---

**Elaborado em**: 12/10/2025
**Versão do Documento**: 2.0
**Status**: ✅ Aprovado em Execução