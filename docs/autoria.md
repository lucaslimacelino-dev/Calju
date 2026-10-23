# Autoria e uso de inteligência artificial

## Autor e titular

Lucas Lima Celino.

## Ferramenta utilizada

Assistente de programação baseado em IA generativa (Opus5.5), usado por meio
de extensão do editor VS Code, com acesso à pasta do projeto.

## Divisão de responsabilidades

| Atividade | Responsável |
|---|---|
| Definição do produto, requisitos e objetivos | Autor |
| Aprovação de decisões técnicas (linguagens, versões, arquitetura, licença, segurança) | Autor |
| Propostas de arquitetura, comparação de alternativas e recomendações | Assistente de IA, com aprovação do autor |
| Geração de rascunhos de código, configuração e documentação | Assistente de IA |
| Execução de comandos de verificação (testes, builds) durante as sessões | Assistente de IA, com permissão do autor |
| Revisão, aceitação, commits e publicação no repositório | Autor |

O assistente não executa `git commit` nem `git push`: todo conteúdo do repositório entra no
histórico por ação do autor. Não são atribuídos percentuais de autoria, pois não há medição
objetiva que os sustente.

## Registro por fase

### Fase 1 — Arquitetura e planejamento

- `docs/arquitetura.md` redigido com o assistente a partir dos requisitos do autor;
  decisões (nome, pacote, versões, autenticação, modelo de dados) aprovadas pelo autor.

### Fase 2 — Ambiente e estrutura inicial

- Arquivos de repositório (`README.md`, `.gitignore`, `.gitattributes`, `LICENSE`,
  `docker-compose.yml`, `.env.example`) gerados pelo assistente.
- Licença proprietária: recomendada pelo assistente e aprovada pelo autor.
- Backend: base gerada pelo Spring Initializr (start.spring.io), ajustada pelo assistente;
  testes de integração escritos pelo assistente.
- Java 17 no backend no lugar de Java 25: decisão do autor.
- App Android: estrutura inicial escrita pelo assistente; Gradle Wrapper gerado pela
  distribuição oficial do Gradle. Ícones da barra inferior baseados nos Material Icons
  (Google, Apache License 2.0).
- Identidade visual: paletas e protótipos de tela propostos pelo assistente; escolha da
  paleta "castanha e caju", da tela inicial (v3), do registro de um alimento por vez, do
  painel animado e das porções padrão para frutas e alimentos de tamanho padrão feita pelo autor.

### Fase 3 — Domínio e calculadora

- Pacote `domain` (modelos, `NutritionCalculator`, `FoodInputParser`) e testes JUnit escritos
  pelo assistente a partir das regras definidas no planejamento e pelo autor.

### Fase 4a — Catálogo

- Conferência dos termos de uso da TACO e do IBGE feita pelo assistente nas publicações
  oficiais; autorização para versionar a planilha da TACO dada pelo autor.
- `tools/catalog-builder` e testes escritos pelo assistente. Tratamento das anomalias da
  planilha (carboidrato negativo, célula inválida) definido pelo assistente com base na
  tabela impressa e registrado em `docs/fontes-de-dados.md`.
- Uso dos pesos do IBGE com citação (opção 2) e importação do catálogo por CSV (opção A):
  decisões do autor entre opções apresentadas pelo assistente. Curadoria das 50 porções,
  banco Room, importador, busca e testes escritos pelo assistente.
- Campo de registro, lista de sugestões e tela de quantidade escritos pelo assistente,
  seguindo a tela v3 aprovada pelo autor; verificados no emulador pelo assistente.

### Fase 4b — Diário

- Tabelas do diário (com snapshot dos nutrientes), migração do banco, repositório, painel do
  dia com números animados, edição, remoção com desfazer, navegação por data, painel de
  micronutrientes e testes escritos pelo assistente, seguindo o planejamento e a tela v3.

### Fase 4c — Metas e refeições salvas

- Metas com vigência, aba Perfil (metas, fontes dos dados, sobre), painel com kcal restantes
  e barras dos macros, migração do banco para a versão 3 e testes escritos pelo assistente.
  Abas Diário | Histórico | Perfil: recomendação do assistente aplicada no planejamento.
- Refeições salvas (tabelas, migração para a versão 4, repositório, edição de registros com
  vários itens, editor em três modos, seção "Refeições prontas" no Diário) e testes escritos
  pelo assistente, seguindo as regras do planejamento (molde × fato histórico) e a tela v3.

### Fase 4d — Completar o app antes do backend

- Antecipação da fase (Histórico, "Salvar como refeição", alimentos personalizados antes do
  backend): opção recomendada pelo assistente e escolhida pelo autor.
- Histórico (dias com totais, meta do dia, média semanal, abrir o dia no Diário) e
  "Salvar como refeição" escritos e testados pelo assistente.
- Tela inicial v4 (resumo grande em castanho, seções por refeição com ícones e "+", campo fixo
  mantido): pedido de estilo e escolha do layout híbrido feitos pelo autor entre opções
  apresentadas pelo assistente; implementação, cores, medição de contraste e testes pelo
  assistente.
- Alimentos personalizados (banco v5, conversão do rótulo para 100 g, validação, cadastro pelo
  Diário e pelo Perfil) e testes escritos pelo assistente, seguindo o desenho proposto pelo
  assistente e aprovado pelo autor (valores digitados como estão na embalagem).

### Fase 4e — Visual com o mascote Caju

- Pedido de um design mais amigável e de um mascote com storytelling: do autor. Personagem
  Caju (desenho original em vetor), propostas de tela A e B e falas: propostos pelo assistente;
  escolha da proposta A com as refeições em lista compacta: do autor.
- Implementação (vetores dos 4 humores, tela de boas-vindas, falas do Caju por situação,
  novo resumo, lista compacta, cores, medição de contraste) e testes escritos pelo assistente.

### Fase 5 — API REST do backend

- Ordem da fase (API já protegida por token, com a emissão na Fase 6): opção recomendada pelo
  assistente e escolhida pelo autor. Login com Google: pedido do autor, previsto para a Fase 6.
- Segurança da API, erros em ProblemDetail, usuário atual, metas, alimentos do usuário,
  refeições salvas e diário (migrações V2 a V5, entidades, serviços, controllers) e testes de
  integração escritos pelo assistente.

### Fase 6 — Conta e login

- Código de 6 dígitos, tokens só após confirmar o e-mail e Mailpit em desenvolvimento: opções
  recomendadas pelo assistente e escolhidas pelo autor. Definir a senha junto com o código (e não
  no cadastro): ajuste de segurança proposto pelo assistente durante a implementação.
- Etapas 1 a 3 (cadastro, confirmação por código, login, renovação com rotação e detecção de
  reuso, logout, Argon2id, e-mails, redefinição de senha, limitação de tentativas e os valores
  iniciais dos limites), etapa 4 (login com Google: verificação do ID token conforme a
  documentação oficial, regra de vínculo por e-mail) e etapa 5 (sessões por aparelho, troca de
  senha, exclusão de conta) e testes escritos pelo assistente.

### Fase 7 — Sincronização

- Envio pelas rotas PUT/DELETE e regras de conflito para alimentos (cópia) e metas (escolha do
  usuário): opções recomendadas pelo assistente e escolhidas pelo autor.
- Etapa 1 (sequência de mudanças com gatilho, trava por usuário, `GET /sync/pull`) e etapa 2
  (rede no app, guarda cifrada da sessão, telas de conta) e testes escritos pelo assistente;
  etapa 3a (motor de sincronização, banco v6) e etapa 3b (sincronização automática com
  WorkManager, vínculo dos dados do aparelho com a conta) e etapa 4 (conflitos: cópia
  "(conflito)", escolha do usuário, reenvio sem duplicar, recusados) e testes escritos pelo
  assistente;
  testes de ponta a ponta no emulador contra o backend local feitos pelo assistente.

### Fase 8 — Refino visual, símbolo e acessibilidade

- Ícone com o rosto do Caju, fonte Nunito embutida e unidade ml contando 1 ml = 1 g: opções
  recomendadas pelo assistente e escolhidas pelo autor.
- Ícone adaptativo gerado pelo assistente a partir do desenho do Caju; pesos estáticos da Nunito
  gerados pelo assistente com fontTools a partir da fonte oficial (OFL).
- Unidade ml (app e servidor), ajustes para fonte grande, correção da descrição do anel para o
  TalkBack, teste automático de acessibilidade e demais testes escritos pelo assistente;
  verificação visual (tema escuro, fonte 200%, paisagem) feita pelo assistente no emulador.

### Fase 9 — Versão de release e backend pronto para produção

- APK sem conta enquanto não há servidor e chave de assinatura fora do repositório: opções
  recomendadas pelo assistente e escolhidas pelo autor. A chave de release é criada e guardada
  pelo autor.
- Configuração de assinatura, versão final sem permissão de internet, Dockerfile, perfil de
  produção, exclusão de conta pela web (página e rotas), testes e documentação escritos pelo
  assistente; teste do APK de release no emulador e do contêiner de produção feitos pelo
  assistente; regra de verificação de desenvolvedor conferida pelo assistente nas páginas oficiais.
- Aviso de saúde e texto de privacidade no Perfil → Sobre escritos pelo assistente.
