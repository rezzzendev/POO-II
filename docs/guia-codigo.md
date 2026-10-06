# Guia simples do código

Este projeto usa Java puro, sem Spring. Por isso algumas tarefas que um framework faria
automaticamente aparecem de forma explícita no código: abrir rotas HTTP, converter JSON, criar
dependências e executar SQL.

## O padrão usado

Todos os módulos principais seguem este fluxo:

```text
Handler HTTP → Service → Repository → implementação JDBC → PostgreSQL
                    ↓
                 Domínio
```

| Peça | Pergunta que responde | Exemplo |
|---|---|---|
| Entidade de domínio | Quais dados e regras este conceito possui? | `Atividade` |
| Service | Como executar um caso de uso? | `AtividadeService` |
| Repository | Quais operações de persistência a aplicação precisa? | `AtividadeRepository` |
| Repository JDBC | Como essas operações são feitas no PostgreSQL? | `AtividadeRepositoryJdbc` |
| Handler | Como receber e responder HTTP/JSON? | `AtividadeHttpHandler` |

### Repository e Service não fazem a mesma coisa

`AtividadeRepository` é uma **interface**. Ela só declara ações de persistência, como salvar,
buscar e remover. Ela não sabe HTTP e não decide regras de programação.

`AtividadeService` executa os **casos de uso**. Ele verifica se o evento está em rascunho, se a
atividade cabe no período e se existe conflito de sala e horário. Depois pede ao repository para
salvar.

`AtividadeRepositoryJdbc` é quem contém SQL e implementa a interface `AtividadeRepository`.

```text
AtividadeService ──usa──> AtividadeRepository <──implementa── AtividadeRepositoryJdbc
```

Antes, o serviço se chamava `ProgramacaoService`, apesar de trabalhar com `Atividade`. Ele foi
renomeado para `AtividadeService` para seguir o mesmo padrão de `EventoService`, `UsuarioService`,
`InscricaoService`, `FrequenciaService` e `AvaliacaoService`.

`RelatorioService` não possui `RelatorioRepository` porque relatório não é uma entidade salva em
uma tabela própria: ele consulta dados que já existem nos repositories de inscrição, atividade,
evento e frequência.

## Organização dos adaptadores de persistência

Cada repository JDBC fica na pasta do assunto correspondente:

```text
adapter/out/persistence/
├── atividade/AtividadeRepositoryJdbc.java
├── avaliacao/AvaliacaoRepositoryJdbc.java
├── evento/EventoRepositoryJdbc.java
├── frequencia/FrequenciaRepositoryJdbc.java
├── inscricao/InscricaoRepositoryJdbc.java
└── usuario/UsuarioRepositoryJdbc.java
```

Na raiz ficam somente utilitários compartilhados por todos eles: `ConnectionFactory`, `Sql` e
`PersistenciaException`.

## Exceções do domínio

`RegraViolada` é a base comum para falhas esperadas de negócio. Algumas entidades maiores usam
nomes mais específicos, como `EventoInvalidoException` e `AtividadeInvalidaException`, para deixar
testes e mensagens de erro mais precisos. Objetos menores de avaliação e frequência lançam
`RegraViolada` diretamente porque nenhum código precisa distinguir uma categoria própria.

Não é necessário criar uma exceção para cada classe. A regra é: crie uma exceção específica apenas
quando o programa ou os testes precisam identificar aquela categoria; caso contrário, use a base
comum. O `Endpoint` converte todas as subclasses de `RegraViolada` em resposta HTTP 400.

## Exemplo: criar uma atividade

1. O web ou desktop envia `POST /atividades` com JSON.
2. `AtividadeHttpHandler` lê o JSON e confirma que o usuário é organizador ou administrador.
3. `AtividadeService.criar` procura o evento e cria uma `Atividade`.
4. A entidade valida título, tipo, local, datas e capacidade.
5. O service verifica período do evento e conflitos de programação.
6. `AtividadeRepository` recebe o pedido de persistência.
7. `AtividadeRepositoryJdbc` executa o SQL no PostgreSQL.
8. O handler converte o resultado para JSON e responde HTTP 201.

## Por que o construtor de Atividade tem muitos parâmetros?

Uma atividade realmente possui vários dados: título, descrição, tipo, trilha, local, início, fim,
capacidade e evento. O JDBC também precisa reconstruir o objeto quando lê uma linha do banco.

A sintaxe é Java válido:

```java
public Atividade(Long id, String titulo, ..., Evento evento) {
    this.id = id;
    this.evento = evento;
    aplicarDados(...);
}
```

`this.titulo = titulo` significa: guardar o parâmetro `titulo` no atributo `titulo` do objeto
atual. O método de fábrica `Atividade.nova(...)` usa `id = null`, pois o banco gera o ID. A edição
reutiliza `aplicarDados(...)`, evitando repetir validação e atribuições.

É possível criar um Builder para esconder os parâmetros, mas isso acrescentaria várias classes e
métodos. Para o tamanho deste projeto acadêmico, a forma explícita é mais fácil de rastrear.

## Onde cada regra fica

- Regra interna de um único objeto: entidade em `domain`.
- Regra que combina entidades ou repositories: service em `application`.
- SQL e reconstrução de objetos: `adapter/out/persistence`.
- HTTP, rota, autenticação e JSON: handler em `adapter/in/api`.
- Composição das dependências: `ServidorApi`.

## O que explicar ao professor

> Web e desktop consomem a mesma API. O handler traduz HTTP e JSON. O service executa o caso de
> uso e coordena as regras. As entidades protegem seu próprio estado. O repository é uma interface
> que desacopla a aplicação do banco, e a implementação JDBC executa SQL no PostgreSQL. Como não há
> framework, o ServidorApi instancia e conecta essas classes manualmente.

Essa separação não tem como objetivo produzir a menor quantidade possível de linhas. O objetivo é
evitar regras duplicadas nas duas telas e permitir testar o domínio sem iniciar servidor ou banco.
