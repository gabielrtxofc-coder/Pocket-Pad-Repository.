# Missão 04 — Pendências, decisões pendentes e questões abertas

> Criado na Missão 04 (tarefas 165 e 169). **Este documento é a lista do que a limpeza NÃO resolveu**,
> com o motivo e quem precisa decidir. Nada aqui foi removido por falta de resposta.

---

## 1. Respostas a questões abertas da Missão 03

A auditoria deixou 22 perguntas em `docs/audit/OPEN-QUESTIONS.md`. Estas foram **respondidas por
evidência** nesta missão:

| Q | Pergunta | Resposta | Evidência |
|---|---|---|---|
| Q18 | `backup_rules.xml`/`data_extraction_rules.xml` são referenciados? | **Não** — o Manifest não tem `fullBackupContent` nem `dataExtractionRules` | `grep` no Manifest → 0 |
| Q21 | Os testes-exemplo devem sair? | **Sim, os dois.** O instrumentado ainda quebra por design no rebranding | `assertEquals("com.darusc.mousedroid", ...)` |
| Q22 | `proguard-rules.pro` tem papel hoje? | **Tem consumidor.** É argumento de `proguardFiles` | `app/build.gradle:24` |
| Q4 | `server/adb/adb.exe` é redistribuível? | **Ainda não verificado** — segue aberta (§3) | — |
| Q17 | `logo.png` é órfão? | **Não.** 2 referências `@drawable/logo` | `fragment_main.xml:19`, `nav_header_mousedroid.xml:12` |
| — | `BatteryMonitor.kt` é código morto? | **Não.** É compilado e `stop()` é chamado | `ConnectionManager.kt:7,16,25,32`; `MainActivity.kt:63` |

---

## 2. Pendências **obrigatórias** para a próxima missão

### P1 — Criar testes automatizados reais ⚠️ PRIORIDADE ALTA

| Campo | Conteúdo |
|---|---|
| **Situação** | A Missão 04 removeu os 2 testes-exemplo. Os jobs `android` e `android-instrumented` do CI agora executam **zero** testes e ficam verdes |
| **Risco** | **Falsa sensação de segurança.** Uma regressão de protocolo passa sem detecção. Já era o risco R8 da auditoria e agora é mais visível |
| **Por que não foi feito aqui** | A Missão 04 proíbe implementar funcionalidades e sua diretriz final é limpeza. Escrever testes de `Translators`/`GestureHandler` é escopo novo |
| **O que fazer** | Na ordem: (a) `Translators.toSocketReport` — bytes por evento; (b) `Translators.toHIDReport` — contagem/tamanho; (c) `KeyboardLayout` — char → `Keycode`; (d) `GestureHandler` — decisão scroll vs move vs clique direito |
| **Plano original** | `docs/audit/CLEANUP-PLAN.md` Etapa 5 |
| **Dependências já prontas** | `junit:4.13.2`, `androidx.test.ext:junit`, `espresso-core` continuam declarados — não será preciso mexer no build |
| **Critério de conclusão** | Os dois jobs executam ≥ 1 teste e o resumo do CI mostra `tests > 0` |

### P2 — Corrigir as 8 divergências preservadas

Detalhadas em [`CLEANUP-EXECUTION.md`](CLEANUP-EXECUTION.md) §4 (D1–D8). Resumo:
uniformizar JDK 17 no `.idea` (D1), resolver MinGW × MSVC (D2), remover dupla definição de
`PLATFORM_SRC` no CMake (D3), corrigir `UpdateView(false)` (D4), `install.bat` → `release.bat` (D5),
`distconfig.ini` fora de `dist\` (D6), `GetFilePath()` sem `return` (D7), `./adb` no Linux (D8).

**Por que não aqui:** são correções de comportamento, não limpeza. A missão proíbe refatorar.

### P3 — Deduplicar os 3 pares de blobs idênticos

| Blob | Arquivos | Quem decide |
|---|---|---|
| `11a7261` | `res/drawable/logo.png` × `imgs/logo.png` (40.170 B) | rebranding: qual é o logo canônico? |
| `e2074a5` | `imgs/icon.png` × `server/icon.png` | rebranding |
| `67820c5` | `ic_launcher.xml` × `ic_launcher_round.xml` | **não deduplicar** — são dois recursos distintos que por acaso têm o mesmo conteúdo hoje |

**Por que não aqui:** remover uma das cópias exige decidir qual é a canônica, o que é decisão de marca.

---

## 3. Decisões pendentes (não são tarefas, são escolhas)

### D1 — O Numpad faz parte do Pocket Pad?

| Campo | Conteúdo |
|---|---|
| **Estado** | 6 arquivos preservados, **INVESTIGAR** |
| **Evidência** | Funcional e completo. Instanciado em `Input.kt:94` via `R.id.mode_numpad` |
| **Custo de remover** | 6 arquivos + 3 pontos em `Input.kt`/menu + `InputEvent.NumpadKeyPress` + `Translators.kt:134,196` + opcode no servidor (`inputmanager.hpp`) → **mexe no protocolo**, que é proibido |
| **Quem decide** | mantenedor de produto |
| **Recomendação** | **Manter até a definição de produto.** O custo de remover é alto e o custo de manter é ~9,7 KB |

### D2 — Política de backup do app

| Campo | Conteúdo |
|---|---|
| **Estado** | `backup_rules.xml` e `data_extraction_rules.xml` preservados, **INVESTIGAR** |
| **Fato relevante** | O Manifest **não** declara `fullBackupContent`, então o **auto-backup padrão está ativo** e inclui `SharedPreferences` — onde vivem os hosts Wi-Fi salvos |
| **Questão de produto** | Os hosts salvos **devem** ir para o backup? Se forem considerados sensíveis, é preciso *referenciar* `data_extraction_rules.xml` e excluí-los |
| **Quem decide** | mantenedor, junto com a decisão de autenticação (R5 da auditoria) |
| **Recomendação** | Decidir **junto** com a autenticação; os dois arquivos são o lugar certo da implementação |

### D3 — Toolchain Windows do servidor: MinGW ou MSVC?

| Campo | Conteúdo |
|---|---|
| **Estado** | `server/.vscode/tasks.json` (MinGW) preservado; conflita com `CMakeSettings.json` + `README` (MSVC/vcpkg) |
| **Questão** | A rota MinGW é suportada? |
| **Recomendação** | MSVC/vcpkg (é a única validada pelo CI). Alinhar `tasks.json` e corrigir |
| **Não remover `tasks.json`** — quem usa VS Code ficaria sem build |

### D4 — `.idea/` completo deve continuar versionado?

| Campo | Conteúdo |
|---|---|
| **Estado** | 11 arquivos preservados por cumprirem a regra objetiva (§3 de `REPOSITORY-HYGIENE.md`) |
| **Questão** | O time prefere `.idea/` fora do repositório por completo? |
| **Trade-off** | Versionar ajuda 1 dev e atrapalha N; a alternativa é ignorar tudo e versionar só `.name` |
| **Não decidível nesta missão** | A missão proíbe remover configurações de IDE indiscriminadamente, e 10 dos 11 não têm caminho de máquina |
| **Recomendação** | Reavaliar **depois** do rebranding, quando `.name`, `misc.xml`, `compiler.xml` e `kotlinc.xml` precisarão ser editados de qualquer forma |

### D5 — Licença de redistribuição dos binários ADB

| Campo | Conteúdo |
|---|---|
| **Estado** | `server/adb/*` (6.098.248 B = **84,1% do repositório**) preservado e **protegido** |
| **Risco jurídico** | Licença de redistribuição do ADB da Google **não verificada** |
| **Não fazer** | **Não remover** — desabilita o USB no Windows, um dos três modos do produto |
| **Ação** | verificar a licença e, se necessário, mover para download/instalação em vez de redistribuição. Isso **aumenta** o risco de regressão, então exige missão própria |
| **Quem decide** | mantenedor |

---

## 4. O que **permanece sem resposta** (honestidade de escopo)

| # | Pergunta aberta | Por que não foi resolvida |
|---|---|---|
| 1 | O servidor Linux consegue fazer USB/ADB de fato? | `settingsmanager.cpp:127` chama `./adb` mas `server/adb/` só tem binários **Windows**. É defeito (D8), exige teste em hardware com Android |
| 2 | O modo Bluetooth funciona em hardware real? | Exige dois dispositivos. A auditoria já registrou como não validado |
| 3 | A latência artificial (10 ms TCP, 15 ms UDP, 2 s BT) é aceitável para o Pocket Pad? | É decisão de UX/produto, não de limpeza |
| 4 | O `cmake/bin/Mousedroid` do `launch.json`/`install.sh` é o caminho correto? | O CI usa `build/server-linux/bin/`; o README usa `cmake/`. Divergência não resolvida (D2) |
| 5 | Alguma ferramenta externa consome este repositório por caminho (`local.properties`, `client/README.md`)? | Impossível provar ausência de consumidores externos; o risco é aceito e documentado |
| 6 | O `.idea/` é usado por outro desenvolvedor além do autor original? | Só o mantenedor pode responder |

---

## 5. Riscos aceitos conscientemente

| Risco | Mitigação |
|---|---|
| Jobs de CI verdes com 0 testes | Registrado como P1; os jobs ainda validam **compilação** de Android, Linux e Windows |
| Histórico Git contém caminhos de máquina antigos | Reescrita de histórico é proibida; são caminhos, não credenciais |
| `server/adb/*` sem licença verificada | Não removido (protegido); documentado em D5 |
| Remoções de `.idea/` sem teste em IDE real | Nenhum arquivo removido é lido por build/CI; são metadados que o IDE recria |
