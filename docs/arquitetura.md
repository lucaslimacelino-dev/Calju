# CaLju — Arquitetura e planejamento (Fase 1)

Versão do documento: 1.0 — 20/10/2026.
Status: planejamento concluído; decisões marcadas como "pendente" aguardam aprovação.

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
| Android | Java (nível 17), minSdk 26, targetSdk 36 | targetSdk 36 exigido pela Play desde 31/08/2026 |
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
   (`ENERC_KCAL`, `PROCNT`, `CHOCDF`, `FAT`, `FASAT`, `FATRN`, `FIBTG`, `SUGAR`, `NA`,
   `K`, `CA`, `FE`, `MG`, `P`, `ZN`, …). Novo nutriente = nova linha, sem migração.
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
- Arredondamento apenas na exibição: kcal inteiro, macros 1 casa, mg inteiro.
  Somas sem arredondamento intermediário (a soma das linhas exibidas pode diferir ±1 do total).
- Fórmula: `consumido = valor_por_100 × quantidade_g ÷ 100`.
  Ex.: 165 kcal/100 g × 150 g = 247,5 kcal (exibido "248 kcal").
- Energia vem da fonte; não é recalculada a partir dos macros.

### Fontes de dados

- TACO (4ª ed., NEPA/UNICAMP) como base inicial; TBCA e USDA FoodData Central como
  complementos. **Conferir termos de uso antes de embutir qualquer tabela** (Fase 3) e
  registrar em `docs/fontes-de-dados.md`.
- Medidas caseiras não são inventadas: no MVP, porções definidas pelo usuário e
  identificadas como tal.

## 4. Offline e sincronização

- Catálogo oficial pré-instalado (SQLite gerado por `tools/catalog-builder`, carregado
  com `Room.createFromAsset`).
- **UUID gerado no aparelho**: mesmo identificador local e remoto.

| Coluna | Onde | Papel |
|---|---|---|
| `id` UUID | ambos | identidade global |
| `version` | ambos | concorrência otimista |
| `deleted_at` | ambos | exclusão como lápide (tombstone) |
| `sync_state` (PENDING/SYNCED) | Android | o que falta enviar |
| `change_seq` (bigint) | servidor | cursor do pull (não depende do relógio do celular) |

Protocolo (Fase 7):

1. **Push** de linhas `PENDING` com a `version` base; servidor aplica se coincidir,
   senão retorna conflito. UUID pertencente a outro usuário é rejeitado.
2. **Pull** por cursor (`change_seq`).
3. **Idempotência:** upsert por UUID + versão; reenvio não duplica.
4. **Conflitos nunca sobrescrevem em silêncio:** refeição → cópia "(conflito)";
   registro do diário → usuário escolhe.
5. **Exclusões** como lápides; limpeza física posterior no servidor.
6. **Falhas de rede:** WorkManager com restrição de rede e backoff; UI nunca bloqueia.

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

### Senhas

- **Argon2id** (`Argon2PasswordEncoder` + Bouncy Castle): memória 19 MiB, 2 iterações,
  paralelismo 1, salt de 16 bytes, hash de 32 bytes (mínimo recomendado pela OWASP).
- `DelegatingPasswordEncoder` (prefixo `{argon2}`); hash atualizado no próximo login
  quando os parâmetros mudarem.
- Alternativa: BCrypt com custo 12.

### Abuso e enumeração

- Limitação progressiva por (IP, e-mail) e por IP; **sem bloqueio de conta** (evita que
  terceiros tranquem contas alheias).
- Respostas uniformes em cadastro, recuperação e login.
- Tokens de e-mail de uso único, armazenados como hash; redefinição de senha válida por 30 min.

### Autorização e API

- Política explícita em todas as rotas; padrão negar.
- Verificação de posse em toda consulta (`findByIdAndOwnerId`); recurso de outro
  usuário responde **404**.
- Testes de acesso entre usuários (IDOR) obrigatórios.
- Bean Validation nos DTOs; erros no formato `ProblemDetail` (RFC 9457) sem stack trace.
- CORS desligado (sem frontend web; CORS não é autenticação).
- Logs de auditoria sem senhas, tokens ou dados alimentares; Dependabot; JPA parametrizado.
- Referências: OWASP ASVS nível 1 (partes do 2 em autenticação), OWASP API Security
  Top 10, OWASP MASVS-L1.

### Android

- Sem tráfego em texto puro (exceção: `10.0.2.2` apenas em debug); sem pinning de
  certificado no MVP.
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

### Paleta (contraste a validar na Fase 8)

| Token | Claro | Escuro |
|---|---|---|
| primary | `#1E5B3F` | `#7CC59A` |
| primaryContainer | `#E2EFE6` | `#23392C` |
| background | `#FAFAF7` | `#121513` |
| surface | `#FFFFFF` | `#1A1E1B` |
| onSurface | `#1A1D1B` | `#E6E9E6` |
| onSurfaceVariant | `#5E6661` | `#A3ABA6` |
| outline | `#E2E5E2` | `#2C322E` |
| error | `#B3261E` | `#F2B8B5` |

- Macros em tons de verde + rótulo + número (nunca só cor).
- Roboto do sistema, escala Material 3, algarismos tabulares.
- Cantos de 12 dp (cartões) e 16 dp (sheets); bordas de 1 dp no lugar de sombras pesadas.
- Tema escuro no MVP.
- Símbolo: forma geométrica monocromática inspirada no caju; sem mascote.

### Tela inicial (Diário)

```
◐ CaLju        ‹  Hoje, 20 out  ›  ⚙
1.240 kcal restantes            ← número principal
▓▓▓▓▓▓▓▓▓░░░░░  760 de 2.000
Proteína / Carbo / Gordura: consumido / meta + barra fina
Café da manhã · Almoço · Jantar · Lanches (subtotal e "+ Add" por ocasião)
[ + Adicionar ]  (FAB estendido)
Diário | Refeições | Histórico   (barra inferior)
```

(Números ilustrativos, não são dados nutricionais reais.)

### Fluxo Adicionar (3 a 4 toques)

1. "+ Adicionar"; a ocasião é sugerida pelo horário e editável.
2. Tela de busca sem abrir o teclado: Recentes e Refeições salvas; toque no campo para pesquisar.
3. Alimento → sheet com quantidade (teclado numérico), seletor g/porção, prévia ao vivo, "Adicionar".
4. Refeição salva → sheet com ingredientes editáveis no lugar; os ajustes valem só para o
   registro, salvo se o usuário marcar "atualizar refeição salva".
5. Volta ao Diário com snackbar "Adicionado ao almoço · Desfazer"; totais atualizados via LiveData.

"Salvar como refeição" fica disponível na aba Refeições e em lançamentos do diário com vários itens.

### Navegação

- Barra inferior: **Diário, Refeições, Histórico**.
- Configurações (metas, conta, sincronização, fontes, sobre) no ícone ⚙.
- Estado da sincronização como ícone discreto (✓ / ↻ / ⊘).

### Componentes reutilizáveis

`SummaryCard`, `MacroProgressRow`, `OccasionSection`, `LogItemRow`
(RecyclerView + ListAdapter/DiffUtil), `QuantityInputSheet`, `NutrientTable`
("—" para desconhecido, "Tr" para traço), `EmptyState`.

### Acessibilidade e responsividade

- Edge-to-edge com tratamento de `WindowInsets` (obrigatório a partir do targetSdk 35).
- targetSdk 36 ignora o travamento de orientação em telas ≥ 600 dp: layouts devem
  funcionar em paisagem e redimensionados.
- `adjustResize` para o teclado.
- Alvos de toque ≥ 48 dp, texto em `sp`, testes com fonte em 200%.
- Descrições para TalkBack.
- Animações apenas padrão do Material.

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
| Pesos de medidas caseiras inventados | Porções definidas pelo usuário, identificadas como tal |
| Erros de arredondamento | `BigDecimal`, arredondar só na exibição, testes de casos-limite |
| Sincronização "come" o projeto | Só na Fase 7; UUID e versão no esquema desde o início |
| Autenticação feita à mão | Só componentes do Spring Security; testes de IDOR |
| Teste fechado obrigatório da Play para contas pessoais novas | Conferir regra vigente na Fase 9; recrutar testadores cedo |
| Mudanças de requisitos da Play | Revisão nas fontes oficiais na Fase 9 |
| Entrega de e-mails | Provedor SMTP transacional; SPF/DKIM |
| Autoria com uso de IA | `docs/autoria.md`, commits feitos pelo autor, sem percentuais inventados |
| applicationId permanente | Definido: `io.github.lucaslimacelino_dev.calju` |

## 9. Escopo do MVP

**Entra:**

- Catálogo TACO offline e busca.
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
exportação CSV (v1.1), widgets, fotos, Wear OS, estimativa por IA, interface web, SQLCipher.

## 10. Sequência de implementação

| Fase | Entrega | Verificação |
|---|---|---|
| 2 | README, .gitignore, docker-compose, backend com Flyway V1 + health, app Android com tema e 3 abas | `mvn test` verde; app abre no emulador |
| 3 | `domain/`: modelos, NutritionCalculator | JUnit: 165 × 150 g = 247,5; zero, negativo, nulo, ausente, traço, somas |
| 4a | Room, catálogo pré-instalado, busca, tela de quantidade | Testes de DAO; busca em modo avião |
| 4b | Diário com snapshot, edição, remoção, desfazer | Alterar alimento não altera registro antigo; persiste após reiniciar |
| 4c | Refeições salvas e metas | Ajuste no registro não altera molde; duplicar gera novos UUIDs |
| 5 | API REST, DTOs, validação, ProblemDetail | Testes de integração |
| 6 | Spring Security, JWT, refresh, e-mail, limitação | Testes de autenticação, IDOR e enumeração |
| 7 | Sync push/pull, conflitos, WorkManager | Reenvio não duplica; conflito não sobrescreve; queda no meio |
| 8 | Refino visual, símbolo, acessibilidade | TalkBack, fonte 200%, tema escuro |
| 9 | AAB assinado (Play App Signing), R8, backend de produção, Data safety, teste fechado | Aparelho real; revisão de dependências |
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

## Fontes consultadas (20/10/2026)

- Spring Boot: https://endoflife.date/spring-boot · https://versionlog.com/spring-boot/
- Target SDK da Play: https://developer.android.com/google/play/requirements/target-sdk
- INPI e-Software: https://www.gov.br/inpi/pt-br/servicos/programas-de-computador/arquivos/guia-basico/eSoftware.pdf
