-- ================================================================
-- Script de inicialização do banco de dados da PoC
-- Propagação de Emoções em Redes Sociais Digitais
-- TCC - Engenharia de Software, UnB, 2026
-- Autor: Gabriel Campello Marques
-- ================================================================
-- Uso:
--   psql -U postgres -c "CREATE DATABASE poc_emocoes;"
--   psql -U postgres -d poc_emocoes -f init_db.sql
-- ================================================================

-- Tabela: simulacao
-- Registra os metadados de cada execução da simulação.

CREATE TABLE IF NOT EXISTS simulacao (
    id          SERIAL       PRIMARY KEY,
    iniciada_em TIMESTAMP    NOT NULL DEFAULT NOW(),
    num_agentes INTEGER      NOT NULL,
    num_ciclos  INTEGER      NOT NULL,
    alpha       DOUBLE PRECISION NOT NULL,
    delta       DOUBLE PRECISION NOT NULL,
    fonte_dados VARCHAR(50),
    twitter_ego_id VARCHAR(64)
);

COMMENT ON TABLE  simulacao              IS 'Metadados de cada execução da simulação da PoC';
COMMENT ON COLUMN simulacao.id          IS 'Identificador único da execução';
COMMENT ON COLUMN simulacao.iniciada_em IS 'Timestamp de início da execução';
COMMENT ON COLUMN simulacao.num_agentes IS 'Número de agentes na rede simulada';
COMMENT ON COLUMN simulacao.num_ciclos  IS 'Número de ciclos executados';
COMMENT ON COLUMN simulacao.alpha       IS 'Taxa de persistência emocional (α)';
COMMENT ON COLUMN simulacao.delta       IS 'Taxa de decaimento emocional (δ)';

-- Tabela: estado_emocional
-- Armazena o estado emocional de cada agente ao final de cada ciclo.
CREATE TABLE IF NOT EXISTS estado_emocional (
    id           SERIAL           PRIMARY KEY,
    simulacao_id INTEGER          NOT NULL REFERENCES simulacao(id) ON DELETE CASCADE,
    ciclo        INTEGER          NOT NULL,
    id_agente    INTEGER          NOT NULL,
    id_externo   VARCHAR(128),
    valor        DOUBLE PRECISION NOT NULL,
    categoria    VARCHAR(20)      NOT NULL,
    anger       DOUBLE PRECISION NOT NULL DEFAULT 0,
    disgust     DOUBLE PRECISION NOT NULL DEFAULT 0,
    joy         DOUBLE PRECISION NOT NULL DEFAULT 0,
    sadness     DOUBLE PRECISION NOT NULL DEFAULT 0
);

COMMENT ON TABLE  estado_emocional              IS 'Estado emocional de cada agente por ciclo';
COMMENT ON COLUMN estado_emocional.simulacao_id IS 'Referência à execução da simulação';
COMMENT ON COLUMN estado_emocional.ciclo        IS 'Número do ciclo de simulação (0 = estado inicial)';
COMMENT ON COLUMN estado_emocional.id_agente    IS 'Índice local do agente na rede (0 a N-1)';
COMMENT ON COLUMN estado_emocional.id_externo   IS 'Identificador original do nó na fonte externa, quando disponível';
COMMENT ON COLUMN estado_emocional.valor        IS 'Valor contínuo do estado emocional [-1.0, 1.0]';
COMMENT ON COLUMN estado_emocional.categoria    IS 'Emoção dominante no modelo multidimensional';
COMMENT ON COLUMN estado_emocional.anger       IS 'Intensidade de raiva [0,1]';
COMMENT ON COLUMN estado_emocional.disgust     IS 'Intensidade de nojo [0,1]';
COMMENT ON COLUMN estado_emocional.joy         IS 'Intensidade de alegria [0,1]';
COMMENT ON COLUMN estado_emocional.sadness     IS 'Intensidade de tristeza [0,1]';

-- Índices para consultas analíticas
CREATE INDEX IF NOT EXISTS idx_ee_simulacao_ciclo
    ON estado_emocional (simulacao_id, ciclo);

CREATE INDEX IF NOT EXISTS idx_ee_simulacao_agente
    ON estado_emocional (simulacao_id, id_agente);

-- 1. Distribuição de categorias por ciclo (para a simulação mais recente)
-- SELECT ciclo,
--        categoria,
--        COUNT(*) AS qtd_agentes,
--        ROUND(AVG(valor)::numeric, 4) AS media_valor
-- FROM estado_emocional
-- WHERE simulacao_id = (SELECT MAX(id) FROM simulacao)
-- GROUP BY ciclo, categoria
-- ORDER BY ciclo, categoria;

-- 2. Evolução do estado emocional médio da rede ao longo dos ciclos
-- SELECT ciclo,
--        ROUND(AVG(valor)::numeric, 4) AS media_rede
-- FROM estado_emocional
-- WHERE simulacao_id = (SELECT MAX(id) FROM simulacao)
-- GROUP BY ciclo
-- ORDER BY ciclo;

-- 3. Histórico de um agente específico (ex.: Agente 0 — a semente)
-- SELECT ciclo, valor, categoria
-- FROM estado_emocional
-- WHERE simulacao_id = (SELECT MAX(id) FROM simulacao)
--   AND id_agente = 0
-- ORDER BY ciclo;
