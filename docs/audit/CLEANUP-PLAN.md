# Plano de limpeza e adaptação — Pocket Pad

> ## ⚠️ STATUS ATUALIZADO — Missão 04 (2026-11-09)
>
> **Este plano foi parcialmente executado.** O corpo abaixo é preservado **sem alteração** como
> registro histórico do que a Missão 03 propôs. Para o que realmente aconteceu, ver
> [`../cleanup/CLEANUP-EXECUTION.md`](../cleanup/CLEANUP-EXECUTION.md).
>
> | Etapa do plano | Status na Missão 04 | Onde |
> |---|---|---|
> | **0** — corrigir gatilho do CI | ✅ já feito na Missão 03.1 | `docs/foundation/CI-OPERATIONS.md` |
> | **1** — remover `client/.gradle/` | ✅ **executada** | commit `7f55026` |
> | **2** — destrackear `local.properties` | ✅ **executada** | commit `31c181b` |
> | **3** — bits `+x` | ✅ **executada** | commit `ff09004` |
> | **4** — remover 5 drawables órfãos | ✅ **executada** (`logo.png` **não** é órfão — preservado) | commit `9cef765` |
> | **5** — testes reais **antes** de remover código | ⚠️ **não executada** — a remoção dos exemplos foi feita **sem** substituto; vira pendência **P1** | `../cleanup/OPEN-QUESTIONS.md` |
> | **6** — remover caminho de bateria | ❌ **cancelada** — `BatteryMonitor.kt` **não** é código morto (é importado e chamado). Reclassificado INVESTIGAR | `../cleanup/REMOVAL-ANALYSIS.md` MC12 |
> | **7** — `PacketType` / `Connection::Send` | ❌ **não executada** — exige editar C++, que é refatoração proibida nesta missão | `../cleanup/CLEANUP-EXECUTION.md` §4 |
> | **8** — remover `client/README.md` | ✅ **executada** | commit `7f55026` |
> | **9** — 7 correções do servidor | ❌ **não executada** — correções de comportamento, fora do escopo de limpeza. Registradas como D3–D8 | `../cleanup/CLEANUP-EXECUTION.md` §4 |
> | **10** — decidir sobre `.idea/` | ⚠️ **parcial** — 4 arquivos removidos por critério objetivo; 11 preservados; decisão final adiada | `../cleanup/REPOSITORY-HYGIENE.md` §3 |
> | **11** — remover Gson/kapt/Espresso | ❌ **não executada** — exige editar `build.gradle`, que é ADAPTAR | `../cleanup/OPEN-QUESTIONS.md` |
> | **12** — higiene de nomes | ❌ **não executada** | — |
> | **13** — atualizar dependências | ❌ **não executada** — explicitamente fora do escopo ("não atualizar bibliotecas sem necessidade") | — |
>
> ### Duas correções importantes ao plano
>
> 1. **A Etapa 6 estava errada.** O plano afirma que o caminho de bateria é "código morto comprovado".
>    Não é: `ConnectionManager.kt:7,16,25,32` e `MainActivity.kt:63` usam `BatteryMonitor` em código
>    **não comentado**. Executar a Etapa 6 teria quebrado o build.
> 2. **A Etapa 5 é pré-requisito da Etapa 4 de "testes", não das remoções de recursos.** A ordem correta
>    foi mantida para os drawables (nenhum teste necessário), mas a remoção dos testes-exemplo sem
>    substituto deixou os jobs de CI sem cobertura — registrado como pendência obrigatória **P1**.
>
> ---
>
> Missão 03. **Nada neste documento foi executado** *à época em que foi escrito*. É um plano para missões futuras.
> Princípio invariante: **em nenhum momento o repositório pode deixar de compilar** para Android, Linux e Windows, nem perder a capacidade de rodar os três modos.
> Cada etapa é independente e reversível; cada etapa termina com o CI verde como condição de aceitação.

---

## 0. Regras gerais do plano

1. **Uma etapa = um commit (ou um PR pequeno).** Nunca misturar limpeza estrutural com mudança de comportamento.
2. **CI verde é a definição de "pronto".** O workflow `.github/workflows/foundation-build.yml` precisa ser corrigido **antes** de tudo (etapa 0) para realmente rodar nas branches de trabalho.
3. **Testes antes de remoções de risco.** As remoções de código (etapas 4–6) exigem, no mínimo, que exista um teste (manual documentado ou automatizado) cobrindo o fluxo afetado.
4. **Rollback por `git revert` do commit da etapa** — por isso uma etapa por commit.
5. **Nada de `server/adb/*`, `LICENSE`, `docs/foundation/*`** nas etapas de remoção: são dependência funcional ou evidência histórica.
6. **Nenhuma atualização de dependência junto com uma remoção.** Versões (`coroutines 1.3.9`, `Material 1.5.0`, etc.) são tratadas em etapa própria, com revalidação.

---

## Prioridade ALTA — desbloqueiam o trabalho ou corrigem defeitos ativos

### Etapa 0 — Consertar o gatilho do CI (sem tocar em código)

| Campo | Conteúdo |
|---|---|
| **Arquivo/componente** | `.github/workflows/foundation-build.yml` (C04) |
| **Situação atual** | `on.push.branches: [arena/39232774-pocket-pad-repository]` (`:4-12`). Essa branch é da Missão 02; na branch de trabalho atual o workflow **só** roda por `workflow_dispatch`. |
| **Problema** | O único gate de build do projeto está efetivamente desligado para o desenvolvimento corrente. Nenhuma das etapas seguintes pode ser validada. |
| **Motivo da alteração** | Restabelecer a validação automática contínua. |
| **Dependências afetadas** | Nenhuma dependência de código. |
| **Impacto esperado** | CI roda em push de qualquer branch de trabalho e em PRs. |
| **Riscos** | Baixo. Risco de custo de minutos de CI. |
| **Testes necessários** | Um push de teste; confirmar os 4 jobs verdes. |
| **Rollback** | `git revert`. |

**Escopo sugerido:** trocar a lista fixa por `branches: ['**']` (ou pelas branches reais), adicionar `pull_request`, e atualizar ações depreciadas (`checkout@v4`, `setup-java@v4`, `setup-gradle@v4`, `upload-artifact@v4` → major atual) que emitem aviso de Node 20.

---

### Etapa 1 — Remover estado local de build versionado por engano

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `client/.gradle/buildOutputCleanup/buildOutputCleanup.lock`, `cache.properties`, `client/.gradle/config.properties`, `client/.gradle/vcs-1/gc.properties` (C06) |
| **Situação atual** | Rastreados pelo Git apesar de a regra `.gradle` existir em `client/.gitignore`. `config.properties` contém `java.home=D:\Programs\Android Studio\jbr` — caminho absoluto da máquina do autor. |
| **Problema** | Estado de máquina; zero valor funcional; polui o índice e confunde auditoria. |
| **Motivo** | Higiene do repositório e portabilidade. |
| **Dependências afetadas** | Nenhuma. O Gradle recria `.gradle/` automaticamente no primeiro build. |
| **Impacto esperado** | Repositório ~5 arquivos menor; nenhuma mudança de comportamento. |
| **Riscos** | Muito baixo. Se o `.gitignore` não for corrigido junto, os arquivos voltam no próximo build. |
| **Testes necessários** | `bash ./gradlew testDebugUnitTest assembleDebug` após `git rm --cached`. |
| **Rollback** | `git revert` + `git checkout -- client/.gradle`. |

**Pré-requisito obrigatório:** ampliar `client/.gitignore` com `local.properties` e `.idea/` (hoje ele tem 4 linhas: `build`, `app/build`, `app/release`, `.gradle`). Observação técnica: a regra `.gradle` **já existe** e ainda assim `client/.gradle/*` está rastreado, porque regras de ignore não removem arquivos já no índice — daí a necessidade de `git rm --cached` nesta etapa.

---

### Etapa 2 — Portabilidade de `local.properties`

| Campo | Conteúdo |
|---|---|
| **Arquivo afetado** | `client/local.properties` (C05) |
| **Situação atual** | `sdk.dir=C:\\Users\\Darius\\AppData\\Local\\Android\\Sdk` (`:8`), versionado. |
| **Problema** | Caminho do SDK de outra máquina. Impede build fora do ambiente do autor; já exigiu workaround no CI (`printf 'sdk.dir=%s' "$ANDROID_HOME" > local.properties`). |
| **Motivo** | Portabilidade (item de gravidade **Alta** na Missão 02, §9). |
| **Dependências afetadas** | Nenhuma dependência de código; apenas a resolução do SDK pelo Gradle/Android Studio. |
| **Impacto esperado** | Build usa `ANDROID_HOME`/`sdk.dir` local do desenvolvedor. |
| **Riscos** | Baixo. Quem já tem o arquivo local não é afetado; quem clona precisa do SDK configurado (o que já é esperado). |
| **Testes necessários** | Build em runner limpo (CI) e em máquina sem o arquivo. |
| **Rollback** | `git revert` (o arquivo volta ao índice). |

**Nota de preservação histórica:** o conteúdo original deve ser citado em `docs/foundation/` (ou no commit message) para não se perder o registro — mas o arquivo em si não deve continuar rastreado.

---

### Etapa 3 — Bits executáveis dos scripts

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `client/gradlew`, `server/install.sh`, `server/release.sh` (todos `100644`) |
| **Situação atual** | Versionados sem bit executável; o README instrui `chmod +x` manualmente (`server/README.md:63-67`). |
| **Problema** | `./gradlew` e `./install.sh` falham por permissão; o CI contorna com `bash ./gradlew`. |
| **Motivo** | Corrigir uma armadilha de onboarding. |
| **Dependências afetadas** | Nenhuma. |
| **Impacto esperado** | Scripts executáveis após clone. |
| **Riscos** | Muito baixo (`git update-index --chmod=+x`). Em Windows não tem efeito. |
| **Testes necessários** | `./gradlew --version` e `bash -n install.sh`. |
| **Rollback** | `git update-index --chmod=-x`. |

---

### Etapa 4 — Remover recursos Android comprovadamente órfãos

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `res/drawable/baseline_add_24.xml`, `res/drawable/devicelistitembackground.xml`, `res/drawable/ic_iconmonstr_check_mark_thin.xml`, `res/drawable/ic_iconmonstr_x_mark_thin.xml`, `res/drawable/ic_pause.xml` (C20); investigar `res/drawable/logo.png` |
| **Situação atual** | Nenhuma referência `@drawable/<nome>` em `res/` nem `R.drawable.<nome>` em Kotlin (verificado por varredura). |
| **Problema** | Peso morto no APK e ruído no inventário de recursos. |
| **Motivo** | Reduzir superfície e ambiguidade de manutenção. |
| **Dependências afetadas** | Nenhuma — a ausência de referência é a própria evidência. |
| **Impacto esperado** | APK marginalmente menor; nenhuma mudança visual. |
| **Riscos** | Baixo. **Não remover** recursos referenciados apenas por nome em atributos dinâmicos (o projeto não usa `getIdentifier`, então o risco é nulo). |
| **Testes necessários** | `assembleDebug` + teste instrumentado + inspeção visual das 5 telas. |
| **Rollback** | `git revert`. |

**Ação de investigação prévia:** `logo.png` (40.170 B, idêntico a `imgs/logo.png`) não aparece por id — confirmar com `grep -rn "R.drawable.logo"` e com busca por `"logo"` em `assets`/código antes de remover.

---

### Etapa 5 — Substituir os testes-exemplo por testes reais (antes de qualquer remoção de código)

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` (C22) |
| **Situação atual** | Dois testes gerados pelo Android Studio. O instrumentado valida `packageName == "com.darusc.mousedroid"` — **quebrará por design** no rebranding. |
| **Problema** | Não há cobertura de nenhuma lógica; o CI "verde" dá uma falsa sensação de segurança (1 teste trivial). |
| **Motivo** | Criar a rede de segurança mínima para as etapas seguintes. |
| **Dependências afetadas** | Jobs `android` e `android-instrumented` do CI. |
| **Impacto esperado** | Cobertura inicial de: `Translators.toSocketReport` (bytes por evento), `Translators.toHIDReport` (contagem/tamanho de relatórios), `GestureHandler` (decisão scroll vs move vs clique direito), `KeyboardInputWatcher` (DEL/ENTER/caractere). |
| **Riscos** | Médio — testes de gestos exigem instrumentação ou refatoração para injetar `MotionEvent`; preferir primeiro os testes *puros* de `Translators` (nenhuma dependência Android além de `Log`). |
| **Testes necessários** | Os próprios. |
| **Rollback** | `git revert`. |

**Ordem interna recomendada:** (a) testes de `Translators` → (b) testes de `KeyboardLayout` (char → Key) → (c) testes instrumentados de gestos → (d) só então remover os exemplos.

---

## Prioridade MÉDIA — dívida técnica que já causa defeitos ou confusão

### Etapa 6 — Removing código morto do cliente (bateria)

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `BatteryMonitor.kt`, interface `BatteryMonitor.Listener` em `ConnectionManager.kt:41-43,86-90`, `BatteryReport` em `Reports.kt:51-56`, bloco comentado de bateria em `Descriptor.kt`, `InputEvent.BatteryEvent` |
| **Situação atual** | `start()` está **comentado** (`MainActivity.kt:33`), logo o receiver nunca é registrado e o listener nunca dispara. O descriptor de bateria está comentado. No modo socket o evento vira um array vazio (`Translators.kt:205`). |
| **Problema** | Três camadas de código morto interdependentes, sugerindo uma feature que foi abandonada no meio. |
| **Motivo** | Clareza e redução de superfície. |
| **Dependências afetadas** | `ConnectionManager` (implementa `BatteryMonitor.Listener`), `InputEvent` (sealed class), `Reports.kt`. |
| **Impacto esperado** | Nenhuma mudança de comportamento observável. |
| **Riscos** | Médio se a intenção for **reimplementar** o report de bateria no Pocket Pad (é uma feature plausível para um periférico). Alternativa conservadora: manter `BatteryMonitor` e apenas documentar o estado |
| **Testes necessários** | Build + teste de que nenhum evento deixa de ser enviado (os testes da etapa 5 cobrem `Translators`). |
| **Rollback** | `git revert`. |

### Etapa 7 — Código morto do servidor (`PacketType` e `Send`)

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `server/src/net/server.h:31-40` (`PacketType` com FRAME…EFFECT), `Connection::Send` (`connection.h:41-42`, `connection.cpp:57-65`) |
| **Situação atual** | `PacketType` não é referenciado em nenhum lugar (`grep` → 0 usos fora da declaração). `Send` nunca é chamado. |
| **Problema** | Sugere um protocolo de streaming de vídeo que não existe, induzindo a erro quem lê o código. |
| **Motivo** | Precisão da documentação viva do código. |
| **Dependências afetadas** | Nenhuma. |
| **Impacto esperado** | Servidor compila igual; leitura do protocolo fica correta. |
| **Riscos** | Baixo — mas se houver intenção futura de streaming, o registro deve migrar para um documento/issue em vez de desaparecer. |
| **Testes necessários** | Build Linux + Windows no CI. |
| **Rollback** | `git revert`. |

### Etapa 8 — Remover `client/README.md` (1 byte)

| Campo | Conteúdo |
|---|---|
| **Arquivo afetado** | `client/README.md` (conteúdo: `\n`) |
| **Situação atual** | Arquivo vazio herdado. |
| **Problema** | Ruído; sugere documentação inexistente. |
| **Motivo** | Higiene. |
| **Dependências afetadas** | Nenhuma. |
| **Impacto/Riscos** | Nenhum / nenhum. |
| **Testes** | Nenhum necessário além do CI. |
| **Rollback** | `git revert`. |

### Etapa 9 — Corrigir defeitos concretos do servidor (sem refatorar)

Cada item é um commit isolado, com o mesmo formato:

| # | Defeito | Arquivo | Correção proposta | Teste |
|---|---|---|---|---|
| 9.1 | `GetFilePath()` sem `return` fora do Windows (UB) | `settingsmanager.cpp:81-88` | retornar `""` ou compilar apenas em `_WIN32` | build Linux com `-Wreturn-type` |
| 9.2 | `release.bat` gera `distconfig.ini` fora de `dist` e cria `mousedroid_win64` sem usar | `release.bat:4,17` | `dist\config.ini`; remover o `mkdir` inútil (ou usar o diretório) | executar o script em Windows |
| 9.3 | `.vscode/tasks.json` (MinGW) divergente do toolchain oficial (MSVC) | `server/.vscode/tasks.json` | alinhar ao MSVC/vcpkg ou documentar como experimental | build pelo VS Code |
| 9.4 | `PLATFORM_SRC`/`PLATFORM_LIBS` definidos duas vezes; `resource.rc` pode não entrar | `CMakeLists.txt:22-27,41-46` | unificar em um único bloco por plataforma | verificar ícone do `.exe` gerado |
| 9.5 | `UpdateView(false)` pode indexar fora do vetor | `wxdevicelist.cpp:41-43` | validar índice ou remover o caminho não usado | teste manual com 2 dispositivos |
| 9.6 | Documentação cita `install.bat` inexistente | `server/README.md:78` | corrigir o nome (`release.bat`) | revisão |
| 9.7 | `ADBOn/ADBOff` usa caminhos inconsistentes e `./adb` no Linux | `adb.hpp:33,60`; `settingsmanager.cpp:127,146` | centralizar o caminho do adb; usar `PATH` no Linux | teste manual USB em ambos OS |

**Riscos comuns:** todos tocam o caminho de build/execução do servidor → exigem CI Linux+Windows verde e, para 9.1/9.7, teste manual de USB.

### Etapa 10 — Ampliar `.gitignore` e decidir sobre `.idea/`

| Campo | Conteúdo |
|---|---|
| **Arquivos afetados** | `client/.gitignore`, `client/.idea/*` (15 arquivos, C06) |
| **Situação atual** | `.gitignore` tem 4 linhas (`build`, `app/build`, `app/release`, `.gradle`) e não cobre `local.properties` nem `.idea/`. `.idea/` inteiro (15 arquivos) está versionado, incluindo `caches/deviceStreaming.xml` (69.823 B) e caminhos absolutos (`assetWizardSettings.xml`, `misc.xml` com JDK 21). |
| **Problema** | Configuração de máquina versionada; divergências com o build (JDK 21 no IDE vs 17 no Gradle). |
| **Motivo** | Portabilidade e consistência de toolchain. |
| **Dependências afetadas** | Nenhuma de código. |
| **Impacto esperado** | Cada dev usa seu próprio `.idea`; `misc.xml`/`compiler.xml` deixam de impor JDK 21. |
| **Riscos** | Médio — algumas equipes versionam `.idea` de propósito (code style, run configurations). **Requer decisão explícita do mantenedor.** |
| **Testes necessários** | Android Studio abre o projeto sem erros; build CLI verde. |
| **Rollback** | `git revert`. |

**Alternativa conservadora (recomendada se houver mais de um dev):** manter `.idea/` versionado mas remover apenas `caches/`, `assetWizardSettings.xml`, `deploymentTargetDropDown.xml`, `deploymentTargetSelector.xml` e corrigir `misc.xml`/`compiler.xml` para JDK 17.

---

## Prioridade BAIXA — melhorias de clareza, sem urgência

### Etapa 11 — Remover dependências declaradas e não usadas

| Item | Evidência de não uso | Ação |
|---|---|---|
| `com.google.code.gson:gson:2.10.1` | `grep -rn "gson" client/app/src` → 0 ocorrências | remover de `app/build.gradle:49` |
| `kotlin-kapt` | nenhum processador de anotação, nenhum `kapt(...)` | remover de `app/build.gradle:4` |
| `androidx.test.espresso:espresso-core:3.5.1` | nenhum `import androidx.test.espresso` em `client/` (verificado) | remover **junto com a etapa 5** |

**Riscos:** baixo, mas exige build verde (a resolução de dependências pode mudar transitivamente). **Rollback:** `git revert`.

### Etapa 12 — Higiene de nomes e consistência

| Item | Situação | Ação |
|---|---|---|
| `res/drawable/ic_skip_previos.xml` | typo no nome do recurso | renomear + atualizar referências em layouts |
| `keymap.h` (2 cópias, 179 e 193 linhas) | nomes idênticos em pastas diferentes | manter (correto), documentar |
| `PacketType`, `maxPacketSize` | órfãos | já coberto nas etapas 7 e 11 |
| `imgs/*` × `client/app/src/main/res/drawable/logo.png` | duplicata (40.170 B cada) | deduplicar quando o material de marca for refeito |

### Etapa 13 — Atualização planejada de dependências (não é limpeza)

| Dependência | Atual | Natureza |
|---|---|---|
| `kotlinx-coroutines-android` | 1.3.9 (2020) | desatualizada há várias majors |
| `androidx.appcompat` | 1.4.1 | antiga |
| `androidx.core:core-ktx` | 1.8.0 | antiga |
| `com.google.android.material` | 1.5.0 | antiga (Material3 recente exige ≥1.9) |
| `androidx.constraintlayout` | 2.1.3 | antiga |
| ações do CI | v4 (Node 20) | depreciadas |

**Regra:** uma dependência por commit, CI verde, nenhuma mudança de comportamento no mesmo commit. **Não é remoção** e não deve ser feita junto com as etapas acima.

---

## Sem necessidade de alteração (identificados e justificados)

| Componente | Por quê não alterar |
|---|---|
| `LICENSE` | Restrição legal da missão. |
| `docs/foundation/BASELINE.md`, `BUILD-VALIDATION.md` | Evidência histórica primária; alterá-los destruiria a rastreabilidade. |
| `client/gradle/wrapper/*`, `gradle.properties`, `setup.gradle` (repositórios) | Corretos e validados; só o `rootProject.name` muda no rebranding. |
| `mkinput/InputEvent.kt` | Contrato central correto; apenas extensível. |
| `networking/Translators.kt` | Correto e crítico; **não deve ser "limpo"**, deve ser preservado e testado (etapa 5). |
| `networking/bluetooth/Descriptor.kt`, `Reports.kt` | Corretos; o descriptor não deve ser tocado sem revalidação em hosts reais. |
| `layouts/KeyboardLayout.kt`, `Keycode.kt`, `languages/*.kt` | Implementação limpa; nenhuma ação. |
| `input/input.h`, `inputmanager.hpp`, `linux|win32/keymap.h` | Base sólida (exceto validação de tamanho, que é correção, não limpeza). |
| `res/layout/*.xml` (exceto os de popup, que precisam de i18n) | Estrutura correta; muda por design, não por limpeza. |
| `server/adb/*` | Dependência funcional do USB no Windows; **não remover**. |
| `server/res/*`, `app.ico`, `icon.png` | Ativos referenciados por CMake/install.sh. |

---

## Sequenciamento recomendado

```text
Etapa 0  (CI)            ── bloqueia todas as outras
   │
   ├─ Etapa 1 (gradle state)   ─┐
   ├─ Etapa 2 (local.properties)├─ independentes, baixo risco
   ├─ Etapa 3 (modos +x)       ─┤
   ├─ Etapa 8 (client/README)  ─┘
   │
   ├─ Etapa 5 (testes reais)   ── habilita as remoções de código
   │      ├─ Etapa 6 (bateria)
   │      ├─ Etapa 7 (PacketType/Send)
   │      └─ Etapa 11 (deps não usadas)
   │
   ├─ Etapa 4 (drawables órfãos) ─ independente
   ├─ Etapa 9 (defeitos do servidor) ─ 7 commits isolados
   ├─ Etapa 10 (.idea) ─ requer decisão
   ├─ Etapa 12/13 (higiene e versões) ─ por último
   │
   ▼
Missão seguinte: rebranding (namespace/applicationId/marca) + design do Pocket Pad
```

---

## Matriz de risco consolidada

| Etapa | Risco de quebra | Risco de perda de funcionalidade | Reversibilidade | Precisa de teste em hardware |
|---|---|---|---|---|
| 0 | Baixo | Nenhum | Total | Não |
| 1 | Muito baixo | Nenhum | Total | Não |
| 2 | Baixo | Nenhum | Total | Não |
| 3 | Muito baixo | Nenhum | Total | Não |
| 4 | Baixo | Nenhum | Total | Não (visual) |
| 5 | Médio | Nenhum | Total | Parcial (gestos) |
| 6 | Médio | Nenhum (feature já desligada) | Total | Não |
| 7 | Baixo | Nenhum | Total | Não |
| 8 | Nulo | Nenhum | Total | Não |
| 9 | Médio | Potencial (USB, ícone) | Total | **Sim** (9.1, 9.7) |
| 10 | Baixo | Nenhum | Total | Não |
| 11 | Baixo | Nenhum | Total | Não |
| 12 | Baixo | Nenhum | Total | Não |
| 13 | **Alto** | Potencial (regressão de UI) | Total | Não (mas exige CI) |

---

## Critérios de aceitação de qualquer etapa

1. `git diff --stat` mostra **apenas** os arquivos previstos na etapa.
2. CI verde nos 4 jobs (Android unit, Android instrumentado, Server Linux, Server Windows).
3. Nenhuma alteração de protocolo, de opcode ou de formato de handshake.
4. Nenhuma alteração em `LICENSE`, `docs/foundation/`, `server/adb/`.
5. Se a etapa remover código, existe teste (ou teste manual registrado) cobrindo o fluxo afetado.
6. Commit com mensagem que cita a etapa deste plano e o ID do componente (`C06`, `C18`, …) para rastreabilidade de volta à auditoria.
