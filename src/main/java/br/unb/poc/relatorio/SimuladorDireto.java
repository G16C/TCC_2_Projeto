package br.unb.poc.relatorio;

import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.EmotionPropagationModel;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.emocao.FanEmotionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execução síncrona e determinística do mesmo modelo de propagação usado
 * por {@code UsuarioAgent} ({@link FanEmotionModel}), porém sem JADE, sem
 * mensagens ACL e sem PostgreSQL.
 *
 * <p><b>Finalidade:</b> alimentar a camada de interface (Etapas 4.9/5.0)
 * de forma imediata e testável — por exemplo, para pré-visualizar um
 * cenário antes de rodar a simulação completa via agentes, ou para rodar
 * a interface em ambientes sem plataforma JADE/banco disponíveis.
 *
 * <p>Esta classe <b>não substitui</b> a execução oficial da simulação
 * (Etapa 4.7, feita por agentes JADE trocando mensagens de forma
 * assíncrona e persistindo em PostgreSQL). Ela aplica a mesma equação a
 * todos os agentes em paralelo lógico, ciclo a ciclo, o que é uma
 * simplificação razoável para fins de visualização, dado que o modelo de
 * Fan é uma média ponderada das influências recebidas.
 */
public final class SimuladorDireto {

    private SimuladorDireto() {}

    /**
     * Executa a propagação por {@code config.getNumCiclos()} ciclos.
     *
     * @param rede             rede social já construída (sintética ou Twitter)
     * @param config           configuração do cenário (alpha, delta, número de ciclos)
     * @param estadosIniciais  estado emocional inicial por agente (id local -> estado);
     *                         agentes ausentes do mapa recebem {@link EmotionState#neutro()}
     * @return histórico por ciclo; índice 0 é o estado inicial (ciclo 0)
     */
    public static List<Map<Integer, EmotionState>> executar(
            RedeSocial rede,
            SimulationConfiguration config,
            Map<Integer, EmotionState> estadosIniciais) {

        int n = rede.getNumAgentes();
        EmotionPropagationModel modelo = new FanEmotionModel(config.getAlpha(), config.getDelta());

        Map<Integer, EmotionState> atual = new HashMap<>();
        for (int i = 0; i < n; i++) {
            EmotionState inicial = estadosIniciais == null ? null : estadosIniciais.get(i);
            atual.put(i, inicial != null ? inicial : EmotionState.neutro());
        }

        List<Map<Integer, EmotionState>> historico = new ArrayList<>();
        historico.add(Map.copyOf(atual));

        for (int ciclo = 1; ciclo <= config.getNumCiclos(); ciclo++) {
            Map<Integer, EmotionState> proximo = new HashMap<>();
            for (int i = 0; i < n; i++) {
                Map<Integer, Double> pesos = rede.getVizinhosComPesos(i);
                if (pesos.isEmpty()) {
                    proximo.put(i, atual.get(i));
                    continue;
                }
                Map<Integer, EmotionState> influencias = new HashMap<>();
                for (int vizinho : pesos.keySet()) {
                    influencias.put(vizinho, atual.get(vizinho));
                }
                proximo.put(i, modelo.calcularProximoEstado(atual.get(i), influencias, pesos));
            }
            atual = proximo;
            historico.add(Map.copyOf(atual));
        }
        return historico;
    }
}
