# Mapa de dependências — Pocket Pad (fundação Mousedroid v1.5)

> Missão 03. Dependências identificadas por leitura de `app/build.gradle`, `client/build.gradle`,
> `settings.gradle`, `CMakeLists.txt`, `server/README.md`, `install.sh`, `.github/workflows/foundation-build.yml`
> e dos `import` efetivamente usados no código.
> Versões foram conferidas no repositório; licenças são **indicativas** e não constituem revisão jurídica.

Legenda de importância: **Crítica** (sem ela não compila/roda) · **Alta** · **Média** · **Baixa**.

---

## 1. Classificação geral

| Categoria | Itens |
|---|---|
| **Obrigatórias** | AGP, Kotlin, Gradle Wrapper, AndroidX core-ktx, androidx.fragment, lifecycle, navigation, coroutines, Material Components, ConstraintLayout, C++17, CMake ≥ 3.26, Asio, wxWidgets, ADB (para USB), libc/uinput (Linux), SendInput/user32 (Win32) |
| **Opcionais / de conveniência** | Gson (declarado, **sem uso verificado**), AppCompat (usado como base de Activity), Espresso/AndroidX Test (só testes), `logo.png`/`imgs` (documentação) |
| **Ferramentas de desenvolvimento** | Android Studio (configs `.idea`), Visual Studio + vcpkg, VS Code (`.vscode`), JDK 17, `adb` CLI |
| **Ferramentas de compilação** | Gradle 8.11.1, `kotlin-kapt`, Ninja, MSVC/MinGW, GNU Make, `pkg-config`, GitHub Actions runners |
| **Componentes de execução** | `/dev/uinput` + regra udev, `user32.dll`, `adb.exe` empacotado, wxWidgets DLLs (no release Windows) |
| **Aparentemente abandonados / mortos** | Gson (declarado e nunca importado), `kotlin-kapt` (nenhum processador de anotação), `PacketType` (C++), `Connection::Send`, `BatteryMonitor` (nunca iniciado), `maxPacketSize` (nunca lido), `adapters` de ícones órfãos |

---

## 2. Dependências Android (Gradle)

Declaradas em `client/app/build.gradle:47-58` e `client/build.gradle:2-6`.

| # | Nome | Versão | Localização | Responsabilidade | Módulos que dependem | Plataforma | Importância | Licença (indicativa) | Consequência da remoção |
|---|---|---|---|---|---|---|---|---|---|
| A1 | **Android Gradle Plugin** | 8.9.1 | `client/build.gradle:3-4` | Compilar o módulo Android | todo o `client/` | Host (build) | **Crítica** | Apache-2.0 | Não compila |
| A2 | **Kotlin Android** (`org.jetbrains.kotlin.android`) | 2.1.0 | `client/build.gradle:5` | Compilar Kotlin | 37 arquivos `.kt` | Host (build) | **Crítica** | Apache-2.0 | Não compila |
| A3 | **kotlin-kapt** | (bundled no Kotlin 2.1.0) | `app/build.gradle:4` | Processamento de anotações | — | Host (build) | **Baixa** | Apache-2.0 | Nenhum impacto observável: **não há `kapt` nem `@Entity`/Room/Dagger** no código. Candidato a remoção futura com revalidação de build |
| A4 | **Gradle Wrapper** | 8.11.1 | `gradle/wrapper/` | Bootstrap do build | tudo | Host (build) | **Crítica** | Apache-2.0 | Build não reproduzível |
| A5 | **androidx.core:core-ktx** | 1.8.0 | `app/build.gradle:50` | Extensões Kotlin do core | `isGone`, `edit{}`, `ContextCompat` | Android | **Crítica** | Apache-2.0 | Falha de compilação |
| A6 | **androidx.appcompat:appcompat** | 1.4.1 | `:51` | Base `AppCompatActivity` | `MainActivity` | Android | **Crítica** | Apache-2.0 | Não compila |
| A7 | **androidx.fragment:fragment-ktx** | (transitivo via navigation 2.7.7) | transitiva | `Fragment`, `activityViewModels` | todos os fragments | Android | **Crítica** | Apache-2.0 | Não compila |
| A8 | **androidx.lifecycle** (viewmodel-ktx / runtime-ktx) | transitiva | transitiva | `ViewModel`, `viewModelScope`, `repeatOnLifecycle` | 6 viewmodels + 4 fragments | Android | **Crítica** | Apache-2.0 | Não compila |
| A9 | **androidx.navigation:navigation-fragment-ktx** | 2.7.7 | `:54` | NavHost + `findNavController` | `Main`, `DeviceList`, `Input`, `activity_main.xml` | Android | **Crítica** | Apache-2.0 | Quebra a navegação e o build |
| A10 | **com.google.android.material:material** | 1.5.0 | `:52` | Material3, `MaterialButton`, `NavigationView`, `MaterialAlertDialogBuilder` | layouts, `Input.kt`, `DeviceList.kt` | Android | **Crítica** | Apache-2.0 | Não compila; tema depende de Material3 |
| A11 | **androidx.constraintlayout** | 2.1.3 | `:53` | `ConstraintLayout` | 11 layouts | Android | **Crítica** | Apache-2.0 | Layouts não inflam |
| A12 | **kotlinx-coroutines-android** | **1.3.9** | `:58` | `Dispatchers.IO`, `Channel`, `delay`, `launch` | `ConnectionManager`, `BluetoothConnection`, `GestureHandler` | Android | **Crítica** | Apache-2.0 | Não compila |
| A13 | **com.google.code.gson:gson** | 2.10.1 | `:49` | Serialização JSON | **NENHUM** — `grep -rn "gson" client/app/src` retorna 0 ocorrências | Android | **Baixa** | Apache-2.0 | **Removível sem impacto funcional** (deve ser confirmado com build verde). Provável resíduo de um protocolo JSON anterior |
| A14 | **androidx.databinding** (buildFeature) | do AGP | `:42-44` | `DataBindingUtil`, classes `*Binding` | 5 fragments | Android | **Crítica** | Apache-2.0 | Não compila |
| A15 | **junit:junit** | 4.13.2 | `:55` | Teste unitário | `ExampleUnitTest` | Host (test) | Baixa | EPL-1.0 | Perde o único teste local (que é um exemplo) |
| A16 | **androidx.test.ext:junit** | 1.1.5 | `:56` | Teste instrumentado | `ExampleInstrumentedTest` | Android (test) | Baixa | Apache-2.0 | Perde o único teste instrumentado |
| A17 | **androidx.test.espresso:espresso-core** | 3.5.1 | `:57` | UI testing | **não usado** — nenhum `import androidx.test.espresso` em `client/` (verificado) | Android (test) | **Baixa** | Apache-2.0 | Sem impacto se os testes-exemplo forem substituídos |

### 2.1 Observações críticas nas dependências Android

1. **coroutines 1.3.9 é de 2020** (versão atual da série 1.x é muito superior). Funciona, mas `Channel`/`Flow`/`Dispatchers` têm correções relevantes em versões posteriores. Não é motivo para remoção — é motivo para **atualização planejada e testada**.
2. **Material 1.5.0 + AppCompat 1.4.1 + core-ktx 1.8.0 + ConstraintLayout 2.1.3** são versões de 2021–2022 enquanto AGP 8.9.1/Kotlin 2.1.0 são de 2025. O conjunto funciona (CI verde), mas é uma combinação heterogênea; atualizações devem ser feitas em bloco e com o CI como gate.
3. **Gson 2.10.1 não é usado.** Remover reduz superfície de ataque e tamanho do APK.
4. `dependenciesInfo { includeInApk = false; includeInBundle = false }` (`:37-40`) suprime o bloco de metadados de dependências do APK — decisão informada do autor (privacidade), a manter.

### 2.2 Repositórios

`client/settings.gradle:1-14` — `google()`, `mavenCentral()`, `gradlePluginPortal()` com `RepositoriesMode.FAIL_ON_PROJECT_REPOS`. Configuração correta e determinística.

---

## 3. Dependências do servidor C++

Declaradas em `server/CMakeLists.txt` e documentadas em `server/README.md`.

| # | Nome | Versão | Localização | Responsabilidade | Módulos que dependem | Plataforma | Importância | Licença (indicativa) | Consequência da remoção |
|---|---|---|---|---|---|---|---|---|---|
| S1 | **CMake** | ≥ 3.26 (`CMakeLists:1`) | build | Sistema de build | todo o `server/` | Host | **Crítica** | BSD-3 | Não configura |
| S2 | **Compilador C++17** | GCC 11+ / Clang / MSVC 2022-2026 | README, CI | Compilar | todo o `server/` | Host | **Crítica** | — | Não compila |
| S3 | **Asio (standalone)** | 1.28.1 (Linux CI) / `asio` do vcpkg 1.38.2 (Windows) | `CMakeLists:15,35,63,65`; `ASIO_PATH` no Linux | Rede assíncrona (TCP/UDP, io_context) | `net/server.*`, `net/connection.*` | Linux + Windows | **Crítica** | Boost Software License 1.0 | Sem rede. A versão **precisa** ser ≥ a que possui `asio::ip::make_address` (correção da Missão 02 em `server.cpp:35`) |
| S4 | **wxWidgets** | 3.3 (família); 3.3.4 no CI | `CMakeLists:16,30,54-57` | GUI, tray icon, listas | `gui/*`, `wxapplication.*`, `logger.*` (inclui `wx/wx.h`) | Linux + Windows | **Crítica** (para o produto atual) | wxWindows Library Licence (LGPL-2 + exceção) | Sem GUI o app não sobe. **Atenção:** `logger.h:3` inclui `wx/wx.h`, então *todo* o servidor depende de wxWidgets, mesmo o núcleo de rede. Um servidor headless exigiria desacoplar o logger |
| S5 | **libstdc++ / STL** | do compilador | implícito | `thread`, `vector`, `stringstream`, `map` | todos | Linux + Windows | **Crítica** | GPL-with-runtime-exception (libstdc++) | Não compila |
| S6 | **pthread** (implícito) | do sistema | `std::thread` em `server.cpp:51` | Thread do `io_context` | `Server` | Linux | **Crítica** | — | Não linka/não roda |
| S7 | **`ws2_32`, `wsock32`** | sistema Windows | `CMakeLists:19` | Winsock | Asio | Windows | **Crítica** | — | Não linka |
| S8 | **`user32`** | sistema Windows | `CMakeLists:19` | `SendInput` | `windowsinputmanager.cpp` | Windows | **Crítica** | — | Não linka |
| S9 | **`/dev/uinput` + kernel input subsystem** | kernel Linux | `linuxinputmanager.cpp:13` | Injeção de entrada | `InputManager::Linux` | Linux | **Crítica** | — (kernel) | Sem controle do mouse |
| S10 | **udev** (`/etc/udev/rules.d/50-uinput.rules`, TAG `uaccess`) | sistema | `install.sh:27-30` | Permissão de escrita em `/dev/uinput` | `InputManager::Linux` | Linux | **Alta** | — | App roda mas não movimenta o mouse (erro em `:15-17`) |
| S11 | **ADB** | vcpkg/apt/GitHub runner (`adb`) | `settingsmanager.cpp`, `adb.hpp` | Port forward reverso (USB) | modo USB | Linux + Windows | **Alta** (só USB) | Android SDK / Apache-2.0 (adb) | Modo USB deixa de funcionar; Wi-Fi e BT seguem |
| S12 | **`server/adb/adb.exe` + 2 DLLs** | binários (tamanhos em BASELINE §4) | `server/adb/` | ADB no Windows | `adb.hpp` | Windows | **Alta** (só USB) | **Não verificada** — redistribuição de ferramentas do Android SDK | Modo USB no Windows para de funcionar. **5,9 MB** = 84% do repositório |
| S13 | **vcpkg** | CI usa o do runner | CI/README | Gerenciar Asio/wx no Windows | build Windows | Host (Windows) | **Alta** | MIT | Build Windows manual |
| S14 | **Ninja** | CI | CI/`CMakeSettings.json` | Gerador | build | Host | Média | Apache-2.0 | Usar Make/MSBuild |
| S15 | **`pkg-config` + GTK/GL dev libs** | apt no CI | CI | Dependências de link do wxWidgets | wx | Linux (build) | **Alta** | — | Configuração do CMake falha |

### 3.1 Observações críticas no servidor

1. **`find_package(wxWidgets REQUIRED net gl core base adv)`** exige os componentes `gl` e `net` do wx — `net` (wxSocket) **não é usado** no código (a rede é Asio). Reduzir para `core base adv` encurtaria dependências, mas precisa de validação.
2. **`ASIO_PATH`** (`CMakeLists:35`): no Linux o Asio é encontrado por variável manual `-DASIO_PATH`; se não definida, `ASIO_INCLUDE_DIR="/include"` e o build falha. O CI contorna instalando `libasio-dev` (headers em `/usr/include`).
3. **wxWidgets 3.3 não está no APT do Ubuntu 24.04** (que traz 3.2) — por isso o CI compila 3.3.4 do fonte (`BUILD-VALIDATION §3`). Isso é uma dependência de build **pesada** e um risco de longo prazo.
4. **Dupla definição de `PLATFORM_SRC`/`PLATFORM_LIBS`** em `CMakeLists:22-27` e `41-46`: a segunda sobrescreve a primeira. Consequência: no MSVC, `resource.rc` **pode não ser incluído** na lista final de fontes, afetando o ícone do `.exe`. Precisa de verificação; documentado em `OPEN-QUESTIONS.md` Q5.

---

## 4. Dependências de ambiente / CI

| Item | Versão | Onde | Observação |
|---|---|---|---|
| JDK | 17 (Temurin 17.0.21 validado) | CI, README | `sourceCompatibility/targetCompatibility 17` |
| Android SDK Platform | 34 | CI | `compileSdk/targetSdk 34` |
| Android Build Tools | 35.0.0 | CI | não fixado no Gradle |
| Emulador Android | API 34 | CI (job instrumentado) | exige KVM no runner |
| `setup-android` | v4.0.4 | CI | |
| `actions/checkout` | v4 | CI | emite aviso de deprecação Node 20 |
| `actions/setup-java` | v4 | CI | idem |
| `gradle/actions/setup-gradle` | v4 | CI | idem |
| `actions/upload-artifact` | v4 | CI | idem |
| Ubuntu runner | 24.04 | CI | GCC 13.3.0 |

---

## 5. Dependências implícitas no código (não declaradas em build)

Estas são acoplamentos que a documentação normalmente omite e que importam para o Pocket Pad:

| Dependência implícita | Onde aparece | Risco |
|---|---|---|
| **Porta 6969** | 6 pontos em 4 arquivos: `wxapplication.cpp:13`, `settingsmanager.cpp:125,127,144`, `ConnectionViewModel.kt:74`, `DeviceListViewModel.kt:91` | Mudar exige editar 6 pontos; não há constante compartilhada |
| **Layout do protocolo** (opcodes 0x01-0x0A) | `Translators.kt:25-36` ↔ `inputmanager.hpp:22-31` | Duplicação entre linguagens |
| **Formato do handshake** (`a/b/c/d`) | `Utils.kt:25` ↔ `connection.cpp:11-22` | Posicional, sem validação |
| **`8.8.8.8:80`** | `server.cpp:35` | Descoberta de IP local depende de rota externa |
| **`config.ini` no diretório de trabalho** | `settingsmanager.cpp:21,41` | Depende de cwd; não há caminho por usuário |
| **`app.log` no diretório de trabalho** | `logger.cpp:6` | idem |
| **Nome `"Mousedroid"`** | SDP BT (`BluetoothConnection.kt:50-51`), uinput (`linuxinputmanager.cpp:50`), janela (`wxmain.cpp:14`), `.desktop` (`install.sh:17-18`), config Windows Run key (`settingsmanager.cpp:54`) | Identidade dispersa em 6 pontos |
| **`"com.darusc.mousedroid"`** | `namespace`/`applicationId`, Manifest, teste instrumentado, `SharedPreferences("devices")` não é afetado | Rebranding exige migração de dados do usuário |
| **Data binding + nomes de ID de recursos** | 5 fragments ↔ layouts | Qualquer renomeação de ID quebra em tempo de compilação (bom) |
| **`/dev/uinput`** | `linuxinputmanager.cpp:13` | Requer privilégio/udev |

---

## 6. O que **não** é dependência (verificado e descartado)

| Suspeita | Verificação | Resultado |
|---|---|---|
| Room / Dagger / Hilt | `grep -rn "Room|Dagger|Hilt|kapt"` | nenhum uso real |
| Retrofit / OkHttp / Ktor | `grep -rn "retrofit|okhttp|ktor"` | nenhum |
| Firebase / Google Play Services | grep + `build.gradle` | nenhum |
| OpenCV / WebRTC / FFmpeg (streaming) | grep + CMakeLists | nenhum (só o enum morto `PacketType`) |
| Boost completo | CMakeLists | não — apenas Asio standalone |
| Submódulos Git / LFS | `git ls-files` + BASELINE §4 | nenhum |

---

## 7. Consequências de remoção — resumo executivo

| Remoção | Impacto | Recomendação |
|---|---|---|
| Gson 2.10.1 | nenhum (zero imports em `client/app/src`) | **Remover na limpeza** (etapa de baixo risco, com build verde) |
| `kotlin-kapt` | nenhum (nenhum processador) | Remover com revalidação |
| Espresso 3.5.1 | nenhum (não usado) | Remover junto com a substituição dos testes-exemplo |
| `net`/`gl` do `find_package(wxWidgets)` | precisa validação | Avaliar; não remover sem build em ambos SOs |
| `server/adb/*` (3 binários) | perde USB no Windows, reduz 84% do repo | **Não remover** sem decisão de produto |
| wxWidgets | perde a GUI inteira | **Não remover**; considerar desacoplar `logger` como passo preparatório |
| Coroutines / Material / Navigation | perde funcionalidade central | **Não remover** |
