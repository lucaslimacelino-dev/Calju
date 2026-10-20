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
