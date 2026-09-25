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

- Login do participante demo e carregamento de 100 atividades.
- Exibição de questionário com texto, escolha e escala.
- Mensagem de impedimento de avaliação sem presença.
- Upload de arquivo PNG do QR, leitura e confirmação de presença.
- Envio de avaliação válida e mensagem ao tentar responder novamente.

O desktop foi compilado, mas não houve inspeção interativa de todas as janelas Swing. Habny precisa ensaiar as operações gráficas e concluir suas telas; Carlos e Matheus Lima precisam finalizar o site e revisar usabilidade/responsividade. A S8 permanece uma entrega do grupo a validar.

## Volume e medição local

Base fictícia: 503 usuários, dos quais 501 participantes; 100 atividades; 501 inscrições no evento demo. Duas cargas consecutivas em banco novo mantiveram 503 usuários, 100 atividades, 501 inscrições e um questionário, sem duplicação. Não são dados reais. O script do roteiro acrescentou outro evento e outro participante em sua própria demonstração.

Resultado de `python3 scripts/medir-api.py --base http://localhost:18080`, três consultas sequenciais por rota:

| Consulta | Registros | Mediana | Maior tempo |
|---|---:|---:|---:|
| Eventos publicados/administrativos | 2 | 2,5 ms | 3,1 ms |
| Atividades do evento demo | 100 | 27,7 ms | 28,6 ms |
| Relatório de inscritos | 501 | 205,5 ms | 249,7 ms |
| Relatório de frequência | 501 | 288,8 ms | 393,6 ms |

Medição local, com banco aquecido, na máquina desta sessão. Não é teste de carga concorrente nem garantia de latência em outro computador. A API usa um processo e tratamento sequencial de requisições para simplificar a consistência.

## Entrega e autoria

Os arquivos `Divisao` e `Evidencias` estão na raiz. O segundo responde S1 a S7 como consolidação técnica atual, com a data de apresentação ao professor explicitamente a confirmar. Nenhuma evidência foi submetida ao portal. Nenhum commit ou push foi realizado nesta sessão; links definitivos precisam apontar para uma versão efetivamente publicada.

O repositório já tinha mudanças locais e arquivos `target/` rastreados antes desta sessão. O `.gitignore` foi atualizado para novos artefatos, mas o histórico/índice Git não foi reescrito. Ao preparar o commit, revisar os arquivos gerados já rastreados; os JARs podem ser reconstruídos pelo Maven.
