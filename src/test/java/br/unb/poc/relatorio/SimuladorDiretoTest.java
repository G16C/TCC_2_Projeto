package br.unb.poc.relatorio;

import br.unb.poc.dados.FonteDados;
import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.topologia.TipoTopologia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de {@link SimuladorDireto}, sem depender de JADE ou PostgreSQL. */
class SimuladorDiretoTest {

    private static RedeSocial redeDoisNosConectados() {
        List<Map<Integer, Double>> adj = List.of(
                Map.of(1, 1.0),
                Map.of(0, 1.0)
        );
        List<String> ids = List.of("a", "b");
        TwitterNetworkData dados = new TwitterNetworkData("teste", ids, adj, Map.of(), List.of(), new int[0]);
        return new RedeSocial(configPadrao(), dados);
    }

    private static SimulationConfiguration configPadrao() {
        return SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(teste)")
                .twitterEgoId("teste")
                .alpha(0.6)
                .delta(0.0)
                .numCiclos(3)
                .build();
    }

    @Test
    @DisplayName("Rede totalmente neutra permanece neutra em todos os ciclos")
    void redeNeutraPermaneceNeutra() {
        RedeSocial rede = redeDoisNosConectados();
        Map<Integer, EmotionState> iniciais = Map.of(0, EmotionState.neutro(), 1, EmotionState.neutro());

        List<Map<Integer, EmotionState>> historico =
                SimuladorDireto.executar(rede, configPadrao(), iniciais);

        assertEquals(4, historico.size(), "ciclo 0 (inicial) + 3 ciclos configurados");
        for (Map<Integer, EmotionState> ciclo : historico) {
            for (EmotionState estado : ciclo.values()) {
                assertTrue(estado.isNeutro());
            }
        }
    }

    @Test
    @DisplayName("Semente de alegria em um nó se propaga para o vizinho conectado")
    void sementePropagaParaVizinho() {
        RedeSocial rede = redeDoisNosConectados();
        Map<Integer, EmotionState> iniciais = Map.of(
                0, EmotionState.semente(Emotion.JOY, 0.8),
                1, EmotionState.neutro()
        );

        List<Map<Integer, EmotionState>> historico =
                SimuladorDireto.executar(rede, configPadrao(), iniciais);

        EmotionState estadoFinalVizinho = historico.get(historico.size() - 1).get(1);
        assertTrue(estadoFinalVizinho.getIntensity(Emotion.JOY) > 0,
                "o vizinho deveria ter recebido alguma alegria do nó semente");
    }

    @Test
    @DisplayName("Nó isolado (sem vizinhos) mantém seu estado inicial inalterado")
    void noIsoladoMantemEstado() {
        List<Map<Integer, Double>> adj = List.of(Map.of(), Map.of());
        List<String> ids = List.of("a", "b");
        TwitterNetworkData dados = new TwitterNetworkData("teste", ids, adj, Map.of(), List.of(), new int[0]);
        RedeSocial rede = new RedeSocial(configPadrao(), dados);

        Map<Integer, EmotionState> iniciais = Map.of(0, EmotionState.semente(Emotion.SADNESS, 0.6), 1, EmotionState.neutro());
        List<Map<Integer, EmotionState>> historico = SimuladorDireto.executar(rede, configPadrao(), iniciais);

        EmotionState estadoFinal = historico.get(historico.size() - 1).get(0);
        assertEquals(0.6, estadoFinal.getIntensity(Emotion.SADNESS), 1e-9);
    }
}
