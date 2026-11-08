# Fundação Mousedroid — baseline do Pocket Pad

## 1. Identificação e procedência

| Item | Valor verificado |
|---|---|
| Projeto de origem | Mousedroid |
| Repositório oficial | <https://github.com/darusc/Mousedroid> |
| Mantenedor | `darusc` |
| Branch de origem | `master` |
| Commit da fundação | `3d077786c4ab333f648fc3c2b2ad668203af09b2` |
| Release/tag correspondente | Mousedroid 1.5 / `v1.5` |
| Data da release | 2026-03-13 |
| Data da importação | 2026-11-08 (UTC) |
| Repositório de destino | <https://github.com/gabielrtxofc-coder/Pocket-Pad-Repository.> |
| Commit inicial do destino | `4e35fe7e8459f2fffae525092c5d8ecf7e522663` |
| Branch de trabalho publicada | `arena/39232774-pocket-pad-repository` |

A procedência foi verificada pelos metadados do GitHub: `darusc/Mousedroid` não é fork (`isFork=false`), não é mirror (`isMirror=false`), não tem repositório pai e pertence a `darusc`. O `HEAD` simbólico remoto aponta para `master`. O commit escolhido é simultaneamente o HEAD de `master` e o commit da tag/release estável mais recente `v1.5`. A API do GitHub também reporta assinatura válida e verificada para esse commit de merge (`commit.verification.verified=true`, `reason=valid`).

O antigo `gabielrtxofc-coder/Pocket-Pad-Repository` (sem ponto final) foi consultado apenas como referência e continha somente um `README.md`; nenhum código ou personalização foi copiado dele. O destino, com ponto final no nome, foi confirmado pelo `origin`, pelos metadados do GitHub e pelo diretório de trabalho.

**Restrição de branch:** a missão sugeria `foundation/mousedroid-import`, mas a sessão Arena está vinculada obrigatoriamente a `arena/39232774-pocket-pad-repository` e não permite criar, trocar ou publicar outra branch. Por isso, todo o trabalho foi realizado exclusivamente nessa branch de sessão. Nenhuma alteração foi enviada diretamente à `main`.

## 2. Estratégia de importação e histórico

1. O upstream oficial foi adicionado como remote `mousedroid-upstream` e sua branch `master` e suas tags foram obtidas sem shallow fetch.
2. Foi executado merge com `--allow-unrelated-histories --no-ff` entre o commit inicial do Pocket Pad e `3d077786c4ab333f648fc3c2b2ad668203af09b2`.
3. O merge de importação é `7fc888573aa9f32be25d5098b87cf5db833d16e7`, com dois pais:
   - `4e35fe7e8459f2fffae525092c5d8ecf7e522663` — commit inicial do destino;
   - `3d077786c4ab333f648fc3c2b2ad668203af09b2` — fundação oficial Mousedroid.
4. O único conflito foi um add/add em `README.md`. Foi mantida integralmente a versão upstream; o README inicial permanece acessível no primeiro pai do merge.
5. Não houve force push, rebase, reescrita de commits, merge na `main` ou alteração do repositório antigo.

O histórico oficial está preservado na íntegra: **109 commits** alcançáveis a partir de `master`. Após a integração, o histórico combinado tinha 111 commits (109 upstream + commit inicial + merge de importação). As sete tags upstream (`v1.0` a `v1.5`, incluindo `v1.5-beta`) foram obtidas localmente; a publicação da branch não publica tags automaticamente.

## 3. Estrutura importada

Foram importados **203 arquivos**, totalizando aproximadamente 6,95 MB de blobs:

| Área | Arquivos | Conteúdo principal |
|---|---:|---|
| Raiz | 2 | `README.md` oficial e `LICENSE` MIT |
| `client/` | 151 | Aplicativo Android em Kotlin, recursos, testes, Gradle Wrapper e configurações upstream |
| `server/` | 42 | Servidor C++/CMake para Linux e Windows, GUI wxWidgets, entrada, rede, scripts e ADB para Windows |
| `fastlane/` | 3 | Metadados e ícone para distribuição Android |
| `imgs/` | 5 | Logos e capturas de tela |

Estrutura funcional resumida:

```text
.
├── LICENSE
├── README.md
├── client/
│   ├── app/
│   │   ├── build.gradle
│   │   └── src/{main,test,androidTest}/
│   ├── gradle/wrapper/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   └── gradlew.bat
├── fastlane/metadata/android/en-US/
├── imgs/
└── server/
    ├── adb/
    ├── res/
    ├── src/{gui,input,net}/
    ├── CMakeLists.txt
    ├── install.sh
    ├── release.sh
    └── release.bat
```

Arquivos de IDE e metadados já versionados pelo upstream (`.idea`, `.gradle`, `.vscode`, `local.properties`) foram mantidos para assegurar fidelidade. Isso não representa importação de temporários do Arena; nenhum arquivo de trabalho do ambiente Arena foi acrescentado.

## 4. Verificação de integridade

Antes da criação deste documento, foram executados:

```bash
git diff --exit-code mousedroid-upstream/master HEAD
git ls-tree -r --name-only mousedroid-upstream/master
git fsck --full --no-dangling
```

Resultados:

- árvore upstream: `03e3cd268a38f6bdbd555829f4bb0305ccbd2466`;
- árvore do merge de importação: `03e3cd268a38f6bdbd555829f4bb0305ccbd2466`;
- diferenças de nomes, conteúdo ou modos entre as árvores: **0**;
- arquivos upstream/importados: **203/203**;
- `git fsck`: nenhum erro;
- worktree após o merge: limpo;
- não há submódulos, symlinks ou ponteiros Git LFS no baseline;
- este `docs/foundation/BASELINE.md` é o único arquivo novo planejado além da árvore oficial.

Integridade de componentes binários:

- `client/gradle/wrapper/gradle-wrapper.jar`: teste de todos os itens ZIP concluído sem erros;
- `server/adb/adb.exe`, `AdbWinApi.dll` e `AdbWinUsbApi.dll`: tamanhos idênticos aos blobs upstream e assinaturas MZ/PE válidas.

SHA-256 registrados:

```text
2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046  client/gradle/wrapper/gradle-wrapper.jar
9ffacedc41b2752075571e1a474ff50c5dcbe1f64db56db24aaec78aea1126df  server/adb/AdbWinApi.dll
13918fdab0c3ac77d077453a6036247cfeca10910aec845f188c41148c630bb2  server/adb/AdbWinUsbApi.dll
705ddc21f33ac52105d1b075b019962ad0e44fb3d560bde69ce8cb3a36bca183  server/adb/adb.exe
```

## 5. Dependências funcionais identificadas

### Cliente Android

- JDK/Java 17; Android SDK com `compileSdk 34` e `targetSdk 34`; `minSdk 28` (Android 9+).
- Gradle Wrapper **8.11.1**, Android Gradle Plugin **8.9.1**, Kotlin Android **2.1.0** e `kotlin-kapt`.
- AndroidX: `core-ktx 1.8.0`, `appcompat 1.4.1`, `constraintlayout 2.1.3`, `navigation-fragment-ktx 2.7.7`.
- Material Components **1.5.0**, Gson **2.10.1**, Kotlinx Coroutines Android **1.3.9**.
- Testes: JUnit **4.13.2**, AndroidX Test Ext JUnit **1.1.5**, Espresso **3.5.1**; testes instrumentados exigem dispositivo/emulador Android.
- Repositórios de dependências declarados: Google Maven, Maven Central e Gradle Plugin Portal.

### Servidor desktop

- CMake **3.26+**, compilador com C++17; no Linux, GCC 11+ ou Clang e Make conforme o README upstream.
- wxWidgets (README: **3.3**) com componentes `net`, `gl`, `core`, `base` e `adv`.
- Asio standalone; Windows usa pacotes `asio` e `wxwidgets` via vcpkg e Visual Studio 2022/2026 com desenvolvimento C++.
- Bibliotecas Windows de plataforma: `ws2_32`, `wsock32` e `user32`.
- Linux: ADB do sistema para modo USB e permissões `uinput` configuradas por `install.sh`.
- Windows: `server/adb/adb.exe`, `AdbWinApi.dll` e `AdbWinUsbApi.dll` estão incluídos e foram preservados.
- Recursos gráficos/ícones e scripts de empacotamento `release.sh`/`release.bat` também fazem parte do baseline.

Não existem workflows de CI no commit importado.

## 6. Licença e atribuições

O arquivo `LICENSE` oficial foi preservado sem alterações. Ele contém a licença **MIT** e o aviso:

```text
Copyright (c) 2025 darusc
```

O `README.md`, namespaces `com.darusc.mousedroid`, imagens, textos Fastlane e demais atribuições upstream também permanecem originais. Nenhuma relicenciação ou mudança de autoria foi realizada. Não foram localizados outros arquivos de licença ou notice no baseline; isso é um registro da busca, não uma auditoria jurídica completa das dependências de terceiros.

## 7. Testes e compilação executados

Ambiente disponível: Debian, GCC/G++ 12.2.0 e GNU Make 4.3; **sem Java/JDK, Android SDK e CMake**. Os comandos foram executados sem modificar o código:

| Verificação | Comando | Resultado |
|---|---|---|
| Testes unitários Android | `cd client && bash ./gradlew test --no-daemon` | **Não executado — bloqueado pelo ambiente**, saída 1: `JAVA_HOME is not set and no 'java' command could be found in your PATH` |
| Configuração do servidor | `cmake -S server -B /tmp/mousedroid-server-build -DCMAKE_BUILD_TYPE=Release` | **Não executado — bloqueado pelo ambiente**, saída 127: `cmake: command not found` |
| Integridade do Gradle Wrapper | `unzip -t client/gradle/wrapper/gradle-wrapper.jar` | **Passou**, nenhum erro nos dados comprimidos |
| Integridade dos binários ADB | SHA-256 + validação de cabeçalhos MZ/PE | **Passou**, arquivos íntegros e formato PE reconhecido |
| Comparação com upstream | `git diff --exit-code mousedroid-upstream/master HEAD` | **Passou**, zero diferenças antes deste documento |
| Objetos Git | `git fsck --full --no-dangling` | **Passou**, nenhum erro |

O upstream contém apenas dois testes-exemplo: `ExampleUnitTest.kt` (soma `2 + 2`) e `ExampleInstrumentedTest.kt` (nome do pacote). O teste instrumentado não foi executado, pois exigiria Java, Android SDK e dispositivo/emulador. Não há suíte de testes do servidor.

**Conclusão de build:** este baseline está importado e estruturalmente íntegro, mas **não foi possível comprovar compilação ou execução funcional neste ambiente**. Não se declara o projeto compilável com base apenas na importação. As indisponibilidades acima são limitações do ambiente, não falhas observadas no código ou regressões atribuíveis à importação.

## 8. Problemas e pendências para as próximas missões

1. Provisionar JDK 17, Android SDK 34 e acesso aos repositórios Gradle/Maven; executar `test`, `assembleDebug` e os testes instrumentados em dispositivo/emulador.
2. Provisionar CMake 3.26+, wxWidgets e Asio; configurar, compilar e testar o servidor Linux. Validar separadamente o build Windows/vcpkg e os binários ADB em Windows.
3. O upstream versiona `client/local.properties` com um caminho absoluto de SDK Windows do autor. Ele foi preservado nesta missão; uma missão futura deve definir configuração local/portável sem apagar esse histórico retroativamente.
4. `client/gradlew`, `server/install.sh` e `server/release.sh` estão versionados pelo upstream sem bit executável (`100644`). Foram preservados assim; a necessidade de ajuste deve ser avaliada separadamente.
5. Metadados `.gradle` e `.idea` são rastreados pelo upstream e foram mantidos. Sua limpeza deve ser uma decisão explícita de missão posterior, nunca parte da importação fiel.
6. Não há CI upstream. Após comprovar os builds localmente, propor CI reproduzível para Android e servidores Linux/Windows.
7. Realizar testes funcionais reais de Wi-Fi, Bluetooth e USB/ADB em hardware compatível; nenhum foi realizado nesta missão.
8. Planejar a auditoria de licenças das dependências externas e dos binários ADB, mantendo os avisos aplicáveis.
9. Somente após essas validações, iniciar limpeza e adaptação de identidade para Pocket Pad em missões independentes.

## 9. Escopo

Não foram desenvolvidas funcionalidades Pocket Pad, alterações de interface ou protocolo, gestos, refatorações, remoções de arquivos ou reuso de código experimental. A única adição posterior à árvore oficial é este documento de rastreabilidade.
