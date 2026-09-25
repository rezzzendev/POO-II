import {Api} from './api.js';
const api = new Api();
const $ = id => document.getElementById(id);
const mensagem = texto => { $('mensagem').textContent = texto; };
const dados = form => Object.fromEntries(new FormData(form));
let eventos = [], atividades = [], escolherAtividades = true;
// Conteúdo da API entra como texto, nunca como HTML executável.
function elemento(tag, texto) { const e = document.createElement(tag); if (texto !== undefined) e.textContent = texto; return e; }
function acao(id, evento, tarefa) {
  $(id).addEventListener(evento, async e => {
    e.preventDefault();
    const botao = e.currentTarget.matches('button') ? e.currentTarget : e.currentTarget.querySelector('button');
    if (botao) botao.disabled = true;
    try { await tarefa(e); } catch (erro) { mensagem(erro.message); }
    finally { if (botao) botao.disabled = false; }
  });
}
async function carregarEventos() {
  eventos = await api.eventos(); $('eventos').replaceChildren(new Option('Selecione', ''));
  eventos.forEach(e => $('eventos').add(new Option(`${e.titulo} · ${e.status}`, e.id)));
}
async function programacao() {
  const id = Number($('eventos').value); if (!id) return;
  const evento = eventos.find(e => e.id === id);
  $('evento-descricao').textContent = `${evento.descricao || ''} · ${evento.local} · ${evento.inicio} a ${evento.fim} (${evento.fuso})`;
  const regras = await api.enviar(`/regras-inscricao/${id}`); escolherAtividades = regras.escolherAtividades;
  $('regras').textContent = `${escolherAtividades ? 'Escolha suas atividades.' : 'Inscrição no evento inteiro.'} Cancelamento até ${regras.prazoCancelamento} (${evento.fuso}).`;
  const filtros = Object.fromEntries(Object.entries(dados($('filtros'))).filter(([,v]) => v));
  atividades = await api.atividades(id, filtros);
  $('atividades').replaceChildren(); $('atividade-avaliacao').replaceChildren(new Option('Selecione', '')); $('questionarios').replaceChildren();
  for (const a of atividades) {
    const bloco = elemento('div'); bloco.className = 'atividade'; const label = elemento('label');
    const check = elemento('input'); check.type = 'checkbox'; check.value = a.id; check.name = 'atividade'; check.disabled = !escolherAtividades;
    label.append(check, document.createTextNode(` ${a.titulo} · ${a.tipo} · ${a.trilha || ''}`));
    bloco.append(label, elemento('p', `${a.inicio} — ${a.fim} · ${a.local}`));
    const pessoas = await api.enviar(`/atividades/${a.id}/pessoas`);
    if (pessoas.length) bloco.append(elemento('p', pessoas.map(p => `${p.nomePessoa} (${p.papel})`).join(', ')));
    $('atividades').append(bloco); $('atividade-avaliacao').add(new Option(a.titulo, a.id));
  }
  if (!atividades.length) $('atividades').append(elemento('p', 'Nenhuma atividade encontrada para os filtros.'));
}
async function agenda() {
  const inscricoes = await api.enviar('/inscricoes/minhas'); $('inscricoes').replaceChildren();
  for (const i of inscricoes) {
    const linha = elemento('p', `Inscrição ${i.id} · evento ${i.eventoId} · ${i.status} `);
    if (i.status === 'CONFIRMADA') {
      const cancelar = elemento('button', 'Cancelar inscrição');
      cancelar.onclick = async () => { try { await api.cancelar(i.id); mensagem('Inscrição cancelada.'); await agenda(); } catch (e) { mensagem(e.message); } }; linha.append(cancelar);
      const selecionar = elemento('button', 'Usar seleção atual de atividades');
      selecionar.onclick = async () => { try { if(Number($('eventos').value)!==i.eventoId) throw new Error('Selecione a programação deste evento primeiro.');await api.selecionar(i.id, selecionadas());mensagem('Agenda atualizada.');await agenda(); } catch(e){mensagem(e.message);} };linha.append(' ',selecionar);
    }
    $('inscricoes').append(linha);
  }
  $('agenda').replaceChildren(); (await api.agenda()).forEach(a => $('agenda').append(elemento('li', `${a.titulo} · ${a.inicio} — ${a.fim} · ${a.local}`)));
}
function selecionadas() { return [...document.querySelectorAll('input[name=atividade]:checked')].map(e => Number(e.value)); }
acao('login', 'submit', async e => { const d = dados(e.target); const u = await api.entrar(d.email, d.senha); $('usuario').textContent = `${u.nome} (${u.papel})`; mensagem('Login realizado.'); await carregarEventos(); await agenda(); });
acao('cadastro', 'submit', async e => { await api.enviar('/usuarios', 'POST', dados(e.target)); mensagem('Conta criada. Entre com seu e-mail e senha.'); e.target.reset(); });
acao('sair', 'click', async () => { api.sair(); $('usuario').textContent = 'Visitante'; $('inscricoes').replaceChildren(); $('agenda').replaceChildren(); $('atividades').replaceChildren(); $('questionarios').replaceChildren(); await carregarEventos(); mensagem('Você saiu da conta.'); });
acao('eventos', 'change', programacao); acao('filtros', 'submit', programacao);
acao('inscrever', 'click', async () => { const eventoId = Number($('eventos').value); if (!eventoId) throw new Error('Selecione um evento.'); await api.inscrever(eventoId, selecionadas()); mensagem('Inscrição confirmada.'); await agenda(); });
acao('atualizar-agenda', 'click', agenda);
acao('presenca', 'submit', async e => {
  const arquivo = e.target.elements.imagem.files[0]; if (!arquivo || arquivo.size > 1000000) throw new Error('Selecione uma imagem de até 1 MB.');
  const data = await new Promise((ok, falha) => { const leitor = new FileReader(); leitor.onload = () => ok(leitor.result.split(',')[1]); leitor.onerror = falha; leitor.readAsDataURL(arquivo); });
  await api.enviar('/frequencia/qr', 'POST', {imagemBase64: data}); mensagem('Presença registrada.');
});
acao('atividade-avaliacao', 'change', async () => {
  const id = Number($('atividade-avaliacao').value); if (!id) return;
  $('questionarios').replaceChildren();
  for (const q of await api.questionarios(id)) {
    const form = elemento('form'); form.className = 'questionario'; form.append(elemento('h3', q.titulo), elemento('p', q.politicaIdentificacao));
    for (const p of q.perguntas) {
      const label = elemento('label', p.enunciado); let campo;
      if (p.tipo === 'ESCOLHA_UNICA') { campo = elemento('select'); campo.add(new Option('Selecione', '')); p.opcoes.forEach(v => campo.add(new Option(v, v))); }
      else if (p.tipo === 'ESCALA') { campo = elemento('input'); campo.type = 'number'; campo.min = p.minimo; campo.max = p.maximo; campo.step = '1'; }
      else { campo = elemento('textarea'); campo.maxLength = 4000; }
      campo.name = p.id; campo.required = true; label.append(campo); form.append(label);
    }
    const botao = elemento('button', 'Enviar avaliação'); form.append(botao);
    form.onsubmit = async e => { e.preventDefault();botao.disabled=true;try { await api.responder(q.id, dados(form)); mensagem('Avaliação enviada.'); } catch(e){mensagem(e.message);}finally{botao.disabled=false;} };
    $('questionarios').append(form);
  }
});
carregarEventos().catch(e => mensagem(e.message));
if (api.token) api.enviar('/usuarios/me').then(u => { $('usuario').textContent = `${u.nome} (${u.papel})`; }).catch(() => {api.sair();});
