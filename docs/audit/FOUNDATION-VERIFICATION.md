# Verificação da fundação — Fase 01 detalhada

> Missão 03, Fase 01. Complemento de `README.md` §3.
> Executado **sem** reset, limpeza, checkout destrutivo ou sobrescrita do ambiente de trabalho.

---

## 1. Identificação do repositório

```text
cwd     : /home/user/Pocket-Pad-Repository.
origin  : https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository..git
          (fetch e push — mesmo URL)
```

Confirmado também por `git ls-remote origin`:

```text
164372b6a39bd6c5ec13a7ae858b3167a47915ff   HEAD
164372b6a39bd6c5ec13a7ae858b3167a47915ff   refs/heads/main
d58a408c15ec8d8d6b12732f1521dd4564d7a519   refs/heads/arena/39232774-pocket-pad-repository
d58a408c15ec8d8d6b12732f1521dd4564d7a519   refs/pull/1/head
```

**Nota sobre o nome do repositório:** ele termina em ponto (`Pocket-Pad-Repository.`). Isso é um
defeito conhecido e já documentado (`BUILD-VALIDATION.md §9`): invalida o caminho padrão de checkout
no Windows e exigiu workaround no CI. É um problema pré-existente do repositório de destino, não desta
missão. **Não foi alterado** (renomear o repositório está fora do escopo).

---

## 2. Branch e commit

```text
* arena/45d35856-pocket-pad-repository   164372b
  main                                  164372b  [origin/main]
```

```text
HEAD                = 164372b6a39bd6c5ec13a7ae858b3167a47915ff
main                = 164372b6a39bd6c5ec13a7ae858b3167a47915ff
origin/main         = 164372b6a39bd6c5ec13a7ae858b3167a47915ff
origin/HEAD         = 164372b6a39bd6c5ec13a7ae858b3167a47915ff
```

`HEAD` é um commit de merge com dois pais:

```text
parent 4e35fe7e8459f2fffae525092c5d8ecf7e522663   (commit inicial do destino)
parent d58a408c15ec8d8d6b12732f1521dd4564d7a519   (branch da Missão 02)
```

→ `164372b` é o **merge do PR #1** (`Merge pull request #1 from gabielrtxofc-coder/arena/39232774-pocket-pad-repository`).

---

## 3. A `main` corresponde ao commit esperado?

**Sim, exatamente — não é descendente, é o próprio commit.**

| Comparação | Resultado |
|---|---|
| `main` vs commit esperado `164372b6a3…` | idênticos |
| `origin/main` vs esperado | idêntico |
| `origin/HEAD` vs esperado | idêntico (nenhum commit foi enviado à `main` depois da Missão 02.5) |
| `HEAD` (branch de sessão) vs esperado | idêntico |

**Divergência formal encontrada:** o contexto da missão descreve o commit esperado como
`164372b6a39bd6c5ec13a7ae858b3167a47915ff` — confere. O `BASELINE.md` (§2) registrava 111 commits
no momento da importação (109 upstream + commit inicial + merge de importação); o histórico hoje tem
**120** commits alcançáveis porque a Missão 02 acrescentou 9 commits (workflow + documentos +
correção Asio) e o merge do PR #1. Isso é **crescimento legítimo**, não divergência.

---

## 4. Alterações locais pendentes

```text
$ git status --porcelain=v1
(sem saída)
```

**Nenhuma alteração pendente no início da missão.** Worktree limpo, nada em stash, nada não rastreado.

---

## 5. Presença dos documentos de fundação

| Arquivo | Presente | Tamanho | Linhas |
|---|---|---|---|
| `docs/foundation/BASELINE.md` | ✅ | 20.411 B (o maior documento do repo) | 177 |
| `docs/foundation/BUILD-VALIDATION.md` | ✅ | — | 360 |
| `.github/workflows/foundation-build.yml` | ✅ | 9.864 B | 264 |

`BASELINE.md` foi lido integralmente: registra procedência (`darusc/Mousedroid`, não-fork, `master`,
commit `3d07778` = tag `v1.5`), 203 arquivos importados, SHA-256 dos 4 binários e a limitação de branch
da sessão anterior. `BUILD-VALIDATION.md` foi lido integralmente: registra a execução de CI
42524050240 com 4 jobs verdes e a única alteração de código da fundação.

---

## 6. Histórico Git — alterações após a importação original

### 6.1 Divergência encontrada: clone shallow

O ambiente foi entregue com `.git/shallow` contendo `164372b6a39bd6c5ec13a7ae858b3167a47915ff`:

```text
$ cat .git/shallow
164372b6a39bd6c5ec13a7ae858b3167a47915ff

$ git rev-list --count HEAD
1

$ git cat-file -t 4e35fe7e8459f2fffae525092c5d8ecf7e522663
fatal: could not get object info        ← pai do HEAD ausente localmente
```

Isso impediria a Fase 01 (item 6: consultar o histórico). **Corrigido de forma não destrutiva:**

```text
$ git fetch --unshallow origin
$ git rev-list --count HEAD
120
```

Nenhum commit foi criado, reescrito ou removido; apenas objetos ausentes foram baixados do `origin`.
O `.git/shallow` foi eliminado pelo próprio `git fetch`. **Nenhum `reset`, `clean` ou `checkout` foi executado.**

### 6.2 Os 9 commits após a importação

```text
$ git log --oneline 7fc8885..HEAD
164372b Merge pull request #1 from gabielrtxofc-coder/arena/39232774-pocket-pad-repository
d58a408 Document successful multiplatform foundation validation
a434774 Fix Windows build with current Asio address API
4d80210 Expose CI tool versions and Windows compiler diagnostics
23499ee Complete Windows CI setup and run Android device test
d1630d0 Use current Android setup and Windows-safe clone
0a9319d Fix CI runner provisioning and Windows checkout
d9af542 Add reproducible foundation build validation
e9fa3ad Document verified Mousedroid foundation baseline
```

| Commit | Natureza | O que alterou |
|---|---|---|
| `e9fa3ad` | Documentação | `docs/foundation/BASELINE.md` |
| `d9af542` | CI | `.github/workflows/foundation-build.yml` (novo) |
| `0a9319d`, `d1630d0`, `23499ee`, `4d80210` | CI (iterações de ajuste) | mesmo workflow |
| `a434774` | **Código (1 linha)** | `server/src/net/server.cpp:35` — `address::from_string` → `make_address` |
| `d58a408` | Documentação | `docs/foundation/BUILD-VALIDATION.md` |
| `7fc8885` | Importação | merge `--allow-unrelated-histories` do upstream `3d07778` |
| `164372b` | Merge | PR #1 → `main` |

### 6.3 Diff contra o upstream oficial (a prova de que nada mais mudou)

```text
$ git diff --stat 3d077786c4ab333f648fc3c2b2ad668203af09b2 HEAD
 .github/workflows/foundation-build.yml | 264 ++++++++++++++++++++++++
 docs/foundation/BASELINE.md            | 177 ++++++++++++++++++++++++
 docs/foundation/BUILD-VALIDATION.md    | 360 ++++++++++++++++++++++++++++++++++
 server/src/net/server.cpp              |   2 +-
 4 files changed, 802 insertions(+), 1 deletion(-)
```

**Conclusão:** contra o Mousedroid v1.5 oficial, a fundação do Pocket Pad difere em **exatamente**:

1. um workflow de CI novo (não existia no upstream);
2. dois documentos de rastreabilidade;
3. **uma linha de código** (`server.cpp:35`), que é a correção de compatibilidade Asio com a API
   oficial `asio::ip::make_address`, já justificada em `BUILD-VALIDATION.md §8`.

Nenhuma alteração em `client/`, `README.md`, `LICENSE`, `fastlane/`, `imgs/` ou em qualquer outro
arquivo do servidor.

### 6.4 Integridade dos objetos

Nenhum `git fsck` foi executado nesta missão sobre o repositório completo (é caro e não acrescentaria
informação à auditoria estrutural); o `BASELINE.md §4` registra `git fsck --full --no-dangling` sem
erros na importação. Os binários mantêm os SHA-256 registrados lá (os tamanhos conferem:
`adb.exe` 5.916.440 B, `AdbWinApi.dll` 108.312 B, `AdbWinUsbApi.dll` 73.496 B,
`gradle-wrapper.jar` 43.583 B).

---

## 7. Divergências e observações a registrar (sem correção)

| # | Observação | Severidade | Ação nesta missão |
|---|---|---|---|
| 1 | Clone shallow (1 commit) entregue pelo ambiente | Média | Resolvido com `fetch --unshallow` (não destrutivo) |
| 2 | Nome do repositório termina em ponto → checkout padrão inválido no Windows | Alta (Windows) | Documentado (`BUILD-VALIDATION.md §9` já o registrava) |
| 3 | CI dispara apenas na branch `arena/39232774-…` (Missão 02), não nas branches de trabalho atuais | **Alta** | Documentado; correção proposta como **Etapa 0** do plano |
| 4 | `main` não recebeu nenhum commit desde o merge do PR #1 | — | Nenhuma ação |
| 5 | Os 7 `tags` upstream (`v1.0`–`v1.5`) existiam localmente na Missão 01 mas **não estão presentes neste clone** | Baixa | Registrado; não é bloqueio (o commit baseline continua alcançável pelo SHA) |
| 6 | `origin` mantém `refs/heads/arena/39232774-pocket-pad-repository` (branch da missão anterior) | Baixa | **Não removida** (proibido nesta missão) |

---

## 8. Conclusão da Fase 01

A fundação está **verificada e íntegra**:

- ✅ repositório e `origin` corretos;
- ✅ branch de sessão e `main` no commit esperado `164372b6a3…`;
- ✅ `main` é exatamente o commit esperado (não apenas descendente);
- ✅ worktree limpo, nenhuma alteração local;
- ✅ `BASELINE.md` e `BUILD-VALIDATION.md` presentes e consistentes;
- ✅ histórico completo recuperado (120 commits) e os 9 commits pós-importação auditados;
- ✅ diff contra o upstream oficial limitado a 1 workflow, 2 documentos e **1 linha de código**.

**As divergências relevantes são de infraestrutura (item 3) e de nomenclatura (item 2), ambas
documentadas e nenhuma delas corrigida nesta missão, conforme as restrições.**

Prosseguiu-se para as Fases 02–09.
