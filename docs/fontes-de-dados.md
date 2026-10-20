# Fontes de dados nutricionais

Registro das tabelas usadas pelo CaLju, das condições de uso e de como cada uma deve ser citada.
Nenhum valor nutricional ou peso de porção entra no app sem constar aqui.

## TACO — composição de alimentos

| Item | Valor |
|---|---|
| Título | Tabela Brasileira de Composição de Alimentos — TACO |
| Edição | 4ª edição revisada e ampliada, 2011 (597 alimentos) |
| Responsável | Núcleo de Estudos e Pesquisas em Alimentação — NEPA/UNICAMP |
| Arquivos oficiais | https://www.nepa.unicamp.br/publicacoes/tabela-taco-pdf/ (PDF) e https://www.nepa.unicamp.br/publicacoes/tabela-taco-excel/ (planilha) |
| SHA-256 do PDF baixado em 20/10/2026 | `2002aec5615b5b1395aaa8fa675635bbb7f712c33f278af5e332f1cac8f108c8` |
| SHA-256 da planilha baixada em 20/10/2026 | `a66b8ec528daeabc63bc2b015fc9bd8c6d76b941c2fc0ed93a4311d449302d14` |
| Uso no app | Valores por 100 g de energia, macronutrientes e micronutrientes |
| Situação | **Liberada, com citação obrigatória** |

Condição de uso, transcrita da página de créditos do PDF oficial:

> "É permitida a reprodução parcial ou total desta obra, desde que citada a fonte."

Citação a exibir na tela "Fontes" do app:

> NÚCLEO DE ESTUDOS E PESQUISAS EM ALIMENTAÇÃO — NEPA/UNICAMP. Tabela brasileira de
> composição de alimentos — TACO. 4. ed. rev. e ampl. Campinas: NEPA-UNICAMP, 2011. 161 p.

Observações para o gerador do catálogo:

- "Tr" (traço) vira `qualifier = TRACE`; célula em branco ("análises não solicitadas") vira
  nutriente ausente (desconhecido), nunca zero.
- A planilha tem três abas: composição geral, ácidos graxos e aminoácidos. O MVP usa a primeira.

## IBGE — medidas caseiras (porções padrão)

| Item | Valor |
|---|---|
| Título | Pesquisa de Orçamentos Familiares 2008-2009: Tabela de Medidas Referidas para os Alimentos Consumidos no Brasil |
| Edição | IBGE, Rio de Janeiro, 2011. ISBN 978-85-240-4196-9 |
| Arquivo | https://biblioteca.ibge.gov.br/visualizacao/livros/liv50000.pdf |
| Uso no app | Peso em gramas de "1 unidade" e outras medidas, para registrar sem digitar gramas |
| Situação | **Pendente: aguardando confirmação do IBGE** |

O que foi encontrado:

- A publicação traz apenas "© IBGE. 2011", sem autorização expressa de reprodução.
- A página oficial de termos de uso do IBGE não traz regra geral de reprodução de publicações.
- Fontes secundárias (respostas antigas do atendimento do IBGE e da CGU) indicam que a
  reprodução é aceita com citação da fonte, mas não substituem uma posição oficial.

Recomendação (aguardando aprovação do autor): pedir confirmação ao IBGE antes de embutir os
pesos no app. Até a resposta, o app funciona com porções criadas pelo usuário.
