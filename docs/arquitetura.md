# CaLju — Arquitetura e planejamento (Fase 1)

Versão do documento: 1.1 — 20/10/2026.
Status: planejamento concluído; Fase 2 concluída. Versão 1.1: Java 17 no backend, paleta
"castanha e caju", tela inicial v3, registro de um alimento por vez, distribuição por APK.

## 1. Visão do produto

CaLju é um diário alimentar e calculadora nutricional pessoal para Android.
Princípio central: **facilitar o registro alimentar, não transformá-lo em mais uma tarefa.**

- O usuário abre o app e já está no diário do dia, com a ação de adicionar visível.
- Refeições salvas e reutilizáveis são a funcionalidade prioritária.
- Tudo o que é essencial funciona offline. A conta serve para backup e sincronização,
  nunca para bloquear o uso local.

## 2. Arquitetura geral (local-first)

O aparelho é a fonte primária dos dados; o backend é opcional.

| Camada | Escolha | Justificativa |
|---|---|---|
| Android | Java (nível 17), minSdk 26, targetSdk 36, compileSdk 37; AGP 9.4.1, Gradle 9.6.1 | targetSdk 36 exigido pela Play desde 31/08/2026; compileSdk só define a API usada na compilação |
| UI | Single-Activity + Fragments + Navigation + Material 3 (XML + ViewBinding) | Jetpack Compose exige Kotlin; Views são o caminho moderno em Java |
| Estado | MVVM: ViewModel + LiveData | UI atualiza automaticamente quando o Room muda |
| Persistência local | Room (SQLite) | Oficial, consultas verificadas em compilação |
| Rede (Fase 5+) | Retrofit + OkHttp + Gson | Padrão consolidado |
| Sync em segundo plano | WorkManager | Restrição de rede, backoff, economia de bateria |
| DI | Manual (`AppContainer`) | Sem Hilt/Dagger: menos mágica, mais didático |
| Backend | Java 17 LTS + Spring Boot 4.1.1 | Java 17 em vez de 25: o computador de desenvolvimento usa o 17 em outro projeto (decisão de 20/10/2026); Spring Boot 4.1 aceita Java 17 a 26 |
| Banco remoto | PostgreSQL + Flyway | Esquema versionado |
| Testes backend | JUnit 5 + Spring Boot Test + Testcontainers | Testes contra PostgreSQL real |
| Infra | Docker Compose + Caddy (TLS automático) em uma VPS | Sem Kubernetes/microsserviços |

O calculador nutricional fica no Android, no pacote `domain` (Java puro, testável com
JUnit). O backend apenas armazena e sincroniza no MVP.

### Diagrama de componentes

```
ANDROID
  UI (Fragments/XML) ──observa LiveData──► ViewModels ──► domain/ (NutritionCalculator)
                                              │
                                        Repositories
                                         │         │
                                  Room (SQLite)   SyncWorker (WorkManager, só logado + rede)
                                                   │   TokenStore (Android Keystore)
                                                   │ HTTPS + Bearer token
SERVIDOR (Docker Compose)                          ▼
  Caddy (TLS) ► Spring Boot: SecurityFilterChain (JWT) ► Controllers (DTOs)
                ► Services (regras + verificação de posse) ► Repositories (JPA)
                ► PostgreSQL (Flyway)          Provedor SMTP (e-mails de conta)
```

Comunicação: o app só chama o backend para autenticação, `POST /sync/push`,
`GET /sync/pull?cursor=…` e gestão da conta. O cliente nunca envia `userId`;
o servidor obtém a identidade do token.

## 3. Modelagem de dados

### Decisões

1. **Nutrientes como linhas** (`nutrients` + `food_nutrients`), com códigos INFOODS
   (`ENERC` em kcal, `PROCNT`, `CHOCDF`, `FAT`, `FIBTG`, `CHOLE`, `NA`, `K`, `CA`, `FE`,
   `MG`, `P`, `ZN`, `VITC`, …; lista completa em `data/catalog/nutrients.csv`, conferida no
   corpus oficial de tagnames INFOODS). Novo nutriente = nova linha, sem migração.
2. **Zero ≠ desconhecido:** linha ausente = desconhecido; `amount = 0` = zero medido;
   `qualifier = TRACE` = traço ("Tr" na TACO). Totais com itens desconhecidos são
   exibidos como incompletos ("≥ X"), nunca tratando desconhecido como zero.
3. **`LogEntry` + `LogItem` no lugar de `DailyLog`:** um lançamento (data, ocasião,
   título opcional, refeição de origem opcional) com um ou mais itens.

### Tabelas

```
users              (id uuid, email UNIQUE, email_verified_at, password_hash, created_at, updated_at)
refresh_tokens     (id, user_id, family_id, token_hash, device_label, created_at, expires_at,
                    revoked_at, replaced_by_id)
email_tokens       (id, user_id, purpose[VERIFY|RESET], token_hash, expires_at, used_at)

data_sources       (id, name, edition, license, url, citation)
nutrients          (code PK, name_pt, unit[g|mg|µg|kcal], display_order)
foods              (id uuid, owner_user_id NULL=oficial, source_id NULL, name, brand NULL,
                    reference_amount=100, reference_unit[g|ml], version, updated_at, deleted_at)
food_nutrients     (food_id, nutrient_code, amount numeric, qualifier[VALUE|TRACE])
food_portions      (id, food_id, owner_user_id NULL, label, grams, source)

meals              (id uuid, owner_user_id, name, version, updated_at, deleted_at)
meal_items         (id uuid, meal_id, food_id, quantity_g, portion_id NULL, position)

log_entries        (id uuid, owner_user_id, log_date DATE, occasion[BREAKFAST|LUNCH|DINNER|SNACK],
                    title NULL, source_meal_id NULL, version, updated_at, deleted_at)
log_items          (id uuid, entry_id, food_id NULL, food_name_snap, brand_snap, source_snap,
                    quantity_g, portion_label_snap)
log_item_nutrients (log_item_id, nutrient_code, amount_per_ref, qualifier)   -- snapshot

nutrition_goals    (id uuid, owner_user_id, effective_from DATE, kcal, protein_g, carbs_g,
                    fat_g, version, updated_at)
```

### Histórico sem duplicação desnecessária

- **Refeição salva = molde:** `meal_items` referencia o alimento atual (correções no
  catálogo se refletem no molde).
- **Registro = fato histórico:** ao confirmar, copiam-se nome, quantidade e valores
  **por 100 g** dos nutrientes conhecidos para `log_item_nutrients`. Alterar catálogo ou
  refeição não altera o passado; guardar por 100 g permite editar a quantidade depois.
- Alternativa descartada: versionamento imutável de alimentos (complica a sincronização).
- Datas do diário em `DATE` local (o dia do usuário), sem conversão de fuso.
- Metas com `effective_from`: cada dia é comparado com a meta vigente naquele dia.
- Alimentos personalizados são excluídos logicamente (`deleted_at`).

### Precisão numérica

- `BigDecimal` em todos os cálculos; Room armazena como `TEXT` via `TypeConverter`;
  PostgreSQL `numeric(12,4)`.
- Arredondamento apenas na exibição: kcal inteiro, macros 1 casa, mg e µg inteiros a partir
  de 10 e com 1 casa abaixo disso (cobre 0,2 mg não pode aparecer como "0 mg").
  Somas sem arredondamento intermediário (a soma das linhas exibidas pode diferir ±1 do total).
- Fórmula: `consumido = valor_por_100 × quantidade_g ÷ 100`.
  Ex.: 165 kcal/100 g × 150 g = 247,5 kcal (exibido "248 kcal").
- Energia vem da fonte; não é recalculada a partir dos macros.

### Fontes de dados

- TACO (4ª ed., NEPA/UNICAMP) como base inicial; TBCA e USDA FoodData Central como
  complementos. **Conferir termos de uso antes de embutir qualquer tabela** (Fase 3) e
  registrar em `docs/fontes-de-dados.md`.
- Medidas caseiras não são inventadas. Porções padrão (ex.: "1 banana média") vêm da
  Tabela de Medidas Referidas para os Alimentos Consumidos no Brasil (IBGE, POF 2008-2009),
  com fonte registrada (`source = REFERENCE`); o usuário pode criar as próprias (`USER`).
  Conferir os termos de uso da tabela na Fase 4a.

## 4. Offline e sincronização

- Catálogo oficial embutido no APK como CSV (gerado por `tools/catalog-builder` em
  `data/catalog/`) e importado para o Room na primeira abertura (`CatalogImporter`; reimporta
  quando `CATALOG_VERSION` aumenta). Escolhido no lugar de `Room.createFromAsset` para não
  acoplar o gerador ao esquema interno do Room (decisão de 20/10/2026).
- O app não declara a permissão `INTERNET` enquanto não houver conta/sincronização.
- **UUID gerado no aparelho**: mesmo identificador local e remoto.

| Coluna | Onde | Papel |
|---|---|---|
| `id` UUID | ambos | identidade global |
| `version` | ambos | concorrência otimista |
| `deleted_at` | ambos | exclusão como lápide (tombstone) |
| `sync_state` (PENDING/SYNCED) | Android | o que falta enviar |
| `change_seq` (bigint) | servidor | cursor do pull (não depende do relógio do celular) |

Protocolo (Fase 7). Decisões do autor em 22/10/2026: o envio **reusa as rotas PUT/DELETE**
de cada recurso (uma requisição por registro, com a versão que o app conhece; 409 = conflito);
alimento em conflito vira cópia "(conflito)" como as refeições; meta em conflito o usuário
escolhe, como os registros do diário.

- Recebimento: `GET /api/v1/sync/pull?cursor=&limit=` (padrão 500, máximo 1000) devolve metas,
  alimentos, refeições e registros com `change_seq` maior que o cursor, inclusive lápides, e o
  novo cursor + `hasMore`. `change_seq` vem de uma sequência única e é preenchido por gatilho no
  banco (V8). Toda gravação trava a linha do usuário em `users` antes de mexer nos dados, então
  os números de um usuário saem na ordem de confirmação e o cursor nunca pula uma mudança.

1. **Push** de linhas `PENDING` com a `version` base; servidor aplica se coincidir,
   senão retorna conflito. UUID pertencente a outro usuário é rejeitado.
   No app (Fase 7, `SyncEngine`): banco local v6 com `server_version` (última versão
   confirmada; 0 = nunca enviado) e `sync_state` PENDING/SYNCED/CONFLICT/FAILED. Ordem: alimentos,
   refeições, metas, registros. Criado e excluído antes de enviar não vai ao servidor. 409/404 →
   CONFLICT; 400 → FAILED (não reenvia em laço); sem rede, 5xx ou sessão vencida → para e tenta
   depois. "Synced" só se o registro não mudou durante o envio (compara `version`).
   Recebimento: páginas de 500 aplicadas numa transação; o cursor só avança depois; registro
   com mudança local pendente nunca é sobrescrito; meta do servidor no mesmo dia de uma meta
   local pendente → CONFLICT.
2. **Pull** por cursor (`change_seq`).
3. **Idempotência:** upsert por UUID + versão; reenvio não duplica.
4. **Conflitos nunca sobrescrevem em silêncio:** refeição → cópia "(conflito)";
   registro do diário → usuário escolhe.
5. **Exclusões** como lápides; limpeza física posterior no servidor.
6. **Falhas de rede:** WorkManager com restrição de rede e backoff; UI nunca bloqueia.
   No app (etapa 3b): `SyncScheduler` agenda o `SyncWorker` no WorkManager 2.12.0 — uma rodada
   5 s depois de cada mudança local pendente (pedidos seguidos se substituem; observa as tabelas
   pelo `InvalidationTracker` do Room) e uma periódica a cada 6 h, sempre só com rede; sem rede ou
   5xx → retry com espera exponencial a partir de 30 s.
7. **Um banco por conta (`SyncCoordinator` + `AccountBinding`):** o aparelho guarda a conta à qual
   os dados pertencem (`calju_sync`/`bound_email`). Ao entrar: sem dados ou mesma conta → vincula
   e sincroniza; dados sem vínculo → pergunta "Enviar para a conta" ou "Apagar deste celular";
   dados de outra conta → "Apagar e entrar" ou cancelar (encerra a sessão nova). Ao sair: tenta
   enviar o pendente, cancela o trabalho agendado e pergunta "Manter" ou "Apagar deste celular"
   (o catálogo TACO nunca é apagado).

Conta e aparelho:

- Uso sem conta é suportado.
- Primeiro login: oferecer envio dos dados locais para a conta.
- Troca de aparelho: login + pull completo.
- Logout: perguntar se mantém ou apaga os dados locais.
- O banco local fica vinculado a uma conta; trocar de conta exige limpar.
- Sessão expirada: o app continua funcionando; só a sincronização pausa.

## 5. Autenticação e segurança

### Tokens (pendente de aprovação)

- **Access token JWT de 15 min**, emitido com `JwtEncoder` e validado pelo
  `spring-boot-starter-oauth2-resource-server`. JWT não é "mais seguro" por si; o ganho é
  não consultar o banco a cada requisição, com o custo de não poder revogá-lo antes de expirar.
- **Refresh token opaco** (256 bits), armazenado apenas como hash SHA-256, validade de
  30 dias, rotação a cada uso e **detecção de reutilização** (revoga a família inteira).
- Chave de assinatura via variável de ambiente ou arquivo montado; nunca no repositório ou no APK.
- Troca de senha ou exclusão de conta revoga todos os refresh tokens; lista de sessões
  por aparelho com opção de encerrar.

- **Fase 5 (decisão do autor em 22/10/2026):** a API de dados já nasce protegida. O backend
  valida tokens HS256 com emissor `calju` e segredo em `CALJU_JWT_SECRET` (Base64, ≥ 256 bits;
  sem ele o backend não inicia). O `sub` do token é o id do usuário; conta inexistente → 401.
  A emissão dos tokens (cadastro/login) é da Fase 6.
- **Login com Google (pedido do autor em 22/10/2026; backend na Fase 6):** `POST /auth/google
  {idToken}`. Conferido conforme a documentação do Google: assinatura pelas chaves de
  `https://www.googleapis.com/oauth2/v3/certs`, `iss` = `accounts.google.com` ou
  `https://accounts.google.com`, `aud` = client ID do tipo Web (`CALJU_GOOGLE_CLIENT_ID`; vazio →
  503), `exp`. A conta é ligada pelo `sub` (V7 `users.google_subject`), nunca pelo e-mail. Sem
  vínculo: conta nova (confirmada, sem senha) ou, se o e-mail já existe, vínculo só quando o
  Google é autoridade sobre o endereço (Gmail ou Workspace com `hd`) e `email_verified`; senão
  409 pedindo a senha. Exclusão de conta só com Google: confirmada por um ID token novo da mesma
  conta. No Android (Fase 7): Credential Manager (`androidx.credentials`) + `googleid`, com
  `setServerClientId` = client ID Web; exemplos oficiais em Kotlin, usar a API com callback em Java.

- **Fase 6 (decisões do autor em 22/10/2026):** confirmação e redefinição por **código de 6
  dígitos** no e-mail (sem links); tokens **só depois** de confirmar o e-mail; e-mails de
  desenvolvimento no **Mailpit** (docker-compose, `localhost:8025`).
- Fluxo: `POST /auth/register {email}` → 202 sempre igual (e-mail novo ou não confirmado recebe
  código; já confirmado recebe aviso). `POST /auth/verify {email, code, password}` define a
  senha e devolve os tokens: a senha só é escolhida por quem recebeu o código (impede que outra
  pessoa cadastre o e-mail alheio com a própria senha). Código: 30 min, 5 tentativas, hash
  SHA-256, um novo invalida os anteriores. `POST /auth/login`, `/refresh` (rotação; reuso
  revoga a família), `/logout` (encerra a sessão; idempotente).
- Redefinir senha: `POST /auth/password-reset {email}` → 202 sempre igual (só conta confirmada
  recebe código); `POST /auth/password-reset/confirm {email, code, password}` troca a senha,
  revoga todas as sessões e abre uma nova. Códigos de cadastro e de redefinição não se misturam.
- Limitação (valores iniciais): janelas fixas em memória (um servidor). Login: 10 por (IP,
  e-mail) e 100 por IP a cada 15 min; códigos: 10 por (IP, e-mail) e 50 por IP a cada 15 min;
  pedidos que enviam e-mail: 20 por IP e 5 por e-mail por hora; renovação: 300 por IP a cada
  15 min. Excedeu → 429 com `Retry-After`. Nunca por e-mail sozinho no login (ninguém tranca a
  conta alheia). Atrás do proxy (Fase 9), o IP real virá do cabeçalho encaminhado.
- Conta (com token): `GET /account`; `GET /account/sessions` (um item por aparelho, marcando
  o atual pela claim `sid` do token de acesso); `DELETE /account/sessions/{id}`;
  `POST /account/password {currentPassword, newPassword}` (encerra os outros aparelhos);
  `DELETE /account {password}` (apaga a conta e todos os dados em cascata). Senha errada → 400;
  10 tentativas por usuário a cada 15 min.
- Senha: 12 a 128 caracteres, sem regras de composição (OWASP ASVS). Login errado, e-mail
  inexistente e conta não confirmada dão o mesmo 401; com e-mail inexistente a senha é conferida
  contra um hash falso, para o tempo de resposta não denunciar.

### Senhas

- **Argon2id** (`Argon2PasswordEncoder` + Bouncy Castle): memória 19 MiB, 2 iterações,
  paralelismo 1, salt de 16 bytes, hash de 32 bytes (mínimo recomendado pela OWASP).
- `DelegatingPasswordEncoder` (prefixo `{argon2}`); hash atualizado no próximo login
  quando os parâmetros mudarem.
- Alternativa: BCrypt com custo 12.

### Abuso e enumeração

- Limitação por (IP, e-mail) e por IP, com janelas fixas (valores na Fase 6 acima); **sem
  bloqueio de conta** (evita que terceiros tranquem contas alheias).
- Respostas uniformes em cadastro, recuperação e login.
- Tokens de e-mail de uso único, armazenados como hash; redefinição de senha válida por 30 min.

### Autorização e API

- Política explícita em todas as rotas; padrão negar.
- Verificação de posse em toda consulta (`findByIdAndOwnerId`); recurso de outro
  usuário responde **404**.
- Testes de acesso entre usuários (IDOR) obrigatórios.
- Bean Validation nos DTOs; erros no formato `ProblemDetail` (RFC 9457) sem stack trace.
- CORS desligado (sem frontend web; CORS não é autenticação).
- API REST em `/api/v1`: `PUT /{recurso}/{id}` cria ou atualiza com o UUID do aparelho
  (repetir não duplica). O corpo traz `version`, a versão que o cliente conhece (0 = novo);
  diferente da atual → 409 com `currentVersion`. Linha travada durante a gravação
  (`SELECT … FOR UPDATE`). Respostas 201 (criou) ou 200 (atualizou).
- O catálogo oficial (TACO) não vai para o servidor: já vem no app. `foods` no servidor
  guarda só alimentos do usuário; `food_id` de itens de refeição e do diário não tem chave
  estrangeira (pode ser da TACO). A lista de nutrientes (V3) é cópia de
  `data/catalog/nutrients.csv`, conferida por teste.
- Endpoints (Fase 5): `GET /goals`, `GET|PUT /goals/{id}`; `GET /foods` (ativos),
  `GET|PUT|DELETE /foods/{id}`; `GET /meals`, `GET|PUT|DELETE /meals/{id}`;
  `GET /log-entries?from&to` (até 366 dias), `GET|PUT|DELETE /log-entries/{id}`.
  `DELETE` recebe `?version=` e faz exclusão lógica; um `PUT` depois restaura (desfazer).
  O servidor guarda o snapshot enviado pelo app e não recalcula nutrientes.
- Logs de auditoria sem senhas, tokens ou dados alimentares; Dependabot; JPA parametrizado.
- Referências: OWASP ASVS nível 1 (partes do 2 em autenticação), OWASP API Security
  Top 10, OWASP MASVS-L1.

### Android

- Sem tráfego em texto puro (exceção: `10.0.2.2` apenas em debug, por
  `src/debug/res/xml/network_security_config.xml`); sem pinning de certificado no MVP.
- Rede (Fase 7): Retrofit 3.0.0 + converter-gson 3.0.0 + OkHttp 4.12.0 (a versão que o Retrofit
  3.0.0 pede). `ApiClient` tem dois clientes: um para `/auth` (sem renovação automática) e um
  para os dados, que põe o token de acesso e, no 401, renova uma vez (renovação sincronizada,
  para não gastar o mesmo refresh token duas vezes). Endereço por `BuildConfig.API_BASE_URL`
  (debug: `http://10.0.2.2:8080/`; release: definido na Fase 9). DTOs mantidos pelo R8.
- Conta no app: seção "Conta e backup" no Perfil e `AccountSheet` (e-mail → senha, ou código +
  senha nova para criar conta e para redefinir). Token de acesso só na memória; refresh token em
  `KeystoreSessionStore` (AES-GCM, chave no Android Keystore), arquivo `calju_auth` fora do
  backup e da transferência entre aparelhos.
- Refresh token cifrado com AES-GCM e chave no Android Keystore
  (`security-crypto` foi descontinuada pelo Google).
- Banco local protegido pelo sandbox do app + criptografia de arquivos do Android;
  SQLCipher como evolução futura.
- Backup: tokens e chaves excluídos; o banco pode entrar no backup do Google.
- Permissões apenas `INTERNET` e `ACCESS_NETWORK_STATE`; componentes `exported=false`
  (exceto a Activity de entrada); R8 ativo; sem logs sensíveis em release.
- Limitações: root e instrumentação podem ler dados do app; as defesas só aumentam o
  custo do ataque, e o servidor nunca confia no cliente.
- LGPD: dados alimentares podem ser considerados dados de saúde (sensíveis) —
  minimização e consentimento claro.

## 6. Identidade visual e UX

Revisada em 20/10/2026 (paleta "castanha e caju" e tela inicial v3, aprovadas pelo autor) e em
21/10/2026 (tela inicial v4, no estilo de diários como o Foodvisor: layout híbrido aprovado pelo autor)
e em 22/10/2026 (v5 "Caju conversa", com o mascote Caju: proposta A escolhida pelo autor, com as
refeições do dia em lista compacta; implementada na Fase 4e).

### Paleta "castanha e caju"

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| primary | `#6B4429` | `#E8BE9C` | Castanha: cor principal |
| primaryContainer | `#F2DFCF` | `#4A2F1C` | Destaques suaves, aba ativa |
| onPrimaryContainer | `#2A1607` | `#F2DFCF` | Texto sobre primaryContainer |
| secondary | `#C7402A` | `#FF8A70` | Caju: ações principais, anel de kcal |
| background | `#FAF6F1` | `#1A1613` | Fundo |
| surface | `#FFFFFF` | `#241E1A` | Cartões |
| surfaceContainer | `#F3EDE6` | `#2A231E` | Barra inferior, áreas secundárias |
| onSurface | `#2A211B` | `#EFE6DF` | Texto |
| onSurfaceVariant | `#6B5E55` | `#B9ABA0` | Texto secundário |
| outline | `#E6DDD4` | `#3A312B` | Divisórias |
| error | `#B3261E` | `#F2B8B5` | Erros |

Cartão de resumo (fundo castanho nos dois temas) e refeições:

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| summaryBackground | `#5E3A22` | `#3A2416` | Fundo do cartão de resumo |
| summaryTile | `#6F4A31` | `#4A3020` | Blocos dos macros, trilha do anel |
| onSummary / onSummaryVariant | `#FFF6EE` / `#E9D3C1` | `#FFF6EE` / `#D9C2B0` | Textos no resumo |
| summaryAccent | `#FF8A70` | `#FF8A70` | Anel de kcal, "Definir meta" |
| café da manhã | `#9A6200` sobre `#FCEBC8` | `#F2C46B` sobre `#4A3A1A` | Ícone da seção |
| almoço | `#C7402A` sobre `#FBDCD3` | `#FF8A70` sobre `#4A2219` | Ícone da seção |
| jantar | `#6B4429` sobre `#F2DFCF` | `#E8BE9C` sobre `#4A2F1C` | Ícone da seção |
| lanche (folha do cajueiro) | `#4C7A34` sobre `#DDEBD3` | `#A9D18E` sobre `#26361E` | Ícone da seção |

- Contraste medido (WCAG): todas as combinações de texto acima de 5:1; ícones das seções
  acima de 3:1 (mínimo para elementos gráficos).
- Macros no resumo: proteína em caju (`#FF8A70`), carboidrato em amarelo (`#F5C766`),
  gordura em creme (`#F2DFCF`), com bolinha colorida ao lado do nome; sempre com rótulo e
  número (nunca só cor).
- Ícones das refeições e da barra inferior: Material Icons (Google, Apache License 2.0).
- Roboto do sistema, escala Material 3, algarismos tabulares.
- Cartões com cantos arredondados e sombra suave; sheets com cantos de 26 dp.
- Tema escuro no MVP.
- Mascote: **Caju**, personagem original (caju com a castanha como boina), em 4 humores:
  feliz, comemorando, pensando e dormindo. Vetores próprios do app (`ic_caju_*`).
- Tom do Caju: fala curta, em primeira pessoa, sem julgamento. Passar da meta nunca é erro
  ("um dia não define nada"); sem vermelho de alerta para comida.

### Storytelling e mensagens do Caju (v5)

- Primeiro uso: duas telas (`WelcomeActivity`): "Oi! Eu sou o Caju." e "É só me contar"
  (exemplos do campo). Vistas uma vez; "Pular" disponível.
- Diário: o Caju fica no topo com um balão que muda conforme o dia (`CajuMessages`):
  dia vazio (dormindo), registros sem meta, kcal restantes, meta de proteína completa
  (comemorando), acima da meta (sem alarme), dia passado.
- Registro feito: aviso "Anotado no almoço!" com o Caju comemorando e "Desfazer".
- Busca sem resultado: "Criar alimento" com o Caju pensando.
- Fonte: Roboto do sistema; a Nunito (proposta) depende de conferir o suporte a fonte
  variável no Android e fica para depois.

### Tela inicial (Diário) — v5 "Caju conversa"

```
Hoje                                   ‹ ›   (sem barra de título no Diário)
quinta, 22 de outubro
(Caju) ┌ Faltam 1.734 kcal para a meta… ┐    balão: fala muda conforme o dia
┌ Resumo (pêssego; castanho no escuro) ──┐
│ (anel 1.734    )  Meta de hoje         │
│ (kcal restantes)  2.000 kcal           │
│                   266 consumidas       │
│ [•Proteína] [•Carboidrato] [•Gordura]  │
│        Ver micronutrientes             │
└────────────────────────────────────────┘
┌ (☕) Café da manhã         74 kcal  (+) ┐   lista compacta, um cartão só
│      Banana, prata · 75 g          74   │
│ (🍔) Almoço · Nada ainda            (+) │
│ (🍝) Jantar · Nada ainda            (+) │
│ (🥐) Lanche · Nada ainda            (+) │
└────────────────────────────────────────┘
┌ ★ Refeições prontas        2 salvas ▴ ┐   aberta por padrão
│ Marmita                    (✎) (+)     │
│ + Nova refeição                        │
└────────────────────────────────────────┘
( (Caju) Conte pro Caju o que comeu…  ↑ )  campo fixo embaixo
Diário | Histórico | Perfil
```

(Números ilustrativos, não são dados nutricionais reais.)

- **Painel animado:** ao adicionar, editar ou remover um alimento, o anel de kcal e as barras
  dos macros animam do valor anterior até o novo (respeitando a opção do sistema de reduzir
  animações).
- **Micronutrientes sob demanda:** o painel mostra kcal e macros; "Ver micronutrientes" abre
  a lista completa do dia.

### Registro de alimento (um por vez)

1. O usuário digita no campo fixo, por exemplo `arroz 150`.
2. Sugestões aparecem a cada letra, buscadas no catálogo local (offline).
3. Regras do texto (implementadas em `FoodInputParser`):
   - número depois do nome, ou com "g": gramas (`arroz 150`, `150 g de arroz`);
   - número antes do nome: unidades da porção padrão (`2 bananas`, `1,5 banana`);
   - só o nome: 1 porção padrão, se o alimento tiver uma (`banana`);
   - sem porção padrão e sem gramas: o app pergunta a quantidade.
   O peso calculado aparece antes de confirmar, sempre editável.
4. A ocasião (café, almoço, jantar, lanche) é sugerida pelo horário e pode ser trocada. O "+"
   de uma seção prepara o campo para aquela refeição ("O que você comeu no almoço?"), e ela
   vem marcada na tela de quantidade.
5. "Adicionar" registra o item; o painel anima até os novos totais; snackbar com "Desfazer".
6. Tocar num registro abre a edição (quantidade, ocasião, remoção).

Vários alimentos numa única frase e interpretação por IA ficam para depois do MVP.

### Refeições prontas

- Seção recolhível na tela inicial; aberta, lista as refeições salvas.
- **+** adiciona a refeição ao dia como está.
- **✎** abre a refeição: alterar gramas, remover ou incluir alimentos. Ações:
  "Salvar refeição" (altera o molde) e "Adicionar hoje" (registra no dia com os ajustes,
  sem alterar o molde).
- "+ Nova refeição" cria uma do zero.

### Alimentos personalizados

- Para produtos de embalagem que não estão na TACO. Entradas: "Criar alimento \"…\"" no
  cartão de sugestões (aparece sempre que há texto digitado, mesmo sem resultados) e
  "Meus alimentos" no Perfil (criar, editar, excluir com "Desfazer").
- O usuário digita a tabela nutricional como está na embalagem, **por 100 g** ou **por
  porção** (com o peso da porção); o app converte para 100 g (`NutritionLabel`, 4 casas).
- Obrigatórios: nome, valor energético, proteínas, carboidratos e gorduras totais. Fibra e
  sódio são opcionais; vazio = desconhecido, nunca zero. Marca e porção são opcionais; a
  porção vira a porção padrão do alimento (`USER`), permitindo "2 unidades" no campo.
- Validação: valores não negativos; proteínas + carboidratos + gorduras não podem passar do
  peso de referência (folga de 1 g para o arredondamento dos rótulos).
- Criado a partir do Diário, o alimento abre direto na tela de quantidade.
- Editar muda moldes de refeição (que apontam para o alimento atual), nunca registros já
  feitos (snapshot). Excluir é lógico (`deleted_at`): some da busca e da lista.
- Localmente, `foods` ganhou `brand`, `is_custom`, `updated_at`, `deleted_at`, `version` e
  `sync_state` (banco v5); `owner_user_id` fica para o servidor (Fase 5+).
- Unidade de referência só em gramas por enquanto; produtos rotulados em ml são digitados
  como se 1 ml = 1 g (limitação conhecida).

### Navegação

- Barra inferior: **Diário, Histórico, Perfil**.
- Metas: o app não sugere valores; até o usuário definir, o painel mostra kcal consumidas e o
  botão "Definir meta". Com meta, mostra kcal restantes (ou o excesso) e o anel enche.
- Perfil: metas, conta, sincronização, fontes dos dados, sobre.
- Estado da sincronização como ícone discreto (✓ / ↻ / ⊘).

### Componentes reutilizáveis

`DaySummaryCard` (anel + macros animados), `MacroCard`, `SavedMealsSection`, `LogEntryCard`
(RecyclerView + ListAdapter/DiffUtil), `FoodInputBar` (campo + sugestões),
`QuantityInputSheet`, `MealEditorSheet`, `NutrientTable` ("—" para desconhecido, "Tr" para
traço), `EmptyState`.

### Acessibilidade e responsividade

- Edge-to-edge com tratamento de `WindowInsets` (obrigatório a partir do targetSdk 35).
- targetSdk 36 ignora o travamento de orientação em telas ≥ 600 dp: layouts devem
  funcionar em paisagem e redimensionados.
- `adjustResize` para o teclado (o campo fixo sobe junto).
- Alvos de toque ≥ 48 dp, texto em `sp`, testes com fonte em 200%.
- Descrições para TalkBack (o anel anuncia "1.240 kcal restantes de 2.000").
- Animações curtas e desativadas quando o sistema pede redução de movimento.

## 7. Estrutura do repositório

```
Calju/
├── README.md · LICENSE · .gitignore · docker-compose.yml
├── docs/  (arquitetura.md, adr/, fontes-de-dados.md, licencas-terceiros.md, autoria.md)
├── android/app/src/main/java/io/github/lucaslimacelino_dev/calju/
│     CaLjuApp, AppContainer, domain/, data/local/, data/remote/, data/repository/,
│     data/sync/, ui/{diary,add,meals,history,settings,components}
├── backend/src/main/java/io/github/lucaslimacelino_dev/calju/
│     config/, common/, auth/, user/, food/, meal/, diary/, goal/, sync/
│     (cada pacote com Controller, Service, Repository, Entity, dto/)
│     resources/db/migration/V1__init.sql …
└── tools/catalog-builder/   (gera o SQLite do catálogo e o seed do Flyway a partir da TACO)
```

O backend é organizado por funcionalidade, com as camadas dentro de cada pacote.

## 8. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Licença das tabelas nutricionais | Conferir termos antes de embutir; atribuição em "Sobre" |
| Pesos de medidas caseiras inventados | Porções padrão só da tabela do IBGE, com fonte; demais definidas pelo usuário |
| Erros de arredondamento | `BigDecimal`, arredondar só na exibição, testes de casos-limite |
| Sincronização "come" o projeto | Só na Fase 7; UUID e versão no esquema desde o início |
| Autenticação feita à mão | Só componentes do Spring Security; testes de IDOR |
| Verificação de desenvolvedor do Android (vigente no Brasil desde 30/09/2026, inclusive para APK fora da Play) | Distribuição inicial por APK com conta de distribuição limitada (até 20 aparelhos); reconferir a regra na Fase 9 |
| Perda da chave de assinatura do APK | Keystore fora do Git, com cópia de segurança; sem ela as atualizações não instalam por cima |
| Requisitos da Play, se publicar depois | Revisão nas fontes oficiais antes de publicar |
| Entrega de e-mails | Provedor SMTP transacional; SPF/DKIM |
| Autoria com uso de IA | `docs/autoria.md`, commits feitos pelo autor, sem percentuais inventados |
| applicationId permanente | Definido: `io.github.lucaslimacelino_dev.calju` |

## 9. Escopo do MVP

**Entra:**

- Catálogo TACO offline e campo de registro com sugestões (um alimento por vez, peso no texto).
- Painel do dia com kcal e macros animados; micronutrientes sob demanda.
- Alimentos personalizados com porções próprias.
- Calculadora com valores desconhecidos e traços.
- Refeições salvas: criar, editar, duplicar, excluir, ver totais e adicionar ao dia com ajuste.
- Diário por data e ocasião, com edição, remoção, desfazer e totais.
- Metas de kcal e macros com vigência.
- Histórico.
- Tema claro e escuro.
- Conta completa: cadastro, verificação de e-mail, login, logout, recuperação e troca de
  senha, sessões, exclusão no app e via web.
- Sincronização com conflitos visíveis.
- Política de privacidade.

**Depois:** código de barras, metas de micronutrientes, MFA, receitas com peso cozido,
exportação CSV (v1.1), widgets, fotos, Wear OS, vários alimentos numa frase, interpretação
de texto por IA, publicação na Play Store, interface web, SQLCipher.

## 10. Sequência de implementação

| Fase | Entrega | Verificação |
|---|---|---|
| 2 (concluída) | README, .gitignore, docker-compose, backend com Flyway V1 + health, app Android com tema e 3 abas | `mvn test` verde; app abre no emulador |
| 3 (concluída) | `domain/`: modelos, NutritionCalculator, FoodInputParser, porção padrão | JUnit: 165 × 150 g = 247,5; zero, negativo, nulo, ausente, traço, somas; regras do texto |
| 4a (concluída) | Room, importação do catálogo, busca, campo de registro com sugestões, tela de quantidade | Testes de DAO e importação; app sem permissão de internet; "arroz 150" preenche 150 g |
| 4b (concluída) | Diário com snapshot, edição, remoção, desfazer, painel do dia, micronutrientes | Alterar alimento não altera registro antigo; persiste após reiniciar |
| 4c (concluída) | Refeições salvas e metas | Ajuste no registro não altera molde; duplicar gera novos UUIDs |
| 4d (concluída) | Histórico, "Salvar como refeição" e alimentos personalizados (antecipados antes do backend por decisão do autor em 21/10/2026, para o app ficar completo para uso próprio) | Histórico com totais e meta do dia; molde criado a partir de registro; alimento do usuário aparece na busca |
| 4e (concluída) | Visual "Caju conversa" e storytelling com o mascote (antecipado da Fase 8 por decisão do autor em 22/10/2026) | Testes de UI continuam passando; mensagens do Caju testadas sem Android |
| 5 (concluída) | API REST, DTOs, validação, ProblemDetail (já protegida por token; decisão do autor em 22/10/2026) | Testes de integração |
| 6 (concluída) | Spring Security, JWT, refresh, e-mail, limitação, login com Google (backend) | Testes de autenticação, IDOR e enumeração |
| 7 | Sync push/pull, conflitos, WorkManager | Reenvio não duplica; conflito não sobrescreve; queda no meio |
| 8 | Refino visual, símbolo, acessibilidade | TalkBack, fonte 200%, tema escuro |
| 9 | APK de release assinado com R8; distribuição limitada; backend de produção se a conta/sync estiver no escopo. Publicação na Play é opcional e posterior | Aparelho real; revisão de dependências |
| 10 | Documentação e pacote do INPI | Tag congelada; hash SHA-512 só dessa tag |

## 11. INPI (resumo; reconferir nas fontes oficiais na Fase 10)

- Pedido pelo sistema e-Software, com o mesmo login da GRU.
- Resumo hash (guias institucionais recomendam SHA-512) dos trechos escolhidos, gerado
  de uma tag Git congelada; o código não é enviado, mas o original deve ser preservado
  exatamente como estava.
- Declaração de Veracidade assinada com certificado ICP-Brasil qualificado, em nome do
  titular que consta na GRU.
- Valores de taxa: consultar a tabela oficial no momento do pedido.
- O registro protege a expressão do código, não a ideia abstrata. Publicar na Play
  Store não substitui o registro.
- Não exige publicação nem comercialização; recomenda-se programa "suficientemente
  finalizado". Não se envia código nem executável, só o hash e a Declaração de Veracidade.
- Validade de 50 anos a partir de 1º de janeiro do ano seguinte à publicação ou, na
  ausência desta, à criação. O registro não é obrigatório; serve como prova de autoria.

## Fontes consultadas (20/10/2026)

- Spring Boot: https://endoflife.date/spring-boot · https://versionlog.com/spring-boot/
- Target SDK da Play: https://developer.android.com/google/play/requirements/target-sdk
- INPI e-Software: https://www.gov.br/inpi/pt-br/servicos/programas-de-computador/arquivos/guia-basico/eSoftware.pdf
- INPI perguntas frequentes: https://www.gov.br/inpi/pt-br/servicos/perguntas-frequentes/programas-de-computador
- Verificação de desenvolvedor Android: https://developer.android.com/developer-verification/guides
- Compatibilidade AGP/Android Studio: https://developer.android.com/build/releases/about-agp
- IBGE, medidas referidas (POF 2008-2009): https://www.ibge.gov.br/home/estatistica/populacao/condicaodevida/pof/2008_2009_medidas/default.shtm
