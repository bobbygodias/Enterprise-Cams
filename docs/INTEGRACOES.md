# Integrações — estado verificável

Verificação das identidades: 10 de setembro de 2026. As páginas vinculadas são dos próprios publicadores na loja; a presença na loja não comprova um contrato de deep link.

| Aplicativo | Pacote Android | Fonte do publicador | Abertura por câmera |
|---|---|---|---|
| Yoosee | `com.yoosee` | [Google Play](https://play.google.com/store/apps/details?id=com.yoosee) | Não verificada |
| iCSee | `com.xm.csee` | [Google Play](https://play.google.com/store/apps/details?id=com.xm.csee) | Rota de atalho identificada no 7.9.2_G; teste real pendente |
| V380 | `com.macrovideo.v380` | [Google Play](https://play.google.com/store/apps/details?id=com.macrovideo.v380) | Não verificada |
| V380 Pro | `com.macrovideo.v380pro` | [Google Play](https://play.google.com/store/apps/details?id=com.macrovideo.v380pro) | Não verificada |
| Hilevel | `com.sotaviz.hilevelen` | [Google Play](https://play.google.com/store/apps/details?id=com.sotaviz.hilevelen) | Não verificada |

V380 e V380 Pro são registros separados. O aplicativo não escolhe silenciosamente um em lugar do outro. Pacotes clonados ou instalados em outro perfil Android não são tratados como o aplicativo do perfil atual.

## Encaminhamento desta versão

`CameraEntry.providerId → Providers → PackageManager.getLaunchIntentForPackage → startActivity`

O estado informado ao usuário é **disponibilidade do aplicativo**, jamais online/offline da câmera. Ao retornar da loja ou de outro aplicativo, a disponibilidade é reavaliada. Aplicativos ausentes ou desativados não permitem concluir um cadastro novo. Um aplicativo removido depois do cadastro não apaga a câmera do painel.

Sem aplicativo: abrir `market://details?id=PACOTE` explicitamente no Google Play; se indisponível, abrir a página HTTPS no navegador. Instalação e consentimentos ficam a cargo do Android/loja. Instalações obtidas pelo canal oficial do fabricante também são reconhecidas se tiverem o pacote esperado; o Hub não baixa APKs de sites de terceiros.

## Referências Android

- [Visibilidade por pacotes específicos](https://developer.android.com/training/package-visibility/declaring)
- [PackageManager.getLaunchIntentForPackage](https://developer.android.com/reference/android/content/pm/PackageManager#getLaunchIntentForPackage(java.lang.String))
- [Intents e filtros](https://developer.android.com/guide/components/intents-filters)

O manifest consulta somente os cinco pacotes do catálogo. Não usa QUERY_ALL_PACKAGES, acessibilidade, root, Shizuku, sobreposição ou instalação silenciosa.

## Próxima etapa para abrir a câmera certa

Para cada família: registrar versão do app e modelo da câmera, obter documentação/manifest autorizado, identificar um contrato exportado e autenticado, verificar UID e conta, testar dois dispositivos na mesma conta e confirmar que o retorno não troca a câmera. Só então habilitar um adaptador de abertura direta e seu identificador local. Não inventar extras, esquemas URI ou alegações de suporte.

Testes com o simulador QA provam o mecanismo Android; não comprovam comportamento de nenhuma versão oficial.

## Inspeção dos pacotes recebidos

Ver [ANALISE-APKS.md](ANALISE-APKS.md). No iCSee 7.9.2_G foi identificada a produção e recepção do atalho `DESK_DEVICE_ID` para `WelcomePageActivity`, com ACTION_VIEW. O contrato ainda não está habilitado no Enterprise Cams. Os caminhos examinados do Yoosee e Hilevel não comprovaram reprodução por câmera.

O Hilevel recebido contém componentes Pairip/LicenseCheck, e o iCSee contém referências a Play Integrity. A presença estática não demonstra execução nem bloqueio; um aplicativo instalado pode aceitar o Intent e depois solicitar outro canal de instalação. A confirmação de imagem continua sendo feita pelo usuário dentro do aplicativo oficial.

As três versões recebidas exigem Android 7/API 24 ou superior, embora o painel em si suporte Android 6/API 23. A compatibilidade do painel não amplia a compatibilidade dos aplicativos oficiais.

## Identificação antes da instalação

O usuário pode ler um QR pela câmera ou por uma imagem, colar o link ou escrever o nome do aplicativo. `ProviderIdentifier` reconhece os cinco pacotes/nomes do catálogo, URLs exatas da Play Store/market e os esquemas Yoosee/Hilevel inspecionados. Isso identifica a família do aplicativo, não a câmera individual. Não segue encurtadores, não abre links desconhecidos e não transforma serial isolado em fabricante. Nesses casos, exige seleção pelo manual.

O cadastro começa sem fornecedor selecionado. Após identificação/seleção, se o pacote estiver ausente, o fluxo oferece Instalar na página oficial da Play Store. A conclusão depende da presença do aplicativo e da confirmação do usuário sobre a configuração. O painel não distribui os APKs recebidos para estudo.

O leitor usa [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded), com processamento local e desugaring para API 23. Câmera é opcional; imagem e seleção manual permanecem disponíveis.
