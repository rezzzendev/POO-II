#!/usr/bin/env python3
"""Medição local simples, não representa teste de carga de produção."""
import argparse
import json
import statistics
import time
from urllib.request import Request, urlopen
p=argparse.ArgumentParser()
p.add_argument('--base',default='http://localhost:8080')
a=p.parse_args()
def obter(rota,token=None,corpo=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    with urlopen(Request(a.base+rota,headers=headers,data=None if corpo is None else json.dumps(corpo).encode()),timeout=60) as r:
        return json.load(r)
token=obter('/login',corpo={'email':'organizador@demo.local','senha':'Demo123!'})['token']
e=next(e for e in obter('/eventos',token) if e['titulo']=='JAVA8 — Demonstração')
for rota in ['/eventos',f"/atividades?eventoId={e['id']}",f"/relatorios/inscritos?eventoId={e['id']}",f"/relatorios/frequencia?eventoId={e['id']}"]:
    tempos=[]
    for _ in range(3):
        inicio=time.perf_counter(); dados=obter(rota,token);tempos.append((time.perf_counter()-inicio)*1000)
    total=len(dados) if isinstance(dados,list) else dados['total']
    print(f'{rota}: {total} registros; mediana {statistics.median(tempos):.1f} ms; máximo {max(tempos):.1f} ms (3 consultas)')
