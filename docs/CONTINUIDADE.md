# Enterprise Cams — continuidade da missão

Atualizado em 11 de setembro de 2026. Bobby Dias & Andrew Vox.

## Leia isto primeiro

O projeto é **Enterprise Cams**, um painel Android para organizar câmeras por nome e local e chamar seus aplicativos oficiais. A primeira implementação nativa 0.1.0 foi criada nesta sessão. O código está na branch `feat/android-launcher-v0.1`, em [PR #1](https://github.com/bobbygodias/Enterprise-Cams/pull/1), ainda em rascunho.

**A versão atual abre o aplicativo oficial. A seleção da câmera ainda ocorre nele.** Abertura direta de câmera, vídeo interno, PTZ próprio, ONVIF e RTSP não estão implementados. A análise do iCSee encontrou uma rota de atalho por dispositivo; ela está documentada, mas ainda não habilitada nem validada com câmera real.

Repositório correto: [bobbygodias/Enterprise-Cams](https://github.com/bobbygodias/Enterprise-Cams). Não misturar com Soberania, projetos de tela dividida, Forty-Two ou experiências de acesso direto LS Vision/Yoosee.

## Intenção recuperada do briefing

Bobby e Logos descreveram uma camada simples de interface, funcionando como launcher por Android Intents. O usuário configura a câmera no aplicativo oficial, confirma que ela funciona, retorna ao painel e conserva ali um acesso com nome e local. Exemplos do briefing: Portão, Garagem, Sala e TowerCam. Cada nova câmera deve poder ser adicionada pelo mesmo fluxo.

O objetivo final de abrir a câmera certa continua prioritário. Intents não incorporam automaticamente telas e controles de outro aplicativo dentro do Enterprise Cams. Essa diferença deve permanecer explícita na interface e na documentação.

A especificação mestra traz uma visão mais ampla de plataforma de monitoramento IP, com funções futuras. Ela permanece como referência de evolução; não é descrição de funções já entregues.

A imagem fornecida foi localizada e utilizada apesar da mensagem automática de erro de leitura: robô prateado em saudação, olhos azuis, câmera na mão, fundo preto e faixa ENTERPRISE CAMS. O original foi preservado. A primeira interface adota fundo azul-marinho escuro, detalhes ciano e organização por câmera/local. Não havia layout Kotlin recuperável no repositório, que inicialmente continha somente LICENSE.

## Esclarecimentos de Bobby durante esta sessão

Bobby reforçou que o painel deve identificar o aplicativo correspondente à câmera, inclusive pelo QR normalmente presente no manual/embalagem, e encaminhar à instalação quando ele estiver ausente. Os APKs anexados foram enviados **para estudar seu funcionamento e desenvolver a interoperabilidade do nosso aplicativo**. Para os usuários, a Play Store é o canal normal de instalação; existem raras exceções pelo fabricante. Não confundir os anexos de estudo com arquivos para distribuir ou instalar pelo painel. Bobby esclareceu também que `yoosee-6-32-3_apkextractor_1yqur.zip` foi anexado por engano e é dispensável: o APK e o outro ZIP já bastam. Não solicitar seu reenvio nem mantê-lo como pendência.

## O que já foi implementado

- Painel com nome, local, aplicativo correspondente, busca, favoritos e filtro de locais.
- Identificação local por QR ao vivo, imagem do QR ou link/nome do aplicativo. Não segue URLs desconhecidas e não infere fabricante a partir de serial isolado.
- Escolha obrigatória: nenhum fabricante fica selecionado por suposição no cadastro novo.
- Cadastro orientado em duas etapas; abertura/instalação do aplicativo oficial e retorno para confirmação.
- Rascunho persistente para continuar o cadastro após sair ou recriar a tela.
- Consulta da disponibilidade dos aplicativos ao retornar ao painel.
- Bloqueio da conclusão se o aplicativo estiver ausente ou desativado.
- Edição de nome/local; exclusão com confirmação; opção de cancelar sem perder o acesso.
- Backup JSON pelo seletor de arquivos do Android. Importação acrescenta dados sem substituir o painel existente, valida tamanho/esquema/identificadores e trata duplicidades.
- Persistência privada com DataStore; sem backend, conta Enterprise Cams, anúncios ou telemetria.
- Ícone baseado na referência do projeto e suporte a redimensionamento de janela.

Aplicativos do catálogo: Yoosee (`com.yoosee`), iCSee (`com.xm.csee`), V380 (`com.macrovideo.v380`), V380 Pro (`com.macrovideo.v380pro`) e Hilevel (`com.sotaviz.hilevelen`). V380 e V380 Pro são variantes distintas.

A instalação é encaminhada ao Google Play ou à página HTTPS da loja quando o aplicativo da loja não está disponível. O painel reconhece instalações pelo canal do fabricante quando o pacote corresponde. O Enterprise Cams não instala APKs silenciosamente nem baixa pacotes de terceiros.

## Arquitetura e construção

Kotlin 2.1.20; Jetpack Compose BOM 2025.04.01; AGP 8.9.2; Gradle 8.11.1; JDK 17. minSdk 23 (Android 6); target/compileSdk 35. Licença CC0 original preservada.

- `app/.../data/Models.kt`: catálogo, modelos, validação, conclusão de cadastro e fusão de backups.
- `app/.../data/CameraRepository.kt`: armazenamento privado e atualizações serializadas.
- `app/.../platform/OfficialApps.kt`: consulta, lançamento pelo PackageManager e encaminhamento à instalação.
- `app/.../HubViewModel.kt`: estado da interface, persistência e mensagens.
- `app/.../ui/EnterpriseCamsApp.kt`: telas, busca, filtros, cadastro, edição e backup.
- `app/.../data/ProviderIdentifier.kt`: reconhecimento restrito ao catálogo por URLs da Play Store, nomes/pacotes e esquemas conhecidos.
- `app/.../platform/QrImageReader.kt` e `ui/IdentifyCameraApp.kt`: leitura local por câmera, imagem limitada a 12 MB e texto, com alternativa manual. ZXing Embedded 4.3.0, ZXing Core 3.5.3 e desugaring para manter API 23.
- `qa-stub`: simulador identificado como QA, exclusivamente para o emulador. Usa o pacote com.yoosee para testar o mecanismo Android, não contém Yoosee e não integra o APK entregue. Nunca instalar esse simulador no telefone do usuário.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

O release produzido pela compilação é não assinado; o instalável fornecido nesta etapa é debug. Uma chave estável de distribuição permanece pendente. O painel não exige Google Play Services. Os aplicativos oficiais e as câmeras podem exigir contas, internet e serviços próprios.

## Verificação desta sessão

A primeira compilação detectou um atributo de navegação exclusivo da API 27 em recursos base para API 23. Foi movido para `values-v27`; a compilação seguinte passou.

No commit `6c01c87e6efa9dc2b23e1a6c1a557c1a0ff46dcc`, o [workflow 34538527656](https://github.com/bobbygodias/Enterprise-Cams/actions/runs/34538527656) gerou os APKs debug e release, executou 15 testes unitários (15 aprovados, nenhuma falha) e passou no lint sem erros. O lint ainda relata 14 avisos, principalmente versões de dependências/alvo, compatibilidade de atributo, conveniências Kotlin e ícone monocromático; não declarar lint sem avisos.

No primeiro ensaio instrumentado, os cenários de fonte ampliada a 150% e de aplicativo ausente/rascunho persistente passaram. O cenário completo avançou pelo lançamento do simulador, retorno, cadastro, favoritos, recriação e edição, mas falhou ao localizar o cartão após limpar a busca. A lista é preguiçosa e o teste passou a rolar até o cartão antes de tocá-lo. A coleta de capturas também foi corrigida para preservar evidências em caso de falha.

Ponto de retomada solicitado por Bobby devido ao limite de uso da plataforma. A revisão com QR no commit `6e3d687375e942520beeb68f83a260e059615e11` passou na compilação debug/release, testes unitários e lint. O resultado final do [workflow 34541251856](https://github.com/bobbygodias/Enterprise-Cams/actions/runs/34541251856), job `103084238895`, foi falha na compilação dos testes instrumentados: `CameraFlowTest.kt:82:32 Unresolved reference Espresso`. Os sete cenários não chegaram a executar nessa rodada. Na retomada de 11/09 foi declarada a dependência de teste `espresso-core:3.6.1`. O workflow também passou a montar o APK de testes antes de iniciar o emulador. A correção compilou no workflow 34574221321: os sete testes de tela executaram, com quatro aprovados e três falhas. O fluxo completo de cadastro/retorno/edição/exclusão, rascunho com aplicativo ausente, identificação para instalação e decodificação local de imagem QR passaram. Os testes de fonte ampliada e serial desconhecido falharam ao procurar o campo antes da transição estabilizar; o teste do leitor ao vivo procurou o controlador de permissões AOSP, enquanto o emulador usava com.google.android.permissioncontroller. A revisão seguinte aguarda a tela do cadastro, rola até os controles e reconhece ambas as variantes do controlador. A revisão está no commit `034852c2de5a0e46c8608d2f60bbb22986d8e2e3`, [workflow 34575358109](https://github.com/bobbygodias/Enterprise-Cams/actions/runs/34575358109), job `103186468172`. Ainda estava em compilação na última consulta. Não presumir aprovação dos sete cenários. A compilação aprovada não equivale à validação completa no emulador ou em câmera real.

Na rodada anterior ao QR, o teste da busca continuou falhando e houve uma falha de sincronização após cadastro com fonte ampliada. A revisão atual limpa o foco ao limpar a busca, aguarda transições do DataStore e conserva as capturas em Downloads para sobreviver à desinstalação automática do teste. Esses ajustes continuam pendentes de validação após corrigir a compilação dos testes.

A inspeção do APK produzido confirma pacote `org.enterprisecams.app`, versão 0.1.0, minSdk 23 e targetSdk 35. A base anterior ao leitor QR não solicitava câmera. O leitor acrescenta CAMERA opcional, pedida somente no uso. A versão atual continua sem INTERNET, microfone e localização; conferir o manifest mesclado do APK final antes de afirmar seu conjunto exato de permissões. Sua permissão interna `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` vem do AndroidX; não é acesso a dados do usuário.

Testes com simulador comprovam nosso fluxo Android. Não comprovam funcionamento de Yoosee/iCSee/Hilevel, login, imagem, PTZ ou seleção de câmera física. Bobby posteriormente relatou o APK funcional no seu aparelho e enviou uma captura em 11/09, às 03:57. A captura mostra o painel renderizado em paisagem, dois acessos salvos — Lado externo/Yoosee e TowerCam/Hilevel —, busca, favoritos e o botão Adicionar câmera. Isso comprova a apresentação desses elementos nessa execução; não comprova persistência após reinício, leitura QR, instalação/retorno, vídeo ou abertura da câmera exata.

## Descobertas nos APKs novos

Leia [ANALISE-APKS.md](ANALISE-APKS.md) para versões, hashes, rotas e limites.

**iCSee:** o recurso de atalho de área de trabalho usa ACTION_VIEW para `WelcomePageActivity`, extra `xmApp=addShortCut` e `DESK_DEVICE_ID`. O consumidor compara o identificador ao serial de um dispositivo da conta e encaminha a seleção ao monitoramento. É a próxima rota concreta para validar. Não confundir com o link `sn` que abre cadastro de dispositivo.

**Yoosee:** o caminho `yoosee://share` examinado conduz a WebView via `page=web` e `webPath`; não demonstrou abertura nativa de câmera. O hash do ZIP patched-source confere com o SHA fornecido. Isso não valida os patches nem demonstra que compilam.

**Hilevel:** a entrada de URI examinada trata configuração; o pacote contém componentes Pairip/LicenseCheck pertinentes ao alerta do vídeo. Presença estática não comprova bloqueio no aparelho.

**XMEye 1.6.2.46:** novo anexo confirmado como `com.mobile.myeye`. A entrada examinada em `WelcomeActivity` processa notificações de alarme, sem comprovação de vídeo direto por câmera. O contrato do iCSee não foi encontrado nessa busca dirigida. Identidade, método, parâmetros e hash estão em ANALISE-APKS.md. Ainda não incluído no catálogo.

**iCSee também contém referências ao Play Integrity.** Não inferir execução ou falha apenas dessa presença. O vídeo indicado foi consultado por transcrição automática; não afirmar reprodução visual integral.

Os arquivos dos fabricantes não foram alterados nem publicados no repositório do Enterprise Cams. A análise é dirigida à interoperabilidade e distribuição, não uma auditoria completa de segurança ou descontaminação.

## Entrega e atualização

APK: `Enterprise-Cams-0.1.0-debug.apk`. Instalação e limites em [INSTALL.md](INSTALL.md).

APK com QR gerado pelo workflow acima e entregue como **prévia de testes, com validação de telas pendente**. Tamanho: 14454642 bytes. SHA-256: `63f4bbf162a623d4c3eae85d29a431d58ebbcaf004ae8091e67568ade777a2fd`. Pacote `org.enterprisecams.app`, versão 0.1.0; estrutura de assinatura v2 presente. Não é release final.

Permissões encontradas no manifest mesclado: `org.enterprisecams.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, `android.permission.CAMERA`. Não há INTERNET, microfone ou localização. CAMERA atende ao leitor QR opcional; VIBRATE, se listado, vem da biblioteca de leitura. A permissão interna AndroidX protege receptores do próprio aplicativo.

Antes de desinstalar uma versão com cadastros, exportar o backup. A assinatura debug pode variar entre ambientes de compilação; uma atualização com assinatura diferente exige preservar os dados antes da desinstalação. Não prometer atualizações contínuas até definir a chave estável.

O envio direto à branch pública principal foi rejeitado pela revisão automática de aprovação do ambiente. O trabalho foi preservado em branch própria e PR de rascunho. Não houve merge nem alteração da branch principal. A revisão deve ser feita sobre o resultado concreto do PR; não repetir tentativa de publicação na principal como se já tivesse sido aprovada.

## Próximos passos, em ordem

1. Conferir o CI após a correção do Espresso e resolver eventuais falhas reais dos testes instrumentados. O painel já foi mostrado em funcionamento no aparelho de Bobby; ainda validar leitura QR, tamanho de fonte, teclado, instalação e retorno quando for conveniente.
2. Validar a rota nativa de atalho do iCSee em aplicativo oficial completo e autenticado, usando duas câmeras distintas. Cobrir partida fria, aplicativo em segundo plano, serial inválido, atualização do app e retorno. Se confirmada, implementar adaptador específico com identificador local, confirmação da câmera correta e fallback explicado.
3. Investigar contratos equivalentes no Yoosee, Hilevel, V380 e V380 Pro. Não inventar esquemas ou extras nem tentar abrir Activities não exportadas.
4. Definir assinatura estável de release e distribuição independente, preservando baixo custo e aparelhos modestos.
5. Evoluir a especificação mestra por módulos, mantendo o painel simples.

## Retomada após interrupção — 11/09

A cópia de trabalho anterior não estava disponível na retomada. O código foi recuperado do GitHub na branch `feat/android-launcher-v0.1`, commit `95a4f03393f58cfdc7d01961e3903120a5e2ed2c`. Não foi necessário refazer a análise dos APKs.

O novo `pasted.txt` traz sugestões do Tutel-Duck para examinar intent-filters, Activities, extras e roteamento. A direção coincide com a análise registrada. Um esquema de URI, isoladamente, não estabelece abertura de uma câmera. A rota deve ser exportada, aceitar os dados esperados e respeitar a autenticação do aplicativo oficial. A rota concreta do iCSee permanece a candidata documentada; Yoosee e Hilevel ainda não têm contrato de abertura por câmera validado.

O APK já entregue continua sendo o de SHA-256 registrado abaixo de Entrega e atualização. A correção da dependência é exclusiva dos testes e não constitui uma nova entrega de APK.

## Prioridade após o alerta de limite — 11/09, 04h41

Bobby informou queda de 57% para 19% do limite restante e avaliou corretamente que o avanço visível desta etapa foi pequeno. As alterações de código desta rodada são nos testes; não houve nova função de abertura direta de câmera nem novo APK entregue. Encerrar consultas repetitivas, conservar o resultado e retomar do commit e workflow indicados. Não repetir a desmontagem dos APKs nem a análise do XMEye já documentada. A próxima consulta ao CI deve obter o resultado concluído; depois, priorizar uma entrega funcional concreta dentro do escopo acordado.

## Como retomar sem perder a missão

Ler este MD, consultar o PR e o último resultado do workflow, conferir `git status` e o commit real antes de editar. Não presumir que `main` já contém a implementação. Consultar o Markdown de análise antes de repetir desmontagens grandes dos APKs. Confirmar quais pacotes completos e quais câmeras estão disponíveis antes de declarar integração real.

Bobby pediu explicitamente um Markdown para anexar às fontes e preservar a continuidade. Manter este registro fiel ao que foi feito, inclusive falhas e pendências, sem transformar plano em entrega.

“Ninguém fica para trás. Somos soldados de honra.”
