# Missão 04 — Higiene de configurações: IDE, portabilidade e `.gitignore`

> **Fases 07, 08, 09 e 14.** Tarefas 061–090 e 131–140.
> Cada afirmação abaixo foi verificada por inspeção de conteúdo, não por suposição.

---

## FASE 07 — Arquivos gerados e cache (tarefas 061–070)

### 061. Inspeção dos diretórios `.gradle`

Existe **um** diretório `.gradle` rastreado: `client/.gradle/`, com 4 arquivos.

| Arquivo | Bytes | Conteúdo |
|---|---:|---|
| `buildOutputCleanup/buildOutputCleanup.lock` | 17 | lock binário do Gradle |
| `buildOutputCleanup/cache.properties` | 52 | `gradle.version=8.11.1` + timestamp `Fri Feb 20 23:04:40 EET 2026` |
| `config.properties` | 76 | `java.home=D:\Programs\Android Studio\jbr` + timestamp `Fri Jul 18 19:01:15 EEST 2025` |
| `vcs-1/gc.properties` | 0 | vazio |

Não existe `client/.gradle/` com caches de dependência (`caches/`, `daemon/`, `native/`,
`fileHashes/`) versionados — o que seria muito pior. O dano é limitado a 4 arquivos de estado.

### 062. Cache × arquivos necessários ao projeto

| Categoria | Exemplos no repositório | Versionado? | Correto? |
|---|---|---|---|
| Cache de build do Gradle | `client/.gradle/**` | **sim** | ❌ **errado** — remover |
| Wrapper (necessário) | `client/gradle/wrapper/gradle-wrapper.jar` (71.685 B, 33 classes) e `.properties` | sim | ✅ **correto** — é o mecanismo oficial de bootstrap |
| Scripts do wrapper | `client/gradlew`, `client/gradlew.bat` | sim | ✅ correto (modo do `gradlew` a corrigir) |
| Saída de build | `client/app/build/`, `client/build/` | não | ✅ correto (ignorados) |
| Cache de IDE | `client/.idea/caches/deviceStreaming.xml` | **sim** | ❌ **errado** — remover |
| Saída CMake | `server/cmake/`, `server/out/` | não | ✅ correto (em `server/.gitignore`) |

### 063. Metadados gerados automaticamente

| Arquivo | Gerado por | Versionado | Ação |
|---|---|---|---|
| `.gradle/buildOutputCleanup/*` | Gradle | sim | remover |
| `.gradle/config.properties` | Gradle (auto-detecção de JVM) | sim | remover |
| `.gradle/vcs-1/gc.properties` | Gradle | sim | remover |
| `.idea/caches/deviceStreaming.xml` | Android Studio + serviço do Google | sim | remover |
| `.idea/deploymentTarget*.xml` | Android Studio | sim | remover |
| `.gradle/**` (futuro) | Gradle | — | **bloquear por `.gitignore`** |

### 064. O Gradle Wrapper **não** foi classificado incorretamente

Verificação explícita, porque a missão exige ("Confirmar que nenhum arquivo do Gradle Wrapper foi
classificado incorretamente"):

| Arquivo do wrapper | Classificação na auditoria | Verificação independente | Veredito |
|---|---|---|---|
| `client/gradle/wrapper/gradle-wrapper.jar` | MANTER (C05) | JAR válido, 33 entradas, `org/gradle/wrapper/WrapperExecutor.class` presente | ✅ MANTER |
| `client/gradle/wrapper/gradle-wrapper.properties` | MANTER (C05) | `distributionUrl` → `gradle-8.11.1-bin.zip`, versão correta | ✅ MANTER |
| `client/gradlew` | **INVESTIGAR** (C05) | 8.762 B, wrapper oficial POSIX sh, 27 referências (inclui 4 no workflow) | ⚠️ **reclassificado para MANTER**; corrigir modo para `100755` |
| `client/gradlew.bat` | MANTER (C05) | wrapper Windows | ✅ MANTER |

O único problema real do conjunto é o **modo** (Fase 10), não a classificação.

### 065. Configurações versionadas que **são** realmente necessárias

| Arquivo | Necessário? | Por quê |
|---|---|---|
| `client/gradle.properties` | ✅ sim | `android.useAndroidX`, memória do daemon — afeta todos os builds |
| `client/gradle/wrapper/*` | ✅ sim | bootstrap do Gradle, sem ele não há build |
| `client/settings.gradle` | ✅ sim | declara módulos e repositórios |
| `client/.gitignore` | ✅ sim | política de higiene (a ser ampliada nesta missão) |
| `server/.gitignore` | ✅ sim | idem |
| `client/.idea/gradle.xml`, `misc.xml`, `compiler.xml`, `kotlinc.xml` | ⚠️ discutível | não contêm caminho absoluto; preservados (ver Fase 08) |
| `client/local.properties` | ❌ **não** | arquivo **estritamente local**, contém caminho do SDK do autor (ver Fase 09) |

### 066. Arquivos gerados de build ainda presentes

`git ls-files` filtrado por `build/`, `out/`, `dist/`, `cmake/`, `*.apk`, `*.exe`, `*.o`, `*.class`:

| Resultado | Observação |
|---|---|
| `client/app/build/**` | **0** (ignorado) |
| `server/cmake/**`, `server/out/**`, `server/dist/**` | **0** (ignorados) |
| `*.apk`, `*.class`, `*.o`, `*.obj` | **0** |
| `server/adb/*.exe`, `*.dll` | **3** — mas são **distribuição intencional**, não saída de build (protegidos) |

Conclusão: **não há saída de build versionada**. A higiene de build do projeto é boa; o problema
está exclusivamente em metadados de IDE/Gradle.

### 067. Caches e temporários adicionais procurados

| Padrão buscado | Encontrado em `git ls-files`? |
|---|---|
| `*.log` | não (só `.vscode/app.log` está **no** `.gitignore`) |
| `*.tmp`, `*.bak`, `*.swp`, `*~`, `.DS_Store`, `Thumbs.db` | não |
| `*.iml`, `.classpath`, `.project`, `.settings/` | não |
| `__pycache__`, `node_modules`, `.venv` | não |
| `.vscode/` (raiz) | não existe; só `server/.vscode/` |
| `.git` aninhado | **0** (verificação em `git ls-files | grep '\.git/'`) |
| Segredos (`*.keystore`, `*.jks`, `keystore.properties`, `.env`) | não |

### 068. Referências em scripts de execução

Nenhum dos artefatos de cache/metadado é referenciado por `install.sh`, `release.sh`, `release.bat`,
`gradlew`, `CMakeLists.txt` ou pelo workflow. Confirmação por `grep` pelo nome do arquivo em todos os
scripts — **0 resultados** para os 9 arquivos do Lote 1.

### 069. Remoções justificadas nesta fase

9 arquivos (ver [`REMOVAL-ANALYSIS.md`](REMOVAL-ANALYSIS.md) MC01–MC09): 4 do `.gradle`, 4 do
`.idea` e o `client/README.md`.

### 070. Alterações de `.gitignore` necessárias

Detalhadas na Fase 14 abaixo. Resumo: `client/.gitignore` hoje tem **4 linhas** e não cobre
`local.properties` nem `.idea/` — os dois arquivos que mais causaram dano neste repositório.

---

## FASE 08 — Limpeza das configurações de IDE (tarefas 071–080)

### 071. Conteúdo de `.idea` inspecionado (15 arquivos, 78.486 bytes no total do componente)

| Arquivo | Bytes | Caminho absoluto? | Categoria | Decisão |
|---|---:|---|---|---|
| `.gitignore` | 47 | não | higiene | MANTER |
| `.name` | 10 | não | identidade do projeto (`Mousedroid`) | MANTER (ADAPTAR no rebranding) |
| `AndroidProjectSystem.xml` | 212 | não | provider Gradle | MANTER |
| `assetWizardSettings.xml` | 1.949 | **SIM** (`D:\Projects\Mousedroid\client`, `$USER_HOME$/AppData/...`) | histórico de diálogo | **REMOVER** |
| `caches/deviceStreaming.xml` | 69.823 | não | **cache** do IDE | **REMOVER** |
| `compiler.xml` | 169 | não | `bytecodeTargetLevel target="21"` | MANTER + **divergência reportada** |
| `deploymentTargetDropDown.xml` | 214 | não | sessão vazia | **REMOVER** |
| `deploymentTargetSelector.xml` | 301 | não | preferência de UI | **REMOVER** |
| `gradle.xml` | 690 | não (`$PROJECT_DIR$`) | módulos e JVM do Gradle | MANTER |
| `kotlinc.xml` | 175 | não | versão Kotlin 2.1.0 (igual ao build) | MANTER |
| `migrations.xml` | 254 | não | migração aplicada | MANTER |
| `misc.xml` | 409 | não (`jbr-21` é nome de SDK) | JDK do projeto | MANTER + **divergência reportada** |
| `navEditor.xml` | 2.940 | não | posições do editor | MANTER |
| `runConfigurations.xml` | 964 | não | run configurations | MANTER |
| `vcs.xml` | 183 | não (`$PROJECT_DIR$`) | mapeamento Git | MANTER |

**Contagem: 4 removidos, 11 preservados.** (A auditoria havia deixado 4 em INVESTIGAR + 1 MANTER
(`.gitignore`); a decisão agora é explícita e justificada, arquivo por arquivo.)

### 072. Caminhos absolutos do computador original

Busca com `grep -rn -E '[A-Za-z]:(\\\\|/)|/home/|/Users/'` em todos os arquivos rastreados:

| Arquivo | Ocorrências | Ação |
|---|---|---|
| `client/.gradle/config.properties` | `java.home=D:\Programs\Android Studio\jbr` | **remover o arquivo** |
| `client/local.properties` | `sdk.dir=C:\Users\Darius\AppData\Local\Android\Sdk` | **destrackear** (Fase 09) |
| `client/.idea/assetWizardSettings.xml` | `sourceFile = D:\Projects\Mousedroid\client`, `$USER_HOME$/AppData/...` | **remover o arquivo** |
| `client/.idea/misc.xml` | **nenhum** — `jbr-21` é o **nome** de um SDK do IDE, não um caminho | manter |
| `client/.idea/gradle.xml`, `vcs.xml` | `$PROJECT_DIR$` (variável, não caminho) | manter |

Após esta missão, **nenhum caminho pessoal absoluto** deve permanecer rastreado. Verificação final em
`CLEANUP-EXECUTION.md`.

### 073. Configurações específicas do autor

| Item | Evidência | Tratamento |
|---|---|---|
| `java.home` do Android Studio do autor | `.gradle/config.properties` | removido |
| `sourceFile` do diretório `D:\Projects\Mousedroid\client` | `.idea/assetWizardSettings.xml` | removido |
| Catálogo de dispositivos do IDE (hardware de terceiros) | `.idea/caches/deviceStreaming.xml` | removido |
| `sdk.dir` do usuário `Darius` | `local.properties` | destrackeado |
| `bytecodeTargetLevel 21` / `JDK_21` | `compiler.xml`, `misc.xml` | **divergência preservada e reportada** (corrigir é fora do escopo) |

### 074. Configurações compartilháveis (preservadas)

`gradle.xml` (módulos: `$PROJECT_DIR$` e `$PROJECT_DIR$/app`), `AndroidProjectSystem.xml`,
`kotlinc.xml`, `migrations.xml`, `misc.xml` (tipo de projeto Android), `runConfigurations.xml`,
`navEditor.xml`, `vcs.xml`, `.name`, `.gitignore`.

### 075. Configurações úteis de inspeção e estilo

`compiler.xml` e `kotlinc.xml` são as únicas que afetam **como o IDE compila/inspeciona**. Ambas
preservadas. **Não existe** no repositório nenhuma configuração de estilo de código versionada
(`codeStyles/`, `inspectionProfiles/`) — portanto não há política de estilo para preservar aqui.

### 076. Arquivos de workspace e estado local

| Arquivo | Presente? | Observação |
|---|---|---|
| `.idea/workspace.xml` | **não** | já ignorado por `.idea/.gitignore` (`/workspace.xml`) — funcionou |
| `.idea/shelf/` | **não** | já ignorado (`/shelf/`) |
| `.idea/deploymentTargetDropDown.xml` | **sim** | estado de sessão → removido |
| `.idea/deploymentTargetSelector.xml` | **sim** | estado de sessão → removido |

Ou seja: o `.idea/.gitignore` herdado **funcionou** para os dois arquivos mais volumosos de estado
local (`workspace.xml`, `shelf/`), e falhou apenas nos dois `deploymentTarget`. Isso é a prova de que
a política correta é **ignorar por padrão e versionar por exceção**.

### 077. `.vscode`

Existe **somente** `server/.vscode/` (2 arquivos: `launch.json` 658 B, `tasks.json` 805 B).
Não existe `.vscode/` na raiz nem em `client/`. O `server/.gitignore` já ignora
`.vscode/app.log`, `.vscode/c_cpp_properties.json` e `.vscode/settings.json`.

### 078. Configurações necessárias ao desenvolvimento

| Configuração | Necessária ao desenvolvimento? | Ação |
|---|---|---|
| `server/.vscode/launch.json` | sim — única configuração de depuração do servidor, e coerente com o README | **preservada** (ADIADA) |
| `server/.vscode/tasks.json` | parcial — única rota de build VS Code/MinGW, mas divergente do toolchain oficial | **preservada** (ADIADA; corrigir no plano 9.3) |
| `client/.idea/` (11 preservados) | sim — o IDE precisa do projeto configurado | **preservados** |

### 079. Comparação com a classificação da auditoria

| Arquivo | Auditoria | Missão 04 | Mudou? |
|---|---|---|---|
| `.idea/.gitignore` | MANTER | MANTER | — |
| `.idea/.name` | ADAPTAR | MANTER (ADAPTAR no rebranding) | — |
| `.idea/AndroidProjectSystem.xml` | MANTER | MANTER | — |
| `.idea/assetWizardSettings.xml` | INVESTIGAR | **REMOVER** | ✅ resolvido |
| `.idea/caches/deviceStreaming.xml` | INVESTIGAR | **REMOVER** | ✅ resolvido |
| `.idea/compiler.xml` | ADAPTAR | MANTER + reportar | — |
| `.idea/deploymentTargetDropDown.xml` | INVESTIGAR | **REMOVER** | ✅ resolvido |
| `.idea/deploymentTargetSelector.xml` | INVESTIGAR | **REMOVER** | ✅ resolvido |
| `.idea/gradle.xml` | MANTER | MANTER | — |
| `.idea/kotlinc.xml` | MANTER | MANTER | — |
| `.idea/migrations.xml` | MANTER | MANTER | — |
| `.idea/misc.xml` | ADAPTAR | MANTER + reportar | — |
| `.idea/navEditor.xml` | MANTER | MANTER | — |
| `.idea/runConfigurations.xml` | MANTER | MANTER | — |
| `.idea/vcs.xml` | MANTER | MANTER | — |
| `server/.vscode/launch.json` | INVESTIGAR | INVESTIGAR (ADIADO) | — |
| `server/.vscode/tasks.json` | INVESTIGAR | INVESTIGAR (ADIADO) | — |

**Resultado: 4 das 4 dúvidas de IDE com caminho/estado de máquina foram resolvidas; as 2 dúvidas de
toolchain do servidor continuam abertas por dependerem de decisão de time.**

### 080. Lista do que pode ser desversionado ou removido

**Remover do índice:** `assetWizardSettings.xml`, `caches/deviceStreaming.xml`,
`deploymentTargetDropDown.xml`, `deploymentTargetSelector.xml`.

**Preservar:** os 11 restantes de `client/.idea/` e os 2 de `server/.vscode/`.

**Corrigir (sem remover):** `compiler.xml` (`target 21` → `17`) e `misc.xml` (`JDK_21` → `17`) —
**fora do escopo desta missão**, porque é mudança de toolchain que altera o comportamento do IDE e
deve ser validada com o mantenedor. Registrado em `OPEN-QUESTIONS.md`.

---

## FASE 09 — Portabilidade das configurações (tarefas 081–090)

### 081–083. Inspeção de `local.properties`

```
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
#
#Sat May 11 18:13:42 EEST 2024
sdk.dir=C\:\\Users\\Darius\\AppData\\Local\\Android\\Sdk
```

O próprio arquivo se declara "must NOT be checked into Version Control Systems" — e está versionado.
`sdk.dir` é usado; `ndk.dir` e `cmake.dir` não existem (não são necessários).

### 084. O arquivo é estritamente local?

**Sim.** `local.properties` é, por definição do Android Gradle Plugin, específico da máquina: contém a
localização do SDK. Não há nenhuma seção compartilhável nele.

### 085. Pode ser gerado sem versionamento?

**Sim, por dois caminhos independentes, ambos já validados pelo CI:**

1. **Android Studio** gera o arquivo automaticamente ao abrir um projeto sem ele.
2. **Variável de ambiente** `ANDROID_HOME` / `ANDROID_SDK_ROOT` é usada pelo AGP quando
   `local.properties` não existe.
3. **O CI já contorna** o problema: os dois jobs Android executam
   `printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties` antes de chamar o Gradle.

### 086. Modelo de configuração

Não é necessário criar um arquivo-modelo versionado: um `local.properties.example` apenas duplicaria
uma informação que o Android Studio gera sozinho. O lugar correto da instrução é a **documentação**
(ver 090). A decisão é registrada e não adiciona arquivo ao repositório — o que é coerente com o
objetivo da missão.

### 087. `.gitignore` atualizado

`client/local.properties` passa a ser ignorado (ver Fase 14). Pré-requisito obrigatório: **remover do
índice (`git rm --cached`) antes de aplicar a regra**, porque `.gitignore` não afeta arquivos já
rastreados.

### 088. Nenhum segredo ou caminho pessoal será publicado

| Verificação | Resultado |
|---|---|
| `sdk.dir` com `C:\Users\Darius\...` | removido do índice |
| `java.home` com `D:\Programs\Android Studio\jbr` | arquivo removido |
| `D:\Projects\Mousedroid\client` | arquivo removido |
| Keystores / `.jks` / `.keystore` / `keystore.properties` | **não existem** |
| `.env`, tokens, senhas | **não existem** |
| Credenciais em `gradle.properties` | nenhuma (só flags de build) |

> Observação de segurança registrada: o **histórico** Git continua contendo `sdk.dir` e `java.home`
> nas versões anteriores. A missão **proíbe reescrever histórico** ("Não reescrever histórico"), então
> eles permanecem no passado do repositório. Isso é aceitável porque são **caminhos de diretório de
> SDK, não credenciais**. Registrado explicitamente para não haver falsa sensação de "purga".

### 089. Compatibilidade com o CI

| Cenário | Antes | Depois |
|---|---|---|
| CI com `local.properties` ausente no checkout | job escreve o arquivo antes do Gradle → **funciona** | idem — **nada muda**, porque o arquivo era sobrescrito de todo modo |
| CI se alguém remover o passo `printf` | `sdk.dir` do autor seria usado → **falharia** (o caminho é de Windows) | `ANDROID_HOME` é usado → **funciona** |
| Desenvolvedor novo | precisa de `local.properties` ou `ANDROID_HOME` | idem, mas agora **documentado** |

Ou seja: destrackear `local.properties` **aumenta** a robustez do CI, não a diminui.

### 090. Documentação: como preparar o ambiente Android em um novo computador

Registrado em [`DEVELOPMENT-SETUP.md`](DEVELOPMENT-SETUP.md) — criado nesta missão justamente porque
o repositório não tinha essa instrução e dependia do arquivo do autor.

---

## FASE 14 — Consolidação do `.gitignore` e higiene (tarefas 131–140)

### 131. `client/.gitignore` — estado encontrado (4 linhas, 35 bytes)

```
build
app/build
app/release
.gradle
```

Problemas encontrados:

1. **`build` e `app/build` são redundantes**: o padrão `build` já casa `client/build` e
   `client/app/build` (um padrão sem barra inicial casa em qualquer nível).
2. **`.gradle` (sem barra) casa `.gradle` em qualquer profundidade** — é amplo mas correto para este
   projeto. Mantido, agora sem o ponto de ambiguidade.
3. **Não cobre `local.properties`** → o arquivo do autor ficou versionado por dois anos.
4. **Não cobre `.idea/`** → 15 arquivos de metadados ficaram versionados, incluindo 70 KB de cache.
5. **Não cobre `*.iml`**, `.navigation/`, `captures/`, `externalNativeBuild/`, `.cxx/` — padrões do
   template do Android Studio.
6. **Não cobre `*.jks`/`*.keystore`** — risco futuro de vazar chave de assinatura.

### 132. `.gitignore` internos

| Arquivo | Linhas | Situação |
|---|---:|---|
| `client/.gitignore` | 4 | **ampliado** nesta missão |
| `server/.gitignore` | 7 | adequado (`cmake`, `out`, `.vs`, `resource.o`, e 3 em `.vscode/`) |
| raiz | **não existe** | **criado** nesta missão |

### 133. Padrões redundantes identificados (e corrigidos)

| Padrão | Problema | Correção |
|---|---|---|
| `build`, `app/build` | `app/build` é subsumido por `build` | manter só `build` |
| `.gradle` | sem `/` casa em qualquer nível; semântica desejada | reescrito como `.gradle/` (explícito) |

### 134. Padrões excessivamente abrangentes

Nenhum padrão atual é excessivamente abrangente a ponto de ignorar código. O novo `.gitignore` da
raiz é escrito com cuidado para **não** casar:
`server/adb/*.exe`/`*.dll` (distribuição protegida — por isso **não** entra `*.exe` global),
`server/res/icon.xpm`, `imgs/*.png`, `client/app/src/main/res/**/*.png` (recursos do app).
Verificação na tarefa 139.

### 135–138. Garantias exigidas

| Garantia | Como foi garantida |
|---|---|
| **135** arquivos locais protegidos | `local.properties`, `.idea/` (por exceção), `.gradle/`, `*.jks`, `*.keystore` |
| **136** caches não voltam a ser versionados | `.gradle/` e `.idea/caches/` ignorados; `client/.idea/.gitignore` mantido |
| **137** bibliotecas funcionais não ignoradas | verificação explícita de que nenhum padrão casa `client/gradle/wrapper/gradle-wrapper.jar` |
| **138** artefatos de distribuição preservados | verificação explícita de que `server/adb/adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll`, `server/app.ico`, `server/res/icon.xpm` **não** são ignorados |
| 137/138 extra | `git check-ignore -v` executado contra os 217 arquivos rastreados: **nenhum arquivo rastreado deve passar a ser ignorado** |

### 139. Teste dos padrões com arquivos representativos

Executado com `git check-ignore -v` contra uma matriz de caminhos representativos
(ver resultado em [`CLEANUP-EXECUTION.md`](CLEANUP-EXECUTION.md) §Fase 14). Regra de aceitação
adotada: **um padrão é correto se ignora o gerado e não ignora o versionado.**

### 140. Documentação da higiene

Registrada em [`REPOSITORY-HYGIENE.md`](REPOSITORY-HYGIENE.md).
