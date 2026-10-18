package br.unb.poc.modelo.topologia;

import java.util.List;
import java.util.Map;
import java.util.Random;
import br.unb.poc.modelo.SimulationConfiguration;

/**
 * Contrato para geração da estrutura topológica da rede social simulada.
 *
 * <p>Cada implementação é responsável por produzir a lista de adjacência
 * ponderada da rede — um {@code Map<Integer, Double>} por agente, associando
 * cada vizinho ao peso {@code w_ij} da conexão — conforme o modelo de grafo
 * G = (V, E, W) descrito na seção 5.2.2 da monografia.
 *
 * <p>Essa abstração permite que {@link br.unb.poc.modelo.RedeSocial} seja
 * agnóstica ao algoritmo de geração, possibilitando alternar entre topologias
 * (Small-World, Scale-Free, fonte externa de dados) sem qualquer alteração
 * na lógica dos agentes ou no restante do sistema.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public interface GeradorTopologia {

    /**
     * Gera a lista de adjacência ponderada da rede.
     *
     * @param numAgentes número total de agentes (nós) a considerar
     * @param random     gerador de números aleatórios já inicializado com a
     *                   semente de reprodutibilidade da simulação
     * @return lista onde a posição {@code i} contém um mapa
     *         {@code idVizinho -> peso} representando as conexões do agente
     *         {@code i}. A relação é sempre simétrica: se {@code j} está no
     *         mapa de {@code i}, então {@code i} está no mapa de {@code j}.
     */
    List<Map<Integer, Double>> gerar(int numAgentes, Random random, SimulationConfiguration configuracao);
}
