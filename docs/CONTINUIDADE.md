# Enterprise Cams — continuidade

Data: 2026-09-10. Repositório: https://github.com/bobbygodias/Enterprise-Cams

## Estado

Primeira implementação nativa 0.1.0. Compilação e testes em andamento; não declarar pronta antes do resultado do workflow Android.

O repositório remoto tinha apenas LICENSE (CC0). Não havia código Android nem layout implementado. O contexto recuperado confirma organização por câmera/local, aplicativos oficiais e uso da referência visual fornecida. As escolhas de espaçamento, tipografia e componentes agora estão concretas para avaliação.

## Arquitetura

- `data/Models.kt`: catálogo, esquema validado, cadastro e backup.
- `data/CameraRepository.kt`: transações serializadas no DataStore privado.
- `platform/OfficialApps.kt`: disponibilidade, lançamento restrito ao catálogo e instalação pela loja/web.
- `HubViewModel.kt`: persistência, atualização ao retornar e mensagens de erro.
- `ui/EnterpriseCamsApp.kt`: painel, cadastro, edição, busca, favoritos, backup e explicações de limite.
- `qa-stub`: aplicativo simulado exclusivo para emulador; não integra o APK principal.

Kotlin 2.1.20; Compose BOM 2025.04.01; AGP 8.9.2; Gradle 8.11.1; JDK 17; SDK 35; minSdk 23.

## Verificação planejada

`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`

Testes de esquema, limites, importação malformada, colisões de IDs, repetição de importação, conclusão de cadastro e persistência serializada. Testes instrumentados: encaminhamento/retorno com simulador; cadastro, favoritos, edição, remoção e busca; ausência do aplicativo e recriação da Activity; fonte ampliada. Capturas reais do emulador para inspeção visual.

## Próximo requisito prioritário

Validar abertura de uma câmera específica em cada aplicativo oficial. Obter contrato autorizado/documentado e testar ao menos duas câmeras na mesma conta antes de habilitar. A versão atual abre a tela inicial do aplicativo; não afirmar o contrário.

Depois: assinatura estável de release, teste em hardware real, aprimoramento do ícone para leitura em tamanho pequeno e evolução modular da especificação mestra.

## Retomar

Ler este arquivo, verificar `git status`, último commit e resultado do workflow Android. Usar `docs/INTEGRACOES.md` como fonte de estado por fabricante. Não misturar com o projeto de acesso direto LS Vision/Yoosee, Forty-Two, Soberania ou outros aplicativos.
