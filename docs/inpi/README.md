# Registro do CaLju no INPI

Roteiro para pedir o registro de programa de computador (RPC) no INPI. As regras abaixo foram
conferidas em outubro de 2026 no portal gov.br e no *Manual do Usuário e-Software* (v1.8.4) do
INPI; confira os valores de novo no dia do pedido.

## O que o registro faz

- A proteção do programa **não depende de registro** (Lei 9.609/98, art. 2º, §3º). O registro é
  uma **prova** de autoria e de data: o INPI guarda o resumo hash do código; o código fica com
  você.
- Validade: 50 anos, contados de 1º de janeiro do ano seguinte à publicação (ou à criação).
- Versões posteriores podem ser registradas de novo; o manual recomenda isso quando o código
  mudar tanto que um perito teria dificuldade de ligá-lo à versão registrada.

## Custos e requisitos

| Item | Situação conferida |
|---|---|
| Taxa (GRU, código **730**, pedido eletrônico pelo e-Software) | R$ 210,00, sem desconto para programas de computador |
| Assinatura da Declaração de Veracidade | **Certificado digital ICP-Brasil** (e-CPF). Assinatura gov.br **não** é aceita |
| Cadastro | Conta no e-INPI |
| Prazo estimado do serviço | Até 8 dias corridos |

O certificado e-CPF é pago à parte, em uma autoridade certificadora (lista em https://mapa.iti.gov.br/).

## Passo a passo

1. **Congelar a versão.** Quando o app estiver como você quer registrar, crie a tag e envie:

   ```powershell
   git tag -a v1.0.0 -m "Versão registrada no INPI"
   git push origin v1.0.0
   ```

2. **Gerar o pacote e o hash** (fica fora do repositório, em `$HOME\CaLju-INPI\v1.0.0`):

   ```powershell
   .\tools\inpi\gerar-pacote.ps1 -Tag v1.0.0
   ```

   O script cria um único `.zip` com o código-fonte dessa tag (sem os arquivos oficiais de
   terceiros de `data/fontes`), calcula o **SHA-512**, o algoritmo recomendado pelo manual, e
   grava `resumo_hash.txt` e `LEIA-ME.txt`. O resultado é o mesmo do comando do manual
   (`CertUtil -hashfile arquivo SHA512`), e gerar de novo a partir da mesma tag dá o mesmo hash.

3. **Guardar o pacote.** Não altere nem renomeie o `.zip`. Faça cópias de segurança (pendrive e
   nuvem pessoal). Num processo judicial, é esse arquivo que um perito confere com o hash.

4. **Cadastro no e-INPI** e emissão da **GRU código 730**. Pague **sem agendamento**: o manual
   avisa que pagamento agendado faz a petição "não conhecida", sem devolução.

5. **Formulário e-Software** (sugestões abaixo), anexando a Declaração de Veracidade (DV) em
   PDF assinada com o seu e-CPF. Baixe a DV original gerada pelo sistema e assine esse arquivo:
   o manual avisa que DV impressa ou recriada em outro PDF é recusada. A GRU e a assinatura da DV
   precisam ser da mesma pessoa física (o titular). Quem pede em nome próprio não precisa de
   procuração.

6. **Acompanhar** a Revista da Propriedade Industrial (publicada às terças-feiras) e baixar o
   certificado no sistema de busca do INPI.

## Sugestões para o formulário

| Campo | Sugestão |
|---|---|
| Título | CaLju – diário alimentar e calculadora nutricional |
| Titular e autor | Lucas Lima Celino (pessoa física): nome, CPF, endereço, e-mail e telefone |
| Data de criação | Data em que o programa passou a cumprir todas as suas funções (ex.: data do commit da tag registrada, que aparece no `LEIA-ME.txt`) |
| Data de publicação | Data em que o programa ficou acessível ao público. O código está público no GitHub desde 20/10/2026 (criação do repositório) e o app ainda não foi distribuído; decida qual data informar e, na dúvida, consulte um especialista |
| Linguagens | JAVA; SQL (ou POSTGREE SQL); XML; HTML; JAVA SCRIPT. Em "outras": Kotlin (scripts de build) |
| Campo de aplicação | Escolha na lista do formulário a opção ligada a saúde, nutrição ou alimentação |
| Tipo de programa | Escolha na lista do formulário as opções de aplicativo e de gerenciamento de dados |
| Algoritmo hash | SHA-512 |
| Resumo hash | Conteúdo de `resumo_hash.txt` |
| Derivação autorizada | Não |

As tabelas oficiais de campo de aplicação e tipo de programa saíram do endereço citado no
manual; os códigos aparecem como lista no próprio formulário. Não use códigos de memória.

## Pontos de atenção

- **Uso de assistente de IA.** O autor de um programa, para o INPI, é a pessoa física que o
  criou. Parte do código foi escrita com apoio de um assistente de IA, sob direção, decisões e
  revisão do autor, como registrado em [docs/autoria.md](../autoria.md). Há discussão jurídica
  em aberto sobre a proteção de conteúdo gerado por IA; antes do pedido, vale uma consulta a um
  advogado de propriedade intelectual sobre como declarar a autoria.
- **Dados de terceiros.** A TACO (NEPA/UNICAMP) e as medidas do IBGE não são obra do autor: o
  registro cobre o programa, não esses dados. Eles ficam fora do pacote e são citados no app.
- **Repositório público.** O registro não depende de o código ser secreto; o que importa é guardar
  o pacote exatamente como foi gerado.
- **Declarações verdadeiras.** As informações do formulário são de responsabilidade de quem pede
  (Decreto 2.556/98, art. 2º).

## Fontes

- Solicitar o registro de programa de computador (gov.br): https://www.gov.br/pt-br/servicos/solicitar-o-registro-de-programa-de-computador
- Manual do Usuário e-Software v1.8.4 (INPI): https://www.gov.br/inpi/pt-br/assuntos/arquivos-programa-de-computador/ManualdoUsurioV1.8.4.pdf
- Lei 9.609/98: https://www.planalto.gov.br/ccivil_03/leis/l9609.htm
- Instrução Normativa INPI/PR nº 099/2019: https://www.gov.br/inpi/pt-br/servicos/programas-de-computador/arquivos/legislacao/IN992019.pdf
