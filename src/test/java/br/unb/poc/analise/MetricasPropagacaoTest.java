package br.unb.poc.analise;

import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de {@link MetricasPropagacao} sobre um histórico sintético, sem depender de {@code SimuladorDireto}. */
class MetricasPropagacaoTest {

    @Test
    @DisplayName("Média por ciclo, distribuição inicial/final e ciclo de convergência calculados corretamente")
    void calculaMetricasCorretamente() {
        List<Map<Integer, EmotionState>> historico = new ArrayList<>();

        // ciclo 0: agente 0 com raiva 0.8, agente 1 neutro (média ANGER = 0.4)
        historico.add(Map.of(
                0, EmotionState.semente(Emotion.ANGER, 0.8),
                1, EmotionState.neutro()
        ));
        // ciclo 1: variação grande ainda (média ANGER = 0.445)
        historico.add(Map.of(
                0, EmotionState.semente(Emotion.ANGER, 0.79),
                1, EmotionState.semente(Emotion.ANGER, 0.1)
        ));
        // ciclo 2: estabilizou — variação entre c1 e c2 abaixo do limiar (0.001)
        historico.add(Map.of(
                0, EmotionState.semente(Emotion.ANGER, 0.7899),
                1, EmotionState.semente(Emotion.ANGER, 0.1004)
        ));

        MetricasPropagacao m = MetricasPropagacao.calcular(historico);

        assertEquals(3, m.serieMediaPorCiclo().size());
        assertEquals(0.4, m.serieMediaPorCiclo().get(0).get(Emotion.ANGER), 1e-9);
        assertEquals(0.0, m.serieMediaPorCiclo().get(0).get(Emotion.JOY), 1e-9);

        assertEquals(Map.of("ANGER", 1, "NEUTRO", 1), m.distribuicaoInicial());
        assertEquals(Map.of("ANGER", 2), m.distribuicaoFinal());

        assertEquals(2, m.cicloConvergencia());
    }

    @Test
    @DisplayName("Sem variação nenhuma entre os dois únicos ciclos, converge já no ciclo 1")
    void convergeImediatamenteQuandoEstavel() {
        Map<Integer, EmotionState> estadoFixo = Map.of(0, EmotionState.semente(Emotion.JOY, 0.5));
        List<Map<Integer, EmotionState>> historico = List.of(estadoFixo, estadoFixo);

        MetricasPropagacao m = MetricasPropagacao.calcular(historico);

        assertEquals(1, m.cicloConvergencia());
    }

    @Test
    @DisplayName("Histórico vazio não lança exceção e retorna listas/mapas vazios")
    void historicoVazio() {
        MetricasPropagacao m = MetricasPropagacao.calcular(List.of());

        assertTrue(m.serieMediaPorCiclo().isEmpty());
        assertTrue(m.distribuicaoInicial().isEmpty());
        assertTrue(m.distribuicaoFinal().isEmpty());
        assertEquals(-1, m.cicloConvergencia());
    }
}
