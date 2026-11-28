# Enterprise Cams — análise dos aplicativos recebidos

Atualização: 11 de setembro de 2026. Escopo: identidade dos pacotes, manifest, entradas de navegação e componentes associados à distribuição. Análise estática dos arquivos fornecidos por Bobby. Não é auditoria completa de segurança, ensaio de tráfego ou teste com câmeras físicas. Nenhum APK de terceiro foi alterado nesta etapa. Bobby esclareceu expressamente: os anexos servem ao estudo de funcionamento para nosso projeto; os usuários obtêm normalmente o aplicativo oficial pela Play Store, com raras exceções pelo canal do fabricante.

## Resultado prático

A versão 0.1.0 do Enterprise Cams abre o aplicativo escolhido. A análise identificou uma rota promissora de atalho por dispositivo no iCSee, ainda não habilitada no painel. Os caminhos inspecionados no Yoosee e Hilevel não demonstraram abertura direta de uma câmera.

## iCSee 7.9.2_G

Pacote `com.xm.csee`, versão 7.9.2_G, código 79231. Manifest do APK base: minSdk 24, targetSdk 37. Pacote recebido em contêiner APKM, com splits; a extração do base não constitui uma instalação completa.

Há uma rota utilizada pelo próprio recurso de atalho da área de trabalho. Evidência nas classes `DevSettingActivity.Y1`, `IDRMainSetActivity.u2` e `WbsDevSettingActivity.f2`:

- Activity explícita `com.xworld.activity.welcome.view.WelcomePageActivity`, exportada no manifest.
- Ação `android.intent.action.VIEW`.
- Extra de texto `xmApp` com valor `addShortCut`.
- Extra de texto `DESK_DEVICE_ID` com o identificador do dispositivo.

Em `WelcomePageActivity.I0`, o aplicativo lê `DESK_DEVICE_ID`; quando já está pronto, publica `EventDeskDeviceId`, e no outro ramo conserva o identificador no `DataCenter`. Em `DeviceFragment.deviceIntoMonitor`, o identificador é comparado ao `SDBDeviceInfo.getSN()` dos dispositivos existentes e a seleção correspondente é encaminhada ao fluxo de monitoramento.

Isso é evidência estática de um contrato interno de atalho por câmera. Não significa API pública estável, acesso a dispositivos fora da conta ou confirmação de funcionamento em qualquer versão. A rota de URI com parâmetro `sn` que leva a `SnAddDevActivity` pertence ao cadastro; não deve ser confundida com o atalho de monitoramento.

Próximo ensaio: aplicativo oficial completo, conta autenticada, duas câmeras conhecidas e seus identificadores; verificar partida fria, aplicativo já aberto, troca entre câmeras e retorno ao painel. Só habilitar o adaptador após confirmação da câmera correta. Nunca gravar senha ou token no atalho.

Também foram encontradas referências a `com/google/android/play/core/integrity` em classes2.dex. A presença da biblioteca não prova que uma verificação esteja sendo chamada nem que o aparelho será bloqueado.

## Hilevel 1.20260805.295

Pacote `com.sotaviz.hilevelen`, código 295, minSdk 24, targetSdk 35. Recebido em XAPK; preservar os splits e o canal de instalação apropriado.

O manifest expõe `com.eooliinext.SplashActivity` e entradas como `hilevelen` e `oep.70cb3b4933b31://app/open`. O trecho examinado de `SplashActivity` encaminha a URI ao auxiliar `com.eooliinext.utils.l0.f`, que lê o parâmetro `config` e o interpreta como configuração JSON. Essa entrada não demonstrou abertura de vídeo por câmera.

`HWPushActivity` processa dados de notificação. Não extrapolar isso para um contrato externo de câmera.

classes2.dex contém componentes `com/pairip/`, incluindo referências a `LicenseActivity` e `LicenseCheck`. É um indício concreto pertinente ao alerta de instalação trazido no vídeo, mas não comprova em qual condição a proteção dispara. O Enterprise Cams não modifica esses componentes.

## Yoosee 6.32.3

Pacote `com.yoosee`, código 6323, minSdk 24, targetSdk 34. O launcher e o tratamento de deep links foram confrontados com os manifests e trechos smali recebidos.

`LogoActivity.onParseParams` encaminha ACTION_VIEW para `IDeepLinkApi.onDeepLinkIntent`. No `DeepLinkImpl` inspecionado, `yoosee://share` lê `page` e `webPath`; a página reconhecida é `web`, conduzindo a uma WebView após a condição de login. Isso não é uma rota demonstrada de reprodução nativa por UID.

Activities internas de reprodução inspecionadas não estavam exportadas. Não criar Intents explícitos para forçar sua abertura nem inventar extras.

A busca dirigida nos DEX do APK original não encontrou os marcadores Pairip/LicenseCheck pesquisados. Ausência desses marcadores nessa busca não equivale à ausência de proteções, telemetria ou outros componentes.

O ZIP rotulado como patched-source teve o SHA-256 confrontado com o arquivo SHA recebido, com correspondência. Isso confirma integridade do arquivo enviado, não qualidade dos patches, segurança, compilação ou funcionamento. Não houve integração desse código ao Enterprise Cams nem auditoria completa das diferenças.

## XMEye 1.6.2.46 — anexo recebido em 11/09

Identidade confirmada no manifest e nos recursos do APK: **XMEye**, pacote `com.mobile.myeye`, versão `1.6.2.46`, código `16246`, minSdk 21, targetSdk 30. Contém dois DEX e bibliotecas `arm64-v8a` e `armeabi`. Essa versão do arquivo não deve ser tratada como a versão atual da loja. A [página oficial do XMEye na Play Store](https://play.google.com/store/apps/details?id=com.mobile.myeye) foi consultada em 11/09 e confirma o pacote e o nome.

A entrada principal é `com.mobile.myeye.activity.welcome.view.WelcomeActivity`. No manifest examinado, há filtros para `xm.intent.action.Push` com `customscheme://com.mobile.myeye/notify_detail` e ACTION_VIEW com `xgscheme://com.xg.push/notify_detail`.

O método `WelcomeActivity.dealWithIntentData` trata a ação de notificação e, no ramo ACTION_VIEW, exige o parâmetro `action=xm.intent.action.Push`. Lê `alarmSn`, `alarmId`, `alarmEvent` e `alarmTime`, monta `PushMsgBean` e encaminha ao processamento de notificações, ou conserva os dados no DataCenter. Isso demonstra um caminho de alarmes; não comprova abertura direta do vídeo ao vivo da câmera. Os filtros do Facebook no manifest atendem à integração de autenticação e não estabelecem um contrato de câmera.

A busca dirigida nos DEX não encontrou as chaves `DESK_DEVICE_ID` e `addShortCut` do contrato identificado no iCSee. Não reutilizar o adaptador do iCSee por semelhança de fabricante. Ainda não houve teste dinâmico, verificação com conta/câmera nem inclusão do XMEye no catálogo do painel. Nenhum dado de autenticação foi extraído ou reutilizado.

SHA-256 do APK completo recebido: `b416b519bc134e50225df5f26b63dd46e1e1330e95584f851e1b5e2f28c8767f`.

## V380 e V380 Pro

As identidades são `com.macrovideo.v380` e `com.macrovideo.v380pro`. Não foram recebidos APKs dessas duas variantes nesta rodada. Abertura por câmera continua sem contrato verificado.

## Alerta do vídeo

O arquivo “Possível erro. ESTRITAMENTE IMPORTANTE” contém o link [Faça o download deste app no Google Play](https://www.youtube.com/watch?v=Rmbyhvbt6eU). Foi examinada sua transcrição automática; a reprodução visual integral não foi confirmada. O vídeo discute exigência de origem de instalação e modificações em componentes de proteção.

A documentação do publicador explica que a [proteção automática do Google Play](https://support.google.com/googleplay/android-developer/answer/10183279?hl=en) pode verificar a origem de instalação e orientar a obtenção pela loja. Há diferenças entre disponibilidade no PackageManager, lançamento de Activity e efetiva entrada no aplicativo. O painel não consegue inferir sucesso de login ou de vídeo apenas porque startActivity foi aceito.

Consequência para o projeto: preservar distribuição independente do nosso APK, reconhecer os limites dos apps oficiais e indicar o canal deles quando necessário. Não atribuir ao Enterprise Cams a capacidade de remover restrições dos fabricantes.

## Impressões SHA-256

Os hashes Hilevel e iCSee abaixo identificam somente o APK base extraído, não os contêineres completos.

- Yoosee APK original: `3b8021b351df0dea14827d71e66c1fb5a0f631f7cdfb70a597505e3600ef5d98`.
- ZIP Yoosee patched-source: `3860b7cc1f938cbc4f2956923cba55ae65e4ca92591dd652ed1b5df2fbab7195`.
- Hilevel base: `1bf19be8ddb6c5452b3b20ba1f3d4bfea22b4ae04796c5ebb4ce63cb8975e4fe`.
- iCSee base: `708d4f5dadbf179370a3fbf69c15554e156a86af84e3af0d19b7852c65cdbe71`.

Os arquivos de fabricantes e suas desmontagens permanecem separados do repositório público do Enterprise Cams. O repositório recebe este resumo técnico e nosso código próprio.


## Recebimento e análise dirigida — 28/11/2026 (Yoosee 6.48 e Hilevel 1.20261113.459)

Os dois arquivos foram fornecidos por Bobby para a **pesquisa de interoperabilidade** do Enterprise Cams. São contêineres `.apks` (ZIPs contendo base e splits), com `APKCube.txt` declarando `Source: https://apkcube.com`. **Não foi verificada a cadeia de publicação ou a assinatura oficial do aplicativo** e os pacotes não foram instalados ou executados. Os resultados abaixo derivam da inspeção estática do manifest binário, índice de arquivos e tabela de strings/classes dos DEX. Não incorporar nem distribuir estes APKs, componentes nativos, chaves ou segredos.

| Propriedade | Yoosee | Hilevel |
| --- | --- | --- |
| Nome do arquivo | `Yoosee_6.48.apks` | `Hilevel_1.20261113.459.apks` |
| Identificador no manifest | `com.yoosee` | `com.sotaviz.hilevelen` |
| Versão / code | `6.48` / `64731` | `1.20261113.459` / `459` |
| API mínima / alvo | `24` / `36` | `29` / `37` |
| Pacotes internos | base + 5 splits (arm64, en, pt, tvdpi, xxhdpi) | base + 3 splits (arm64, en, xxhdpi) |
| Arquivos DEX no base | 11 | 10 |
| Bibliotecas nativas no split arm64 | 58 | 79 |
| SHA-256 contêiner completo | `a745140e877481981cc8b48ca277fb4509516decc3ed57cd1c72505fc404f56c` | `f0df294ccc241d1f4b9b8a71c14a25c345bd22241af753decab3c389ca701151` |
| SHA-256 base.apk | `8ee10fbadf0db55dac4f5763aa841b9ec8806cc841d1ea16494e7d085941a294` | `ab250094707be57b37b74ca8b376c4b38cfd867d4307be63dc6f52d7edbf0c84` |

### Yoosee 6.48 — efeito sobre a autenticação e o vídeo

- As strings exatas `/openapi/app/user/login/account` e `/openapi/app/user/device/listDevice` continuam presentes. Também há `HttpServiceAdapter`, `HttpInterface` e `AddBaseParamsInterceptor`: indícios de operação cliente/serviço, **não** prova de que se possa chamar essas rotas sem registro de cliente, assinatura e sessão autorizada.
- O pacote agora também expõe classes `com/gwell/kmp/accountMgr2`, `com/yoosee/yooseecompose/features/account`, `com/jwkj/iotvideo/player/LivePlayer` e `com/gw/player`. As enumerações de método de login incluem **Phone** e **YooseeId**, além de Email; o Enterprise Cams não deve forçar o caractere `@` na identificação local da conta.
- Bibliotecas presentes no split arm64: `libgwplayer.so`, `libgwmediaplayer.so`, `libp2pav.so` e `libgwbase.so`. A presença de um player interno não transforma esse componente num SDK público reutilizável.
- Há mensagens de usuário confirmando suporte de **certos dispositivos** a RTSP/ONVIF **na mesma LAN** quando o recurso é habilitado e a senha própria da câmera é configurada. Isso respalda manter a implementação ONVIF/RTSP aberta, não presumir que todos os modelos suportem o recurso nem confundir senha da câmera com senha da conta de nuvem.

### Hilevel 1.20261113.459 — efeito sobre o inventário e o vídeo

- O manifest conserva `com.sotaviz.hilevelen`, mas a versão nova eleva a API mínima de 24 (arquivo antigo) para **29**, ao passo que o Enterprise Cams continua independente desse requisito de instalação.
- Estão presentes `/core/v1/auth/login`, a classe `com/v2/clhttpclient/api/protocol/account/ISession` e modelos de resultado da conta. Para listagem aparecem `/iot/v1/device/list.json`, `/lecam/service/device/deviceListApp` e `/lecam/sigV2/device/deviceListApp/V2`. A busca dirigida **não recuperou a string exata** `getDeviceListV2` do APK examinado anteriormente. Isso **não prova remoção do recurso**: várias famílias de API coexistem, partes podem ser ofuscadas e os contratos utilizáveis dependem do fluxo e sessão corretos. Não fixar um endpoint a partir da mera existência da string.
- Bibliotecas nativas `libP2PWrapper.so`, `libclP2P.so`, `libp2p.so` e `libmv3_rtplayer.so` permanecem; nenhuma dessas bibliotecas foi executada ou anexada ao nosso projeto.
- Existem referências `/api/system/onvif/discovery`, `/api/system/onvif/rtsp_url`, `/api/system/onvif/snapshot_url` em componentes ligados a SmartBox. **Não generalizar** isso ao modelo físico da câmera Hilevel de Bobby nem apresentar como solução universal de cloud.

### Decisões aplicadas e próximos testes

- Os IDs de pacote permanecem compatíveis com o catálogo atual: **não há necessidade de alterar a identificação por QR/Play Store**.
- O contrato abstrato `YooseeAccountApi` passa a usar `login(accountIdentifier, ...)` em vez de `emailLogin(...)`; `YooseeAccountApi` e `HilevelAccountApi` expõem `listDevices(...)` em vez de nomes internos específicos de uma versão. A validação de qual método de login existe cabe ao driver autorizado de cada fabricante. Testes com drivers sintéticos verificam roteamento e independência de conta.
- A sequência funcional continua: **conta previamente registrada no fabricante → autenticação autorizada → inventário da conta → seleção da câmera → vídeo interno**. Para cada fabricante ainda é necessário o driver real de sessão/inventário/stream correspondente, autenticação válida e ensaio com câmera física; CI e strings dos APKs não comprovam essas etapas.
- Não utilizar chaves embutidas no aplicativo de terceiro como identidade do Enterprise Cams, não armazenar credenciais de nuvem no painel, não instalar os pacotes de estudo em aparelhos do usuário como teste implícito.
