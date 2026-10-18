package br.unb.poc.modelo;

import br.unb.poc.modelo.topologia.TipoTopologia;

/**
 * Configuração centralizada dos parâmetros da Prova de Conceito.
 *
 * <p>Todos os parâmetros descritos na seção 5.2.3 (Projeto da PoC)
 * do Capítulo 5 da monografia estão definidos aqui, permitindo
 * reprodutibilidade e facilidade de ajuste para experimentos futuros.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */
public final class ConfiguracaoPoc {

    /** Número total de agentes (nós) na rede simulada. */
    public static final int NUM_AGENTES = 10;

    /**
     * Número de vizinhos imediatos de cada lado no anel base
     * (antes das conexões aleatórias Small-World).
     */
    public static final int VIZINHOS_ANEL = 2;

    /** Probabilidade de reconexão aleatória (parâmetro p do modelo Small-World). */
    public static final double PROB_RECONEXAO = 0.3;

    /** Peso uniforme de todas as conexões da rede (usado pelas topologias sintéticas). */
    public static final double PESO_CONEXAO = 1.0;

    /**
     * Tipo de topologia usado na simulação atual.
     *
     * <p>Alterne entre {@link TipoTopologia#SMALL_WORLD},
     * {@link TipoTopologia#SCALE_FREE} e {@link TipoTopologia#FONTE_EXTERNA}
     * para trocar o modelo de rede sem alterar código em {@code AgenteControle}.
     */
    public static final TipoTopologia TIPO_TOPOLOGIA_ATUAL = TipoTopologia.SMALL_WORLD;

    /** Fonte de dados estrutural padrão: rede sintética. */
    public static final String TWITTER_DATASET_PATH = "data/twitter.tar.gz";
    /** Ego utilizado quando a fonte Twitter for selecionada. */
    public static final String TWITTER_EGO_ID = "11681802";
    /** Nó real usado como semente; vazio = primeiro nó do grafo carregado. */
    public static final String TWITTER_SEED_NODE_ID = "";

    /**
     * Número de conexões que cada novo nó estabelece ao entrar na rede,
     * no modelo Scale-Free (parâmetro {@code m} de Barabási-Albert).
     */
    public static final int SCALE_FREE_CONEXOES_INICIAIS = 2;

    /** Parâmetros padrão da topologia Modular (TCC II). */
    public static final int MODULAR_COMUNIDADES = 4;
    public static final double MODULAR_PROB_INTERNA = 0.70;
    public static final double MODULAR_PROB_EXTERNA = 0.03;

    /**
     * Taxa de persistência emocional (α).
     * Controla o quanto cada agente mantém seu estado emocional anterior.
     * Intervalo: [0.0, 1.0]. Valor maior → maior resistência à influência.
     */
    public static final double ALPHA = 0.6;

    /**
     * Taxa de decaimento emocional (δ).
     * Modela a tendência natural de retorno ao estado neutro na ausência
     * de estímulos externos.
     * Intervalo: [0.0, 1.0].
     */
    public static final double DELTA = 0.05;

    /**
     * Estado emocional inicial do agente "semente" (Agente 0).
     * Representa a introdução de um estímulo emocional negativo na rede.
     */
    public static final double ESTADO_INICIAL_SEMENTE = -1.0;

    /**
     * Estado emocional inicial de todos os demais agentes.
     */
    public static final double ESTADO_INICIAL_PADRAO = 0.0;

    /** Número total de ciclos (passos de tempo discretos) da simulação. */
    public static final int NUM_CICLOS = 20;

    /**
     * Intervalo de espera (em milissegundos) entre ciclos da simulação,
     * para garantir sincronização entre os agentes JADE.
     */
    public static final long INTERVALO_CICLO_MS = 500L;

    /** Semente padrão para geração reprodutível da rede. */
    public static final long SEMENTE_SIMULACAO = 42L;

    /** Nome do agente de controle da simulação na plataforma JADE. */
    public static final String NOME_AGENTE_CONTROLE = "ControleSimulacao";

    /** Prefixo do nome dos agentes usuário na plataforma JADE. */
    public static final String PREFIXO_USUARIO = "Usuario";


    /** URL de conexão JDBC com o PostgreSQL. */
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/poc_emocoes";

    /** Usuário do banco de dados. */
    public static final String DB_USER = "postgres";

    /** Senha do banco de dados. */
    public static final String DB_PASSWORD = "postgres";

    /** Construtor privado: classe utilitária, não deve ser instanciada. */
    private ConfiguracaoPoc() {}
}
