package br.unb.poc.modelo.emocao;

import java.util.EnumMap;
import java.util.Map;

/**
 * Adaptação multidimensional do modelo descrito na proposta.
 *
 * <p>A mesma regra de persistência, influência ponderada e decaimento
 * é aplicada separadamente a cada uma das quatro emoções:
 *
 * E_i,e(t+1) = alpha * E_i,e(t)
 *           + (1-alpha) * I_i,e(t)
 *           tudo multiplicado por (1-delta).
 *
 * <p>Esta é uma adaptação de implementação para a representação
 * multidimensional. A definição exata de competição/normalização entre
 * emoções será refinada após a revisão do modelo original de Fan e dos
 * dados empíricos.</p>
 */
public final class FanEmotionModel implements EmotionPropagationModel {

    private final double alpha;
    private final double delta;

    public FanEmotionModel(double alpha, double delta) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha deve estar em [0,1]");
        }
        if (delta < 0 || delta > 1) {
            throw new IllegalArgumentException("delta deve estar em [0,1]");
        }
        this.alpha = alpha;
        this.delta = delta;
    }

    @Override
    public EmotionState calcularProximoEstado(
            EmotionState estadoAtual,
            Map<Integer, EmotionState> influencias,
            Map<Integer, Double> pesos) {

        Map<Emotion, Double> resultado = new EnumMap<>(Emotion.class);

        for (Emotion emotion : Emotion.values()) {
            double influencia = calcularInfluenciaAgregada(emotion, influencias, pesos);
            double valor = (
                    alpha * estadoAtual.getIntensity(emotion)
                    + (1.0 - alpha) * influencia
            ) * (1.0 - delta);

            resultado.put(emotion, clamp(valor));
        }

        return new EmotionState(resultado);
    }

    private double calcularInfluenciaAgregada(
            Emotion emotion,
            Map<Integer, EmotionState> influencias,
            Map<Integer, Double> pesos) {

        double somaPonderada = 0.0;
        double somaPesos = 0.0;

        for (Map.Entry<Integer, EmotionState> entrada : influencias.entrySet()) {
            double peso = pesos.getOrDefault(entrada.getKey(), 0.0);
            if (peso <= 0) {
                continue;
            }
            somaPonderada += peso * entrada.getValue().getIntensity(emotion);
            somaPesos += peso;
        }

        return somaPesos > 0.0 ? somaPonderada / somaPesos : 0.0;
    }

    private double clamp(double valor) {
        return Math.max(0.0, Math.min(1.0, valor));
    }
}
