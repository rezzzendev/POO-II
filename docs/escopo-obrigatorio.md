# Escopo obrigatório do projeto

Este projeto implementa somente o núcleo marcado como **M** na especificação revisada.

## O que precisa existir

1. **Conta e acesso:** cadastro, login, permissões e edição do próprio perfil.
2. **Eventos e programação:** evento, atividade, tipo, trilha, local, horário, conflito e pessoas vinculadas.
3. **Site público:** eventos publicados, programação, cadastro e início da inscrição.
4. **Inscrição e agenda:** regras configuráveis, vagas, cancelamento, escolha de atividades e conflito de horário.
5. **Frequência:** política por atividade, QR Code, alternativa manual e cálculo da presença.
6. **Avaliação:** questionário com texto, escolha única e escala; somente inscrito presente responde uma vez; resultados consolidados.
7. **Relatórios:** inscritos, frequência e exportação CSV.
8. **POO e arquitetura:** domínio com regras, polimorfismo real, interfaces de repositório, API e persistência separadas, testes e documentação.

## Como o desktop foi organizado

O desktop do organizador segue a ordem natural da demonstração:

1. **Evento:** criar, editar, configurar inscrição, publicar e encerrar.
2. **Programação e frequência:** cadastrar atividades e pessoas, escolher a política de presença, gerar QR Code e lançar presença manual.
3. **Avaliações:** criar questionários e consultar resultados.
4. **Relatórios:** consultar inscritos/frequência e exportar CSV.

A gestão de usuários aparece fora das etapas e somente para o administrador, pois não pertence a um evento específico. A exclusão de rascunhos foi mantida para permitir corrigir cadastros antes da publicação.

## Divisão das interfaces

- **Desktop:** administrador e organizador criam e operam eventos, atividades, frequência, questionários e relatórios.
- **Web:** visitante consulta a programação; participante entra, inscreve-se, organiza a agenda, registra presença e responde avaliações.
- **API:** reúne os casos de uso, verifica permissões e acessa o banco para as duas interfaces.

O site usa uma navegação de página única feita com JavaScript puro: a navbar mostra uma seção de cada vez e atualiza a URL com `#programacao`, `#conta` etc. Isso não é um framework e não replica regras da API.

## O que ficou fora

- Certificados de participante ou palestrante (RF-32 a RF-35, desejáveis).
- Envio de certificados por e-mail.
- Lista de espera, rede social, notificações e análises avançadas (opcionais).
- Recuperação de senha, login social, MFA, pagamentos e infraestrutura de produção (fora do escopo).

Não se deve remover API, banco, testes ou classes de domínio apenas para diminuir a quantidade de arquivos: a separação entre domínio, aplicação e adaptadores é exigida pelos RNF-01 e ROO-09.
