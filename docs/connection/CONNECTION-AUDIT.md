# CONNECTION-AUDIT.md

## Missão 06 — Auditoria da Infraestrutura de Conexão

### Fluxo Atual (USB)

```
1. Usuário inicia servidor Windows (Mousedroid.exe)
2. Servidor executa: settings.ADBOn() → adb reverse tcp:6969 tcp:6969
3. Usuário conecta cabo USB
4. Usuário abre app Android
5. Usuário vê tela inicial com 3 botões:
   - "Server (USB/WIFI)"
   - "Bluetooth"
   - "Pair"
6. Usuário clica em "Server (USB/WIFI)"
7. App verifica hasUsbConnection() (USB_STATE sticky broadcast)
8. Se USB: TCPConnection("127.0.0.1", 6969)
9. Se conectar: navega para touchpad
10. Se falhar: mostra popup de erro, usuário deve tentar novamente
```

### Fluxo Atual (WiFi)

```
1. Usuário inicia servidor Windows
2. Servidor escuta TCP/UDP 6969
3. Usuário abre app Android
4. Usuário clica em "Server (USB/WIFI)"
5. hasUsbConnection() retorna false
6. App navega para DeviceListFragment
7. Usuário clica em "Add Device"
8. Usuário digita IP e nome manualmente
9. Usuário clica no dispositivo na lista
10. App tenta TCPConnection(ip, 6969) + UDPConnection(ip, 6969)
11. Se conectar: navega para touchpad
```

### Componentes Envolvidos

#### Servidor (C++)
- `server/src/adb.hpp` — Wrapper para comandos ADB
- `server/src/settingsmanager.cpp` — Gerencia configurações e ADB
- `server/src/net/server.cpp` — Servidor TCP/UDP, inicia ADB no construtor
- `server/src/gui/wxmain.cpp` — Interface gráfica, botão ADB On/Off
- `server/adb/adb.exe` — Binário ADB (Windows)

#### Cliente (Kotlin)
- `client/app/src/main/java/com/darusc/mousedroid/fragments/Main.kt` — Tela inicial
- `client/app/src/main/java/com/darusc/mousedroid/viewmodels/ConnectionViewModel.kt` — Lógica de conexão
- `client/app/src/main/java/com/darusc/mousedroid/networking/ConnectionManager.kt` — Gerencia conexões
- `client/app/src/main/java/com/darusc/mousedroid/networking/NetUtils.kt` — hasUsbConnection(), hasWifiConnection()
- `client/app/src/main/java/com/darusc/mousedroid/networking/sockets/TCPConnection.kt` — Conexão TCP
- `client/app/src/main/java/com/darusc/mousedroid/networking/sockets/UDPConnection.kt` — Conexão UDP

### Problemas Identificados

#### CRÍTICO

| ID | Problema | Causa | Impacto | Solução |
|----|----------|-------|---------|---------|
| C1 | Usuário deve selecionar manualmente o modo de conexão | `startServerMode()` requer clique no botão | Experiência não é "plug-and-play" | Auto-detecção na inicialização |
| C2 | Nenhuma reconexão automática após desconexão | `ConnectionManager.disconnect()` limpa tudo, sem retry | Usuário deve navegar de volta e reconectar manualmente | Lógica de reconexão com backoff |
| C3 | Bug no path do adb.exe no kill command | `adb.hpp:60` usa `path + "\\adb\\adb.exe"` mas adb.exe está em `path + "\\adb.exe"` | `adb kill-server` falha silenciosamente, porta pode ficar ocupada | Corrigir path para `path + "\\adb.exe"` |
| C4 | Nenhuma verificação de dispositivo antes de `adb reverse` | `ADBOn()` executa reverse imediatamente | Se nenhum dispositivo está conectado, reverse falha ou é inútil | Verificar `adb devices` antes |
| C5 | Nenhuma detecção de dispositivo não autorizado | `adb devices` não é consultado | Usuário não sabe que precisa autorizar o PC no Android | Parsear output de `adb devices` |

#### IMPORTANTE

| ID | Problema | Causa | Impacto | Solução |
|----|----------|-------|---------|---------|
| I1 | Botão "ADB On/Off" confuso | Interface expõe detalhes técnicos | Usuário não entende o que é ADB | Remover botão, automatizar |
| I2 | Nenhuma indicação de progresso | `State.Connecting` mostra apenas "Connecting..." | Usuário não sabe o que está acontecendo | Estados mais descritivos |
| I3 | Mensagens de erro genéricas | Popup de erro não diferencia causas | Usuário não sabe o que fazer | Mensagens específicas por causa |
| I4 | hasUsbConnection() pode retornar stale data | USB_STATE é sticky broadcast | Pode conectar quando USB já foi desconectado | Verificar com timeout |
| I5 | Nenhuma validação de dispositivo WiFi salvo | DeviceListViewModel não verifica se IP é alcançável | Usuário vê dispositivos offline na lista | Ping ou timeout curto |

#### COSMÉTICO

| ID | Problema | Causa | Impacto | Solução |
|----|----------|-------|---------|---------|
| CO1 | Textos hardcoded em inglês | Strings literais em Kotlin e XML | Não suportado pela missão | Documentar como limitação |
| CO2 | Nome "Mousedroid" ainda aparece | Não foi renomeado para "Pocket Pad" | Inconsistência de marca | Fora do escopo |

### Responsabilidades dos Processos

| Processo | Responsabilidade | Ciclo de Vida |
|----------|------------------|---------------|
| `Mousedroid.exe` (servidor) | Iniciar ADB, escutar TCP/UDP, injetar input | Inicia manualmente, termina no close |
| `adb.exe` (servidor) | Gerenciar reverse tunnel | Iniciado pelo servidor, termina com kill-server |
| `com.darusc.mousedroid` (app) | Conectar ao servidor, enviar input events | Inicia pelo usuário, termina no onDestroy |
| ADB daemon (Android) | Aceitar conexões do servidor | Sempre ativo quando USB debugging está on |

### Decisões de Simplificação

#### O que manter

- Protocolo binário (1-byte opcode + args)
- Porta 6969
- Formato de handshake (deviceDetails)
- Lógica de input (Translators, GestureHandler)
- Interface de touchpad após conexão

#### O que automatizar

- Detecção de USB na inicialização do app
- Conexão automática quando USB está disponível
- Reconexão automática com backoff exponencial
- Detecção de dispositivo no servidor
- Limpeza de reverse tunnel no encerramento

#### O que simplificar na UI

- Remover seleção manual de modo quando USB está disponível
- Mostrar estados claros: "Procurando computador...", "Conectando...", "Conectado"
- Mensagens de erro específicas: "Dispositivo não autorizado", "Servidor não encontrado"
- Remover botão "ADB On/Off" da interface do servidor

### Riscos Técnicos

1. **ADB não está no PATH do Windows** — O servidor usa `adb.exe` local, mas se o usuário tiver ADB global, pode haver conflito.
   - **Mitigação:** Usar sempre o `adb.exe` do diretório do servidor.

2. **Múltiplos dispositivos USB** — `adb reverse` aplica-se ao primeiro dispositivo.
   - **Mitigação:** Detectar múltiplos dispositivos e mostrar mensagem clara.

3. **Dispositivo não autorizado** — ADB requer autorização RSA do usuário.
   - **Mitigação:** Detectar "unauthorized" no output de `adb devices` e instruir o usuário.

4. **Stale USB_STATE** — Android pode reportar USB conectado quando já foi desconectado.
   - **Mitigação:** Tentar conexão TCP com timeout curto; se falhar, re-verificar USB.

5. **Porta 6969 ocupada** — Se o servidor foi encerrado incorretamente, a porta pode estar em uso.
   - **Mitigação:** `SO_REUSEADDR` já está habilitado no servidor.

### Componentes que Permanecem Inalterados

- `Translators.kt` — Codificação de eventos
- `GestureHandler.kt` — Detecção de gestos
- `InputEvent.kt` — Modelo de dados
- `BluetoothConnection.kt` — Modo Bluetooth (fora do escopo)
- Protocolo binário (opcodes, formato)
- Lógica de input do servidor (Linux/Windows input managers)
