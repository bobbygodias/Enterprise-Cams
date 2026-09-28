# Enterprise Cams — contrato atual do agregador

## Prévia do visualizador interno — atualização posterior de 27/09/2026

A branch agora contém a implementação 0.2.0-preview: tela interna por câmera, player Media3, clipe local de teste e reprodução temporária de links HTTPS autorizados; cadastro sem depender da instalação oficial. O CI foi acionado para compilar e verificar. **Não há integração autenticada com as nuvens nem teste com câmera física.** O relato abaixo sobre “apenas launcher” descreve o estado anterior desta sessão. O roteiro e os limites atuais estão em [PLAYER-TEST.md](PLAYER-TEST.md). Não presumir APK compilado ou testes aprovados sem conferir o workflow do commit.

O login moderno examinado em `HttpServiceAdapter.getRequestBody` acrescenta `appId`/`appToken`, dados de versão/pacote/região; a assinatura anônima depende de uma interface do SDK. Portanto, mapear email/senha e `accessId`/`accessToken` não encerra a integração independente. Nenhum segredo de terceiros foi levado para o código próprio.

Atualizado em 27/09/2026. Bobby Dias e Andrew Vox.

## Decisões confirmadas nesta sessão

O Enterprise Cams deve mostrar vídeo e controles dentro de sua própria interface. Abrir o aplicativo do fabricante para assistir à câmera não satisfaz o requisito atual. Preservar o layout interno existente e usar o novo emblema circular azul/ciano fornecido por Bobby.

As câmeras continuam vinculadas às contas, servidores e serviços de nuvem dos fabricantes. O Enterprise Cams não substitui o serviço vendido por eles. O usuário compra e renova a assinatura no aplicativo oficial. Respeitar as permissões, a validade e as limitações reais do serviço contratado. Nenhuma alteração de firmware, mudança de vinculação, modo exclusivamente local ou desativação de cloud deve ser usada como solução automática.

Bobby esclareceu que muitos dos modelos em uso não oferecem ONVIF no aplicativo oficial, ou vinculam essa opção a um modo que interrompe a nuvem. Isso é uma informação de compatibilidade trazida pelo usuário; não foi feito ensaio físico nesta sessão. Portanto, ONVIF/RTSP **não foi aprovado como substituto da integração principal**. Um transporte local só poderá ser adicional quando sua coexistência com os serviços contratados estiver demonstrada para o modelo.

Pagamento, vencimento e renovação continuam sob controle do fabricante. Uma assinatura vencida deve restringir somente as funções que o fabricante efetivamente condiciona ao pagamento; não inventar uma obrigação de assinatura para vídeo ao vivo ou outras funções que ele ofereça gratuitamente. Estado desconhecido, erro de rede e assinatura vencida são estados diferentes.

Instalação e configuração inicial, quando necessárias, continuam no aplicativo oficial conforme o briefing. A renovação é outra exceção expressamente confirmada ao requisito de uso diário sem abrir outros aplicativos. Não apresentar essa exceção como autorização para encaminhar a visualização normal ao fabricante.

## Privacidade

O Enterprise Cams deve ser gratuito de anúncios e telemetria comercial. As trocas necessárias para autenticação, vídeo, controles e serviço de nuvem devem ser limitadas à função solicitada. Não afirmar que removemos a telemetria de um fabricante sem analisar e testar o componente efetivamente integrado. Incorporar um APK inteiro com seus SDKs publicitários não atende ao requisito.

## O que foi efetivamente recuperado e verificado

- Repositório recuperado de `main`, commit `d146221be01e12db60ccadf1eaaf793131f1c79b`. O PR inicial já foi incorporado à principal; o MD antigo ainda falava em PR não incorporado.
- A base 0.1.0 é um launcher Kotlin/Compose. Não possui player, cliente de nuvem, transporte P2P, controle interno ou acesso ao inventário privado dos aplicativos oficiais.
- O novo logotipo foi encontrado e aberto corretamente: PNG RGBA, 1254 × 1254, com transparência. Foi aplicado ao recurso `app/src/main/res/drawable-nodpi/enterprise_badge.png` na branch de revisão. O arquivo de origem foi preservado.
- Foram examinados os manifests binários dos APKs enviados: Yoosee 6.32.3, XMEye 1.6.2.46, Hilevel 1.20260805.295, iCSee 7.9.4_G e Enterprise Cams 0.1.0.
- Nas quatro versões dos fabricantes examinadas, não foram encontradas as declarações `allowUntrustedActivityEmbedding` ou `knownActivityEmbeddingCerts`. Portanto, a rota padrão de incorporação de Activities entre aplicativos não está habilitada por esses manifests para o Enterprise Cams.
- Não foi localizado, entre os serviços/providers explicitamente exportados examinados, um contrato confirmado para ceder ao agregador uma superfície de vídeo autenticada. Essa inspeção não prova a inexistência de toda API possível.
- XMEye contém `com.lib.FunSDK`, incluindo `XMCloundPlatformInit`, `DevLogin`, `MediaRealPlay` e métodos de reprodução da nuvem. Hilevel contém `com.v2.clsdk.CLSDK`, `changeCloudToken` e bibliotecas nativas próprias. Yoosee contém `com.p2p.core.MediaPlayer`, callbacks e bibliotecas P2P próprias. A presença desses componentes identifica caminhos de integração, não comprova que sejam reutilizáveis isoladamente nem que contas/assinaturas funcionem no Enterprise Cams.

## Bloqueio atual

Ainda não há uma conexão autenticada e funcional entre o Enterprise Cams e qualquer uma dessas nuvens. A instalação do aplicativo oficial não concede automaticamente acesso à sessão, ao inventário ou à sua tela de reprodução. Intents e atalhos abrem o outro aplicativo; não resolvem o requisito de visualização interna.

Para prosseguir no caminho principal, é necessário estabelecer um contrato funcional por fabricante: autenticação da conta existente, identificação da câmera, abertura de vídeo dentro do host, comandos, consulta de direitos do serviço e encerramento seguro da sessão. Pode vir de SDK/API, de uma ponte fornecida pelo fabricante ou de outra interface de interoperabilidade efetivamente identificada e validada. Não assumir que um SDK genérico dá acesso às contas e assinaturas já existentes.

Isso é uma dependência técnica ainda não resolvida, não uma cobrança de prova de titularidade ou de autorização do Bobby. Não repetir pedidos genéricos de credenciais já dispensados por ele. Não copiar chaves de aplicativo de terceiros nem publicar senhas, tokens ou arquivos dos fabricantes no repositório.

## Experimento interrompido

Foi iniciada uma implementação de player LibVLC, cofre local de credenciais e ONVIF. Após o esclarecimento sobre preservação da cloud, o experimento foi separado da linha principal no commit `0ec3f5b`, branch `research/onvif-not-primary-20260927`. Não foi compilado, integrado ao fluxo principal nem testado em câmera. **Não é uma entrega, não é uma alternativa aprovada ao agregador e não deve ser instalado como solução.**

Se essa pesquisa for retomada futuramente, revisar especialmente o orçamento de reconexão — o contador é reiniciado junto do efeito de reprodução —, lifecycle, proteção do transporte, capacidades PTZ e testes de autenticação. Não reutilizar o experimento como se estivesse validado.

## Critério de conclusão da primeira integração

1. Usar uma câmera já associada à conta oficial, sem alterar seu vínculo ou modo de funcionamento.
2. Autenticar por uma interface verificada, sem ler dados privados de outro aplicativo como atalho.
3. Mostrar a imagem e as telas reais da câmera dentro do Enterprise Cams; não confundir perfis de qualidade com lentes diferentes.
4. Operar somente os controles realmente suportados, incluindo PTZ, áudio e sirene quando a interface os oferecer.
5. Confirmar que a gravação e os serviços contratados continuam funcionando no fabricante durante e depois da sessão no agregador.
6. Respeitar vencimento e renovação. Não simular resposta do servidor nem usar o relógio local como substituto da autoridade do fabricante.
7. Testar câmera errada, conta incorreta, permissão revogada, duas câmeras da mesma conta, perda de rede e retorno do segundo plano.
8. Verificar ausência de anúncios/telemetria comercial nos componentes distribuídos e registrar testes e limitações antes de gerar uma entrega.

## Retomada objetiva

Ler este arquivo antes dos MDs históricos. Abrir a branch `feat/cloud-aggregation-contract`. Priorizar a primeira integração real com o fabricante usando o material já fornecido; não recomeçar pelo launcher e não tratar ONVIF como substituição silenciosa. Não chamar uma nova compilação do launcher com ícone atualizado de aplicativo concluído.

## Investigação adicional — Yoosee

Foi localizado e clonado para consulta o repositório do próprio GWTimes `IoTVideo-Android`, revisão `0e8ae768843018f7d46194f715f9bd46f4a79fb6`, último commit de 13/11/2020. Ele inclui `iotvideo-release.aar` com arm64-v8a, armeabi-v7a e armeabi, exemplos de player, login e reprodução de nuvem. Não foi copiado para o Enterprise Cams.

O exemplo registra o SDK com `IoTVideoSdk.register(accessId, accessToken)` e possui camada de contas com credenciais de produto/serviço. O APK Yoosee enviado usa `com.jwkj.iotvideo.init.IoTVideoInnerInitializer`, com `nativeRegister(JLjava/lang/String;Ljava/lang/String;ISSI)V` e atualização de token. Há diferença concreta de interface; compatibilidade de conta, dispositivo, protocolo e serviço atual não foi demonstrada.

O código do exemplo de 2020 também desativa validação de certificado/hostname e grava credenciais em logs na camada de contas. Essas práticas pertencem ao exemplo examinado; não foram encontradas por isso automaticamente no APK atual e não devem ser transplantadas ao nosso código.

Um segundo caminho de integração a avaliar com o fabricante é uma ponte local autenticada no aplicativo oficial: ele mantém sessão, nuvem e assinaturas, e oferece ao agregador uma superfície de vídeo e comandos autorizados. Isso exigiria suporte ou alteração no aplicativo oficial; não está disponível nos APKs examinados e não foi implementado. Não tratar esse caminho como autorizado para modificar/distribuir APKs de terceiros só porque foi descrito aqui.

Não foi feito login em conta de usuário, envio de credenciais a servidores, teste de cobrança, alteração de firmware nem tentativa de burlar validações. Falta uma interface atual e validada de autenticação/reprodução que permita a primeira conexão real.

## Preservação e publicação

A primeira tentativa de envio da branch `feat/cloud-aggregation-contract` ao GitHub foi rejeitada pela revisão automática do ambiente por falta de autorização explícita para publicação. Bobby então habilitou o conector GitHub e autorizou expressamente tentar de novo, reiterando “Vai fundo”. O conector confirmou acesso de escrita ao repositório `bobbygodias/Enterprise-Cams`. A nova tentativa pelo Git do terminal falhou por ausência de credencial; a publicação das alterações preparadas passou a ser feita pelo conector autorizado. Conferir a branch remota e seu conteúdo antes de afirmar que o envio foi concluído.

O checkpoint privado salvo antes da autorização de nova tentativa inclui o histórico Git e as duas linhas locais: contrato/ícone na branch `feat/cloud-aggregation-contract` e experimento descartado como caminho principal em `research/onvif-not-primary-20260927`. O experimento é incompleto e não testado e não faz parte das alterações para publicação. A autorização para enviar a branch não implica que a integração esteja concluída. Nenhuma dessas alterações foi incorporada à branch principal pública.

Recuperação do bundle incluído no checkpoint:

```sh
git clone Enterprise-Cams-2026-09-27.bundle Enterprise-Cams
cd Enterprise-Cams
git switch feat/cloud-aggregation-contract
```

## Avanço após a hipótese de Bobby — autenticação Yoosee

Bobby propôs que o cadastro da câmera libera credenciais e que os endereços de integração estão no APK. A investigação estática da fonte Yoosee enviada confirmou parcialmente esse caminho: existem rotas de login, vinculação, inventário e renovação de token. Isso não demonstra que uma credencial do firmware, sozinha, autorize nosso aplicativo.

| Evidência na fonte enviada | Resultado observado |
| --- | --- |
| `com.libhttp.http.HttpService.login` | Rota `Users/LoginCheck.ashx`, com parâmetros de conta e sessão. |
| `LoginResult.DataBean` | Campos `accessId`, `accessToken`, `expireTime`, região e sessão. Nenhum valor real de usuário foi usado. |
| `m8.a.a` | Copia o identificador e o token da resposta de login para os campos `q` e `r` da conta ativa. |
| `com.jwkj.c.g` | Lê esses campos e chama `IoTVideoInitializer.register` e `AccountMgr.setAccessInfo`. |
| `HttpInterface` | Declara `/openapi/app/user/device/listDevice` e `/openapi/app/user/reGenUsrAcceccToken`, além de rotas distintas para vinculação. |
| `AddBaseParamsInterceptor` e `ao.c` | A assinatura de requisições usa o token da conta e delega uma operação a `IP2PAlgorithm.sha1WithBase256`. |

O mapa de infraestrutura também ficou claro na fonte: `HttpServiceAdapter` inicia a API IoT em `https://openapi-iot.cloudlinks.cn`; `Constants` separa o host de reprodução `https://saas-playback.cloudlinks.cn/`; a camada histórica lista `api1.cloudlinks.cn` até `api4.cloud-links.net`; e o registro do GSDK carrega hosts P2P `p2p1` a `p2p10` da infraestrutura Cloud Links. Esses nomes são endpoints observados no código fornecido, não autorização para acessá-los nem prova de que todos estejam ativos para cada região.

O caminho de vídeo também é identificável: `com.jwkj.iotvideo.player.LivePlayer` recebe `deviceId` no construtor e expõe pausa, retomada, encerramento, definição de qualidade, intercom e chamada de vídeo por interfaces apoiadas por métodos nativos. `MultiViewModel` instancia esse player com o identificador do dispositivo. Isso mostra que o APK traz uma camada de reprodução própria; não demonstra que suas bibliotecas nativas possam ser redistribuídas ou usadas fora do aplicativo sem o contrato do fabricante.

A fonte examinada é o ZIP fornecido como `enterprise-patched-source`. O script próprio `tools/inspect_yoosee_auth.py` reproduz o inventário sem fazer rede, sem extrair arquivos e sem imprimir valores de chaves ou corpos de métodos. O relatório `docs/YOOSEE-AUTH-EVIDENCE-2026-09-27.json` registra hashes, nomes e presença de marcadores no DEX do APK original enviado. Presença de strings no APK não comprova equivalência integral com a fonte modificada nem execução bem-sucedida.

Próximo passo preciso: verificar como autenticar a conta existente pelo fluxo correto, obter a sessão e acoplar uma implementação compatível de assinatura/P2P/player ao Enterprise Cams. Não começar vinculando novamente a câmera e não copiar a sessão privada do aplicativo oficial. Não presumir que basta conhecer a URL, nem afirmar que a ausência de um SDK novo já prove impossibilidade. Ainda faltam uma execução autenticada e reprodução real; nenhum token de usuário foi obtido nesta sessão. A ferramenta e o relatório registram os hosts sem valores de credenciais ou chaves de cliente.

## Publicação confirmada

A publicação pelo conector GitHub foi concluída e conferida. PR de trabalho: https://github.com/bobbygodias/Enterprise-Cams/pull/2, em rascunho. O primeiro commit remoto é `d2c5f47a9cdfd68be9076cd62434bb05f5c6a413`; os achados posteriores de autenticação são acrescentados na mesma branch. A principal permanece sem essas alterações. O PR contém documentação, ícone e ferramenta de inspeção própria; não contém APKs, SDKs, código descompilado ou credenciais de fabricantes.

## Referências técnicas consultadas

- Android, incorporação entre aplicativos e adesão exigida do aplicativo hospedado: https://developer.android.com/develop/ui/views/layout/activity-embedding#cross-app_embedding
- Exemplo de integração Android FunSDK do fornecedor: https://github.com/jlinklab/jlink-funsdk-android-demo
- Repositório Gwell/GWTimes, útil como referência de integração; o SDK ali documentado é para iOS e não constitui um SDK Android entregue: https://github.com/GWTimes/GWP2PSDK
- Exemplo Android oficial encontrado nesta investigação: https://github.com/GWTimes/IoTVideo-Android

Nenhuma nova integração com nuvem, compra, renovação, mensagem a fabricante ou teste em câmera física foi realizado nesta sessão. Nenhum novo APK foi validado ou entregue.

## Consulta oficial complementar — 27/09/2026, São Paulo

A [documentação Tencent para aplicativos próprios](https://cloud.tencent.com/document/product/1131/83097) orienta criar uma aplicação para obter AppKey/AppSecret e manter AppSecret no backend. Há [SDK Android de vídeo documentado](https://cloud.tencent.com/document/product/1131/54716). Isso identifica uma oferta pública de SDK; não demonstra acesso de um projeto novo às contas, câmeras e assinaturas existentes do Yoosee/Cloud Links. Compatibilidade e autorização entre os dois contextos continuam sem validação. Não criar uma nuvem paralela nem refazer o vínculo das câmeras como atalho.

A página oficial [Multi-Platform Access](https://www.yoosee.com/productcontentb/491.html) anuncia acesso por navegador de PC, mas não apresentou endpoint de login ou API incorporável na consulta. O [artigo de desktop do próprio fornecedor](https://business.yoosee.com/blog/yoosee-app-for-pc-download), datado de 09/09/2026, descreve cliente Windows e emulador Android conforme o modelo. O [link de compartilhamento](https://share.yoosee.com/share/) orienta abrir/instalar Yoosee e autenticar lá. Portanto, não foi comprovado um player web que possa simplesmente ser incorporado ao Enterprise; essa possibilidade permanece uma investigação, não uma função entregue.
