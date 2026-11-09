# Protocolos de comunicação — auditoria independente

> Missão 03. Cada protocolo foi analisado isoladamente, a partir do código.
> Regra aplicada: **código presente ≠ protocolo ativo**. Cada seção termina com uma tabela
> "implementado / desativado / requer validação física".

---

## 0. Quadro comparativo

| | USB / ADB | Bluetooth | Wi-Fi |
|---|---|---|---|
| Transporte | TCP sobre `adb reverse` | Bluetooth HID (L2CAP, perfil HID Device) | TCP (controle) + UDP (eventos) |
| Quem injeta a entrada | servidor C++ | o **host** (PC), via stack HID | servidor C++ |
| Precisa do servidor? | **sim** | **não** | **sim** |
| Porta | 6969 (TCP) | n/a (PSM do stack) | 6969 (TCP **e** UDP) |
| Encoding | 1 byte opcode + args | HID report (IDs 1/2/3) | 1 byte opcode + args |
| Arquivos-chave | `ConnectionManager.kt:115-127`, `settingsmanager.cpp:120-151`, `adb.hpp` | `BluetoothConnection.kt`, `Descriptor.kt`, `Reports.kt`, `Translators.toHIDReport` | `ConnectionManager.kt:96-109`, `TCPConnection.kt`, `UDPConnection.kt`, `server.cpp` |
| Estado | Implementado, não validado em hardware | Implementado, não validado em hardware | Implementado nos dois lados, não validado em hardware |
| Evidência de funcionamento | apenas README/CI (compilação) | apenas README/CI | apenas README/CI |

> **Nenhum dos três protocolos tem teste automatizado, log de execução real ou evidência física registrada nesta auditoria.** A validação prática do Mousedroid citada no contexto da missão foi feita em um celular real com "responsividade satisfatória", mas não há registro por protocolo; portanto não é possível atribuí-la a um modo específico. Ver `OPEN-QUESTIONS.md` Q1.

---

## 1. USB / ADB

### 1.1 Como funciona no Mousedroid

O USB **não** usa o protocolo USB nativo do Android. Ele usa **ADB port forwarding reverso**: o telefone não é acessível na rede, então o servidor no PC pede ao ADB que abra uma porta **no telefone** que encaminhe para o PC.

```text
PC                                   Telefone
┌─────────────────────┐              ┌──────────────────────────────┐
│ Mousedroid (C++)    │              │ Mousedroid (Kotlin)          │
│ Server :6969        │              │ 127.0.0.1:6969               │
│   ▲                 │              │      │                       │
│   │ TCP             │              │      │ TCPConnection         │
│   │                 │              │      ▼                       │
│ adb reverse tcp:6969│◄── USB ─────►│ adb daemon (device side)     │
│   tcp:6969          │              │                              │
└─────────────────────┘              └──────────────────────────────┘
```

`adb reverse tcp:6969 tcp:6969` faz com que `127.0.0.1:6969` **no telefone** encaminhe para `127.0.0.1:6969` **no PC**. Por isso o cliente conecta em `"127.0.0.1"` (`ConnectionManager.kt:119`).

### 1.2 Inicialização

**Lado servidor (quem dispara o reverse):**

- `SettingsManager::ADBOn()` — `server/src/settingsmanager.cpp:120-136`
  - Windows: `bAdbStarted = adb::start(6969)` → `adb.hpp:30-45`
    ```cpp
    command = path + "\\adb.exe " + "reverse tcp:" + port + " tcp:" + port;   // adb.hpp:33
    if (system(command.c_str()) == 0) ...
    ```
    O binário usado é `<dir do executável>\adb.exe` (verificado em `adb.hpp:16-23,33`).
  - Linux: `a = system("./adb reverse tcp:6969 tcp:6969")` — `settingsmanager.cpp:127`
    > **Achado:** o caminho é relativo ao diretório de trabalho (`./adb`) e `server/adb/` contém **apenas** `adb.exe` + 2 DLLs (Windows). Portanto no Linux o `adb` usado é o do **sistema** *se* copiado/exposto em `./adb` pelo usuário, ou o comando falha silenciosamente (apenas loga "ADB Server FAILED"). O README Linux manda instalar o pacote `adb` (`apt install ... adb`), mas o código não chama `adb` do PATH.
- Quando é chamado: **no construtor do `Server`** — `server.cpp:21` `settings.ADBOn();`, ou seja, sempre que o app sobe, independentemente de haver dispositivo USB conectado.
- Encerramento: `Server::Close()` → `settings.ADBOff()` (`server.cpp:87`) → `adb::kill(6969)` (chamado em `settingsmanager.cpp:144`; implementado em `adb.hpp:51-72`) no Windows, que executa:
  ```cpp
  path + "\\adb.exe reverse --remove tcp:6969 & " + path + "\\adb\\adb.exe kill-server"   // adb.hpp:60
  ```
  > **Achado:** caminho inconsistente — `path + "\\adb.exe"` vs `path + "\\adb\\adb.exe"` na mesma linha. O segundo não existe (o exe está em `server/adb/adb.exe`, ou seja `path\adb\adb.exe` **se** o pacote for copiado para `adb/` no destino final). Isso explica o comentário do autor nas linhas 55-58 ("sometimes with kill-server it takes too long..."). Não foi validado.

**Lado cliente:**

- `ConnectionViewModel.startServerMode` (`ConnectionViewModel.kt:69-80`): se `hasUsbConnection(context)` → `connectUSB(6969, details)`.
- `ConnectionManager.connectUSB` (`:115-127`) cria `TCPConnection("127.0.0.1", port, isOverAdb = true, listener)` e envia o handshake.

### 1.3 Detecção do dispositivo

- Cliente: `hasUsbConnection()` (`NetUtils.kt:10-16`) registra um receiver nulo para `android.hardware.usb.action.USB_STATE` e lê o extra booleano `"connected"`. **Não** usa `UsbManager`; não requer `USB_PERMISSION`.
- Servidor: **não** detecta dispositivo. Ele apenas executa o comando `adb reverse`. Se não houver dispositivo autorizado, `adb` falha e o app segue funcional para Wi-Fi.
- Não há escolha de dispositivo: com múltiplos aparelhos conectados, `adb reverse` aplica-se ao "primeiro" escolhido pelo adb (comportamento do adb, não do app). **Não implementado** no Mousedroid.

### 1.4 Transporte de dados e encaminhamento de eventos

- TCP apenas (comentário explícito: *"adb doesn't allow Udp port forwarding"* — `ConnectionManager.kt:113`).
- Handshake: cliente envia `deviceDetails` sem framing (`tcpConn.sendBytes(...)` em `:120`); servidor faz `read_some(128)` **bloqueante** e considera tudo o que veio como a string (`server.cpp:108-110`).
- Eventos: cada clique/movimento é um `write()` seguido de `flush()` e **`Thread.sleep(10)`** (`TCPConnection.kt:52-61`).
- Fim de stream: quando `isOverAdb && read() == -1`, o cliente considera desconectado (`TCPConnection.kt:73-76`) — proteção específica do caminho ADB.

### 1.5 Dependências do ADB

| Dependência | Onde | Observação |
|---|---|---|
| `adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll` | `server/adb/` | 5,9 MB + 108 KB + 73 KB; PE válido (BASELINE §4). Licença Android SDK/AOSP — **não verificada juridicamente** |
| adb do sistema | README Linux | usado por `./adb` (ver 1.2) |
| USB debugging habilitado + autorização RSA | requisito de ambiente | não verificado por código |
| `android.hardware.usb.action.USB_STATE` | `NetUtils.kt:13` | broadcast interno do sistema |

### 1.6 Tratamento de falhas

| Falha | Comportamento | Evidência |
|---|---|---|
| `adb` ausente / falha | loga e marca `bAdbStarted=false`; app continua | `settingsmanager.cpp:128-135` |
| TCP recusa | `ConnectionFailedException` → `onConnectionFailed(USB)` | `TCPConnection.kt:47-49`, `ConnectionManager.kt:122-125` |
| Reverse órfão de sessão anterior | **risco**: `ADBOn` pode falhar com "port already in use"; não há `reverse --remove` prévio | inferido de `adb.hpp:55-60` (comentário do autor) |
| Queda do cabo | `read()` retorna `-1` → `onDisconnected` → popup no cliente | `TCPConnection.kt:73-76`, `Main.kt:120-131` |
| Servidor não iniciado | timeout do socket (`java.net.Socket` sem timeout explícito — usa o default do SO) | `TCPConnection.kt:38` |

### 1.7 Veredito USB/ADB

| Item | Estado |
|---|---|
| Reverse ADB automático no Windows | **Implementado** (não validado) |
| Reverse ADB automático no Linux | **Implementado de forma incompleta/duvidosa** (`./adb` não existe em `server/adb/` no Linux) |
| Seleção de dispositivo | **Não implementado** |
| Recuperação de reverse órfão | **Não implementado** |
| Validação física | **Pendente** |

---

## 2. Bluetooth (HID Device)

### 2.1 Arquitetura

O Android comporta-se como **dispositivo HID** (mouse/teclado) e o PC como **host**. **O servidor C++ não participa.** O PC precisa apenas de Bluetooth e pareamento; não precisa de nenhum software.

```text
Telefone (HID Device)                        PC (HID Host)
BluetoothHidDevice.registerApp(sdp, qos) ──► aparece como mouse/teclado
BluetoothHidDevice.connect(hostMac)      ──► conexão HID
sendReport(host, reportId, bytes)        ──► SO do PC interpreta
```

### 2.2 Inicialização e registro do perfil

1. `MainActivity.onCreate` → `BluetoothAdapterWrapper.initialize(context)` (`MainActivity.kt:32`).
2. `Main.kt:77` (botão Bluetooth) → `ConnectionViewModel.startBluetoothMode` (`:87-95`) → `connectionManager.registerBluetoothHID(context)`.
3. `ConnectionManager.registerBluetoothHID` (`:132-136`) cria `BluetoothConnection(context, this)` em `Dispatchers.IO`.
4. `BluetoothConnection` init (`:136-158`):
   - `bluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)`
   - ao conectar no proxy: `unregisterApp()` (limpa "ghost states") e então `registerApp(sdp, null, null, singleThreadExecutor, callback)`.

**SDP** (`:49-53`):
```kotlin
BluetoothHidDeviceAppSdpSettings(
    "Mousedroid", "Android HID", "Mousedroid",
    BluetoothHidDevice.SUBCLASS1_COMBO, HID_REPORT_DESC)
```
**QoS** (`:58-61`): `SERVICE_BEST_EFFORT, 800, 9, 0, 11250, -1`.

> **Achado de marca:** os três nomes `"Mousedroid"` aparecem no SDP, ou seja, **o nome exibido no PC ao parear** vem daqui e precisará mudar no Pocket Pad.

### 2.3 Pareamento

- Descoberta: `Main.kt:80-86` dispara `BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE` por 120 s.
- O app **não pareia** programaticamente; usa `bluetoothAdapter.bondedDevices` (`BluetoothConnection.kt:195`) — o pareamento é feito pelo SO.
- Lista de hosts: `DeviceList.kt:51-59` → `DeviceListViewModel.Factory(pairedDevices)` (`DeviceListViewModel.kt:42-48`).
- Conexão: `BluetoothConnection.connect(mac)` (`:189-204`) **espera 2000 ms fixos** antes de `bluetoothHidDevice?.connect(target)` (comentário: "otherwise connection fails immediately").

### 2.4 Permissões

| Permissão | Onde | Necessidade |
|---|---|---|
| `BLUETOOTH_CONNECT` | `MainActivity.kt:26-30`, `Manifest:8` | API 31+ para `bondedDevices`, `connect`, `name` |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | `Manifest:6-7` | APIs ≤ 30 |
| `BLUETOOTH_ADVERTISE` | `Manifest:9` | declarada, **sem uso identificado** (a descoberta usa o Intent do sistema) |

Há `@SuppressLint("MissingPermission")` na classe (`BluetoothConnection.kt:24`) — a checagem é delegada ao fluxo de permissão da Activity.

### 2.5 Comunicação HID

- **Descriptor** (`Descriptor.kt`): teclado (ID 1), mouse (ID 2), consumer/media (ID 3). Bloco de bateria (ID 4) **comentado** (`Descriptor.kt` final).
- **Reports** (`Reports.kt:8-56`): `MouseReport(button,dx,dy,wheel)` → 4 bytes; `KeyboardReport(mod, keys×6)` → 8 bytes; `MediaReport(bitmask)` → 1 byte; `BatteryReport` existe mas nunca é enviado.
- **Encoder** (`Translators.toHIDReport`, `:69-150`):
  - `MouseMove` → `MouseReport(button, -dx, -dy, 0)` — eixos **invertidos**, coerente com o servidor (`move(-dx,-dy)`).
  - `MouseClick` → **2 reports** (press + release).
  - `MouseScroll` → `dy/10` clampado; se horizontal, usa **Shift + wheel** (`:102-106`).
  - `Zoom` → CTRL + wheel (`:120-132`).
  - `KeyPress` → um par press/release **por tecla** da lista de modificadores mapeados.
- **Envio** (`:124-134`): `Channel<Array<HIDReport>>` ilimitado → job que chama `sendReport` e aplica `delay(15)` **apenas** para `KeyboardReport`.
- **`MouseDragState`** (DOWN/UP) mapeia para `MouseReport` com o botão pressionado — permite arrastar.

### 2.6 Tratamento de desconexões

`BluetoothConnection.callback.onConnectionStateChanged` (`:64-107`) distingue três casos:

| Estado | Ação |
|---|---|
| `STATE_CONNECTED` | `listener.onConnected(BLUETOOTH, name)`; envio de bateria **comentado** (`:78-84`) |
| `STATE_DISCONNECTED` + `isClosing` | `cleanupProxy()` (encerramento solicitado pelo usuário) |
| `STATE_DISCONNECTED` + `connectionEstablished` | sessão perdida (host desligou/saiu de alcance) → `onDisconnected` |
| `STATE_DISCONNECTED` + `!connectionEstablished` | falha de conexão → `onConnectionFailed` |

`close()` (`:165-184`) cancela o job, fecha o channel, tenta `disconnect(host)` e faz fallback para `cleanupProxy()` se falhar.

### 2.7 Compatibilidade Android

- `BluetoothHidDevice` requer **API 28** — daí `minSdk 28` (`app/build.gradle:14`).
- `getProfileProxy` requer API 28+; `BluetoothManager` (em vez de `getDefaultAdapter`) a partir de API 31 (`BluetoothAdapterWrapper.kt:44-50`).
- O `SUBCLASS1_COMBO` declara mouse **e** teclado no mesmo dispositivo.

### 2.8 Veredito Bluetooth

| Item | Estado |
|---|---|
| Registro de app HID Device | **Implementado** |
| Descriptor de mouse/teclado/mídia | **Implementado** |
| Descriptor de bateria | **Desativado** (comentado) |
| Envio de relatório de bateria | **Desativado** (código comentado em 2 lugares) |
| Reconexão automática | **Não implementado** (é preciso refazer o fluxo desde a tela inicial) |
| Pareamento programático | **Não implementado** (usa os já pareados) |
| Validação física em Windows/Linux/Android host | **Pendente** |

---

## 3. Wi-Fi

### 3.1 Implementação: dois sockets com papéis diferentes

```text
                    ┌─────────────── TCP :6969 ───────────────┐
Cliente ────────────►  handshake (deviceDetails)               │ Servidor
                    └─────────────────────────────────────────┘
                    ┌─────────────── UDP :6969 ───────────────┐
Cliente ────────────►  todos os eventos de entrada             │ Servidor
                    └─────────────────────────────────────────┘
```

Justificativa no código: *"Tcp socket is used only for connections, commands are sent using Udp"* (`ConnectionManager.kt:92-95`).

### 3.2 Inicialização

**Cliente** (`ConnectionManager.kt:96-109`):
```kotlin
connectionStateCallback?.onConnectionInitiated(Mode.WIFI)
CoroutineScope(Dispatchers.IO).launch {
    tcpConn = TCPConnection(ipAddress, port, false, this@ConnectionManager)  // handshake
    udpConn = UDPConnection(ipAddress, port)                                  // eventos
    tcpConn!!.sendBytes(deviceDetails.toByteArray())
    onConnected(Mode.WIFI, ipAddress)
}
```
- `UDPConnection.init` (`:28-38`) faz `DatagramSocket()`, `connect(address, port)`, `sendBufferSize = 921600`.
- O cliente considera a conexão estabelecida **sem receber resposta** do servidor — não há ACK de handshake.

**Servidor** (`server.cpp`):
- Construtor: `udpsocket(context, udp::endpoint(udp::v4(), port))` (`:11`) e `acceptor` TCP na mesma porta (`:13-17`).
- `GetHostInfo()` (`:25-44`) descobre o IPv4 local abrindo um socket UDP dummy para `8.8.8.8:80` e lendo `local_endpoint()`. A GUI mostra nome do host e IP (`wxmain.cpp:175-179`).

### 3.3 Portas e protocolos

| Item | Valor | Evidência |
|---|---|---|
| Porta | **6969** TCP e UDP | `server.cpp:11,13`; `wxapplication.cpp:13` |
| Cliente (USB) | 6969 | `ConnectionViewModel.kt:74` |
| Cliente (Wi-Fi) | 6969 | `DeviceListViewModel.kt:91` |
| ADB reverse | 6969 | `settingsmanager.cpp:125,127,144` |
| Handshake máx. | 128 bytes | `server.cpp:108` |
| Buffer de evento | 255 bytes (`Connection::MAX_BUFFER_SIZE`) | `connection.h:19`, `server.cpp:19,124` |
| `maxPacketSize` declarado | 65472 (TCP e UDP) | `TCPConnection.kt:27`, `UDPConnection.kt:24` — **propriedade declarada e nunca lida** |

### 3.4 Descoberta do servidor

**Não existe.** O usuário digita nome e IPv4 manualmente em `DeviceList` → `showAddDeviceDialog` (`DeviceList.kt:171-203`), validado por regex de IPv4 (`:184-188`), e salvo em `SharedPreferences("devices")` (`DeviceListViewModel.kt:76-79`). Não há mDNS, broadcast UDP nem QR code.

### 3.5 Componentes preservados mas não usados

| Componente | Estado | Evidência |
|---|---|---|
| Recepção de dados no UDP | **Não implementada** — `"No packet receiving implementation"` | `UDPConnection.kt:13,54` (receive comentado) |
| Sincronização/ACK (`send_to("done")`) | **Desativado** (comentado) | `server.cpp:129-135` |
| `Server::PacketType` (FRAME, RESOLUTION, ACTIVATION, CAMERA, QUALITY, WB, EFFECT) | **Código morto** — resquício de streaming de vídeo | `server.h:31-40`, grep sem usos |
| `Connection::Send(...)` | **Nunca chamado** | `connection.cpp:57-65` |
| Envio de bateria pelo socket | **Implementado como no-op** — `socketReport()` vazio | `Translators.kt:205` |

### 3.6 Dependências do servidor para Wi-Fi

- `asio::ip::tcp::acceptor` e `asio::ip::udp::socket` no mesmo `io_context` e na mesma porta.
- Firewall do host precisa liberar TCP e UDP 6969 — mencionado no texto de erro do cliente (`Main.kt:141`) e não automatizado.
- `GetHostInfo()` depende de rota para `8.8.8.8` para descobrir o IP local: **em rede sem gateway/internet** o `connect()` do socket UDP pode falhar (não há tratamento — `server.cpp:35` está fora de try/catch).

### 3.7 Tratamento de falhas

| Falha | Comportamento | Evidência |
|---|---|---|
| TCP recusa (servidor off) | `ConnectionFailedException` → `onConnectionFailed(WIFI)` → popup | `ConnectionManager.kt:104-107`, `Main.kt:133-144` |
| UDP falha ao abrir | `ConnectionFailedException` **não capturada** no `connectWIFI` porque `UDPConnection` é construído dentro do try (é capturada — mesma cláusula) | `:100-107` |
| Perda do servidor | cliente não detecta (UDP sem keepalive); só percebe se o TCP cair (`TCPConnection.startReceiveBytesLoop`) | `TCPConnection.kt:67-84` |
| Perda do cliente | servidor não detecta no Wi-Fi se o cliente só usava UDP; `Connection::Read` só reage ao TCP | `connection.cpp:34-55` |
| Pacote perdido/reordenado (UDP) | **sem tratamento** — eventos podem ser perdidos | `UDPConnection.kt:40-49` |

### 3.8 Veredito Wi-Fi

| Item | Estado |
|---|---|
| TCP + UDP na mesma porta | **Implementado nos dois lados, coerente** |
| Handshake | **Implementado, sem ACK** |
| Descoberta automática | **Não implementado** |
| Recuperação de pacotes / ACK | **Desativado** (comentado) |
| Detecção de queda por UDP | **Não implementado** |
| Autenticação / criptografia | **Inexistente** (qualquer host da rede pode enviar datagramas para a porta) |
| Validação física | **Pendente** |

---

## 4. Riscos transversais aos três protocolos

| # | Risco | Protocolos | Severidade |
|---|---|---|---|
| P1 | Sem versionamento de protocolo — cliente e servidor de versões diferentes não se detectam | Wi-Fi, USB | Alta |
| P2 | Sem autenticação: qualquer máquina na LAN pode injetar eventos na porta 6969 | Wi-Fi | Alta |
| P3 | Sem validação de tamanho de mensagem (cliente pode causar UB) | Wi-Fi, USB | Alta |
| P4 | Latência artificial fixa (`sleep` 10/15 ms, `delay` 15/2000 ms) | todos | Alta (UX) |
| P5 | Middle click ausente no modo socket | Wi-Fi, USB | Média |
| P6 | Handshake posicional por `/`, sem validação | Wi-Fi, USB | Média |
| P7 | Nome "Mousedroid" embutido no SDP Bluetooth e no dispositivo uinput | BT, USB/Wi-Fi (uinput) | Média (identidade) |
| P8 | Bateria desativada em todos os caminhos | todos | Baixa |
| P9 | Nenhum teste automatizado de protocolo | todos | Alta (manutenção) |

---

## 5. O que permanece desconhecido (não presumido)

1. Se o modo USB funciona de fato em Linux — o código chama `./adb`, que não existe no pacote do repositório. **Sem evidência de sucesso.**
2. Se o Bluetooth HID conecta de forma confiável em Windows 10/11 (o `delay(2000)` sugere instabilidade conhecida pelo autor, registrada apenas em comentário).
3. Se `8.8.8.8` em `GetHostInfo()` funciona em redes isoladas (risco teórico, não observado).
4. Qual a taxa real de eventos suportada antes de o `sleep(15)` do UDP saturar a fila (limite teórico: ~66 eventos/s por evento de 2-3 bytes).
5. Se o descriptor HID com `SUBCLASS1_COMBO` é aceito por todos os stacks host.
