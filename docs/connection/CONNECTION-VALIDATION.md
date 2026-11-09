# CONNECTION-VALIDATION.md

## Plano de Validação Física

### Pré-requisitos
- Celular Android com USB debugging habilitado
- Cabo USB funcional
- Computador Windows com Pocket Pad server
- ADB driver instalado

### Primeira Conexão

1. **No computador:**
   - Iniciar `Mousedroid.exe`
   - Verificar status "Device ready" na aba Status
   
2. **No celular:**
   - Conectar cabo USB
   - Autorizar o computador (diálogo "Allow USB debugging?")
   - Marcar "Always allow from this computer"
   - Abrir Pocket Pad
   
3. **Resultado esperado:**
   - App mostra "Connecting to computer..."
   - Após 1-3 segundos: touchpad aparece
   - Mover dedo → cursor se move

### Conexões Seguintes

1. Conectar cabo USB
2. Abrir Pocket Pad
3. **Resultado esperado:** Conexão automática em 1-2 segundos

### Desconexão e Reconexão

1. **Remover cabo USB durante uso:**
   - Popup "Disconnected" aparece
   - Mensagem: "Reconnecting automatically..."
   
2. **Reconectar cabo:**
   - Reconexão automática em 1-5 segundos
   - Touchpad volta a funcionar

3. **Desligar servidor:**
   - Popup "Disconnected" aparece
   - Reconexão tenta automaticamente
   - Após 10 tentativas: volta à tela inicial

### Estados do Servidor

| Status no servidor | Significado | Ação do usuário |
|---|---|---|
| "Device ready" | Dispositivo conectado e autorizado | Pode abrir o app |
| "No Android device connected" | Nenhum USB | Conectar cabo |
| "Authorize USB debugging on phone" | Dispositivo conectado, não autorizado | Autorizar no celular |
| "Multiple devices detected" | Mais de um dispositivo | Manter apenas um |

### Estados do App

| Estado | O que o usuário vê |
|--------|-------------------|
| Procurando | "Connecting to computer..." + spinner |
| Conectando | "Connecting via USB..." + spinner |
| Conectado | Touchpad visível |
| Reconectando | "Reconnecting... (attempt N)" + spinner |
| Erro | Popup com causa e sugestão |

### Problemas Comuns

| Sintoma | Causa provável | Solução |
|---------|---------------|---------|
| "No Android device connected" | USB debugging desativado | Ativar em Developer Options |
| "Authorize USB debugging" | Não autorizou o PC | Aceitar diálogo no celular |
| "Connecting..." eterno | Servidor não está rodando | Iniciar Mousedroid.exe |
| "Connection failed" | Porta 6969 bloqueada | Verificar firewall |
| Cursor não se move | Conexão instável | Reconectar cabo |

### Dispositivo de Referência

- **Galaxy A12** (SM-A125M)
- Android 12/13
- USB debugging em Developer Options
