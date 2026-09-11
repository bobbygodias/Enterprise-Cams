# Escopo de implementação e evolução

## Combinado preservado

Enterprise Cams é a central Android simples por câmera/local. Os aplicativos oficiais permanecem responsáveis pelo provisionamento e pelos recursos dos equipamentos. O painel deve reduzir o trabalho para encontrar cada câmera e continuar útil em aparelhos modestos.

A instrução atual do projeto e a conversa inicial com Logos definem o primeiro caminho. A especificação mestra de monitoramento é a visão ampliada e permanece como referência de evolução. Os recursos abaixo não são considerados concluídos só por existirem em documentação.

## Primeira entrega

Tela inicial por câmera/local; cadastro guiado; catálogo de cinco aplicativos; encaminhamento à instalação/configuração; confirmação explícita de conclusão; persistência; retomada; busca; favoritos; edição; remoção só do acesso; exportação/importação de cadastros. Identidade visual baseada na imagem fornecida, com controles grandes, contraste alto e lista que aceita nomes longos e fonte ampliada.

## Requisito central ainda pendente

Abertura **direta da câmera individual**, com todas as funções do fabricante. Depende de contrato por ecossistema. A primeira implementação abre o aplicativo correspondente e diz isso claramente. Não existe sincronização automática de câmeras provisionadas nem callback de configuração confirmado.

## Integração e operação nativa futuras

- APIs oficiais, estado real, miniaturas e eventos, apenas quando expostos.
- ONVIF/RTSP e acesso LAN para modelos compatíveis, com autenticação e política explícita de transporte.
- Vídeo, áudio, PTZ, lentes duplas, gravação, reprodução e exportação de evidência.
- Coordenação lente fixa/PTZ, calibração, tracking temporal e reaquisição.
- Automação local, sensores, eventos correlacionados, mapas e múltiplas câmeras.
- Identidade local, convites, permissões, armazenamento/relay próprio, criptografia, diagnóstico e observabilidade.

A lista mestra inclui firmware, detecção de chama/fumaça, integridade de gravações, pré-roll e muitos outros recursos de segurança. Implementação exige capacidades reais do hardware, testes específicos e limites comunicados. O launcher não consegue modificar firmware, acrescentar controles a um aplicativo fechado ou garantir que um app oficial funcione offline.

## Distribuição

APK independente de loja, sem Google Play Services no painel. Android 6.0+ nesta base. A distribuição pública final deverá usar assinatura estável e documentada. Verificação/sideload do Android deve ser revisitada antes de cada release público; não presumir que a situação do ecossistema ficará inalterada.
