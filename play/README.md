# Publicar o CaLju na Google Play

Roteiro para a primeira publicação (versão 1.0.0, offline, sem conta). Os nomes dos menus do
Play Console podem variar um pouco; as regras abaixo foram conferidas em outubro de 2026 nas
páginas de ajuda do Google.

## 1. Antes do Play Console

1. Criar a chave de upload e configurar o Gradle: ver "APK de release" no README principal.
   Na Play, essa chave é a **chave de upload**: o Google guarda a chave que assina o app final
   (Play App Signing). Guarde a cópia de segurança mesmo assim.
2. Publicar a política de privacidade (`site/privacidade.html`) num endereço público. Ela precisa
   ser uma página web acessível sem login (PDF não vale). Antes, troque `[SEU E-MAIL DE CONTATO]`
   pelo e-mail que vai responder dúvidas.
3. Gerar o pacote que a Play aceita (AAB):

   ```powershell
   cd android
   .\gradlew bundleRelease    # app/build/outputs/bundle/release/app-release.aab
   ```

## 2. Conta de desenvolvedor

- Taxa única de US$ 25, cartão de crédito ou débito em seu nome (pré-pago não é aceito); o Google
  pode pedir documento de identidade.
- Conta pessoal criada depois de 13/11/2023 precisa do **teste fechado** (passo 5) antes de poder
  publicar para todos.

## 3. Criar o app e preencher "Conteúdo do app"

| Formulário | O que responder |
|---|---|
| Política de privacidade | O endereço público do passo 1.2 |
| Acesso ao app | Todas as funções ficam disponíveis sem acesso especial (não há login) |
| Anúncios | Não contém anúncios |
| Classificação do conteúdo | Responder o questionário com sinceridade (não há violência, apostas, conteúdo gerado por outros usuários etc.) |
| Público-alvo | Recomendado: 18 anos ou mais (o app não é direcionado a crianças) |
| Segurança dos dados | O app **não coleta nem compartilha** dados: tudo fica no aparelho, sem internet, sem bibliotecas de análise ou anúncios. Confira a definição de "coleta" na própria tela antes de enviar |
| Declaração de apps de saúde | Obrigatória para todos. Marcar a categoria "Nutrição e controle de peso" |
| Apps governamentais, financeiros, notícias | Não |

Exigência para apps de saúde que não são dispositivo médico: o aviso "não é um dispositivo
médico…" e a recomendação de procurar um profissional. Já estão na descrição da loja
(`ficha-da-loja.md`) e no app (Perfil → Sobre).

## 4. Ficha da loja

Textos e imagens em `ficha-da-loja.md` e nesta pasta.

## 5. Teste fechado (obrigatório para conta pessoal nova)

1. "Testar e lançar" → "Teste fechado" → criar uma faixa e enviar o AAB.
2. Adicionar os testadores (lista de e-mails das contas Google deles).
3. Mandar a eles o link de participação: cada um aceita e **instala** o app pela Play.
4. Regra: pelo menos **12 testadores** inscritos **por 14 dias seguidos**, contados até o pedido
   de acesso à produção. Quem sai e volta recomeça a contagem; convidado que não instala não conta.
5. Depois dos 14 dias: pedir acesso à produção no painel. O Google responde o questionário e
   revisa (costuma levar até cerca de 7 dias).

Peça aos testadores que usem o app de verdade e anotem o que acharam: é a melhor rodada de
ajustes antes do lançamento.

## 6. Atualizações

Cada nova versão: aumentar `versionCode` (e o `versionName`) em `android/app/build.gradle.kts`,
gerar o AAB com a mesma chave de upload e enviar numa nova versão da faixa.

## Fontes

- Requisitos dos recursos gráficos: https://support.google.com/googleplay/android-developer/answer/9866151
- Teste fechado de contas pessoais novas: https://support.google.com/googleplay/android-developer/answer/14151465
- Declaração de apps de saúde: https://support.google.com/googleplay/android-developer/answer/14738291
- Segurança dos dados e política de privacidade: https://support.google.com/googleplay/android-developer/answer/10787469
- Taxa de registro: https://support.google.com/googleplay/android-developer/answer/6112435
- Nível de API exigido: https://support.google.com/googleplay/android-developer/answer/11926878
