package br.unb.poc.modelo.topologia;

import br.unb.poc.modelo.SimulationConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gera uma rede modular ponderada.
 *
 * <p>Os nós são distribuídos entre comunidades e cada par de nós recebe
 * uma aresta segundo duas probabilidades: uma maior probabilidade para
 * pares pertencentes à mesma comunidade e uma menor probabilidade para
 * pares pertencentes a comunidades diferentes. Esse mecanismo produz
 * comunidades densas e baixa interconexão entre comunidades, permitindo
 * representar bolhas informacionais e câmaras de eco.</p>
 */
public class TopologiaModular implements GeradorTopologia {

    @Override
    public List<Map<Integer, Double>> gerar(
            int numAgentes,
            Random random,
            SimulationConfiguration configuracao) {

        List<Map<Integer, Double>> vizinhos = new ArrayList<>();
        for (int i = 0; i < numAgentes; i++) {
            vizinhos.add(new HashMap<>());
        }

        int comunidades = Math.min(configuracao.getModularComunidades(), numAgentes);
        double pInterna = configuracao.getModularProbabilidadeInterna();
        double pExterna = configuracao.getModularProbabilidadeExterna();
        double peso = configuracao.getPesoConexao();

        int[] comunidade = distribuirComunidades(numAgentes, comunidades);

        for (int i = 0; i < numAgentes; i++) {
            for (int j = i + 1; j < numAgentes; j++) {
                double probabilidade = comunidade[i] == comunidade[j]
                        ? pInterna
                        : pExterna;

                if (random.nextDouble() < probabilidade) {
                    vizinhos.get(i).put(j, peso);
                    vizinhos.get(j).put(i, peso);
                }
            }
        }

        return vizinhos;
    }

    /** Distribuição balanceada dos nós entre as comunidades. */
    private int[] distribuirComunidades(int numAgentes, int comunidades) {
        int[] resultado = new int[numAgentes];
        for (int i = 0; i < numAgentes; i++) {
            resultado[i] = i % comunidades;
        }
        return resultado;
    }
}
