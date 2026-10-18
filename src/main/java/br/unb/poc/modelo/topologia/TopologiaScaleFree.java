package br.unb.poc.modelo.topologia;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.SimulationConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Gera uma topologia <i>Scale-Free</i> (livre de escala) segundo o modelo
 * de fixação preferencial de Barabási e Albert (1999): a rede evolui por
 * crescimento incremental, em que cada novo nó se conecta a {@code m} nós
 * já existentes, escolhidos com probabilidade proporcional ao grau atual
 * de cada nó (fenômeno de "os ricos ficam mais ricos").
 *
 * <p>Esse mecanismo produz uma distribuição de grau em lei de potência,
 * com poucos nós concentrando um número muito elevado de conexões (hubs)
 * — característica descrita na seção 5.2.2 da monografia como aproximação
 * da estrutura observada em plataformas digitais como Twitter e Instagram.
 *
 * <p>Assim como na topologia Small-World, os pesos das conexões são
 * uniformes ({@code w_ij = PESO_CONEXAO}), definidos em {@link ConfiguracaoPoc}.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public class TopologiaScaleFree implements GeradorTopologia {

    @Override
    public List<Map<Integer, Double>> gerar(int numAgentes, Random random, SimulationConfiguration configuracao) {
        List<Map<Integer, Double>> vizinhos = new ArrayList<>();
        for (int i = 0; i < numAgentes; i++) {
            vizinhos.add(new HashMap<>());
        }

        double peso = configuracao.getPesoConexao();
        int m = configuracao.getScaleFreeConexoesIniciais();

        // Núcleo inicial: grafo completo entre os primeiros nós, garantindo
        // grau inicial suficiente para o processo de fixação preferencial.
        int nucleoInicial = Math.min(numAgentes, Math.max(m + 1, 2));
        for (int i = 0; i < nucleoInicial; i++) {
            for (int j = i + 1; j < nucleoInicial; j++) {
                conectar(vizinhos, i, j, peso);
            }
        }

        // Lista de "bolas na urna": cada nó aparece uma vez por conexão que
        // possui, de forma que sortear um índice aleatório da lista equivale
        // a escolher um nó com probabilidade proporcional ao seu grau atual.
        List<Integer> urnaPorGrau = new ArrayList<>();
        for (int i = 0; i < nucleoInicial; i++) {
            for (int k = 0; k < vizinhos.get(i).size(); k++) {
                urnaPorGrau.add(i);
            }
        }

        // Crescimento incremental: cada novo nó se conecta a m nós existentes,
        // escolhidos por fixação preferencial (proporcional ao grau).
        for (int novoNo = nucleoInicial; novoNo < numAgentes; novoNo++) {
            int conexoesDesejadas = Math.min(m, novoNo);
            Set<Integer> escolhidos = new HashSet<>();
            int tentativas = 0;
            int limiteTentativas = numAgentes * 10;

            while (escolhidos.size() < conexoesDesejadas && tentativas < limiteTentativas) {
                int candidato = urnaPorGrau.isEmpty()
                        ? random.nextInt(novoNo)
                        : urnaPorGrau.get(random.nextInt(urnaPorGrau.size()));
                escolhidos.add(candidato);
                tentativas++;
            }

            for (int destino : escolhidos) {
                conectar(vizinhos, novoNo, destino, peso);
                urnaPorGrau.add(destino);
                urnaPorGrau.add(novoNo);
            }
        }

        return vizinhos;
    }

    private void conectar(List<Map<Integer, Double>> vizinhos, int i, int j, double peso) {
        vizinhos.get(i).put(j, peso);
        vizinhos.get(j).put(i, peso);
    }
}
