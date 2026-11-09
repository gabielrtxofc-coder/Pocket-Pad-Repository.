# Missão 04 — Linha de base de execução

> Documento da **Fase 01** (tarefas 001–010) e da **Fase 03** (tarefas 021–030).
> Registra o estado do repositório **antes de qualquer alteração** desta missão.
> Nenhum arquivo foi removido ou modificado quando este documento foi produzido.

---

## 1. Identificação (tarefas 001–006)

| # | Tarefa | Resultado | Evidência |
|---|---|---|---|
| 001 | Identidade do repositório | ✅ `gabielrtxofc-coder/Pocket-Pad-Repository.` | `gh repo view`, `git remote -v` |
| 002 | Remote `origin` | ✅ `https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository..git` (fetch e push) | `git remote -v` |
| 003 | Branch principal | ✅ `main` (`origin/HEAD -> origin/main`) | `git branch -a` |
| 004 | Referências remotas atualizadas | ✅ `git fetch --all --prune` sem divergências | saída do comando |
| 005 | HEAD da `main` | ✅ `8b796fa4f20693896b2260a5df02f4fdcdd6963f` | `git rev-parse main` == `git rev-parse origin/main` |
| 006 | Integração da Missão 03.1 presente | ✅ `docs/foundation/CI-OPERATIONS.md` + `docs/foundation/MISSION-03.1-REPORT.md`; workflow com gatilhos em `main` | `git ls-files docs/foundation/` |

### 1.1 Ancestralidade do HEAD conhecido

O HEAD informado na missão (`8b796fa…`) **coincide exatamente** com `main` e `origin/main`:

```
main        = 8b796fa4f20693896b2260a5df02f4fdcdd6963f
origin/main = 8b796fa4f20693896b2260a5df02f4fdcdd6963f
```

Não há commits em `main` além do esperado. O histórico contém as integrações das Missões 03 (PR #2)
e 03.1 (PRs #4 e #5), todas por **merge commit**.

### 1.2 Estado do worktree (tarefas 007–009)

| # | Tarefa | Resultado |
|---|---|---|
| 007 | Estado do worktree | ✅ limpo — `git status --porcelain` vazio |
| 008 | Arquivos modificados antes do trabalho | ✅ **nenhum** (0 arquivos modificados, 0 staged, 0 untracked) |
| 009 | Branch isolada da Missão 04 | ✅ `arena/c39918ee-pocket-pad-repository` (sessão nova, criada da `main`) |

> A branch de trabalho é `arena/c39918ee-pocket-pad-repository`, criada a partir de `main` em
> `8b796fa`. Ela é **distinta** das branches das sessões anteriores
> (`arena/45d35856-…` e `arena/da738890-…`), conforme exigido pela seção 4 da missão.

### 1.3 Divergência de ambiente encontrada e corrigida

O clone de trabalho inicial era **shallow** (`git rev-list --count HEAD` → `1`). A Missão 03 já havia
registrado esse mesmo problema. Corrigido com `git fetch --unshallow`:

```
antes:  1 commit    (shallow)
depois: 139 commits (histórico completo, .git/shallow ausente)
```

Sem essa correção, as subtarefas de **histórico Git por arquivo** (item 3 de cada subtarefa dinâmica)
seriam impossíveis. **O histórico foi apenas baixado; nada foi reescrito.**

---

## 2. Fotografia do repositório na linha de base

| Métrica | Valor |
|---|---|
| Arquivos rastreados | **217** |
| Commits no histórico | **139** |
| Tamanho da árvore (soma dos arquivos rastreados) | **7.248.314 bytes (6,91 MB)** |
| Tamanho de `.git` | 3,4 MB |
| Hash da árvore (`HEAD^{tree}`) | `3bfe7898fb8d78edb30719becbef5d2f25b25479` |
| Manifesto SHA-256 de todos os arquivos | `/tmp/baseline-sha256.txt` (217 linhas) |
| Hash do manifesto agregado | `a65e325d4c2e678970068f02c95b550d560d35b6e8835da25d3649d6dae14b9b` |
| Modos de arquivo | **217 × `100644`** — nenhum bit executável no índice |

O manifesto completo por arquivo está em [`BASELINE-SHA256.tsv`](BASELINE-SHA256.tsv)
(217 linhas, `sha256`, `path`).

### 2.1 Composição por área

| Área | Arquivos | Bytes | % do total |
|---|---:|---:|---:|
| `client/` (Android) | 167 | 617.038 | 8,5% |
| `server/adb/` (binários) | 3 | 6.098.248 | **84,1%** |
| `server/` (sem `adb/`) | 25 | 180.286 | 2,5% |
| `docs/` | 13 | 216.118 | 3,0% |
| `imgs/` + `fastlane/` | 8 | 152.394 | 2,1% |
| Raiz + `.github/` | 3 | 15.127 | 0,2% |
| **Total** | **217** | **7.248.314** | 100% |

> Nota: 84,1% do peso do repositório está em `server/adb/`, componente **INVESTIGAR** e
> expressamente **protegido** nesta missão. A limpeza não tem como objetivo reduzir esse número.

### 2.2 Blobs duplicados (candidatos de deduplicação, **não** removidos nesta missão)

| SHA-1 do blob | Arquivos |
|---|---|
| `11a7261…` | `client/app/src/main/res/drawable/logo.png` · `imgs/logo.png` |
| `67820c5…` | `client/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` · `…/ic_launcher_round.xml` |
| `e2074a5…` | `imgs/icon.png` · `server/icon.png` |

Todos são **MANTER/ADAPTAR**, portanto nenhum foi removido. O registro serve à missão de rebranding.

---

## 3. Reconciliação da auditoria (Fase 02)

### 3.1 Como os **13 arquivos REMOVER** foram obtidos

A cifra "13" da Missão 03 tem duas granularidades, e misturá-las produziria um erro grave:

| Fonte | Natureza | Quantidade |
|---|---|---|
| `CLASSIFICATION.tsv` | **3 componentes** classificados REMOVER (C06, C18, C22) | 3 |
| `FILE-INVENTORY.tsv` (coluna `classification`) | **13 arquivos** classificados REMOVER | 13 |

Os 13 arquivos vêm de `FILE-INVENTORY.tsv` e a soma por componente é coerente:

| Componente | Arquivos REMOVER | Quais |
|---|---:|---|
| C06 — Metadados locais de build e IDE | **5** | 4 × `client/.gradle/*` + `client/README.md` |
| C18 — Bateria | **1** | `BatteryMonitor.kt` |
| C22 — Testes | **2** | `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` |
| C20 — Tema, cores e recursos visuais | **5** | 5 drawables órfãos |
| **Total** | **13** | — |

### 3.1.1 Achado crítico: classificação de componente ≠ classificação de arquivo

A auditoria registra **duas** classificações, e elas discordam em **74 dos 206 arquivos**:

| Classificação | `FILE-INVENTORY.tsv` (por arquivo) | `CLASSIFICATION.tsv` (por componente, recontado por arquivo) |
|---|---:|---:|
| MANTER | 71 | 21 |
| ADAPTAR | 101 | 148 |
| REMOVER | **13** | **23** |
| INVESTIGAR | 21 | 14 |
| Total | 206 | 206 |

A fonte **autoritativa** é a **por arquivo** (`FILE-INVENTORY.tsv`), porque é exatamente ela que
reproduz a cifra declarada na missão (71 / 101 / 13 / 21) e porque é a que descreve o estado real de
cada arquivo. A coluna `aggregate_file_classification` de `CLASSIFICATION.tsv`, somada componente a
componente, produz 71 / 101 / 13 / 21 — ela é correta; o que é incorreto é **aplicar a etiqueta do
componente a cada um de seus arquivos**.

Casos concretos que provam o perigo de trabalhar por componente:

| Componente | Etiqueta do componente | Composição real em arquivos |
|---|---|---|
| **C06** (metadados de IDE/build) | REMOVER | MANTER=8, ADAPTAR=3, **REMOVER=5**, INVESTIGAR=4 |
| **C20** (tema, cores, recursos) | ADAPTAR | 53 arquivos, dos quais **5 são REMOVER** (drawables órfãos) |
| **C11** (Numpad) | INVESTIGAR | MANTER=1, INVESTIGAR=5 |
| **C22** (testes) | REMOVER | REMOVER=2 |

**Consequências operacionais adotadas:**

1. Interpretar "C06 = REMOVER" como "apague `client/.idea/` e `client/.gradle/`" destruiria **15
   arquivos preservados** (8 MANTER + 3 ADAPTAR + 4 INVESTIGAR). **Não será feito.**
2. Interpretar "C20 = ADAPTAR" como "nenhum arquivo de C20 pode ser removido" impediria a remoção
   dos 5 drawables órfãos, que **são** REMOVER por arquivo. **Serão removidos.**
3. Interpretar "13 REMOVER" como "13 componentes" subestimaria o trabalho; como "23 arquivos" (soma
   da coluna agregada por componente) superestimaria. O número correto é **13 arquivos**.

Por isso esta missão trabalha **arquivo por arquivo, nunca pasta por pasta**. A coluna
`component_vs_file` do inventário reconciliado marca `DIFFERS` nesses 74 casos.

### 3.2 Fotografia × estrutura atual

| Verificação | Resultado |
|---|---|
| Arquivos da fotografia de 206 ainda existentes | ✅ **206/206** — nenhum desapareceu |
| Arquivos da fotografia já removidos em commits posteriores | ✅ **nenhum** |
| Arquivos criados **após** a Missão 03 | **11** |
| Arquivos rastreados hoje | 206 + 11 = **217** |
| Cobertura da classificação | 206/206, cada arquivo em exatamente 1 componente |

**Os 11 arquivos criados após a auditoria** (todos documentação; nenhum classificado, nenhum candidato):

| Arquivo | Commit de criação | Componente lógico |
|---|---|---|
| `docs/audit/ARCHITECTURE.md` | `be00798` | C01 estendido |
| `docs/audit/CLASSIFICATION.tsv` | `1aa29b6` | C01 estendido |
| `docs/audit/CLEANUP-PLAN.md` | `6b581f6` | C01 estendido |
| `docs/audit/DEPENDENCIES.md` | `775cca3` | C01 estendido |
| `docs/audit/FILE-INVENTORY.tsv` | `17c4ef1` | C01 estendido |
| `docs/audit/FOUNDATION-VERIFICATION.md` | `d7f9664` | C01 estendido |
| `docs/audit/OPEN-QUESTIONS.md` | `f11a261` | C01 estendido |
| `docs/audit/PROTOCOLS.md` | `b21d3ce` | C01 estendido |
| `docs/audit/README.md` | `8b0fc33` | C01 estendido |
| `docs/foundation/CI-OPERATIONS.md` | `f315942` | C01 estendido |
| `docs/foundation/MISSION-03.1-REPORT.md` | `08e31ed` | C01 estendido |

> `docs/foundation/BASELINE.md` e `docs/foundation/BUILD-VALIDATION.md` são **anteriores** à
> fotografia (constam em C01 com 206 arquivos), portanto não entram nos 11.

O inventário reconciliado está em [`RECONCILED-INVENTORY.tsv`](RECONCILED-INVENTORY.tsv).

### 3.3 Documentos da auditoria lidos integralmente (tarefas 011–017)

| # | Documento | Situação |
|---|---|---|
| 011 | `CLEANUP-PLAN.md` | ✅ lido (14 etapas, matriz de risco, critérios de aceitação) |
| 012 | `CLASSIFICATION.tsv` | ✅ lido (36 componentes, 206 arquivos, contagens validadas por script) |
| 013 | `FILE-INVENTORY.tsv` | ✅ lido (206 linhas, 13 colunas, 13 REMOVER extraídos) |
| 014 | `DEPENDENCIES.md` | ✅ lido |
| 015 | `ARCHITECTURE.md` | ✅ lido |
| 016 | `PROTOCOLS.md` | ✅ lido |
| 017 | `OPEN-QUESTIONS.md` | ✅ lido |

---

## 4. Linha de base de compilação (Fase 03)

### 4.1 Worktree e integridade estática (tarefas 021–023)

| # | Tarefa | Resultado |
|---|---|---|
| 021 | `git status` limpo | ✅ `nothing to commit, working tree clean` |
| 022 | `git diff --check` | ✅ sem saída (nenhum whitespace error) |
| 023 | Workflow multiplataforma presente | ✅ `.github/workflows/foundation-build.yml`, 4 jobs |

### 4.2 Execução de referência do CI (tarefas 024–030)

A linha de base foi executada **de verdade** no pipeline oficial, via `workflow_dispatch` na `main`
(commit `8b796fa`), e não apenas inferida:

| # | Tarefa | Job do CI | Resultado |
|---|---|---|---|
| 024 | Workflow manual de referência | run **#42592706901** (`workflow_dispatch`, ref `main`, sha `8b796fa`) | ✅ disparado |
| 025 | Compilação Android | `Android unit tests and debug APK` (`assembleDebug`) | ✅ sucesso |
| 026 | Testes unitários Android | `testDebugUnitTest` (mesmo job) | ✅ sucesso |
| 027 | Testes instrumentados Android | `Android instrumented test on API 34` (emulador) | ✅ sucesso |
| 028 | Compilação Linux | `Linux C++ server` (wxWidgets 3.3.4 + Asio + ctest) | ✅ sucesso |
| 029 | Compilação Windows | `Windows C++ server` (MSVC + vcpkg + ctest) | ✅ sucesso |
| 030 | Hashes, artifacts e resultados | artifacts `mousedroid-debug-apk`, `mousedroid-linux-server`, `mousedroid-windows-server`, `android-test-reports`, `android-instrumented-test-reports`, `windows-compile-log` | ✅ registrados |

**Referência cruzada:** a execução `push` em `main` imediatamente anterior (`42559591317`, commit
`8b796fa`, 13m33s) também concluiu **sucesso** nos 4 jobs — o que confirma a linha de base de forma
independente e cobre a possibilidade de o `workflow_dispatch` se comportar de forma diferente.

### 4.3 Limitação de ambiente declarada (honestidade de escopo)

Este sandbox **não** possui JDK, Android SDK, Gradle, CMake, wxWidgets nem GTK, e o acesso à rede é
restrito a `github.com`, `codeload.github.com`, `api.github.com`, `registry.npmjs.org`, `pypi.org` e
`files.pythonhosted.org`. Consequências, já enfrentadas pela Missão 03.1:

| Verificação | Onde é executada |
|---|---|
| Compilação e testes Android | **Somente** nos runners GitHub Actions |
| Compilação e testes do servidor Linux | **Somente** nos runners GitHub Actions |
| Compilação e testes do servidor Windows | **Somente** nos runners GitHub Actions |
| Análise estática, dependências, referências, permissões, `.gitignore` | ✅ executada localmente, com evidência em cada documento |

**Não haverá nenhuma afirmação de "build aprovado" baseada apenas em raciocínio.** Cada aprovação de
build cita o número da execução do CI. As verificações locais cobrem tudo o que é verificável
localmente (referências cruzadas, ordenação de dependências, sintaxe de shell, YAML, XML, padrões de
`.gitignore`), e são registradas como tal.

**Regra de segurança adotada:** como a compilação não pode ser executada no sandbox, toda remoção
que possa afetar a compilação é decidida **antes** de sua execução, e a validação posterior é feita no
CI. Remoções de alto risco de build só são feitas se houver evidência estática conclusiva e
reversibilidade por `git revert`.

---

## 5. Resumo do estado inicial

| Item | Valor |
|---|---|
| Repositório | `gabielrtxofc-coder/Pocket-Pad-Repository.` |
| Branch de trabalho | `arena/c39918ee-pocket-pad-repository` |
| Commit de base | `8b796fa4f20693896b2260a5df02f4fdcdd6963f` |
| Arquivos rastreados | 217 |
| Arquivos classificados como REMOVER | 13 (em 4 componentes) |
| Componentes INVESTIGAR (protegidos) | 21 arquivos em 4 componentes |
| Candidatos a remoção somados | 13 + 21 = **34 arquivos** analisados individualmente |
| Worktree antes do trabalho | limpo |
| Alterações feitas nesta fase | apenas este documento e os dois inventários |

**Nenhuma alteração destrutiva foi executada até o fim desta fase.**
