# Matriz de rastreabilidade — 24/09/2026

**Escopo desta entrega:** backend e bases dos clientes. “Disponível na API” não significa que todas as telas finais foram concluídas ou que a demonstração já foi realizada diante do professor. Ver [equipe](equipe.md) e [validação](validacao.md).

Legenda: **M** obrigatório, **D** desejável, **P** opcional. Fontes: especificação versão 1.1, 10/08/2026, e cronograma do portal.

## RF e evidência

| RF | Prioridade | Responsabilidade / implementação | Verificação ou roteiro | Estado nesta entrega |
|---|---|---|---|---|
| 01 Cadastro | M | Usuario, UsuarioService, UsuarioRepositoryJdbc | UsuarioServiceTest, UsuarioTest, ApiIntegracaoTest | API e site; cadastro desktop existente |
| 02 Login/permissões | M | UsuarioService, Autenticador, SessaoStore, UsuarioHttpHandler | ApiIntegracaoTest.autorizacaoRascunhoEMalformedJson | API e clientes; gerenciamento de papéis por API |
| 03 Perfil | M | Usuario.editarPerfil, UsuarioService | UsuarioServiceTest | API; tela a cargo de Carlos |
| 04 Eventos | M | Evento, EventoHttpHandler, EventoRepositoryJdbc | EventoTest, EventoRepositoryJdbcTest, roteiro CA-01 | API; desktop base, edição completa pendente |
| 05 Tipos abertos | M | Atividade, ProgramacaoService | AtividadeTest; criação no roteiro | API e base desktop |
| 06 Trilhas/espaços | M | AtividadeRepository.buscar | AtividadeRepositoryJdbcTest; filtros no site | API e site |
| 07 Conflito programação | M | Atividade.conflitaCom, ProgramacaoService | AtividadeTest; erro de conflito antes de publicar | API; validar tela final |
| 08 Pessoas vinculadas | M | VinculoPessoa, AtividadeRepositoryJdbc | AtividadeRepositoryJdbcTest; seed com palestrante | API e exibição web; editor desktop pendente |
| 09 Filtros combináveis | M | AtividadeRepository.buscar, AtividadeHttpHandler | AtividadeRepositoryJdbcTest; data/trilha/tipo/local no site | API e site |
| 10 Página pública | M | SiteHttpHandler, web | ApiIntegracaoTest e navegador | Base funcional |
| 11 Programação/pessoas | M | web/app.js, /atividades e /pessoas | Seed e navegador; foto não exigida | Base funcional |
| 12 Inscrição pelo site | M | web/Api.inscrever, InscricaoService | ApiIntegracaoTest; roteiro + revisão da interface | Base funcional |
| 13 Configurar inscrição | M | RegrasInscricao, InscricaoService.configurar | ApiIntegracaoTest.inscricaoSomenteEventoEFrequenciaManual | API; editor desktop pendente |
| 14 Vagas | M | Atividade.temVagaDisponivel, InscricaoService | Teste HTTP da última vaga concorrente | API |
| 15 Cancelamento | M | RegrasInscricao, Inscricao.cancelar | ApiIntegracaoTest.cancelamentoNoPrazoLiberaVaga | API e site |
| 16 Seleção | M | InscricaoService.selecionar | InscricaoTest, ApiIntegracaoTest | API e site |
| 17 Agenda cronológica | M | AgendaHttpHandler | ApiIntegracaoTest; script CA-03 | API e site |
| 18 Conflito agenda | M | InscricaoService.validar | ApiIntegracaoTest.vagasConflitoVinculoCancelamentoEAtomicidade | API; erro mostrado no cliente |
| 19 Política frequência | M | PoliticaFrequencia e três implementações | FrequenciaTest | API e desktop |
| 20 Gerar QR | M | CodigoFrequencia, QrCode, FrequenciaService | Fluxo HTTP e script geram PNG real | API e desktop |
| 21 Ler QR | M | QrCode.ler, FrequenciaService | HTTP lê imagem; teste de navegador envia arquivo | API e site |
| 22 Lançamento manual | M | RegistroFrequencia, FrequenciaService.manual | Teste de correções com autoria/histórico | API e desktop |
| 23 Calcular presença | M | Frequencia + Strategy | FrequenciaTest: check-in, entrada/saída, manual | API |
| 24 Questionário | M | Questionario, AvaliacaoService, AvaliacaoRepositoryJdbc | QuestionarioTest, ApiIntegracaoTest | API; editor desktop pendente |
| 25 Tipos de resposta | M | Pergunta, Texto, EscolhaUnica, Escala | QuestionarioTest; persistência HTTP dos três tipos | API e formulário web |
| 26 Elegibilidade | M | AvaliacaoService, FrequenciaService | Bloqueio sem presença no HTTP e navegador | API e site |
| 27 Duplicidade avaliação | M | Restrição UNIQUE de avaliacoes | API e navegador bloqueiam reenvio | API |
| 28 Consolidação | M | AvaliacaoService.resumo / AvaliacaoHttpHandler | Script e teste HTTP, média/distribuição/comentário | API; tela desktop pendente |
| 29 Relatório inscritos | M | RelatorioService.inscritos | ApiIntegracaoTest; seed de 501 inscritos | API; tela completa pendente |
| 30 Frequência/participação | M | RelatorioService.frequencia, histórico de frequência | Script CA-07 e teste HTTP | API; exportação desktop, consulta visual pendente |
| 31 Exportação | M | RelatorioHttpHandler | Script salva CSV fora do sistema | API e desktop |
| 32 Elegibilidade certificado | D | Fora do recorte desta entrega | Não testado | Não implementado |
| 33 Certificado PDF | D | Fora do recorte desta entrega | Não testado | Não implementado |
| 34 Envio por e-mail | D | Fora do recorte desta entrega | Não testado | Não implementado |
| 35 Certificado palestrante | D | Fora do recorte desta entrega | Não testado | Não implementado |
| 36 Extensão social | P | Fora do recorte desta entrega | Não testado | Não implementado |

## ROO

| ROO | Evidência concreta |
|---|---|
| 01 Modelo de domínio | Evento, Atividade, Inscricao, Frequencia e Questionario; glossário em arquitetura.md |
| 02 Encapsulamento | Transições publicar/encerrar/cancelar; edição de seleção cancelada rejeitada; listas protegidas |
| 03 Objetos de valor | RegrasInscricao, Pergunta, Escala, CodigoFrequencia e RegistroFrequencia imutáveis |
| 04 Composição | Pergunta compõe TipoResposta; Frequencia compõe PoliticaFrequencia |
| 05 Polimorfismo | Três estratégias de frequência e três validadores de resposta; testes específicos |
| 06 Herança | Sem herança artificial de entidades; Endpoint usado como Template Method, relação substituível de handler |
| 07 Interfaces | Repositórios como portas e contratos de políticas; UsuarioServiceTest substitui persistência |
| 08 SOLID | Casos de uso dependem de interfaces; refatoração de inscrição e usuário documentadas |
| 09 Arquitetura | Domínio/aplicação independentes de HTTP/JDBC/JSON; composição explícita em ServidorApi |
| 10 Padrões | Strategy e Template Method, problema/alternativa/efeito em decisoes.md |
| 11 Erros | Exceções de domínio e RegraViolada; tradução para mensagem sem detalhes de banco |
| 12 Testes/refatorações | Testes de comportamento, persistência e integração; comparações antes/depois em decisoes.md |

## RN e RNF

- RN-01/02/03: e-mail único, foto opcional e atividades simultâneas em locais distintos.
- RN-04/05/06/07/08: estado público/inscrição, configuração, capacidade, política uniforme de conflito e agenda derivada.
- RN-09/10/11/12: estratégia por atividade, token sem dados pessoais, marcações UTC com origem/responsável, correções aditivas.
- RN-13/14/15: inscrição/presença para avaliar, unicidade no banco e política identificada retornada/exibida antes do envio.
- RN-16: certificado não implementado (D).
- RN-17: regras bloqueadas ao existir histórico, questionários imutáveis, programação bloqueada após publicar; correções não apagam avaliações.
- RN-18/19/20: organização global autorizada na API, banco compartilhado e fuso do evento; instantes de frequência em UTC.
- RNF-01/02/03/04: Java puro, portas/adaptadores, dois clientes HTTP e esquema SQL reproduzível.
- RNF-05/06/07/08: hash, permissões no servidor, validação e política de identificação. Melhorias de mensagens/telas continuam com os responsáveis dos clientes.
- RNF-09: base funcional; avaliação final de usabilidade depende da equipe concluir e ensaiar interfaces.
- RNF-10: seed de pelo menos 500 participantes e 100 atividades; medição local em validacao.md, sem alegação de benchmark de produção.
- RNF-11/12: duplicidades bloqueadas, transações e testes de comportamento.
- RNF-13/14/15: nomes por responsabilidade, falhas internas registradas por classe sem dados sensíveis, README reproduzível.

## CA-01 a CA-07

`scripts/demo-api.py` executa os sete fluxos pela API. `ApiIntegracaoTest` cobre autorização, persistência, regras e falhas. O teste de navegador cobre login, programação, avaliação inelegível, upload/leitura QR e avaliação válida.

A demonstração final do grupo deve completar os fluxos usando as **telas**, especialmente os editores e consolidações pendentes no desktop. Compilar o Swing não equivale a validar manualmente todas as interações gráficas. CA-08 é desejável e não foi implementado.
