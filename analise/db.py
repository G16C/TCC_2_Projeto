"""
Conexão e consultas ao banco da PoC de propagação de emoções.

Credenciais lidas de variáveis de ambiente (com os mesmos padrões usados
pela aplicação Java, em ConfiguracaoPoc.java), para não deixar segredo
nenhum hardcoded:

    POC_DB_HOST, POC_DB_NAME, POC_DB_USER, POC_DB_PASSWORD, POC_DB_PORT
"""
import os

import pandas as pd
import psycopg2

DB_CONFIG = {
    "host": os.environ.get("POC_DB_HOST", "localhost"),
    "dbname": os.environ.get("POC_DB_NAME", "poc_emocoes"),
    "user": os.environ.get("POC_DB_USER", "postgres"),
    "password": os.environ.get("POC_DB_PASSWORD", "postgres"),
    "port": int(os.environ.get("POC_DB_PORT", "5432")),
}


def conectar():
    """Abre uma nova conexão com o banco da PoC."""
    return psycopg2.connect(**DB_CONFIG)


def listar_simulacoes(limite: int = 20) -> pd.DataFrame:
    """Últimas simulações registradas, mais recente primeiro."""
    sql = """
        SELECT id, iniciada_em, num_agentes, num_ciclos, alpha, delta,
               fonte_dados, twitter_ego_id
        FROM simulacao
        ORDER BY id DESC
        LIMIT %s;
    """
    with conectar() as conexao:
        return pd.read_sql(sql, conexao, params=(limite,))


def ultima_simulacao_id() -> int:
    """ID da simulação mais recente registrada no banco."""
    with conectar() as conexao, conexao.cursor() as cursor:
        cursor.execute("SELECT MAX(id) FROM simulacao;")
        (valor,) = cursor.fetchone()
        if valor is None:
            raise RuntimeError(
                "Nenhuma simulação encontrada no banco — rode a simulação "
                "primeiro (ver README.md, 'Simulação completa')."
            )
        return valor


def carregar_metadados(simulacao_id: int) -> pd.Series:
    """Metadados (config) de uma simulação específica."""
    sql = """
        SELECT id, iniciada_em, num_agentes, num_ciclos, alpha, delta,
               fonte_dados, twitter_ego_id
        FROM simulacao WHERE id = %s;
    """
    with conectar() as conexao:
        df = pd.read_sql(sql, conexao, params=(simulacao_id,))
    if df.empty:
        raise RuntimeError(f"simulacao_id={simulacao_id} não encontrada no banco.")
    return df.iloc[0]


def carregar_estados(simulacao_id: int | None = None) -> pd.DataFrame:
    """
    Histórico multidimensional de emoções (anger/disgust/joy/sadness e o
    rótulo `categoria`) de uma simulação, em formato "longo": uma linha
    por (agente, ciclo).

    Se `simulacao_id` for None, usa a simulação mais recente.
    """
    if simulacao_id is None:
        simulacao_id = ultima_simulacao_id()

    sql = """
        SELECT id_agente, id_externo, ciclo, categoria, valor,
               anger, disgust, joy, sadness
        FROM estado_emocional
        WHERE simulacao_id = %s
        ORDER BY id_agente, ciclo;
    """
    with conectar() as conexao:
        df = pd.read_sql(sql, conexao, params=(simulacao_id,))
    if df.empty:
        raise RuntimeError(
            f"Nenhum estado encontrado para simulacao_id={simulacao_id}. "
            "A simulação rodou até o fim?"
        )
    return df
