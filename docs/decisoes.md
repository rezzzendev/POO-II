# Decisões e refatorações

## Decisões técnicas (D-01 a D-08)

| ID | O que escolhemos | Por que / alternativa |
|---|---|---|
| D-01 | Desktop em **Java Swing** | Já vem no JDK. JavaFX precisaria de dependências extras. As chamadas para a API rodam em segundo plano (`TarefaTela`, com `SwingWorker`) para a tela não travar. |
| D-02 | API com o **HttpServer do JDK** e login por token guardado em memória | Não usamos framework (Spring) para mostrar as camadas na mão. JWT seria complicado demais para um sistema local. A API confere o papel do usuário em toda ação protegida (`Autenticador`). O cadastro sempre cria participante; organizador só existe se o admin promover. |
| D-03 | **PostgreSQL** com **JDBC** puro e scripts SQL (`db/001-inicial.sql`, `002-politicas.sql`) | Com ORM (Hibernate) não daria para ver o SQL e o mapeamento. O banco sobe com Docker Compose. |
| D-04 | Site em **HTML, CSS e JavaScript** puros | React seria mais coisa para aprender fora do foco da matéria. O site só conversa com a API, nunca direto com o banco. |
| D-05 | **QR Code**: a API gera a imagem com ZXing e o site lê pela câmera com **jsQR** | O QR guarda só um código aleatório que vale 5 minutos, e não dados pessoais. Cada pessoa só consegue marcar presença uma vez por código. |
| D-06 | **Regras configuráveis**: inscrição por evento, vagas por atividade, política de frequência por atividade | Depois que alguém se inscreve ou marca presença, a regra trava, para não mudar o histórico. A alternativa seria guardar versões das regras, o que é bem mais complexo. |
| D-07 | **Padrões**: Strategy e Template Method | Explicados abaixo. |
| D-08 | **Testes** com JUnit 5 e H2 em memória | Os testes não usam o banco da demo, então não apagam os dados. Também testamos o caso de uso sem tela e sem banco (`UsuarioServiceTest`). |

## Padrões de projeto

**Strategy (frequência).** Cada atividade pode contar a presença de um jeito: check-in único, entrada e saída, ou validação manual. Em vez de um `if` gigante, criamos a interface `PoliticaFrequencia`, com uma classe para cada jeito (`CheckInUnico`, `EntradaSaida`, `ValidacaoManual`). A atividade guarda a política escolhida e só chama `presente(...)`. Para criar um jeito novo, basta criar uma classe nova. O mesmo vale para os tipos de resposta do questionário (`Texto`, `EscolhaUnica`, `Escala`).

**Template Method (API).** Todos os handlers da API precisavam fazer as mesmas coisas: responder o CORS e transformar erro em status HTTP (400, 401, 403, 404). A classe `Endpoint` faz essa parte no método `handle()`, e cada handler só escreve o próprio `executar()` com suas rotas. Sem isso, teríamos o mesmo `try/catch` copiado em todos os handlers.

## Refatoração 1: a regra de inscrição saiu do handler

**Problema:** no começo, o `InscricaoHttpHandler` fazia tudo: lia o JSON, consultava o banco, conferia vagas e conflito de horário e salvava. A regra de negócio ficava misturada com HTTP, não dava para testar sem subir a API e tinha falhas. Por exemplo, não verificava se a atividade era mesmo daquele evento.

**O que fizemos:** criamos o `InscricaoService`, que cuida das regras (vagas, conflito, prazo), e deixamos a classe `Inscricao` protegendo o próprio estado (inscrição cancelada não pode mudar). O handler ficou só com a parte de receber o JSON e devolver a resposta.

**Alternativa que não usamos:** copiar as validações para o desktop e para o site. Assim a regra ficaria repetida em três lugares.

**Resultado:** a regra fica em um lugar só e vale tanto para o site quanto para o desktop. O `ApiIntegracaoTest` testa vagas, conflito, cancelamento e duas inscrições ao mesmo tempo.

## Refatoração 2: cadastro/login e testes sem depender da tela e do banco

**Problema:** o cadastro e o login também estavam dentro do handler. Os testes usavam o mesmo banco da demonstração e apagavam as tabelas dele toda vez que rodavam.

**O que fizemos:** criamos o `UsuarioService`, que recebe a interface `UsuarioRepository` em vez da classe JDBC. Assim, no teste dá para passar um repositório em memória (`UsuarioServiceTest`). A `ConnectionFactory` passou a ler o endereço do banco por configuração, e o Maven roda os testes num H2 em memória separado.

**Alternativa que não usamos:** continuar testando no banco da demo e limpar os dados manualmente antes de cada teste.

**Resultado:** os testes rodam sem tela, sem API e sem PostgreSQL, e não estragam mais os dados da demonstração.
