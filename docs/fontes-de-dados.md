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

Cópias guardadas no repositório: `data/fontes/taco/taco-4a-edicao-2011.pdf` e
`data/fontes/taco/taco-4a-edicao-2011.xlsx` (mesmos SHA-256 acima).

### Como o catálogo é gerado

`tools/catalog-builder` lê a aba "CMVCol taco3" da planilha e grava em `data/catalog/`:
`nutrients.csv`, `foods.csv` e `food_nutrients.csv`. Resultado em 20/10/2026: 596 alimentos,
15 categorias, 12.561 valores (1.786 traços).

| Situação na planilha | Tratamento |
|---|---|
| Número | Arredondado para 4 casas decimais (a planilha tem precisão total; a tabela impressa é arredondada) |
| "Tr" | Traço (`TRACE`) |
| Vazio ("análises não solicitadas"), "NA" (não aplicável), "*" (em reavaliação) | Desconhecido: o nutriente não entra |
| Alimento sem nenhum valor conhecido | Fica de fora: nº 591 "Coco, verde, cru" (todo "*") |
| Carboidrato negativo (por diferença) | Zero, como na tabela impressa: nº 288, 322, 337 e 400 (entre −0,045 e −0,007 na planilha; "0,0" no PDF) |
| Texto inválido | Interrompe a geração, exceto a célula listada abaixo |
| Célula AA do nº 373 "Carne, bovina, paleta, com gordura, crua" contém ",0,02" | Piridoxina tratada como desconhecida (não foi possível confirmar o valor no PDF) |

Colunas não usadas: energia em kJ (duplica a kcal; o INFOODS recomenda guardar só uma). Abas de
ácidos graxos e aminoácidos ficam para depois.

Vitamina A: a planilha tem RE (coluna W, código `VITA`) e RAE (coluna X, código `VITA_RAE`, o
mesmo usado pela TBCA/USP). Desde 26/10/2026 as duas são importadas; o app mostra a RAE, que é a
medida do valor diário de referência da ANVISA (IN 75/2020). Na planilha, RAE = RE para retinol
e RAE = RE ÷ 2 para carotenoides (ex.: cenoura crua, RE 1.326 µg e RAE 663 µg).

## USDA FoodData Central — vitamina B12 (`data/fontes/usda/b12-alimentos-comuns.csv`)

A TACO não mede vitamina B12. Por decisão do autor (26/10/2026), a B12 dos alimentos comuns vem
do USDA FoodData Central, base **SR Legacy (abril de 2018)**, que é de domínio público (CC0),
baixada de fdc.nal.usda.gov. O arquivo guarda, para cada alimento: número e nome na TACO, B12 em
µg por 100 g, número FDC e descrição do alimento americano usado (valores copiados por script
a partir do número FDC, nunca digitados). Critérios:

- **Origem animal de um ingrediente** (77 alimentos: carnes, aves, peixes, ovos, leite, queijos,
  manteiga, maionese, chocolate ao leite, doce de leite): valor do alimento americano mais
  parecido no corte e no preparo. É uma aproximação: cortes, raças e preparos diferem (ex.:
  cupim → brisket; queijo prato → gouda; merluza → whiting; sardinha fresca → sardinha em lata).
- **Só de origem vegetal** (128 alimentos: frutas, verduras, feijões, cereais simples, óleos,
  açúcar, café, refrigerantes etc.): 0, pois a B12 só existe naturalmente em alimentos de origem
  animal.
- **Sem valor**: pratos e produtos com mistura de ingredientes (estrogonofe, pastel, bolo, pão de
  queijo, empanados, omelete com queijo), linguiça de frango e queijos sem equivalente (minas
  meia cura, requeijão, petit suisse), e itens que costumam ser fortificados (cereal matinal,
  achocolatado em pó, bebida de soja). Os demais alimentos da TACO também ficam sem B12.

O gerador do catálogo confere número e nome de cada alimento na TACO e falha se divergirem.

### Lista de alimentos comuns (`data/catalog/alimentos-comuns.csv`)

Curadoria própria (25/10/2026, pedido do autor para simplificar o app para quem não é da área):
272 alimentos da TACO escolhidos entre os mais consumidos, cada um com um nome do dia a dia
(`numero_taco,nome`, por exemplo `488,Ovo cozido`). Os valores nutricionais continuam os da TACO;
só muda o nome mostrado. Critérios: versões cozidas ou preparadas em vez das cruas (frutas e
saladas ficam cruas), um representante por tipo quando as variedades têm valores parecidos e
pratos regionais.

No app, esses alimentos aparecem primeiro na busca (junto com os criados pelo usuário); os outros
ficam em "Ver mais opções". O nome original da TACO continua guardado e também é encontrado na
busca ("arroz tipo 1 cozido" acha "Arroz branco cozido").

Correção: o nº 540 aparece como "L" na aba "CMVCol taco3" da planilha oficial, mas a aba de
ácidos graxos da mesma planilha ("AGtaco3") traz, na mesma sequência, "Feijoada" (logo depois de
"Feijão tropeiro mineiro"). A lista de comuns usa o nome "Feijoada".

O cabeçalho (nome e unidade de cada coluna) é conferido a cada geração; se a planilha mudar,
a geração falha em vez de trocar nutrientes. Códigos de nutriente conferidos no corpus oficial
de tagnames INFOODS (pacote `@infoods/tagnames` 1.0.1).

## IBGE — medidas caseiras (porções padrão)

| Item | Valor |
|---|---|
| Título | Pesquisa de Orçamentos Familiares 2008-2009: Tabela de Medidas Referidas para os Alimentos Consumidos no Brasil |
| Edição | IBGE, Rio de Janeiro, 2011. ISBN 978-85-240-4196-9 |
| Publicação (PDF) | https://biblioteca.ibge.gov.br/visualizacao/livros/liv50000.pdf |
| Dados usados | `tabelamedidas_bd.zip` (planilha `tabelamedidas_bd.xls`, aba "Tab_Medidas Caseiras") em https://ftp.ibge.gov.br/Orcamentos_Familiares/Pesquisa_de_Orcamentos_Familiares_2008_2009/Tabela_de_Medidas_Referidas_para_os_Alimentos_Consumidos_no_Brasil/ |
| SHA-256 do zip baixado em 20/10/2026 | `3d0ff06acf0b55c22a621f57e6fb218d4505af204ed2be1571bfbe02fbab17c9` |
| Uso no app | Peso em gramas de "1 unidade" de 50 alimentos, para registrar sem digitar gramas |
| Situação | **Em uso com citação da fonte** (decisão do autor em 20/10/2026, sem confirmação formal do IBGE) |

O que foi encontrado:

- A publicação traz apenas "© IBGE. 2011", sem autorização expressa de reprodução.
- A página oficial de termos de uso do IBGE não traz regra geral de reprodução de publicações.
- Fontes secundárias (respostas antigas do atendimento do IBGE e da CGU) indicam que a
  reprodução é aceita com citação da fonte, mas não substituem uma posição oficial.

Decisão do autor: usar os pesos citando a fonte, aceitando o risco de não haver confirmação
formal. Se o IBGE vier a restringir o uso, basta esvaziar `porcoes-unidade.csv` e regenerar o
catálogo; o app continua funcionando com porções criadas pelo usuário.

Citação a exibir na tela "Fontes" do app:

> IBGE. Pesquisa de orçamentos familiares 2008-2009: tabela de medidas referidas para os
> alimentos consumidos no Brasil. Rio de Janeiro: IBGE, 2011.

O próprio IBGE compilou cada peso a partir de fontes de referência (aba "Código e Fonte de
Referência" da planilha), por exemplo Pinheiro et al. (2004), Philippi (2008) e Fisberg e Villar
(2002). A coluna `ibge_reference_source` da curadoria guarda esse código.

### Curadoria (`data/fontes/ibge/porcoes-unidade.csv`)

Cópia do arquivo original em `data/fontes/ibge/tabelamedidas_bd.zip`. A curadoria liga 50
alimentos da TACO a linhas da tabela do IBGE em que a medida é "UNIDADE":

- referência do IBGE com variedade (ex.: "Banana prata - unidade") vale só para essa variedade
  na TACO; outras bananas ficam sem porção padrão;
- referência genérica (ex.: "Laranja - unidade média") vale para as variedades cruas da TACO;
- o preparo segue o da TACO quando o IBGE distingue (ovo cru e cozido 45 g, ovo frito 50 g);
- gramas copiados por script diretamente da planilha, nunca digitados; cada linha guarda o
  número da linha no Excel (`ibge_excel_row`) para conferência;
- o gerador do catálogo confere número e nome de cada alimento na TACO e falha se divergirem.
