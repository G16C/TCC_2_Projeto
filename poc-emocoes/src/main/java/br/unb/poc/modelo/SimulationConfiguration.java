package br.unb.poc.modelo;

import br.unb.poc.modelo.topologia.TipoTopologia;
import br.unb.poc.dados.FonteDados;
import br.unb.poc.modelo.emocao.Emotion;

/**
 * Configuração imutável de uma execução da simulação.
 *
 * <p>Esta classe substitui gradualmente a configuração global da PoC por
 * uma configuração explícita de cenário. Isso permite que, nas próximas
 * etapas, diferentes execuções sejam criadas pela interface sem alterar
 * constantes globais do processo.</p>
 */
public final class SimulationConfiguration {

    private final int numAgentes;
    private final FonteDados fonteDados;
    private final String twitterDatasetPath;
    private final String twitterEgoId;
    private final String twitterSeedNodeId;
    private final int vizinhosAnel;
    private final double probReconexao;
    private final double pesoConexao;
    private final TipoTopologia tipoTopologia;
    private final int scaleFreeConexoesIniciais;
    private final int modularComunidades;
    private final double modularProbabilidadeInterna;
    private final double modularProbabilidadeExterna;
    private final double alpha;
    private final double delta;
    private final double estadoInicialSemente;
    private final double estadoInicialPadrao;
    private final Emotion emocaoSemente;
    private final int numCiclos;
    private final long intervaloCicloMs;
    private final long semente;
    private final String nomeAgenteControle;
    private final String prefixoUsuario;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean logDetalhado;

    private SimulationConfiguration(Builder builder) {
        this.numAgentes = builder.numAgentes;
        this.fonteDados = builder.fonteDados;
        this.twitterDatasetPath = builder.twitterDatasetPath;
        this.twitterEgoId = builder.twitterEgoId;
        this.twitterSeedNodeId = builder.twitterSeedNodeId;
        this.vizinhosAnel = builder.vizinhosAnel;
        this.probReconexao = builder.probReconexao;
        this.pesoConexao = builder.pesoConexao;
        this.tipoTopologia = builder.tipoTopologia;
        this.scaleFreeConexoesIniciais = builder.scaleFreeConexoesIniciais;
        this.modularComunidades = builder.modularComunidades;
        this.modularProbabilidadeInterna = builder.modularProbabilidadeInterna;
        this.modularProbabilidadeExterna = builder.modularProbabilidadeExterna;
        this.alpha = builder.alpha;
        this.delta = builder.delta;
        this.estadoInicialSemente = builder.estadoInicialSemente;
        this.estadoInicialPadrao = builder.estadoInicialPadrao;
        this.emocaoSemente = builder.emocaoSemente;
        this.numCiclos = builder.numCiclos;
        this.intervaloCicloMs = builder.intervaloCicloMs;
        this.semente = builder.semente;
        this.nomeAgenteControle = builder.nomeAgenteControle;
        this.prefixoUsuario = builder.prefixoUsuario;
        this.dbUrl = builder.dbUrl;
        this.dbUser = builder.dbUser;
        this.dbPassword = builder.dbPassword;
        this.logDetalhado = builder.logDetalhado;
        validar();
    }

    public static SimulationConfiguration padrao() {
        return builder()
                .numAgentes(ConfiguracaoPoc.NUM_AGENTES)
                .fonteDados(FonteDados.SINTETICA)
                .twitterDatasetPath(ConfiguracaoPoc.TWITTER_DATASET_PATH)
                .twitterEgoId(ConfiguracaoPoc.TWITTER_EGO_ID)
                .twitterSeedNodeId(ConfiguracaoPoc.TWITTER_SEED_NODE_ID)
                .vizinhosAnel(ConfiguracaoPoc.VIZINHOS_ANEL)
                .probReconexao(ConfiguracaoPoc.PROB_RECONEXAO)
                .pesoConexao(ConfiguracaoPoc.PESO_CONEXAO)
                .tipoTopologia(ConfiguracaoPoc.TIPO_TOPOLOGIA_ATUAL)
                .scaleFreeConexoesIniciais(ConfiguracaoPoc.SCALE_FREE_CONEXOES_INICIAIS)
                .modularComunidades(ConfiguracaoPoc.MODULAR_COMUNIDADES)
                .modularProbabilidadeInterna(ConfiguracaoPoc.MODULAR_PROB_INTERNA)
                .modularProbabilidadeExterna(ConfiguracaoPoc.MODULAR_PROB_EXTERNA)
                .alpha(ConfiguracaoPoc.ALPHA)
                .delta(ConfiguracaoPoc.DELTA)
                .estadoInicialSemente(ConfiguracaoPoc.ESTADO_INICIAL_SEMENTE)
                .estadoInicialPadrao(ConfiguracaoPoc.ESTADO_INICIAL_PADRAO)
                .emocaoSemente(Emotion.ANGER)
                .numCiclos(ConfiguracaoPoc.NUM_CICLOS)
                .intervaloCicloMs(ConfiguracaoPoc.INTERVALO_CICLO_MS)
                .semente(ConfiguracaoPoc.SEMENTE_SIMULACAO)
                .nomeAgenteControle(ConfiguracaoPoc.NOME_AGENTE_CONTROLE)
                .prefixoUsuario(ConfiguracaoPoc.PREFIXO_USUARIO)
                .dbUrl(ConfiguracaoPoc.DB_URL)
                .dbUser(ConfiguracaoPoc.DB_USER)
                .dbPassword(ConfiguracaoPoc.DB_PASSWORD)
                .logDetalhado(false)
                .build();
    }

    /** Cria uma configuração pronta para executar uma rede real do Twitter Social Circles. */
    public static SimulationConfiguration twitter(String datasetPath, String egoId) {
        return builder()
                .fonteDados(br.unb.poc.dados.FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(br.unb.poc.modelo.topologia.TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath(datasetPath)
                .twitterEgoId(egoId)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    private void validar() {
        if (fonteDados == null) throw new IllegalArgumentException("fonteDados não pode ser nula");
        if (tipoTopologia == null) throw new IllegalArgumentException("tipoTopologia não pode ser nulo");
        if (numAgentes < 2) throw new IllegalArgumentException("numAgentes deve ser >= 2");
        if (vizinhosAnel < 1) throw new IllegalArgumentException("vizinhosAnel deve ser >= 1");
        if (probReconexao < 0 || probReconexao > 1) throw new IllegalArgumentException("probReconexao deve estar em [0,1]");
        if (pesoConexao <= 0) throw new IllegalArgumentException("pesoConexao deve ser > 0");
        if (scaleFreeConexoesIniciais < 1) throw new IllegalArgumentException("scaleFreeConexoesIniciais deve ser >= 1");
        if (modularComunidades < 1 || modularComunidades > numAgentes) throw new IllegalArgumentException("modularComunidades deve estar entre 1 e numAgentes");
        if (modularProbabilidadeInterna < 0 || modularProbabilidadeInterna > 1) throw new IllegalArgumentException("modularProbabilidadeInterna deve estar em [0,1]");
        if (modularProbabilidadeExterna < 0 || modularProbabilidadeExterna > 1) throw new IllegalArgumentException("modularProbabilidadeExterna deve estar em [0,1]");
        if (alpha < 0 || alpha > 1) throw new IllegalArgumentException("alpha deve estar em [0,1]");
        if (delta < 0 || delta > 1) throw new IllegalArgumentException("delta deve estar em [0,1]");
        if (fonteDados == FonteDados.TWITTER_SOCIAL_CIRCLES && (twitterDatasetPath == null || twitterDatasetPath.isBlank())) throw new IllegalArgumentException("twitterDatasetPath é obrigatório para a fonte Twitter");
        if (fonteDados == FonteDados.TWITTER_SOCIAL_CIRCLES && (twitterEgoId == null || twitterEgoId.isBlank())) throw new IllegalArgumentException("twitterEgoId é obrigatório para a fonte Twitter");
        if (emocaoSemente == null) throw new IllegalArgumentException("emocaoSemente não pode ser nula");
        if (numCiclos < 1) throw new IllegalArgumentException("numCiclos deve ser >= 1");
        if (intervaloCicloMs <= 0) throw new IllegalArgumentException("intervaloCicloMs deve ser > 0");
    }

    public int getNumAgentes() { return numAgentes; }
    public FonteDados getFonteDados() { return fonteDados; }
    public String getTwitterDatasetPath() { return twitterDatasetPath; }
    public String getTwitterEgoId() { return twitterEgoId; }
    public String getTwitterSeedNodeId() { return twitterSeedNodeId; }
    public int getVizinhosAnel() { return vizinhosAnel; }
    public double getProbReconexao() { return probReconexao; }
    public double getPesoConexao() { return pesoConexao; }
    public TipoTopologia getTipoTopologia() { return tipoTopologia; }
    public int getScaleFreeConexoesIniciais() { return scaleFreeConexoesIniciais; }
    public int getModularComunidades() { return modularComunidades; }
    public double getModularProbabilidadeInterna() { return modularProbabilidadeInterna; }
    public double getModularProbabilidadeExterna() { return modularProbabilidadeExterna; }
    public double getAlpha() { return alpha; }
    public double getDelta() { return delta; }
    public double getEstadoInicialSemente() { return estadoInicialSemente; }
    public double getEstadoInicialPadrao() { return estadoInicialPadrao; }
    public Emotion getEmocaoSemente() { return emocaoSemente; }
    public int getNumCiclos() { return numCiclos; }
    public long getIntervaloCicloMs() { return intervaloCicloMs; }
    public long getSemente() { return semente; }
    public String getNomeAgenteControle() { return nomeAgenteControle; }
    public String getPrefixoUsuario() { return prefixoUsuario; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }

    /**
     * Quando {@code false} (padrão), os agentes evitam imprimir uma linha
     * por ciclo/por agente no console — em datasets grandes (dezenas ou
     * centenas de agentes) isso enche o terminal rapidamente. As mensagens
     * de marco da simulação ({@code AgenteControle}: início, id no banco,
     * contagem de agentes, conclusão) continuam sempre visíveis. Ative com
     * {@code .logDetalhado(true)} no builder, ou {@code --verbose} em
     * {@code LancadorSimulacao}, para depurar ciclo a ciclo.
     */
    public boolean isLogDetalhado() { return logDetalhado; }

    public static final class Builder {
        private int numAgentes = ConfiguracaoPoc.NUM_AGENTES;
        private FonteDados fonteDados = FonteDados.SINTETICA;
        private String twitterDatasetPath = ConfiguracaoPoc.TWITTER_DATASET_PATH;
        private String twitterEgoId = ConfiguracaoPoc.TWITTER_EGO_ID;
        private String twitterSeedNodeId = ConfiguracaoPoc.TWITTER_SEED_NODE_ID;
        private int vizinhosAnel = ConfiguracaoPoc.VIZINHOS_ANEL;
        private double probReconexao = ConfiguracaoPoc.PROB_RECONEXAO;
        private double pesoConexao = ConfiguracaoPoc.PESO_CONEXAO;
        private TipoTopologia tipoTopologia = ConfiguracaoPoc.TIPO_TOPOLOGIA_ATUAL;
        private int scaleFreeConexoesIniciais = ConfiguracaoPoc.SCALE_FREE_CONEXOES_INICIAIS;
        private int modularComunidades = ConfiguracaoPoc.MODULAR_COMUNIDADES;
        private double modularProbabilidadeInterna = ConfiguracaoPoc.MODULAR_PROB_INTERNA;
        private double modularProbabilidadeExterna = ConfiguracaoPoc.MODULAR_PROB_EXTERNA;
        private double alpha = ConfiguracaoPoc.ALPHA;
        private double delta = ConfiguracaoPoc.DELTA;
        private double estadoInicialSemente = ConfiguracaoPoc.ESTADO_INICIAL_SEMENTE;
        private double estadoInicialPadrao = ConfiguracaoPoc.ESTADO_INICIAL_PADRAO;
        private Emotion emocaoSemente = Emotion.ANGER;
        private int numCiclos = ConfiguracaoPoc.NUM_CICLOS;
        private long intervaloCicloMs = ConfiguracaoPoc.INTERVALO_CICLO_MS;
        private long semente = ConfiguracaoPoc.SEMENTE_SIMULACAO;
        private String nomeAgenteControle = ConfiguracaoPoc.NOME_AGENTE_CONTROLE;
        private String prefixoUsuario = ConfiguracaoPoc.PREFIXO_USUARIO;
        private String dbUrl = ConfiguracaoPoc.DB_URL;
        private String dbUser = ConfiguracaoPoc.DB_USER;
        private String dbPassword = ConfiguracaoPoc.DB_PASSWORD;
        private boolean logDetalhado = false;

        public Builder numAgentes(int v) { numAgentes = v; return this; }
        public Builder fonteDados(FonteDados v) { fonteDados = v; return this; }
        public Builder twitterDatasetPath(String v) { twitterDatasetPath = v; return this; }
        public Builder twitterEgoId(String v) { twitterEgoId = v; return this; }
        public Builder twitterSeedNodeId(String v) { twitterSeedNodeId = v; return this; }
        public Builder vizinhosAnel(int v) { vizinhosAnel = v; return this; }
        public Builder probReconexao(double v) { probReconexao = v; return this; }
        public Builder pesoConexao(double v) { pesoConexao = v; return this; }
        public Builder tipoTopologia(TipoTopologia v) { tipoTopologia = v; return this; }
        public Builder scaleFreeConexoesIniciais(int v) { scaleFreeConexoesIniciais = v; return this; }
        public Builder modularComunidades(int v) { modularComunidades = v; return this; }
        public Builder modularProbabilidadeInterna(double v) { modularProbabilidadeInterna = v; return this; }
        public Builder modularProbabilidadeExterna(double v) { modularProbabilidadeExterna = v; return this; }
        public Builder alpha(double v) { alpha = v; return this; }
        public Builder delta(double v) { delta = v; return this; }
        public Builder estadoInicialSemente(double v) { estadoInicialSemente = v; return this; }
        public Builder estadoInicialPadrao(double v) { estadoInicialPadrao = v; return this; }
        public Builder emocaoSemente(Emotion v) { emocaoSemente = v; return this; }
        public Builder numCiclos(int v) { numCiclos = v; return this; }
        public Builder intervaloCicloMs(long v) { intervaloCicloMs = v; return this; }
        public Builder semente(long v) { semente = v; return this; }
        public Builder nomeAgenteControle(String v) { nomeAgenteControle = v; return this; }
        public Builder prefixoUsuario(String v) { prefixoUsuario = v; return this; }
        public Builder dbUrl(String v) { dbUrl = v; return this; }
        public Builder dbUser(String v) { dbUser = v; return this; }
        public Builder dbPassword(String v) { dbPassword = v; return this; }
        public Builder logDetalhado(boolean v) { logDetalhado = v; return this; }

        public SimulationConfiguration build() { return new SimulationConfiguration(this); }
    }
}
