# Enterprise Cams

Agregador Android com telas internas e contas, nuvens e assinaturas mantidas pelos fabricantes. Renovação da nuvem continua no aplicativo oficial.

**Estado desta branch: 0.2.0-preview, visualizador interno em implementação/teste.** Tocar em uma câmera agora abre sua tela no Enterprise. O player reproduz um clipe local claramente identificado e links HTTPS HLS/MP4 autorizados. **Login e vídeo das nuvens Yoosee, iCSee, Hilevel, V380 e V380 Pro ainda não estão integrados.** Não confundir o teste do player com uma câmera conectada. Confira o resultado do workflow para saber se o APK deste commit foi compilado e testado.

## O que mudou

- Visualizador interno nativo com pausa, som, estados de carga/erro e liberação em segundo plano.
- Cadastro independente da instalação do aplicativo do fabricante; busca, favoritos, QR, edição e backups locais preservados.
- Clipe de teste incluído no APK, sem internet ou conta. Modo de link HTTPS temporário na compilação debug.
- Novo logotipo circular aprovado aplicado ao painel e ao ícone Android.
- Nenhum redirecionamento automático para outro aplicativo ao tocar na câmera. Configuração inicial/instalação oficiais permanecem ações explícitas na etapa de cadastro.
- Sem anúncios ou telemetria; sem segredos de fabricantes ou bibliotecas proprietárias copiadas dos APKs.

## Testar e construir

[Instalação](docs/INSTALL.md) · [Roteiro de teste e limites](docs/PLAYER-TEST.md) · [PR #2](https://github.com/bobbygodias/Enterprise-Cams/pull/2)

JDK 17, Android SDK 35, Gradle 8.11.1. Android mínimo: API 23. Kotlin e Jetpack Compose, DataStore e Media3 1.8.0.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:assembleRelease :app:assembleDebugAndroidTest
```

Debug instalável: `app/build/outputs/apk/debug/app-debug.apk`. Release otimizado ainda sem assinatura de distribuição. O workflow publica o APK de teste depois de compilar e executar testes unitários; lint, release e testes em emulador continuam em seguida. Baixar um artefato antes do fim do workflow não comprova aprovação de todas as etapas.

O módulo `qa-stub` é exclusivo do emulador e nunca compõe o APK Enterprise. Não instalar esse simulador no telefone.

## Continuidade

- [Contrato atual e pesquisa de integração](docs/AGREGADOR-CLOUD-2026-09-27.md)
- [Histórico de trabalho](docs/CONTINUIDADE.md)
- [Privacidade](docs/PRIVACIDADE.md)

Documentos antigos de escopo/integração descrevem a etapa 0.1.0 do launcher; o contrato de 27/09/2026 e o roteiro desta prévia prevalecem.

Código próprio sob [CC0 1.0](LICENSE). Dependências conservam suas respectivas licenças. Bobby Dias & Andrew Vox.
