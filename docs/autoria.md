# Autoria e uso de inteligência artificial

## Autor e titular

Lucas Lima Celino.

## Ferramenta utilizada

Assistente de programação baseado em IA generativa (modelo de linguagem), usado por meio
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
