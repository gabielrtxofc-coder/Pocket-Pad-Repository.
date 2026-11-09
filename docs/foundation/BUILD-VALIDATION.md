# Validação de build da fundação Mousedroid

## 1. Escopo e resultado executivo

- **Data:** 2026-11-08 (UTC).
- **Baseline validado:** `3d077786c4ab333f648fc3c2b2ad668203af09b2` (`darusc/Mousedroid`, `master`, `v1.5`).
- **Branch de trabalho:** `arena/39232774-pocket-pad-repository`.
- **Workflow reproduzível:** `.github/workflows/foundation-build.yml`.
- **Execução principal:** [GitHub Actions 42524050240](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42524050240), no commit `a4347745a088f22e5d6085947d0dd31068157ae6`.
- **Execução de diagnóstico:** [GitHub Actions 42522680658](https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository./actions/runs/42522680658).

| Componente | Resultado comprovado |
|---|---|
| Android — configuração, compilação e APK debug | **Passou** |
| Android — teste unitário local | **Passou: 1 teste, 0 falhas, 0 erros** |
| Android — teste instrumentado em emulador API 34 | **Passou: 1 teste, 0 falhas, 0 erros** |
| Servidor Linux — configuração e compilação | **Passou** |
| Servidor Linux — testes CTest | Comando executado; o projeto não registra testes CTest |
| Servidor Windows — configuração CMake | **Passou** |
| Servidor Windows — configuração e compilação | **Passou após a correção mínima de compatibilidade Asio** |

A fundação é comprovadamente compilável para **Android, Linux e Windows** no ambiente CI descrito. A execução principal terminou com `status=completed` e `conclusion=success` nos quatro jobs. Não houve alteração de protocolo, interface, versões de dependências do aplicativo ou arquitetura. A única mudança de código foi a correção mínima de compatibilidade Asio documentada na seção 8. Nenhum resultado foi simulado.

## 2. Auditoria do ambiente local Arena

### Sistema

```text
Sistema: Debian GNU/Linux 12 (bookworm)
Kernel: Linux 6.1.158+ x86_64
Arquitetura de pacotes: amd64
CPUs: 2
Memória: 3,8 GiB
Espaço livre inicial: aproximadamente 20 GiB
Usuário: uid=1001(user), sudo sem senha
```

### Ferramentas inicialmente disponíveis

| Ferramenta | Versão/estado |
|---|---|
| GCC/G++ | 12.2.0 (`12.2.0-14+deb12u1`) |
| GNU Make | 4.3 |
| Python | 3.11.2 |
| Git, curl, wget, tar, unzip | Disponíveis |
| `build-essential` | 12.9 |

### Ferramentas inicialmente ausentes

Java/JDK e `javac`, Android SDK, Build Tools, ADB, emulador, `sdkmanager`, Gradle, CMake/CTest, Ninja, Clang, `pkg-config`, wxWidgets e Asio.

### Provisionamento local tentado

Foram instalados em `/tmp/pocket-pad-tools`, fora do Git:

| Pacote | Versão | Resultado |
|---|---|---|
| CMake via PyPI | 3.26.4 | Funcional; compatível com `cmake_minimum_required(VERSION 3.26)` |
| Ninja via PyPI | 1.13.2 | Funcional |
| `jdk4py` / Temurin runtime | 17.0.9 | Java funcional, mas o pacote contém runtime, **não JDK completo nem `javac`** |

O download dos binários oficiais Temurin 17.0.21 e CMake 3.26.6 pelo GitHub Releases foi tentado, mas o redirecionamento para `release-assets.githubusercontent.com` foi bloqueado pela rede. O runtime local foi suficiente para testar o bootstrap do Gradle Wrapper, não para validar um build Android.

### Limitações de rede locais

- `deb.debian.org`: atualização do APT falhou por conexão indisponível.
- `services.gradle.org`: handshake TLS encerrado ao baixar Gradle 8.11.1.
- `dl.google.com`, `maven.google.com` e `repo.maven.apache.org`: conexões bloqueadas.
- GitHub API/código e PyPI: acessíveis; hosts secundários de logs/artifacts do Actions e release assets: bloqueados.

Essas restrições impediram uma compilação completa dentro do sandbox, mas não impediram a validação em runners GitHub independentes e reproduzíveis.

## 3. Versões exigidas e compatibilidade

### Android

| Componente | Configuração original | Ambiente validado |
|---|---|---|
| Java | `JavaVersion.VERSION_17`, `jvmTarget = '17'` | Temurin OpenJDK **17.0.21** |
| Gradle Wrapper | **8.11.1** | **8.11.1**, sem alteração |
| Android Gradle Plugin | **8.9.1** | 8.9.1, sem alteração |
| Kotlin Android | **2.1.0** | 2.1.0, sem alteração |
| `compileSdk` / `targetSdk` | **34 / 34** | Plataforma Android **34** |
| `minSdk` | **28** (Android 9+) | Inalterado |
| Build Tools | Não fixado no `build.gradle`; padrão do AGP | **35.0.0** |
| SDK Manager | Ferramenta de ambiente | **22.0** |
| Testes | JUnit 4.13.2; AndroidX JUnit 1.1.5; Espresso 3.5.1 | Versões originais resolvidas pelo Gradle |

As demais dependências permaneceram exatamente como no baseline: Gson 2.10.1, Core KTX 1.8.0, AppCompat 1.4.1, Material 1.5.0, ConstraintLayout 2.1.3, Navigation 2.7.7 e Coroutines Android 1.3.9.

### Servidor C++

| Componente | Exigência/configuração original | Ambiente validado |
|---|---|---|
| Linguagem | C++17 | C++17 |
| CMake | **3.26+** | 3.26.4 local; **3.31.6** no runner Linux |
| Compilador Linux | README: GCC 11+ ou Clang | GCC/G++ 12.2 local; **GCC/G++ 13.3.0** no runner |
| wxWidgets | README: família **3.3** | Release oficial **3.3.4**, compilada no runner Linux |
| Asio | Standalone, sem versão fixada | **1.28.1** no Linux; `asio:x64-windows` no Windows |
| Windows | Visual Studio 2022/2026, vcpkg | `windows-latest`, Visual Studio detectado por `vswhere`, vcpkg |
| ADB | Linux: sistema; Windows: binários incluídos | Pacote `adb` no Linux; arquivos originais preservados |

A escolha de wxWidgets 3.3.4 evita substituir silenciosamente a exigência 3.3 pelo wxWidgets 3.2 disponível no APT do Ubuntu 24.04.

## 4. Ambiente reproduzível em GitHub Actions

O workflow possui quatro jobs:

1. `android`: JDK 17, SDK 34, testes unitários e `assembleDebug`;
2. `android-instrumented`: emulador Android API 34 e `connectedDebugAndroidTest`;
3. `server-linux`: CMake/Ninja, Asio, wxWidgets 3.3.4 e compilação Linux;
4. `server-windows`: MSVC x64, vcpkg, configuração e compilação Windows.

Ele é executado em pushes da branch de sessão que alterem o workflow, `client/**` ou `server/**`, e também admite `workflow_dispatch`. Artifacts separam APK, relatórios Android e binários de servidor. O workflow contém `permissions: contents: read`.

### Comandos Android

```bash
# No runner, o caminho local do autor é substituído somente no arquivo efêmero:
cd client
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

bash ./gradlew --version
bash ./gradlew testDebugUnitTest assembleDebug --no-daemon --stacktrace

# Em job separado, com emulador API 34:
bash ./gradlew connectedDebugAndroidTest --no-daemon --stacktrace
```

### Comandos Linux

```bash
# Dependências de ambiente instaladas pelo APT do runner:
cmake ninja-build pkg-config adb libasio-dev
libgtk-3-dev libgl1-mesa-dev libglu1-mesa-dev
libcurl4-openssl-dev libsecret-1-dev libnotify-dev libxtst-dev

# wxWidgets oficial 3.3.4:
cmake -S wxWidgets-3.3.4 -B wx-build -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DwxBUILD_SAMPLES=OFF -DwxBUILD_DEMOS=OFF -DwxBUILD_TESTS=OFF
cmake --build wx-build --parallel 2
cmake --install wx-build

# Servidor:
cmake -S server -B build/server-linux -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build/server-linux --parallel 2 --verbose
ctest --test-dir build/server-linux --output-on-failure
file build/server-linux/bin/Mousedroid
ldd build/server-linux/bin/Mousedroid
```

### Comandos Windows

```powershell
vcpkg install asio:x64-windows wxwidgets:x64-windows
cmake -S server -B build/server-windows -A x64 `
  -DCMAKE_TOOLCHAIN_FILE="$env:VCPKG_INSTALLATION_ROOT/scripts/buildsystems/vcpkg.cmake"
cmake --build build/server-windows --config Release --parallel 2 --verbose
ctest --test-dir build/server-windows -C Release --output-on-failure
```

Como o nome do repositório termina em ponto, o checkout padrão do GitHub é inválido no Windows. O job usa clone Git público em `D:\a\mousedroid-work`; isso altera apenas o diretório de trabalho do runner.

## 5. Resultados Android

### Build host e testes unitários

Na execução principal, o job `Android unit tests and debug APK` concluiu com sucesso as etapas de JDK, SDK, Gradle, testes, compilação e publicação dos artifacts.

Anotações verificáveis pela API do job `128863549391`:

```text
Android tool versions:
openjdk version "17.0.21" 2026-10-20; sdkmanager 22.0; Gradle 8.11.1

Android unit tests:
tests=1, failures=0, errors=0, skipped=0, suites=1
```

Resultado:

- resolução das dependências: passou;
- configuração Gradle: passou;
- `testDebugUnitTest`: passou;
- `assembleDebug`: passou;
- APK `client/app/build/outputs/apk/debug/app-debug.apk`: gerado e publicado;
- artifact `mousedroid-debug-apk`: aproximadamente 4,86 MB comprimidos;
- artifact `android-test-reports`: publicado.

O teste unitário existente é apenas o exemplo upstream `ExampleUnitTest.kt`, que verifica `2 + 2 = 4`.

### Teste instrumentado

As execuções 42521368478, 42522680658 e 42524050240 concluíram `connectedDebugAndroidTest` com sucesso em emulador Android API 34, incluindo a publicação de `android-instrumented-test-reports`. Na execução principal, job `128864194591`, a anotação confirma:

```text
tests=1, failures=0, errors=0, skipped=0, suites=1
```

O teste existente é `ExampleInstrumentedTest.kt`, que verifica o nome do pacote `com.darusc.mousedroid`. Não foram acrescentados testes ou funcionalidades.

## 6. Resultados do servidor Linux

O Linux foi compilado com sucesso nas execuções 42520047725, 42520776381, 42521368478, 42522680658 e 42524050240, demonstrando repetição independente e também validando a chamada `make_address` com Asio 1.28.1.

Na execução principal, job `128863549324`, a anotação de ambiente registra:

```text
cmake version 3.31.6; c++ (Ubuntu 13.3.0-6ubuntu2~24.04.1) 13.3.0; wxWidgets 3.3.4; Asio 1.28.1
```

Nesse job:

- instalação das dependências: passou;
- wxWidgets 3.3.4: compilado e instalado;
- configuração CMake: passou;
- compilação: passou;
- CTest: comando passou; não há testes C++ registrados no `CMakeLists.txt`;
- `file` e `ldd`: executados com sucesso;
- `build/server-linux/bin/Mousedroid`: gerado e publicado;
- artifact `mousedroid-linux-server`: **163.233 bytes comprimidos**.

Isso comprova geração do executável Linux, não sua interação funcional com dispositivos ou GUI.

## 7. Resultados do servidor Windows

Na execução final 42524050240, job `128863549119`, todas as etapas passaram:

| Etapa | Resultado |
|---|---|
| Clone em caminho Windows válido | Passou |
| Detecção de CMake 4.4.5, Visual Studio 18.11 e vcpkg | Passou |
| `vcpkg install asio:x64-windows wxwidgets:x64-windows` | Passou |
| Configuração CMake x64 | Passou |
| Compilação com a correção `make_address` | **Passou** |
| CTest | Comando passou; não há testes registrados |
| Publicação de `windows-compile-log` | Passou; 2.306 bytes comprimidos |
| Publicação de `Mousedroid.exe` | Passou |

O artifact `mousedroid-windows-server` tem **108.396 bytes comprimidos** e contém `build/server-windows/bin/Release/Mousedroid.exe`. Isso comprova geração do executável Windows, não teste funcional da GUI ou injeção de entrada.

Artifacts verificados pela API na execução final:

| Artifact | ID | Tamanho comprimido |
|---|---:|---:|
| `mousedroid-debug-apk` | 13633599702 | 4.863.491 bytes |
| `android-test-reports` | 13633863954 | 5.822 bytes |
| `android-instrumented-test-reports` | 13634138842 | 5.212 bytes |
| `mousedroid-linux-server` | 13634822139 | 163.233 bytes |
| `mousedroid-windows-server` | 13634214316 | 108.396 bytes |
| `windows-compile-log` | 13634483324 | 2.306 bytes |

O diagnóstico da falha anterior e sua correção estão registrados na seção 8. Antes disso, falhas de infraestrutura foram distinguidas da incompatibilidade de código:

1. `actions/checkout` não aceita o workspace padrão com ponto final;
2. `cl` não está no `PATH` do PowerShell padrão — substituído apenas para telemetria por `vswhere`;
3. após essas correções, a configuração passou e a compilação chegou a ser executada.

## 8. Erros, diagnósticos e correções mínimas

### Cronologia das execuções

| Run | Android | Linux | Windows |
|---|---|---|---|
| 42520047725 | Falhou no setup Android v3, antes do Gradle | **Passou** | Checkout inválido pelo ponto final |
| 42520776381 | `sdkmanager` ausente no `PATH`, exit 127 | **Passou** | Mesmo problema de checkout |
| 42521072986 | **Build e teste unitário passaram** | **Passou** | Clone passou; telemetria `cl` falhou antes do vcpkg |
| 42521368478 | **Build, teste unitário e instrumentado passaram** | **Passou** | vcpkg/CMake passaram; compilação falhou |
| 42522680658 | **Build, teste unitário e instrumentado passaram** | **Passou** | vcpkg/CMake passaram; erros Asio capturados |
| 42524050240 | **Build, teste unitário e instrumentado passaram** | **Passou com `make_address`** | **Compilação e artifact passaram; run inteiro verde** |

### Diagnóstico Windows da execução 42522680658

O passo de compilação gravou `windows-compile.log`, publicou o arquivo como artifact de 1.682 bytes e converteu as linhas relevantes em anotações. O job `128859405809` registrou:

```text
server/src/net/server.cpp(35,50): error C2039:
'from_string': is not a member of 'asio::ip::address'

server/src/net/server.cpp(35,50): error C3861:
'from_string': identifier not found
```

A anotação de ambiente do mesmo job confirma:

```text
cmake version 4.4.5
Visual Studio 18.11.12328.437
vcpkg package management program version 2026-10-29-139f2e53438b1529e178f6295bc563f4e0de734f
```

O port vcpkg consultado declara **Asio 1.38.2**. O histórico oficial do Asio mostra que o commit `13f0683879b127c6d679c682cf0a726f9f8dd643`, de 2024-10-21, removeu os métodos depreciados `ip::address::from_string`. O patch desse commit também mostra que esses métodos apenas chamavam `asio::ip::make_address`.

### Correção mínima de compatibilidade

Foi alterada somente a linha 35 de `server/src/net/server.cpp`:

```diff
-socket.connect(udp::endpoint(asio::ip::address::from_string("8.8.8.8"), 80));
+socket.connect(udp::endpoint(asio::ip::make_address("8.8.8.8"), 80));
```

Justificativa: é a substituição oficial indicada pelo Asio, indispensável para compilar com a versão atual do vcpkg e compatível com Asio 1.28.1 usado no Linux. O endereço `8.8.8.8`, a porta 80, o protocolo e o comportamento permanecem inalterados. Commit isolado: `a4347745a088f22e5d6085947d0dd31068157ae6`. A validação completa foi repetida na execução 42524050240 e **Linux e Windows compilaram com sucesso**.

### Correções de ambiente realizadas

Somente no `.github/workflows/foundation-build.yml`:

- provisionamento Android atualizado para `android-actions/setup-android@v4.0.4`;
- SDK 34 e Build Tools 35.0.0 configurados;
- caminho de `client/local.properties` sobrescrito **apenas no runner**;
- checkout Windows substituído por clone em `D:\a\mousedroid-work`;
- consulta de Visual Studio feita por `vswhere`, sem exigir `cl` no `PATH`;
- logs, versões e totais de testes expostos como artifacts/anotações;
- emulador API 34 adicionado para o teste upstream já existente.

Além da substituição de uma chamada Asio em `server/src/net/server.cpp`, nenhum arquivo de `client/`, `server/`, licença ou modo executável foi alterado.

## 9. Auditoria das configurações originais

| Problema | Local | Impacto | Solução futura possível | Gravidade |
|---|---|---|---|---|
| SDK Android absoluto do autor | `client/local.properties:8`: `C:\Users\Darius\AppData\Local\Android\Sdk` | Impede portabilidade e pode apontar para SDK inexistente | Não versionar `local.properties`; ignorá-lo e usar `ANDROID_HOME` ou arquivo local não rastreado | **Alta** |
| `local.properties` não ignorado | `client/.gitignore` não contém a regra | Caminhos de outras máquinas podem ser commitados | Adicionar regra específica e remover o arquivo do índice em missão própria | **Alta** |
| Wrapper sem bit executável | `client/gradlew`, modo Git `100644` | `./gradlew` falha por permissão no Unix; `bash ./gradlew` funciona | Commit apenas do modo `100755` | Média |
| Scripts shell sem bit executável | `server/install.sh` e `server/release.sh`, `100644` | Execução direta falha; README pede `chmod +x` | Registrar modo `100755` ou documentar `bash script.sh` | Média |
| IDE configurada para JDK 21 | `client/.idea/misc.xml` (`jbr-21`) e `compiler.xml` (`target=21`) | Pode divergir do JDK 17 exigido pelo Gradle | Padronizar IDE para JDK 17 ou esclarecer que JBR 21 apenas hospeda o Gradle | Média |
| Caminho absoluto em configuração de IDE | `client/.idea/assetWizardSettings.xml`: `D:\Projects\Mousedroid\client` | Afeta assistentes do Android Studio, não o build CLI validado | Remover configuração local do índice em missão de limpeza | Baixa |
| Metadados locais versionados | `client/.gradle/` e `client/.idea/caches/` | Poluição e possíveis configurações específicas de máquina | Revisar `.gitignore` e índice em missão separada | Média |
| Toolchains Windows divergentes | README/CMakeSettings usam MSVC/vcpkg; `.vscode/tasks.json` usa MinGW | Dois caminhos de build podem produzir resultados diferentes | Escolher e documentar toolchain suportada após validação Windows | Média |
| Configuração CMake parcialmente dependente de ambiente | `ASIO_PATH` em `server/CMakeLists.txt`; includes de sistema no Linux | Um `ASIO_PATH` incorreto pode quebrar includes; sistema validado funciona | Documentar variável ou modelar dependência imported target em missão futura | Baixa |
| API Asio depreciada e removida | `server/src/net/server.cpp:35` usava `address::from_string` | Compilação falha com Asio 1.38.2/vcpkg atual | Substituir por `make_address`; correção mínima realizada e validada no CI | **Alta para Windows** |
| Nome do destino com ponto final | `Pocket-Pad-Repository.` | Workspace padrão inválido no Windows e checkout Actions falha | Preferencialmente renomear destino sem ponto, com decisão do mantenedor; workaround já no CI | **Alta para Windows** |

Esses problemas são preexistentes, exceto o nome com ponto final do novo repositório de destino. Nenhuma limpeza foi feita nesta missão.

## 10. Limitações remanescentes

- Não houve teste funcional de mouse, teclado, Bluetooth, Wi-Fi ou USB/ADB com hardware real.
- A GUI e a injeção de entrada não foram exercitadas; runners são automatizados e não representam uma sessão desktop real.
- O APK validado é `debug`; não foi produzido ou assinado APK release.
- Não existe suíte CTest no baseline; o sucesso do comando não deve ser interpretado como testes C++ executados.
- Os testes Android upstream são apenas dois testes-exemplo, não cobertura funcional do protocolo.
- O sandbox não consegue baixar logs/artifacts dos hosts secundários do Actions; metadados, estados, artifacts e anotações foram verificados pela API GitHub.
- Ações `checkout@v4`, `setup-java@v4`, `setup-gradle@v4` e `upload-artifact@v4` emitem aviso de depreciação do Node 20; não afetaram os resultados, mas devem ser atualizadas em manutenção futura.

## 11. Recomendações para a Missão 03

1. Preservar a correção Asio e manter o workflow verde como gate de Android, Linux e Windows antes de qualquer transformação Pocket Pad.
2. Executar os builds novamente após qualquer mudança de dependência ou toolchain, especialmente atualizações de Asio, wxWidgets e vcpkg.
3. Corrigir portabilidade local em mudanças separadas: `local.properties`, `.gitignore` e bits executáveis.
4. Decidir se o repositório será renomeado sem ponto final para eliminar o workaround Windows.
5. Padronizar documentação IDE/toolchain para JDK 17 e MSVC/vcpkg ou justificar alternativas.
6. Atualizar ações de CI depreciadas sem alterar dependências do Mousedroid.
7. Só depois de builds verdes, planejar testes funcionais em hardware e uma suíte de testes real do protocolo/servidor.

## 12. Integridade do escopo

Contra o upstream, `client/`, `LICENSE` e `README.md` permanecem sem alterações. Em `server/`, a única mudança é a substituição documentada da chamada Asio em `src/net/server.cpp:35`. As demais mudanças da Missão 02 limitam-se ao workflow de validação e a este relatório. Não houve merge do PR #1, alteração da `main`, force push ou modificação do repositório antigo.
