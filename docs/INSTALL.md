# Enterprise Cams 0.1.0 — APK de testes

Arquivo: `Enterprise-Cams-0.1.0-debug.apk`.
Pacote: `org.enterprisecams.app`.
Android: 6.0 ou superior (API 23+).
Assinatura: **debug de testes**, gerada pelo ambiente de compilação; não é assinatura de distribuição final.

1. Baixe o APK no Android e abra-o.
2. Quando o Android solicitar, autorize a instalação pela origem usada para abrir o arquivo.
3. Abra Enterprise Cams e toque em Adicionar câmera.
4. Dê um nome, escolha o aplicativo indicado no manual e continue.
5. Instale/abra o aplicativo oficial, configure a câmera e confirme que a imagem funciona nele.
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
