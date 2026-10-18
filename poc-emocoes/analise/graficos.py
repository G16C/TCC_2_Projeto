"""
Gráficos de evolução emocional a partir do histórico de uma simulação
(DataFrame no formato retornado por `db.carregar_estados`).

Substitui o script original `analise_emocoes.py` da primeira versão da
PoC, que (a) só lia a coluna `valor` (modelo escalar antigo, hoje a PoC
usa quatro dimensões: anger/disgust/joy/sadness) e (b) quebrava com
`IndexError` para redes com mais de 10 agentes, por usar paletas de
cores/estilos/marcadores de tamanho fixo (10) para desenhar uma curva
por agente.
"""
import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

CORES_EMOCAO = {
    "anger": "#e0483e",
    "disgust": "#5a8f4f",
    "joy": "#e8b933",
    "sadness": "#3f7ab0",
}
ROTULOS_EMOCAO = {
    "anger": "raiva",
    "disgust": "nojo",
    "joy": "alegria",
    "sadness": "tristeza",
}
CORES_CATEGORIA = {
    "ANGER": CORES_EMOCAO["anger"],
    "DISGUST": CORES_EMOCAO["disgust"],
    "JOY": CORES_EMOCAO["joy"],
    "SADNESS": CORES_EMOCAO["sadness"],
    "NEUTRO": "#5b6070",
}


def grafico_evolucao_media(df: pd.DataFrame, caminho_saida: str, titulo: str | None = None) -> None:
    """
    Média ± desvio-padrão de cada emoção ao longo dos ciclos, agregando
    todos os agentes. Funciona para qualquer número de agentes.
    """
    emocoes = ["anger", "disgust", "joy", "sadness"]
    resumo = df.groupby("ciclo")[emocoes].agg(["mean", "std"])

    fig, ax = plt.subplots(figsize=(10, 5.5))
    for emocao in emocoes:
        media = resumo[(emocao, "mean")]
        desvio = resumo[(emocao, "std")].fillna(0)
        ax.plot(media.index, media.values, color=CORES_EMOCAO[emocao],
                linewidth=2, label=ROTULOS_EMOCAO[emocao])
        ax.fill_between(media.index, media - desvio, media + desvio,
                         color=CORES_EMOCAO[emocao], alpha=0.15)

    ax.set_xlabel("Ciclo")
    ax.set_ylabel("Intensidade emocional (média ± desvio-padrão)")
    ax.set_title(titulo or "Evolução emocional média da rede")
    ax.grid(True, linestyle="--", alpha=0.4)
    ax.legend()
    fig.tight_layout()
    fig.savefig(caminho_saida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def grafico_distribuicao_categoria(df: pd.DataFrame, caminho_saida: str, titulo: str | None = None) -> None:
    """Área empilhada com a contagem de agentes por rótulo dominante em cada ciclo."""
    tabela = df.groupby(["ciclo", "categoria"]).size().unstack(fill_value=0).sort_index()
    colunas = list(tabela.columns)
    cores = [CORES_CATEGORIA.get(c, "#888888") for c in colunas]

    fig, ax = plt.subplots(figsize=(10, 5))
    ax.stackplot(tabela.index, tabela.T.values, labels=colunas, colors=cores, alpha=0.85)
    ax.set_xlabel("Ciclo")
    ax.set_ylabel("Número de agentes")
    ax.set_title(titulo or "Distribuição do rótulo dominante ao longo dos ciclos")
    ax.legend(loc="upper left", bbox_to_anchor=(1.01, 1))
    fig.tight_layout()
    fig.savefig(caminho_saida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def grafico_trajetorias_agentes(
    df: pd.DataFrame,
    caminho_saida: str,
    emocao: str = "anger",
    max_agentes: int = 15,
    titulo: str | None = None,
    semente: int = 42,
) -> None:
    """
    Trajetória individual de até `max_agentes` agentes (amostrados
    aleatoriamente, com semente fixa para reprodutibilidade) para uma
    única emoção — o equivalente moderno do gráfico "todos os agentes"
    do script original, mas que não quebra com mais de 10 agentes: em
    vez de uma cor/estilo fixo por agente, amostra um subconjunto legível.
    """
    agentes = df["id_agente"].unique()
    rng = np.random.default_rng(semente)
    tamanho_amostra = min(max_agentes, len(agentes))
    amostrados = sorted(rng.choice(agentes, size=tamanho_amostra, replace=False).tolist())

    fig, ax = plt.subplots(figsize=(10, 5.5))
    cmap = plt.cm.viridis(np.linspace(0, 1, len(amostrados)))
    for cor, agente in zip(cmap, amostrados):
        serie = df[df["id_agente"] == agente].sort_values("ciclo")
        ax.plot(serie["ciclo"], serie[emocao], color=cor, linewidth=1.4,
                marker="o", markersize=3, label=f"agente {agente}")

    ax.set_xlabel("Ciclo")
    ax.set_ylabel(f"Intensidade de {ROTULOS_EMOCAO.get(emocao, emocao)}")
    ax.set_title(titulo or (
        f"Trajetórias individuais — {ROTULOS_EMOCAO.get(emocao, emocao)} "
        f"(amostra de {len(amostrados)}/{len(agentes)} agentes)"
    ))
    ax.grid(True, linestyle="--", alpha=0.4)
    if len(amostrados) <= 20:
        ax.legend(fontsize=7, ncol=2, loc="upper left", bbox_to_anchor=(1.01, 1))
    fig.tight_layout()
    fig.savefig(caminho_saida, dpi=200, bbox_inches="tight")
    plt.close(fig)
