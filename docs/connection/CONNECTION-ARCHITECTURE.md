# CONNECTION-ARCHITECTURE.md

## Missão 06 — Arquitetura de Conexão

### Visão Geral

A conexão USB é o modo principal do Pocket Pad. O objetivo é que o usuário conecte o cabo USB, abra o app e comece a usar — sem etapas técnicas intermediárias.

### Fluxo de Conexão (pós-alterações)

```
┌─────────────────────────────────────────────────────────────────┐
│                        FLUXO USB                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  SERVIDOR (Windows)                                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 1. Mousedroid.exe inicia                                 │    │
│  │ 2. Verifica adb.exe disponível                           │    │
│  │ 3. Verifica dispositivo Android (adb devices)            │    │
│  │ 4. Se conectado: adb reverse tcp:6969 tcp:6969          │    │
│  │ 5. Se não autorizado: mostra mensagem na UI             │    │
│  │ 6. Monitora dispositivo a cada 2 segundos               │    │
│  │ 7. Escuta TCP 6969 para conexões do app                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                         │                                         │
│                    USB Cable                                      │
│                         │                                         │
│  CLIENTE (Android)                                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 1. App abre                                              │    │
│  │ 2. Verifica USB_STATE (connected = true?)               │    │
│  │ 3. Se USB: tenta TCP 127.0.0.1:6969 automaticamente    │    │
│  │ 4. Se conecta: vai direto para touchpad                 │    │
│  │ 5. Se falha: tenta novamente (5x, com delay)            │    │
│  │ 6. Se não consegue: mostra botões manuais               │    │
│  │ 7. Se desconecta: reconecta automaticamente             │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Estados da Conexão

| Estado | Descrição | O que o usuário vê |
|--------|-----------|-------------------|
| `Idle` | Nenhum processo de conexão ativo | Tela inicial com botões |
| `Searching` | Procurando servidor automaticamente | "Connecting to computer..." + spinner |
| `Connecting` | Tentativa de conexão em andamento | "Connecting via USB..." + spinner |
| `Connected` | Conexão estabelecida | Touchpad |
| `Reconnecting(n)` | Tentando reconectar após desconexão | "Reconnecting... (attempt n)" |
| `Error` | Falha com mensagem específica | Popup com causa e solução |

### Gerenciamento ADB (Servidor)

#### Responsabilidade
O servidor é o único responsável pelo gerenciamento do ADB:
- Inicialização do reverse tunnel
- Monitoramento do estado do dispositivo
- Limpeza do tunnel no encerramento
- Tentativa de reconexão se o dispositivo reaparecer

#### Não duplicar
- O cliente Android NÃO executa comandos ADB
- O cliente apenas conecta ao TCP 127.0.0.1:6969 (disponível via reverse)
- Não há segundo gerenciador ADB

#### Detecção de dispositivo
```
DeviceStatus.NONE        → Nenhum dispositivo USB
DeviceStatus.UNAUTHORIZED → Dispositivo conectado, não autorizado
DeviceStatus.OFFLINE     → Dispositivo offline
DeviceStatus.CONNECTED   → Um dispositivo pronto
DeviceStatus.MULTIPLE    → Múltiplos dispositivos
```

#### Fluxo de inicialização
1. `Server()` construtor → `settings.ADBOn()`
2. `ADBOn()` → `checkDevices()`
3. Se `CONNECTED`: `adb::start(6969)` → `bAdbStarted = true`
4. Se `UNAUTHORIZED`: loga mensagem, espera
5. Se `NONE`: loga mensagem, espera
6. Timer de 2 segundos → `TryReconnectADB()` verifica novamente

### Reconexão Automática (Cliente)

#### Estratégia
Quando a conexão USB cai:
1. `onDisconnected()` é chamado
2. Se modo USB: inicia `startReconnection()`
3. Loop com backoff exponencial: 1s, 2s, 4s, 8s, 10s (máx)
4. Máximo 10 tentativas
5. Se conectar: vai para touchpad
6. Se esgotar tentativas: volta ao estado Idle

#### Limites
- Backoff exponencial evita sobrecarga
- Máximo 10 tentativas evita loop infinito
- Cancela se usuário navegar para outra tela
- Cancela se outra conexão for estabelecida

### Ciclo de Vida dos Processos

```
Mousedroid.exe
  ├─ Inicia: ADBOn() → reverse tunnel
  ├─ Monitora: Timer 2s → checkDevices() + TryReconnectADB()
  ├─ Escuta: TCP 6969 (aceita conexões do app)
  └─ Termina: ADBOff() → stop(6969) → remove reverse

com.darusc.mousedroid (app)
  ├─ onCreate: BluetoothAdapterWrapper.initialize()
  ├─ onResume: tryAutoConnectUsb()
  ├─ conectado: envia eventos de input via TCP
  ├─ desconectado: startReconnection() → tenta reconectar
  └─ onDestroy: disconnect() → limpa conexões
```

### Protocolo (inalterado)

O protocolo binário não foi modificado:
- Porta: 6969
- Handshake: deviceDetails (manufacturer/name/model/connectionType)
- Eventos: 1-byte opcode + args
- Opcodes: LCLICK(0x01) a MEDIA(0x0A)

### Componentes Modificados

| Arquivo | Alteração | Justificativa |
|---------|-----------|---------------|
| `server/src/adb.hpp` | Device detection, exec com output, status enum | Necessário para verificar dispositivo antes de reverse |
| `server/src/settingsmanager.cpp/h` | TryReconnectADB(), GetDeviceStatus() | Monitoramento automático |
| `server/src/gui/wxmain.cpp/h` | Timer de device check, status text | UI mostra estado em vez de botão ADB |
| `server/src/wxapplication.cpp` | Set server reference on main frame | Comunicação server→UI |
| `client/.../ConnectionViewModel.kt` | tryAutoConnect(), startReconnection(), novos states | Auto-conexão e reconexão |
| `client/.../fragments/Main.kt` | tryAutoConnectUsb() no onResume, novos estados | UI reflete auto-conexão |
| `client/.../layout/fragment_main.xml` | Texto do botão atualizado | Clareza para o usuário |

### O que NÃO mudou

- Protocolo binário
- Lógica de input (Translators, GestureHandler)
- Conexão WiFi (DeviceListFragment, DeviceListViewModel)
- Conexão Bluetooth (BluetoothConnection)
- Layout do touchpad
- Numpad, teclado
- Compilação e dependências
