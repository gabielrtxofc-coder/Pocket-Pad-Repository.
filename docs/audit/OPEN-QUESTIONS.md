# Dúvidas técnicas, limitações e pontos a investigar

> Missão 03. Este documento existe para registrar **o que não foi possível concluir**, com a mesma
> honestidade com que se registra o que foi. Nenhuma conclusão foi inventada para preencher lacunas.
> Legenda de bloqueio: **[AMBIENTE]** limitação do sandbox · **[HARDWARE]** exige dispositivo/PC real ·
> **[PRODUTO]** depende de decisão do mantenedor · **[CÓDIGO]** exige leitura/experimento adicional.

---

## A. Limitações do ambiente desta auditoria

| # | Limitação | Impacto na auditoria | Mitigação |
|---|---|---|---|
| L1 | **[AMBIENTE]** O clone inicial era **shallow (depth 1)**: `.git/shallow` continha `164372b`, `git rev-list --count HEAD` retornava **1** commit. | A Fase 01 pedia consultar o histórico pós-importação. | Resolvido com `git fetch --unshallow origin` → **120 commits** alcançáveis, histórico completo analisado (§ ver README). Não houve reescrita. |
| L2 | **[AMBIENTE]** Sem JDK, Android SDK, Gradle, CMake, wxWidgets, Asio no sandbox. | Não foi possível compilar nem executar nada nesta missão. | A comprovação de build apoia-se no CI (execução 42524050240, registrada em `docs/foundation/BUILD-VALIDATION.md`). Nada foi revalidado aqui. |
| L3 | **[AMBIENTE]** Sem dispositivo Android e sem PC com Windows/X11. | Nenhum dos três protocolos foi exercitado. | Documentado explicitamente em `PROTOCOLS.md` §0 e §5. |
| L4 | **[AMBIENTE]** Logs/artifacts do GitHub Actions não acessíveis (apenas API). | Resultados de build tomados de `BUILD-VALIDATION.md`, não reverificados. | Tratado como evidência secundária. |

---

## B. Origem e estado da fundação

**Q1. A validação prática em celular real citada no contexto da Missão 03 — quais modos foram exercitados?**
O contexto afirma "responsividade satisfatória e interação funcional básica" em um celular real, mas não registra qual protocolo (Wi-Fi, USB ou Bluetooth), qual build (APK debug do CI?), qual PC (Windows/Linux) nem quais gestos. **Não é possível atribuir a evidência a nenhum dos três modos.** Registrar por protocolo é pré-requisito para dizer que algo "funciona".
→ **[HARDWARE]** + **[PRODUTO]**

**Q2. O histórico de 109 commits upstream foi verificado por conteúdo?**
Foram verificados: total de 120 commits alcançáveis, os 9 commits pós-importação, e o diff `3d07778..HEAD` (4 arquivos: workflow, 2 docs, `server.cpp` 1 linha). **Não** foi feita revisão commit a commit dos 109 originais.
→ já respondido no que importa; detalhe histórico completo **[CÓDIGO]** se necessário.

**Q3. `git diff 3d07778 HEAD` mostra `server/src/net/server.cpp` com 2 linhas alteradas (1+/1-). Isso é exatamente a correção Asio documentada?**
Sim: `git diff --stat` retorna `server.cpp | 2 +-`, e `BUILD-VALIDATION.md §8` documenta a troca `address::from_string` → `make_address` na linha 35. **Confirmado.** Nenhuma outra alteração de código existe contra o upstream.

---

## C. Pontos de código que exigem investigação adicional

**Q4. `server/adb/adb.exe` é redistribuível?**
O binário e as duas DLLs vêm do Android SDK Platform Tools (Google). A licença de redistribuição **não foi verificada** nesta auditoria (é uma questão jurídica, não técnica). O `BASELINE.md §8` já marcava isso como pendência. É relevante porque são **84% do tamanho do repositório**.
→ **[PRODUTO]** (jurídico)

**Q5. O `resource.rc` é realmente compilado no Windows?**
`CMakeLists.txt` define `PLATFORM_SRC` duas vezes: em `:22-27` (incluindo `resource.rc`) e novamente em `:41-46` (apenas o `.cpp`). A segunda sobrescreve a primeira, e `add_executable(... ${PLATFORM_SRC})` usa o valor final. **Hipótese forte:** o `.rc` não entra e o ícone do `.exe` vem de outra fonte. Não foi possível confirmar sem compilar e inspecionar o `.exe`.
→ **[CÓDIGO]**

**Q6. O modo USB funciona no Linux?**
`settingsmanager.cpp:127` executa `./adb reverse tcp:6969 tcp:6969` (caminho **relativo**), mas `server/adb/` contém apenas binários **Windows** (`adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll`) e `release.sh:9` copia `adb/*` para `mousedroid_linux/adb/` — ou seja, DLLs e `.exe` para um pacote Linux. Não há evidência de que `./adb` exista no diretório de trabalho em Linux. **Hipótese:** o USB no Linux só funciona se o usuário criar manualmente o link/cópia, ou não funciona.
→ **[HARDWARE]**

**Q7. O Bluetooth HID conecta de forma confiável?**
Há um `delay(2000)` fixo antes de `connect()` com o comentário *"otherwise connection fails immediately"* (`BluetoothConnection.kt:190-193`) e um `unregisterApp()` preventivo para "ghost states" (`:141-143`). Ambos indicam problemas reais encontrados pelo autor, mas **não há registro de teste**. O comportamento com stacks Windows 10/11 vs Linux (BlueZ) é desconhecido.
→ **[HARDWARE]**

**Q8. O que acontece se `GetHostInfo()` não tiver rota para `8.8.8.8`?**
`server.cpp:35` faz `socket.connect(...)` **sem try/catch**, dentro de `GetHostInfo()`, chamado em `OnInit` (`wxapplication.cpp:15`). Em rede isolada (sem gateway), a chamada pode lançar `system_error` e derrubar a inicialização. **Não reproduzido.**
→ **[CÓDIGO]** / **[HARDWARE]**

**Q9. `PacketType` (FRAME, RESOLUTION, ACTIVATION, CAMERA, QUALITY, WB, EFFECT) é resíduo de quê?**
São 7 constantes em `server.h:31-40` sem nenhum uso, junto com `Connection::Send` nunca chamado, `udpsocket.send_to("done")` comentado (`server.cpp:129-135`) e `"video streaming"` no comentário de `server.h:20-22`. Tudo indica um protocolo de controle de streaming de vídeo (provavelmente herdado de um projeto anterior do autor). **Não há código do lado Android correspondente.** Confirmar se há intenção futura antes de remover.
→ **[PRODUTO]**

**Q10. `maxPacketSize = 65472` (TCP e UDP) tem algum consumidor?**
`Connection.kt:14` declara `open val maxPacketSize`; `TCPConnection.kt:27` e `UDPConnection.kt:24` sobrescrevem. **Nenhuma leitura** em todo o código. Resíduo de um caminho de fragmentação/streaming. Seguro remover, mas é código morto de baixo impacto.
→ **[CÓDIGO]** (já resolvido na prática)

**Q11. Por que `scroll()` no Linux aplica Shift+wheel e `scroll_h()` usa `REL_HWHEEL`, enquanto no Windows ambos usam Shift?**
`linuxinputmanager.cpp:99-115` vs `windowsinputmanager.cpp:46-74`. O cliente envia `SCROLL (0x06)` para `dy` e `SCROLL_H (0x08)` para `dx` (`Translators.kt:165-171`). A intenção declarada em `Translators.kt:102-106` (Bluetooth) é que Shift+wheel produza scroll horizontal. **Hipótese:** no Linux o `scroll` "vertical" também vira horizontal — possível bug de comportamento, não confirmado em execução.
→ **[HARDWARE]**

**Q12. Mid-click não existe no modo socket: intencional?**
`Translators.kt:173-177` mapeia `MouseButton.MIDDLE` para `RCLICK`. Existe `BTN_MIDDLE` registrado no Linux (`linuxinputmanager.cpp:24`) e `getMouseButtonHIDCode` retorna 4 para MIDDLE no Bluetooth. Ou seja, a capacidade existe em dois lugares e falta no terceiro. **Hipótese:** lacuna/oversight, não decisão.
→ **[CÓDIGO]**

**Q13. `hasUsbConnection()` detecta corretamente um cabo conectado?**
`NetUtils.kt:10-16` registra um receiver **nulo** para `android.hardware.usb.action.USB_STATE` e lê o extra. Esse broadcast só é enviado durante a transição, e `registerReceiver` com `receiver = null` retorna o *sticky intent*. Funciona em teoria, mas depende de o sistema manter o sticky. **Não verificado em dispositivo.**
→ **[HARDWARE]**

**Q14. `ConnectionManager` é um singleton com callback mutável — há risco de vazamento/estado inconsistente?**
`getInstance(callback)` (`:30-38`) sobrescreve o callback. `ConnectionViewModel` é `activityViewModels` (escopo da Activity), então em geral há um só. Mas `TouchpadViewModel`/`KeyboardViewModel`/`NumpadViewModel` chamam `getInstance()` sem callback. Após recriação de Activity (não configurada em `configChanges` para `uiMode`/`locale`), o callback antigo pode persistir por um instante. **Não reproduzido.**
→ **[CÓDIGO]**

**Q15. O logging é thread-safe?**
`Logger::displayOutput` (`logger.cpp:8-19`) faz `monitor->InsertItem` numa `wxListView` a partir de callbacks do `io_context` (thread separada) e usa um `std::stringstream` global sem mutex. Além disso, `Logger::log` é variadic template e **concatena em um buffer global**, então mensagens de threads diferentes podem se misturar. **Hipótese de corrupção de log e possível crash em wxWidgets** (que exige GUI no thread principal). Não reproduzido.
→ **[CÓDIGO]** — provavelmente o risco mais sério do servidor depois do protocolo.

**Q16. `connections` (vetor) é acessado concorrentemente?**
`server.cpp:113` (push_back no handler async) vs `:90-98` (`GetConnectedDevicesInfo`, chamado da GUI por `wxApplication::OnDeviceConnected`). **Sem mutex.** Risco de corrida/UB sob conexões simultâneas.
→ **[CÓDIGO]**

---

## D. Lacunas de inventário e classificação

**Q17. `client/app/src/main/res/drawable/logo.png` é realmente órfão?**
Não há referência `@drawable/logo` nem `R.drawable.logo` (verificado). Mas é idêntico em tamanho a `imgs/logo.png` (40.170 B) e pode ter sido usado em uma versão anterior. Classificado como **INVESTIGAR** por prudência.
→ **[CÓDIGO]** (busca em histórico Git resolve)

**Q18. `AndroidManifest.xml` referencia `backup_rules.xml`/`data_extraction_rules.xml`?**
**Não.** Verificado por grep: não há `android:fullBackupContent` nem `android:dataExtractionRules`. Os dois arquivos são templates vazios. Classificados como **INVESTIGAR** (e não REMOVER) porque a política de backup afeta os hosts Wi-Fi salvos em `SharedPreferences("devices")`.
→ **[PRODUTO]**

**Q19. `server/.vscode/tasks.json` (MinGW) já funcionou?**
Divergente de `CMakeSettings.json` (MSVC) e do README (VS+vcpkg). Não há evidência em nenhuma direção. **INVESTIGAR.**
→ **[CÓDIGO]** / **[PRODUTO]**

**Q20. Os 4 layouts de teclado são todos utilizáveis?**
US/ES/FR/RO estão registrados (`KeyboardViewModel.kt:19-22`) e há suporte a dead keys. Romeno não aparece no README nem no fastlane. **Não há informação sobre demanda** para RO — classificado MANTER (funciona) mas registrado aqui.
→ **[PRODUTO]**

**Q21. O Numpad deve fazer parte do Pocket Pad?**
Componente completo e funcional, mas a visão de produto descrita na Missão 03 (experiência Logitech-like de trackpad) não o menciona. Remover arrastaria 6 arquivos + menu + 3 drawables. **Decisão de produto não tomada** → INVESTIGAR.
→ **[PRODUTO]**

**Q22. `proguard-rules.pro` tem papel hoje?**
`minifyEnabled false` (`app/build.gradle:23`) e não há APK release assinado validado. O arquivo é inerte. Não foi possível determinar a intenção. **INVESTIGAR.**
→ **[PRODUTO]**

---

## E. Decisões arquiteturais pendentes (registradas, não tomadas)

| # | Decisão | Opções | Consequência |
|---|---|---|---|
| D1 | **Versionar o protocolo?** | (a) manter binário sem versão; (b) adicionar handshake de versão; (c) migrar para framing com tamanho | (b)/(c) exigem mudança coordenada Android+C++ e quebram compatibilidade com Mousedroid instalado |
| D2 | **Transporte primário do Pocket Pad** | (a) manter UDP+TCP; (b) só TCP; (c) WebSocket/QUIC | afeta latência, perda de pacotes e firewall |
| D3 | **Eliminar a latência artificial?** | remover `sleep(10/15)`, trocar por coalescência por frame | é o maior ganho de fluidez, mas exige repensar a taxa de eventos e o buffer de 255 B no servidor |
| D4 | **Bluetooth como modo principal?** | sim (dispensa instalar servidor) / não (menos controle) | se sim, o foco sai do C++ e vai para o descriptor/relatórios HID; e o servidor pode virar opcional |
| D5 | **Autenticação no Wi-Fi** | nenhuma / token no handshake / TLS | hoje qualquer host da LAN pode injetar eventos |
| D6 | **Servidor headless?** | manter GUI wxWidgets / separar núcleo de GUI / tray-only | hoje `logger.h` inclui `wx/wx.h`, acoplando tudo à GUI |
| D7 | **Descoberta automática de servidor** | manual (atual) / mDNS / broadcast UDP / QR | impacta a UX de onboarding |
| D8 | **Identidade e namespace** | `com.darusc.mousedroid` → novo | quebra o teste instrumentado e exige migração de `SharedPreferences` se o app já estiver instalado |
| D9 | **Manter o numpad e o teclado?** | sim/não | define escopo do redesign |
| D10 | **Caminho de `config.ini` e `app.log`** | cwd (atual) / `~/.config` / `%APPDATA%` | cwd falha quando iniciado pelo menu de aplicativos (o `Exec=` do .desktop não define cwd) |

> **D10 é um risco concreto não verificado:** `install.sh` cria um `.desktop` com `Exec=/usr/share/mousedroid/Mousedroid` e `Terminal=false`, sem `Path=`. O app abre `config.ini` e `app.log` no **diretório de trabalho**, que nesse caso é o home do usuário — não `/usr/share/mousedroid`. As sensitividades salvas podem ir para `~/config.ini`. Não reproduzido (exigiria Linux desktop).

---

## F. O que esta auditoria **não** cobre

1. **Testes funcionais reais** de qualquer protocolo (§L3).
2. **Auditoria jurídica** de licenças (Gson, coroutines, Material, wxWidgets, Asio, ADB) — apenas indicações.
3. **Revisão dos 109 commits upstream** individualmente.
4. **Análise de desempenho medida** (latência real, taxa de eventos, consumo de bateria do app).
5. **Segurança ofensiva** (fuzzing do parser de opcodes e do handshake; varredura de portas).
6. **Acessibilidade** (contraste, tamanhos de toque, TalkBack) do app atual.
7. **Compatibilidade com Android 15/16** além do `targetSdk 34` declarado.
8. **Wayland** — o autor apenas menciona que o tray icon pode não aparecer; não foi testado.

---

## G. Recomendação de próximos passos (para a Missão 04)

1. Executar a **Etapa 0** do `CLEANUP-PLAN.md` (consertar o gatilho do CI) — nada mais pode ser validado antes disso.
2. Produzir um **registro de teste funcional por protocolo** (Q1): uma tabela com data, aparelho, PC, OS, protocolo, gesto e resultado. Isso converte "código presente" em "funciona".
3. Responder às decisões **D1, D2, D4 e D9** — elas determinam se a próxima missão é de refatoração de transporte, de Bluetooth ou de UI.
4. Investigar **Q6 (USB no Linux)**, **Q15/Q16 (concorrência no servidor)** e **Q5 (resource.rc)** — são os três achados técnicos com maior chance de serem defeitos reais.
