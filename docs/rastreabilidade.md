# Matriz de rastreabilidade

Legenda de prioridade: **M** obrigatório, **D** desejável, **P** opcional.

Como conferir:
- Testes: `mvn test` (pasta `src/test/java`, usa H2 em memória).
- Fluxos pela API: `python3 scripts/demo-api.py` com a API rodando (executa CA-01 a CA-07).
- Telas: site em `http://localhost:8080` (participante) e desktop (organizador/admin). Contas no README.

## Requisitos funcionais (RF)

| RF | Prio. | Onde está no código | Como verificar | Status |
|---|---|---|---|---|
| 01 Cadastro | M | `Usuario`, `UsuarioService.cadastrar`, `UsuarioRepositoryJdbc` | `UsuarioTest`, `UsuarioRepositoryJdbcTest.emailDuplicadoEhRejeitado`, `ApiIntegracaoTest.cadastroRetornaMensagensClarasParaEmailESenhaInvalidos` | Feito (site) |
| 02 Login e permissões | M | `UsuarioService.autenticar`, `SessaoStore`, `Autenticador` | `ApiIntegracaoTest.autorizacaoRascunhoEMalformedJson` (401/403) | Feito (site e desktop) |
| 03 Editar perfil | M | `Usuario.editarPerfil`, `UsuarioService.editar` | `UsuarioServiceTest.cadastroLoginEPerfilSemInterface` | Feito (site) |
| 04 Eventos | M | `Evento` (`publicar`, `encerrar`, `editar`), `EventoService` | `EventoTest`, `EventoRepositoryJdbcTest`, CA-01 | Feito (desktop) |
| 05 Tipos de atividade | M | `Atividade` (tipo é texto livre) | `AtividadeTest.tipoEObrigatorio` | Feito (desktop) |
| 06 Trilhas e locais | M | `Atividade`, `AtividadeRepository` | `AtividadeRepositoryJdbcTest.buscaComFiltrosCombinaveis` | Feito (desktop e site) |
| 07 Conflito de local/horário | M | `Atividade.conflitaCom`, `AtividadeService` | `AtividadeTest.mesmoLocalComHorarioSobrepostoConflita` | Feito (desktop) |
| 08 Pessoas vinculadas | M | `VinculoPessoa`, `AtividadeRepositoryJdbc` | `AtividadeRepositoryJdbcTest.vinculaEListaPessoas` | Feito (desktop vincula, site exibe) |
| 09 Filtros da programação | M | `AtividadeRepository`, `AtividadeHttpHandler` | `AtividadeRepositoryJdbcTest.buscaComFiltrosCombinaveis` | Feito (site: data, trilha, tipo, local) |
| 10 Página pública | M | `SiteHttpHandler`, pasta `web/` | Abrir o site sem login | Feito |
| 11 Programação e pessoas no site | M | `web/app.js`, `/atividades`, `/atividades/{id}/pessoas` | Abrir o site; foto não é obrigatória | Feito |
| 12 Inscrição pelo site | M | `InscricaoService.inscrever` | `ApiIntegracaoTest`, CA-02 | Feito (site) |
| 13 Regras de inscrição | M | `RegrasInscricao`, `InscricaoService.configurar` | `ApiIntegracaoTest.inscricaoSomenteEventoEFrequenciaManual` | Feito (desktop) |
| 14 Controle de vagas | M | `Atividade.temVagaDisponivel`, `InscricaoService` | `AtividadeTest.comCapacidadeVagaFechaQuandoAtinjeLimite`, `ApiIntegracaoTest.duasRequisicoesSimultaneasNaoExcedemCapacidade` | Feito |
| 15 Cancelamento | M | `Inscricao.cancelar`, `RegrasInscricao` | `InscricaoTest.naoCancelaDuasVezes`, `ApiIntegracaoTest.cancelamentoNoPrazoLiberaVaga` | Feito (site) |
| 16 Escolher atividades | M | `Inscricao.definirAtividades`, `InscricaoService.selecionar` | `InscricaoTest.definirAtividadesSubstituiEscolha` | Feito (site) |
| 17 Agenda pessoal | M | `AgendaHttpHandler` | CA-03 no `demo-api.py` | Feito (site) |
| 18 Conflito na agenda | M | `InscricaoService` (validação) | `ApiIntegracaoTest.vagasConflitoVinculoCancelamentoEAtomicidade` | Feito |
| 19 Critério de frequência | M | `PoliticaFrequencia` + `CheckInUnico`, `EntradaSaida`, `ValidacaoManual` | `FrequenciaTest` | Feito (desktop) |
| 20 Gerar QR Code | M | `CodigoFrequencia`, `QrCode`, `FrequenciaService` | `QrCodeTest.geraQrQuePodeSerLidoSemAlterarToken` | Feito (desktop) |
| 21 Ler QR Code | M | jsQR no site, `FrequenciaService.registrarQr` | `ApiIntegracaoTest.qrExpiradoNaoRegistra`, `FrequenciaTest.checkInValidaEDuplicidadeFalha` | Feito (site, pela câmera) |
| 22 Lançamento manual | M | `RegistroFrequencia`, `FrequenciaService.manual` | `FrequenciaTest.manualNaoAceitaQrEExigeJustificativa`, CA-05 | Feito (desktop) |
| 23 Calcular presença | M | `Frequencia` + Strategy | `FrequenciaTest.entradaExigeSaidaPosterior`, `ultimaCorrecaoPrevaleceSemApagarHistorico` | Feito |
| 24 Criar questionário | M | `Questionario`, `AvaliacaoService.criar` | `QuestionarioTest.configuracaoProtegidaEImutavel` | Feito (desktop) |
| 25 Tipos de resposta | M | `Pergunta`, `Texto`, `EscolhaUnica`, `Escala` | `QuestionarioTest.validaTresTiposPorPolimorfismo` | Feito (site responde) |
| 26 Só avalia quem estava presente | M | `AvaliacaoService.responder` | `ApiIntegracaoTest.fluxoCompletoQrManualQuestionarioRelatorio` | Feito |
| 27 Não avaliar duas vezes | M | `UNIQUE(questionario_id, usuario_id)` no banco | `ApiIntegracaoTest.fluxoCompletoQrManualQuestionarioRelatorio` | Feito |
| 28 Resultado das avaliações | M | `AvaliacaoService.resumo` | CA-06 no `demo-api.py` | Feito (desktop) |
| 29 Relatório de inscritos | M | `RelatorioService.inscritos` | CA-07 no `demo-api.py` | Feito (desktop) |
| 30 Relatório de frequência | M | `RelatorioService.frequencia` | CA-07 no `demo-api.py` | Feito (desktop) |
| 31 Exportar relatório | M | `RelatorioHttpHandler` (CSV) | `demo-api.py` salva o CSV | Feito (desktop) |
| 32 Elegibilidade de certificado | D | — | — | Não feito |
| 33 Certificado em PDF | D | — | — | Não feito |
| 34 Envio por e-mail | D | — | — | Não feito |
| 35 Certificado de palestrante | D | — | — | Não feito |
| 36 Rede social interna | P | — | — | Não feito |

## Requisitos de orientação a objetos (ROO)

| ROO | Onde aparece | Status |
|---|---|---|
| 01 Modelo de domínio | `Evento`, `Atividade`, `Inscricao`, `Frequencia`, `Questionario`, `Usuario` (ver `arquitetura.md`) | Feito |
| 02 Encapsulamento | Entidades se validam no construtor; `Evento.publicar/encerrar` só em estado certo; lista de atividades da inscrição não pode ser alterada por fora (`InscricaoTest.listaDeAtividadesEImutavelParaQuemLe`) | Feito |
| 03 Objetos de valor | `RegrasInscricao`, `CodigoFrequencia`, `RegistroFrequencia`, `Pergunta` | Feito |
| 04 Composição | `Frequencia` usa uma `PoliticaFrequencia`; `Pergunta` usa um `TipoResposta` | Feito |
| 05 Polimorfismo | 3 políticas de frequência e 3 tipos de resposta (`FrequenciaTest`, `QuestionarioTest`) | Feito |
| 06 Herança | Só em `Endpoint` (handlers herdam o tratamento comum). Não criamos subclasse para cada tipo de atividade porque não mudava comportamento | Feito |
| 07 Interfaces | Repositórios (`UsuarioRepository` etc.), `PoliticaFrequencia`, `TipoResposta` | Feito |
| 08 SOLID | Services dependem de interfaces, não do JDBC; cada handler só cuida de HTTP (ver `decisoes.md`) | Feito |
| 09 Arquitetura hexagonal | Pastas `domain`, `application`, `adapter/in`, `adapter/out`; montagem em `ServidorApi` | Feito |
| 10 Padrões de projeto | Strategy e Template Method (ver `decisoes.md`) | Feito |
| 11 Erros do domínio | `RegraViolada` e exceções por entidade; `Endpoint` transforma em mensagem 400 | Feito |
| 12 Testes e refatoração | 78 testes em `src/test/java`; duas refatorações em `decisoes.md` | Feito |

## Regras de negócio (RN) e não funcionais (RNF)

- **RN-01** e-mail único: `UNIQUE` no banco + `UsuarioRepositoryJdbcTest.emailDuplicadoEhRejeitado`.
- **RN-02** foto opcional: não existe campo obrigatório de foto.
- **RN-06** vagas: `ApiIntegracaoTest.duasRequisicoesSimultaneasNaoExcedemCapacidade`.
- **RN-10** QR sem dados pessoais: o QR guarda só um código aleatório que vale 5 minutos.
- **RN-12/RN-17** histórico: correção manual não apaga registro (`ultimaCorrecaoPrevaleceSemApagarHistorico`); regras de inscrição e política de frequência travam depois do primeiro uso.
- **RN-13/RN-14** avaliação: só com presença, uma vez por pessoa.
- **RN-18** só admin/organizador altera evento: `Autenticador` na API (403 para participante).
- **RN-19 / RNF-02** mesmo banco e API: site e desktop chamam a mesma API (`ServidorApi`).
- **RN-20** fuso: cada evento guarda seu fuso (`Evento.definirLocalEFuso`).
- **RNF-01** arquitetura hexagonal (ROO-09).
- **RNF-04** banco: scripts em `src/main/resources/db`.
- **RNF-05** senha com hash PBKDF2 (`SenhaHasher`, `UsuarioTest.senhaNuncaFicaGuardadaEmTextoPuro`).
- **RNF-06** permissão checada no servidor (`ApiIntegracaoTest.autorizacaoRascunhoEMalformedJson`).
- **RNF-10** volume: `--demo` cria 500 participantes, 10 eventos e 100 atividades (`DadosDemonstracao`).
- **RNF-11** sem duplicidade: inscrição duplicada, presença repetida e avaliação repetida são bloqueadas.
- **RNF-12** testes sem interface: ver ROO-12.
- **RNF-15** como rodar: README.

## Cenários CA-01 a CA-07

| CA | O que mostrar | Pela tela | Pela API / teste |
|---|---|---|---|
| 01 Publicação | Organizador cria evento e atividades, publica e o evento aparece no site | Desktop → site | `demo-api.py` |
| 02 Inscrição | Visitante se cadastra, se inscreve e vê a confirmação | Site | `demo-api.py`, `ApiIntegracaoTest` |
| 03 Agenda | Participante escolhe atividades, conflito é bloqueado, agenda em ordem | Site | `demo-api.py`, `vagasConflitoVinculoCancelamentoEAtomicidade` |
| 04 Frequência QR | Organizador gera QR, participante lê e a presença é calculada | Desktop → site | `demo-api.py`, `qrExpiradoNaoRegistra` |
| 05 Manual | Organizador lança/corrige presença com justificativa e autoria | Desktop | `demo-api.py`, `FrequenciaTest` |
| 06 Avaliação | Participante presente responde; quem não estava é bloqueado | Site | `demo-api.py`, `fluxoCompletoQrManualQuestionarioRelatorio` |
| 07 Relatório | Organizador consulta e exporta CSV | Desktop | `demo-api.py` |
| 08 Certificado (D) | — | — | Não feito |
