package br.unb.poc.relatorio;

import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.emocao.EmotionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coletor em memória, independente do banco de dados, que acumula o
 * histórico emocional de uma simulação em execução.
 *
 * <p>Esta classe existe para viabilizar a Etapa 4.9 (preparar dados
 * para a interface) sem depender de reconsultar o PostgreSQL — o
 * esquema atual ({@code simulacao}/{@code estado_emocional}) não
 * armazena a topologia da rede, apenas o estado por ciclo. Como a
 * topologia só existe em memória durante a execução do
 * {@code AgenteControle}, o coletor mantém uma referência à
 * {@link RedeSocial} e ao histórico de estados por ciclo, permitindo
 * montar o JSON de interface a qualquer momento da execução — inclusive
 * enquanto os agentes JADE ainda estão propagando emoções.
 *
 * <p>Todos os métodos são thread-safe e não lançam exceções para não
 * interferir na simulação principal caso algo inesperado ocorra.
 */
public final class ColetorResultados {

    private ColetorResultados() {}

    private static final class Registro {
        volatile RedeSocial rede;
        final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, EmotionState>> porCiclo =
                new ConcurrentHashMap<>();
    }

    private static final ConcurrentHashMap<Integer, Registro> SIMULACOES = new ConcurrentHashMap<>();

    private static Registro registro(int simulacaoId) {
        return SIMULACOES.computeIfAbsent(simulacaoId, id -> new Registro());
    }

    /** Associa a topologia da rede a uma simulação em execução. */
    public static void definirRede(int simulacaoId, RedeSocial rede) {
        try {
            registro(simulacaoId).rede = rede;
        } catch (RuntimeException ignorado) {
            // A coleta para a interface nunca deve interromper a simulação.
        }
    }

    /** Registra o estado emocional de um agente em um ciclo, em memória. */
    public static void registrarEstado(int simulacaoId, int ciclo, int idAgente, EmotionState estado) {
        try {
            registro(simulacaoId)
                    .porCiclo
                    .computeIfAbsent(ciclo, c -> new ConcurrentHashMap<>())
                    .put(idAgente, estado);
        } catch (RuntimeException ignorado) {
            // idem
        }
    }

    /** Instantâneo consistente do que foi coletado até o momento da chamada. */
    public record Instantaneo(RedeSocial rede, List<Map<Integer, EmotionState>> historico) {}

    /**
     * Retorna a rede e o histórico ordenado por ciclo (índice da lista =
     * número do ciclo), ou {@code null} se a simulação ainda não registrou
     * a topologia da rede.
     */
    public static Instantaneo obterInstantaneo(int simulacaoId) {
        Registro registro = SIMULACOES.get(simulacaoId);
        if (registro == null || registro.rede == null) return null;

        TreeMap<Integer, ConcurrentHashMap<Integer, EmotionState>> ordenado =
                new TreeMap<>(registro.porCiclo);

        List<Map<Integer, EmotionState>> historico = new ArrayList<>();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, EmotionState>> entrada : ordenado.entrySet()) {
            historico.add(Map.copyOf(entrada.getValue()));
        }
        return new Instantaneo(registro.rede, historico);
    }

    /** Remove os dados coletados de uma simulação (encerramento/limpeza). */
    public static void limpar(int simulacaoId) {
        SIMULACOES.remove(simulacaoId);
    }
}
