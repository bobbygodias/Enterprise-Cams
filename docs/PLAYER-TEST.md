# Enterprise Cams 0.2.0-preview — teste do visualizador interno

Esta prévia abre cada câmera em uma tela do Enterprise. Não autentica contas Yoosee, iCSee, Hilevel, V380 ou V380 Pro. O cadastro de uma câmera não representa uma conexão com ela.

## Teste no telefone de Bobby ou Peter

1. Abra o Enterprise e cadastre uma câmera, ou use seu cadastro existente.
2. Toque no cartão. O Enterprise deve continuar em primeiro plano, na tela “Sua câmera”.
3. A tela informa “Conexão da câmera pendente”. Isso é esperado: o adaptador de nuvem ainda não está pronto.
4. Toque em “Testar vídeo interno”. Deve aparecer uma imagem em movimento marcada “VIDEO DE TESTE — SEM CAMERA”. O arquivo está no APK e funciona sem internet.
5. Experimente Pausar, Reproduzir, Ativar som e Silenciar. O vídeo tem um tom baixo de teste. Ele começa mudo.
6. Coloque o aplicativo em segundo plano. A reprodução deve parar; ao voltar, o teste é reiniciado. Volte ao painel e confira busca, favoritos e edição.
7. Se houver um link HTTPS de vídeo HLS ou MP4 autorizado pelo serviço de origem, “Tenho um link de vídeo autorizado” permite reproduzi-lo internamente. O link fica em memória e é descartado ao sair da tela/recriar a atividade. Não existe um link de câmera de exemplo nem tentativa de adivinhar credenciais.

O teste 7 exige um link de vídeo real. QR de instalação, ID/serial, URL da loja e link da página do fabricante não bastam. Não altere o firmware nem o vínculo com a nuvem para executar estes testes.

Relatar: modelo/versão do Android; passo que falhou; mensagem visível; se a imagem de teste se moveu; se o app continuou na mesma tela. Não incluir senhas nem links assinados no relato.

## Implementação e limites

- Media3/ExoPlayer 1.8.0, player nativo, controles acessíveis de pausa e som. Sem WebView ou abertura externa para visualizar.
- HTTP em texto claro, URLs com senha, arquivos locais e protocolos fora de HTTPS são recusados no modo remoto. Cada recurso HLS passa pela mesma verificação. Redirecionamentos entre protocolos ficam desabilitados.
- Nenhum link de vídeo entra em DataStore, backup, estado salvo ou logs do Media3. Não há cache de vídeo em disco.
- Liberação do player ao sair da tela e quando o aplicativo deixa de estar visível. Uma repetição automática de carga antes de apresentar erro; novo teste apenas por ação do usuário.
- O vídeo de teste foi gerado por FFmpeg, não contém imagens de terceiros e não representa câmera conectada.
- Login, inventário remoto, gravações de nuvem, PTZ, microfone/intercomunicação e transmissão P2P proprietária continuam pendentes.
- A inspeção de Yoosee demonstrou `accessId`/`accessToken` passando do login ao SDK de vídeo. O corpo do login também contém identidade do aplicativo (`appId`/`appToken`); a assinatura anônima depende do SDK. Localizar essas chamadas não estabelece um cliente independente autorizado e compatível. Nenhum segredo de terceiros foi incorporado ao Enterprise.

Fontes técnicas: [Media3](https://developer.android.com/jetpack/androidx/releases/media3), [ExoPlayer](https://developer.android.com/media/media3/exoplayer/hello-world). Pesquisa de interoperabilidade: [contrato de nuvem](AGREGADOR-CLOUD-2026-09-27.md).

## Evidência

Esta mudança acrescenta testes unitários da política de links e adapta os testes de tela para verificar reprodução real do clipe local, pausa/retomada, segundo plano, permanência no pacote Enterprise e cadastro sem o aplicativo do fabricante. A execução e o resultado devem ser conferidos no workflow do commit. A existência dos testes não significa que passaram. Mesmo com aprovação no emulador, a conexão com uma câmera física continua sem validação.
