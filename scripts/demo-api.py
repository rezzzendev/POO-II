#!/usr/bin/env python3
"""Demonstra CA-01 a CA-07 usando a API real e uma conta demo de organização."""
import argparse
import base64
import json
import time
from datetime import datetime, timedelta
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

parser = argparse.ArgumentParser()
parser.add_argument('--base', default='http://localhost:8080')
args = parser.parse_args()

def chamar(metodo, rota, corpo=None, token=None, esperado=200):
    cabecalhos = {'Content-Type': 'application/json'}
    if token:
        cabecalhos['Authorization'] = 'Bearer ' + token
    r = Request(args.base + rota, data=None if corpo is None else json.dumps(corpo).encode(), headers=cabecalhos, method=metodo)
    try:
        resposta = urlopen(r, timeout=30)
    except HTTPError as e:
        resposta = e
    with resposta:
        texto = resposta.read().decode('utf-8-sig')
        assert resposta.status == esperado, (rota, resposta.status, texto)
        return json.loads(texto) if 'application/json' in resposta.headers.get('Content-Type', '') else texto

org = chamar('POST', '/login', {'email': 'organizador@demo.local', 'senha': 'Demo123!'})['token']
marca = str(time.time_ns())
# Horários locais serão interpretados no fuso configurado no evento.
inicio = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(days=1)
iso = lambda d: d.isoformat()
evento = chamar('POST', '/eventos', {'titulo': 'Roteiro CA ' + marca, 'descricao': 'Demonstração reproduzível', 'inicio': iso(inicio), 'fim': iso(inicio + timedelta(days=1)), 'modalidade': 'PRESENCIAL', 'local': 'Campus', 'fuso': 'America/Sao_Paulo'}, org, 201)
eid = evento['id']
atividade = chamar('POST', '/atividades', {'eventoId': eid, 'titulo': 'Oficina de POO', 'tipo': 'Oficina', 'trilha': 'Java', 'local': 'Sala 1', 'inicio': iso(inicio + timedelta(hours=9)), 'fim': iso(inicio + timedelta(hours=10)), 'capacidade': 2}, org, 201)
aid = atividade['id']
chamar('POST', f'/eventos/{eid}/publicar', {}, org)
assert any(e['id'] == eid for e in chamar('GET', '/eventos'))
print('CA-01: publicação e consulta pública conferidas')
email = f'roteiro{marca}@demo.local'
usuario = chamar('POST', '/usuarios', {'nome': 'Participante do roteiro', 'email': email, 'senha': 'Demo123!'}, esperado=201)
token = chamar('POST', '/login', {'email': email, 'senha': 'Demo123!'})['token']
chamar('POST', '/inscricoes', {'eventoId': eid, 'atividadeIds': [aid]}, token, 201)
chamar('POST', '/inscricoes', {'eventoId': eid, 'atividadeIds': [aid]}, token, 400)
print('CA-02: inscrição persistida e duplicidade bloqueada')
assert chamar('GET', '/agenda', token=token)[0]['id'] == aid
print('CA-03: agenda derivada da inscrição')
q = chamar('POST', '/questionarios', {'atividadeId': aid, 'titulo': 'Avaliação', 'perguntas': [{'enunciado': 'Comentário', 'tipo': 'TEXTO'}, {'enunciado': 'Recomenda?', 'tipo': 'ESCOLHA_UNICA', 'opcoes': ['Sim', 'Não']}, {'enunciado': 'Nota', 'tipo': 'ESCALA', 'minimo': 1, 'maximo': 5}]}, org, 201)
respostas = {'respostas': {str(p['id']): v for p, v in zip(q['perguntas'], ['Bom conteúdo', 'Sim', 5])}}
chamar('POST', f"/questionarios/{q['id']}/respostas", respostas, token, 400)
qr = chamar('POST', f'/frequencia/{aid}/codigos', {'tipo': 'CHECK_IN'}, org, 201)
chamar('POST', '/frequencia/qr', {'imagemBase64': qr['imagemBase64']}, token, 201)
chamar('POST', '/frequencia/qr', {'token': qr['token']}, token, 400)
assert chamar('GET', f'/frequencia/{aid}', token=token)['presente']
print('CA-04: imagem QR lida, presença validada e repetição rejeitada')
for presente in (False, True):
    chamar('POST', f'/frequencia/{aid}/manual', {'usuarioId': usuario['id'], 'presente': presente, 'justificativa': 'Conferência do roteiro'}, org, 201)
historico = chamar('GET', f'/frequencia/{aid}', token=token)
assert historico['presente'] and len(historico['registros']) == 3
print('CA-05: correções manuais preservam autoria e histórico')
chamar('POST', f"/questionarios/{q['id']}/respostas", respostas, token, 201)
chamar('POST', f"/questionarios/{q['id']}/respostas", respostas, token, 400)
assert chamar('GET', f"/questionarios/{q['id']}/resultados", token=org)['total'] == 1
print('CA-06: elegibilidade, três tipos de resposta e consolidação conferidos')
assert chamar('GET', f'/relatorios/frequencia?eventoId={eid}', token=org)['presentes'] == 1
csv = chamar('GET', f'/relatorios/frequencia?eventoId={eid}&formato=csv', token=org)
Path('target').mkdir(exist_ok=True)
Path('target/demo-api.csv').write_text(csv, encoding='utf-8-sig')
print('CA-07: relatório coerente; CSV salvo em target/demo-api.csv')
print(f'Evento do roteiro: {eid}. Teste também as interfaces; o script valida a API.')
