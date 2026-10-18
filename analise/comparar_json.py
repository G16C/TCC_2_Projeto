"""
Analisa o arquivo JSON de comparação entre a rede real do Twitter e as
redes sintéticas equivalentes, produzido pelo dashboard
(GET /api/comparacao.json) ou por `LancadorComparacao`.

Complementa a visualização em JavaScript do dashboard com figuras em
qualidade de publicação (para a monografia) e um teste estatístico que
o dashboard não faz.

Uso:
    curl http://localhost:8080/api/comparacao.json -o comparacao.json
    python comparar_json.py comparacao.json --saida ./saida_comparacao
"""
import argparse
import json
import os

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats

PALETA = ["#e8b933", "#3f7ab0", "#e0483e", "#5a8f4f", "#9467bd", "#8c564b"]


def carregar(caminho_json: str) -> dict:
    with open(caminho_json, encoding="utf-8") as arquivo:
        return json.load(arquivo)


def tabela_estrutura(dados: dict) -> pd.DataFrame:
    linhas = [{"cenario": c["nome"], **c["estrutura"]} for c in dados["cenarios"]]
    return pd.DataFrame(linhas).set_index("cenario")


def tabela_serie_media(dados: dict) -> pd.DataFrame:
    """Série temporal (ciclo x emoção) de todos os cenários, empilhada com uma coluna 'cenario'."""
    partes = []
    for cenario in dados["cenarios"]:
        df = pd.DataFrame(cenario["propagacao"]["serieMedia"])
        df["cenario"] = cenario["nome"]
        partes.append(df)
    return pd.concat(partes, ignore_index=True)


def grafico_comparacao_estrutura(dados: dict, caminho_saida: str) -> None:
    tabela = tabela_estrutura(dados)
    metricas = ["grauMedio", "coeficienteAgrupamentoMedio", "distanciaMediaAprox"]
    titulos = ["Grau médio", "Coeficiente de agrupamento médio", "Distância média (aprox.)"]
    cores = PALETA[: len(tabela)]

    fig, eixos = plt.subplots(1, len(metricas), figsize=(4.2 * len(metricas), 4.4))
    for eixo, metrica, titulo in zip(eixos, metricas, titulos):
        eixo.bar(tabela.index, tabela[metrica], color=cores)
        eixo.set_title(titulo)
        eixo.tick_params(axis="x", rotation=30)
        for tick in eixo.get_xticklabels():
            tick.set_ha("right")
    fig.tight_layout()
    fig.savefig(caminho_saida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def grafico_comparacao_evolucao(dados: dict, emocao: str, caminho_saida: str) -> None:
    tabela = tabela_serie_media(dados)
    fig, ax = plt.subplots(figsize=(9, 5))
    for (cenario, grupo), cor in zip(tabela.groupby("cenario", sort=False), PALETA):
        ax.plot(grupo["ciclo"], grupo[emocao], marker="o", markersize=3,
                linewidth=1.8, color=cor, label=cenario)
    ax.set_xlabel("Ciclo")
    ax.set_ylabel(f"Intensidade média de {emocao}")
    ax.set_title(f"Evolução de '{emocao}' — rede real x sintéticas")
    ax.legend()
    ax.grid(True, linestyle="--", alpha=0.4)
    fig.tight_layout()
    fig.savefig(caminho_saida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def teste_distribuicao_final(dados: dict):
    """
    Qui-quadrado comparando a distribuição final de rótulos dominantes
    entre todos os cenários (tabela cenário x rótulo) — indica se a
    diferença observada entre a rede real e as sintéticas é
    estatisticamente significativa, não apenas ruído de uma única
    execução aleatória.
    """
    linhas = []
    for cenario in dados["cenarios"]:
        linha = dict(cenario["propagacao"]["distribuicaoFinal"])
        linha["cenario"] = cenario["nome"]
        linhas.append(linha)
    tabela = pd.DataFrame(linhas).set_index("cenario").fillna(0)
    tabela = tabela.loc[:, (tabela.sum(axis=0) > 0)]
    estatistica, p_valor, _, _ = stats.chi2_contingency(tabela)
    return estatistica, p_valor, tabela


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("json", help="Caminho do comparacao.json")
    parser.add_argument("--saida", default=".", help="Pasta onde salvar os gráficos")
    parser.add_argument("--emocao", default=None,
                         help="Emoção a comparar na evolução (padrão: a mais semeada na rede real)")
    args = parser.parse_args()

    os.makedirs(args.saida, exist_ok=True)
    dados = carregar(args.json)

    print("=== Métricas estruturais ===")
    print(tabela_estrutura(dados).to_string())

    emocao = args.emocao
    if emocao is None:
        dist_inicial = dados["cenarios"][0]["propagacao"]["distribuicaoInicial"]
        candidatos = {k: v for k, v in dist_inicial.items() if k != "NEUTRO"}
        emocao = max(candidatos, key=candidatos.get).lower() if candidatos else "joy"

    grafico_comparacao_estrutura(dados, os.path.join(args.saida, "comparacao_estrutura.png"))
    grafico_comparacao_evolucao(dados, emocao, os.path.join(args.saida, f"comparacao_evolucao_{emocao}.png"))

    print(f"\n=== Teste qui-quadrado: a distribuição final difere entre cenários? "
          f"(emoção de referência: {emocao}) ===")
    estatistica, p_valor, tabela = teste_distribuicao_final(dados)
    print(tabela.to_string())
    print(f"\nqui² = {estatistica:.3f}, p-valor = {p_valor:.5f}")
    if p_valor < 0.05:
        print("=> Diferença estatisticamente significativa (p < 0.05): a topologia parece "
              "influenciar o resultado final da propagação, não é só ruído.")
    else:
        print("=> Sem evidência estatística de diferença (p >= 0.05) com os dados disponíveis.")

    print(f"\nGráficos salvos em: {args.saida}")


if __name__ == "__main__":
    main()
