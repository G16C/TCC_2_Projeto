package br.unb.poc.analise;

import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;

import java.util.*;

/**
 * Métricas derivadas do histórico de propagação emocional
 * ({@code SimuladorDireto}), usadas para comparar o *resultado* da
 * simulação entre cenários (ex.: rede real do Twitter vs. topologias
 * sintéticas), independentemente das métricas puramente estruturais em
 * {@link MetricasEstruturais}.
 *
 * @param serieMediaPorCiclo   média de cada emoção (anger/disgust/joy/sadness) em cada ciclo, na ordem do histórico
 * @param distribuicaoInicial  contagem de rótulos dominantes (incluindo "NEUTRO") no ciclo 0
 * @param distribuicaoFinal    contagem de rótulos dominantes no último ciclo
 * @param cicloConvergencia    primeiro ciclo em que a variação média entre ciclos consecutivos cai abaixo do limiar; -1 se não convergiu
 */
public record MetricasPropagacao(
        List<Map<Emotion, Double>> serieMediaPorCiclo,
        Map<String, Integer> distribuicaoInicial,
        Map<String, Integer> distribuicaoFinal,
        int cicloConvergencia) {

    private static final double LIMIAR_CONVERGENCIA = 0.001;

    public static MetricasPropagacao calcular(List<Map<Integer, EmotionState>> historico) {
        List<Map<Emotion, Double>> serie = new ArrayList<>();
        for (Map<Integer, EmotionState> ciclo : historico) {
            serie.add(mediaPorEmocao(ciclo));
        }

        Map<String, Integer> inicial = historico.isEmpty() ? Map.of() : distribuicaoRotulos(historico.get(0));
        Map<String, Integer> finalDist = historico.isEmpty()
                ? Map.of() : distribuicaoRotulos(historico.get(historico.size() - 1));

        int cicloConvergencia = -1;
        for (int c = 1; c < serie.size(); c++) {
            double maiorVariacao = 0.0;
            for (Emotion e : Emotion.values()) {
                double diff = Math.abs(serie.get(c).get(e) - serie.get(c - 1).get(e));
                maiorVariacao = Math.max(maiorVariacao, diff);
            }
            if (maiorVariacao < LIMIAR_CONVERGENCIA) {
                cicloConvergencia = c;
                break;
            }
        }

        return new MetricasPropagacao(serie, inicial, finalDist, cicloConvergencia);
    }

    private static Map<Emotion, Double> mediaPorEmocao(Map<Integer, EmotionState> ciclo) {
        Map<Emotion, Double> soma = new EnumMap<>(Emotion.class);
        for (Emotion e : Emotion.values()) soma.put(e, 0.0);
        if (ciclo.isEmpty()) return soma;
        for (EmotionState estado : ciclo.values()) {
            for (Emotion e : Emotion.values()) {
                soma.merge(e, estado.getIntensity(e), Double::sum);
            }
        }
        int n = ciclo.size();
        Map<Emotion, Double> media = new EnumMap<>(Emotion.class);
        for (Emotion e : Emotion.values()) media.put(e, soma.get(e) / n);
        return media;
    }

    private static Map<String, Integer> distribuicaoRotulos(Map<Integer, EmotionState> ciclo) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (EmotionState estado : ciclo.values()) {
            contagem.merge(estado.getRotuloDominante(), 1, Integer::sum);
        }
        return contagem;
    }
}
