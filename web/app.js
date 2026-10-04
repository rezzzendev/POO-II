import {Api} from './api.js';
const api = new Api();
const $ = id => document.getElementById(id);
const mensagem = texto => { $('mensagem').textContent = texto; };
const dados = form => Object.fromEntries(new FormData(form));
let eventos = [], atividades = [], inscricoes = [], escolherAtividades = true;
let selecoesPorEvento = new Map();
let sequenciaProgramacao = 0;
let eventoProgramacaoAtual = null;
let camera, detector, cameraAtiva = false;
// Conteúdo da API entra como texto, nunca como HTML executável.
function elemento(tag, texto) { const e = document.createElement(tag); if (texto !== undefined) e.textContent = texto; return e; }
function exibirPerfil(usuario) {
  $('usuario').textContent = `${usuario.nome} (${usuario.papel})`;
  $('sair').disabled = false;
  $('sair').hidden = false;
  $('perfil-form').elements.nome.value = usuario.nome;
  $('perfil-form').elements.email.value = usuario.email;
  $('perfil-section').hidden = false;
  $('login').hidden = true;
  $('cadastro-container').hidden = true;
  $('inscrever').disabled = false;
  $('abrir-camera').disabled = false;
  $('atualizar-agenda').disabled = false;
  $('atividade-avaliacao').disabled = false;
}
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
  if (!eventos.length) $('evento-descricao').textContent = 'Ainda não há eventos publicados.';
}
function formatarData(valor) {
  const partes = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})/.exec(valor || '');
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]} ${partes[4]}:${partes[5]}` : (valor || '');
}
function inscricaoConfirmada(eventoId) {
  return inscricoes.find(i => i.eventoId === eventoId && i.status === 'CONFIRMADA');
}
function selecaoDoEvento(eventoId) {
  if (!selecoesPorEvento.has(eventoId)) {
    const inscricao = inscricaoConfirmada(eventoId);
    selecoesPorEvento.set(eventoId, new Set(inscricao?.atividadeIds || []));
  }
  return selecoesPorEvento.get(eventoId);
}
async function programacao() {
  const chamada = ++sequenciaProgramacao;
  const id = Number($('eventos').value);
  if (!id) {
    eventoProgramacaoAtual = null;
    $('evento-descricao').textContent = '';
    $('regras').textContent = '';
    $('selecao-resumo').textContent = '';
    $('atividades').replaceChildren();
    $('atividade-avaliacao').replaceChildren(new Option('Selecione uma atividade na programação', ''));
    $('questionarios').replaceChildren();
    $('inscrever').hidden = true;
    return;
  }
  const evento = eventos.find(e => e.id === id);
  if (!evento) throw new Error('O evento selecionado não está mais disponível. Atualize a página.');
  const mudouEvento = eventoProgramacaoAtual !== id;
  const avaliacaoAtual = mudouEvento ? null : $('atividade-avaliacao').selectedOptions[0];
  const atividadeAvaliacaoId = avaliacaoAtual?.value || '';
  $('evento-descricao').textContent = `${evento.descricao || ''} · ${evento.local} · ${formatarData(evento.inicio)} a ${formatarData(evento.fim)} (${evento.fuso})`;
  const regras = await api.regrasInscricao(id); escolherAtividades = regras.escolherAtividades;
  if (chamada !== sequenciaProgramacao) return;
  const inscricao = inscricaoConfirmada(id);
  $('regras').textContent = `${escolherAtividades ? 'Escolha suas atividades.' : 'A inscrição vale para o evento inteiro.'} Cancelamento até ${formatarData(regras.prazoCancelamento)} (${evento.fuso}).`;
  const filtros = Object.fromEntries(Object.entries(dados($('filtros'))).filter(([,v]) => v));
  atividades = await api.atividades(id, filtros);
  if (chamada !== sequenciaProgramacao) return;
  eventoProgramacaoAtual = id;
  $('atividades').replaceChildren();
  $('atividade-avaliacao').replaceChildren(new Option('Selecione', ''));
  if (mudouEvento) $('questionarios').replaceChildren();
  const selecao = selecaoDoEvento(id);
  for (const a of atividades) {
    const bloco = elemento('div'); bloco.className = 'atividade'; const label = elemento('label');
    const check = elemento('input'); check.type = 'checkbox'; check.value = a.id; check.name = 'atividade'; check.disabled = !escolherAtividades; check.checked = selecao.has(a.id);
    check.addEventListener('change', () => {
      if (check.checked) selecao.add(a.id); else selecao.delete(a.id);
      $('selecao-resumo').textContent = `${selecao.size} atividade(s) selecionada(s).`;
    });
    label.append(check, document.createTextNode(` ${a.titulo} · ${a.tipo} · ${a.trilha || ''}`));
    bloco.append(label, elemento('p', `${formatarData(a.inicio)} — ${formatarData(a.fim)} · ${a.local} (${evento.fuso})`));
    const pessoas = await api.pessoas(a.id);
    if (chamada !== sequenciaProgramacao) return;
    if (pessoas.length) bloco.append(elemento('p', pessoas.map(p => `${p.nomePessoa} (${p.papel})`).join(', ')));
    $('atividades').append(bloco); $('atividade-avaliacao').add(new Option(a.titulo, a.id));
  }
  if (!atividades.length) $('atividades').append(elemento('p', 'Nenhuma atividade encontrada para os filtros.'));
  if (avaliacaoAtual && atividadeAvaliacaoId) {
    const opcaoPresente = [...$('atividade-avaliacao').options]
      .some(opcao => opcao.value === atividadeAvaliacaoId);
    if (!opcaoPresente) {
      $('atividade-avaliacao').add(new Option(`${avaliacaoAtual.textContent} (fora do filtro)`, atividadeAvaliacaoId));
    }
    $('atividade-avaliacao').value = atividadeAvaliacaoId;
  } else if (avaliacaoAtual) {
    $('questionarios').replaceChildren();
  }
  $('selecao-resumo').textContent = escolherAtividades
    ? `${selecao.size} atividade(s) selecionada(s).${inscricao ? ' Sua seleção atual foi carregada.' : ''}${api.token ? '' : ' Entre na conta para confirmar a inscrição.'}`
    : 'A inscrição cobre o evento inteiro; não é necessário selecionar atividades.';
  $('inscrever').hidden = Boolean(inscricao && !escolherAtividades);
  $('inscrever').textContent = inscricao ? 'Salvar seleção de atividades' : 'Inscrever-me neste evento';
}
async function agenda() {
  inscricoes = await api.minhasInscricoes(); $('inscricoes').replaceChildren();
  inscricoes
    .filter(i => i.status === 'CONFIRMADA')
    .forEach(i => selecoesPorEvento.set(i.eventoId, new Set(i.atividadeIds || [])));
  for (const i of inscricoes) {
    const nomeEvento = eventos.find(e => e.id === i.eventoId)?.titulo || `evento ${i.eventoId}`;
    const linha = elemento('p', `${nomeEvento} · inscrição ${i.id} · ${i.status} `);
    if (i.status === 'CONFIRMADA') {
      const cancelar = elemento('button', 'Cancelar inscrição');
      cancelar.onclick = async () => {
        if (!confirm(`Cancelar sua inscrição em ${nomeEvento}?`)) return;
        try { await api.cancelar(i.id); selecoesPorEvento.delete(i.eventoId); mensagem('Inscrição cancelada.'); await agenda(); await programacao(); }
        catch (e) { mensagem(e.message); }
      };
      linha.append(cancelar);
      const selecionar = elemento('button', 'Alterar atividades');
      selecionar.onclick = async () => {
        $('eventos').value = String(i.eventoId);
        selecoesPorEvento.set(i.eventoId, new Set(i.atividadeIds || []));
        try { await programacao(); $('eventos').scrollIntoView({behavior: 'smooth', block: 'center'}); mensagem('Altere as caixas de seleção e salve a agenda.'); }
        catch (e) { mensagem(e.message); }
      };
      linha.append(' ', selecionar);
    }
    $('inscricoes').append(linha);
  }
  $('agenda').replaceChildren();
  (await api.agenda()).forEach(a => $('agenda').append(elemento('li', `${a.titulo} · ${formatarData(a.inicio)} — ${formatarData(a.fim)} · ${a.local} (${a.fuso})`)));
}
function selecionadas() { return [...(selecoesPorEvento.get(Number($('eventos').value)) || new Set())]; }
acao('login', 'submit', async e => {
  const eventoAtual = $('eventos').value;
  const d = dados(e.target);
  await api.entrar(d.email, d.senha);
  exibirPerfil(await api.perfil());
  mensagem('Login realizado.');
  await carregarEventos();
  await agenda();
  if (eventoAtual) {
    $('eventos').value = eventoAtual;
    await programacao();
  }
});
acao('cadastro', 'submit', async e => { await api.enviar('/usuarios', 'POST', dados(e.target)); mensagem('Conta criada. Entre com seu e-mail e senha.'); e.target.reset(); });
acao('perfil-form', 'submit', async e => {
  const dadosPerfil = dados(e.target);
  exibirPerfil(await api.editarPerfil(dadosPerfil.nome, dadosPerfil.email));
  mensagem('Perfil atualizado.');
});
acao('sair', 'click', async () => { pararCamera(); api.sair(); $('usuario').textContent = 'Visitante'; $('login').hidden = false; $('cadastro-container').hidden = false; $('sair').disabled = true; $('sair').hidden = true; $('perfil-section').hidden = true; $('perfil-form').reset(); $('inscrever').disabled = true; $('inscrever').hidden = true; $('abrir-camera').disabled = true; $('atualizar-agenda').disabled = true; $('atividade-avaliacao').disabled = true; inscricoes = []; selecoesPorEvento.clear(); $('eventos').value = ''; $('inscricoes').replaceChildren(); $('agenda').replaceChildren(); $('atividades').replaceChildren(); $('questionarios').replaceChildren(); await programacao(); await carregarEventos(); mensagem('Você saiu da conta.'); });
acao('eventos', 'change', programacao); acao('filtros', 'submit', programacao);
acao('inscrever', 'click', async () => {
  if (!api.token) throw new Error('Entre na sua conta para se inscrever ou alterar sua agenda.');
  const eventoId = Number($('eventos').value);
  if (!eventoId) throw new Error('Selecione um evento.');
  const inscricao = inscricaoConfirmada(eventoId);
  if (inscricao) {
    await api.selecionar(inscricao.id, escolherAtividades ? selecionadas() : []);
    mensagem('Sua agenda foi atualizada.');
  } else {
    await api.inscrever(eventoId, escolherAtividades ? selecionadas() : []);
    mensagem('Inscrição confirmada.');
  }
  await agenda();
  await programacao();
});
acao('atualizar-agenda', 'click', agenda);
function pararCamera() {
  cameraAtiva = false;
  if (camera) camera.getTracks().forEach(trilha => trilha.stop());
  camera = null;
  $('camera-qr').srcObject = null;
  $('camera-qr').hidden = true;
  $('abrir-camera').hidden = false;
  $('fechar-camera').hidden = true;
}
async function lerQr() {
  if (!cameraAtiva) return;
  const video = $('camera-qr');
  try {
    if (video.readyState >= HTMLMediaElement.HAVE_ENOUGH_DATA) {
      const codigos = await detector.detect(video);
      if (!cameraAtiva) return;
      if (codigos.length) {
        const token = codigos[0].rawValue;
        pararCamera();
        await api.registrarPresencaQr(token);
        mensagem('Presença registrada.');
        return;
      }
    }
  } catch (erro) {
    pararCamera();
    mensagem(erro.message || 'Não foi possível ler o QR Code. Aproxime o código da câmera e tente novamente.');
    return;
  }
  if (cameraAtiva) setTimeout(lerQr, 250);
}
$('abrir-camera').addEventListener('click', async () => {
  try {
    if (!('BarcodeDetector' in window) || !navigator.mediaDevices?.getUserMedia)
      throw new Error('Este navegador não oferece leitura de QR pela câmera. Use Chrome ou Edge atualizado.');
    detector = new BarcodeDetector({formats: ['qr_code']});
    camera = await navigator.mediaDevices.getUserMedia({video: {facingMode: 'environment'}, audio: false});
    $('camera-qr').srcObject = camera;
    $('camera-qr').hidden = false;
    $('abrir-camera').hidden = true;
    $('fechar-camera').hidden = false;
    await $('camera-qr').play();
    cameraAtiva = true;
    mensagem('Aponte a câmera para o QR Code da atividade.');
    lerQr();
  } catch (erro) {
    pararCamera();
    mensagem(
      erro.name === 'NotAllowedError'
        ? 'Permita o acesso à câmera para registrar a presença.'
        : erro.name === 'NotFoundError'
          ? 'Nenhuma câmera foi encontrada neste dispositivo.'
          : erro.message || 'Não foi possível abrir a câmera.'
    );
  }
});
$('fechar-camera').addEventListener('click', pararCamera);
window.addEventListener('pagehide', pararCamera);
acao('atividade-avaliacao', 'change', async () => {
  const id = Number($('atividade-avaliacao').value); if (!id) return;
  $('questionarios').replaceChildren();
  for (const q of await api.questionarios(id)) {
    const form = elemento('form'); form.className = 'questionario';
    form.append(elemento('h3', q.titulo));
    const politica = elemento('p', q.politicaIdentificacao);
    politica.className = 'politica-avaliacao';
    form.append(politica);
    for (const p of q.perguntas) {
      const label = elemento('label', p.enunciado); let campo;
      let orientacao;
      if (p.tipo === 'ESCOLHA_UNICA') {
        campo = elemento('select'); campo.add(new Option('Selecione uma opção', ''));
        p.opcoes.forEach(v => campo.add(new Option(v, v)));
        orientacao = `Escolha uma opção entre as ${p.opcoes.length} disponíveis.`;
      } else if (p.tipo === 'ESCALA') {
        campo = elemento('input'); campo.type = 'number'; campo.min = p.minimo; campo.max = p.maximo; campo.step = '1';
        orientacao = `Informe um número inteiro de ${p.minimo} a ${p.maximo}.`;
      } else if (p.tipo === 'TEXTO') {
        campo = elemento('textarea'); campo.maxLength = 4000;
        orientacao = 'Resposta obrigatória, com até 4000 caracteres.';
      } else {
        throw new Error(`Tipo de resposta não reconhecido: ${p.tipo}.`);
      }
      campo.name = p.id; campo.required = true; label.append(campo);
      form.append(label, elemento('small', orientacao));
    }
    const botao = elemento('button', 'Enviar avaliação'); form.append(botao);
    form.onsubmit = async e => {
      e.preventDefault();
      if (form.dataset.enviada === 'true') return;
      botao.disabled = true;
      try {
        const respostas = dados(form);
        for (const p of q.perguntas) {
          const resposta = respostas[p.id];
          if (!resposta || !resposta.trim()) throw new Error('Responda todas as perguntas antes de enviar.');
          if (p.tipo === 'TEXTO' && resposta.length > 4000)
            throw new Error('Cada resposta de texto pode ter no máximo 4000 caracteres.');
          if (p.tipo === 'ESCALA') {
            const valor = Number(resposta);
            if (!Number.isInteger(valor) || valor < p.minimo || valor > p.maximo)
              throw new Error(`Na pergunta de escala, informe um inteiro de ${p.minimo} a ${p.maximo}.`);
          }
          if (p.tipo === 'ESCOLHA_UNICA' && !p.opcoes.includes(resposta))
            throw new Error('Selecione uma das opções apresentadas.');
        }
        await api.responder(q.id, respostas);
        form.dataset.enviada = 'true';
        form.querySelectorAll('input, select, textarea').forEach(campo => campo.disabled = true);
        botao.textContent = 'Avaliação enviada';
        mensagem('Avaliação enviada.');
      } catch (erro) {
        mensagem(erro.message);
        botao.disabled = false;
      }
    };
    $('questionarios').append(form);
  }
});
if (api.token) {
  api.perfil()
    .then(async usuario => {
      exibirPerfil(usuario);
      await carregarEventos();
      await agenda();
    })
    .catch(() => {
      api.sair();
      $('usuario').textContent = 'Visitante';
      $('sair').disabled = true;
      $('sair').hidden = true;
      $('login').hidden = false;
      $('cadastro-container').hidden = false;
      $('perfil-section').hidden = true;
      $('inscrever').disabled = true;
      $('abrir-camera').disabled = true;
      $('atualizar-agenda').disabled = true;
      $('atividade-avaliacao').disabled = true;
      carregarEventos().catch(e => mensagem(e.message));
    });
} else {
  $('sair').disabled = true;
  $('inscrever').disabled = true;
  $('abrir-camera').disabled = true;
  $('atualizar-agenda').disabled = true;
  $('atividade-avaliacao').disabled = true;
  carregarEventos().catch(e => mensagem(e.message));
}
