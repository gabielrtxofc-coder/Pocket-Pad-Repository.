# Auditoria estrutural do Pocket Pad — Missão 03

> **Natureza desta missão:** auditoria e documentação. **Nenhum arquivo de código, build, protocolo,
> licença ou CI foi alterado.** As únicas adições são os oito documentos desta pasta.
> Branch de sessão: `arena/45d35856-pocket-pad-repository` · Base: `164372b` (= `main`, = commit esperado da Missão 02.5).

---

## 1. Índice dos entregáveis

| # | Documento | Conteúdo | Tamanho |
|---|---|---|---|
| 1 | [`README.md`](README.md) | Este índice + resumo executivo + verificação da fundação | — |
| 2 | [`FILE-INVENTORY.tsv`](FILE-INVENTORY.tsv) | Inventário dos **206** arquivos rastreados, 13 colunas por arquivo | 207 linhas |
| 3 | [`ARCHITECTURE.md`](ARCHITECTURE.md) | Arquitetura Android + servidor C++ + caminho completo de cada ação + 15 problemas de manutenção | 6 seções |
| 4 | [`PROTOCOLS.md`](PROTOCOLS.md) | USB/ADB, Bluetooth e Wi-Fi analisados **separadamente**, com estado real de cada um | 5 seções |
| 5 | [`DEPENDENCIES.md`](DEPENDENCIES.md) | Dependências internas e externas, versões, licenças indicativas, consequências de remoção | 7 seções |
| 6 | [`CLASSIFICATION.tsv`](CLASSIFICATION.tsv) | **36 componentes** classificados em MANTER / ADAPTAR / REMOVER / INVESTIGAR com justificativa e evidência | 37 linhas |
| 7 | [`CLEANUP-PLAN.md`](CLEANUP-PLAN.md) | 14 etapas independentes, com risco, teste e rollback; nada executado | 14 etapas |
| 8 | [`OPEN-QUESTIONS.md`](OPEN-QUESTIONS.md) | **22 dúvidas técnicas**, 10 decisões arquiteturais pendentes e 8 limitações declaradas | 22 perguntas |

Documentos de apoio desta missão: [`FOUNDATION-VERIFICATION.md`](FOUNDATION-VERIFICATION.md) (Fase 01 detalhada, incluindo as divergências encontradas).

Leitura recomendada: **este README** → `ARCHITECTURE.md` §1 → `PROTOCOLS.md` §0 → `CLASSIFICATION.tsv` → `CLEANUP-PLAN.md` §Sequenciamento.

---

## 2. Resumo executivo

### 2.1 O que a fundação é

Um projeto **pequeno e coeso**: **206 arquivos**, ~6,99 MB de blobs, distribuídos em
**151 arquivos de cliente Android** (Kotlin + recursos + build), **42 de servidor C++** (Linux e Windows),
3 de metadados Fastlane, 5 imagens e 2 documentos de fundação.

Três modos de conexão, com **dois caminhos de execução**:

- **Wi-Fi** e **USB/ADB** → passam pelo servidor C++, que injeta a entrada via `/dev/uinput` (Linux) ou `SendInput` (Windows).
- **Bluetooth** → o Android é um **dispositivo HID** e o PC é o host; **o servidor não participa**.

A separação de camadas é boa nos dois lados: MVVM + transporte abstrato no cliente (`Connection` → TCP/UDP/Bluetooth), e rede / injeção de entrada / GUI no servidor.

### 2.2 Números da classificação

| Classificação | Arquivos | Componentes | Leitura |
|---|---:|---:|---|
| **MANTER** | **71** | **6** | Base sólida: protocolo, modelo de eventos, teclado, transporte abstrato, build |
| **ADAPTAR** | **101** | **23** | A maior parte — rebranding, i18n, design, robustez. **Não é reescrita.** |
| **REMOVER** | **13** | **3** | Código morto comprovado + higiene de repositório |
| **INVESTIGAR** | **21** | **4** | Depende de decisão de produto (numpad, ADB, `.vscode`, `.idea`, backup) |
| **Total** | **206** | **36** | Cobertura: 206/206 arquivos, cada um em exatamente 1 componente |

Confiança das classificações (por arquivo): **Alta 138 · Média 62 · Baixa 6**.

> As 13 remoções propostas somam **184 KB de 6.995 KB (2,6%)**. Não é uma auditoria "para encolher o projeto": é uma auditoria para **compreendê-lo**. O peso do repositório (84%) está em `server/adb/adb.exe`, classificado como **INVESTIGAR** e explicitamente **não recomendado para remoção**.

### 2.3 Principais dependências

**Android:** AGP 8.9.1 · Kotlin 2.1.0 · Gradle 8.11.1 · `compileSdk/targetSdk 34` · `minSdk 28` (determinado por `BluetoothHidDevice`) · Java 17 · Material 1.5.0 · Navigation 2.7.7 · Coroutines **1.3.9**.

**Servidor:** C++17 · CMake ≥ 3.26 · **Asio** (1.28.1 Linux / 1.38.2 vcpkg) · **wxWidgets 3.3** (3.3.4 compilado no CI, pois o APT do Ubuntu 24.04 traz 3.2) · `user32`/`ws2_32`/`wsock32` · `/dev/uinput` + udev · **ADB** (3 binários Windows, 5,9 MB).

**Três dependências declaradas mas não usadas:** Gson 2.10.1 (zero imports), `kotlin-kapt` (zero processadores), Espresso 3.5.1 (zero usos).

### 2.4 Principais riscos identificados

| # | Risco | Severidade | Onde |
|---|---|---|---|
| R1 | **CI desligado na prática**: `on.push.branches` fixo em `arena/39232774-…` (branch da Missão 02) | **Alta** | `foundation-build.yml:4-12` |
| R2 | **Protocolo binário duplicado** (Kotlin ↔ C++), sem versão, sem tamanho, sem CRC | **Alta** | `Translators.kt:25-36` ↔ `inputmanager.hpp:22-31` |
| R3 | **Latência artificial fixa**: 10 ms por relatório TCP, 15 ms por datagrama UDP, 2 s no connect BT | **Alta** (UX) | `TCPConnection.kt:59`, `UDPConnection.kt:46`, `BluetoothConnection.kt:193` |
| R4 | **Parser sem validação de tamanho**: `tokens[0..3]` sem checar; `bytes[1]/bytes[2]` sem checar `size` | **Alta** (UB) | `connection.cpp:18-21`, `inputmanager.hpp:47-86` |
| R5 | **Sem autenticação** na porta 6969: qualquer host da LAN injeta eventos | **Alta** | `server.cpp`, `UDPConnection.kt` |
| R6 | **USB no Linux provavelmente quebrado**: chama `./adb`, mas `server/adb/` só tem binários Windows | Alta | `settingsmanager.cpp:127` |
| R7 | **Concorrência no servidor**: logger escreve em `wxListView` a partir do thread do `io_context`; vetor `connections` sem mutex | Alta | `logger.cpp:13`, `server.cpp:113` |
| R8 | **Nenhum teste real**: 2 exemplos gerados pelo Android Studio; o instrumentado quebra por design no rebranding | Alta | `Example*Test.kt` |
| R9 | Configuração de máquina versionada (`local.properties`, `.gradle`, `.idea` com caminhos do autor) | Média | C06 |
| R10 | Toolchain Windows divergente (MinGW via VS Code vs MSVC/vcpkg) | Média | `tasks.json` vs `CMakeSettings.json` |

### 2.5 Recomendações de limpeza (ordem, não execução)

```text
Etapa 0  consertar o gatilho do CI           ← bloqueia tudo (risco baixo)
Etapa 1  remover client/.gradle/ (4 arquivos) ← risco muito baixo
Etapa 2  destrackear local.properties         ← risco baixo
Etapa 3  bits +x em gradlew/install.sh/release.sh
Etapa 4  remover 5 drawables órfãos
Etapa 5  criar testes reais (habilita remoções de código)
Etapa 6  remover caminho de bateria (código morto comprovado)
Etapa 7  remover PacketType / Connection::Send (código morto)
Etapa 8  remover client/README.md vazio
Etapa 9  7 correções pontuais de defeitos do servidor (commits isolados)
Etapa 10 decisão sobre .idea/ (requer mantenedor)
Etapa 11 remover Gson / kotlin-kapt / Espresso
Etapa 12-13 higiene de nomes e atualização planejada de versões
```

Detalhes, riscos, testes e rollback de cada etapa em `CLEANUP-PLAN.md`.

### 2.6 O que a fundação sustenta do Pocket Pad

**Já existe e é diretamente reaproveitável:** `GestureHandler` (tap, duplo-tap→drag, 2 dedos para clique direito e scroll, pinch-to-zoom, drag, fullscreen landscape), `InputEvent` (contrato de eventos), `Translators` (encoders socket e HID), `ConnectionManager`, os 3 transportes, os 4 layouts de teclado com dead keys, e todo o servidor de injeção.

**Precisa ser adaptado:** identidade visual e `namespace`/`applicationId`, i18n (todo o texto está hardcoded em inglês nos Fragments), latência do transporte, robustez do protocolo, GUI do servidor.

**Precisa ser desenvolvido do zero:** inércia/rolagem natural no trackpad, sensibilidade configurável no cliente, descoberta automática de servidor, autenticação, e testes de protocolo.

**Riscos técnicos para essa evolução:** o protocolo sem versão (R2), a latência embutida (R3) e a ausência total de cobertura de testes (R8) — nesta ordem.

**Decisões pendentes:** 10, listadas em `OPEN-QUESTIONS.md` §E. As quatro que mais impactam o produto: versionar o protocolo (D1), transporte primário (D2), Bluetooth como modo principal (D4) e escopo do teclado/numpad (D9).

---

## 3. Estado de verificação da fundação (Fase 01)

| Verificação | Resultado |
|---|---|
| Repositório e `origin` | ✅ `origin` = `https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository..git` |
| Branch e commit | ✅ `arena/45d35856-pocket-pad-repository` @ `164372b6a39bd6c5ec13a7ae858b3167a47915ff` |
| `main` = commit esperado | ✅ `main` == `origin/main` == `164372b` == o commit esperado. **Também é o `HEAD` do remoto** (nenhum descendente) |
| Alterações locais pendentes | ✅ **Nenhuma** (`git status --porcelain` vazio no início da missão) |
| Documentos de fundação | ✅ `docs/foundation/BASELINE.md` (177 linhas) e `docs/foundation/BUILD-VALIDATION.md` presentes |
| Histórico pós-importação | ✅ 9 commits identificados e auditados — ver `FOUNDATION-VERIFICATION.md` |
| Divergência encontrada | ⚠️ O clone inicial era **shallow (1 commit)**; corrigido com `git fetch --unshallow` → 120 commits |
| Alterações de código vs upstream v1.5 | ✅ Exatamente **1 linha** em `server/src/net/server.cpp` (correção Asio), já documentada |
| Reset / limpeza / sobrescrita | ✅ **Nenhum** executado |

Detalhamento completo, incluindo a análise dos 9 commits e a comparação com o upstream: **[`FOUNDATION-VERIFICATION.md`](FOUNDATION-VERIFICATION.md)**.

---

## 4. Método

1. **Inventário mecânico:** `git ls-files` (206 arquivos) + `stat` por arquivo para tamanho exato.
2. **Inspeção de conteúdo:** leitura integral dos 37 arquivos Kotlin e dos 26 arquivos C++ de `src/`, mais build, scripts, CI, Manifest, layouts e recursos. Nenhuma conclusão sobre comportamento foi tirada apenas do nome do arquivo.
3. **Verificação por evidência negativa:** as remoções e os "órfãos" foram provados por varredura (`grep`) documentada, não por impressão.
4. **Coerência cruzada:** cada opcode do protocolo foi conferido nos **dois lados** (Kotlin e C++); cada formato de handshake idem; cada porta em todos os arquivos.
5. **Classificação conservadora:** na dúvida, `INVESTIGAR` — nunca `REMOVER`. 21 arquivos ficaram em INVESTIGAR.
6. **Rastreabilidade:** cada linha do inventário e da classificação carrega `arquivo:linha`.

### Como reproduzir as verificações principais

```bash
cd /caminho/do/repositorio

# fundação
git rev-parse HEAD main origin/main
git status --porcelain
git log --oneline 7fc8885..HEAD
git diff --stat 3d077786c4ab333f648fc3c2b2ad668203af09b2 HEAD

# inventário
git ls-files | wc -l                                  # 206
git ls-files | xargs -I{} stat -c'%s\t%n' {} | sort    # tamanhos

# órfãos de recursos (a prova das remoções)
cd client/app/src/main/res
for f in drawable/*.xml; do n=$(basename "$f" .xml); \
  grep -rqF -- "$n" ../../../../app/src || echo "ORFAO: $f"; done

# código morto
grep -rn "PacketType\|ACTIVATION\|CAMERA" server/src      # só a declaração
grep -rn "com.google.gson" client/app/src                 # nada
grep -rn "BatteryMonitor" client/app/src                  # start() comentado
grep -rn "FOREGROUND_SERVICE\|WAKE_LOCK" client/app/src   # nada

# dependências não usadas do Android
grep -rn "Espresso\|onView" client/app/src
```

---

## 5. Confirmação de escopo

| Restrição da missão | Cumprida |
|---|---|
| Não remover arquivos | ✅ |
| Não alterar arquivos-fonte | ✅ (0 arquivos em `client/`, `server/`, `LICENSE`) |
| Não refatorar | ✅ |
| Não alterar protocolos | ✅ |
| Não modificar dependências | ✅ |
| Não alterar configurações de build | ✅ |
| Não alterar o workflow de CI | ✅ |
| Não fazer redesign | ✅ |
| Não implementar funcionalidades | ✅ |
| Não alterar a licença | ✅ |
| Não fazer merge / não modificar a `main` | ✅ |
| Não force push / não excluir branches | ✅ |
| Não modificar o repositório antigo | ✅ |
| Apenas documentação e inventários adicionados | ✅ `docs/audit/` (8 arquivos) |

**Nenhuma alteração funcional foi introduzida.** A verificação final está na última seção do relatório da missão.
