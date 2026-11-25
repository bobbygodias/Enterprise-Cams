# Escopo de implementação e evolução

## Combinado preservado

Enterprise Cams é uma central Android unificada de câmeras por dispositivo/local, capaz de identificar e integrar ecossistemas de diferentes fabricantes, com vídeo e controles no próprio aplicativo. Os aplicativos oficiais podem continuar responsáveis pelo provisionamento inicial e por etapas de renovação; a autenticação e a autorização dos dispositivos continuam sob a responsabilidade dos respectivos fabricantes. O painel deve continuar útil em aparelhos modestos.

**Especificação reafirmada em 25/11/2026:** construir uma única aplicação com conectores próprios de interoperabilidade, em vez de operar vários aplicativos oficiais. **Yoosee e Hilevel são prioridades indispensáveis**, em razão do uso efetivo; iCSee, V380/V380 Pro e outros fabricantes também pertencem ao escopo expansível. O elenco da primeira versão NÃO é lista definitiva nem limite de implementações. A engenharia deve criar a estrada para cada ecossistema, não uma autoridade alternativa de autenticação.

O QR Code é uma entrada preferencial para identificar e iniciar a integração da câmera, sem exigir conhecimento prévio da marca. O sistema deverá interpretar aquilo que o QR realmente contiver e complementar com descoberta por rede, formatos conhecidos, identificadores, documentação e confirmação assistida quando necessária. Um QR que só contém serial ou link não assegura, isoladamente, a identificação do protocolo. Isso é um problema de descoberta a desenvolver, não razão para restringir a aplicação a cinco aplicativos.

A instrução atual do projeto e a conversa inicial com Logos definem o primeiro caminho. A especificação mestra de monitoramento é a visão ampliada e permanece como referência de evolução. Os recursos abaixo não são considerados concluídos só por existirem em documentação.

## Primeira entrega (marco histórico, não limite do produto)

Tela inicial por câmera/local; cadastro guiado; catálogo inicial de cinco aplicativos; encaminhamento à instalação/configuração; confirmação explícita de conclusão; persistência; retomada; busca; favoritos; edição; remoção só do acesso; exportação/importação de cadastros. Identidade visual baseada na imagem fornecida, com controles grandes, contraste alto e lista que aceita nomes longos e fonte ampliada.

## Requisito central ainda pendente

**Reconhecer uma câmera, obter sessão autorizada pelo fabricante e apresentar seu vídeo e controles no próprio Enterprise Cams.** O fabricante autentica; o Enterprise Cams implementa seus conectores e conduz o fluxo de acesso. A primeira implementação abre apenas o aplicativo correspondente e continua identificada como etapa histórica, NÃO entrega final. Ainda não existe integração de vídeo interno de câmera física comprovada, inventário de dispositivos sincronizado nem integração extensível por QR validada.

## Arquitetura funcional a desenvolver

1. **Identificação aberta:** leitura de QR, análise segura de conteúdo, detecção de fabricante/ecossistema e complementação por descoberta quando o conteúdo for insuficiente. Não atrelar a funcionalidade final à lista atual de pacotes Android.
2. **Registro extensível de conectores:** cada fabricante implementa sua identificação, autenticação perante o próprio serviço (sem recriar autoridade), obtenção da lista de dispositivos, seleção correta e operações compatíveis; entrada de novas marcas sem reescrever o aplicativo.
3. **Sessões e mídia:** separar identidade do fabricante, credenciais temporárias, sessão autorizada, dispositivo selecionado, obtenção do vídeo e controles em uma interface estável. Permitir acesso aos serviços de nuvem e assinaturas existentes quando aplicáveis, sem depender de abrir o aplicativo oficial no uso cotidiano.
4. **Segurança como engenharia:** minimizar permissões, evitar vazamento de credenciais, preservar integridade das sessões, verificar origem e tratar falhas de comunicação conforme riscos concretos. Não prometer segurança absoluta nem usar sua impossibilidade como barreira à implementação.
5. **Marco de aceitação:** demonstrar identificação e seleção corretas, autenticação pelo serviço adequado, vídeo REAL dentro do Enterprise Cams e funcionalidades aplicáveis da câmera; registrar testes de código, emulador e hardware com distinções. Preparar partes independentes e testes de contrato enquanto a integração com hardware evolui.

Dificuldades técnicas são complicações a resolver, não bloqueios presumidos nem justificativa para reduzir o aplicativo a launcher. Priorizar código próprio incremental e validação funcional; recorrer a implementações de terceiros apenas como referências técnicas compatíveis com suas licenças e condições.

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
