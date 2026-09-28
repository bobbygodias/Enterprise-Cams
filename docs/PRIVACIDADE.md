# Privacidade — prévia 0.2.1

O Enterprise guarda nomes, locais, fabricante e favoritos no armazenamento privado do Android. Não cria conta própria, não contém anúncios ou telemetria e não solicita credenciais de nuvem nesta prévia.

O backup JSON, exportado por escolha do usuário, contém esses cadastros. Não contém vídeos, senhas ou links de transmissão. Não é criptografado; escolha um local privado. A importação acrescenta dados sem substituir os existentes.

O teste de vídeo incluído funciona offline. Se o usuário escolher reproduzir um link HTTPS autorizado, o Enterprise acessa esse endereço e os recursos HTTPS referenciados pelo vídeo, como playlists e segmentos. O servidor de origem recebe a solicitação e o IP, conforme o serviço contratado. A reprodução não passa por um servidor Enterprise.

Links ficam somente em memória durante a tela de reprodução, sem DataStore, estado salvo, backup ou cache de vídeo em disco. Logs de Media3 são desativados para evitar exposição de endereços assinados. Links expiram conforme o serviço que os emitiu; a prévia não renova tokens nem assinaturas.

Permissões próprias: INTERNET para links de vídeo e CAMERA opcional para ler QR. O reconhecimento do QR é local e não executa a URL lida. Acesso à câmera é solicitado apenas no leitor ao vivo. Não há localização, microfone ou gravação em segundo plano nesta implementação.

Contas, câmeras, gravações e assinaturas continuam sob responsabilidade dos fabricantes. A integração autenticada com essas nuvens ainda não foi implementada. Abrir/instalar o aplicativo oficial na configuração é uma escolha explícita; tocar no cartão da câmera mantém o usuário no Enterprise.
