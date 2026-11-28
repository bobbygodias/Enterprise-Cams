# Enterprise Cams

Projeto Android de **central unificada de câmeras de diferentes fabricantes**, com identificação por QR Code, conectores próprios e vídeo e controles dentro de um único aplicativo. **Yoosee e Hilevel são prioridades indispensáveis; outras marcas não são excluídas.** A primeira versão de painel/launcher foi uma etapa inicial, não o objetivo definitivo.

## Finalidade acordada e arquitetura (25/11/2026)

O Enterprise Cams deve permitir que o usuário identifique e adicione câmeras de ecossistemas variados sem precisar conhecer antecipadamente qual aplicativo oficial usar. O QR Code é uma entrada de descoberta: o aplicativo interpreta os dados realmente presentes (como identificador, endereço, marca, modelo ou link) e complementa a identificação por mecanismos de descoberta e informações disponíveis. O catálogo inicial de cinco pacotes **não limita o número de fabricantes** que poderemos integrar.

Cada fabricante continua responsável pela **autenticação de suas contas e dispositivos**. Nosso trabalho é implementar a comunicação com esses fluxos existentes e construir a camada de interoperabilidade: identificação, escolha do conector, sessão autorizada, seleção da câmera, vídeo e controles na mesma interface. Não inventamos um novo sistema de autenticação em lugar do fabricante. Independência dos aplicativos oficiais não significa independência obrigatória dos servidores de nuvem de cada marca.

Complicações de protocolos, compatibilidade e segurança são problemas de engenharia a enfrentar. A segurança deve ser implementada por medidas proporcionais aos riscos concretos, não por promessas de 100% de segurança. Os marcos técnicos devem produzir **código funcional e testes verificáveis**, além da documentação necessária. Consulte [Escopo](docs/ESCOPO.md) e [Integrações](docs/INTEGRACOES.md).

**Estado observado na main em 25/11/2026: prévia Android 0.1.0, com leitura QR limitada ao catálogo e encaminhamento aos aplicativos oficiais.** A branch de QA do [PR #3](https://github.com/bobbygodias/Enterprise-Cams/pull/3) teve workflow aprovado em 22/11/2026, mas não demonstra vídeo de câmera real. O [PR #2](https://github.com/bobbygodias/Enterprise-Cams/pull/2) contém player interno experimental, e o [PR #4](https://github.com/bobbygodias/Enterprise-Cams/pull/4), pesquisa de contratos. Essas branches não comprovam o objetivo final. O repositório começou com a licença CC0; veja [CONTINUIDADE.md](docs/CONTINUIDADE.md).

## O que esta versão faz

- Organiza acessos por câmera/local, com busca, favoritos e edição.
- Lê QR pela câmera ou por imagem; reconhece os aplicativos catalogados e, em links exatos da Play Store, registra **candidatos de pacotes externos** com validação de formato. O fabricante e a compatibilidade desses candidatos permanecem **não verificados**; um serial isolado não autoriza identificação por inferência.
- Nesta prévia, o catálogo verificado inclui Yoosee, iCSee, V380, V380 Pro e Hilevel. A seleção de outros pacotes por QR é apenas a primeira etapa de descoberta, **não comprova conector nem streaming**, e depende de confirmação explícita da pessoa.
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
- [Player interno experimental — PR #2](https://github.com/bobbygodias/Enterprise-Cams/pull/2)
- [QA de interface — PR #3](https://github.com/bobbygodias/Enterprise-Cams/pull/3)
- [Pesquisa de contratos — PR #4](https://github.com/bobbygodias/Enterprise-Cams/pull/4)
- [Escopo e evolução](docs/ESCOPO.md)
- [Privacidade](docs/PRIVACIDADE.md)

A referência visual fornecida para o projeto foi preservada no ícone e na tela inicial: robô prateado, olhos azuis, câmera e fundo preto. O painel segue a organização por câmera/local explicitada no briefing. A tipografia, o espaçamento e os componentes desta primeira implementação são propostas concretas para revisão.

Licença [CC0 1.0](LICENSE), preservada da criação do repositório. Bobby Dias & Andrew Vox.

### Câmeras locais via RTSP (experimental)

O visualizador interno aceita agora um endereço RTSP fornecido pelo proprietário da câmera, limitado a IP privado IPv4 (RFC 1918). A sessão usa Media3 RTSP sobre TCP, não grava o endereço nem as credenciais e exige ação explícita. RTSP não é criptografado: use exclusivamente rede local confiável. A restrição valida o endereço inicial e não constitui isolamento de rede do servidor. Não há varredura, descoberta de senhas ou presunção de suporte RTSP em um determinado modelo. Não substitui a integração P2P/nuvem.

#### Inspeção estática dos APKs fornecidos em 28/11/2026

- Yoosee 6.32.3: SHA-256 3b8021b351df0dea14827d71e66c1fb5a0f631f7cdfb70a597505e3600ef5d98; 20 DEX; arm64 inclui libgwbase.so, libgwmediaplayer.so, libgwplayer.so e libp2pav.so. O cabeçalho ProtocolParser.h no APK enumera HLS/M3U8, TS, MP4, DASH/MPD e H264 como formatos reconhecidos pelo player, não como contratos de autenticação.
- Hilevel 1.20260805.295: SHA-256 6db517c4398cf906ada31653931316d6ee721f5252f6565255280aad862a5bb4; 9 DEX; arm64 inclui libP2PWrapper.so, libclP2P.so, libp2p.so e libmv3_rtplayer.so.
- ZIPs recebidos posteriormente e comparados: original Apktool (SHA-256 cab0a7eafa6cb3f1d21366994d5733a7252aad683ccd60b8507455adab329fc4) e patch Enterprise (SHA-256 3860b7cc1f938cbc4f2956923cba55ae65e4ca92591dd652ed1b5df2fbab7195, coincide com manifesto fornecido). Comparação das entradas por tamanho/CRC: uma classe de inicialização de publicidade/recompensas alterada, um JAR removido, notas adicionadas. As fontes são árvore Apktool/Smali, não um SDK Android independente. Interfaces internas de conta e player identificadas, mas não validadas como contrato externo de autenticação/inventário. Não redistribuir os arquivos ou extrair credenciais. Nenhum APK reconstruído ou teste físico realizado.

Inventário de evidências, sem publicar APKs, bibliotecas ou código proprietário. A presença das bibliotecas não comprova autenticação, inventário remoto, compatibilidade de ABI nem reprodução de câmera física. Próxima etapa: usar interfaces autorizadas específicas de cada fabricante, validar com câmera de propriedade do usuário e manter o estado experimental até então.

#### ONVIF Media local (28/11)
As strings AA1707, AA847, AA928 e open_rtsp_prompt do Yoosee confirmam a opção RTSP com senha e integração ONVIF na mesma LAN em dispositivos compatíveis. No Hilevel, os DEX contêm OnvifBindResult, deviceOnvifBind e campos onvifIP/onvifHttpPort/onvifRtspPort; não demonstram que todo dispositivo Hilevel exporte ONVIF. O cliente independente implementa GetProfiles e GetStreamUri via HTTPS com WS-Security UsernameToken PasswordDigest, seleção no visualizador e reprodução RTSP. TLS padrão, sem redirecionamento, sem credenciais persistidas e sem envio a outro IP. Câmeras HTTP-only e certificados não confiáveis ficam no modo manual RTSP; nenhuma validação física ou autenticação de nuvem foi realizada. Não se declara integração proprietária concluída.
