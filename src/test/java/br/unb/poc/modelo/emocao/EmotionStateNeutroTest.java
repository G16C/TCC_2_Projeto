package br.unb.poc.modelo.emocao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de regressão para {@link EmotionState#isNeutro()} e
 * {@link EmotionState#getRotuloDominante()}, adicionados após a
 * correção de um bug real encontrado ao rodar a simulação com o
 * dataset do Twitter: nós sem nenhuma feature emocional reconhecida
 * (estado neutro) eram rotulados como "ANGER" por causa do desempate
 * do enum em {@link EmotionState#getDominantEmotion()} — ver
 * {@code docs/ETAPAS_EVOLUCAO.md}, seção sobre a inspeção do dataset real.
 */
class EmotionStateNeutroTest {

    @Test
    @DisplayName("Estado neutro (todas as intensidades zero) é reconhecido como neutro")
    void estadoNeutro() {
        EmotionState estado = EmotionState.neutro();
        assertTrue(estado.isNeutro());
        assertEquals("NEUTRO", estado.getRotuloDominante());
    }

    @Test
    @DisplayName("Estado neutro não deve ser confundido com raiva pelo rótulo de exibição/persistência")
    void estadoNeutroNaoVideComoRaiva() {
        EmotionState estado = EmotionState.neutro();
        // getDominantEmotion() ainda resolve o empate para ANGER (comportamento
        // antigo, preservado por compatibilidade) — por isso getRotuloDominante()
        // existe e deve ser preferido para exibição/persistência.
        assertEquals(Emotion.ANGER, estado.getDominantEmotion());
        assertNotEquals("ANGER", estado.getRotuloDominante());
    }

    @Test
    @DisplayName("Estado com sinal real (não neutro) usa o rótulo da própria emoção dominante")
    void estadoComSinalUsaRotuloReal() {
        EmotionState estado = EmotionState.semente(Emotion.JOY, 0.7);
        assertFalse(estado.isNeutro());
        assertEquals("JOY", estado.getRotuloDominante());
        assertEquals(Emotion.JOY, estado.getDominantEmotion());
    }
}
