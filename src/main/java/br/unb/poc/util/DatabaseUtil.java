package br.unb.poc.util;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.EstadoEmocional;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;

import java.sql.*;

/**
 * Utilitário responsável pela conexão com o banco de dados PostgreSQL
 * e pelas operações de persistência dos resultados da simulação.
 *
 * <p>O esquema do banco utilizado pela PoC é composto por duas tabelas:
 * <ul>
 *   <li>{@code simulacao}: registra os metadados de cada execução;</li>
 *   <li>{@code estado_emocional}: armazena o estado emocional de cada
 *       agente ao final de cada ciclo.</li>
 * </ul>
 *
 * <p>O DDL de criação das tabelas pode ser executado pelo método
 * {@link #inicializarEsquema()}, chamado automaticamente no início
 * de cada execução.
 *
 * <p><b>Concorrência:</b> a PoC usa uma única {@link Connection} JDBC
 * compartilhada por todos os agentes ({@code UsuarioAgent} roda como
 * threads dentro da mesma JVM). Como uma {@code Connection} não é
 * thread-safe, todos os métodos que a utilizam são {@code synchronized}
 * na classe — isso serializa as escritas no banco (aceitável para o
 * volume desta PoC) e evita erros de "connection has been closed" sob
 * simulações com muitos agentes gravando estado ao mesmo tempo.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */
public class DatabaseUtil {

    private static Connection conexao;

    /** Construtor privado: classe utilitária. */
    private DatabaseUtil() {}

    /**
     * Obtém (ou cria) a conexão singleton com o PostgreSQL.
     *
     * @return {@link Connection} ativa
     * @throws SQLException em caso de falha na conexão
     */
    public static Connection getConexao() throws SQLException {
        return getConexao(SimulationConfiguration.padrao());
    }

    public static synchronized Connection getConexao(SimulationConfiguration configuracao) throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            conexao = DriverManager.getConnection(
                    configuracao.getDbUrl(),
                    configuracao.getDbUser(),
                    configuracao.getDbPassword()
            );
        }
        return conexao;
    }

    /**
     * Cria as tabelas do esquema da PoC caso ainda não existam.
     *
     * <p>DDL executado:
     * <pre>
     * CREATE TABLE IF NOT EXISTS simulacao (
     *     id          SERIAL PRIMARY KEY,
     *     iniciada_em TIMESTAMP DEFAULT NOW(),
     *     num_agentes INTEGER,
     *     num_ciclos  INTEGER,
     *     alpha       DOUBLE PRECISION,
     *     delta       DOUBLE PRECISION
     * );
     *
     * CREATE TABLE IF NOT EXISTS estado_emocional (
     *     id             SERIAL PRIMARY KEY,
     *     simulacao_id   INTEGER REFERENCES simulacao(id),
     *     ciclo          INTEGER,
     *     id_agente      INTEGER,
     *     valor          DOUBLE PRECISION,
     *     categoria      VARCHAR(10)
     * );
     * </pre>
     *
     * @throws SQLException em caso de erro na execução do DDL
     */
    public static synchronized void inicializarEsquema() throws SQLException {
        String ddlSimulacao = """
                CREATE TABLE IF NOT EXISTS simulacao (
                    id          SERIAL PRIMARY KEY,
                    iniciada_em TIMESTAMP DEFAULT NOW(),
                    num_agentes INTEGER,
                    num_ciclos  INTEGER,
                    alpha       DOUBLE PRECISION,
                    delta       DOUBLE PRECISION,
                    fonte_dados VARCHAR(50),
                    twitter_ego_id VARCHAR(64)
                );
                """;

        String ddlEstado = """
                CREATE TABLE IF NOT EXISTS estado_emocional (
                    id           SERIAL PRIMARY KEY,
                    simulacao_id INTEGER REFERENCES simulacao(id),
                    ciclo        INTEGER,
                    id_agente    INTEGER,
                    id_externo   VARCHAR(128),
                    valor        DOUBLE PRECISION,
                    categoria    VARCHAR(10)
                );
                """;

        try (Statement stmt = getConexao().createStatement()) {
            stmt.execute(ddlSimulacao);
            stmt.execute(ddlEstado);
            stmt.execute("ALTER TABLE simulacao ADD COLUMN IF NOT EXISTS fonte_dados VARCHAR(50)");
            stmt.execute("ALTER TABLE simulacao ADD COLUMN IF NOT EXISTS twitter_ego_id VARCHAR(64)");
            stmt.execute("ALTER TABLE estado_emocional ADD COLUMN IF NOT EXISTS id_externo VARCHAR(128)");
            stmt.execute("ALTER TABLE estado_emocional ADD COLUMN IF NOT EXISTS anger DOUBLE PRECISION DEFAULT 0");
            stmt.execute("ALTER TABLE estado_emocional ADD COLUMN IF NOT EXISTS disgust DOUBLE PRECISION DEFAULT 0");
            stmt.execute("ALTER TABLE estado_emocional ADD COLUMN IF NOT EXISTS joy DOUBLE PRECISION DEFAULT 0");
            stmt.execute("ALTER TABLE estado_emocional ADD COLUMN IF NOT EXISTS sadness DOUBLE PRECISION DEFAULT 0");
            stmt.execute("ALTER TABLE estado_emocional ALTER COLUMN categoria TYPE VARCHAR(20)");
        }
    }

    /**
     * Registra uma nova execução de simulação e retorna o ID gerado.
     *
     * @return ID da simulação recém-inserida
     * @throws SQLException em caso de erro na inserção
     */
    public static int registrarSimulacao() throws SQLException {
        return registrarSimulacao(SimulationConfiguration.padrao());
    }

    public static int registrarSimulacao(SimulationConfiguration configuracao) throws SQLException {
        return registrarSimulacao(configuracao, configuracao.getNumAgentes());
    }

    /**
     * Registra uma execução usando a quantidade efetivamente carregada.
     * Isso é necessário para fontes externas, nas quais o número de nós
     * é definido pelo dataset e não por uma constante da configuração.
     */
    public static synchronized int registrarSimulacao(
            SimulationConfiguration configuracao,
            int numAgentesReal) throws SQLException {

        String sql = """
                INSERT INTO simulacao
                    (num_agentes, num_ciclos, alpha, delta, fonte_dados, twitter_ego_id)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        try (PreparedStatement ps = getConexao(configuracao).prepareStatement(sql)) {
            ps.setInt(1, numAgentesReal);
            ps.setInt(2, configuracao.getNumCiclos());
            ps.setDouble(3, configuracao.getAlpha());
            ps.setDouble(4, configuracao.getDelta());
            ps.setString(5, configuracao.getFonteDados().name());
            ps.setString(6, configuracao.getTwitterEgoId());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        }
    }

    /**
     * Persiste o estado emocional de um agente em um determinado ciclo.
     *
     * @param simulacaoId ID da simulação corrente
     * @param ciclo       número do ciclo (1 a NUM_CICLOS)
     * @param idAgente    índice do agente (0 a NUM_AGENTES - 1)
     * @param estado      estado emocional a ser registrado
     * @throws SQLException em caso de erro na inserção
     */
    public static synchronized void salvarEstado(int simulacaoId,
                                    int ciclo,
                                    int idAgente,
                                    EstadoEmocional estado) throws SQLException {
        String sql = """
                INSERT INTO estado_emocional (simulacao_id, ciclo, id_agente, valor, categoria)
                VALUES (?, ?, ?, ?, ?);
                """;

        try (PreparedStatement ps = getConexao().prepareStatement(sql)) {
            ps.setInt(1, simulacaoId);
            ps.setInt(2, ciclo);
            ps.setInt(3, idAgente);
            ps.setDouble(4, estado.getValor());
            ps.setString(5, estado.getCategoria().name());
            ps.executeUpdate();
        }
    }

    /**
     * Persiste o estado emocional multidimensional da etapa 2.
     *
     * <p>Os campos valor e categoria são mantidos para compatibilidade
     * com consultas da PoC: valor representa a maior intensidade e
     * categoria representa a emoção dominante.</p>
     */
    public static void salvarEstado(int simulacaoId,
                                    int ciclo,
                                    int idAgente,
                                    EmotionState estado) throws SQLException {
        salvarEstado(simulacaoId, ciclo, idAgente, null, estado);
    }

    /** Persiste o estado e, quando aplicável, o identificador original do dataset. */
    public static synchronized void salvarEstado(int simulacaoId,
                                    int ciclo,
                                    int idAgente,
                                    String idExterno,
                                    EmotionState estado) throws SQLException {
        String sql = "INSERT INTO estado_emocional "
                + "(simulacao_id, ciclo, id_agente, id_externo, valor, categoria, anger, disgust, joy, sadness) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = getConexao(SimulationConfiguration.padrao()).prepareStatement(sql)) {
            ps.setInt(1, simulacaoId);
            ps.setInt(2, ciclo);
            ps.setInt(3, idAgente);
            ps.setString(4, idExterno);
            ps.setDouble(5, estado.getValor());
            ps.setString(6, estado.getRotuloDominante());
            ps.setDouble(7, estado.getIntensity(Emotion.ANGER));
            ps.setDouble(8, estado.getIntensity(Emotion.DISGUST));
            ps.setDouble(9, estado.getIntensity(Emotion.JOY));
            ps.setDouble(10, estado.getIntensity(Emotion.SADNESS));
            ps.executeUpdate();
        }
    }

    /**
     * Encerra a conexão com o banco de dados.
     */
    public static synchronized void fecharConexao() {
        if (conexao != null) {
            try {
                conexao.close();
            } catch (SQLException e) {
                System.err.println("[DB] Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}
