package br.unb.poc.modelo.emocao;

import java.util.Map;

/**
 * Estratégia para estimar o estado emocional inicial de um agente.
 *
 * <p>A heurística recebe as características observadas na fonte de dados
 * e produz um estado emocional multidimensional. A estratégia é separada
 * do modelo de propagação para permitir sua substituição sem alterar
 * a dinâmica de propagação.</p>
 */
public interface HeuristicaEmocional {

    EmotionState inferir(Map<String, Integer> features);

    default EmotionState inferir(Map<String, Integer> features, int[] vetor, java.util.List<String> nomes) {
        return inferir(features);
    }
}
