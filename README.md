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
| 2 | Ambiente e estrutura inicial | Concluída |
| 3 | Domínio e calculadora nutricional | Concluída |
| 4a | Catálogo, busca e campo de registro | Concluída |
| 4b | Diário com registro, edição, remoção e painel do dia | Concluída |
| 4c | Metas e refeições salvas | Concluída |
| 4d | Histórico, salvar registro como refeição, alimentos personalizados | Concluída |
| 5 | API REST do backend | A fazer |
| 6–10 | Ver [docs/arquitetura.md](docs/arquitetura.md#10-sequência-de-implementação) | A fazer |

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
| Backend (`backend/`) | Java 17, Spring Boot 4.1, Maven, PostgreSQL 18, Flyway |
| Infraestrutura | Docker Compose |

Detalhes, modelo de dados, protocolo de sincronização e decisões de segurança estão em
[docs/arquitetura.md](docs/arquitetura.md).

## Estrutura do repositório

```
Calju/
├── android/   app Android
├── backend/   API Spring Boot
├── tools/     catalog-builder: gera o catálogo a partir da TACO
├── data/      fontes oficiais (data/fontes) e catálogo gerado (data/catalog)
└── docs/      arquitetura, decisões, fontes de dados e autoria
```

## Como executar

### Pré-requisitos

- JDK 17
- Docker Desktop (Docker Compose v2)
- Android SDK (somente para o app)

O Maven não precisa estar instalado: o projeto usa o Maven Wrapper (`mvnw`).

### Banco de dados de desenvolvimento

```powershell
Copy-Item .env.example .env   # depois defina POSTGRES_PASSWORD no .env
docker compose up -d
docker compose ps             # aguarde o estado "healthy"
```

### Backend

```powershell
cd backend
.\mvnw test               # testes (usam Testcontainers; o Docker precisa estar rodando)
.\mvnw spring-boot:run    # sobe a API em http://localhost:8080
```

Verificação: `http://localhost:8080/actuator/health` deve responder `{"status":"UP", ...}`.

### Catálogo de alimentos

Gerado a partir da planilha oficial da TACO (ver [docs/fontes-de-dados.md](docs/fontes-de-dados.md)):

```powershell
cd tools\catalog-builder
.\mvnw test package
java -cp target\classes io.github.lucaslimacelino_dev.calju.catalog.Main ..\..\data\fontes\taco\taco-4a-edicao-2011.xlsx ..\..\data\fontes\ibge\porcoes-unidade.csv ..\..\data\catalog
```

### App Android

Requer o Android SDK (instalado pelo Android Studio Quail 4 ou mais recente). No Android
Studio, abra a pasta `android/`. Pela linha de comando:

```powershell
cd android
.\gradlew testDebugUnitTest            # testes do domínio (sem emulador)
.\gradlew assembleDebug                # gera app/build/outputs/apk/debug/app-debug.apk
.\gradlew connectedDebugAndroidTest    # testes de interface (emulador ligado)
```

## Segurança

Senhas, tokens, chaves de assinatura e demais segredos **nunca** são versionados.
Eles são fornecidos por variáveis de ambiente ou arquivos locais ignorados pelo Git
(ver [.gitignore](.gitignore)).

## Autoria

Autor: Lucas Lima Celino.

O uso de ferramentas de IA no desenvolvimento é documentado em `docs/autoria.md`.

## Licença

Software proprietário — todos os direitos reservados. Ver [LICENSE](LICENSE).
