package br.unb.poc.modelo.emocao;

import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;

/**
 * Estado emocional multidimensional de um agente.
 *
 * <p>Cada emoção possui uma intensidade no intervalo [0, 1].
 * Nesta etapa, as quatro emoções são mantidas separadamente.
 * A normalização/competição entre emoções será refinada após a
 * validação matemática do modelo de Fan e dos dados empíricos.</p>
 */
public final class EmotionState {

    private final EnumMap<Emotion, Double> intensidades = new EnumMap<>(Emotion.class);

    public EmotionState() {
        for (Emotion emotion : Emotion.values()) {
            intensidades.put(emotion, 0.0);
        }
    }

    public EmotionState(Map<Emotion, Double> valores) {
        this();
        if (valores != null) {
            valores.forEach(this::setIntensity);
        }
    }

    public static EmotionState neutro() {
        return new EmotionState();
    }

    public static EmotionState semente(Emotion emotion, double intensidade) {
        EmotionState state = new EmotionState();
        state.setIntensity(emotion, intensidade);
        return state;
    }

    public double getIntensity(Emotion emotion) {
        return intensidades.get(emotion);
    }

    public void setIntensity(Emotion emotion, double valor) {
        if (valor < 0 || valor > 1) {
            throw new IllegalArgumentException("Intensidade deve estar em [0,1]");
        }
        intensidades.put(emotion, valor);
    }

    public Map<Emotion, Double> asMap() {
        return new EnumMap<>(intensidades);
    }

    /**
     * Retorna a emoção de maior intensidade.
     * Em caso de empate, prevalece a ordem do enum.
     *
     * <p><b>Atenção:</b> quando o estado é neutro (todas as intensidades
     * iguais a zero — nenhuma feature reconhecida pela heurística, por
     * exemplo), este método ainda retorna um valor não nulo por causa do
     * empate ({@code Emotion.ANGER}, primeiro do enum). Use
     * {@link #isNeutro()} antes de interpretar o resultado como um sinal
     * emocional real.</p>
     */
    public Emotion getDominantEmotion() {
        Emotion dominante = Emotion.ANGER;
        double maior = -1.0;
        for (Emotion emotion : Emotion.values()) {
            double valor = intensidades.get(emotion);
            if (valor > maior) {
                maior = valor;
                dominante = emotion;
            }
        }
        return dominante;
    }

    /** {@code true} quando nenhuma emoção tem intensidade acima de zero. */
    public boolean isNeutro() {
        return intensidades.values().stream().allMatch(v -> v == 0.0);
    }

    /**
     * Rótulo textual da emoção dominante, retornando {@code "NEUTRO"} em
     * vez do resultado (enganoso) de {@link #getDominantEmotion()} quando
     * o estado é neutro. Recomendado para exibição/persistência.
     */
    public String getRotuloDominante() {
        return isNeutro() ? "NEUTRO" : getDominantEmotion().name();
    }

    /**
     * Compatibilidade temporária com a representação escalar da PoC.
     * Retorna a maior intensidade, preservando uma API útil para logs.
     */
    public double getValor() {
        return intensidades.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    /**
     * Serialização simples para mensagens ACL sem dependência externa.
     */
    public String serialize() {
        return String.format(Locale.US,
                "anger=%.8f;disgust=%.8f;joy=%.8f;sadness=%.8f",
                getIntensity(Emotion.ANGER),
                getIntensity(Emotion.DISGUST),
                getIntensity(Emotion.JOY),
                getIntensity(Emotion.SADNESS));
    }

    public static EmotionState deserialize(String content) {
        EmotionState state = new EmotionState();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Estado emocional vazio");
        }
        String[] fields = content.split(";");
        for (String field : fields) {
            String[] pair = field.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Campo emocional inválido: " + field);
            }
            Emotion emotion = Emotion.valueOf(pair[0].trim().toUpperCase(Locale.ROOT));
            state.setIntensity(emotion, Double.parseDouble(pair[1].trim()));
        }
        return state;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "anger=%.4f, disgust=%.4f, joy=%.4f, sadness=%.4f | dominante=%s",
                getIntensity(Emotion.ANGER),
                getIntensity(Emotion.DISGUST),
                getIntensity(Emotion.JOY),
                getIntensity(Emotion.SADNESS),
                getRotuloDominante());
    }
}
