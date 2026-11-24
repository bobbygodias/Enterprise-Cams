# Enterprise Cams — matriz de evidências para vídeo autenticado
Data: 24/11/2026 (Goiânia). Estado: INVESTIGAÇÃO TÉCNICA, integração funcional PENDENTE.

## Decisão de escopo preservada

O objetivo atual é assistir às câmeras *dentro* do Enterprise Cams, sem navegação diária pelos aplicativos oficiais, preservando associação às contas, serviços de nuvem, assinaturas e controles que cada fabricante realmente oferece. Instalação/configuração inicial e renovação podem permanecer no aplicativo oficial. **ONVIF, RTSP ou links genéricos não são substitutos aprovados para a integração com a nuvem**; só podem ser complementares quando a coexistência for demonstrada. Nenhum teste físico, login real, captura de tráfego nem extração de segredo foi realizado na elaboração desta matriz.

Fontes confrontadas:
- `docs/AGREGADOR-CLOUD-2026-09-27.md` (branch `feat/cloud-aggregation-contract`): decisões sobre serviços contratados, autenticação e condições de conclusão.
- `docs/ANALISE-APKS.md` (main): observações estáticas por pacote/versão.
- `docs/INTEGRACOES.md` e `docs/CONTINUIDADE.md` (main): limites e histórico.
- PR #2 (player interno HTTPS HLS/MP4 e clipe de teste); PR #3 (reparo do teste instrumentado). Ambos estavam abertos e em rascunho na consulta de 24/11.

## Matriz: capacidade demonstrada versus hipótese

| Fabricante | Evidência disponível | O que **não** foi demonstrado | Próxima verificação segura |
| --- | --- | --- | --- |
| iCSee (`com.xm.csee`) | Contrato *interno* de atalho de launcher: `WelcomePageActivity`, `xmApp=addShortCut` e `DESK_DEVICE_ID` (análise estática do pacote recebido). | Streaming autenticado dentro do Enterprise Cams; token/API externo; funcionamento do atalho em aparelho físico. O atalho continua abrindo o app oficial. | Identificar documentação atual de interoperabilidade e um contrato autorizado para obter stream/controles; se for testar o atalho, classificar estritamente como navegação, não como vídeo interno. |
| Hilevel (`com.sotaviz.hilevelen`) | Deep link com parâmetro `config` e componentes `com.v2.clsdk.CLSDK` na versão estudada. | Endpoint externo de reprodução, compatibilidade de conta/assinatura, abertura direta do vídeo; o deep link examinado não a comprovou. | Localizar especificação e licença de integração do serviço atual; separar configuração, autenticação e vídeo. |
| Yoosee (`com.yoosee`) | `yoosee://share` alcança WebView; bibliotecas P2P próprias. Um exemplo antigo `GWTimes/IoTVideo-Android` apresenta registro por tokens, mas com diferenças para o pacote atual. | API/SDK de sessão vigente compatível com a conta e serviços já associados, vídeo interno, direitos de assinatura, segurança dos exemplos antigos. | Confrontar documentação atual, interfaces públicas e licenças, **sem** portar exemplos que desabilitam verificação TLS ou registram credenciais. |
| V380 (`com.macrovideo.v380`) | Identidade de pacote conhecida na base do painel. | SDK, autenticação externa, identificador de câmera, vídeo/controles; não havia APK disponível na rodada documentada. | Buscar documentação pública do fabricante, sem inferir recursos pela semelhança com V380 Pro. |
| V380 Pro (`com.macrovideo.v380pro`) | Identidade de pacote distinta de V380. | Os mesmos contratos de vídeo, conta e serviço; nenhum ensaio com APK físico confirmado nesta investigação. | Investigar separadamente a versão Pro e suas condições de uso. |

**XMEye**: consta na análise histórica, mas **não** está entre os cinco alvos iniciais do painel. Seus parâmetros de notificação de alarme não equivalem a uma API de reprodução. Não reutilizar o contrato interno do iCSee por suposição.

## Contrato mínimo necessário para chamar uma integração de real

Uma implementação por fabricante precisa demonstrar, por evidência verificável:

1. **Conta e direitos:** autorização do próprio usuário por fluxo permitido; estado de conta/serviços consultado sem copiar sessões privadas de outros aplicativos, sem armazenar senhas no repositório e sem presumir que plano pago é exigido para recursos gratuitos.
2. **Dispositivo correto:** identificador estável no escopo da conta; seleção inequívoca entre pelo menos duas câmeras; erro distinto para câmera inexistente, conta sem acesso e rede indisponível.
3. **Reprodução interna:** negociação do vídeo por interface/SDK autorizado; primeiro quadro da câmera física dentro do host; comportamento em perda de rede, retorno de segundo plano, expiração e encerramento.
4. **Controle e nuvem:** somente operações realmente suportadas; demonstrar que gravação/serviços contratados permanecem operacionais quando aplicáveis, sem alteração de firmware nem associação da câmera.
5. **Privacidade e segurança:** credenciais e URLs temporárias fora de logs/backup/telemetria; sem componentes publicitários de terceiros; TLS íntegro; sessão encerrada e recursos liberados quando apropriado.
6. **Evidência de execução:** separar testes unitários e emulador, clipe/URL artificial, teste de stream autorizado e uso com câmera real. CI verde **não** satisfaz o item 3.

## Caso de teste de aceitação — primeiro fabricante

- Dois dispositivos diferentes **já vinculados** à mesma conta oficial; autenticação permitida.
- Iniciar o Enterprise Cams sem aplicativo oficial em primeiro plano; abrir A e depois B; verificar no vídeo qual câmera está ativa, sem trocar silenciosamente de dispositivo.
- Repetir com app frio, app em segundo plano e após revogação de acesso à conta.
- Interromper/repor conectividade; verificar erro interpretável e encerramento limpo, sem impressão de token/URL em logs.
- Comparar serviços de nuvem/gravadores contratados antes e depois, conforme recursos aplicáveis àquele fabricante.
- Registrar versão da câmera e do APK, método/autorização de acesso, passos reproduzíveis e limitações. Não rodar testes reais automaticamente nem solicitar credenciais ao interlocutor.

## Resultado desta rodada e continuidade

Foi feita **triagem estática** das fontes públicas e dos PRs #2/#3 em 24/11/2026. **Não** houve implementação de conector autenticado, teste de hardware, reprodução de câmera nem validação do serviço do fabricante. A matriz identifica exatamente onde faltam contratos verificáveis e impede que um Intent, clipe simulado ou link avulso seja declarado integração concluída.

**Próximo passo técnico:** investigar as interfaces de interoperabilidade **atuais** e condições de licença para um fabricante de cada vez, começando pelo mais documentado, sem alterar o produto antes de existir um contrato verificável. Se nenhum contrato for localizável, registrar bloqueio por fabricante e avançar com outro alvo ativo em vez de repetir build do launcher. Manter PR #2 e PR #3 em rascunho e sem merge/release.
