# Enterprise Cams

Projeto Android de **central unificada de câmeras de diferentes fabricantes**, com identificação por QR Code, conectores próprios e vídeo e controles dentro de um único aplicativo. **Yoosee e Hilevel são prioridades indispensáveis; outras marcas não são excluídas.** A primeira versão de painel/launcher foi uma etapa inicial, não o objetivo definitivo.

## Finalidade acordada e arquitetura (27/11/2026)

O Enterprise Cams deve permitir que o usuário identifique e adicione câmeras de ecossistemas variados sem precisar conhecer antecipadamente qual aplicativo oficial usar. O QR Code é uma entrada de descoberta: o aplicativo interpreta os dados realmente presentes (como identificador, endereço, marca, modelo ou link) e complementa a identificação por mecanismos de descoberta e informações disponíveis. O catálogo inicial de cinco pacotes **não limita o número de fabricantes** que poderemos integrar.

Cada fabricante continua responsável pela **autenticação de suas contas e dispositivos**. Nosso trabalho é implementar a comunicação com esses fluxos existentes e construir a camada de interoperabilidade: identificação, escolha do conector, sessão autorizada, seleção da câmera, vídeo e controles na mesma interface. Não inventamos um novo sistema de autenticação em lugar do fabricante. Independência dos aplicativos oficiais não significa independência obrigatória dos servidores de nuvem de cada marca.

Complicações de protocolos, compatibilidade e segurança são problemas de engenharia a enfrentar. A segurança deve ser implementada por medidas proporcionais aos riscos concretos, não por promessas de 100% de segurança. Os marcos técnicos devem produzir **código funcional e testes verificáveis**, além da documentação necessária. Consulte [Escopo](docs/ESCOPO.md) e [Integrações](docs/INTEGRACOES.md).

**Estado observado na main em 25/11/2026: prévia Android 0.1.0, com leitura QR limitada ao catálogo e encaminhamento aos aplicativos oficiais.** A branch de QA do [PR #3](https://github.com/bobbygodias/Enterprise-Cams/pull/3) teve workflow aprovado em 22/11/2026, mas não demonstra vídeo de câmera real. O [PR #2](https://github.com/bobbygodias/Enterprise-Cams/pull/2) contém player interno, e o [PR #4](https://github.com/bobbygodias/Enterprise-Cams/pull/4), pesquisa de contratos. Essas branches não comprovam o objetivo final. O repositório começou com a licença CC0; veja [CONTINUIDADE.md](docs/CONTINUIDADE.md). (Ainda a ser atualizado).

## O que esta versão faz

- Organiza acessos por câmera/local, com busca, favoritos e edição.
- Lê QR pela câmera ou por imagem; reconhece os aplicativos catalogados e, em links exatos da Play Store, registra **candidatos de pacotes externos** com validação de formato. O fabricante e a compatibilidade desses candidatos permanecem **não verificados**; um serial isolado não autoriza identificação por inferência.
- Nesta prévia, o catálogo verificado inclui Yoosee e Hilevel. A seleção de outros pacotes por QR é apenas a primeira etapa de descoberta, **não comprova conector nem streaming**, e depende de confirmação explícita da pessoa.
- Encaminha à Play Store; se a loja não abrir, tenta sua página HTTPS no navegador.
- Orienta a configuração da câmera no aplicativo oficial e conserva o cadastro ao sair e voltar.
- Adiciona o acesso ao painel após confirmação do usuário e verificação de que o aplicativo está disponível.
- Abre o aplicativo correspondente ao tocar no acesso.
- Exporta/importa backup JSON local, sem apagar os cadastros existentes na importação.
- Funciona sem conta Enterprise Cams, backend, anúncios, telemetria.
- Necessário conta com os fabricantes e suas respectivas câmeras físicas.


O objetivo de abrir diretamente uma câmera específica está direto no aplicativo Enterprise Cams no release. Cada adaptador retorna diretamente com identificação da câmera e retorno consistente. Não dispensa o login e senha dos fabricantes. Instalar o aplicativo original, por si só, não concede acesso ao inventário nem a seus controles. O cadastro com as aplicações dos fabricantes ainda é necessária.

## Aplicativo em fase BETA.

Neste momento. Somente o Yoosee e o Hilevel estão em funcionamento. A integração com os demais está pendente.

O painel funciona offline. A câmera, os anúncios, o login, a latência e a dependência de internet do aplicativo seguem. Esta versão remove publicidade, mas não substitui o cadastro com os fabricantes e as clouds.

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
