# CaLju

Diário alimentar e calculadora nutricional pessoal para Android.

> Princípio central: **facilitar o registro alimentar, não transformá-lo em mais uma tarefa.**

O CaLju é *local-first*: tudo o que é essencial funciona 100% offline, no próprio
aparelho. A conta é opcional e serve apenas para backup e sincronização entre
aparelhos — nunca para bloquear o uso local.

## Status

Projeto em desenvolvimento, ainda sem versão publicada.

| Fase | Descrição | Situação |
|---|---|---|
| 1 | Arquitetura e planejamento | Concluída |
| 2 | Ambiente e estrutura inicial | Em andamento |
| 3–10 | Ver [docs/arquitetura.md](docs/arquitetura.md#10-sequência-de-implementação) | A fazer |

## Funcionalidades previstas para o MVP

- Catálogo de alimentos offline (base inicial: TACO, sujeita à conferência dos termos de uso).
- Alimentos personalizados com porções definidas pelo usuário.
- Calculadora nutricional que distingue valor zero, traço e valor desconhecido.
- Refeições salvas e reutilizáveis.
- Diário por data e ocasião (café da manhã, almoço, jantar, lanches).
- Metas de calorias e macronutrientes com data de vigência.
- Histórico, tema claro e escuro.
- Conta opcional com backup e sincronização.

## Arquitetura (resumo)

| Parte | Tecnologias |
|---|---|
| App Android (`android/`) | Java, Views/XML + Material 3, MVVM, Room, WorkManager |
| Backend (`backend/`) | Java, Spring Boot, Maven, PostgreSQL, Flyway |
| Infraestrutura | Docker Compose |

Detalhes, modelo de dados, protocolo de sincronização e decisões de segurança estão em
[docs/arquitetura.md](docs/arquitetura.md).

## Estrutura do repositório

```
Calju/
├── android/   app Android (a criar na Fase 2)
├── backend/   API Spring Boot (a criar na Fase 2)
├── tools/     ferramentas auxiliares, como o gerador do catálogo (Fase 3+)
└── docs/      arquitetura, decisões, fontes de dados e autoria
```

## Como executar

As instruções de compilação e execução serão adicionadas aqui à medida que o backend e
o app Android forem criados, junto com as versões exatas de cada ferramenta necessária.

## Segurança

Senhas, tokens, chaves de assinatura e demais segredos **nunca** são versionados.
Eles são fornecidos por variáveis de ambiente ou arquivos locais ignorados pelo Git
(ver [.gitignore](.gitignore)).

## Autoria

Autor: Lucas Lima Celino.

O uso de ferramentas de IA no desenvolvimento é documentado em `docs/autoria.md`.

## Licença

Todos os direitos reservados. A licença definitiva será definida antes da publicação.
