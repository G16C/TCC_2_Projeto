package br.unb.poc.analise;

import br.unb.poc.dados.FonteDados;
import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.topologia.TipoTopologia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de {@link SementesComparaveis}: base da comparação justa entre rede real e sintéticas. */
class SementesComparaveisTest {

    @Test
    @DisplayName("contar() ignora nós neutros e agrupa por emoção dominante")
    void contarSementes() {
        Map<Integer, EmotionState> estados = new HashMap<>();
        estados.put(0, EmotionState.semente(Emotion.JOY, 0.7));
        estados.put(1, EmotionState.semente(Emotion.JOY, 0.7));
        estados.put(2, EmotionState.semente(Emotion.ANGER, 0.7));
        estados.put(3, EmotionState.neutro());
        estados.put(4, EmotionState.neutro());

        Map<Emotion, Integer> contagem = SementesComparaveis.contar(estados);

        assertEquals(2, contagem.get(Emotion.JOY));
        assertEquals(1, contagem.get(Emotion.ANGER));
        assertEquals(0, contagem.get(Emotion.DISGUST));
        assertEquals(0, contagem.get(Emotion.SADNESS));
    }

    @Test
    @DisplayName("atribuir() distribui exatamente a quantidade de sementes pedida, sem repetir nó")
    void atribuirRespeitaContagem() {
        int n = 6;
        List<Map<Integer, Double>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<Integer, Double> viz = new LinkedHashMap<>();
            viz.put((i + 1) % n, 1.0);
            viz.put((i - 1 + n) % n, 1.0);
            adj.add(viz);
        }
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) ids.add("n" + i);
        TwitterNetworkData dados = new TwitterNetworkData("teste", ids, adj, Map.of(), List.of(), new int[0]);
        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(teste)")
                .twitterEgoId("teste")
                .build();
        RedeSocial rede = new RedeSocial(config, dados);

        Map<Emotion, Integer> contagem = new EnumMap<>(Emotion.class);
        contagem.put(Emotion.JOY, 2);
        contagem.put(Emotion.ANGER, 1);
        contagem.put(Emotion.DISGUST, 0);
        contagem.put(Emotion.SADNESS, 0);

        Map<Integer, EmotionState> estados = SementesComparaveis.atribuir(rede, contagem, 123L);

        long qtdJoy = estados.values().stream()
                .filter(e -> !e.isNeutro() && e.getDominantEmotion() == Emotion.JOY).count();
        long qtdAnger = estados.values().stream()
                .filter(e -> !e.isNeutro() && e.getDominantEmotion() == Emotion.ANGER).count();
        long qtdNeutro = estados.values().stream().filter(EmotionState::isNeutro).count();

        assertEquals(2, qtdJoy);
        assertEquals(1, qtdAnger);
        assertEquals(3, qtdNeutro);
        assertEquals(n, estados.size());
    }

    @Test
    @DisplayName("Pedir mais sementes do que nós disponíveis não lança exceção, apenas satura")
    void naoEstouraQuandoContagemMaiorQueRede() {
        List<Map<Integer, Double>> adj = List.of(Map.of(1, 1.0), Map.of(0, 1.0));
        List<String> ids = List.of("a", "b");
        TwitterNetworkData dados = new TwitterNetworkData("teste", ids, adj, Map.of(), List.of(), new int[0]);
        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(teste)")
                .twitterEgoId("teste")
                .build();
        RedeSocial rede = new RedeSocial(config, dados);

        Map<Emotion, Integer> contagem = new EnumMap<>(Emotion.class);
        contagem.put(Emotion.JOY, 10); // rede só tem 2 nós
        contagem.put(Emotion.ANGER, 0);
        contagem.put(Emotion.DISGUST, 0);
        contagem.put(Emotion.SADNESS, 0);

        Map<Integer, EmotionState> estados = assertDoesNotThrow(
                () -> SementesComparaveis.atribuir(rede, contagem, 1L));

        assertEquals(2, estados.size());
    }
}
