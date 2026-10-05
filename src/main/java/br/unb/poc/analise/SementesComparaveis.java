package br.unb.poc.analise;

import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;

import java.util.*;

/**
 * Atribui estados iniciais a uma rede, distribuindo aleatoriamente o
 * mesmo número de nós-semente por emoção observado em outra rede
 * (tipicamente a rede real do Twitter), com o restante dos nós neutro.
 *
 * <p>Existe para viabilizar uma comparação justa entre a rede real e as
 * topologias sintéticas: como os nós não têm identidade equivalente
 * entre redes diferentes, não é possível "copiar" literalmente o estado
 * inicial de um nó específico. Em vez disso, replica-se a mesma
 * <b>quantidade</b> de nós-semente por emoção — de forma que a única
 * variável entre os cenários comparados seja a topologia da rede, não a
 * quantidade de sinal emocional injetado.
 */
public final class SementesComparaveis {

    private SementesComparaveis() {}

    private static final double PESO_SEMENTE_PADRAO = 0.70; // mesmo peso padrão de HeuristicaTwitter

    /** Conta, por emoção, quantos nós de {@code estados} são não-neutros e têm aquela emoção como dominante. */
    public static Map<Emotion, Integer> contar(Map<Integer, EmotionState> estados) {
        Map<Emotion, Integer> contagem = new EnumMap<>(Emotion.class);
        for (Emotion e : Emotion.values()) contagem.put(e, 0);
        for (EmotionState estado : estados.values()) {
            if (!estado.isNeutro()) {
                contagem.merge(estado.getDominantEmotion(), 1, Integer::sum);
            }
        }
        return contagem;
    }

    /**
     * Distribui aleatoriamente {@code contagem.get(emocao)} nós-semente
     * para cada emoção em {@code rede}, com peso {@value #PESO_SEMENTE_PADRAO}
     * (mesmo valor padrão usado por {@code HeuristicaTwitter}), deixando os
     * demais nós neutros.
     */
    public static Map<Integer, EmotionState> atribuir(RedeSocial rede, Map<Emotion, Integer> contagem, long semente) {
        int n = rede.getNumAgentes();
        List<Integer> indices = new ArrayList<>(n);
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices, new Random(semente));

        Map<Integer, EmotionState> estados = new HashMap<>();
        for (int i = 0; i < n; i++) estados.put(i, EmotionState.neutro());

        int cursor = 0;
        for (Emotion e : Emotion.values()) {
            int quantidade = Math.min(contagem.getOrDefault(e, 0), n - cursor);
            for (int k = 0; k < quantidade; k++) {
                estados.put(indices.get(cursor++), EmotionState.semente(e, PESO_SEMENTE_PADRAO));
            }
        }
        return estados;
    }
}
