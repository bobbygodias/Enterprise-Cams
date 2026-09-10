# Privacidade da primeira versão

Enterprise Cams armazena, no espaço privado do aplicativo, nomes de câmeras, locais, aplicativo associado, favoritos e cadastro em andamento. Não solicita nem guarda login, senha, token, áudio ou vídeo das câmeras.

O APK principal não declara permissão de internet, câmera, microfone, localização, armazenamento amplo, contatos, telefone, acessibilidade ou sobreposição. A biblioteca AndroidX pode declarar uma permissão interna com proteção por assinatura para receptores privados. Isso não dá acesso a dados de outros apps e não exige autorização do usuário.

Não há conta Enterprise Cams, backend, telemetria, publicidade ou SDK de publicidade. O painel consulta somente os pacotes necessários à sua função. Exportação/importação usa o seletor de arquivos do Android, somente para o documento escolhido.

Backup exportado é JSON legível e sem criptografia: contém nomes e locais. A interface avisa disso antes da exportação. Nenhuma senha ou gravação é incluída. Backup automático de nuvem/transferência está excluído; o backup manual fica sob controle do usuário.

Ao abrir um aplicativo oficial ou sua página de instalação, passam a valer o comportamento e as políticas desse aplicativo/site. O Enterprise Cams não remove anúncios, altera autenticação nem garante privacidade adicional ao software de terceiros.

Não aceitar campos arbitrários de pacote, URI ou Intent no backup. Tamanho, versão do esquema, quantidade de registros, IDs e campos são validados antes de qualquer alteração persistente. Arquivo inválido não apaga o painel atual. Importação válida é aditiva, com tratamento de colisão de IDs.
