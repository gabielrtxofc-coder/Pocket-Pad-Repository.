# Pocket Pad — preparação do ambiente de desenvolvimento

> Criado na Missão 04 (tarefa 090). Antes deste documento, o repositório dependia de um
> `client/local.properties` versionado que apontava para o SDK **da máquina do autor**
> (`C:\Users\Darius\AppData\Local\Android\Sdk`) — ou seja, só compilava naquela máquina.
> Este documento substitui essa dependência por instruções.

---

## 1. Requisitos por plataforma

### 1.1 Android (cliente)

| Requisito | Versão | Observação |
|---|---|---|
| JDK | **17** | `jvmTarget = '17'` em `client/app/build.gradle`; o CI usa Temurin 17 |
| Android SDK Platform | **34** | `compileSdk 34`, `targetSdk 34` |
| Android SDK Build-Tools | **35.0.0** | o que o CI instala |
| Android SDK Platform-Tools | qualquer recente | fornece `adb` |
| Gradle | **8.11.1** | fornecido pelo wrapper — **não instale Gradle** |
| Emulador | API 34, `x86_64`, `google_apis` | necessário só para testes instrumentados |
| `minSdk` do app | 28 | determinado por `BluetoothHidDevice` |

**A única coisa que você precisa configurar é onde está o SDK.** Duas formas, escolha uma:

**Opção A — variável de ambiente (recomendada, não cria arquivo):**
```bash
export ANDROID_HOME="$HOME/Android/Sdk"      # Linux
export ANDROID_SDK_ROOT="$ANDROID_HOME"      # compatibilidade
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

**Opção B — deixar o Android Studio gerar o arquivo:**
Abra `client/` no Android Studio. Ele cria `client/local.properties` automaticamente.
Esse arquivo **é ignorado pelo Git** (ver `.gitignore`), então nunca será commitado por engano.

> ⚠️ **Não crie `client/local.properties` à mão e não o commite.** Se você já tinha um clone
> antigo do repositório, o arquivo pode estar rastreado no seu índice local. Corrija com:
> ```bash
> git rm --cached client/local.properties
> ```
> (Isto só remove do índice; o arquivo no disco é preservado.)

### 1.2 Servidor Linux

| Requisito | Versão | Observação |
|---|---|---|
| CMake | **≥ 3.26** | `cmake_minimum_required(VERSION 3.26)` |
| Compilador C++ | C++17 | `g++` 12 / `clang++` 15 ou superior |
| Asio | qualquer 1.2x | `libasio-dev`; `#include <asio.hpp>` |
| wxWidgets | **3.3** | `libwxgtk3.3-dev`. ⚠️ o APT do Ubuntu 24.04 traz 3.2 — por isso o CI **compila** 3.3.4 |
| `adb` | qualquer | `adb` no `PATH` (usado no caminho USB) |
| udev | — | regra de `uinput` instalada por `server/install.sh` |

```bash
sudo apt update
sudo apt install build-essential cmake libwxgtk3.3-dev libasio-dev adb
```

### 1.3 Servidor Windows

| Requisito | Versão | Observação |
|---|---|---|
| Visual Studio | 2022 com "Desktop development with C++" | toolchain **oficial** (MSVC) |
| CMake | ≥ 3.26 | vem com o VS |
| vcpkg | — | `vcpkg install asio:x64-windows wxwidgets:x64-windows` |
| `adb` | — | já empacotado em `server/adb/` (não instale) |

> ℹ️ `server/.vscode/tasks.json` oferece uma rota alternativa por **MinGW**. Ela **não** é a rota
> validada pelo CI e é incompatível com a resolução de `wxWidgets`/`asio` via vcpkg descrita em
> `server/CMakeSettings.json` e `server/README.md`. Divergência registrada na Missão 03 (A11) e
> mantida nesta missão — ver `docs/cleanup/OPEN-QUESTIONS.md`.

---

## 2. Primeiro build

### 2.1 Cliente Android

```bash
cd client

# 1. Confirme o JDK 17
java -version              # deve dizer 17.x

# 2. Confirme o SDK (uma das duas)
echo "$ANDROID_HOME"       # opção A
test -f local.properties && echo "SDK configurado via arquivo"   # opção B

# 3. Build + testes unitários (o wrapper baixa o Gradle 8.11.1 sozinho)
./gradlew testDebugUnitTest assembleDebug

# 4. Testes instrumentados (precisa de emulador ou aparelho ligado)
./gradlew connectedDebugAndroidTest
```

> Se `./gradlew` falhar com *Permission denied* em um clone antigo, use `bash ./gradlew` ou
> `chmod +x gradlew`. **A partir da Missão 04 o bit executável `100755` está registrado no Git**,
> então clones novos já vêm executáveis.

APK gerado em `client/app/build/outputs/apk/debug/app-debug.apk`.

### 2.2 Servidor Linux

```bash
cd server
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --parallel
./build/bin/Mousedroid
```

Para instalar no sistema (requer `sudo`, cria entrada de menu e regra udev):
```bash
cd server
./release.sh      # monta mousedroid_linux/ com binário + ícone + config.ini
./install.sh      # copia para /usr/share/mousedroid e cria o .desktop
```

> Os dois scripts já vêm executáveis (`100755`) a partir da Missão 04. O `chmod +x` que o
> `server/README.md` pede continua funcionando, mas deixou de ser necessário.

### 2.3 Servidor Windows

```powershell
cd server
vcpkg install asio:x64-windows wxwidgets:x64-windows
cmake -S . -B build -A x64 `
  -DCMAKE_TOOLCHAIN_FILE="$env:VCPKG_INSTALLATION_ROOT/scripts/buildsystems/vcpkg.cmake"
cmake --build build --config Release
```
Executável em `build\bin\Release\Mousedroid.exe`. Para empacotar, `release.bat`.

---

## 3. Erros comuns e o que significam

| Sintoma | Causa | Solução |
|---|---|---|
| `SDK location not found` | nem `ANDROID_HOME` nem `local.properties` | Opção A ou B da §1.1 |
| `./gradlew: Permission denied` | clone anterior à Missão 04 | `chmod +x gradlew` (ou `bash ./gradlew`) |
| `Could not find wxWidgets` no Linux | APT do Ubuntu 24.04 traz wx 3.2 | compile 3.3.x (o CI faz isso) ou use `-DwxWidgets_ROOT_DIR` |
| `fatal error: asio.hpp: No such file` | falta `libasio-dev` | `sudo apt install libasio-dev` |
| `Não foi possível abrir /dev/uinput` | regra udev ausente | `sudo ./install.sh`, depois relogar |
| `use of undeclared identifier 'make_address'` | Asio antiga demais | Asio ≥ 1.28 (o código foi corrigido na fundação) |
| Mouse não mexe no Wayland | limitação conhecida de uinput/sessão | use sessão X11 |
| Ícone da bandeja não aparece no Linux | limitação de Wayland | use X11 |

---

## 4. O que **não** fazer

| Não faça | Por quê |
|---|---|
| Commitar `local.properties` | contém caminho absoluto do seu SDK; ignorado pelo Git |
| Commitar `client/.idea/` inteiro | 4 arquivos são estado de máquina e estão ignorados por padrão desde a Missão 04 |
| Commitar `client/.gradle/` | cache regenerado a cada build |
| Adicionar `*.exe` ao `.gitignore` | apagaria `server/adb/adb.exe` do controle de versão |
| Rodar `git add -f` em arquivo ignorado sem necessidade | reintroduz exatamente o problema que a Missão 04 removeu |
| Instalar Gradle globalmente | o wrapper fixa a versão (8.11.1) e é o único caminho validado |
