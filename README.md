# Enterprise Cams

Um painel Android para encontrar suas câmeras pelo nome e pelo local, com encaminhamento ao aplicativo oficial de cada câmera.

**Estado: prévia Android 0.1.0 com QR compilada; 22 testes unitários aprovados. Na última rodada concluída, quatro de sete testes de tela passaram. As correções seguintes aguardam resultado final.** O repositório começou com a licença CC0. O registro atualizado de testes e limites está em [CONTINUIDADE.md](docs/CONTINUIDADE.md).

## O que esta versão faz

- Organiza acessos por câmera/local, com busca, favoritos e edição.
- Lê QR pela câmera ou por imagem e identifica aplicativos do catálogo por links/nomes conhecidos, com alternativa manual quando só há um serial.
- Reconhece Yoosee, iCSee, V380, V380 Pro e Hilevel instalados no mesmo perfil Android.
- Encaminha à Play Store; se a loja não abrir, tenta sua página HTTPS no navegador.
- Orienta a configuração da câmera no aplicativo oficial e conserva o cadastro ao sair e voltar.
- Adiciona o acesso ao painel após confirmação do usuário e verificação de que o aplicativo está disponível.
- Abre o aplicativo correspondente ao tocar no acesso.
- Exporta/importa backup JSON local, sem apagar os cadastros existentes na importação.
- Funciona sem conta Enterprise Cams, backend, anúncios, telemetria ou Google Play Services.

## Limite atual, sem ambiguidade

**O toque abre a tela inicial do aplicativo oficial. A seleção da câmera ainda é feita nele.** Não há deep link por dispositivo verificado, importação automática do inventário do fabricante, incorporação de telas externas, player interno, ONVIF ou RTSP nesta entrega. As funções e o vídeo continuam no aplicativo original.

O objetivo de abrir diretamente uma câmera específica permanece no projeto. Cada adaptador precisará de uma interface documentada ou testada, com identificação da câmera e retorno consistente. Instalar o aplicativo original, por si só, não concede acesso ao inventário nem a seus controles.

O painel funciona offline. A câmera, os anúncios, o login, a latência e a dependência de internet do aplicativo oficial seguem as condições dele. Esta versão não remove publicidade nem restrições de terceiros.

## Construção

JDK 17, Android SDK 35 e Gradle 8.11.1. Android mínimo: 6.0/API 23; alvo desta base: API 35. Kotlin nativo com Jetpack Compose; dados privados em DataStore.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:assembleRelease
```

O debug instalável fica em `app/build/outputs/apk/debug/app-debug.apk`. O release é otimizado e **não assinado**; não é um APK de distribuição final. Nenhuma chave privada é versionada. Consulte [INSTALL.md](docs/INSTALL.md).

O workflow Android executa compilação, testes de dados, lint e testes instrumentados com captura de telas. **O módulo `qa-stub` é um simulador isolado, exclusivamente para CI**, com identificação de pacote Yoosee para verificar visibilidade e encaminhamento. Não contém o software oficial, não é uma integração real, não compõe o APK Enterprise Cams e jamais deve ser instalado no aparelho de um usuário ou distribuído.

## Projeto

- [Briefing e continuidade](docs/CONTINUIDADE.md)
- [Adaptadores e fontes](docs/INTEGRACOES.md)
- [Análise dos APKs, incluindo XMEye](docs/ANALISE-APKS.md)
- [Código em revisão — PR #1](https://github.com/bobbygodias/Enterprise-Cams/pull/1)
- [Validação das correções](https://github.com/bobbygodias/Enterprise-Cams/actions/runs/34575358109)
- [Escopo e evolução](docs/ESCOPO.md)
- [Privacidade](docs/PRIVACIDADE.md)

A referência visual fornecida para o projeto foi preservada no ícone e na tela inicial: robô prateado, olhos azuis, câmera e fundo preto. O painel segue a organização por câmera/local explicitada no briefing. A tipografia, o espaçamento e os componentes desta primeira implementação são propostas concretas para revisão.

Licença [CC0 1.0](LICENSE), preservada da criação do repositório. Bobby Dias & Andrew Vox.
