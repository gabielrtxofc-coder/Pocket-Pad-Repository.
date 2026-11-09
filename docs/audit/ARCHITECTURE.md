# Arquitetura do Pocket Pad (fundação Mousedroid v1.5)

> Documento de auditoria — Missão 03. Nenhuma alteração de código foi feita.
> Baseline: `164372b` (= `main`), árvore upstream `3d077786c4ab333f648fc3c2b2ad668203af09b2` (Mousedroid `v1.5`).
> Caminhos e linhas citados foram lidos diretamente do código do repositório.

---

## 1. Visão geral em uma página

O Mousedroid é **um cliente Android** e **um servidor desktop**. Há ainda um **terceiro caminho que dispensa o servidor**: Bluetooth HID.

```text
┌──────────────────────────────┐                       ┌──────────────────────────────┐
│  CLIENTE ANDROID (Kotlin)     │                       │  SERVIDOR DESKTOP (C++17)    │
│                              │                       │                              │
│  MainActivity                │                       │  wxApplication::OnInit       │
│    └ ConnectionManager ──────┼── TCP/UDP :6969 ─────►│  Server(6969)                │
│         ├ TCPConnection      │   (USB via adb       │    ├ tcp::acceptor           │
│         ├ UDPConnection      │    reverse, ou Wi-Fi)│    ├ udp::socket             │
│         └ BluetoothConnection┼── HID over BT ──┐    │    └ Connection (por device) │
│                              │                  │    │         └ INPUT_MANAGER      │
│  UI: Touchpad/Numpad/Teclado  │                  │    │              ├ Linux: uinput│
│  GestureHandler, InputEvent   │                  │    │              └ Win32: SendInput
└──────────────────────────────┘                  │    └ gui/ (wxWidgets)            │
                                                  ▼                                  │
                                    ┌──────────────────────────┐                     │
                                    │  SISTEMA OPERACIONAL DO  │◄────────────────────┘
                                    │  PC (host Bluetooth HID) │
                                    └──────────────────────────┘
```

Três modos de conexão, **dois caminhos de execução distintos**:

| Modo | Android | Servidor C++ | Injeção de entrada |
|---|---|---|---|
| **Wi-Fi** | TCP (handshake) + UDP (eventos) | `Server` TCP+UDP na porta 6969 | `INPUT_MANAGER` |
| **USB/ADB** | TCP em `127.0.0.1:6969` | `Server` + `adb reverse tcp:6969` | `INPUT_MANAGER` |
| **Bluetooth** | HID Device nativo do Android | **não participa** | o próprio PC, via stack HID |

Essa é a decisão arquitetural mais importante da fundação: **Bluetooth não passa pelo servidor** (`ConnectionMode.BLUETOOTH` nunca é enviado ao servidor; `Utils.kt:25` só envia `ordinal` para USB=0/WIFI=1, e o servidor só conhece `enum connection_type {USB=0, WIFI=1}` em `server/src/net/device.h:6-10`).

---

## 2. Arquitetura Android

### 2.1 Componentes e camadas

```text
AndroidManifest.xml
 └ MainActivity (única Activity; exported; configChanges orientation|screenSize|...)
     └ activity_main.xml → NavHostFragment
         └ navigation/navigation.xml
             ├ mainFragment   (fragments/Main.kt)      → escolha do modo
             ├ deviceListFragment (fragments/DeviceList.kt) → escolha do host
             └ inputFragment  (fragments/Input.kt)     → container de entrada
                 ├ Touchpad (childFragment)  ← GestureHandler
                 ├ Numpad   (childFragment)
                 └ Teclado  (EditText oculto + IME + KeyboardInputWatcher)

MVVM: ConnectionViewModel, DeviceListViewModel, TouchpadViewModel,
      KeyboardViewModel, NumpadViewModel, BaseViewModel
Transporte: ConnectionManager (singleton)
             ├ TCPConnection / UDPConnection (sockets)
             └ BluetoothConnection (HID Device)
Modelo: InputEvent (sealed class)
Encoder: Translators.kt (toSocketReport / toHIDReport)
```

### 2.2 Ponto de entrada

- `client/app/src/main/AndroidManifest.xml:21-29` declara **apenas** `com.darusc.mousedroid.MainActivity` como launcher. Não existe `Service`, `BroadcastReceiver` declarado, `AppWidget` ou `ContentProvider`.
- `MainActivity.onCreate` (`MainActivity.kt:22-34`): `setContentView(R.layout.activity_main)` → pede `BLUETOOTH_CONNECT` se `SDK_INT >= S_V2` (API 32) → `BluetoothAdapterWrapper.initialize(applicationContext)`.
- **Não há inicialização de conexão na Activity.** Toda a orquestração acontece nos fragments/viewmodels.
- Observação: o Manifest declara `FOREGROUND_SERVICE` e `WAKE_LOCK` (`AndroidManifest.xml:11-12`) mas **não existe nenhum `Service` no código** (confirmado por varredura). São permissões sem consumidor.

### 2.3 Activities, Services, Interfaces

| Tipo | Quantidade | Arquivos |
|---|---|---|
| `Activity` | 1 | `MainActivity.kt` |
| `Service` | **0** | — |
| `Fragment` | 5 | `Main`, `DeviceList`, `Input`, `Touchpad`, `Numpad` |
| `ViewModel` | 6 | `BaseViewModel` + 5 concretos |
| `BroadcastReceiver` | 1 (registrado em runtime) | `BatteryMonitor.receiver` |
| Interoperabilidade | `Connection.Listener`, `ConnectionManager.ConnectionStateCallback`, `BatteryMonitor.Listener` | `Connection.kt:18-23`, `ConnectionManager.kt:59-64`, `BatteryMonitor.kt:29-31` |

### 2.4 Gerenciamento de permissões

| Permissão | Onde é pedida | Uso real |
|---|---|---|
| `BLUETOOTH_CONNECT` | `MainActivity.kt:26-30` (runtime, API 32+) | `BluetoothConnection`, `Main.kt`, `DeviceList.kt`, `getDeviceDetails` |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | só Manifest (`:6-7`) | legado (≤ API 30) |
| `BLUETOOTH_ADVERTISE` | só Manifest (`:9`) | **sem uso identificado** no código |
| `INTERNET` | Manifest (`:10`) | sockets TCP/UDP |
| `ACCESS_NETWORK_STATE` | Manifest (`:5`) | `hasWifiConnection()` (`NetUtils.kt:19-24`) |
| `FOREGROUND_SERVICE`, `WAKE_LOCK` | Manifest (`:11-12`) | **sem uso identificado** |

Tratamento de negação: `MainActivity.onRequestPermissionsResult` (`:36-59`) mostra um `AlertDialog` com atalho para as configurações do app e re-inicializa o wrapper se concedido.

### 2.5 Caminho completo de uma ação do usuário (rastreado no código)

**Exemplo A — arrastar um dedo na superfície do trackpad (modo Wi-Fi):**

1. `fragment_touchpad.xml` → `FrameLayout @id/touchpadContainer` recebe o listener em `Touchpad.kt:42-45`:
   `GestureHandler(requireContext()) { event -> viewModel.sendMouseEvent(event) }`.
2. `GestureHandler.onTouch` (`GestureHandler.kt:172`) entrega o `MotionEvent` a `GestureDetectorCompat`.
3. `onScroll` (`:118-164`) decide entre **scroll** (2 ponteiros, ou < 150 ms do último scroll) e **movimento** de 1 ponteiro, produzindo `InputEvent.MouseScroll(dx, dy)` ou `InputEvent.MouseMove(dx, dy, button)`, com `coerceIn(-128, 127)` — limite imposto pelo byte do protocolo.
4. `TouchpadViewModel.sendMouseEvent` (`:15-17`) → `ConnectionManager.send(event)`.
5. `ConnectionManager.send` (`:162-171`) escolhe a conexão ativa por prioridade **UDP > TCP > BT** (`:54-55`) e executa em `Dispatchers.IO`.
6. `UDPConnection.send` (`:40-49`): `event.toSocketReport()` → `DatagramPacket` → `socket.send(...)`, com `Thread.sleep(15)` entre relatórios.
7. `Translators.toSocketReport` (`:159-171`) serializa em **3 bytes**: `[0x05, dx, dy]` (`RawSocketEvents.MOVE`).
8. Servidor: `Server::StartReceive` (`server/src/net/server.cpp:122-139`) → `inputmanager.execute(byteBuffer, bytesReceived)`.
9. `InputManager::Base::execute` (`server/src/input/inputmanager.hpp:43-87`) faz `switch(bytes[0])` → `case MOVE: move(-(int)bytes[1], -(int)bytes[2])` (sinal invertido).
10. `InputManager::Linux::move` (`linuxinputmanager.cpp:92-97`) → `emit(fd, EV_REL, REL_X/REL_Y)` gravando em `/dev/uinput`; no Windows, `Windows::move` (`windowsinputmanager.cpp:34-44`) → `SendInput`.
11. O kernel/Windows move o cursor. **Fim do caminho.**

**Exemplo B — toque simples (clique esquerdo):** `onSingleTapUp` (`:83-91`) agenda `singleTapRunnable` com 150 ms de atraso (para permitir o duplo-toque cancelar); ao disparar, `InputEvent.MouseClick(LEFT)` → `[0x01]` → `click()` → `BTN_LEFT 1 + SYN + BTN_LEFT 0 + SYN`.

**Exemplo C — clique direito:** `onTouch` detecta `ACTION_POINTER_UP` com 2 ponteiros e nenhum scroll recente (`GestureHandler.kt:174-185`) → `MouseClick(RIGHT)` → `[0x02]`.

**Exemplo D — teclado (modo Wi-Fi):** `Input.kt:61-67` instala `KeyboardInputWatcher` no `EditText @id/hiddenInput`; `KeyboardInputWatcher.onTextChanged` (`:22-48`) converte DEL/ENTER/caractere em `CharArray`; `KeyboardViewModel.handleKeypress` (`:45-52`) usa `activeKeyboardLayout.getMapping(char)` → lista de `Key(modifier, code)`; `InputEvent.KeyPress` → `[0x07, code, modifier]` por tecla; servidor `send_key(keycode, modifier)` aplica modificadores em máscara e libera em ordem reversa (`linuxinputmanager.cpp:129-158`).

**Exemplo E — Bluetooth (caminho alternativo, sem servidor):** `TouchpadViewModel` → `ConnectionManager.send` → `connection` = `btConn` → `BluetoothConnection.send` (`:160-163`) enfileira no `Channel<Array<HIDReport>>` → job em `Dispatchers.IO` (`:125-134`) chama `bluetoothHIDDevice.sendReport(host, report.id, report.bytes)`. O report é interpretado pelo SO do PC como um mouse/teclado HID real.

### 2.6 Gerenciamento de conexões

`ConnectionManager` é um **singleton com `ConnectionStateCallback` mutável** (`:30-38`): quem chama `getInstance(callback)` por último sobrescreve o callback. `ConnectionViewModel` registra-se no construtor (`:40`).

- **Wi-Fi** (`:96-109`): abre `TCPConnection` (handshake) **e** `UDPConnection` (eventos), envia `deviceDetails` pelo TCP e reporta conectado. Se o TCP falhar, `ConnectionFailedException` → callback de falha.
- **USB** (`:115-127`): `TCPConnection("127.0.0.1", port, isOverAdb=true, ...)`.
- **Bluetooth** (`:132-144`): `registerBluetoothHID(context)` cria `BluetoothConnection` (que registra o app HID no proxy do perfil); `connectBluetooth(mac)` conecta ao host.
- **Desconexão** (`:149-160`): fecha os três e zera as referências. `TCPConnection.onDisconnected` (`:92-96`) infere o modo a partir do host (`127.0.0.1` ⇒ USB).

### 2.7 Entrada de mouse e teclado

- **Mouse/touchpad:** `GestureHandler` (gestos) + `Touchpad.kt` (botões físicos de clique esquerdo/direito/meio, `TouchpadViewModel.onMouseButtonClick`).
- **Drag:** `MouseDragState` (DOWN/UP). Iniciado no duplo-toque seguido de movimento (`GestureHandler.kt:200-225`) — o duplo clique é cancelado e convertido em arrasto — ou, segundo o README, por "long press and drag".
- **Zoom:** `ScaleGestureDetector` (`:42-65`) acumula pixels e emite `Zoom(ticks)` a cada 20 px; traduzido a CTRL+wheel.
- **Teclado:** IME do sistema em `EditText` invisível (não há `InputMethodService` customizado). 4 layouts: US, ES, FR, RO (`layouts/languages/`).
- **Numpad:** botões com `MaterialButton` → `NumpadKeyPress(Keycode.KEYPAD_*)`.
- **Mídia:** 8 botões no trackpad → `MediaEvent` → bitmask consumer control.

### 2.8 Recursos gráficos

80 arquivos XML + 22 PNG (ver `FILE-INVENTORY.tsv`). Tema `Theme.Mousedroid` (Material3 Dark, `values/themes.xml`), tipografia por `TextAppearance` do Material, data binding ativo (`buildFeatures.dataBinding true` em `app/build.gradle:42-44`).

44 drawables; **5 sem nenhuma referência** (`baseline_add_24`, `devicelistitembackground`, `ic_iconmonstr_check_mark_thin`, `ic_iconmonstr_x_mark_thin`, `ic_pause`) e `logo.png` sem referência por id.

### 2.9 Dependências Gradle e configuração de build

| Item | Valor | Arquivo |
|---|---|---|
| AGP | 8.9.1 | `client/build.gradle:3-4` |
| Kotlin | 2.1.0 | `client/build.gradle:5` |
| Gradle Wrapper | 8.11.1 | `gradle/wrapper/gradle-wrapper.properties:3` |
| `namespace` / `applicationId` | `com.darusc.mousedroid` | `app/build.gradle:8,12` |
| `compileSdk` / `targetSdk` / `minSdk` | 34 / 34 / 28 | `app/build.gradle:9,13-14` |
| `versionCode` / `versionName` | 5 / "1.5" | `app/build.gradle:15-16` |
| Java/Kotlin target | 17 | `app/build.gradle:28-35` |
| `minifyEnabled` | **false** | `app/build.gradle:23` |
| `dependenciesInfo` | excluído do APK/bundle | `app/build.gradle:37-40` |

### 2.10 Compatibilidade entre versões Android

- `minSdk 28` (Android 9) — declarado no README como "Android 9.0 and newer"; o fastlane diz "Android 8".
- Bifurcações por versão encontradas:
  - `MainActivity.kt:26` — `SDK_INT >= S_V2` (32) para `BLUETOOTH_CONNECT`.
  - `BluetoothAdapterWrapper.kt:44-50` — `SDK_INT >= S` (31) usa `BluetoothManager`.
  - `Utils.kt:19-23` — `SDK_INT >= S_V2` usa `BluetoothManager.adapter.name`, senão `Settings.Secure "bluetooth_name"`.
  - `DeviceList.kt:92` — `@RequiresApi(Build.VERSION_CODES.P)` (28) na seleção de dispositivo BT.
- **Sem** uso de `BluetoothHidDevice` abaixo de API 28: a API existe desde API 28, alinhado ao `minSdk`, portanto a escolha de `minSdk 28` é determinada pelo HID Device.
- `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"` (`Manifest:24`) + `requestedOrientation` dinâmico em `Touchpad.kt:33,59-69,75` — a rotação é gerenciada manualmente, não por recriação de Activity.

---

## 3. Arquitetura do servidor C++

### 3.1 Ponto de entrada e inicialização

```text
wxapplication.cpp:3   wxIMPLEMENT_APP(wxApplication)      ← ponto de entrada real
wxapplication.cpp:9   wxApplication::OnInit()
  ├ :11  main_frame = new wxMain(settings)                → GUI
  ├ :13  server = new Server(6969, *this, settings, inputManager)
  │        Server ctor (server.cpp:5-23):
  │          acceptor.open/bind/listen (TCP v4, porta 6969)
  │          udpsocket(context, udp::endpoint(v4, 6969))  ← bind UDP
  │          byteBuffer = new char[255]
  │          settings.ADBOn()        → adb reverse tcp:6969
  │          StartReceive()          → primeiro async_receive_from
  ├ :15  hostInfo = server->GetHostInfo()   (name, IPv4, porta)
  ├ :18  main_frame->SetHostInfo(...)
  ├ :20-21 if(!settings.GetRunAtStartup()) main_frame->Show()
  ├ :23  server->Start()            → TCPDoAccept + thread(context.run)
  └ :25  main_frame->UpdateUI()
```

Detalhe relevante: `SettingsManager::ADBOn()` é chamado **no construtor do `Server`**, antes de `Server::Start()`; e `Server::Close()` chama `settings.ADBOff()` (`server.cpp:87`).

### 3.2 Ciclo de execução

- **Um** `asio::io_context context` (`server.h:70`), executado em **uma** thread dedicada criada em `Start()` (`server.cpp:51-53`).
- Estado assíncrono: `acceptor.async_accept` (re-armado em `:118`) e `udpsocket.async_receive_from` (re-armado em `:136`).
- O thread principal é o da GUI (wxWidgets event loop). **Não há sincronização explícita** entre os callbacks do `io_context` e a GUI: `Logger::log` (`logger.h:26-30`) chama `monitor->InsertItem` (`logger.cpp:13-14`) de dentro de handlers assíncronos. Isso é um risco de concorrência a registrar (ver `OPEN-QUESTIONS.md`).
- `connections` é um `std::vector<std::shared_ptr<Connection>>` (`server.h:78`) modificado dentro de callbacks (`server.cpp:113,143`) e lido pela GUI via `GetConnectedDevicesInfo()` (`:90-98`, chamado por `wxApplication::OnDeviceConnected`). Sem mutex.

### 3.3 Comunicação de rede

| Aspecto | Implementação | Evidência |
|---|---|---|
| Porta | 6969 em TCP **e** UDP (mesmo número) | `server.cpp:11,13`; `wxapplication.cpp:13` |
| Handshake | `socket.read_some(128 bytes)` **bloqueante** logo após o accept; string `fabricante/nome/modelo/ctype` | `server.cpp:108-110`; parse em `connection.cpp:11-22` |
| Eventos | UDP, `MAX_BUFFER_SIZE = 255` | `connection.h:19`; `server.cpp:124` |
| Eventos via TCP | também suportados (`Connection::Read`) | `connection.cpp:34-55` |
| Descoberta do host | `GetHostInfo()` cria socket UDP dummy para 8.8.8.8:80 e lê o `local_endpoint()` | `server.cpp:25-44` |
| Encerramento | `Close()`: fecha acceptor/udp, fecha conexões, `context.stop()`, `thread.join()`, `ADBOff()` | `server.cpp:62-88` |

`Connection::Send` existe (`connection.h:41-42`, `connection.cpp:57-65`) mas **não é chamado em nenhum lugar** — resíduo de um protocolo de retorno (o `udpsocket.send_to` de sincronização também está comentado em `server.cpp:129-135`).

### 3.4 Tratamento de mensagens

O protocolo é **um byte de opcode seguido de argumentos**:

| Opcode | Constante | Cliente (`Translators.kt`) | Servidor (`inputmanager.hpp`) | Bytes |
|---|---|---|---|---|
| `0x01` | LCLICK | `MouseClick(LEFT)` | `click()` | 1 |
| `0x02` | RCLICK | `MouseClick(RIGHT/MIDDLE)` | `right_click()` | 1 |
| `0x03` | DOWN | `MouseDragState(down)` | `down()` | 1 |
| `0x04` | UP | `MouseDragState(up)` | `up()` | 1 |
| `0x05` | MOVE | `MouseMove` | `move(-dx,-dy)` | 3 |
| `0x06` | SCROLL | `MouseScroll(dy)` | `scroll(dy)` | 2 |
| `0x07` | KEYPRESS | `KeyPress`/`NumpadKeyPress` | `send_key(key, mod)` | 3 |
| `0x08` | SCROLL_H | `MouseScroll(dx)` | `scroll_h(dx)` | 2 |
| `0x09` | ZOOM | `Zoom` | `zoom(scale)` | 2 |
| `0x0A` | MEDIA | `MediaEvent` | `media(action)` | 2 |

Sem cabeçalho de tamanho, sem versão, sem CRC, sem ACK. **A correspondência byte a byte é o contrato mais frágil do sistema.**

Nota importante: `MouseClick(MIDDLE)` é traduzido para `RCLICK` (`Translators.kt:174-176`) — o botão do meio **não** tem opcode próprio no modo socket, embora exista em Bluetooth (`getMouseButtonHIDCode` → `4`) e `BTN_MIDDLE` esteja registrado no Linux (`linuxinputmanager.cpp:24`). É uma lacuna funcional comprovada.

### 3.5 Manipulação de entrada (integração com o SO)

```text
input/input.h  →  escolhe o tipo por plataforma:
   _WIN32   → InputManager::Windows  (win32/windowsinputmanager.cpp)
   __unix__ → InputManager::Linux    (linux/linuxinputmanager.cpp)
```

- **Linux:** abre `/dev/uinput` em `O_WRONLY|O_NONBLOCK` (`:13`); registra EV_KEY/EV_REL/EV_SYN e todos os keycodes presentes em `KEYMAP`, `MEDIAMAP`, `MODMAP` (`:20-44`); cria o dispositivo virtual com nome `"Mousedroid"` (`:50`); `usleep(100000)` para o SO registrar o device (`:60`). Cada ação emite `input_event()` via `write()`.
- **Windows:** `SendInput()` com `INPUT` para mouse (`MOUSEEVENTF_*`) e teclado (`KEYEVENTF_KEYUP`), usando `KEYMAP`/`MEDIAMAP`/`MODMAP` de `win32/keymap.h`.
- **Sensibilidade:** variáveis globais `InputManager::MOVE_SENSITIVITY` e `SCROLL_SENSITIVITY` (`linuxinputmanager.cpp:8-9`, `windowsinputmanager.cpp:8-9`), ajustadas por sliders da GUI (`wxmain.cpp:198-204` → `SettingsManager::SetMoveSensitivity`). Ambas são divididas por 10.0 no cálculo, ou seja **o slider vai de 1 a 20 e o fator efetivo é 0.1–2.0**.

Diferença cross-platform comprovada:
- `Linux::scroll` **pressiona Shift** e usa `REL_WHEEL` (`:99-109`), enquanto `Linux::scroll_h` usa `REL_HWHEEL` (`:111-115`).
- `Windows::scroll_h` usa **Shift + MOUSEEVENTF_WHEEL** (`windowsinputmanager.cpp:57-74`).
Ou seja, scroll horizontal é feito de duas maneiras diferentes; e `scroll()` no Linux faz Shift+wheel (o que, em muitos apps, resulta em scroll horizontal, não vertical).

### 3.6 Componentes específicos do Windows / Linux

| Item | Windows | Linux |
|---|---|---|
| Injeção | `SendInput` + `user32` | `/dev/uinput` (`libudev`/kernel) |
| ADB | `adb.exe` + 2 DLLs incluídos (`server/adb/`), via `adb.hpp` | `system("./adb ...")` (`settingsmanager.cpp:127`) — mas **`server/adb/` só tem binários Windows** |
| Build | vcpkg (`asio::asio`, `wxwidgets`), resource `.rc`, `-Wl,-subsystem,windows` no MinGW | `find_package(wxWidgets)`, `ASIO_PATH` manual |
| Registro/autostart | `HKEY_CURRENT_USER\...\Run` (`settingsmanager.cpp:11,54-57`) | não implementado (`#ifdef _WIN32` em `:48-59`) |
| Instalação | `release.bat` (manual) | `install.sh` (udev + .desktop) |
| Toolchain IDE | MSVC (CMakeSettings) **e** MinGW (`.vscode/tasks.json`) | n/a |

### 3.7 Tratamento de erros e encerramento

- `Server::Start` captura `std::exception` **por valor** e apenas loga (`server.cpp:56-59`).
- `Connection::Read` captura `std::exception` por valor e fecha (`connection.cpp:52`).
- `Server::Close` usa `asio::error_code` e loga a mensagem do acceptor (`:66-68`).
- **Não há validação de tamanho em `Connection`:** `tokens[0]..tokens[3]` são acessados sem checar `tokens.size()` (`connection.cpp:18-21`).
- **Não há validação do tamanho mínimo das mensagens** em `Base::execute`: `bytes[1]`/`bytes[2]` são lidos sem verificar `size` (`inputmanager.hpp:47-86`).
- Vazamento: `byteBuffer` alocado com `new[]` em `Server` (`:19`) e em `Connection` (`connection.cpp:24`) sem `delete`.
- Encerramento da GUI: `wxApplication::OnWindowCloseEvent` (`:42-55`) pede confirmação, e só então `server->Close()`, `Logger::monitor->Destroy()`, `main_frame->Destroy()`.

### 3.8 Pontos em que uma alteração afeta outros componentes

| Alteração | Afeta | Motivo |
|---|---|---|
| Nova constante de opcode em `inputmanager.hpp` | `Translators.kt` (Android) **e** `wm*inputmanager` das duas plataformas | protocolo binário duplicado em dois repositórios de código |
| Mudança no formato de `deviceDetails` | `Utils.kt:25` ↔ `connection.cpp:11-22` | split posicional por `/` |
| Novo campo em `Connection::PacketType` | ninguém hoje (código morto) | `PacketType` não é usado |
| Troca de porta | `wxapplication.cpp:13`, `settingsmanager.cpp:125,127,144`, `ConnectionViewModel.kt:74`, `DeviceListViewModel.kt:91` | 6969 hardcoded em 6 pontos de 4 arquivos |
| Alteração do descriptor HID | `Descriptor.kt` + `Reports.kt` + `Translators.toHIDReport` | layout de bytes dos relatórios |
| Alteração do `CMakeLists` `PLATFORM_SRC` | compilação do `.rc`/ícone no Windows | dupla definição nas linhas 22-27 e 41-46 |
| Alteração de `Theme.Mousedroid` / `@string/app_name` | Manifest, layouts, fastlane | marca propagada |

---

## 4. Estado de cada modo (implementado / desativado / a validar)

| Modo | Implementado | Estado | Base da conclusão |
|---|---|---|---|
| **Wi-Fi** | Android (TCP+UDP) + servidor | **Implementado e coerente nos dois lados**; sem validação física registrada | `ConnectionManager.kt:96-109`, `server.cpp:11-17,122-139` |
| **USB/ADB** | Android TCP 127.0.0.1 + `adb reverse` | **Implementado**; dependente de `adb` disponível no PC e do dispositivo autorizado. Não validado fisicamente | `ConnectionManager.kt:115-127`, `settingsmanager.cpp:120-151`, `adb.hpp` |
| **Bluetooth** | Android HID Device | **Implementado**; independente do servidor. Sem validação física | `BluetoothConnection.kt`, `Descriptor.kt` |
| Streaming de vídeo | restos em `Server::PacketType` (7 constantes) | **Não implementado / desativado** — nenhuma referência | grep de `PacketType|ACTIVATION|CAMERA` → 0 usos |
| Bateria (HID e socket) | `BatteryMonitor`, `BatteryReport`, `InputEvent.BatteryEvent` | **Desativado** (start comentado; bloco do descriptor comentado; socket envia array vazio) | `MainActivity.kt:33`, `Descriptor.kt` (bateria comentada), `Translators.kt:205` |
| Middle click no modo socket | opcode ausente | **Não implementado** (vira RCLICK) | `Translators.kt:173-177` |
| Sincronização UDP (ACK) | `send_to("done")` comentado | **Desativado** | `server.cpp:129-135`, `UDPConnection.kt:54` |

---

## 5. Problemas de manutenção identificados na arquitetura

Classificados por severidade, **sem correção nesta missão**:

| # | Problema | Severidade | Evidência |
|---|---|---|---|
| A1 | CI só dispara na branch `arena/39232774-...` (Missão 02) | **Alta** | `foundation-build.yml:4-12` |
| A2 | Protocolo sem versão/tamanho; duas cópias (Kotlin/C++) | **Alta** | §3.4 |
| A3 | Latência artificial: `sleep(10)` TCP, `sleep(15)` UDP por relatório | **Alta** (UX do trackpad) | `TCPConnection.kt:59`, `UDPConnection.kt:46` |
| A4 | Acesso sem validação de tamanho no parse do handshake e dos opcodes | **Alta** (UB/crash) | `connection.cpp:18-21`, `inputmanager.hpp:47-86` |
| A5 | porta 6969 repetida em **6 pontos de 4 arquivos**, sem constante compartilhada | Média | §3.8 |
| A6 | Ausência de sincronização entre `io_context` e GUI (logger, lista de conexões) | Média | `logger.cpp:13`, `server.cpp:113` |
| A7 | `.idea`/`.gradle`/`local.properties` versionados com caminhos do autor | Média | inventory |
| A8 | Texto de UI hardcoded em Kotlin e em layouts (sem i18n) | Média | `Main.kt:120-158`, `Input.kt:136-158` |
| A9 | `PacketType`, `Connection::Send`, `maxPacketSize`, `PacketType` e `logo.png` órfãos | Baixa | §4, inventory |
| A10 | `CMakeLists` com lógica duplicada e `GLOB_RECURSE` | Baixa | `CMakeLists.txt:12-50` |
| A11 | `.vscode/tasks.json` (MinGW) divergente de `CMakeSettings.json`/README (MSVC) | Média | §3.6 |
| A12 | Bloqueio (`read_some`) dentro de handler assíncrono | Média | `server.cpp:109` |
| A13 | `SettingsManager::GetFilePath()` sem `return` fora do Windows | Média | `settingsmanager.cpp:81-88` |
| A14 | `wxDeviceList::UpdateView(false)` pode indexar fora do vetor | Baixa (não usado) | `wxdevicelist.cpp:41-43` |
| A15 | `release.bat` gera `distconfig.ini` fora de `dist` | Baixa | `release.bat:17` |

---

## 6. Conclusão da fase

A fundação é uma implementação **coesa e pequena** (37 arquivos Kotlin, 26 arquivos C++), com separação de camadas consistente no cliente (MVVM + transporte abstrato) e no servidor (rede / input / GUI). A dívida técnica concentra-se em **três eixos**:

1. **Protocolo binário duplicado e sem versionamento** — maior risco para qualquer evolução do Pocket Pad.
2. **Latência embutida no transporte** (`sleep` fixos) — maior inimigo da sensação de "trackpad Logitech".
3. **Configurações de máquina e de CI acopladas ao ambiente do autor** — maior atrito operacional.

Nada disso exige reescrita. Todos os três eixos são endereçáveis dentro da estrutura atual.
