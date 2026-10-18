# Análise de dados — PoC de Propagação de Emoções

Toolkit em Python para análise estatística e geração de gráficos a partir
dos resultados da simulação, complementando o dashboard web (que é ótimo
para explorar interativamente, mas não gera figuras para a monografia
nem roda testes estatísticos).

Duas fontes de dados diferentes, dois scripts diferentes:

| Fonte                                         | Script                  | O que analisa |
|------------------------------------------------|--------------------------|----------------|
| PostgreSQL (`simulacao`/`estado_emocional`)     | `relatorio_simulacao.py` | Uma simulação real (via JADE), agente a agente, ciclo a ciclo |
| `comparacao.json` (dashboard ou `LancadorComparacao`) | `comparar_json.py`       | Rede real x redes sintéticas equivalentes |

O banco não guarda a topologia da rede (só os estados por ciclo), por
isso a comparação estrutural depende do JSON exportado pela camada de
interface (`RelatorioExport`/`AnaliseExport`, Etapa 4.9), não do banco.

## Instalação

```bash
cd analise
pip install -r requirements.txt
```

## `relatorio_simulacao.py` — relatório de uma simulação real

```bash
# simulação mais recente
python relatorio_simulacao.py

# uma simulação específica, salvando em uma pasta própria
python relatorio_simulacao.py --id 57 --saida ./saida_sim57
```

Credenciais do banco lidas de variáveis de ambiente (mesmos padrões da
aplicação Java): `POC_DB_HOST`, `POC_DB_NAME`, `POC_DB_USER`,
`POC_DB_PASSWORD`, `POC_DB_PORT`.

Gera:
- `evolucao_media_simN.png` — média ± desvio-padrão de cada emoção por ciclo;
- `distribuicao_categoria_simN.png` — área empilhada com a contagem de agentes por rótulo dominante ao longo dos ciclos;
- `trajetorias_anger_simN.png` / `trajetorias_joy_simN.png` — trajetória individual de uma amostra de agentes (até 15 por padrão — ajustável com `--max-agentes-trajetoria`) para raiva e alegria;
- `resumo_simN.csv` — média/desvio/mínimo/máximo de cada emoção por ciclo, para abrir em Excel/planilha;
- no terminal: teste qui-quadrado comparando a distribuição de rótulos entre o primeiro e o último ciclo (a mudança observada é estatisticamente significativa, ou só ruído?).

> `analise_emocoes.py` (nome do script original desta PoC) continua funcionando — é hoje só um atalho para `relatorio_simulacao.py`. O script original só lia o modelo emocional escalar antigo (uma única coluna `valor`) e quebrava com `IndexError` para redes com mais de 10 agentes, por usar paletas de cores/estilos fixas por agente; `relatorio_simulacao.py` funciona com qualquer número de agentes e já considera as quatro dimensões emocionais atuais.

## `comparar_json.py` — rede real x redes sintéticas

```bash
curl http://localhost:8080/api/comparacao.json -o comparacao.json
python comparar_json.py comparacao.json --saida ./saida_comparacao
```

(ou aponte para o arquivo salvo por `LancadorComparacao`, se preferir gerar os dados sem passar pelo navegador.)

Gera:
- `comparacao_estrutura.png` — grau médio, coeficiente de agrupamento e distância média lado a lado, um cenário por barra;
- `comparacao_evolucao_<emocao>.png` — evolução da emoção de referência (a mais semeada na rede real, por padrão — ajustável com `--emocao`) em cada cenário, sobrepostas;
- no terminal: teste qui-quadrado comparando a distribuição final de rótulos entre os 4 cenários — indica se a diferença entre a rede real e as sintéticas é estatisticamente significativa, ou só ruído de uma execução aleatória específica.

## Módulos reutilizáveis

Caso queira montar sua própria análise (ex.: em um notebook Jupyter):

- `db.py` — `conectar()`, `listar_simulacoes()`, `carregar_estados(simulacao_id)`, `carregar_metadados(simulacao_id)`, `ultima_simulacao_id()` — todos retornando `pandas.DataFrame`/`Series`;
- `graficos.py` — `grafico_evolucao_media`, `grafico_distribuicao_categoria`, `grafico_trajetorias_agentes`;
- `estatisticas.py` — `resumo_por_ciclo`, `resumo_geral`, `distribuicao_categoria_por_ciclo`, `teste_mudanca_distribuicao`.

Exemplo mínimo:

```python
import db, graficos

df = db.carregar_estados()  # simulação mais recente
graficos.grafico_evolucao_media(df, "evolucao.png")
```

## Achados já confirmados com este toolkit (ego 11681802)

Rodando contra os dados reais gerados nesta PoC:

- A distribuição de rótulos dominantes muda significativamente do ciclo 0 para o último ciclo (χ² ≈ 212, p < 0.001) — a propagação realmente move a rede para fora do estado neutro inicial, não é ruído.
- Comparando a rede real com as três topologias sintéticas equivalentes (mesmo tamanho, mesma quantidade de sementes por emoção), a distribuição final de rótulos difere significativamente entre os cenários (χ² ≈ 102, p < 0.001) — a topologia da rede influencia o resultado da propagação, confirmando estatisticamente a observação qualitativa feita a partir do dashboard (ver `docs/ETAPAS_EVOLUCAO.md`, seção "Análise comparativa").
