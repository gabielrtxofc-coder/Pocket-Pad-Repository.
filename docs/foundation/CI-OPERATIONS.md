# Operação do CI — `.github/workflows/foundation-build.yml`

Documento operacional do pipeline de validação multiplataforma do Pocket Pad.
Ele descreve **quando** o CI roda, **o que** roda, **como** consultar resultados,
**como** executá-lo manualmente e **como** mantê-lo.

> Escrito na **Missão 03.1** (restauração do CI). O pipeline em si já estava
> funcional (provado na Missão 02); o problema era que seus gatilhos estavam
> vinculados a uma branch de sessão antiga do Arena, desligando o CI na prática.
> A correção trouxe o pipeline para a `main` e para os pull requests.

---

## 1. Resumo executivo

| Item | Valor |
|---|---|
| Workflow | `.github/workflows/foundation-build.yml` |
| Nome | `Foundation build validation` |
| Jobs | `android`, `android-instrumented`, `server-linux`, `server-windows` |
| Permissões | `contents: read` (princípio do menor privilégio) |
| Concorrência | `foundation-build-${{ github.ref }}`, `cancel-in-progress: false` |
| Plataformas | Ubuntu 24.04 (x86_64) e Windows (`windows-latest`) |
| Artefatos | APK debug, relatórios de teste, executáveis dos servidores, log de compilação Windows |

O workflow valida a **fundação oficial Mousedroid v1.5** em três plataformas
sem alterar o comportamento do aplicativo: compila o APK Android, roda os testes
unitários e instrumentados, e compila o servidor C++ no Linux e no Windows.

---

## 2. Eventos que disparam o CI

Após a correção da Missão 03.1, o workflow responde a três eventos:

| Evento | Condição | Tipos |
|---|---|---|
| `push` | apenas para a branch `main` | `created`, `updated` (default) |
| `pull_request` | PR com destino (`base`) `main` | `opened`, `synchronize`, `reopened` (default) |
| `workflow_dispatch` | qualquer branch (execução manual) | n/a |

```yaml
on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
  workflow_dispatch:
```

- **Não há filtro de `paths`.** Qualquer alteração — incluindo documentação e o
  próprio workflow — dispara o CI. Isso evita que arquivos importantes da
  compilação sejam excluídos por acidente por um filtro estrito.
- **Não há dependência de nomes de branches temporárias.** A antiga referência a
  `arena/39232774-pocket-pad-repository` foi removida dos gatilhos.
- O gatilho de `push` fica restrito à `main`. Pushes em branches de trabalho
  (incluindo a branch da sessão Arena) **não** disparam o workflow automaticamente;
  para validar uma branch em desenvolvimento, use `workflow_dispatch` (§7) ou abra
  um pull request para a `main`.

### Concorrência

```yaml
concurrency:
  group: foundation-build-${{ github.ref }}
  cancel-in-progress: false
```

Um push mais recente ao mesmo branch **não cancela** uma execução já em curso.
Isso evita invalidar uma validação que está sendo realizada.

---

## 3. Jobs executados

### Dependências

```text
android ───────────────┐
                       ├──> android-instrumented
server-linux ───────────┤
server-windows ─────────┘
```

- `android-instrumented` **depende** de `android` (`needs: android`).
- `server-linux` e `server-windows` são **independentes** entre si e de `android`.
- Os quatro jobs são construídos em paralelo, exceto o instrumentado, que espera
  o `android` concluir.

### Tabela de jobs

| Job | Runner | Tempo limite | Dependência | Função |
|---|---|---|---|---|
| `android` | `ubuntu-24.04` | default | — | Compilação + APK debug + testes unitários |
| `android-instrumented` | `ubuntu-24.04` | 30 min | `android` | Teste instrumentado em emulador API 34 |
| `server-linux` | `ubuntu-24.04` | default | — | Configurar, compilar e testar o servidor C++ no Linux |
| `server-windows` | `windows-latest` | default | — | Configurar, compilar e testar o servidor C++ no Windows |

---

## 4. Ferramentas utilizadas

### `android` e `android-instrumented` (Ubuntu 24.04)

| Componente | Versão / origem |
|---|---|
| JDK | Temurin **17** (`actions/setup-java@v4`) |
| Android SDK | `android-actions/setup-android@v4.0.4`; pacotes `platforms;android-34`, `build-tools;35.0.0`, `platform-tools` |
| Gradle | Wrapper do projeto (**8.11.1**), cache via `gradle/actions/setup-gradle@v4` |
| Emulador | `reactivecircus/android-emulator-runner@v2.38.0`, API **34**, `x86_64`, `google_apis` |
| Compilação | `./gradlew testDebugUnitTest assembleDebug --no-daemon --stacktrace` |
| Teste instrumentado | `./gradlew connectedDebugAndroidTest --no-daemon --stacktrace` |

O job grava `sdk.dir` em `client/local.properties` **apenas no runner** (o arquivo
versionado do autor não é usado), e registra as versões de ferramentas com
`::notice`.

### `server-linux` (Ubuntu 24.04)

| Componente | Versão / origem |
|---|---|
| Compilador | GCC/G++ (do runner) |
| Build | CMake + Ninja |
| Dependências (apt) | `cmake ninja-build pkg-config adb libasio-dev libgtk-3-dev libgl1-mesa-dev libglu1-mesa-dev libcurl4-openssl-dev libsecret-1-dev libnotify-dev libxtst-dev` |
| wxWidgets | **3.3.4**, compilado do tarball oficial do GitHub (o apt do Ubuntu 24.04 traz 3.2, que não corresponde à versão validada) |
| Asio | `libasio-dev` (standalone) |
| Configuração | `cmake -S server -B build/server-linux -G Ninja -DCMAKE_BUILD_TYPE=Release` |
| Compilação | `cmake --build build/server-linux --parallel 2 --verbose` |
| Testes | `ctest --test-dir build/server-linux --output-on-failure` |

### `server-windows` (windows-latest)

| Componente | Versão / origem |
|---|---|
| Compilador | MSVC via Visual Studio (`vswhere`) |
| Build | CMake (`-A x64`) |
| Dependências | `vcpkg install asio:x64-windows wxwidgets:x64-windows` |
| Toolchain | `$VCPKG_INSTALLATION_ROOT/scripts/buildsystems/vcpkg.cmake` |
| Configuração | `cmake -S server -B build/server-windows -A x64 -DCMAKE_TOOLCHAIN_FILE=...` |
| Compilação | `cmake --build build/server-windows --config Release --parallel 2 --verbose` |
| Testes | `ctest --test-dir build/server-windows -C Release --output-on-failure` |

> **Por que o job Windows faz um `git clone` manual?** O nome do repositório
> termina com um ponto (`Pocket-Pad-Repository.`), e `actions/checkout` rejeita
> esse caminho por segurança no Windows. O job clona explicitamente para
> `D:\a\mousedroid-work` usando `github.ref_name` (e `github.repository`).

---

## 5. Dependências

- **Java:** JDK 17.
- **Android:** SDK `compileSdk/targetSdk 34`, `minSdk 28`.
- **Gradle:** wrapper **8.11.1**; AGP **8.9.1**; Kotlin **2.1.0**.
- **wxWidgets:** 3.3.4 (Linux compilado no CI; Windows via vcpkg).
- **Asio:** standalone (Linux `libasio-dev`; Windows vcpkg `asio`).
- **wxWidgets** components usados: `net`, `gl`, `core`, `base`, `adv`.
- **Repositórios de pacotes:** Google Maven, Maven Central, Gradle Plugin Portal,
  GitHub Releases (wxWidgets tarball), apt (Ubuntu), vcpkg (Windows).

O job do Linux precisa de `sudo` (instalação de pacotes, `ldconfig`, KVM rules) —
disponível no runner sem senha.

---

## 6. Artefatos gerados

| Job | Artefato | Conteúdo | Comportamento |
|---|---|---|---|
| `android` | `mousedroid-debug-apk` | `client/app/build/outputs/apk/debug/app-debug.apk` | `if-no-files-found: error` |
| `android` | `android-test-reports` | `client/app/build/reports/` | `if: always()`, `ignore` |
| `android-instrumented` | `android-instrumented-test-reports` | `client/app/build/reports/androidTests/` | `if: always()`, `ignore` |
| `server-linux` | `mousedroid-linux-server` | `build/server-linux/bin/Mousedroid` | `if-no-files-found: error` |
| `server-windows` | `mousedroid-windows-server` | `build/server-windows/bin/Release/Mousedroid.exe` | `if-no-files-found: error` |
| `server-windows` | `windows-compile-log` | `windows-compile.log` | `if: always()`, `ignore` |

Os artifacts podem ser consultados na aba **Artifacts** da execução e downloads
no jobb. A política de retenção é a **default do GitHub** (90 dias para
repos públicos) e não foi personalizada nesta missão.

---

## 7. Como executar manualmente (`workflow_dispatch`)

1. No repositório, vá em **Actions**.
2. Escolha **Foundation build validation**.
3. Clique em **Run workflow**.
4. Escolha o **branch** a validar (por exemplo `main` ou a branch de trabalho).
5. Clique em **Run workflow**.

Via API (com um token com escopo `workflow`):

```bash
gh api repos/gabielrtxofc-coder/Pocket-Pad-Repository./actions/workflows/foundation-build.yml/dispatches \
  -f ref=arena/da738890-pocket-pad-repository -f inputs.example=value
```

> A execução manual **não prova** que o gatilho automático de `push`/`pull_request`
> funciona; ela apenas comprova que o pipeline roda. A prova dos gatilhos
> automáticos vem de uma push real na `main` e de um PR real (§9–§10).

---

## 8. Como interpretar falhas

- **`android` falha na configuração do Gradle/SDK** → verifique o `::notice` de
  versões de ferramentas; confirme `compileSdk 34` e `build-tools;35.0.0`.
- **`android` falha na compilação** → o erro real está no log do `gradlew`
  (`--stacktrace`); os artifacts de relatório são publicados com `if: always()`.
- **`android-instrumented` falha ao começar o emulador** → o job configura KVM
  (`99-kvm4all.rules`). Em runners sem KVM o emulador não inicia; o teste de
  aparecimento do emulator é o primeiro passo.
- **`server-linux` falha na compilação do wxWidgets** → o download do tarball
  3.3.4 vem do GitHub; conferir o link e o `sudo ldconfig`.
- **`server-windows` falha** → o job já extrai e imprime os erros
  `error C…`/`error LNK…`/`FAILED:` no `::error`, e salva `windows-compile-log`.
- **Artifact com `if-no-files-found: error` não foi criado** → o passo que
  gera o arquivo falhou; a falha real está no log do job.

Falhas em artifacts de relatório (`ignore`) não falham o job.

---

## 9. Como consultar logs

- **Actions tab:** cada execution lista os jobs com tempo e conclusão
  (`success`, `failure`, `cancelled`). Clique em um job → **Logs**.
- **URLs de execução:**
  `https://github.com/<owner>/<repo>/actions/runs/<run_id>`.
- **Logs por etapa:** abra o job → cada step tem seu próprio log expandível.
- **Metadados:** os passos "Record tool versions" emitem `::notice` visíveis no
  log e no cabeçalho da execução.
- **Artifacts:** aba **Artifacts** na execução (§6).

---

## 10. Como adicionar novos testes

1. **Android unit:** adicione a classe em
   `client/app/src/test/java/com/darusc/mousedroid/`. O job já roda
   `testDebugUnitTest` e publica o relatório.
2. **Android instrumentado:** adicione em
   `client/app/src/androidTest/java/com/darusc/mousedroid/`. O job já roda
   `connectedDebugAndroidTest` em API 34.
3. **Servidor (C++):** registre o teste no `CMakeLists.txt` via `add_test`.
   Os jobs Linux e Windows já rodam `ctest`.
4. O pipeline não precisa de mudança para rodar novos testes, desde que eles
   estejam dentro dos scopes já cobertos (`testDebugUnitTest`,
   `connectedDebugAndroidTest`, `ctest`).

---

## 11. Como manter o workflow

- **Permissões:** mantenha `contents: read` (menor privilégio). Evite
  `write`/`all` a menos que um passo exija explicitamente.
- **Gatilhos:** mantenha a política documentada em §2. Não adicione filtros de
  `paths` que excluam arquivos de build.
- **Concorrência:** mantenha `cancel-in-progress: false` para não invalidar
  execuções em curso.
- **Versionamento:** não atualize `actions/*`, JDK, SDK ou toolchains sem
  necessidade — a fundação foi validada com versões específicas (§4).
- **CI do próprio CI:** como não há filtro de `paths`, qualquer mudança no
  `foundation-build.yml` dispara o CI e valida a mudança.
- **Sempre que possível, siga o fluxo:** branch de trabalho → `workflow_dispatch`
  ou PR para `main` → o gatilho automático valida.

---

## 12. Alterações realizadas na Missão 03.1

| Alteração | Local | Justificativa |
|---|---|---|
| `on.push.branches`: `arena/39232774-…` → `main` | workflow | O gatilho estava preso a uma branch de sessão, desligando o CI |
| `on.pull_request.branches: [main]` | workflow | Ativar validação de PRs destinados à main |
| `workflow_dispatch` mantido | workflow | Execução manual sob demanda |
| Remoção do filtro `paths` | workflow | Evitar excluir por acidente arquivos importantes da compilação |
| `concurrency` explícito (`cancel-in-progress: false`) | workflow | Evitar cancelar execuções em curso |
| `permissions: contents: read` mantido | workflow | Menor privilégio |
| Documento operacional | `docs/foundation/CI-OPERATIONS.md` | Documentação exigida |

Nenhuma alteração foi feita no código Android, C++, protocolo, licença ou
dependências do aplicativo.

---

## 13. Histórico de execuções

(Atualizado após cada execução relevante.)

| Data | Tipo | Run | Commit | Resultado |
|---|---|---|---|---|
| 2026-11-09 | `workflow_dispatch` (branch de trabalho) | [#7 / 42551971029](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42551971029) | `f315942` | **sucesso** — 4/4 jobs |
| 2026-11-08 | `push` (branch antiga, antes da correção) | [#6 / 42524050240](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42524050240) | `a434774` | sucesso — 4/4 jobs |

### Detalhe da execução da Missão 03.1 (run 42551971029)

| Job | Status | Tempo |
|---|---|---|
| `android` | `completed` / `success` | — |
| `android-instrumented` | `completed` / `success` | 4m 1s |
| `server-linux` | `completed` / `success` | — |
| `server-windows` | `completed` / `success` | 13m 57s |

Testes comprovados: unidade Android `tests=1, failures=0, errors=0, skipped=0`;
instrumentado Android `tests=1, failures=0, errors=0, skipped=0`.

Artefatos publicados:

| Artefato | Tamanho |
|---|---|
| `mousedroid-debug-apk` | 4 863 488 bytes |
| `mousedroid-linux-server` | 163 233 bytes |
| `mousedroid-windows-server` | 108 397 bytes |
| `android-test-reports` | 5 826 bytes |
| `android-instrumented-test-reports` | 5 214 bytes |
| `windows-compile-log` | 2 306 bytes |

Observações (warnings não bloqueantes): ações `actions/*@v4` e `setup-java@v4`
sob Node.js 20/48 deprecados; `setup-java@v4` deprecado. Eram as versões
validadas na Missão 02 e **não** foram alteradas nesta missão.
