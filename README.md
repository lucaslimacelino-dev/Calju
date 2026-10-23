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
| 4e | Visual "Caju conversa" com o mascote Caju e boas-vindas | Concluída |
| 5 | API REST do backend | Concluída |
| 6 | Conta: cadastro, login (e-mail e Google), tokens | Concluída (backend) |
| 7 | Sincronização (backend e app) | Concluída |
| 8 | Ícone, fonte Nunito, unidade ml e acessibilidade | Concluída |
| 9 | APK de release (sem conta enquanto não há servidor de produção) | Concluída (falta criar a chave e testar no celular) |
| 10 | Ver [docs/arquitetura.md](docs/arquitetura.md#10-sequência-de-implementação) | A fazer |

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
Copy-Item .env.example .env   # depois defina POSTGRES_PASSWORD e CALJU_JWT_SECRET no .env
docker compose up -d
docker compose ps             # aguarde o estado "healthy"
```

O `docker compose` também sobe o **Mailpit**, um servidor de e-mail falso: os e-mails do
backend (códigos de confirmação) não saem para a internet e aparecem em http://localhost:8025.

### Backend

O backend exige `CALJU_JWT_SECRET` no `.env` (segredo que assina os tokens). O login com
Google só funciona com `CALJU_GOOGLE_CLIENT_ID` (ID do cliente OAuth do tipo Web, criado no
Google Cloud); sem ele, `/api/v1/auth/google` responde 503 e o resto funciona normalmente. Para gerar um
valor aleatório de 32 bytes no PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

```powershell
cd backend
.\mvnw test               # testes (usam Testcontainers; o Docker precisa estar rodando)
.\mvnw spring-boot:run    # sobe a API em http://localhost:8080
```

Verificação: `http://localhost:8080/actuator/health` deve responder `{"status":"UP", ...}`.

Página pública de exclusão de conta (sem app e sem senha, com código por e-mail):
`http://localhost:8080/excluir-conta.html`.

#### Imagem Docker de produção

O backend ainda não está publicado. A imagem fica pronta para quando houver um servidor:

```powershell
docker build -t calju-backend backend
```

Ela roda no perfil `prod`, como usuário sem privilégios, e exige estas variáveis de ambiente
(sem valor padrão; faltando alguma, o servidor não sobe): `POSTGRES_HOST`, `POSTGRES_DB`,
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `CALJU_JWT_SECRET`, `MAIL_HOST`, `MAIL_USERNAME`,
`MAIL_PASSWORD` e `MAIL_FROM` (opcionais: `POSTGRES_PORT`, `MAIL_PORT`, `CALJU_GOOGLE_CLIENT_ID`).
O e-mail exige TLS (STARTTLS). O HTTPS fica num proxy reverso na frente do contêiner; o IP
real do cliente é lido do `X-Forwarded-For` só quando a conexão vem da rede interna.

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

### APK de release (versão final)

A versão final é 100% offline: sem conta, sem sincronização e sem permissão de internet,
até existir um servidor de produção. Ela é assinada com uma chave que **nunca** entra no
repositório. Perder a chave impede instalar atualizações por cima da versão já instalada;
guarde uma cópia de segurança dela e das senhas fora deste computador.

1. Criar a chave (uma vez só), numa pasta fora do repositório. O `keytool` vem com o JDK e
   pede a senha no terminal (no formato PKCS12, a senha da chave é a mesma do arquivo):

   ```powershell
   New-Item -ItemType Directory -Force $HOME\.android-keys
   keytool -genkeypair -v -keystore $HOME\.android-keys\calju-release.jks -storetype PKCS12 `
       -alias calju -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Informar ao Gradle onde está a chave, no arquivo do usuário
   `%USERPROFILE%\.gradle\gradle.properties` (também fora do repositório):

   ```properties
   CALJU_RELEASE_STORE_FILE=C:/Users/SEU_USUARIO/.android-keys/calju-release.jks
   CALJU_RELEASE_STORE_PASSWORD=a senha escolhida no passo 1
   CALJU_RELEASE_KEY_ALIAS=calju
   CALJU_RELEASE_KEY_PASSWORD=a mesma senha
   ```

3. Gerar e conferir o APK:

   ```powershell
   cd android
   .\gradlew assembleRelease             # app/build/outputs/apk/release/app-release.apk
   & "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs `
       app\build\outputs\apk\release\app-release.apk
   ```

Sem o passo 2, o `assembleRelease` gera um APK sem assinatura, que o Android não instala.

Verificação de desenvolvedor do Android: no Brasil, desde 30/09/2026, apps cujo nome de pacote
não está registrado deixam de ser instaláveis em aparelhos certificados. Para distribuir a
outras pessoas, crie uma conta de distribuição limitada no Android Developer Console (gratuita,
para estudantes e projetos sem fins comerciais, até 20 aparelhos autorizados) e registre o
pacote `io.github.lucaslimacelino_dev.calju` com o SHA-256 da sua chave, mostrado por
`keytool -list -v -keystore $HOME\.android-keys\calju-release.jks`. Para instalar só no seu
celular, a instalação por adb continua possível.
Para instalar no celular: copie o APK, permita "instalar apps desconhecidos" para o app que
vai abri-lo e desinstale antes a versão de desenvolvimento (as assinaturas são diferentes).

## Segurança

Senhas, tokens, chaves de assinatura e demais segredos **nunca** são versionados.
Eles são fornecidos por variáveis de ambiente ou arquivos locais ignorados pelo Git
(ver [.gitignore](.gitignore)).

## Autoria

Autor: Lucas Lima Celino.

O uso de ferramentas de IA no desenvolvimento é documentado em `docs/autoria.md`.

## Licença

Software proprietário — todos os direitos reservados. Ver [LICENSE](LICENSE).
