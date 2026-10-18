package br.unb.poc.modelo.emocao;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeuristicaTwitterTest {

    @Test
    void deveInferirAlegriaAPartirDeFeatureConhecida() {
        HeuristicaTwitter h = new HeuristicaTwitter();
        EmotionState estado = h.inferir(
                new int[]{1, 0},
                List.of("#love", "#sad"));

        assertEquals(0.70, estado.getIntensity(Emotion.JOY), 1e-9);
        assertEquals(0.0, estado.getIntensity(Emotion.SADNESS), 1e-9);
    }

    @Test
    void deveManterFeatureDesconhecidaNeutra() {
        HeuristicaTwitter h = new HeuristicaTwitter();
        EmotionState estado = h.inferir(
                new int[]{1},
                List.of("#feature_sem_rotulo"));

        assertEquals(0.0, estado.getValor(), 1e-9);
    }

    @Test
    void devePermitirMultiplasEmocoes() {
        HeuristicaTwitter h = new HeuristicaTwitter();
        EmotionState estado = h.inferir(
                Map.of("#love", 1, "#sad", 1));

        assertEquals(0.70, estado.getIntensity(Emotion.JOY), 1e-9);
        assertEquals(0.70, estado.getIntensity(Emotion.SADNESS), 1e-9);
    }
}
