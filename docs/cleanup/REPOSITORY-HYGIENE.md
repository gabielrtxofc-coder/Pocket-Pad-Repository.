# Pocket Pad — política de higiene do repositório

> Criado na Missão 04 (tarefa 140). Este é o documento que responde "posso commitar isto?".
> Substitui a ausência de política que permitiu, por dois anos, que o caminho do SDK de um
> desenvolvedor e 70 KB de cache de IDE ficassem versionados.

---

## 1. A regra única

> **Versione o que descreve o projeto. Ignore o que descreve a sua máquina.**

Aplicando a regra, os três eixos:

| Pergunta | Se SIM | Se NÃO |
|---|---|---|
| O arquivo é necessário para **compilar ou executar** em uma máquina limpa? | versionar | ignorar |
| O arquivo **descreve o projeto** (build, código, contrato, documentação, licença)? | versionar | ignorar |
| O arquivo **muda sozinho** quando você abre o IDE ou roda um build? | ignorar | versionar |

---

## 2. O que é versionado de propósito (não remova)

| Caminho | Por que é versionado |
|---|---|
| `client/gradle/wrapper/gradle-wrapper.jar` + `.properties` | é o **mecanismo de bootstrap** do Gradle; sem ele o build não começa. **Nunca** coloque `*.jar` no `.gitignore` |
| `client/gradlew`, `client/gradlew.bat` | entrypoints do build (modo `100755` desde a Missão 04) |
| `client/gradle.properties` | flags `android.useAndroidX`, `kotlin.code.style`, memória do daemon |
| `client/settings.gradle`, `client/build.gradle`, `client/app/build.gradle` | definição do build |
| `client/app/proguard-rules.pro` | argumento de `proguardFiles` em `app/build.gradle:24` |
| `client/.idea/` — **11 arquivos** | configuração de projeto **sem** caminho de máquina (ver §3) |
| `client/.idea/.gitignore` | é a política local do IDE (ignora `workspace.xml` e `shelf/`) |
| `server/adb/adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll` | **distribuição intencional**; dependência funcional do modo USB no Windows. Protegidos |
| `server/app.ico`, `server/icon.png`, `server/res/icon.xpm` | referenciados por `CMakeLists.txt`, `install.sh`, `release.sh`, `wxmain.cpp` |
| `imgs/*`, `fastlane/metadata/**/images/*` | material de apresentação e de loja |
| `client/app/src/main/res/**/*.png` | recursos da aplicação |
| `server/.vscode/launch.json` e `tasks.json` | configuração de IDE compartilhável do servidor |
| `docs/**` | evidência histórica e auditoria |
| `LICENSE` | obrigação legal |

---

## 3. `.idea/` — versionado **por exceção**, não por padrão

`.idea/` é ignorado como diretório, e **11 arquivos** são reabertos explicitamente:

```
!.idea/.gitignore
!.idea/.name
!.idea/AndroidProjectSystem.xml
!.idea/compiler.xml
!.idea/gradle.xml
!.idea/kotlinc.xml
!.idea/migrations.xml
!.idea/misc.xml
!.idea/navEditor.xml
!.idea/runConfigurations.xml
!.idea/vcs.xml
```

**Por que uma lista de exceção e não "versione tudo"?** Porque o teste é objetivo: um arquivo de IDE
só entra se **não contiver caminho absoluto nem estado de sessão**. Foram removidos por violar o teste:

| Removido | Violação |
|---|---|
| `.idea/assetWizardSettings.xml` | `D:\Projects\Mousedroid\client` e `$USER_HOME$/AppData/...` |
| `.idea/caches/deviceStreaming.xml` | cache de 70 KB, catálogo de hardware de terceiros |
| `.idea/deploymentTargetDropDown.xml` | estado de sessão (`<State />` vazio) |
| `.idea/deploymentTargetSelector.xml` | preferência de UI de sessão |

**Se você precisar versionar um arquivo novo de `.idea/`:** adicione a linha `!` correspondente em
**ambos** `.gitignore` (raiz e `client/`) e justifique em um PR. Não use `git add -f`.

---

## 4. O que **nunca** deve ser versionado

| Padrão | Por quê |
|---|---|
| `local.properties` | caminho do SDK **desta** máquina. O próprio template do arquivo diz para não versionar |
| `client/.gradle/`, `build/`, `app/build/` | saída de build, regenerada a cada execução |
| `.idea/workspace.xml`, `.idea/shelf/`, `.idea/caches/` | estado local do IDE |
| `*.iml` | metadado de módulo gerado pelo IDE |
| `*.jks`, `*.keystore`, `keystore.properties` | **chave de assinatura**; vazamento é irreversível |
| `server/cmake/`, `server/out/`, `server/dist/` | saída de build CMake |
| `*.log`, `*.tmp`, `*.bak`, `*~` | temporários |
| `.DS_Store`, `Thumbs.db` | metadado de sistema operacional |

---

## 5. O que **não** pode entrar no `.gitignore` (armadilhas já evitadas)

| Padrão tentador | Dano |
|---|---|
| `*.exe` | **apagaria `server/adb/adb.exe`** do controle de versão (5,9 MB de dependência funcional) |
| `*.dll` | apagaria `AdbWinApi.dll` e `AdbWinUsbApi.dll` |
| `*.jar` | apagaria `gradle-wrapper.jar` → **build Android impossível** |
| `*.png` | apagaria `imgs/*`, `fastlane/**/icon.png` e todos os `res/mipmap-*/` |
| `*.ico`, `*.xpm` | apagaria os ícones do servidor referenciados pelo CMake |
| `.vscode/` sem exceção | apagaria as duas únicas configurações de IDE do servidor |
| `*.properties` | apagaria `gradle.properties` e `gradle-wrapper.properties` |

**Por isso o `.gitignore` deste repositório usa caminhos específicos, nunca extensões globais.**

---

## 6. Como verificar antes de commitar

### 6.1 O arquivo que você vai adicionar deveria estar ignorado?

```bash
git check-ignore -v <caminho>
```
- **Retorna uma linha** → está ignorado, e a linha mostra **qual regra** o ignorou. Não force.
- **Retorna vazio** → será versionado. Pergunte-se se descreve o projeto ou a sua máquina.

### 6.2 Alguma regra nova está ignorando algo que já é versionado? (teste obrigatório)

Este é o teste que impede o desastre de apagar `server/adb/*` com um `*.exe`:

```bash
git ls-files | while read -r f; do git check-ignore -q "$f" && echo "IGNORADO INDEVIDAMENTE: $f"; done
```
**Saída esperada: nenhuma linha.** Se aparecer algo, a regra está errada.

### 6.3 Verificar as regras sem considerar o índice

```bash
git check-ignore -v --no-index <caminho>
```
Útil para `local.properties`: mesmo estando rastreado (num clone antigo), mostra a regra que o cobre.

---

## 7. Resultado da aplicação da política na Missão 04

| Verificação | Resultado |
|---|---|
| `git check-ignore` contra todos os arquivos rastreados | **0 arquivos versionados são ignorados** |
| Arquivos removidos reincidem? | **Não** — todos casam uma regra de `.gitignore` |
| Arquivos preservados continuam `git add`áveis? | **Sim** — testado com `client/.idea/compiler.xml` |
| Arquivo removido + recriado é recusado pelo Git? | **Sim** — testado ("paths are ignored") |
| Assets protegidos ignorados? | **Não** — `adb.exe`, DLLs, `app.ico`, `icon.xpm`, `gradle-wrapper.jar`, `LICENSE`, `imgs/*.png`, `res/drawable/logo.png`, `fastlane/**/icon.png` todos confirmados como rastreáveis |
| Caminhos pessoais absolutos restantes | **0** |

---

## 8. Dívida de higiene conhecida e não resolvida nesta missão

| Item | Situação | Onde |
|---|---|---|
| `client/.idea/compiler.xml` declara `target 21` e `misc.xml` usa `jbr-21`, mas o Gradle usa 17 | **preservado e reportado** — corrigir é mudança de toolchain, fora do escopo | `OPEN-QUESTIONS.md` |
| `server/.vscode/tasks.json` usa MinGW, divergindo do MSVC/vcpkg oficial | **preservado e reportado** — corrigir é o item 9.3 do plano | `OPEN-QUESTIONS.md` |
| Nenhum teste automatizado real no projeto | **pendência obrigatória** da próxima missão | `OPEN-QUESTIONS.md` |
| `server/README.md` cita `install.bat` inexistente | **preservado** — é ADAPTAR, não limpeza | `OPEN-QUESTIONS.md` |
| O histórico Git ainda contém `sdk.dir` e `java.home` de versões antigas | **aceito** — reescrever histórico é proibido; são caminhos, não credenciais | `CONFIGURATION-HYGIENE.md` §088 |
