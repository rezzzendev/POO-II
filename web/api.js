// Única fronteira HTTP do site. Nenhuma regra de negócio é reimplementada aqui.
export class Api {
  constructor(base = '') { this.base = base; this.token = sessionStorage.getItem('token'); }
  async enviar(caminho, metodo = 'GET', corpo) {
    const headers = {};
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    if (corpo !== undefined) headers['Content-Type'] = 'application/json';
    const resposta = await fetch(this.base + caminho, {method: metodo, headers, body: corpo === undefined ? undefined : JSON.stringify(corpo)});
    const texto = await resposta.text();
    const json = texto ? JSON.parse(texto) : null;
    if (!resposta.ok) throw new Error(json?.erro || `Erro ${resposta.status}`);
    return json;
  }
  async entrar(email, senha) { const u = await this.enviar('/login', 'POST', {email, senha}); this.token = u.token; sessionStorage.setItem('token', u.token); return u; }
  sair() { this.token = null; sessionStorage.removeItem('token'); }
  eventos() { return this.enviar('/eventos'); }
  atividades(eventoId, filtros = {}) { return this.enviar('/atividades?' + new URLSearchParams({eventoId, ...filtros})); }
  inscrever(eventoId, atividadeIds) { return this.enviar('/inscricoes', 'POST', {eventoId, atividadeIds}); }
  selecionar(id, atividadeIds) { return this.enviar(`/inscricoes/${id}/atividades`, 'PUT', {atividadeIds}); }
  cancelar(id) { return this.enviar(`/inscricoes/${id}/cancelar`, 'POST', {}); }
  agenda() { return this.enviar('/agenda'); }
  questionarios(atividadeId) { return this.enviar(`/questionarios?atividadeId=${atividadeId}`); }
  responder(id, respostas) { return this.enviar(`/questionarios/${id}/respostas`, 'POST', {respostas}); }
}
