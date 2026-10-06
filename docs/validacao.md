# Validação da entrega — 24/09/2026

## Compilação e testes

Ambiente: Linux, OpenJDK 25.0.2, compilação direcionada a Java 21, Maven. A execução específica em JDK 21 deve ser conferida no computador da equipe; não foi usada uma VM Java 21 nesta sessão.

- API: `mvn package`, **72 testes, zero falhas, zero erros, zero ignorados** na rodada final.
- Desktop: `mvn -f desktop/pom.xml package`, compilação e JAR concluídos.
- Verificação de alterações: `git diff --check` sem erros.
- Execução da API com H2 em arquivo separado em `/tmp`; a base original em `data/` não foi usada nos testes.

A suíte cobre domínio, repositórios JDBC, cadastro/login com porta em memória, contratos HTTP, permissões, capacidade concorrente, cancelamento, vínculo da atividade, entrada/saída, correções, elegibilidade, questionários, resultados e CSV. Inclui rollback de inscrição/escolhas quando uma gravação falha.

Foi detectada uma falha intermitente na detecção geométrica de QR. O leitor passou a tentar detecção reforçada e, se necessário, leitura direta de código digital. `QrCodeTest` gera e lê **100 tokens determinísticos distintos** para evitar depender de um único desenho QR; também rejeita arquivo que não é imagem.

## Demonstração da API

`scripts/demo-api.py --base http://localhost:18080` executou com sucesso:

1. Publicação e consulta pública de evento.
2. Cadastro, inscrição e rejeição de duplicidade.
3. Agenda derivada da inscrição.
4. Geração de PNG QR, leitura da imagem, cálculo de presença e bloqueio de repetição.
5. Invalidação e validação manuais, mantendo os três registros e autoria.
6. Avaliação dos três tipos, bloqueio sem presença e reenvio, consolidação.
7. Relatório coerente e exportação de `target/demo-api.csv`.

Os cenários adicionais de conflito, vagas e expiração estão nos testes JUnit. O script representa demonstração técnica pela API; não substitui a apresentação das interfaces finais ao docente.

## Navegador

Chrome em modo headless acessou o site servido pela API local. Foi verificado nos formulários reais:

- Login do participante demo e carregamento da programação pública.
- Exibição de questionário com texto, escolha e escala.
- Mensagem de impedimento de avaliação sem presença.
- Upload de arquivo PNG do QR, leitura e confirmação de presença.
- Envio de avaliação válida e mensagem ao tentar responder novamente.

O desktop foi compilado, mas não houve inspeção interativa de todas as janelas Swing. A tela de atividades agora inclui criação de questionários e consulta de resultados, ainda pendentes de conferência manual. A equipe também precisa revisar os fluxos e a usabilidade/responsividade do site. A S8 permanece uma entrega do grupo a validar.

## Volume da base de demonstração

Base fictícia atual: 502 usuários, dos quais 500 participantes; 10 eventos; 100 atividades, distribuídas em 10 por evento; e 500 inscrições confirmadas no primeiro evento. A carga é idempotente e não duplica os registros ao reiniciar. Não são dados reais. O script do roteiro pode acrescentar outro evento e outro participante em sua própria demonstração.

O script `scripts/medir-api.py` permite repetir uma medição local. Os números registrados em versões anteriores foram removidos porque correspondiam ao seed antigo; eles não representam a base atual nem são uma garantia de latência em outro computador. A API usa um processo e tratamento sequencial de requisições para simplificar a consistência.

## Entrega e autoria

Os arquivos `Divisao` e `Evidencias` estão na raiz. O segundo responde S1 a S7 como consolidação técnica atual, com a data de apresentação ao professor explicitamente a confirmar. Nenhuma evidência foi submetida ao portal. Nenhum commit ou push foi realizado nesta sessão; links definitivos precisam apontar para uma versão efetivamente publicada.

O repositório já tinha mudanças locais e arquivos `target/` rastreados antes desta sessão. O `.gitignore` foi atualizado para novos artefatos, mas o histórico/índice Git não foi reescrito. Ao preparar o commit, revisar os arquivos gerados já rastreados; os JARs podem ser reconstruídos pelo Maven.

## Revalidação da API — 27/09/2026

Após a validação anterior, `mvn test` e `mvn package` foram executados novamente: **72 testes passaram, sem falhas ou erros**. O roteiro `python3 scripts/demo-api.py --base http://localhost:18080` também foi executado contra uma base H2 temporária em `/tmp`; CA-01 a CA-07 passaram, incluindo inscrição duplicada, agenda, QR, correção manual, avaliação, relatório e CSV. Essa execução confirma os fluxos da API; não substitui a demonstração das interfaces do grupo.

## Validação do cadastro — 28/09/2026

O cadastro agora rejeita e-mail fora do formato esperado e senha com menos de 8 caracteres ou sem letras e números. A API retorna mensagens específicas; o desktop exibe a resposta e usa uma mensagem padrão se a API enviar corpo vazio ou inesperado. `mvn test` passou com **76 testes**, incluindo os novos casos de validação; `mvn -f desktop/pom.xml package` compilou o cliente Swing. O formulário web também informa o critério da senha. A conferência interativa dos formulários ainda depende de abrir as telas em ambiente gráfico.

## Leitura de QR no site

O site usa `BarcodeDetector` em navegadores compatíveis e envia somente o token à API. A leitura é feita pela câmera física e exige `localhost` ou HTTPS. O teste automatizado decodifica o PNG gerado para conferir o conteúdo, mas a câmera ainda precisa ser conferida manualmente.

## Administração Swing — 04/10/2026

O desktop passou a oferecer criação e edição de evento em rascunho com local/fuso, edição e remoção de atividade, vínculo de pessoa pelo ID de conta, edição das regras de inscrição e consulta/exportação CSV dos relatórios de inscritos e frequência. A configuração continua sendo validada pela API; por exemplo, a API recusa edição de evento publicado e alteração das regras após a primeira inscrição.

`mvn -f desktop/pom.xml clean package` compilou o cliente, e `mvn test` passou com **75 testes**, sem falhas. `git diff --check` também passou. A janela Swing não foi inspecionada interativamente nesta validação; a equipe ainda precisa conferir os diálogos em ambiente gráfico e executar os fluxos completos CA-01 a CA-07 pelas telas.

## Perfil no site — 04/10/2026

O site agora consulta `GET /usuarios/me` após login/restauração da sessão e permite alterar nome e e-mail por `PUT /usuarios/me`. O formulário não pede nem altera senha; validação de perfil permanece na API. A suíte HTTP da API passou e cobre as rotas de perfil. O Chrome headless carregou a página e o módulo JavaScript; como o teste abriu o arquivo diretamente, sem a API, a chamada de rede falhou como esperado. Ainda falta conferir o fluxo interativamente no navegador servido pela aplicação com uma conta de demonstração. `git diff --check` passou.

## Fluxos do participante na web — 04/10/2026

A seleção de atividades agora permanece ao aplicar filtros; ao entrar, a agenda da conta preenche as escolhas já salvas. A página diferencia inscrição nova de alteração de agenda, desabilita operações pessoais para visitantes, confirma cancelamento e formata horários com o fuso do evento. Após enviar uma avaliação, o formulário bloqueia novo envio; erros de presença, prazo, conflito e duplicidade continuam vindo da API e são apresentados na mensagem da página. `mvn test` passou com 75 testes e `git diff --check` passou. O Chrome headless carregou o HTML e o módulo; a interação real com API, conta e câmera continua pendente de ensaio.

## Questionário no site — 04/10/2026

O formulário agora destaca a política de identificação retornada pela API e informa, por pergunta, se a resposta é texto (até 4000 caracteres), escolha única ou escala (inteiro e limites inclusivos). Antes de enviar, valida respostas vazias, tipo, faixa e opção escolhida; a API continua aplicando as regras definitivas. A conferência interativa com questionário real e conta elegível ainda está pendente.

## Painel desktop e revisão visual do site — 04/10/2026

O desktop agora mantém os recursos administrativos em uma janela Swing, com lista de eventos e abas para gestão/regras, programação/presença, questionários/resultados, relatórios e alteração de papéis. A tela de papéis usa o endpoint administrativo existente; a API impede ações sem permissão e autoalteração. Login e cadastro também usam `TarefaTela`, fora da thread gráfica. `mvn -f desktop/pom.xml clean package` compilou com sucesso. O site recebeu navegação por seções, controles agrupados, foco visível por teclado e layout responsivo; o Chrome carregou o site servido pela API com banco demo isolado e as imagens renderizadas em 1365 px e 390 px foram conferidas. A suíte final da API passou com 75 testes. Ainda falta a conferência manual da janela Swing, login e fluxos de escrita com uma conta real, e QR com câmera física.

## Paridade funcional entre web e desktop — 06/10/2026

O site passou a liberar uma área de gestão para ORGANIZADOR e ADMINISTRADOR. Ela cobre cadastro e transição de eventos, regras de inscrição, programação, vínculos de pessoas, política e lançamentos de frequência, geração e download de QR, questionários/resultados, relatórios/CSV e, para administrador, alteração de papéis. A autorização continua na API; a interface apenas apresenta os controles permitidos pelo perfil autenticado.

## Separação final das interfaces — 06/10/2026

A paridade visual anterior foi substituída pela divisão solicitada: administrador e organizador usam somente o desktop; visitante e participante usam o site. A gestão foi removida do HTML, JavaScript e cliente HTTP do web, mas os endpoints e casos de uso permanecem na API para o desktop. O site agora mostra uma seção por vez pela navbar, sem framework, e rejeita login de conta administrativa com orientação para abrir o desktop. O Swing recebeu a mesma paleta azul do site por uma classe de tema centralizada.

`node --check` validou os dois arquivos JavaScript. A compilação Java não pôde ser repetida neste ambiente porque `JAVA_HOME` não está configurado; é necessário executar `mvn -f desktop/pom.xml test` após instalar/configurar o JDK 21.

## Operações de conta por e-mail — 06/10/2026

O desktop deixou de solicitar IDs internos nas duas operações que dependem de uma conta. O vínculo de palestrante/apresentador/responsável e a alteração de papel agora recebem o e-mail único da pessoa. A API resolve o e-mail para o ID internamente; a rota antiga por ID foi mantida somente para compatibilidade e não aparece na interface.

## PostgreSQL e Docker Compose — 06/10/2026

A execução de demonstração foi migrada de H2 em arquivo para PostgreSQL 17 em container. O Compose define banco com volume persistente e healthcheck, e a API depende do banco saudável antes de iniciar. O driver PostgreSQL passou a integrar o JAR; `ConnectionFactory` recebe URL, usuário e senha por propriedades. Os comandos `MERGE` específicos do H2 foram substituídos por atualização/inserção JDBC portável, e as identidades do esquema usam sintaxe aceita por PostgreSQL e H2. H2 permanece somente nos testes automatizados em memória.

A configuração foi revisada estaticamente, mas o Docker não estava disponível no processo do agente para executar `docker compose config` ou o build. A validação final deve ser feita com `docker compose up --build -d`, seguida de `docker compose ps` e `docker compose logs servidor`.

`web/app.js` e `web/api.js` passaram na verificação sintática do Node. A interface foi carregada com respostas locais controladas e conferida nos estados sem evento, com evento em rascunho e com atividade selecionada; não houve erro no console. Java e Maven não estavam disponíveis no ambiente desta revisão, então a suíte Java não foi repetida nesta data. Os fluxos de escrita completos e a câmera física ainda devem ser ensaiados após instalar o JDK 21 e o Maven.

## Revisão contra a especificação e seed de 10 eventos — 06/10/2026

Conferidos os RF-01 a RF-31 obrigatórios, os RNF, os ROO e os entregáveis da especificação. Não foi identificada funcionalidade obrigatória ausente no escopo de código/documentação. Certificados (RF-32 a RF-35) são desejáveis e a extensão social (RF-36) é opcional. A demonstração final pelas janelas Swing e a leitura por câmera física continuam como ensaios da equipe.

O seed foi atualizado para 10 eventos, 500 participantes e 100 atividades, com 10 atividades por evento. `docker compose build servidor` concluiu; os containers foram iniciados e a API confirmou os 10 eventos e a distribuição 10 × 10. O README foi reescrito com instruções de Docker, abertura separada do desktop, contas demo, tecnologias e arquitetura. A cópia atual não inclui `.git`, então o histórico e as contribuições do repositório oficial precisam ser conferidos nele.
