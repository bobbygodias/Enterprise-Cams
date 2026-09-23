# Enterprise Cams — Android de laboratório

Checkpoint: 23/09/2026. Objetivo: comprovar boot de Android vazio com KVM, controle ADB e extração de evidências via GitHub Actions. Sem instalar Enterprise Cams, Yoosee ou outro APK; sem login/câmera, backend Vulkan, serviço pago ou túnel.

## Estado inspecionado
- Repositório correto confirmado por Bobby: bobbygodias/Enterprise-Cams (público).
- main: d146221be01e12db60ccadf1eaaf793131f1c79b.
- feat/android-launcher-v0.1: bd29613d3d116054b61f82c3f97a11c3650eb055.
- main já contém o aplicativo e android.yml, que compila e testa com ReactiveCircus. Alguns textos antigos de continuidade ainda descrevem uma main vazia; os arquivos atuais foram conferidos.
- Branch isolada lab/android-emulator-smoke, criada a partir da main acima. Nenhuma alteração na main, na branch do aplicativo ou no workflow android.yml.
- O encaminhamento inicial citava pocketpal-enterprise e llama.rn-enterprise. Ambos foram inspecionados antes da correção de projeto. Um ensaio vazio foi criado somente na branch lab/android-emulator-smoke do PocketPal; passou no run 35829135720. Não houve APK instalado nem alteração de main/Vulkan. Esse ensaio não é evidência de integração Enterprise Cams/Yoosee.

## Configuração
Workflow: [.github/workflows/android-lab.yml](../.github/workflows/android-lab.yml).
Runner padrão ubuntu-24.04; /dev/kvm com leitura/escrita e -accel on; API 35 google_apis x86_64, perfil pixel_6, 2 cores e 2048 MB.
ReactiveCircus fixado no commit a421e43855164a8197daf9d8d40fe71c6996bb0d (v2).
Sem checkout, build, cache ou download de APK. GPU de software; isso não testa GPU física.

O push só dispara ao modificar este workflow na branch exata de laboratório. O workflow normal do aplicativo não é disparado por lab/**. Para repetir sem alterar parâmetros, usar Re-run jobs na execução existente. workflow_dispatch está declarado, mas pode não aparecer na interface enquanto o arquivo não estiver na branch padrão; não fazer merge só para isso.

## Evidências e falhas
sys.boot_completed deve ser 1. O script registra adb devices -l, getprop completo, wm size/density, envia HOME, captura PNG validado por assinatura e exige logcat curto não vazio.
Logs do emulador e logcat durante o boot ficam no runner antes da action encerrar o emulador. Coleta final e upload usam if: always(). Artefato inclui host/KVM, configuração AVD e SHA256SUMS, com retenção de 7 dias.
O adb-after-action pode ficar vazio porque a action encerra o dispositivo; o arquivo adb-devices.txt é a evidência durante o ensaio.
Timeout de boot: 600 s; job: 20 min; comandos ADB limitados. Perda abrupta do runner ainda pode impedir upload.
Se falhar, preservar artefatos/logs, identificar a etapa e alterar uma variável por vez.

## Execuções
- Primeira execução Enterprise Cams: https://github.com/bobbygodias/Enterprise-Cams/actions/runs/35829345204
- Commit do workflow: fb68192ce8e93a2c087ac28288379cd4fbac96a6.
- Resultado: em andamento neste checkpoint.

## Limites
Android efêmero com controle por scripts e devolução de artefatos. Não é aparelho persistente nem sessão interativa em tempo real.
O boot x86_64 não comprova compatibilidade das bibliotecas ARM do Yoosee, login, P2P, vídeo, áudio ou PTZ. Essas são etapas posteriores.

Referências: https://github.com/ReactiveCircus/android-emulator-runner e https://docs.github.com/en/billing/concepts/product-billing/github-actions.
