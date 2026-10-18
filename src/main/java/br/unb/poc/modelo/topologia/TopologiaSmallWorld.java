package br.unb.poc.modelo.topologia;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.SimulationConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gera uma topologia <i>Small-World</i> segundo o modelo de Watts e
 * Strogatz (1998): parte-se de um anel onde cada nó se conecta aos
 * {@code k} vizinhos imediatos de cada lado e, em seguida, cada aresta
 * é reconectada aleatoriamente com probabilidade {@code p}, criando os
 * atalhos característicos do modelo.
 *
 * <p>Os pesos de todas as conexões são uniformes ({@code w_ij = PESO_CONEXAO}),
 * conforme os parâmetros definidos em {@link ConfiguracaoPoc}.
 *
 * <p>A reconexão garante que nenhum agente fique isolado (grau zero):
 * uma aresta só é removida se o agente do outro lado ainda possuir ao
 * menos um vizinho após a remoção.
 *
 * <p>Esta classe preserva exatamente o comportamento da implementação
 * original de {@code RedeSocial} (Prova de Conceito, TCC I), apenas
 * reorganizada para atender ao contrato {@link GeradorTopologia}.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public class TopologiaSmallWorld implements GeradorTopologia {

    @Override
    public List<Map<Integer, Double>> gerar(int numAgentes, Random random, SimulationConfiguration configuracao) {
        List<Map<Integer, Double>> vizinhos = new ArrayList<>();
        for (int i = 0; i < numAgentes; i++) {
            vizinhos.add(new HashMap<>());
        }

        construirAnel(vizinhos, numAgentes, configuracao);
        aplicarReconexoes(vizinhos, numAgentes, random, configuracao);

        return vizinhos;
    }

    /**
     * Constrói a estrutura base em anel: cada nó i se conecta aos
     * {@code VIZINHOS_ANEL} nós imediatamente à sua esquerda e direita.
     */
    private void construirAnel(List<Map<Integer, Double>> vizinhos, int numAgentes, SimulationConfiguration configuracao) {
        int k = configuracao.getVizinhosAnel();
        double peso = configuracao.getPesoConexao();
        for (int i = 0; i < numAgentes; i++) {
            for (int delta = 1; delta <= k; delta++) {
                int j = (i + delta) % numAgentes;
                vizinhos.get(i).put(j, peso);
                vizinhos.get(j).put(i, peso);
            }
        }
    }

    /**
     * Aplica reconexões aleatórias com probabilidade {@code PROB_RECONEXAO},
     * introduzindo as ligações de "atalho" características do modelo Small-World.
     *
     * <p>Uma reconexão é descartada (a aresta original é preservada) quando
     * removê-la deixaria o agente do outro lado da aresta ({@code j}) sem
     * nenhum vizinho. Sem essa salvaguarda, um agente pode perder sua única
     * conexão quando é o alvo passivo de reconexões de outros agentes,
     * resultando em um nó isolado (grau zero) — o que não é permitido pelo
     * modelo de Watts e Strogatz, cuja premissa é preservar a conectividade
     * do grafo durante a reconexão.
     */
    private void aplicarReconexoes(List<Map<Integer, Double>> vizinhos, int numAgentes, Random random, SimulationConfiguration configuracao) {
        double p = configuracao.getProbReconexao();
        double peso = configuracao.getPesoConexao();

        for (int i = 0; i < numAgentes; i++) {
            // Copia para evitar ConcurrentModificationException
            List<Integer> atualVizinhos = new ArrayList<>(vizinhos.get(i).keySet());
            for (int j : atualVizinhos) {
                if (j > i && random.nextDouble() < p) {
                    // Não reconecta se isso deixaria o agente j sem nenhum vizinho
                    if (vizinhos.get(j).size() <= 1) {
                        continue;
                    }

                    // Encontra um destino aleatório diferente de i e não já conectado
                    int novoVizinho = random.nextInt(numAgentes);
                    int tentativas = 0;
                    while ((novoVizinho == i || vizinhos.get(i).containsKey(novoVizinho))
                            && tentativas < numAgentes) {
                        novoVizinho = random.nextInt(numAgentes);
                        tentativas++;
                    }
                    if (tentativas < numAgentes) {
                        // Remove a aresta antiga e adiciona a nova
                        vizinhos.get(i).remove(j);
                        vizinhos.get(j).remove(i);
                        vizinhos.get(i).put(novoVizinho, peso);
                        vizinhos.get(novoVizinho).put(i, peso);
                    }
                }
            }
        }
    }
}
