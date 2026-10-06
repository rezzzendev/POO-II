import {Api} from './api.js';

const api = new Api();
const $ = id => document.getElementById(id);
const mensagem = texto => { $('mensagem').textContent = texto; };
const dados = form => Object.fromEntries(new FormData(form));
const elemento = (tag, texto) => {
  const item = document.createElement(tag);
  if (texto !== undefined) item.textContent = texto;
  return item;
};

let eventos = [];
let atividades = [];
let inscricoes = [];
let escolherAtividades = true;
let eventoProgramacaoAtual = null;
let sequenciaProgramacao = 0;
let paginaProgramacao = 1;
let totalPaginasProgramacao = 1;
let totalAtividadesProgramacao = 0;
const tamanhoPaginaProgramacao = 10;
let camera;
let detector;
let cameraAtiva = false;
const selecoesPorEvento = new Map();

function abrirPagina(id, atualizarHistorico = true) {
  const pagina = $(id) ? id : 'programacao';
  document.querySelectorAll('[data-pagina-conteudo]').forEach(secao => {
    secao.hidden = secao.id !== pagina;
  });
  document.querySelectorAll('[data-pagina]').forEach(link => {
    const ativo = link.dataset.pagina === pagina;
    link.classList.toggle('ativo', ativo);
    if (ativo) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  document.title = `${$(pagina).querySelector('h2').textContent} — JAVA8`;
  if (atualizarHistorico && location.hash !== `#${pagina}`) history.pushState(null, '', `#${pagina}`);
  window.scrollTo({top: 0, behavior: 'smooth'});
}

document.querySelectorAll('[data-pagina]').forEach(link => link.addEventListener('click', evento => {
  evento.preventDefault();
  abrirPagina(link.dataset.pagina);
}));
window.addEventListener('popstate', () => abrirPagina(location.hash.slice(1), false));

function exigirParticipante(usuario) {
  if (usuario.papel !== 'PARTICIPANTE') {
    api.sair();
    throw new Error('Esta conta usa o aplicativo desktop. O site é exclusivo para visitantes e participantes.');
  }
  return usuario;
}

function exibirPerfil(usuario) {
  $('usuario').textContent = `${usuario.nome} · Participante`;
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

function limparSessao() {
  pararCamera();
  api.sair();
  $('usuario').textContent = 'Visitante';
  $('login').hidden = false;
  $('cadastro-container').hidden = false;
  $('sair').disabled = true;
  $('sair').hidden = true;
  $('perfil-section').hidden = true;
  $('perfil-form').reset();
  $('inscrever').disabled = true;
  $('inscrever').hidden = true;
  $('abrir-camera').disabled = true;
  $('atualizar-agenda').disabled = true;
  $('atividade-avaliacao').disabled = true;
  inscricoes = [];
  selecoesPorEvento.clear();
  $('eventos').value = '';
  $('inscricoes').replaceChildren();
  $('agenda').replaceChildren();
  $('atividades').replaceChildren();
  $('questionarios').replaceChildren();
}

function acao(id, evento, tarefa) {
  $(id).addEventListener(evento, async e => {
    e.preventDefault();
    const botao = e.currentTarget.matches('button')
      ? e.currentTarget
      : e.currentTarget.querySelector('button');
    if (botao) botao.disabled = true;
    try { await tarefa(e); }
    catch (erro) { mensagem(erro.message); }
    finally { if (botao) botao.disabled = false; }
  });
}

async function carregarEventos() {
  eventos = await api.eventos();
  $('eventos').replaceChildren(new Option('Selecione um evento', ''));
  eventos.forEach(evento => $('eventos').add(new Option(`${evento.titulo} · ${evento.status}`, evento.id)));
  if (!eventos.length) $('evento-descricao').textContent = 'Ainda não há eventos publicados.';
}

function formatarData(valor) {
  const partes = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})/.exec(valor || '');
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]} ${partes[4]}:${partes[5]}` : (valor || '');
}

function inscricaoConfirmada(eventoId) {
  return inscricoes.find(inscricao => inscricao.eventoId === eventoId && inscricao.status === 'CONFIRMADA');
}

function selecaoDoEvento(eventoId) {
  if (!selecoesPorEvento.has(eventoId)) {
    const inscricao = inscricaoConfirmada(eventoId);
    selecoesPorEvento.set(eventoId, new Set(inscricao?.atividadeIds || []));
  }
  return selecoesPorEvento.get(eventoId);
}

function atualizarPaginacao(total) {
  totalAtividadesProgramacao = total;
  const paginacao = $('programacao-paginacao');
  paginacao.hidden = total === 0;
  $('pagina-informacao').textContent = total
    ? `Página ${paginaProgramacao} de ${totalPaginasProgramacao} · ${total} atividade(s)`
    : '';
  $('pagina-anterior').disabled = paginaProgramacao <= 1;
  $('pagina-proxima').disabled = paginaProgramacao >= totalPaginasProgramacao;
}

async function reiniciarProgramacao() {
  paginaProgramacao = 1;
  await programacao();
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
    atualizarPaginacao(0);
    $('atividade-avaliacao').replaceChildren(new Option('Selecione uma atividade na programação', ''));
    $('questionarios').replaceChildren();
    $('inscrever').hidden = true;
    return;
  }

  const evento = eventos.find(item => item.id === id);
  if (!evento) throw new Error('O evento selecionado não está mais disponível. Atualize a página.');
  const mudouEvento = eventoProgramacaoAtual !== id;
  const avaliacaoAtual = mudouEvento ? null : $('atividade-avaliacao').selectedOptions[0];
  const atividadeAvaliacaoId = avaliacaoAtual?.value || '';
  $('evento-descricao').textContent = `${evento.descricao || ''} · ${evento.local} · ${formatarData(evento.inicio)} a ${formatarData(evento.fim)} (${evento.fuso})`;

  const regras = await api.regrasInscricao(id);
  escolherAtividades = regras.escolherAtividades;
  if (chamada !== sequenciaProgramacao) return;
  const inscricao = inscricaoConfirmada(id);
  $('regras').textContent = `${escolherAtividades ? 'Escolha suas atividades.' : 'A inscrição vale para o evento inteiro.'} Cancelamento até ${formatarData(regras.prazoCancelamento)} (${evento.fuso}).`;
  const filtros = Object.fromEntries(Object.entries(dados($('filtros'))).filter(([, valor]) => valor));
  const resultado = await api.atividades(
    id, filtros, paginaProgramacao, tamanhoPaginaProgramacao
  );
  atividades = resultado.itens;
  totalPaginasProgramacao = resultado.totalPaginas;
  if (chamada !== sequenciaProgramacao) return;

  eventoProgramacaoAtual = id;
  $('atividades').replaceChildren();
  $('atividade-avaliacao').replaceChildren(new Option('Selecione uma atividade', ''));
  if (mudouEvento) $('questionarios').replaceChildren();
  const selecao = selecaoDoEvento(id);

  for (const atividade of atividades) {
    const bloco = elemento('article');
    bloco.className = 'atividade';
    const label = elemento('label');
    const check = elemento('input');
    check.type = 'checkbox';
    check.value = atividade.id;
    check.name = 'atividade';
    check.disabled = !escolherAtividades;
    check.checked = selecao.has(atividade.id);
    check.addEventListener('change', () => {
      if (check.checked) selecao.add(atividade.id);
      else selecao.delete(atividade.id);
      $('selecao-resumo').textContent = `${selecao.size} atividade(s) selecionada(s).`;
    });
    label.append(check, document.createTextNode(` ${atividade.titulo} · ${atividade.tipo} · ${atividade.trilha || ''}`));
    bloco.append(label, elemento('p', `${formatarData(atividade.inicio)} — ${formatarData(atividade.fim)} · ${atividade.local} (${evento.fuso})`));
    const pessoas = await api.pessoas(atividade.id);
    if (chamada !== sequenciaProgramacao) return;
    if (pessoas.length) bloco.append(elemento('p', pessoas.map(pessoa => `${pessoa.nomePessoa} (${pessoa.papel})`).join(', ')));
    $('atividades').append(bloco);
    $('atividade-avaliacao').add(new Option(atividade.titulo, atividade.id));
  }

  if (!atividades.length) $('atividades').append(elemento('p', 'Nenhuma atividade encontrada para os filtros.'));
  atualizarPaginacao(resultado.total);
  if (avaliacaoAtual && atividadeAvaliacaoId) {
    if (![...$('atividade-avaliacao').options].some(opcao => opcao.value === atividadeAvaliacaoId)) {
      $('atividade-avaliacao').add(new Option(`${avaliacaoAtual.textContent} (fora do filtro)`, atividadeAvaliacaoId));
    }
    $('atividade-avaliacao').value = atividadeAvaliacaoId;
  }
  $('selecao-resumo').textContent = escolherAtividades
    ? `${selecao.size} atividade(s) selecionada(s).${inscricao ? ' Sua seleção atual foi carregada.' : ''}${api.token ? '' : ' Entre na conta para confirmar a inscrição.'}`
    : 'A inscrição cobre o evento inteiro; não é necessário selecionar atividades.';
  $('inscrever').hidden = Boolean(inscricao && !escolherAtividades);
  $('inscrever').textContent = inscricao ? 'Salvar seleção de atividades' : 'Inscrever-me neste evento';
}

async function agenda() {
  inscricoes = await api.minhasInscricoes();
  $('inscricoes').replaceChildren();
  inscricoes.filter(i => i.status === 'CONFIRMADA')
    .forEach(i => selecoesPorEvento.set(i.eventoId, new Set(i.atividadeIds || [])));

  for (const inscricao of inscricoes) {
    const nomeEvento = eventos.find(e => e.id === inscricao.eventoId)?.titulo || `evento ${inscricao.eventoId}`;
    const linha = elemento('p', `${nomeEvento} · inscrição ${inscricao.id} · ${inscricao.status} `);
    if (inscricao.status === 'CONFIRMADA') {
      const cancelar = elemento('button', 'Cancelar inscrição');
      cancelar.onclick = async () => {
        if (!confirm(`Cancelar sua inscrição em ${nomeEvento}?`)) return;
        try {
          await api.cancelar(inscricao.id);
          selecoesPorEvento.delete(inscricao.eventoId);
          mensagem('Inscrição cancelada.');
          await agenda();
          await programacao();
        } catch (erro) { mensagem(erro.message); }
      };
      const alterar = elemento('button', 'Alterar atividades');
      alterar.onclick = async () => {
        $('eventos').value = String(inscricao.eventoId);
        selecoesPorEvento.set(inscricao.eventoId, new Set(inscricao.atividadeIds || []));
        try {
          await programacao();
          abrirPagina('programacao');
          mensagem('Altere as atividades e salve sua seleção.');
        } catch (erro) { mensagem(erro.message); }
      };
      linha.append(cancelar, alterar);
    }
    $('inscricoes').append(linha);
  }

  $('agenda').replaceChildren();
  (await api.agenda()).forEach(item => $('agenda').append(
    elemento('li', `${item.titulo} · ${formatarData(item.inicio)} — ${formatarData(item.fim)} · ${item.local} (${item.fuso})`)
  ));
}

acao('login', 'submit', async e => {
  const eventoAtual = $('eventos').value;
  const credenciais = dados(e.target);
  await api.entrar(credenciais.email, credenciais.senha);
  const usuario = exigirParticipante(await api.perfil());
  exibirPerfil(usuario);
  await carregarEventos();
  await agenda();
  if (eventoAtual) {
    $('eventos').value = eventoAtual;
    await programacao();
  }
  mensagem('Login realizado. Agora você pode se inscrever.');
  abrirPagina('programacao');
});

acao('cadastro', 'submit', async e => {
  await api.enviar('/usuarios', 'POST', dados(e.target));
  e.target.reset();
  mensagem('Conta criada. Entre com seu e-mail e senha.');
});

acao('perfil-form', 'submit', async e => {
  const perfil = dados(e.target);
  exibirPerfil(exigirParticipante(await api.editarPerfil(perfil.nome, perfil.email)));
  mensagem('Perfil atualizado.');
});

acao('sair', 'click', async () => {
  limparSessao();
  await carregarEventos();
  mensagem('Você saiu da conta.');
  abrirPagina('programacao');
});

acao('eventos', 'change', reiniciarProgramacao);
acao('filtros', 'submit', reiniciarProgramacao);
async function mudarPagina(destino) {
  if (destino < 1 || destino > totalPaginasProgramacao) return;
  const anterior = paginaProgramacao;
  paginaProgramacao = destino;
  $('pagina-anterior').disabled = true;
  $('pagina-proxima').disabled = true;
  try {
    await programacao();
  } catch (erro) {
    paginaProgramacao = anterior;
    atualizarPaginacao(totalAtividadesProgramacao);
    mensagem(erro.message);
  }
}

$('pagina-anterior').addEventListener('click', () => mudarPagina(paginaProgramacao - 1));
$('pagina-proxima').addEventListener('click', () => mudarPagina(paginaProgramacao + 1));
acao('inscrever', 'click', async () => {
  if (!api.token) {
    abrirPagina('conta');
    throw new Error('Entre na sua conta para confirmar a inscrição.');
  }
  const eventoId = Number($('eventos').value);
  if (!eventoId) throw new Error('Selecione um evento.');
  const inscricao = inscricaoConfirmada(eventoId);
  const selecionadas = [...(selecoesPorEvento.get(eventoId) || new Set())];
  if (inscricao) {
    await api.selecionar(inscricao.id, escolherAtividades ? selecionadas : []);
    mensagem('Sua agenda foi atualizada.');
  } else {
    await api.inscrever(eventoId, escolherAtividades ? selecionadas : []);
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

async function obterDetectorQr() {
  if (!('BarcodeDetector' in window)) throw new Error('Este navegador não oferece leitura de QR. Use Chrome ou Edge atualizado.');
  if (!detector) {
    const formatos = await BarcodeDetector.getSupportedFormats();
    if (!formatos.includes('qr_code')) throw new Error('Este navegador não reconhece o formato QR Code.');
    detector = new BarcodeDetector({formats: ['qr_code']});
  }
  return detector;
}

async function lerQr() {
  if (!cameraAtiva) return;
  try {
    if ($('camera-qr').readyState >= HTMLMediaElement.HAVE_ENOUGH_DATA) {
      const codigos = await detector.detect($('camera-qr'));
      if (cameraAtiva && codigos.length) {
        pararCamera();
        await api.registrarPresencaQr(codigos[0].rawValue);
        mensagem('Presença registrada.');
        return;
      }
    }
  } catch (erro) {
    pararCamera();
    mensagem(erro.message || 'Não foi possível ler o QR Code. Aproxime o código e tente novamente.');
    return;
  }
  if (cameraAtiva) setTimeout(lerQr, 250);
}

$('abrir-camera').addEventListener('click', async () => {
  try {
    if (!navigator.mediaDevices?.getUserMedia) throw new Error('A câmera exige Chrome/Edge em localhost ou uma conexão HTTPS.');
    detector = await obterDetectorQr();
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
    mensagem(erro.name === 'NotAllowedError'
      ? 'Permita o acesso à câmera para registrar a presença.'
      : erro.name === 'NotFoundError'
        ? 'Nenhuma câmera foi encontrada neste dispositivo.'
        : erro.message || 'Não foi possível abrir a câmera.');
  }
});
$('fechar-camera').addEventListener('click', pararCamera);
window.addEventListener('pagehide', pararCamera);

acao('atividade-avaliacao', 'change', async () => {
  const id = Number($('atividade-avaliacao').value);
  $('questionarios').replaceChildren();
  if (!id) return;
  for (const questionario of await api.questionarios(id)) {
    const form = elemento('form');
    form.className = 'questionario';
    form.append(elemento('h3', questionario.titulo));
    const politica = elemento('p', questionario.politicaIdentificacao);
    politica.className = 'politica-avaliacao';
    form.append(politica);
    for (const pergunta of questionario.perguntas) {
      const label = elemento('label', pergunta.enunciado);
      let campo;
      let orientacao;
      if (pergunta.tipo === 'ESCOLHA_UNICA') {
        campo = elemento('select');
        campo.add(new Option('Selecione uma opção', ''));
        pergunta.opcoes.forEach(opcao => campo.add(new Option(opcao, opcao)));
        orientacao = `Escolha uma das ${pergunta.opcoes.length} opções.`;
      } else if (pergunta.tipo === 'ESCALA') {
        campo = elemento('input');
        campo.type = 'number';
        campo.min = pergunta.minimo;
        campo.max = pergunta.maximo;
        campo.step = '1';
        orientacao = `Informe um número inteiro de ${pergunta.minimo} a ${pergunta.maximo}.`;
      } else {
        campo = elemento('textarea');
        campo.maxLength = 4000;
        orientacao = 'Resposta obrigatória, com até 4000 caracteres.';
      }
      campo.name = pergunta.id;
      campo.required = true;
      label.append(campo);
      form.append(label, elemento('small', orientacao));
    }
    const botao = elemento('button', 'Enviar avaliação');
    form.append(botao);
    form.onsubmit = async evento => {
      evento.preventDefault();
      botao.disabled = true;
      try {
        const respostas = dados(form);
        await api.responder(questionario.id, respostas);
        form.querySelectorAll('input, select, textarea').forEach(campo => { campo.disabled = true; });
        botao.textContent = 'Avaliação enviada';
        mensagem('Avaliação enviada.');
      } catch (erro) {
        botao.disabled = false;
        mensagem(erro.message);
      }
    };
    $('questionarios').append(form);
  }
});

async function iniciar() {
  abrirPagina(location.hash.slice(1), false);
  await carregarEventos();
  if (!api.token) return;
  try {
    const usuario = exigirParticipante(await api.perfil());
    exibirPerfil(usuario);
    await agenda();
  } catch (erro) {
    limparSessao();
    mensagem(erro.message);
  }
}

iniciar().catch(erro => mensagem(erro.message));
