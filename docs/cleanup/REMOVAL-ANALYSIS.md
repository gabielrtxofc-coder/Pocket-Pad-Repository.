# Missão 04 — Análise individual dos 34 candidatos

> **Fases 04, 05, 06 e 11.** Um registro por arquivo, com as 14 subtarefas dinâmicas obrigatórias.
> **Nenhuma remoção foi executada ainda.** O veredito de cada arquivo está na coluna `decision`
> do arquivo [`REMOVAL-CANDIDATES.tsv`](REMOVAL-CANDIDATES.tsv) e é explicado individualmente abaixo.
> Cada arquivo recebeu sua própria análise; não há conclusão agrupada.

## 0. Como ler esta análise

Identificadores: `MC01`…`MC34` (ver [`REMOVAL-CANDIDATES.tsv`](REMOVAL-CANDIDATES.tsv)).

Cada ficha executa as 14 subtarefas obrigatórias do §5 da missão:

| Nº | Subtarefa | Como foi executada |
|---|---|---|
| 1 | Identificação | caminho, bytes, SHA-256, componente, classificação |
| 2 | Inspeção do conteúdo | leitura integral do arquivo (todos os 34 foram lidos; o único não legível integralmente é binário, inspecionado por cabeçalho/tamanho/assinatura) |
| 3 | Histórico Git | `git log --diff-filter=A` e `git log -- <path>` (commit de criação, data, nº de commits) |
| 4 | Dependências | arestas de entrada e saída |
| 5 | Referências no código | `grep` pelo basename e por símbolo em `client/` e `server/` |
| 6 | Referências nos scripts | `install.sh`, `release.sh`, `release.bat`, `gradlew` |
| 7 | Referências nos builds | `app/build.gradle`, `build.gradle`, `settings.gradle`, `CMakeLists.txt`, `.github/workflows/` |
| 8 | Impacto na execução | o que deixa de funcionar se o arquivo desaparecer |
| 9 | Justificativa | por que remover (ou por que **não** remover) |
| 10 | Plano de rollback | como voltar (todas as remoções são por `git revert` do commit do lote) |
| 11 | Remoção autorizada | `SIM` / `NÃO` / `ADIADA` |
| 12 | Verificação de integridade | qual verificação prova que nada quebrou |
| 13 | Teste relacionado | qual teste cobre (ou por que nenhum teste é necessário) |
| 14 | Registro documental | onde o resultado foi registrado |

### 0.1 Legenda de veredito

| Veredito | Significado |
|---|---|
| **REMOVER** | Evidência conclusiva de que a remoção não altera o comportamento do produto e não quebra o build. Autorizado. |
| **NÃO REMOVER** | A investigação demonstrou que o arquivo **tem** consumidor ou valor funcional. Reclassificado como MANTER ou INVESTIGAR. |
| **ADIAR** | Sem evidência conclusiva. Permanece INVESTIGAR. Não é removido nesta missão. |

### 0.2 Resultado consolidado

| Veredito | Arquivos | Bytes |
|---|---:|---:|
| **REMOVER** | **16** | **75.268** |
| **NÃO REMOVER** (reclassificados) | **9** | **6.159.711** |
| **ADIAR** (permanecem INVESTIGAR) | **9** | **11.301** |
| Total analisado | 34 | 6.246.280 |

> **Achado central desta missão:** dos 13 arquivos classificados REMOVER pela auditoria, **12 são
> confirmados** e **1 é refutado** (`BatteryMonitor.kt`, MC12). Em compensação, **4 arquivos
> INVESTIGAR passaram a ter remoção comprovada** (MC05, MC06, MC07, MC08 — metadados locais de IDE;
> `logo.png`/MC23 foi investigado e **não** é órfão). O saldo de arquivos removidos é **16**, maior
> que os 13 históricos, mas
> **por um motivo diferente**: 12 vêm do REMOVER histórico e **4** são metadados de IDE que a
> auditoria havia deixado em INVESTIGAR por falta de decisão do mantenedor, agora decidíveis por
> evidência. **Nenhum arquivo do conjunto protegido (`server/adb/`, LICENSE, C++, Gradle Wrapper,
> Bluetooth, USB/ADB, protocolo) aparece na lista de remoções.**

---

# PARTE A — Os 13 arquivos REMOVER históricos

## MC01 — `client/.gradle/buildOutputCleanup/buildOutputCleanup.lock`

| Subtarefa | Evidência |
|---|---|
| 1. Identificação | 17 bytes · SHA-256 `bf8b8bae907951f6…` · C06 · REMOVER/Alta |
| 2. Conteúdo | binário de lock de 17 bytes (`\x03M-F5M-\x87t…`). É um **file lock** do Gradle. Não é texto, não tem dado de projeto |
| 3. Histórico | criado em `ee60cc4` (2024-05-11, "Fixed random right click and mouse move"); **11 commits** o tocaram — todos por regeneração mecânica do Gradle |
| 4. Dependências | nenhuma aresta de entrada ou saída. Nenhum código lê este arquivo; o Gradle **recria** o lock a cada build |
| 5. Código | 0 referências em `client/` ou `server/` |
| 6. Scripts | 0 |
| 7. Build | 0. Não está em `build.gradle`, `settings.gradle` nem no workflow |
| 8. Execução | nenhum. Lock é adquirido em tempo de build na máquina local; versioná-lo não tem efeito |
| 9. Justificativa | estado de máquina, sem valor funcional; um lock versionado é ativamente prejudicial (pode sugerir build em andamento) |
| 10. Rollback | `git revert` do commit do Lote 1 |
| 11. Autorização | ✅ **REMOVER** |
| 12. Integridade | `git ls-files client/.gradle` deve ficar vazio; `git check-ignore` deve casar `.gradle` |
| 13. Teste | build Android no CI (o Gradle recria o diretório) |
| 14. Registro | `REMOVAL-CANDIDATES.tsv`, `CLEANUP-EXECUTION.md` Lote 1 |

## MC02 — `client/.gradle/buildOutputCleanup/cache.properties`

| Subtarefa | Evidência |
|---|---|
| 1 | 52 bytes · `d77bfc3dbb1b8c29…` · C06 · REMOVER/Alta |
| 2 | Conteúdo integral: `#Fri Feb 20 23:04:40 EET 2026` + `gradle.version=8.11.1`. Cache de versão usado para decidir se o `buildOutputCleanup` pode ser reaproveitado |
| 3 | criado em `ee60cc4`; apenas **2 commits** |
| 4 | nenhuma |
| 5–7 | 0 referências em código, scripts e build |
| 8 | nenhum. Se ausente, o Gradle assume cache inválido e o recria — comportamento seguro |
| 9 | metadado de máquina. Note que `gradle.version=8.11.1` já está declarado de forma **autoritativa** em `client/gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`) |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | `git ls-files client/.gradle` vazio |
| 13 | build Android no CI |
| 14 | idem |

## MC03 — `client/.gradle/config.properties`

| Subtarefa | Evidência |
|---|---|
| 1 | 76 bytes · `cba1b3289bc5604d…` · C06 · REMOVER/Alta |
| 2 | `#Fri Jul 18 19:01:15 EEST 2025` + `java.home=D\:\\Programs\\Android Studio\\jbr` — **caminho absoluto do Windows da máquina do autor** |
| 3 | criado em `ee60cc4`; 2 commits |
| 4 | nenhuma |
| 5–7 | 0 referências |
| 8 | nenhum no build CLI validado. O CI declara seu próprio JDK via `actions/setup-java@v4` (temurin 17) |
| 9 | **duplo motivo**: metadado de máquina **e** caminho pessoal absoluto. É exatamente a categoria "referências obsoletas ao ambiente original do autor" (objetivo 6 da missão). Publicar caminho pessoal viola a diretriz da Fase 09 |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | `grep -rn 'Programs\\\\Android Studio' client/` = 0 após a remoção |
| 13 | build Android no CI |
| 14 | idem; conteúdo original preservado neste documento para rastreabilidade |

## MC04 — `client/.gradle/vcs-1/gc.properties`

| Subtarefa | Evidência |
|---|---|
| 1 | 0 bytes · SHA-256 `e3b0c44298fc1c14…` (= hash do arquivo vazio) · C06 · REMOVER/Alta |
| 2 | arquivo **vazio** |
| 3 | criado em `ee60cc4`; 1 commit |
| 4–7 | nenhuma dependência, 0 referências |
| 8 | nenhum |
| 9 | arquivo vazio de controle de GC do cache do Gradle. Zero informação, zero função |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | diretório `client/.gradle/vcs-1/` deixa de existir no índice |
| 13 | build Android no CI |
| 14 | idem |

## MC09 — `client/README.md`

| Subtarefa | Evidência |
|---|---|
| 1 | 1 byte · `01ba4719c80b6fe9…` · C06 · REMOVER/Alta |
| 2 | conteúdo integral: um único `\n`. **README vazio** |
| 3 | criado em `9d48483` (2023-04-14, "Create README.md"); 1 commit, nunca alterado |
| 4 | 0 dependências de entrada |
| 5 | 0 referências em código |
| 6 | 0 nos scripts |
| 7 | 0 no build |
| 8 | nenhum. Não é `LICENSE`, não é `server/README.md` (que **é** preservado, C36) e não é o `README.md` da raiz (C02, preservado) |
| 9 | ruído: sugere documentação de cliente que não existe. O README real da raiz cobre o projeto |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | `test -f client/README.md` → falso; `test -f README.md` → verdadeiro (raiz intacta) |
| 13 | nenhum teste necessário (nenhum build lê README) |
| 14 | idem |

> ⚠️ **Cuidado executado:** a busca por "README.md" retorna **43** ocorrências — todas em
> `docs/audit/*` e `docs/cleanup/*`, referindo-se ao README da **raiz** e ao `server/README.md`.
> A verificação foi feita por **caminho completo** (`client/README.md`), não por basename.

## MC11 — `client/app/src/androidTest/java/com/darusc/mousedroid/ExampleInstrumentedTest.kt`

| Subtarefa | Evidência |
|---|---|
| 1 | 670 bytes · `f19bb359273d71ba…` · C22 · REMOVER/Alta |
| 2 | Teste-exemplo do Android Studio. Assertiva única: `assertEquals("com.darusc.mousedroid", appContext.packageName)`. Além do valor trivial, o **`package` declarado é `com.example.mousedroid`** (linha 1), divergente do diretório — resíduo do rebranding anterior |
| 3 | criado em `74318b5` (2025-07-20, "Changed package name to com.darusc.mousedroid"); 1 commit |
| 4 | depende de `androidx.test.platform`, `androidx.test.ext:junit` (ambos via `androidTestImplementation`) |
| 5 | nenhum código referencia este teste (testes não são referenciados) |
| 6–7 | executado pelo job `android-instrumented` (`connectedDebugAndroidTest`). **Nenhuma configuração lista o arquivo pelo nome** — a descoberta é por convenção (`src/androidTest/`) |
| 8 | **remove a única asserção instrumentada existente** |
| 9 | a asserção `packageName == "com.darusc.mousedroid"` **quebra por design** no rebranding para Pocket Pad: é uma armadilha que falha no CI exatamente quando a missão de identidade renomear `applicationId`. O teste não cobre nenhuma lógica do produto. **Justificativa: remoção de armadilha de rebranding, não de cobertura** |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** — ver ressalva abaixo |
| 12 | `connectedDebugAndroidTest` deve continuar verde (o job passa a executar **0** testes instrumentados) |
| 13 | o próprio job de CI |
| 14 | idem + `OPEN-QUESTIONS.md` Q21 |

> ⚠️ **Ressalva registrada (não bloqueante):** remover os dois exemplos (MC11 e MC27) deixa o CI sem
> **nenhum** teste executado — o job `android-instrumented` fica verde sem executar nada, o que é uma
> **falsa sensação de segurança** já apontada pela auditoria (C22, risco R8). O [`CLEANUP-PLAN.md`](../audit/CLEANUP-PLAN.md)
> Etapa 5 condiciona a remoção à criação de testes reais. **Decisão desta missão:** a missão 04 é
> explicitamente de limpeza e **proíbe implementar funcionalidades novas**; escrever testes de
> `Translators`/`GestureHandler` seria escopo novo. A remoção é feita, e a **criação de testes reais é
> registrada como pendência obrigatória** em `OPEN-QUESTIONS.md` e no relatório final. A alternativa
> (manter um teste que valida `2 + 2`) não protege nada e ainda mantém uma armadilha de rebranding.

## MC12 — `client/app/src/main/java/com/darusc/mousedroid/BatteryMonitor.kt` ⛔ **REFUTADO**

| Subtarefa | Evidência |
|---|---|
| 1 | 2.017 bytes · `489759af49f36a3e…` · C18 · REMOVER/Alta (**confiança declarada: Alta**) |
| 2 | `class BatteryMonitor` com `BroadcastReceiver` de `ACTION_BATTERY_CHANGED`, singleton, `getBatteryLevel()`, `interface Listener { onBatteryPercentChanged(percentage: Int) }`, `start()`, `stop()` |
| 3 | criado em `dda0c9d` (2026-02-24, "bl: Battery monitor"); 2 commits |
| 4 | **dependência de entrada real:** `ConnectionManager` declara `: Connection.Listener, BatteryMonitor.Listener` (`ConnectionManager.kt:16`), implementa `override fun onBatteryPercentChanged(percentage: Int)` (`:32`) e chama `BatteryMonitor.getInstance().addListener(this)` no `init` (`:25`) |
| 5 | **8 referências de código ativas**, não apenas o `start()` comentado:<br>• `MainActivity.kt:63` → `BatteryMonitor.getInstance().stop(applicationContext)` no `onDestroy()` (código **não comentado**)<br>• `ConnectionManager.kt:7,16,25,32,88`<br>• `Translators.kt:7,146-147` (`is InputEvent.BatteryEvent -> arrayOf(BatteryReport(...))`)<br>• `Translators.kt:205` (`is InputEvent.BatteryEvent -> socketReport()`)<br>• `InputEvent.kt:36` (`data class BatteryEvent(val percentage: Int)`)<br>• `Reports.kt:51` (`class BatteryReport`) |
| 6–7 | nenhum script/build referencia |
| 8 | **se o arquivo for apagado, a compilação Kotlin falha** — `ConnectionManager.kt:7` (`import com.darusc.mousedroid.BatteryMonitor`) e `MainActivity.kt:63` deixam de resolver. Não é "código morto": é **código compilado e chamado** |
| 9 | A auditoria inferiu "código morto" a partir do `start()` **comentado** em `MainActivity.kt:33` — inferência inválida por dois motivos: (a) `start()` comentado significa apenas que o **receiver não é registrado**, não que a **classe** seja inútil; (b) `stop()` é chamado de forma **não comentada**, e a interface `Listener` é implementada por `ConnectionManager`. Removê-la exigiria editar **6 arquivos** (`MainActivity`, `ConnectionManager`, `InputEvent`, `Translators`, `Reports`, e o próprio `BatteryMonitor`) — o que é **refatoração**, expressamente proibida nesta missão ("ADAPTAR: não refatorar"). Além disso, bateria é feature plausível para um periférico, já sinalizado pela auditoria como risco |
| 10 | n/a (não removido) |
| 11 | ❌ **NÃO REMOVER** — reclassificado de REMOVER para **INVESTIGAR** |
| 12 | verificação de compile permanece verde porque **nada mudou** |
| 13 | build Android no CI |
| 14 | registrado em `CLEANUP-EXECUTION.md` §Falsos positivos e em `OPEN-QUESTIONS.md` |

> **Este é o achado mais importante da Missão 04.** A classificação histórica de 13 arquivos REMOVER
> contém **um falso positivo de confiança declarada Alta**. Remover os 13 "porque a auditoria disse"
> teria **quebrado a compilação Android**. A missão autoriza explicitamente reavaliar e reclassificar.

## MC27 — `client/app/src/test/java/com/darusc/mousedroid/ExampleUnitTest.kt`

| Subtarefa | Evidência |
|---|---|
| 1 | 345 bytes · `fa3e7c0b2516e52d…` · C22 · REMOVER/Alta |
| 2 | `assertEquals(4, 2 + 2)` — teste-exemplo gerado pelo Android Studio |
| 3 | criado em `74318b5` (2025-07-20); 1 commit |
| 4 | depende só de `junit:junit:4.13.2` (mantido, pois outros testes futuros usarão) |
| 5–7 | executado por `testDebugUnitTest`; não é referenciado por nome em nenhum lugar |
| 8 | remove a **única** asserção unitária |
| 9 | não valida nenhuma linha do produto. Mesma ressalva de MC11 quanto a deixar o job sem testes |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** (com a ressalva registrada em MC11) |
| 12 | `testDebugUnitTest` continua verde |
| 13 | o próprio job de CI |
| 14 | idem |

## MC17 — `client/app/src/main/res/drawable/baseline_add_24.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 319 bytes · `65e3f85abf9a9340…` · C20 · **REMOVER/Alta** (componente C20 é ADAPTAR — arquivo REMOVER) |
| 2 | vector drawable: ícone Material "add" (path de cruz) |
| 3 | criado em `ab9b2f5` (2025-07-20, "Connection UI/UX redesign"); 1 commit |
| 4–7 | **0 referências** por `@drawable/baseline_add_24` em layouts/menus/temas, e **0** `R.drawable.baseline_add_24` em Kotlin |
| 8 | nenhum — se o recurso não é referenciado, não entra no APK |
| 9 | órfão comprovado. O projeto **não usa** `getIdentifier()` nem reflexão para recursos (`grep -rn getIdentifier` → 0), portanto "referência por string dinâmica" está descartada |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | `aapt`/build resolve todos os `@drawable/`; CI verde |
| 13 | `assembleDebug` no CI |
| 14 | idem |

## MC18 — `client/app/src/main/res/drawable/devicelistitembackground.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 291 bytes · `57d5aa4d97ad735b…` · C20 · REMOVER/Alta |
| 2 | `<shape>` com `<corners radius>` e `<solid color="@color/…">` — fundo arredondado de item de lista |
| 3 | criado em `88f9a92` (2023-04-14, "Initial commit"); 1 commit |
| 4–7 | **0 referências**. O layout `devicelistitem.xml` usa `@drawable/deviceremovepopupbackground`/`item_shape`, não este |
| 8 | nenhum |
| 9 | substituído durante o redesign de UI e nunca removido; órfão comprovado |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12–14 | idem MC17 |

## MC20 — `client/app/src/main/res/drawable/ic_iconmonstr_check_mark_thin.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 397 bytes · `f28170d98cacbcda…` · C20 · REMOVER/REMOVER |
| 2 | vector drawable "check mark" (tema iconmonstr) |
| 3 | criado em `88f9a92` (2023-04-14); 1 commit |
| 4–7 | **0 referências** em qualquer arquivo de `client/` |
| 8 | nenhum |
| 9 | órfão. Ícone de terceiros (iconmonstr) sem atribuição versionada; removê-lo também **reduz risco de licença de ícone** |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12–14 | idem MC17 |

## MC21 — `client/app/src/main/res/drawable/ic_iconmonstr_x_mark_thin.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 490 bytes · `1ca7de341decee6d…` · C20 · REMOVER/Alta |
| 2 | vector drawable "x mark" (tema iconmonstr) |
| 3 | criado em `88f9a92` (2023-04-14); 1 commit |
| 4–7 | **0 referências** |
| 8 | nenhum |
| 9 | órfão, mesmo par de MC20 |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12–14 | idem MC17 |

## MC22 — `client/app/src/main/res/drawable/ic_pause.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 323 bytes · `bee33484260a8a02…` · C20 · REMOVER/Alta |
| 2 | vector drawable "pause" (duas barras) |
| 3 | criado em `86b9c02` (2026-02-22, "ui: Redesigned touchpad fragment"); 1 commit |
| 4–7 | **0 referências**. O cluster de controles de mídia usa `ic_play_pause.xml` e `ic_play.xml`, não `ic_pause.xml` |
| 8 | nenhum |
| 9 | órfão. O irmão `ic_play_pause.xml` é usado e **não é tocado** |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12–14 | idem MC17 |

---

# PARTE B — Os 21 arquivos INVESTIGAR

## B.1 Metadados de IDE com caminho/estado de máquina → **REMOVER** (4 arquivos)

A auditoria deixou estes quatro em INVESTIGAR com confiança **Baixa/Média/Alta** declarando
explicitamente "não foi inspecionado em profundidade" e "requer decisão do mantenedor". A decisão
passa a ser possível porque a missão 04 instituiu uma regra objetiva de aceitação para a pasta
`client/.idea/`: **um arquivo de IDE só permanece versionado se (a) não contiver caminho/estado de
máquina e (b) servir a um consumidor real do repositório**. Aplicando a regra aos 15 arquivos de
`client/.idea/`, **10 são preservados** e **5 são removidos**.

### MC05 — `client/.idea/assetWizardSettings.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 1.949 bytes · `39bd2656e7f82660…` · C06 · INVESTIGAR/Alta → veredito **REMOVER** |
| 2 | `WizardSettings` com memória do **assistente de ícones vetoriais**: `"url" value="file:/$USER_HOME$/AppData/Local/Android/Sdk/icons/material/materialicons/bluetooth/baseline_bluetooth_24.xml"` **e** `"sourceFile" value="D:\Projects\Mousedroid\client"` |
| 3 | criado em `86baa40` (2026-02-20, "bl: Upgraded AGP and dependencies"); 3 commits |
| 4–7 | 0 referências. Nenhum build, script ou código lê `assetWizardSettings.xml`; é lido **apenas** pela GUI do Android Studio |
| 8 | **nenhum no build**. No máximo, o assistente de ícones do IDE não relembra a última escolha — preferência pessoal, não configuração de projeto |
| 9 | **dois caminhos absolutos de máquina** (`$USER_HOME$` do autor e `D:\Projects\Mousedroid\client`). É exatamente o que a Fase 08/09 manda eliminar. Não é configuração compartilhável: é histórico de diálogo do IDE |
| 10 | `git revert` (restaura o arquivo byte a byte) |
| 11 | ✅ **REMOVER** |
| 12 | `grep -rn 'Projects\\\\Mousedroid' client/` → 0 após remoção (a única origem é este arquivo) |
| 13 | build Android no CI (o IDE não participa do CI) |
| 14 | `REMOVAL-CANDIDATES.tsv` + este documento |

### MC06 — `client/.idea/caches/deviceStreaming.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 69.823 bytes · `79149ba7f8e68be2…` · C06 · INVESTIGAR/Média → veredito **REMOVER**. **Maior arquivo de texto do repositório** |
| 2 | `component name="DeviceStreaming"` com `<option name="deviceSelectionList">` e dezenas de `<PersistentDeviceSelectionData>` com `api`, `brand`, `codename`, `id`, `labId`, `manufacturer`, `name`, `screenDensity` (ex.: `Sony Xperia 10`, `api 34`) |
| 3 | criado em `86baa40` (2026-02-20); 3 commits |
| 4–7 | 0 referências em código, scripts ou build |
| 8 | **nenhum**. É o **catálogo de dispositivos remotos** que o Android Studio baixa do Google para permitir selecionar um dispositivo "streaming". O IDE **rebaixa** o catálogo quando necessário; ele é específico de versão do plugin, não do projeto |
| 9 | É um **cache** (`caches/`), contém inventário de hardware de terceiros, e é o maior arquivo textual do repositório (~70 KB de ruído puro). Justificativa mais forte de remoção da pasta `.idea` inteira, isoladamente. **Não confundir com configuração**: nada aqui descreve o Pocket Pad |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | `du` de `client/.idea` cai ~70 KB; `git ls-files client/.idea` passa de 15 para 10 arquivos |
| 13 | build Android no CI |
| 14 | idem |

### MC07 — `client/.idea/deploymentTargetDropDown.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 214 bytes · `8826065e38857915…` · C06 · INVESTIGAR/**Baixa** → veredito **REMOVER** |
| 2 | Conteúdo integral: `component name="deploymentTargetDropDown"` → `entry key="app"` → `<State />` (vazio) |
| 3 | criado em `ee60cc4` (2024-05-11); 1 commit |
| 4–7 | 0 referências |
| 8 | nenhum. `<State />` vazio = "nenhum dispositivo selecionado". Sem informação alguma |
| 9 | metadado de sessão de IDE, vazio. A própria auditoria marcou confiança **Baixa** e nunca o inspecionou. Inspecionado agora: **não contém nada** |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | arquivo ausente; IDE o recria |
| 13 | build Android no CI |
| 14 | idem |

### MC08 — `client/.idea/deploymentTargetSelector.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 301 bytes · `3501593d4e83c06a…` · C06 · INVESTIGAR/**Baixa** → veredito **REMOVER** |
| 2 | `component name="deploymentTargetSelector"` → `SelectionState runConfigName="app"` → `selectionMode = DROPDOWN` |
| 3 | criado em `ab9b2f5` (2025-07-20); 1 commit |
| 4–7 | 0 referências |
| 8 | nenhum. Reflete apenas "o seletor de destino estava em modo dropdown" para a run configuration `app` |
| 9 | preferência de UI do IDE. Nenhum caminho absoluto, mas também nenhum valor de projeto. Par direto do MC07 |
| 10 | `git revert` |
| 11 | ✅ **REMOVER** |
| 12 | arquivo ausente |
| 13 | build Android no CI |
| 14 | idem |

### B.1.1 Os 10 arquivos de `client/.idea/` **preservados** e por quê

| Arquivo | Bytes | Contém caminho/estado de máquina? | Valor de projeto | Decisão |
|---|---:|---|---|---|
| `.gitignore` | 47 | não | evita reincidência de `shelf/` e `workspace.xml` | **MANTER** |
| `.name` | 10 | não (`Mousedroid`) | nome do projeto no IDE; muda no rebranding | **MANTER** (ADAPTAR no rebranding) |
| `AndroidProjectSystem.xml` | 212 | não | declara provider Gradle | **MANTER** |
| `compiler.xml` | 169 | não (`target=21`) | **divergente** do `jvmTarget 17` do Gradle | **MANTER + reportar divergência** (correção é rebranding/toolchain, fora do escopo) |
| `gradle.xml` | 690 | não (usa `$PROJECT_DIR$`) | módulos do projeto, `gradleJvm=#GRADLE_LOCAL_JAVA_HOME` | **MANTER** |
| `kotlinc.xml` | 175 | não (`2.1.0`, igual ao build) | versão do plugin Kotlin | **MANTER** |
| `migrations.xml` | 254 | não | marca migração já aplicada | **MANTER** |
| `misc.xml` | 409 | **não** (`jbr-21` é nome de SDK do IDE, não caminho absoluto) | tipo de projeto, JDK do projeto | **MANTER + reportar divergência JDK 21 vs 17** |
| `navEditor.xml` | 2.940 | não | posições do editor de navegação | **MANTER** (baixo valor, mas inofensivo e sem dado de máquina) |
| `runConfigurations.xml` | 964 | não | run configurations | **MANTER** |
| `vcs.xml` | 183 | não (usa `$PROJECT_DIR$`) | mapeamento de VCS | **MANTER** |

> **Por que não remover `.idea/` inteira?** Porque a Fase 08 proíbe ("Não remova todas as
> configurações de IDE indiscriminadamente") e porque 10 dos 15 arquivos cumprem a regra objetiva.
> O caminho de remover tudo está documentado como alternativa em `CLEANUP-PLAN.md` Etapa 10 e fica
> para a decisão do mantenedor na missão de rebranding — quando `.name`, `misc.xml`, `compiler.xml` e
> `kotlinc.xml` precisarão ser editados de qualquer forma.

## B.2 Código e configuração de build → **NÃO REMOVER** (4 arquivos)

### MC10 — `client/app/proguard-rules.pro` → **NÃO REMOVER**

| Subtarefa | Evidência |
|---|---|
| 1 | 750 bytes · `56fd960cf225f6ca…` · C05 · INVESTIGAR/Média |
| 2 | 100% comentários (template do Android Studio). Nenhuma regra ativa |
| 3 | criado em `88f9a92` (2023-04-14, "Initial commit"); 1 commit |
| 4 | **dependência de saída real:** `client/app/build.gradle:24` → `proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'` |
| 5–7 | referenciado pelo **build**. É literalmente um argumento do Gradle na configuração `release` |
| 8 | hoje `minifyEnabled false`, então o arquivo não tem efeito prático. **Mas** se for removido, a configuração `release` aponta para um arquivo inexistente — o AGP trata `proguardFiles` ausente como erro de configuração em `minifyEnabled true` futuro, e a remoção exige **editar `build.gradle`**, que é ADAPTAR e está fora do escopo |
| 9 | não é órfão. É ponto de extensão documentado e referenciado. `OPEN-QUESTIONS.md` Q22 já registra a dúvida; a resposta é "tem consumidor" |
| 10 | n/a |
| 11 | ❌ **NÃO REMOVER** — reclassificado para **MANTER** (condicionalmente ADAPTAR no rebranding) |
| 12 | n/a |
| 13 | n/a |
| 14 | `OPEN-QUESTIONS.md` Q22 respondida |

### MC28 — `client/gradlew` → **NÃO REMOVER** (corrigir permissão)

| Subtarefa | Evidência |
|---|---|
| 1 | 8.762 bytes · `a3648413b47ef77a…` · C05 · INVESTIGAR/Alta · modo **`100644`** |
| 2 | script wrapper POSIX `sh` oficial do Gradle (cabeçalho "Copyright © 2015-2021 the original authors"), 33 entradas no `.jar` do wrapper |
| 3 | criado em `88f9a92` (2023-04-14); 2 commits |
| 4 | depende de `client/gradle/wrapper/gradle-wrapper.jar` (presente) e de `gradle-wrapper.properties` → `gradle-8.11.1-bin.zip` |
| 5–7 | **27 referências**, incluindo 4 no workflow (`bash ./gradlew --version`, `bash ./gradlew testDebugUnitTest assembleDebug`, `bash ./gradlew connectedDebugAndroidTest`) |
| 8 | **é o único caminho de build Android**. Removê-lo quebra Android por completo |
| 9 | não é candidato a remoção. O que está errado é o **modo**: `100644` em vez de `100755`, o que obriga todos os call sites (README, CI) a usar `bash ./gradlew` e torna `./gradlew` falho após o clone |
| 10 | n/a |
| 11 | ❌ **NÃO REMOVER** — reclassificado para **MANTER**; **corrigir modo** (Fase 10) |
| 12 | `git ls-files -s client/gradlew` deve retornar `100755` |
| 13 | `bash -n client/gradlew` + build Android no CI |
| 14 | `CLEANUP-EXECUTION.md` §Permissões |

### MC23 — `client/app/src/main/res/drawable/logo.png` → **NÃO REMOVER**

| Subtarefa | Evidência |
|---|---|
| 1 | 40.170 bytes · `efce358ef3de5c0a…` · C20 · INVESTIGAR/Média |
| 2 | PNG. **Blob idêntico a `imgs/logo.png`** (SHA-1 `11a7261933cc18e9…`) |
| 3 | criado em `ab9b2f5` (2025-07-20, "Connection UI/UX redesign"); 1 commit |
| 4–7 | **2 referências de layout ativas:** `res/layout/fragment_main.xml:19` → `android:src="@drawable/logo"` e `res/layout/nav_header_mousedroid.xml:12` → `android:src="@drawable/logo"` |
| 8 | **remover quebra o build** (`@drawable/logo` sem definição) **e** remove o logo das telas inicial e do cabeçalho do drawer |
| 9 | A auditoria suspeitou de órfão por não encontrar `R.drawable.logo`. A busca correta é por `@drawable/logo` em XML, que **encontra 2 usos**. É duplicata de `imgs/logo.png`, mas a deduplicação exige decidir qual das duas cópias é a canônica — o que pertence ao rebranding, não à limpeza |
| 10 | n/a |
| 11 | ❌ **NÃO REMOVER** — reclassificado para **MANTER** (deduplicar no rebranding) |
| 12 | build Android no CI inalterado |
| 13 | teste visual das telas inicial e drawer |
| 14 | `OPEN-QUESTIONS.md` (deduplicação), `CLEANUP-PLAN.md` Etapa 12 |

### MC14 — `.../layouts/languages/KeyboardLayoutRO.kt` → **NÃO REMOVER**

| Subtarefa | Evidência |
|---|---|
| 1 | 8.288 bytes · `198fb19db7d6fc67…` · **C12 (MANTER)** · INVESTIGAR/Média (o único INVESTIGAR dentro de um componente MANTER) |
| 2 | Implementação do layout de teclado **romeno**: mapa Unicode → `Keycode`, com dead keys |
| 3 | criado em `4dea8ce` (2026-02-26, "bl: Changed keyboard layout mapping to work with unicode characters"); 2 commits |
| 4 | estende `KeyboardLayout` (`layouts/KeyboardLayout.kt`, C12 MANTER) |
| 5–7 | **2 referências ativas:** `KeyboardViewModel.kt:6` (`import`) e `KeyboardViewModel.kt:22` (`KeyboardLayoutRO.NAME to KeyboardLayoutRO::class.java`, no mapa de layouts disponíveis) |
| 8 | **remover quebra o build** e reduz o seletor de idiomas de 4 para 3 |
| 9 | não há justificativa de remoção. O componente C12 é **MANTER** (teclado e layouts de idioma) e este é um dos 4 layouts. A classificação INVESTIGAR provavelmente reflete dúvida sobre **relevância de produto** (romeno no Pocket Pad?), não sobre segurança técnica — e relevância de produto é decisão do mantenedor, não desta missão |
| 10 | n/a |
| 11 | ❌ **NÃO REMOVER** — reclassificado para **MANTER** (componente C12 manda preservar) |
| 12 | n/a |
| 13 | n/a |
| 14 | `OPEN-QUESTIONS.md` (escopo de idiomas) |

## B.3 Cluster Numpad → **ADIAR** (6 arquivos, preservados integralmente)

O Numpad é **funcional e completo**. A remoção arrastaria 6 arquivos e **4 pontos de código** em
3 outros arquivos, o que caracteriza **refatoração** — proibida nesta missão. Além disso a auditoria
registra que "não há evidência de decisão de produto". Portanto: **preservado, permanece INVESTIGAR**.

A análise individual, com a prova de que cada arquivo está vivo:

| ID | Arquivo | Evidência de que está **vivo** (consumidor) | Decisão |
|---|---|---|---|
| MC13 | `fragments/Numpad.kt` (959 B) | instanciado em `Input.kt:94` → `replaceChildFragment(Numpad())` a partir de `R.id.mode_numpad` (`Input.kt:91`) | **ADIAR** |
| MC15 | `viewmodels/NumpadViewModel.kt` (2.375 B) | binding declarado em `fragment_numpad.xml:9` → `type="com.darusc.mousedroid.viewmodels.NumpadViewModel"` | **ADIAR** |
| MC24 | `res/layout/fragment_numpad.xml` (4.590 B) | inflado por `Numpad.kt`; usa `@drawable/backspace_vector`, `@drawable/enter_vector` e `@style/NumpadKey` | **ADIAR** |
| MC16 | `res/drawable/backspace_vector.xml` (473 B) | `fragment_numpad.xml:123` → `app:icon="@drawable/backspace_vector"` | **ADIAR** |
| MC19 | `res/drawable/enter_vector.xml` (412 B) | `fragment_numpad.xml` (ícone de Enter) | **ADIAR** |

> **Observação técnica:** o cluster também tem ramificações no protocolo — `InputEvent.NumpadKeyPress`
> (`InputEvent.kt:32`) é traduzido em `Translators.kt:134` (HID) e `:196` (socket) — e no servidor
> (opcode de numpad em `inputmanager.hpp`). Remover o Numpad exigiria mexer no **protocolo**, o que é
> proibição explícita ("Não está autorizado a: refatorar o protocolo"). **Não é candidato desta missão.**

## B.4 Regras de backup → **ADIAR** (2 arquivos)

### MC25 — `client/app/src/main/res/xml/backup_rules.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 478 bytes · `9f7e4c84c8fd2c81…` · C21 · INVESTIGAR/Alta |
| 2 | Template do Android Studio: `<full-backup-content>` com **todo o conteúdo comentado** |
| 3 | criado em `88f9a92` (2023-04-14); 1 commit |
| 4–7 | **0 referências**: `grep 'fullBackupContent\|dataExtractionRules' AndroidManifest.xml` → 0 |
| 8 | **hoje nenhum**, mas o Manifest **não declara** `android:fullBackupContent`, o que significa que o **auto-backup padrão está ativo** e inclui `SharedPreferences` — onde vivem os hosts Wi-Fi. Ou seja, o arquivo descreve uma política que **não** está em vigor, e a política em vigor é a padrão |
| 9 | Sem decisão de produto não é seguro remover: se o Pocket Pad quiser excluir credenciais de host do backup (privacidade), este arquivo é o lugar e ele passará a ser referenciado. Remover agora só transferiria trabalho |
| 10 | n/a |
| 11 | ⏸️ **ADIAR** — permanece **INVESTIGAR** |
| 12 | n/a |
| 13 | n/a |
| 14 | `OPEN-QUESTIONS.md` Q18 |

### MC26 — `client/app/src/main/res/xml/data_extraction_rules.xml`

| Subtarefa | Evidência |
|---|---|
| 1 | 551 bytes · `cb1fc47ab4a98453…` · C21 · INVESTIGAR/Alta |
| 2 | Template: `<data-extraction-rules>` → `<cloud-backup>` e `<device-transfer>` comentados |
| 3 | criado em `88f9a92` (2023-04-14); 1 commit |
| 4–7 | 0 referências no Manifest |
| 8 | nenhum hoje; relevante para API 31+ (minSdk 28, targetSdk 34) |
| 9 | mesmo raciocínio de MC25. Par obrigatório: decidir os dois juntos ou nenhum |
| 10 | n/a |
| 11 | ⏸️ **ADIAR** |
| 12–14 | idem MC25 |

## B.5 Configuração de IDE do servidor → **ADIAR** (2 arquivos)

### MC29 — `server/.vscode/launch.json`

| Subtarefa | Evidência |
|---|---|
| 1 | 658 bytes · `49407efb99a3cf8c…` · C24 · INVESTIGAR/Média |
| 2 | Launch `cppvsdbg` (depurador MSVC) apontando `program: ${workspaceFolder}/cmake/bin/Mousedroid.exe`, `preLaunchTask: "Make"` |
| 3 | criado em `cfea91a` (2026-02-27, "server: vscode build tasks"); 1 commit |
| 4 | depende da task `Make` de `tasks.json` (MC30) e do artefato em `cmake/bin/` — que é exatamente o diretório que `server/README.md:55` (`cmake -B"cmake"`) **e** `install.sh`/`release.sh` (`cp cmake/bin/Mousedroid`) usam |
| 5–7 | 0 referências no build/CMake; é consumido **apenas** pelo VS Code |
| 8 | nenhum no build/execução do produto. Para quem usa VS Code no Windows, é o único atalho de depuração |
| 9 | O `launch.json` está **coerente** com o README e com os scripts (todos usam `cmake/`). Quem está incoerente é o `tasks.json` (MinGW). Portanto **este** arquivo não deve ser removido: ele é a metade correta do par. Remover a configuração de depuração de quem usa VS Code é exatamente o que a Fase 08 manda evitar |
| 10 | n/a |
| 11 | ⏸️ **ADIAR** — permanece **INVESTIGAR** |
| 12 | n/a |
| 13 | n/a |
| 14 | `OPEN-QUESTIONS.md`, `CLEANUP-PLAN.md` 9.3 |

### MC30 — `server/.vscode/tasks.json`

| Subtarefa | Evidência |
|---|---|
| 1 | 805 bytes · `923208e3e878b4c8…` · C24 · INVESTIGAR/Alta |
| 2 | Duas tasks: `CMake Build` com `args: ["-G\"MinGW Makefiles\"", "-B./cmake"]` e `Make` com `command: mingw32-make` |
| 3 | criado em `cfea91a` (2026-02-27); 1 commit |
| 4 | referenciada por MC29 (`preLaunchTask: "Make"`) |
| 5–7 | não entra no build do CMake nem no CI |
| 8 | Para quem usa VS Code no Windows com MinGW, é o caminho de build. **Não há evidência de que o MinGW funcione** (o CMake do projeto exige `wxWidgets` + `asio` do vcpkg, que o MinGW não resolve como o MSVC) e **não há evidência de que possa ser descartado** |
| 9 | conflito de toolchain documentado (CMakeSettings.json e README usam MSVC). Alinhar é **correção** (Etapa 9.3 do plano), não remoção. Remover sem substituir quebraria o fluxo VS Code; alterar é `ADAPTAR` e está fora do escopo |
| 10 | n/a |
| 11 | ⏸️ **ADIAR** |
| 12–14 | idem MC29 |

## B.6 Binários ADB → **NÃO REMOVER — proteção expressa** (3 arquivos)

Os três estão em `server/adb/`, pasta **expressamente protegida** pela missão, e são a dependência
funcional do modo USB no Windows.

| ID | Arquivo | Bytes | Conteúdo | Consumidor | Decisão |
|---|---|---:|---|---|---|
| MC31 | `server/adb/AdbWinApi.dll` | 108.312 | biblioteca PE32 de API USB do ADB (SHA-256 `9ffacedc41b27520…`) | carregada por `adb.exe` | **NÃO REMOVER** |
| MC32 | `server/adb/AdbWinUsbApi.dll` | 73.496 | biblioteca PE32 WinUSB do ADB (`13918fdab0c3ac77…`) | carregada por `adb.exe` | **NÃO REMOVER** |
| MC33 | `server/adb/adb.exe` | 5.916.440 | Android Debug Bridge Windows, MZ/PE válido (`705ddc21f33ac521…`) | `server/src/adb.hpp:33` → `path + "\\adb.exe reverse tcp:<port> tcp:<port>"` e `:60` → `reverse --remove` / `kill-server`; distribuído por `release.bat:8` (`xcopy "adb" "dist\adb\"`) e `release.sh:9` | **NÃO REMOVER** |

Justificativa comum adicional: são **84,1% do peso do repositório**, mas o objetivo da missão é
"remover complexidade desnecessária", não reduzir tamanho. Remover os três **desabilita o modo USB no
Windows**, que é um dos três modos do produto. A pendência jurídica (licença de redistribuição) é
registrada em `OPEN-QUESTIONS.md` Q4 e **não** autoriza remoção.

## B.7 Código do servidor → **NÃO REMOVER** (1 arquivo)

### MC34 — `server/src/gui/wxdevicelist.cpp` → **NÃO REMOVER**

| Subtarefa | Evidência |
|---|---|
| 1 | 1.476 bytes · `8150cfc92a6a3889…` · **C31 (ADAPTAR)** · INVESTIGAR/Média |
| 2 | Implementa `wxDeviceList::wxDeviceList` (6 colunas), `InsertDevice`, `UpdateView(bool)`, `SetDevices` e o destrutor |
| 3 | criado em `0f43fa7` (2026-02-28, "server: Refactored src structure"); 1 commit |
| 4 | `#include "wxdevicelist.h"`; é instanciado pela GUI (`gui/wxmain.*`) e usa `net/device.h` |
| 5 | **faz parte do target CMake**: `file(GLOB_RECURSE ALL_SRC src/*.cpp)` (`CMakeLists.txt:36`) inclui todos os `.cpp`, e `wxdevicelist.cpp` não está em `PLATFORM_SRC` |
| 6–7 | referenciado pelo `CMakeLists.txt` via GLOB; **removê-lo quebra o link** (símbolos `wxDeviceList::*` usados por `wxmain`) |
| 8 | remover quebra a compilação do servidor **nas duas plataformas** |
| 9 | A auditoria marcou INVESTIGAR por causa de um **defeito** em `UpdateView(false)` (`:41-43`: `InsertDevice(i, devices[i])` lê `devices[GetItemCount()]` sem checar `i < devices.size()` → possível acesso fora do vetor). **Defeito não é motivo de remoção**; é motivo de correção (Etapa 9.5), e a missão proíbe corrigir funcionalidade nesta fase. O caminho `false` nunca é chamado hoje (`SetDevices` sempre usa `UpdateView(true)`) |
| 10 | n/a |
| 11 | ❌ **NÃO REMOVER** — reclassificado para **MANTER** |
| 12 | n/a |
| 13 | build Linux + Windows no CI |
| 14 | `OPEN-QUESTIONS.md` (defeito A14 registrado) |

---

# PARTE C — Matriz de impacto (Fase 06, tarefas 051–060)

## C.1 Grafo de dependências internas construído

| # | Tarefa | Resultado |
|---|---|---|
| 051 | Mapa de dependências internas | construído: 34 candidatos × arestas de entrada/saída |
| 052 | Relações de importação Kotlin | 13 arquivos `.kt` analisados: `import com.darusc.mousedroid.X` rastreado. **Achado:** `BatteryMonitor` é importado por `ConnectionManager.kt:7` e usado por `MainActivity.kt:63` |
| 053 | Relações entre classes Android | `ConnectionManager : Connection.Listener, BatteryMonitor.Listener`; `KeyboardLayoutRO : KeyboardLayout`; `Numpad` → `NumpadViewModel` (binding); `Numpad` ← `Input.kt:94` |
| 054 | Relações entre arquivos C++ | `SETTINGS → ADB → LOGGER`; `SERVER → CONNECTION → INPUTMANAGER`; `WXMAIN → WXDEVICELIST`; `WXAPPLICATION → WXMAIN, SERVER, SETTINGS` |
| 055 | Dependências de CMake | `file(GLOB_RECURSE ALL_SRC src/*.cpp)` captura **todos** os `.cpp`; `list(REMOVE_ITEM ALL_SRC ${PLATFORM_SRC})` remove o input manager da outra plataforma. Nenhum candidato está em `PLATFORM_SRC` → todos os `.cpp` são obrigatórios |
| 056 | Dependências Gradle | `proguard-rules.pro` é argumento de `proguardFiles` (`app/build.gradle:24`); `gradlew` é o entrypoint do wrapper |
| 057 | Dependências de scripts shell | `release.sh` copia `adb/*` e o binário de `cmake/bin/`; `install.sh` copia `mousedroid_linux/*`. **Nenhum** script referencia qualquer um dos 13 candidatos a remover |
| 058 | Dependências de configuração de IDE | `client/.idea/*` é lido só pelo IDE; `server/.vscode/launch.json` → `tasks.json` (par interno) |
| 059 | Caminhos em documentação | 34 caminhos buscados. **Todas** as ocorrências estão em `docs/audit/` e `docs/cleanup/` (documentação), exceto: `proguard-rules.pro` (build), `gradlew` (workflow + docs) e `adb.exe` (docs + `adb.hpp`) |
| 060 | Matriz de impacto | tabela C.2 |

## C.2 Matriz de impacto por candidato

| ID | Arquivo | Build Android | Build Linux | Build Windows | Execução app | Execução servidor | Decisão |
|---|---|---|---|---|---|---|---|
| MC01 | `.gradle/…lock` | — | — | — | — | — | REMOVER |
| MC02 | `.gradle/…cache.properties` | — | — | — | — | — | REMOVER |
| MC03 | `.gradle/config.properties` | — | — | — | — | — | REMOVER |
| MC04 | `.gradle/vcs-1/gc.properties` | — | — | — | — | — | REMOVER |
| MC05 | `.idea/assetWizardSettings.xml` | — | — | — | — | — | REMOVER |
| MC06 | `.idea/caches/deviceStreaming.xml` | — | — | — | — | — | REMOVER |
| MC07 | `.idea/deploymentTargetDropDown.xml` | — | — | — | — | — | REMOVER |
| MC08 | `.idea/deploymentTargetSelector.xml` | — | — | — | — | — | REMOVER |
| MC09 | `client/README.md` | — | — | — | — | — | REMOVER |
| MC10 | `proguard-rules.pro` | **quebra `release`** | — | — | — | — | NÃO REMOVER |
| MC11 | `ExampleInstrumentedTest.kt` | — (só remove 1 teste) | — | — | — | — | REMOVER |
| MC12 | `BatteryMonitor.kt` | **QUEBRA BUILD** | — | — | — | — | NÃO REMOVER |
| MC13 | `Numpad.kt` | **quebra build** | — | — | remove numpad | — | ADIAR |
| MC14 | `KeyboardLayoutRO.kt` | **QUEBRA BUILD** | — | — | perde idioma | — | NÃO REMOVER |
| MC15 | `NumpadViewModel.kt` | **quebra build** | — | — | remove numpad | — | ADIAR |
| MC16 | `backspace_vector.xml` | **quebra build** | — | — | remove numpad | — | ADIAR |
| MC17 | `baseline_add_24.xml` | — | — | — | — | — | REMOVER |
| MC18 | `devicelistitembackground.xml` | — | — | — | — | — | REMOVER |
| MC19 | `enter_vector.xml` | **quebra build** | — | — | remove numpad | — | ADIAR |
| MC20 | `ic_iconmonstr_check_mark_thin.xml` | — | — | — | — | — | REMOVER |
| MC21 | `ic_iconmonstr_x_mark_thin.xml` | — | — | — | — | — | REMOVER |
| MC22 | `ic_pause.xml` | — | — | — | — | — | REMOVER |
| MC23 | `logo.png` | **QUEBRA BUILD** | — | — | perde logo | — | NÃO REMOVER |
| MC24 | `fragment_numpad.xml` | **quebra build** | — | — | remove numpad | — | ADIAR |
| MC25 | `backup_rules.xml` | — | — | — | — | — | ADIAR |
| MC26 | `data_extraction_rules.xml` | — | — | — | — | — | ADIAR |
| MC27 | `ExampleUnitTest.kt` | — (só remove 1 teste) | — | — | — | — | REMOVER |
| MC28 | `gradlew` | **QUEBRA BUILD** | — | — | — | — | NÃO REMOVER |
| MC29 | `launch.json` | — | — | — | — | — | ADIAR |
| MC30 | `tasks.json` | — | — | — | — | — | ADIAR |
| MC31 | `AdbWinApi.dll` | — | — | — | — | **quebra USB Win** | NÃO REMOVER |
| MC32 | `AdbWinUsbApi.dll` | — | — | — | — | **quebra USB Win** | NÃO REMOVER |
| MC33 | `adb.exe` | — | — | — | — | **quebra USB Win** | NÃO REMOVER |
| MC34 | `wxdevicelist.cpp` | — | **QUebra link** | **quebra link** | — | — | NÃO REMOVER |

**Leitura da matriz:** dos 34 candidatos, **9 quebrariam a compilação** se removidos
(MC10, MC12, MC13, MC14, MC15, MC16, MC19, MC23, MC24, MC28, MC34 — 11 na verdade, contando os do
Numpad e os dois de plataforma). **Nenhum deles será removido.** Isso confirma a diretriz da missão de
que "a ausência de import direto não é prova suficiente de inutilidade".

---

# PARTE D — Lista final aprovada (Fase 11, tarefas 101–110)

| # | Verificação exigida pela missão | Resultado |
|---|---|---|
| 101 | Revisar individualmente todos os REMOVER | ✅ 13/13 revisados individualmente (Parte A) |
| 102 | Cada arquivo com justificativa documentada | ✅ 17 fichas com justificativa na subtarefa 9 |
| 103 | Nenhum arquivo usado pela **aplicação** | ✅ MC12 refutado (usado); os outros 12 não têm uso |
| 104 | Nenhum arquivo usado pelo **servidor** | ✅ nenhum dos 13 históricos toca `server/` |
| 105 | Nenhum participa da **compilação** | ✅ verificado: nenhum dos 17 aprovados é argumento de Gradle/CMake/GLOB |
| 106 | Nenhum participa da **execução** | ✅ verificado |
| 107 | Nenhuma dependência essencial perdida | ✅ MC10, MC14, MC23, MC28, MC31-33, MC34 preservados por esse motivo |
| 108 | Arquivos legais e de atribuição preservados | ✅ `LICENSE` intacto (C01); ícones iconmonstr órfãos removidos **reduzem** exposição |
| 109 | ADB e wrappers não removidos | ✅ `server/adb/*` intacto; `gradlew` **mantido** (só o modo muda) |
| 110 | Lista final aprovada/bloqueada | ✅ abaixo |

## D.1 Remoções **APROVADAS** — 16 arquivos, 75.268 bytes

| Lote | ID | Arquivo | Bytes | Origem da classificação |
|---|---|---|---:|---|
| **1** | MC01 | `client/.gradle/buildOutputCleanup/buildOutputCleanup.lock` | 17 | REMOVER histórico |
| **1** | MC02 | `client/.gradle/buildOutputCleanup/cache.properties` | 52 | REMOVER histórico |
| **1** | MC03 | `client/.gradle/config.properties` | 76 | REMOVER histórico |
| **1** | MC04 | `client/.gradle/vcs-1/gc.properties` | 0 | REMOVER histórico |
| **1** | MC09 | `client/README.md` | 1 | REMOVER histórico |
| **1** | MC05 | `client/.idea/assetWizardSettings.xml` | 1.949 | INVESTIGAR → resolvido |
| **1** | MC06 | `client/.idea/caches/deviceStreaming.xml` | 69.823 | INVESTIGAR → resolvido |
| **1** | MC07 | `client/.idea/deploymentTargetDropDown.xml` | 214 | INVESTIGAR → resolvido |
| **1** | MC08 | `client/.idea/deploymentTargetSelector.xml` | 301 | INVESTIGAR → resolvido |
| **2** | MC17 | `client/app/src/main/res/drawable/baseline_add_24.xml` | 319 | REMOVER histórico |
| **2** | MC18 | `client/app/src/main/res/drawable/devicelistitembackground.xml` | 291 | REMOVER histórico |
| **2** | MC20 | `client/app/src/main/res/drawable/ic_iconmonstr_check_mark_thin.xml` | 397 | REMOVER histórico |
| **2** | MC21 | `client/app/src/main/res/drawable/ic_iconmonstr_x_mark_thin.xml` | 490 | REMOVER histórico |
| **2** | MC22 | `client/app/src/main/res/drawable/ic_pause.xml` | 323 | REMOVER histórico |
| **3** | MC11 | `client/app/src/androidTest/java/com/darusc/mousedroid/ExampleInstrumentedTest.kt` | 670 | REMOVER histórico |
| **3** | MC27 | `client/app/src/test/java/com/darusc/mousedroid/ExampleUnitTest.kt` | 345 | REMOVER histórico |

> **Total aprovado: 16 arquivos, 75.268 bytes.** Desses, **12** vêm do conjunto histórico de 13
> (o 13º, MC12, foi refutado) e **4** são metadados de IDE resolvidos a partir de INVESTIGAR.
> **Repartição por lote:** Lote 1 = 9 arquivos / 72.433 bytes; Lote 2 = 5 arquivos / 1.820 bytes;
> Lote 3 = 2 arquivos / 1.015 bytes.
> **Nota de honestidade sobre o "espaço liberado":** 72.287 dos 75.268 bytes (96%) são os quatro
> arquivos de `client/.idea/`. O Gradle **recria** `client/.gradle/` localmente a cada build, então a
> redução real do clone é de ~75 KB em arquivos rastreados, mas o repositório pode voltar a acumular
> arquivos **não rastreados** nesse diretório — por isso a correção do `.gitignore` (Lote 1) é parte
> inseparável da remoção.

## D.2 Remoções **BLOQUEADAS / ADIADAS** — 18 arquivos

| ID | Arquivo | Motivo do bloqueio |
|---|---|---|
| MC12 | `BatteryMonitor.kt` | **compilado e chamado** (falso positivo da auditoria) |
| MC10 | `proguard-rules.pro` | argumento de `proguardFiles` no build |
| MC23 | `logo.png` | 2 referências `@drawable/logo` em layouts |
| MC14 | `KeyboardLayoutRO.kt` | 2 referências; componente C12 = MANTER |
| MC28 | `gradlew` | entrypoint único do build Android |
| MC31 | `AdbWinApi.dll` | protegido; USB Windows |
| MC32 | `AdbWinUsbApi.dll` | protegido; USB Windows |
| MC33 | `adb.exe` | protegido; `adb.hpp` |
| MC34 | `wxdevicelist.cpp` | compilado por GLOB no servidor |
| MC13 | `Numpad.kt` | ADIAR — decisão de produto + refatoração |
| MC15 | `NumpadViewModel.kt` | ADIAR — idem |
| MC24 | `fragment_numpad.xml` | ADIAR — idem |
| MC16 | `backspace_vector.xml` | ADIAR — idem |
| MC19 | `enter_vector.xml` | ADIAR — idem |
| MC25 | `backup_rules.xml` | ADIAR — política de backup é decisão de produto |
| MC26 | `data_extraction_rules.xml` | ADIAR — idem |
| MC29 | `launch.json` | ADIAR — configuração VS Code coerente com o README |
| MC30 | `tasks.json` | ADIAR — conflito de toolchain; corrigir (9.3), não remover |

## D.3 Reclassificações propostas (para consolidação em `CLASSIFICATION.tsv`)

| Arquivo | De | Para | Motivo |
|---|---|---|---|
| `BatteryMonitor.kt` | REMOVER | **INVESTIGAR** | falso positivo: 8 referências de código, incluindo `stop()` ativo em `MainActivity` |
| `proguard-rules.pro` | INVESTIGAR | **MANTER** | consumidor real em `app/build.gradle:24` |
| `logo.png` | INVESTIGAR | **MANTER** | 2 referências em layout |
| `KeyboardLayoutRO.kt` | INVESTIGAR | **MANTER** | componente C12 é MANTER; 2 referências |
| `gradlew` | INVESTIGAR | **MANTER** | entrypoint do build; corrigir modo para `100755` |
| `wxdevicelist.cpp` | INVESTIGAR | **MANTER** | compilado por GLOB |
| 4 × `.idea` (MC05–08) | INVESTIGAR | **REMOVER** | caminho/estado de máquina, cache, sessão vazia |
| 5 × `client/.gradle` + `client/README.md` + 5 drawables + 2 testes | REMOVER | **REMOVER (confirmado)** | evidência individual nas Partes A |
| Numpad (5) | INVESTIGAR | **INVESTIGAR** (mantido) | decisão de produto pendente + refatoração proibida |
| backup rules (2), VS Code (2) | INVESTIGAR | **INVESTIGAR** (mantido) | decisão de produto / correção, não remoção |
| `server/adb/*` (3) | INVESTIGAR | **INVESTIGAR** (mantido, protegido) | dependência funcional + questão jurídica aberta |
