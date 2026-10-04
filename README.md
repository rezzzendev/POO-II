# Plataforma de Gestão de Eventos — JAVA8

Projeto de POO II em **Java 21, sem framework de aplicação**. API com `HttpServer`, banco H2 via JDBC, desktop Swing e site HTML/CSS/JavaScript. As regras novas ficam em objetos de domínio e casos de uso; as telas consomem a mesma API.

## Executar em outro computador

Instale JDK 21 ou superior e Maven 3.9+. A primeira compilação precisa de internet para baixar bibliotecas. Execute os comandos a partir da raiz deste repositório:

```sh
mvn clean package
java -jar target/gestao-eventos-1.0-SNAPSHOT.jar --demo
java -jar target/gestao-eventos-1.0-SNAPSHOT.jar
```

Abra **http://localhost:8080**. O site é servido pela própria API. Para desenvolvimento, após compilar, também pode usar `mvn exec:java`.

O comando `--demo` cria uma base fictícia com **503 usuários (501 participantes), 100 atividades, 501 inscrições, pessoas vinculadas e um questionário** em banco novo. Não apaga a base existente; repetir não duplica a carga. Use os dados demo apenas para apresentação local. As datas são relativas ao primeiro dia de criação da base; se a apresentação ocorrer após o prazo do evento, crie uma base separada como abaixo.

| Perfil | E-mail | Senha de demonstração |
|---|---|---|
| Administrador | admin@demo.local | Demo123! |
| Organizador | organizador@demo.local | Demo123! |
| Participante | participante@demo.local | Demo123! |

A carga é explícita, não ocorre ao iniciar a API. O autocadastro público sempre cria participante. O administrador pode alterar o papel de outros usuários pela API.

No cadastro, informe nome, e-mail no formato `nome@dominio.com` e senha com pelo menos 8 caracteres, incluindo letras e números. E-mail duplicado ou dados inválidos são recusados pela API com uma mensagem exibida pelo desktop e pelo site.

Para registrar frequência no site, entre com a conta do participante inscrito e use **Ler QR Code com a câmera**. O navegador pedirá permissão de câmera. Use um navegador compatível com o leitor nativo de QR; o site envia somente o token lido para a API. O organizador gera e exibe o código no desktop.

### Desktop

Mantenha a API em execução e, em outro terminal:

```sh
mvn -f desktop/pom.xml clean package
java -jar desktop/target/desktop-1.0-SNAPSHOT.jar
```

O desktop precisa de ambiente gráfico. Entre com `organizador@demo.local`. Após o login, os recursos ficam em uma janela Swing única: a lista de eventos fica à esquerda e as abas à direita separam gestão do evento, programação/presença, questionários/resultados, relatórios e papéis. É possível criar/editar eventos em rascunho, administrar atividades e vínculos por ID, publicar, encerrar, configurar inscrições e frequência, gerar QR, lançar presença e consultar/exportar CSV. A API decide as permissões; a tela não concede papéis por conta própria.

### Configuração e banco

O padrão é `./data/eventos.mv.db`, usuário/senha locais `eventos`. As migrações são executadas na primeira conexão, sem apagar registros. Para uma demonstração independente:

```sh
java '-Ddb.url=jdbc:h2:file:./data/apresentacao;DB_CLOSE_DELAY=-1' -jar target/gestao-eventos-1.0-SNAPSHOT.jar --demo
java '-Ddb.url=jdbc:h2:file:./data/apresentacao;DB_CLOSE_DELAY=-1' -Dapi.port=8081 -jar target/gestao-eventos-1.0-SNAPSHOT.jar
java -Dapi.url=http://localhost:8081 -jar desktop/target/desktop-1.0-SNAPSHOT.jar
```

Execute somente uma API por base H2. O padrão de rede é `127.0.0.1`; `-Dapi.host=0.0.0.0` permite acesso pela rede local de demonstração. Sessões são locais ao processo e deixam de valer ao reiniciar. Não há infraestrutura de produção, HTTPS ou recuperação de senha neste recorte.

## Testes e demonstração

```sh
mvn test
python3 scripts/demo-api.py
python3 scripts/medir-api.py
```

Os testes usam **H2 em memória**, sem apagar `data/`. Incluem domínio, persistência, caso de uso com repositório em memória e HTTP real em portas temporárias. O script demonstra a API contra o servidor iniciado e exige a conta demo de organizador; cria um evento e uma conta fictícia próprios. O CSV fica em `target/demo-api.csv`.

## Onde continuar

- [Divisao — responsabilidades a partir desta entrega](Divisao)
- [Evidencias — textos para S1 a S7](Evidencias)
- [Explicacao — guia para quem está começando](Explicacao)
- [Divisão e pendências detalhadas](docs/equipe.md)
- [Contrato da API e exemplos](docs/api.md)
- [Domínio e arquitetura](docs/arquitetura.md)
- [Decisões D-01 a D-08 e refatorações](docs/decisoes.md)
- [Matriz RF, RN, RNF, ROO e cenários](docs/rastreabilidade.md)
- [Textos para as 16 evidências](docs/evidencias.md)
- [Validação desta entrega](docs/validacao.md)
- [Tecnologias vigentes](docs/tecnologias.md)

## Limites declarados

A API cobre o núcleo obrigatório, com testes de domínio, persistência e HTTP. As telas Swing estão organizadas em uma janela única e o site tem layout responsivo; ambas consomem a mesma API. A conferência manual da janela em ambiente gráfico, os fluxos web com uma conta real e o QR com câmera física ainda precisam ser ensaiados pela equipe. Consulte a divisão detalhada antes da apresentação final.

Políticas de inscrição ficam bloqueadas após a primeira inscrição; frequência após o primeiro registro; questionários são imutáveis após criação. Programação é montada em rascunho e bloqueada após publicação. Isso preserva o histórico com uma implementação simples. Correções manuais de frequência permanecem auditáveis. Relatórios refletem o estado atual; avaliações já enviadas permanecem registradas mesmo após correção posterior de presença.

Certificados (RF-32 a RF-35) e extensão social (RF-36) não foram implementados, pois não substituem o núcleo obrigatório. A equipe deve conseguir explicar o código, inclusive as alterações produzidas com assistência de IA.

## Responsáveis pela continuação

Matheus Rezende: API. Habny: desktop. Carlos: web público, conta/perfil e visual. Matheus Lima: web do participante (inscrição, agenda, presença e avaliação). A lista de tarefas e critérios de conclusão está em [Divisao](Divisao).
