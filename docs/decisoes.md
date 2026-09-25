# Decisões D-01 a D-08 e refatorações

Registro da versão preparada em 24/09/2026. São decisões verificáveis nesta versão; não afirma que foram demonstradas nos encontros anteriores.

| ID | Escolha, alternativa e consequência |
|---|---|
| D-01 | Swing do JDK. JavaFX exigiria módulos/dependências adicionais; Swing permite uma base menor. Chamadas novas usam SwingWorker por meio de TarefaTela para não bloquear a tela. |
| D-02 | HttpServer do JDK e sessões opacas em memória. Framework de aplicação não é permitido pela orientação de Matheus. JWT acrescentaria protocolo e validação desnecessários para um processo local. Reiniciar exige novo login. Autocadastro não escolhe papel; API verifica permissões. |
| D-03 | H2 embarcado e JDBC explícito. PostgreSQL exigiria serviço separado; ORM esconderia mapeamento e transações. Scripts SQL aditivos reproduzem o esquema. Transações protegem agregados; testes usam banco em memória distinto da base real. |
| D-04 | HTML/CSS/JavaScript nativos e fetch encapsulado em Api. React adicionaria dependências e conceitos fora do foco; páginas estáticas isoladas não atenderiam integração. Site é servido pela API e nunca consulta o banco diretamente. |
| D-05 | ZXing gera PNG e lê imagem enviada; token UUID opaco, cinco minutos de validade, associado a atividade/operação. Um código pode atender vários inscritos durante a janela; cada participante/operação só é registrada uma vez. Usar dados pessoais no QR foi descartado. Câmera em tempo real fica como melhoria do cliente. |
| D-06 | RegrasInscricao é valor imutável; frequência usa Strategy; Pergunta compõe TipoResposta. Escolha de atividade e vagas são configuráveis por evento; capacidade por atividade. Políticas são congeladas ao existir histórico, em vez de implementar versões e recálculos complexos. Questionários são imutáveis e respostas identificadas, sem edição/reenvio. Certificação não entra nesta entrega. |
| D-07 | Strategy resolve a variação de frequência e resposta. Template Method em Endpoint concentra preflight, captura de erros e despacho para executar; os quatro handlers novos especializam as rotas. Alternativa: repetir infraestrutura e condicionais de regras em cada endpoint. Interfaces de repositório separam persistência das regras, mas não são apresentadas como substituto dos dois padrões exigidos. |
| D-08 | JUnit 5, objetos reais e repositório em memória para testar aplicação sem interface; JDBC/H2 para persistência; HTTP em porta temporária para integração. Sem Mockito ou framework de testes web. Base demo é explícita, fictícia e idempotente. Refatorações abaixo e dados controlados permitem explicar efeito e falhas. |

## Refatoração 1 — inscrição sai do handler

**Antes:** `InscricaoHttpHandler.criar` e `validarEscolha` concentravam consultas, vagas e conflitos dentro de HTTP. A validação não conferia o vínculo da atividade ao evento, nem cruzava a agenda de outros eventos. A gravação da inscrição e das escolhas não era atômica.

**Depois:** `InscricaoService` coordena portas, `RegrasInscricao` valida configuração/prazo e `Inscricao` protege o estado cancelado. O handler traduz JSON e chama o caso de uso. O repositório grava inscrição e escolhas na mesma transação. Reserva e validação são serializadas na instância local.

**Alternativa descartada:** criar uma classe de serviço para cada rota ou copiar as verificações para desktop e site. A separação por responsabilidade permite testar e reutilizar sem proliferar classes de transporte.

**Evidência:** `ApiIntegracaoTest` cobre conflito, vínculo entre eventos, vagas, cancelamento, duas requisições concorrentes e preservação após falha; `InscricaoRepositoryJdbcTest` cobre persistência do agregado. Compare o diff desta entrega com a cópia inicial da sessão — parte desse código ainda não havia sido commitada.

## Refatoração 2 — usuários e testes independentes da interface

**Antes:** cadastro/login estavam coordenados no handler; testes JDBC obtinham a URL fixa do banco de demonstração e apagavam tabelas dessa base.

**Depois:** `UsuarioService` recebe `UsuarioRepository` e é testado com implementação em memória em `UsuarioServiceTest`; o handler mantém somente protocolo e criação da sessão. `ConnectionFactory` lê configuração, e Surefire fixa um H2 em memória separado. Scripts SQL substituem DDL embutido no método de conexão.

**Efeito:** caso de uso de cadastro/login/edição executável sem tela, HTTP ou banco; testes não apagam a demonstração. A separação permite validar a inversão de dependência em código pequeno.

**Alternativa descartada:** manter testes dependentes da base local e orientar limpeza manual antes de cada execução. Isso não seria reproduzível nem seguro para os dados de demonstração.

## Refatoração 3 — infraestrutura HTTP compartilhada

`Endpoint.handle` é final e executa preflight, chama `executar` e traduz falhas. Frequência, avaliação, relatórios e regras de inscrição implementam somente suas rotas. O nome do padrão é sustentado por código reutilizado, não por uma classe vazia. Os handlers antigos conservam sua estrutura simples; não houve migração artificial de todo o projeto apenas para uniformizar nomes.
