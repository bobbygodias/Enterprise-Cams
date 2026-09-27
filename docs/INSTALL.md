# Enterprise Cams 0.2.0-preview — APK de testes

Arquivo: `Enterprise-Cams-0.2.0-preview-debug.apk`. Pacote: `org.enterprisecams.app`. Android 6.0/API 23 ou superior. Assinatura debug de testes; ainda sem assinatura estável de distribuição.

Esta prévia testa o visualizador interno. **Ainda não conecta a conta nem transmite a câmera pela nuvem do fabricante.** O vídeo de teste não é uma câmera real. Veja [PLAYER-TEST.md](PLAYER-TEST.md) para o roteiro completo.

1. Na versão anterior, menu do painel → Salvar backup. Guarde seus cadastros antes da atualização.
2. Baixe e abra o APK no Android. Autorize a instalação pela origem utilizada se o Android solicitar.
3. Se houver conflito de assinatura, preserve o backup antes de desinstalar a versão anterior. A reinstalação apaga os cadastros locais; importe o backup depois.
4. Abra o Enterprise, cadastre ou selecione uma câmera e toque em “Salvar câmera no painel”. Não é necessário instalar o aplicativo oficial para salvar esse cadastro.
5. Toque no cartão para abrir o visualizador interno e em “Testar vídeo interno” para verificar reprodução, pausa e som.

A configuração inicial de câmeras novas e a compra/renovação de nuvem continuam pelos canais oficiais do fabricante. Nenhuma alteração de firmware, conta ou assinatura faz parte deste teste.

O APK debug traz o teste local e o campo opcional para link HTTPS autorizado de vídeo. Links não são persistidos e não entram no backup. QR/serial/link de instalação não substituem uma conexão de nuvem.

O workflow inclui compilação, testes unitários, lint e testes de tela. Consulte o resultado do commit registrado em `COMMIT.txt`; não presumir aprovação de testes ainda em execução. Não instale o módulo de simulação `qa-stub` no telefone.
