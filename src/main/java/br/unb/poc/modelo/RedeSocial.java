package br.unb.poc.modelo;

import br.unb.poc.modelo.topologia.GeradorTopologia;
import br.unb.poc.modelo.topologia.TopologiaSmallWorld;
import br.unb.poc.dados.TwitterNetworkData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Representa a estrutura topológica da rede social simulada.
 *
 * <p>A rede é modelada como um grafo não-dirigido ponderado G = (V, E, W).
 * A construção efetiva do grafo é delegada a um {@link GeradorTopologia},
 * permitindo alternar entre diferentes modelos de rede (Small-World,
 * Scale-Free, fonte externa de dados) sem qualquer alteração nesta classe
 * ou na lógica dos agentes — conforme discutido na seção 5.2.2 da
 * monografia.
 *
 * <p>Por compatibilidade com a Prova de Conceito original (TCC I), o
 * construtor {@link #RedeSocial(long)} usa a topologia Small-World como
 * padrão. Para escolher outra topologia, utilize
 * {@link #RedeSocial(long, GeradorTopologia)}.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public class RedeSocial {

    /**
     * Lista de adjacência ponderada: adjacencia.get(i) contém, para cada
     * vizinho j do agente i, o peso w_ij da conexão.
     */
    private final List<Map<Integer, Double>> adjacencia;

    private final int numAgentes;
    private final SimulationConfiguration configuracao;
    private final List<String> idsExternos;
    private final TwitterNetworkData dadosTwitter;

    /**
     * Constrói a rede social usando a topologia Small-World (padrão),
     * conforme os parâmetros definidos em {@link ConfiguracaoPoc}.
     *
     * @param semente semente do gerador de números aleatórios (para reprodutibilidade)
     */
    public RedeSocial(long semente) {
        this(semente, new TopologiaSmallWorld(), SimulationConfiguration.padrao());
    }

    /**
     * Constrói a rede social usando o gerador de topologia informado.
     *
     * @param semente  semente do gerador de números aleatórios (para reprodutibilidade)
     * @param gerador  estratégia de geração da topologia (Small-World, Scale-Free,
     *                 fonte externa, etc.)
     */
    public RedeSocial(long semente, GeradorTopologia gerador) {
        this(semente, gerador, SimulationConfiguration.padrao());
    }

    /** Constrói a rede usando explicitamente a configuração do cenário. */
    public RedeSocial(long semente, GeradorTopologia gerador, SimulationConfiguration configuracao) {
        this.configuracao = configuracao;
        Random random = new Random(semente);
        List<Map<Integer, Double>> gerada = gerador.gerar(configuracao.getNumAgentes(), random, configuracao);
        this.adjacencia = gerada;

        if (gerador instanceof br.unb.poc.modelo.topologia.TopologiaFonteExterna fonte
                && fonte.getDadosTwitter() != null) {
            this.dadosTwitter = fonte.getDadosTwitter();
            this.numAgentes = dadosTwitter.getNumAgentes();
            this.idsExternos = dadosTwitter.getIdsExternos();
        } else {
            this.dadosTwitter = null;
            this.numAgentes = gerada.size();
            this.idsExternos = criarIdsLocais(this.numAgentes);
        }
    }

    /** Constrói a rede a partir da configuração, escolhendo a topologia via fábrica. */
    public RedeSocial(SimulationConfiguration configuracao) {
        this(configuracao.getSemente(),
                br.unb.poc.modelo.topologia.GeradorTopologiaFactory.criar(configuracao.getTipoTopologia()),
                configuracao);
    }

    /** Constrói a rede diretamente a partir de uma rede real do Twitter. */
    public RedeSocial(SimulationConfiguration configuracao, TwitterNetworkData dadosTwitter) {
        if (dadosTwitter == null) throw new IllegalArgumentException("dadosTwitter não pode ser nulo");
        this.configuracao = configuracao;
        this.numAgentes = dadosTwitter.getNumAgentes();
        this.adjacencia = dadosTwitter.getAdjacencia();
        this.idsExternos = dadosTwitter.getIdsExternos();
        this.dadosTwitter = dadosTwitter;
    }

    private List<String> criarIdsLocais(int quantidade) {
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < quantidade; i++) ids.add(String.valueOf(i));
        return Collections.unmodifiableList(ids);
    }

    public SimulationConfiguration getConfiguracao() {
        return configuracao;
    }

    /** Retorna o identificador original do nó na fonte externa, quando disponível. */
    public String getIdExterno(int idAgente) {
        return idsExternos.get(idAgente);
    }

    public boolean usaDadosTwitter() {
        return dadosTwitter != null;
    }

    public TwitterNetworkData getDadosTwitter() {
        return dadosTwitter;
    }

    /** Localiza um agente a partir do identificador original da fonte externa. */
    public int getIdLocal(String idExterno) {
        return idsExternos.indexOf(idExterno);
    }

    /** Retorna o vetor de atributos do nó Twitter, quando disponível. */
    public int[] getFeaturesTwitter(int idAgente) {
        if (dadosTwitter == null) return new int[0];
        int[] vetor = dadosTwitter.getFeatures().get(getIdExterno(idAgente));
        return vetor == null ? new int[0] : java.util.Arrays.copyOf(vetor, vetor.length);
    }

    /**
     * Retorna as features ativas de um usuário Twitter em formato legível,
     * associando o nome da feature ao valor binário observado.
     */
    public Map<String, Integer> getFeaturesAtivasTwitter(int idAgente) {
        if (dadosTwitter == null) return Map.of();
        int[] vetor = getFeaturesTwitter(idAgente);
        List<String> nomes = getNomesFeaturesTwitter();
        Map<String, Integer> resultado = new java.util.LinkedHashMap<>();
        int limite = Math.min(vetor.length, nomes.size());
        for (int i = 0; i < limite; i++) {
            if (vetor[i] > 0) resultado.put(nomes.get(i), vetor[i]);
        }
        return Collections.unmodifiableMap(resultado);
    }

    /** Retorna os nomes das features do dataset Twitter. */
    public List<String> getNomesFeaturesTwitter() {
        return dadosTwitter == null ? List.of() : dadosTwitter.getNomesFeatures();
    }

    /**
     * Retorna o conjunto de vizinhos do agente com índice {@code idAgente}.
     *
     * @param idAgente índice do agente (0 a numAgentes-1)
     * @return conjunto imutável de índices dos vizinhos
     */
    public Set<Integer> getVizinhos(int idAgente) {
        return Collections.unmodifiableSet(adjacencia.get(idAgente).keySet());
    }

    /**
     * Retorna os vizinhos do agente com índice {@code idAgente} junto com
     * os pesos {@code w_ij} de cada conexão, conforme a Equação 5.2 da
     * monografia (influência emocional agregada ponderada).
     *
     * @param idAgente índice do agente (0 a numAgentes-1)
     * @return mapa imutável {@code idVizinho -> peso}
     */
    public Map<Integer, Double> getVizinhosComPesos(int idAgente) {
        return Collections.unmodifiableMap(adjacencia.get(idAgente));
    }

    /**
     * Retorna o peso da conexão entre dois agentes, ou {@code 0.0} caso
     * não exista conexão entre eles.
     *
     * @param idAgente  índice do agente de origem
     * @param idVizinho índice do agente vizinho
     * @return peso w_ij da conexão
     */
    public double getPeso(int idAgente, int idVizinho) {
        return adjacencia.get(idAgente).getOrDefault(idVizinho, 0.0);
    }

    /**
     * Retorna o número total de agentes na rede.
     *
     * @return número de nós
     */
    public int getNumAgentes() {
        return numAgentes;
    }

    /**
     * Retorna uma representação textual da lista de adjacência,
     * útil para depuração e geração de logs.
     *
     * @return string com a estrutura da rede
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Rede Social:\n");
        for (int i = 0; i < numAgentes; i++) {
            sb.append(String.format("  Agente %2d -> %s%n", i, adjacencia.get(i).keySet()));
        }
        return sb.toString();
    }
}
