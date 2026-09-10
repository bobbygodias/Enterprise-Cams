# Enterprise Cams — análise dos aplicativos recebidos

Data: 10 de setembro de 2026. Escopo: identidade dos pacotes, manifest, entradas de navegação e componentes associados à distribuição. Análise estática dos arquivos fornecidos por Bobby. Não é auditoria completa de segurança, ensaio de tráfego ou teste com câmeras físicas. Nenhum APK de terceiro foi alterado nesta etapa. Bobby esclareceu expressamente: os anexos servem ao estudo de funcionamento para nosso projeto; os usuários obtêm normalmente o aplicativo oficial pela Play Store, com raras exceções pelo canal do fabricante.

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
