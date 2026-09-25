# Preparação das 16 evidências do JAVA8

Estes são textos-base da versão atual, **não comprovação de que houve apresentação nos encontros passados**. Substitua os campos entre colchetes com fatos reais antes de enviar. O portal não foi alterado. Os horários/prazos finais continuam a confirmar com o professor.

Para S1–S8 o campo é “Observação do grupo”. Para 9–16 é “Link”, com observação opcional. A avaliação é por grupo, com contribuições individuais identificadas corretamente.

## 1 — S1: domínio, responsabilidades e backlog

> Demonstração: [como e quando realmente ocorreu]. Versão: [commit ou link conferível]. A versão atual inicia a API e dispõe de base Swing e site. O glossário, as responsabilidades dos objetos e a arquitetura estão em docs/arquitetura.md; a distribuição das próximas tarefas está em docs/equipe.md. Caminho válido: iniciar a aplicação e consultar eventos publicados. Erro: tentativa de criar evento sem título. Teste: EventoTest. Decisão de POO: Evento protege seu período e suas mudanças de estado. Pendência: [situação real do encontro]; próxima meta: [meta assumida].

## 2 — S2: objetos, invariantes e persistência

> Demonstração: [data/formato real]. Versão: [commit]. Evento e atividade são persistidos com JDBC/H2. Caminho válido: criar e consultar evento/atividade após nova conexão. Erro: período inválido ou conflito de local/horário. Testes: EventoTest, AtividadeTest e repositórios JDBC. Decisão: validações e comportamento no domínio; persistência implementa portas. Os testes atuais usam banco em memória separado da demonstração. Pendências/próxima meta: [registro real].

## 3 — S3: casos de uso, portas e autenticação

> Demonstração: [data/formato real]. Versão: [commit]. Cadastro, login, edição de perfil e autorização são suportados pela API. Caminho válido: cadastro e login com senha correta. Erro: e-mail duplicado, senha incorreta ou ação administrativa por participante. UsuarioServiceTest executa o caso de uso com repositório em memória, sem HTTP ou tela. Decisão: dependência de UsuarioRepository, autocadastro sempre como participante e hash protegido. Pendências/próxima meta: [registro real].

## 4 — S4: adaptadores, site público e inscrição

> Demonstração: [data/formato real]. Versão: [commit]. Swing e JavaScript consomem a mesma API e banco. Caminho válido: criar/publicar evento na organização, consultar no site e inscrever participante. Erro: segunda inscrição ativa no mesmo evento. Evidência técnica: ApiIntegracaoTest e scripts/demo-api.py. Decisão: mensagens JSON ficam nos adaptadores; inscrição é coordenada em InscricaoService. Nesta entrega as interfaces são bases; as telas restantes estão atribuídas em docs/equipe.md. Pendências/próxima meta: [situação real].

## 5 — S5: políticas de inscrição e agenda

> Demonstração: [data/formato real]. Versão: [commit]. A API configura seleção, controle de vagas e prazo de cancelamento. Agenda deriva das escolhas válidas, com bloqueio de conflitos. Testes cobrem última vaga concorrente, cancelamento, atividade de outro evento e seleção conflitante. RegrasInscricao é um objeto de valor; o ponto de variação polimórfico da versão atual pode ser demonstrado nas estratégias de frequência. Não afirmar que essas estratégias já existiam na S5 sem evidência histórica. Pendências/próxima meta: [registro real].

## 6 — S6: frequência configurável

> Demonstração: [data/formato real]. Versão: [commit]. Foram implementadas políticas de check-in único, entrada/saída e validação manual, geração e leitura real de QR por imagem e correções manuais com autoria. Caminho válido: inscrito envia o PNG do QR e obtém presença; organizador pode corrigir com justificativa preservando o histórico. Erros: QR expirado, duplicidade, saída sem entrada e ação manual sem autorização. Testes: FrequenciaTest e ApiIntegracaoTest. Decisão: Strategy por composição em Frequencia. Pendências/próxima meta: [registro real].

## 7 — S7: avaliações, relatórios e refatoração

> Demonstração: [data/formato real]. Versão: [commit]. A API oferece questionários com texto, escolha única e escala, exige inscrição/presença e impede reenvio. Retorna consolidações e relatórios exportáveis em CSV. Caminho válido: validar presença e responder questionário. Erro: avaliar sem presença ou reenviar. Testes: QuestionarioTest e fluxo HTTP de ponta a ponta. Decisões: Pergunta compõe TipoResposta; Endpoint usa Template Method. Refatorações: regras de inscrição extraídas do handler, caso de uso de usuário isolado e testes separados do banco real, documentadas em docs/decisoes.md. Pendência atual: Habny completar editor/resultados no desktop e Carlos e Matheus Lima finalizar os fluxos e a usabilidade do site. Próxima meta: integrar as telas e ensaiar CA-01 a CA-07.

## 8 — S8: integração, qualidade e entrega final

**Rascunho para usar somente após terminar a integração da equipe:**

> Demonstração final: [data confirmada e formato]. Versão final: [commit]. Os cenários CA-01 a CA-07 foram executados em [interfaces efetivamente verificadas]. Testes executados: [comando e resultado]. ROO-01 a ROO-12 rastreados em docs/rastreabilidade.md. Foram explicados Strategy, Template Method, invariantes e refatorações. Limitações: [listar as reais]. Contribuições: [descrever o que cada integrante de fato fez].

Hoje a API e as bases dos clientes estão preparadas; **não registrar S8 como entrega integral concluída enquanto faltarem as telas e a validação conjunta**.

## 9–16 — links para artefatos

O remoto configurado é `https://github.com/rezzzendev/POO-II`. As alterações desta sessão ainda precisam ser commitadas e publicadas para que o docente consiga abri-las. Use o SHA real do commit publicado no lugar de `COMMIT`; não cole estes modelos literalmente no portal.

| Item | Link após publicação | Observação sugerida |
|---|---|---|
| 9 Código/histórico | `https://github.com/rezzzendev/POO-II/tree/COMMIT` | Versão [SHA]. Histórico real de contribuições; autoria inicial informada por Matheus. |
| 10 README | `https://github.com/rezzzendev/POO-II/blob/COMMIT/README.md` | Pré-requisitos, execução de API/site/desktop, contas demo e testes. |
| 11 Modelo/arquitetura | `https://github.com/rezzzendev/POO-II/blob/COMMIT/docs/arquitetura.md` | Glossário, responsabilidades, composição e dependências. |
| 12 Decisões/refatorações | `https://github.com/rezzzendev/POO-II/blob/COMMIT/docs/decisoes.md` | D-01 a D-08, Strategy/Template Method e comparações antes/depois. |
| 13 Banco/demonstração | `https://github.com/rezzzendev/POO-II/tree/COMMIT/src/main/resources/db` | Migrações SQL. Dados em src/main/java/DadosDemonstracao.java; executar --demo conforme README. |
| 14 API | `https://github.com/rezzzendev/POO-II/blob/COMMIT/docs/api.md` | Contrato, exemplos, permissões e roteiro executável. |
| 15 Testes | `https://github.com/rezzzendev/POO-II/tree/COMMIT/src/test/java` | Executar mvn test; resultados e limites em docs/validacao.md. |
| 16 Rastreabilidade | `https://github.com/rezzzendev/POO-II/blob/COMMIT/docs/rastreabilidade.md` | Prioridades M/D/P preservadas; status de API e clientes separados. |
