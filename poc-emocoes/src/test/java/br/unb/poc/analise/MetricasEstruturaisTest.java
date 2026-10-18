package br.unb.poc.analise;

import br.unb.poc.dados.FonteDados;
import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.topologia.TipoTopologia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de {@link MetricasEstruturais} sobre grafos pequenos com valores
 * conhecidos analiticamente (triângulo, caminho, dois componentes), para
 * validar as fórmulas de grau, agrupamento, distância média e componentes
 * conexos independentemente de qualquer dataset real.
 */
class MetricasEstruturaisTest {

    private static RedeSocial redeDeTeste(List<Map<Integer, Double>> adjacencia) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < adjacencia.size(); i++) ids.add("n" + i);
        TwitterNetworkData dados = new TwitterNetworkData("teste", ids, adjacencia, Map.of(), List.of(), new int[0]);
        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(teste)")
                .twitterEgoId("teste")
                .build();
        return new RedeSocial(config, dados);
    }

    private static Map<Integer, Double> vizinhos(Integer... ids) {
        Map<Integer, Double> m = new LinkedHashMap<>();
        for (int id : ids) m.put(id, 1.0);
        return m;
    }

    @Test
    @DisplayName("Triângulo (3 nós totalmente conectados): agrupamento=1, distância média=1, 1 componente")
    void triangulo() {
        List<Map<Integer, Double>> adj = List.of(
                vizinhos(1, 2),
                vizinhos(0, 2),
                vizinhos(0, 1)
        );
        MetricasEstruturais m = MetricasEstruturais.calcular(redeDeTeste(adj));

        assertEquals(3, m.numNos());
        assertEquals(3, m.numArestas());
        assertEquals(2.0, m.grauMedio(), 1e-9);
        assertEquals(1.0, m.coeficienteAgrupamentoMedio(), 1e-9);
        assertEquals(1.0, m.distanciaMediaAprox(), 1e-9);
        assertEquals(1, m.numComponentesConexos());
        assertEquals(1.0, m.fracaoMaiorComponente(), 1e-9);
        assertEquals(1.0, m.densidade(), 1e-9);
    }

    @Test
    @DisplayName("Caminho de 3 nós (0-1-2): sem triângulos, distância média = 4/3")
    void caminhoDeTresNos() {
        List<Map<Integer, Double>> adj = List.of(
                vizinhos(1),
                vizinhos(0, 2),
                vizinhos(1)
        );
        MetricasEstruturais m = MetricasEstruturais.calcular(redeDeTeste(adj));

        assertEquals(2, m.numArestas());
        assertEquals(0.0, m.coeficienteAgrupamentoMedio(), 1e-9);
        assertEquals(4.0 / 3.0, m.distanciaMediaAprox(), 1e-9);
        assertEquals(1, m.numComponentesConexos());
        assertEquals(2, m.grauMaximo());
        assertEquals(1, m.grauMinimo());
    }

    @Test
    @DisplayName("Dois componentes desconexos: fração no maior componente reflete o tamanho relativo")
    void doisComponentes() {
        // {0-1} isolado e {2-3-4} em caminho
        List<Map<Integer, Double>> adj = List.of(
                vizinhos(1),
                vizinhos(0),
                vizinhos(3),
                vizinhos(2, 4),
                vizinhos(3)
        );
        MetricasEstruturais m = MetricasEstruturais.calcular(redeDeTeste(adj));

        assertEquals(2, m.numComponentesConexos());
        assertEquals(3.0 / 5.0, m.fracaoMaiorComponente(), 1e-9);
        // distância média só considera pares dentro do mesmo componente
        assertTrue(m.distanciaMediaAprox() > 0 && m.distanciaMediaAprox() < 2);
    }
}
