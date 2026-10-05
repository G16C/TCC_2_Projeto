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

/** Testes de {@link RelatorioExport}: valida a forma do JSON consumido pelo dashboard. */
class RelatorioExportTest {

    @Test
    @DisplayName("JSON exportado contém nós, arestas e meta consistentes com a rede")
    void jsonContemCamposEsperados() {
        List<Map<Integer, Double>> adj = List.of(
                Map.of(1, 1.0, 2, 1.0),
                Map.of(0, 1.0),
                Map.of(0, 1.0)
        );
        List<String> ids = List.of("x1", "x2", "x3");
        TwitterNetworkData dados = new TwitterNetworkData("ego1", ids, adj, Map.of(), List.of(), new int[0]);
        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(teste)")
                .twitterEgoId("ego1")
                .alpha(0.6).delta(0.02).numCiclos(2)
                .build();
        RedeSocial rede = new RedeSocial(config, dados);

        Map<Integer, EmotionState> iniciais = Map.of(
                0, EmotionState.semente(Emotion.SADNESS, 0.5),
                1, EmotionState.neutro(),
                2, EmotionState.neutro()
        );
        List<Map<Integer, EmotionState>> historico = SimuladorDireto.executar(rede, config, iniciais);

        String json = RelatorioExport.paraJson(rede, config, historico, 0);

        assertTrue(json.contains("\"numAgentes\":3"));
        assertTrue(json.contains("\"fonteDados\":\"TWITTER_SOCIAL_CIRCLES\""));
        assertTrue(json.contains("\"twitterEgoId\":\"ego1\""));
        assertTrue(json.contains("\"idExterno\":\"x1\""));

        // duas arestas não-dirigidas (0-1 e 0-2), sem duplicar i-j/j-i
        long ocorrenciasOrigem = json.split("\"origem\"", -1).length - 1;
        assertEquals(2, ocorrenciasOrigem);

        // 3 ciclos no total: inicial (0) + 2 configurados
        long ocorrenciasCiclo = json.split("\"ciclo\":", -1).length - 1;
        assertEquals(3, ocorrenciasCiclo);
    }

    @Test
    @DisplayName("Rede sintética (sem dados do Twitter) exporta twitterEgoId nulo")
    void redeSinteticaExportaEgoIdNulo() {
        SimulationConfiguration config = SimulationConfiguration.builder()
                .numAgentes(10)
                .alpha(0.5).delta(0.01).numCiclos(1)
                .build();
        RedeSocial rede = new RedeSocial(config);

        Map<Integer, EmotionState> iniciais = new java.util.HashMap<>();
        for (int i = 0; i < rede.getNumAgentes(); i++) iniciais.put(i, EmotionState.neutro());
        List<Map<Integer, EmotionState>> historico = SimuladorDireto.executar(rede, config, iniciais);

        String json = RelatorioExport.paraJson(rede, config, historico, 0);
        assertTrue(json.contains("\"twitterEgoId\":null"));
    }
}
