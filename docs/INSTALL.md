# Enterprise Cams 0.1.0 — APK de testes

Prévia com QR: compilação e testes unitários aprovados. O workflow 34541251856 falhou ao compilar os testes de tela por ausência do Espresso. A dependência foi corrigida e os sete testes de tela executaram na rodada seguinte: quatro passaram e três falharam. Os ajustes de sincronização e do controlador de permissões estão no workflow 34575358109, ainda sem resultado final conferido. A validação completa continua pendente. Bobby mostrou o painel funcionando com dois acessos no aparelho. Consulte o registro de continuidade para distinguir esse teste manual dos cenários ainda pendentes.

Arquivo: `Enterprise-Cams-0.1.0-debug.apk`.
Pacote: `org.enterprisecams.app`.
Android: 6.0 ou superior (API 23+).
Assinatura: **debug de testes**, gerada pelo ambiente de compilação; não é assinatura de distribuição final.

1. Baixe o APK no Android e abra-o.
2. Quando o Android solicitar, autorize a instalação pela origem usada para abrir o arquivo.
3. Abra Enterprise Cams e toque em Adicionar câmera.
4. Dê um nome. Leia o QR pela câmera ou por uma imagem, informe o link/nome do aplicativo, ou escolha o aplicativo indicado no manual. Confira a seleção e continue.
5. Se o aplicativo estiver ausente, toque em Instalar para ir à página oficial na Play Store. Instale/abra o aplicativo oficial, configure a câmera e confirme que a imagem funciona nele.
6. Volte ao Enterprise Cams e toque em Concluí a configuração.

O toque no cartão **abre o aplicativo oficial**. Nesta versão, escolha a câmera dentro dele. Não há vídeo ou controle PTZ dentro do Enterprise Cams.

É possível cadastrar várias câmeras do mesmo aplicativo. Cada entrada conserva seu nome e local; os acessos ainda compartilham a tela inicial do aplicativo oficial.

O painel não depende de Google Play Services nem de conta própria. A instalação pelo fabricante também pode ser reconhecida. A câmera e o aplicativo oficial podem depender de internet, conta e serviços do fabricante.

## Dados e atualização

Menu do painel → Salvar backup exporta nomes, locais e favoritos em JSON. Guarde-o em local privado. Importar backup acrescenta os acessos sem substituir os existentes.

A chave debug de uma compilação futura pode mudar. Se o Android recusar uma atualização por assinatura diferente, **exporte o backup antes de desinstalar**. Desinstalar sem backup perde os cadastros locais. A assinatura estável de release ainda está pendente.

## Limites dos testes

Consulte o registro de continuidade no repositório para os resultados exatos. Testes de encaminhamento em emulador usam um simulador isolado; não comprovam integração por câmera com aplicativos oficiais ou câmeras físicas.

O módulo `qa-stub` do repositório é exclusivo do emulador de testes e não deve ser instalado no telefone.

## Identificação por QR

A permissão de câmera só é solicitada ao usar o leitor ao vivo. Você pode recusá-la e usar uma imagem, colar o link ou selecionar o aplicativo. O reconhecimento é local e limitado ao catálogo confirmado: links da Play Store, nomes/pacotes exatos e alguns esquemas identificados nos aplicativos estudados. Um QR com apenas um serial ou um link não reconhecido exige escolha pelo manual; o painel não adivinha o fabricante e não abre esse link automaticamente.

Os APKs enviados por Bobby serviram para estudo de funcionamento e interoperabilidade. Não são instaladores distribuídos pelo Enterprise Cams. O canal normal do aplicativo de câmera é a Play Store; exceções usam o canal oficial do fabricante.
