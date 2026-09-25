# Contrato da API — versão de 24/09/2026

Base local: `http://localhost:8080`. Requisições com corpo usam `Content-Type: application/json`. Após login, envie `Authorization: Bearer TOKEN`. `GET /eventos` e `/atividades` exibem somente publicados para público/participantes; com token de organização também exibem rascunhos e encerrados.

Respostas: 200 para consultas/alterações, 201 para criações, 204 para remoções; 400 para entrada/regra inválida, 401 sem sessão válida, 403 sem papel autorizado, 404 para rota/recurso não encontrado nos handlers de consulta. Alguns casos de uso retornam recurso inexistente como 400 com mensagem. Erro: `{"erro":"Mensagem compreensível"}`. Não inferir permissão somente pela tela.

## Usuários

| Método e rota | Corpo / retorno | Acesso |
|---|---|---|
| POST `/usuarios` | `{nome,email,senha}` → `{id,nome,email,papel}` | Público; sempre PARTICIPANTE |
| POST `/login` | `{email,senha}` → `{id,nome,email,papel,token}` | Público |
| GET `/usuarios/me` | Perfil autenticado, sem hash | Autenticado |
| PUT `/usuarios/me` | `{nome,email}` → perfil persistido | Próprio usuário |
| PUT `/usuarios/{id}/papel` | `{papel:"ORGANIZADOR"}`; também PARTICIPANTE ou ADMINISTRADOR | Administrador, outro usuário |

Senhas usam PBKDF2 com sal; nunca são devolvidas. E-mail é normalizado e duplicidade é rejeitada. Sessões ficam na memória do processo; reinício exige login novamente.

## Eventos e atividades

| Método e rota | Uso | Acesso |
|---|---|---|
| GET `/eventos` / `/eventos/{id}` | Listar / consultar | Público, respeitando estado |
| POST `/eventos` | Criar rascunho | Organização |
| PUT `/eventos/{id}` | Editar rascunho | Organização |
| POST `/eventos/{id}/publicar` | Publicar; corpo `{}` | Organização |
| POST `/eventos/{id}/encerrar` | Encerrar publicado; corpo `{}` | Organização |
| DELETE `/eventos/{id}` | Remover somente rascunho | Organização |
| GET `/atividades` | Filtros combináveis: eventoId, data, trilha, tipo, local | Público, respeitando estado |
| GET `/atividades/{id}` | Consultar | Público, respeitando estado |
| POST `/atividades` | Criar em evento rascunho | Organização |
| PUT `/atividades/{id}` | Editar em rascunho; todos os campos | Organização |
| DELETE `/atividades/{id}` | Remover em rascunho | Organização |
| GET `/atividades/{id}/pessoas` | Lista `{usuarioId,nomePessoa,papel}` | Público, respeitando estado |
| POST `/atividades/{id}/pessoas` | `{usuarioId,papel:"Palestrante"}` | Organização |

Corpo de evento:

```json
{"titulo":"Semana Java","descricao":"Oficinas e palestras","inicio":"2026-10-01T08:00:00","fim":"2026-10-01T18:00:00","modalidade":"PRESENCIAL","local":"Campus UEG","fuso":"America/Sao_Paulo"}
```

Modalidade: PRESENCIAL, ONLINE ou HIBRIDO. Local/fuso omitidos recebem `A definir` e `America/Sao_Paulo`. O fuso é fixo após criação. Respostas incluem `id`, `status`, local e fuso. Período editado deve continuar abrangendo as atividades existentes.

Corpo de atividade:

```json
{"eventoId":1,"titulo":"Oficina Java","descricao":"Composição de objetos","tipo":"Oficina","trilha":"Programação","local":"Sala 1","inicio":"2026-10-01T09:00:00","fim":"2026-10-01T10:00:00","capacidade":30}
```

Tipo é texto livre. Capacidade omitida/nula significa ilimitada. Programação é montada antes da publicação; conflito de local/horário no mesmo evento é bloqueado. Horários adjacentes não conflitam. Fotografia não é exigida. Pessoas vinculadas usam contas existentes; o ID vem de cadastro ou relatório.

Exemplo de filtros: `/atividades?eventoId=1&data=2026-10-01&trilha=Programa%C3%A7%C3%A3o&tipo=Oficina`.

## Regras, inscrição e agenda

| Método e rota | Uso / corpo | Acesso |
|---|---|---|
| GET `/regras-inscricao/{eventoId}` | Consultar configuração | Público |
| PUT `/regras-inscricao/{eventoId}` | Corpo abaixo; antes da primeira inscrição | Organização |
| POST `/inscricoes` | `{eventoId:1,atividadeIds:[1,2]}` → confirmação | Autenticado, para si |
| GET `/inscricoes/minhas` | Lista com id, eventoId, atividadeIds, status e dataInscricao | Próprio usuário |
| GET `/inscricoes?eventoId=1` | Inscritos do evento | Organização |
| PUT `/inscricoes/{id}/atividades` | `{atividadeIds:[1,2]}` substitui seleção | Titular |
| POST `/inscricoes/{id}/cancelar` | `{}`; respeita prazo e libera vaga | Titular ou organização |
| GET `/agenda` | Lista cronológica com título, local, início/fim, eventoId e fuso | Próprio usuário |

```json
{"escolherAtividades":true,"controlarVagas":true,"prazoCancelamento":"2026-10-01T08:00:00"}
```

Padrão: escolha habilitada, vagas habilitadas, prazo no início do evento. Com `escolherAtividades:false`, inscreva com lista vazia; a inscrição no evento habilita frequência nas atividades, mas a agenda permanece sem escolhas. Conflitos da agenda são bloqueados, inclusive entre eventos de fusos diferentes. Cancelados não podem alterar seleção. O prazo é interpretado no fuso do evento. Uma inscrição ativa por participante/evento.

## Frequência e QR Code

| Método e rota | Uso / corpo | Acesso |
|---|---|---|
| GET `/frequencia/{atividadeId}/politica` | `{politica}` | Autenticado |
| PUT `/frequencia/{atividadeId}/politica` | `{politica:"CHECK_IN"}`; também ENTRADA_SAIDA ou MANUAL | Organização, antes do primeiro registro |
| POST `/frequencia/{atividadeId}/codigos` | `{tipo:"CHECK_IN"}`; ou ENTRADA / SAIDA conforme política | Organização |
| POST `/frequencia/qr` | `{token:"..."}` **ou** `{imagemBase64:"..."}` | Participante autenticado e inscrito |
| POST `/frequencia/{atividadeId}/manual` | `{usuarioId:3,presente:true,justificativa:"Lista conferida"}` | Organização |
| GET `/frequencia/{atividadeId}` | Situação e histórico do usuário autenticado | Próprio usuário |
| GET `/frequencia/{atividadeId}?usuarioId=3` | Situação e histórico do participante indicado | Organização ou próprio usuário |

Gerar código retorna `{token,validade,imagemBase64}`. Imagem é PNG de 300×300; validade em UTC. O cliente exibe `data:image/png;base64,` seguido da imagem ou salva um PNG. O envio da imagem deve conter **apenas o conteúdo Base64**, sem o prefixo data URL. O site já lê o arquivo dessa forma.

QR contém somente token opaco. Válido por cinco minutos; não revela senha ou dados pessoais. Repetir a mesma marcação do participante é rejeitado. Saída exige entrada anterior. Não é obrigatório usar câmera: leitura de uma imagem real do QR cumpre a operação. Não há imposição de janela do horário da atividade além da validade do código emitido pelo organizador; essa simplificação permite demonstração controlada.

Histórico retorna `{id,tipo,origem,responsavelId,instante,justificativa}`. A última correção manual determina a situação, sem remover registros anteriores. `presente:false` registra invalidação manual, não exclusão. Frequência manual e QR exigem inscrição válida.

## Questionários e avaliações

| Método e rota | Uso | Acesso |
|---|---|---|
| POST `/questionarios` | Criar questionário imutável; corpo abaixo | Organização |
| GET `/questionarios?atividadeId=1` | Listar questionários e perguntas | Autenticado |
| GET `/questionarios/{id}` | Consultar perguntas, IDs e política de identificação | Autenticado |
| POST `/questionarios/{id}/respostas` | `{respostas:{"ID_PERGUNTA":"valor"}}` | Inscrito com presença validada |
| GET `/questionarios/{id}/resultados` | total, distribuições, médias de escala, comentários e avaliações identificadas | Organização |

```json
{
  "atividadeId":1,
  "titulo":"Avaliação da oficina",
  "perguntas":[
    {"enunciado":"Comentário","tipo":"TEXTO"},
    {"enunciado":"Recomendaria?","tipo":"ESCOLHA_UNICA","opcoes":["Sim","Não"]},
    {"enunciado":"Nota geral","tipo":"ESCALA","minimo":1,"maximo":5}
  ]
}
```

O servidor atribui IDs às perguntas; use-os nas respostas. Todas são obrigatórias. Escala aceita inteiros nos limites inclusivos; escolha aceita uma das opções; texto tem até 4000 caracteres. Questionário tem 1–50 perguntas. Uma avaliação por usuário/questionário; reenvio é rejeitado. Não há edição de questionário nem resposta nesta versão; para novo instrumento crie outro questionário.

A resposta de consulta inclui `politicaIdentificacao`, que deve ser exibida antes do envio: respostas identificadas, acessíveis à organização, não publicadas no site. Correções futuras de presença não apagam avaliações já submetidas; o instante da avaliação permanece registrado.

## Relatórios

`GET /relatorios/inscritos?eventoId=1` e `GET /relatorios/frequencia?eventoId=1`, somente organização. Aceitam `atividadeId` para filtrar. JSON retorna `total`, `confirmados`, `presentes`, `linhas`. No relatório de frequência as totalizações contam relações participante/atividade, não pessoas distintas.

Linha: `{usuarioId,nome,email,inscricao,atividadeId,atividade,presente,marcacoes}`. O histórico detalhado por participante está em `/frequencia/{atividadeId}?usuarioId=...`. Para CSV, acrescente `&formato=csv`; separador ponto e vírgula, UTF-8 com BOM, aspas escapadas. Salve o corpo em arquivo; não o interprete como JSON. Campos de texto com prefixo de fórmula são protegidos para abertura em planilhas.

## Demonstração executável

Veja [scripts/demo-api.py](../scripts/demo-api.py): login, publicação, inscrição, agenda, leitura do QR, correção manual, avaliação, erro de duplicidade, consolidação e CSV. Pode ser adaptado para coleção Postman, mas não exige Postman nem ferramenta adicional além de Python 3.
