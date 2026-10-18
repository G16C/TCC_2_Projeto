package br.unb.poc.modelo.emocao;

import java.util.Map;

/**
 * Abstração do mecanismo de atualização emocional.
 *
 * <p>A etapa 2 passa a operar sobre um estado multidimensional com
 * quatro emoções, mantendo o agente independente da equação concreta.</p>
 */
public interface EmotionPropagationModel {

    EmotionState calcularProximoEstado(
            EmotionState estadoAtual,
            Map<Integer, EmotionState> influencias,
            Map<Integer, Double> pesos);
}
