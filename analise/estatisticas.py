"""Estatísticas descritivas e testes simples sobre o histórico de uma simulação."""
import pandas as pd
from scipy import stats

EMOCOES = ["anger", "disgust", "joy", "sadness"]


def resumo_por_ciclo(df: pd.DataFrame) -> pd.DataFrame:
    """Média, desvio-padrão, mínimo e máximo de cada emoção, por ciclo."""
    agregados = df.groupby("ciclo")[EMOCOES].agg(["mean", "std", "min", "max"])
    agregados.columns = ["_".join(coluna) for coluna in agregados.columns]
    return agregados.reset_index()


def resumo_geral(df: pd.DataFrame) -> pd.DataFrame:
    """Estatísticas descritivas por emoção, agregando todos os ciclos e agentes."""
    return df[EMOCOES].describe().T


def distribuicao_categoria_por_ciclo(df: pd.DataFrame) -> pd.DataFrame:
    """Contagem de agentes por rótulo dominante (categoria) em cada ciclo."""
    return df.groupby(["ciclo", "categoria"]).size().unstack(fill_value=0).sort_index()


def teste_mudanca_distribuicao(df: pd.DataFrame):
    """
    Teste qui-quadrado comparando a distribuição de rótulos dominantes no
    primeiro e no último ciclo — checa estatisticamente se a propagação
    de fato mudou a composição emocional da rede
    (H0: a distribuição de categorias é a mesma no início e no fim).

    Retorna (estatistica_qui2, p_valor, tabela_contingencia).
    """
    primeiro, ultimo = df["ciclo"].min(), df["ciclo"].max()
    tabela = distribuicao_categoria_por_ciclo(df).loc[[primeiro, ultimo]]
    # chi2_contingency exige que nenhuma coluna seja totalmente zero
    tabela = tabela.loc[:, (tabela.sum(axis=0) > 0)]
    estatistica, p_valor, _, _ = stats.chi2_contingency(tabela)
    return estatistica, p_valor, tabela
