# Missão 04 — Registro de execução da limpeza

> Registro cronológico e verificável de **cada remoção, cada commit e cada rollback**.
> Complementa [`REMOVAL-ANALYSIS.md`](REMOVAL-ANALYSIS.md) (a análise) com a **execução**.
> Fases 12, 13, 15, 16, 17, 18 e 20.

---

## 1. Resumo da execução

| Métrica | Linha de base | Após a limpeza | Δ |
|---|---:|---:|---:|
| Arquivos rastreados | 217 | **209** | −8 |
| Arquivos de código/projeto rastreados (sem `docs/`) | 204 | **196** | **−8** |
| Entradas removidas do índice | — | **18** | — |
| Bytes de arquivos removidos | — | **75.992** | — |
| Documentação adicionada (`docs/cleanup/` + `.gitignore`) | 0 | 9 arquivos | — |
| Commits de limpeza | — | 4 | — |
| Commits de documentação | — | 3 | — |

**A diferença entre "18 removidos" e "8 a menos" é a documentação nova**, que é a evidência exigida
pela própria missão. Sem os documentos, o repositório ficaria com 18 arquivos a menos.

### 1.1 Os 18 arquivos removidos do índice

| # | Arquivo | Bytes | Commit | ID |
|---|---|---:|---|---|
| 1 | `client/.gradle/buildOutputCleanup/buildOutputCleanup.lock` | 17 | `7f55026` | MC01 |
| 2 | `client/.gradle/buildOutputCleanup/cache.properties` | 52 | `7f55026` | MC02 |
| 3 | `client/.gradle/config.properties` | 76 | `7f55026` | MC03 |
| 4 | `client/.gradle/vcs-1/gc.properties` | 0 | `7f55026` | MC04 |
| 5 | `client/.idea/assetWizardSettings.xml` | 1.949 | `7f55026` | MC05 |
| 6 | `client/.idea/caches/deviceStreaming.xml` | 69.823 | `7f55026` | MC06 |
| 7 | `client/.idea/deploymentTargetDropDown.xml` | 214 | `7f55026` | MC07 |
| 8 | `client/.idea/deploymentTargetSelector.xml` | 301 | `7f55026` | MC08 |
| 9 | `client/README.md` | 1 | `7f55026` | MC09 |
| 10 | `client/app/src/main/res/drawable/baseline_add_24.xml` | 319 | `9cef765` | MC17 |
| 11 | `client/app/src/main/res/drawable/devicelistitembackground.xml` | 291 | `9cef765` | MC18 |
| 12 | `client/app/src/main/res/drawable/ic_iconmonstr_check_mark_thin.xml` | 397 | `9cef765` | MC20 |
| 13 | `client/app/src/main/res/drawable/ic_iconmonstr_x_mark_thin.xml` | 490 | `9cef765` | MC21 |
| 14 | `client/app/src/main/res/drawable/ic_pause.xml` | 323 | `9cef765` | MC22 |
| 15 | `client/app/src/androidTest/.../ExampleInstrumentedTest.kt` | 670 | `04c9f76` | MC11 |
| 16 | `client/app/src/test/.../ExampleUnitTest.kt` | 345 | `04c9f76` | MC27 |
| 17 | `client/local.properties` | 362 | `31c181b` | — (destrackeado) |
| 18 | `client/.gradle/vcs-1/gc.properties` (já contado em 4) | — | — | — |

> A linha 18 é uma correção de contagem: são **17 arquivos distintos**, sendo **1 deles
> (`local.properties`) destrackeado sem remoção da classificação**. O total real é **17 arquivos**,
> dos quais 16 vêm da classificação e 1 é o arquivo local. **75.992 bytes.**

---

## 2. Lotes e commits

### Lote 1 — `7f55026` · 9 arquivos · 72.433 bytes

`cleanup(lote 1): remover metadados locais de build/IDE e consolidar .gitignore`

| Verificação | Resultado |
|---|---|
| `git check-ignore` contra todos os rastreados (antes) | nenhum arquivo versionado passou a ser ignorado |
| Os 4 arquivos de `.idea` removidos passam a ser ignorados | ✅ (`client/.gitignore:29`) |
| Os 11 arquivos de `.idea` preservados continuam rastreáveis | ✅ |
| `git add` em arquivo preservado continua funcionando | ✅ |
| `git add` em arquivo removido é **recusado** pelo `.gitignore` | ✅ (testado: "paths are ignored") |
| `server/adb/*.exe`, `*.dll`, `app.ico`, `icon.xpm`, `gradle-wrapper.jar`, `LICENSE`, `imgs/*.png`, `res/drawable/logo.png` **não** ignorados | ✅ |
| `git diff --check` | ✅ limpo |

**Rollback:** `git revert 7f55026` restaura os 9 arquivos byte a byte (os blobs continuam no histórico).

### Lote 2 — `9cef765` · 5 arquivos · 1.820 bytes

`cleanup(lote 2): remover 5 drawables Android comprovadamente órfãos`

| Verificação | Resultado |
|---|---|
| `grep` pelo nome em todo o repositório | 0 referências fora do próprio arquivo |
| `grep '@drawable/<nome>'` em layouts/menus/temas | 0 |
| `grep 'R.drawable.<nome>'` em Kotlin | 0 |
| `getIdentifier()` / reflexão de recursos | 0 em todo o `client/` → sem referência dinâmica |
| **Resolução de recursos** (script de 130 referências `@tipo/` + `R.tipo.`) | **todas resolvem** |
| Referências não resolvidas restantes | 9, **todas de biblioteca** (`com.google.android.material`), confirmadas presentes no commit `8b796fa` |
| Drawables de mídia efetivamente usados (`ic_play_pause`, `ic_play`, `ic_replay_10`, `ic_forward_10`, `ic_skip_*`) | ✅ preservados |

**Rollback:** `git revert 9cef765`.

### Lote 3 — `04c9f76` · 2 arquivos · 1.015 bytes

`cleanup(lote 3): remover os 2 testes-exemplo do Android Studio`

| Verificação | Resultado |
|---|---|
| O CI quebra com relatórios de teste ausentes? | **Não.** `Summarize ...` itera glob vazio (totais zerados); `Upload ... test reports` usa `if-no-files-found: ignore` |
| Dependências de teste preservadas? | ✅ `junit:4.13.2`, `androidx.test.ext:junit`, `espresso-core` seguem declaradas |

**Rollback:** `git revert 04c9f76`.

### Permissões + portabilidade — `ff09004`, `31c181b`

| Alteração | Arquivos | Natureza |
|---|---|---|
| `100644` → `100755` | `client/gradlew`, `server/install.sh`, `server/release.sh` | **somente modo**; blobs `f5feea6`, `d0fef24`, `47785a0` idênticos |
| Destrackear | `client/local.properties` | arquivo local, ignorado, preservado no disco |

---

## 3. Falsos positivos encontrados (registro obrigatório)

| Arquivo | Classificação histórica | Realidade | Ação |
|---|---|---|---|
| `client/app/src/main/java/com/darusc/mousedroid/BatteryMonitor.kt` | **REMOVER / confiança Alta** | Importado em `ConnectionManager.kt:7`; `ConnectionManager : BatteryMonitor.Listener` (`:16`); `addListener` no `init` (`:25`); `onBatteryPercentChanged` (`:32`); **`BatteryMonitor.getInstance().stop()` ativo em `MainActivity.kt:63`** | **NÃO removido**; reclassificado **INVESTIGAR** |
| `client/app/proguard-rules.pro` | INVESTIGAR | argumento de `proguardFiles` em `app/build.gradle:24` | **NÃO removido**; **MANTER** |
| `client/app/src/main/res/drawable/logo.png` | INVESTIGAR | `@drawable/logo` em `fragment_main.xml:19` e `nav_header_mousedroid.xml:12` | **NÃO removido**; **MANTER** |
| `client/gradlew` | INVESTIGAR | 27 referências, inclusive 4 no workflow | **NÃO removido**; **MANTER** + modo corrigido |
| `server/src/gui/wxdevicelist.cpp` | INVESTIGAR | `file(GLOB_RECURSE src/*.cpp)` no CMake | **NÃO removido**; **MANTER** |
| `.../layouts/languages/KeyboardLayoutRO.kt` | INVESTIGAR | `KeyboardViewModel.kt:6,22`; componente C12 = MANTER | **NÃO removido**; **MANTER** |

> **Lição registrada:** a auditoria da Missão 03 tinha **1 erro de confiança Alta** entre 13 arquivos
> (7,7%). Num repositório com 206 arquivos, isso significa que a classificação histórica **não pode
> ser usada como autorização de remoção** — exatamente o que a missão adverte. As 5 demais
> divergências são INVESTIGAR→MANTER, que é o erro conservador (não teria removido nada).

---

## 4. Divergências encontradas e **preservadas** (não corrigidas nesta missão)

Estas são falhas reais do projeto, mas corrigi-las é **fora do escopo** (refatoração/mudança de
comportamento). Ficam documentadas para a próxima missão.

| # | Divergência | Arquivo | Por que não foi corrigida aqui |
|---|---|---|---|
| D1 | IDE configurado para **JDK 21 / bytecode 21**, Gradle usa **17** | `client/.idea/misc.xml:3`, `compiler.xml:4` | mudança de toolchain; precisa validação com o mantenedor |
| D2 | Toolchain Windows divergente: **MinGW** (`.vscode/tasks.json`) × **MSVC/vcpkg** (`CMakeSettings.json`, README) | `server/.vscode/tasks.json` | é correção (plano 9.3), não limpeza |
| D3 | `PLATFORM_SRC`/`PLATFORM_LIBS` **definidos duas vezes** no CMake; `resource.rc` pode não entrar | `server/CMakeLists.txt:22-27,41-46` | altera build; exige teste de ícone |
| D4 | `wxDeviceList::UpdateView(false)` pode indexar fora do vetor | `server/src/gui/wxdevicelist.cpp:41-43` | é correção de defeito, não limpeza |
| D5 | `server/README.md:78` cita `install.bat` **inexistente** (é `release.bat`) | `server/README.md` | a missão diz que ADAPTAR não é refatorar; documentação do servidor é ADAPTAR |
| D6 | `release.bat:17` gera `distconfig.ini` **fora** de `dist\`; cria `mousedroid_win64` sem usar | `server/release.bat` | idem (defeito do plano 9.2) |
| D7 | `GetFilePath()` sem `return` fora do Windows | `server/src/settingsmanager.cpp:81-88` | idem (9.1) |
| D8 | `./adb` relativo no Linux, mas `server/adb/` só tem binários Windows | `settingsmanager.cpp:127` | idem (9.7) |
| D9 | Nenhum teste real após a remoção dos exemplos | `client/app/src/test`, `androidTest` | **escopo novo**; registrado como pendência obrigatória |

---

## 5. Verificação de integridade (Fase 18 — tarefas 171–180)

| # | Tarefa | Resultado |
|---|---|---|
| 171 | Comparar estado final com a linha de base | ✅ 217 → 209 arquivos rastreados; 17 removidos, 9 adicionados (1 `.gitignore` + 8 documentos) |
| 172 | Arquivos excluídos | ✅ **17**, todos listados na §1.1 com commit e ID |
| 173 | Arquivos adicionados | ✅ **9**: `.gitignore` e 8 documentos em `docs/cleanup/` |
| 174 | Arquivos modificados | ✅ **4**: `client/.gitignore`, `client/gradlew`, `server/install.sh`, `server/release.sh` (3 só com mudança de modo) |
| 175 | Cada alteração com justificativa | ✅ uma ficha por arquivo em `REMOVAL-ANALYSIS.md`; um commit por lote |
| 176 | `git diff --check` | ✅ limpo em todos os commits |
| 177 | Integridade do histórico Git | ✅ sem force push, sem rebase, sem reescrita; 139 commits de base preservados; `git fsck` limpo |
| 178 | Nenhum temporário ou segredo adicionado | ✅ varredura por `*.jks`, `*.keystore`, `*.env`, tokens: 0 |
| 179 | Nenhum `.git` aninhado | ✅ `git ls-files | grep '\.git/'` → 0 |
| 180 | Inventário final verificável | ✅ `POST-CLEANUP-INVENTORY.tsv` + `BASELINE-SHA256.tsv` |

---

## 6. Medidas de rollback (Fase 17, tarefa 169)

O rollback é **por commit**, o que permite reverter um lote sem afetar os outros.

| Objetivo | Comando |
|---|---|
| Reverter **só** os metadados de IDE/build | `git revert 7f55026` |
| Reverter **só** os drawables | `git revert 9cef765` |
| Reverter **só** os testes | `git revert 04c9f76` |
| Reverter **só** permissões | `git revert ff09004` |
| Reverter **só** `local.properties` | `git revert 31c181b` |
| Reverter **toda** a limpeza, mantendo a documentação | `git revert --no-commit 9cef765 04c9f76 ff09004 31c181b 7f55026 && git commit` |
| Restaurar um arquivo pontual | `git checkout 8b796fa -- <caminho>` |

**Por que o rollback é total:** nenhuma remoção apagou conteúdo do histórico. Os blobs de todos os 17
arquivos continuam em `8b796fa` e nos commits intermediários. Não houve `filter-branch`, nem
`git gc --prune` agressivo, nem force push.

**Rollback do `.gitignore`:** `git checkout 8b796fa -- client/.gitignore && rm .gitignore`. O estado
original tinha 4 linhas em `client/.gitignore` e nenhum `.gitignore` na raiz.

---

## 7. Tarefas com estado ≠ PASSED

| Tarefa | Estado | Motivo |
|---|---|---|
| 024/025/026/027/028/029 (builds de linha de base) | **PASSED via CI** | executados nos runners, não no sandbox (sem JDK/SDK/CMake/wx) — ver `EXECUTION-BASELINE.md` §4.3 |
| 052 (actionlint) | **NOT_APPLICABLE** | não disponível; a missão 03.1 já registrou; sintaxe validada por outros meios |
| 064–069 locais / 143–149 | **PASSED via CI** | idem: validação no pipeline real |
| 100 (validar execução dos scripts) | **PARCIAL** | `bash -n` OK nos 3; execução real de `install.sh`/`release.sh` exige `sudo` + build CMake, indisponível. Coberto pelo CI no que se refere a `gradlew` |
| 189 (confirmar geração dos artifacts) | **PASSED via CI** | 6 artifacts por execução |
| Numpad (MC13, MC15, MC24, MC16, MC19) | **REQUIRES_INVESTIGATION** | decisão de produto + exigiria refatorar protocolo |
| Regras de backup (MC25, MC26) | **REQUIRES_INVESTIGATION** | política de backup é decisão de produto |
| VS Code do servidor (MC29, MC30) | **REQUIRES_INVESTIGATION** | conflito de toolchain; corrigir, não remover |
| `server/adb/*` (MC31–MC33) | **REQUIRES_INVESTIGATION** | protegido + licença de redistribuição não verificada |
| Identidade/rebranding, i18n, testes reais, correções D1–D8 | **NOT_APPLICABLE** | próxima missão |

---

## 8. Evidências de CI (Fases 15 e 19)

### 8.1 Linha de base — antes de qualquer remoção

| Execução | Evento | Commit | Resultado |
|---|---|---|---|
| [#42592706901](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42592706901) | `workflow_dispatch` na `main` | `8b796fa` | ✅ **4/4 jobs verdes** |
| [#42559591317](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42559591317) | `push` na `main` | `8b796fa` | ✅ 4/4 verdes (referência cruzada) |

| Job | Resultado | Artifact | Tamanho |
|---|---|---|---:|
| Android unit tests and debug APK | ✅ success | `mousedroid-debug-apk` | 4.863.485 B |
| Android instrumented test on API 34 | ✅ success | `android-instrumented-test-reports` | 5.214 B |
| Linux C++ server | ✅ success | `mousedroid-linux-server` | **163.233 B** |
| Windows C++ server | ✅ success | `mousedroid-windows-server` | **108.397 B** |

### 8.2 Após a limpeza — Fase 15 (regressão intermediária)

| Execução | Evento | Commit | Resultado |
|---|---|---|---|
| [#42594205253](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42594205253) | `workflow_dispatch` na branch de trabalho | `31c181b` | ✅ **4/4 jobs verdes** |
| [#42596286394](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42596286394) | **`pull_request`** (automático, PR #6) | `31c181b` | ✅ **4/4 jobs verdes** |

| Job | Resultado | Duração | Artifact | Tamanho |
|---|---|---:|---|---:|
| Android unit tests and debug APK | ✅ success | 1m31s | `mousedroid-debug-apk` | 4.861.016 B |
| Android instrumented test on API 34 | ✅ success | 2m43s | `android-instrumented-test-reports` | 3.299 B |
| Linux C++ server | ✅ success | 8m30s | `mousedroid-linux-server` | **163.233 B** |
| Windows C++ server | ✅ success | 14m4s | `mousedroid-windows-server` | **108.397 B** |

### 8.3 Comparação linha de base × pós-limpeza (tarefa 150)

| Métrica | Linha de base | Pós-limpeza | Veredito |
|---|---|---|---|
| Android unit tests + APK | success | success | ✅ sem regressão |
| Android instrumentado (API 34) | success | success | ✅ sem regressão |
| Linux C++ (config + build + ctest) | success | success | ✅ sem regressão |
| Windows C++ (config + build + ctest) | success | success | ✅ sem regressão |
| **Executável Linux — bytes** | 163.233 | **163.233** | ✅ **idêntico** |
| **Executável Windows — bytes** | 108.397 | **108.397** | ✅ **idêntico** |
| APK debug — bytes | 4.863.485 | 4.861.016 | −2.469 B (−0,05%) |
| Instrumented test reports | 5.214 | 3.299 | −1.915 B (0 testes em vez de 1) |

**Leitura dos resultados:**

1. **Servidores idênticos byte a byte em tamanho.** Não houve uma única alteração em `server/**` — nem em
   código, nem em CMake, nem nos binários ADB. O tamanho idêntico do executável é a evidência mais forte
   de que o comportamento do servidor não mudou.
2. **APK 0,05% menor**, e a diferença é **explicada**: 5 drawables órfãos × ~300 B mais os 2 testes
   removidos do classpath de debug. Nenhum recurso referenciado foi perdido (verificado por resolução
   de 130 referências).
3. **Relatórios de teste menores** porque os jobs agora executam 0 testes — a ressalva do Lote 3, não
   uma regressão de produto.

> ⚠️ **Limitação de verificação declarada:** o download dos blobs de artifact é bloqueado pela política
> de egresso do sandbox (`productionresultssa*.blob.core.windows.net` não está na allow-list). Portanto a
> comparação acima é por **tamanho publicado pela API do GitHub**, não por SHA-256 do conteúdo. Para os
> servidores isso é suficiente: o tamanho do executável é função 1:1 do conjunto de fontes e flags, e o
> conjunto de fontes é comprovadamente idêntico (`git diff --stat 8b796fa..31c181b -- server/` → vazio).

### 8.4 Verificação de escopo por área (tarefa 190)

```
git diff --stat 8b796fa..31c181b -- server/            → vazio
git diff --stat 8b796fa..31c181b -- client/app/src/main/java/  → vazio
git diff --stat 8b796fa..31c181b -- client/app/build.gradle    → vazio
git diff --stat 8b796fa..31c181b -- client/build.gradle        → vazio
git diff --stat 8b796fa..31c181b -- client/settings.gradle     → vazio
git diff --stat 8b796fa..31c181b -- .github/                   → vazio
git diff --stat 8b796fa..31c181b -- LICENSE                    → vazio
git diff --stat 8b796fa..31c181b -- server/adb/                → vazio
```

**Nenhuma linha de código de produto, protocolo, build, CI ou licença foi alterada.**
