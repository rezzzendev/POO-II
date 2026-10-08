# Plataforma de Gestão de Eventos - JAVA8

Projeto integrador de POO II desenvolvido em **Java 21, sem framework de aplicação**.

- **Desktop:** administração e organização dos eventos.
- **Site:** acesso de visitantes e participantes.
- **API:** regras de negócio, autorização e integração das interfaces.
- **PostgreSQL:** banco compartilhado pelo desktop e pelo site por meio da API.

## O que está implementado

O projeto cobre o núcleo obrigatório RF-01 a RF-31 da especificação:

- cadastro, login, permissões e edição do perfil;
- criação, edição, publicação e encerramento de eventos;
- programação com atividades, tipos, trilhas, locais, horários, conflitos e pessoas vinculadas;
- inscrição configurável, controle de vagas, cancelamento e agenda pessoal;
- frequência por QR Code, entrada/saída, check-in único e lançamento manual;
- questionários com texto, escolha única e escala numérica;
- relatórios de inscrições e frequência com exportação CSV;
- API e banco compartilhados entre desktop e site.

Certificados (RF-32 a RF-35) são desejáveis e a extensão social (RF-36) é opcional, portanto não fazem parte desta versão.

## Iniciar API, site e banco

### Pré-requisito

Instale e abra o **Docker Desktop**.

### Primeira execução

Abra o PowerShell na pasta do projeto e execute:

```powershell
docker compose up --build -d
```

Esse único comando:

1. inicia o PostgreSQL;
2. compila e testa a API com Java 21;
3. cria ou atualiza as tabelas;
4. prepara os dados de demonstração;
5. inicia a API e o site.

Abra o site em [http://localhost:8080](http://localhost:8080).

Para conferir os contêineres e acompanhar o servidor:

```powershell
docker compose ps
docker compose logs -f servidor
```

## Abrir o desktop

O Docker mantém o servidor e o banco, mas não abre a janela Swing do Windows. Para o desktop também são necessários **JDK 21** e **Maven** instalados.

Com o Docker ligado, abra outro PowerShell na pasta do projeto:

```powershell
mvn -f desktop/pom.xml clean package
java -jar desktop/target/desktop-1.0-SNAPSHOT.jar
```

Use o desktop para contas de administrador e organizador. Visitantes e participantes usam o site.

## Depois de alterar o código

Para alterações na API, no site, no banco ou no seed:

```powershell
docker compose up --build -d
```

Para alterações somente no desktop:

```powershell
mvn -f desktop/pom.xml clean package
java -jar desktop/target/desktop-1.0-SNAPSHOT.jar
```

## Parar ou reiniciar os dados

Parar a aplicação sem apagar o banco:

```powershell
docker compose down
```

Apagar o banco de demonstração e recriá-lo do zero:

```powershell
docker compose down -v
docker compose up --build -d
```

> `docker compose down -v` apaga o volume PostgreSQL e todos os dados cadastrados localmente.

## Contas de demonstração

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@demo.local` | `Demo123!` |
| Organizador | `organizador@demo.local` | `Demo123!` |
| Participante | `participante@demo.local` | `Demo123!` |

O autocadastro do site sempre cria uma conta de participante. O seed é idempotente e prepara **10 eventos, 500 participantes e 100 atividades, distribuídas em 10 atividades por evento**.

## Como testar o fluxo principal

1. Entre no desktop como organizador e consulte ou crie um evento.
2. Cadastre atividades, configure inscrição e publique o evento.
3. Abra o site como participante, faça a inscrição e escolha as atividades.
4. No desktop, gere o QR Code da atividade.
5. No site, use a câmera para ler o QR e registrar a presença.
6. Responda à avaliação no site.
7. Consulte e exporte os relatórios pelo desktop.

A câmera do navegador funciona em `localhost` ou em uma conexão HTTPS. O QR contém apenas um token temporário, sem senha ou dado pessoal.

## Tecnologias utilizadas

| Parte | Tecnologia | Uso |
|---|---|---|
| Linguagem principal | Java 21 | Domínio, casos de uso, API e desktop |
| API HTTP | `HttpServer` do JDK | Endpoints REST sem framework |
| Desktop | Java Swing | Interface de administrador e organizador |
| Site | HTML, CSS e JavaScript | Interface de visitante e participante |
| Comunicação | Fetch API e `HttpClient` | Consumo da mesma API pelas duas interfaces |
| Banco | PostgreSQL 17 | Persistência da aplicação |
| Acesso ao banco | JDBC | SQL e transações explícitas |
| Contêineres | Docker e Docker Compose | Execução reproduzível da API e do banco |
| Build | Maven | Compilação e execução dos testes Java |
| JSON | `org.json` | Leitura e escrita das mensagens da API |
| QR Code | ZXing e `BarcodeDetector` do navegador | Geração e leitura dos códigos |
| Testes | JUnit 5 e H2 em memória | Testes de domínio, aplicação, JDBC e HTTP |
| Senhas | PBKDF2 com HMAC-SHA-256 | Hash com sal aleatório |

Swing, `HttpServer`, JDBC, `HttpClient`, HTML, CSS e JavaScript são usados diretamente. Não há Spring, Hibernate, React ou outro framework de aplicação.

## Arquitetura

O código segue portas e adaptadores:

```text
Desktop Swing ----\
                   > HTTP API -> aplicação -> domínio
Site web ---------/                 |
                                    v
                              portas de repositório
                                    |
                                    v
                             adaptadores JDBC -> PostgreSQL
```

As regras ficam no domínio e nos casos de uso. Handlers HTTP, telas e repositórios apenas adaptam entrada, saída e persistência.

## Documentação

- [Contrato da API](docs/api.md)
- [Arquitetura e modelo de domínio](docs/arquitetura.md)
