package br.unb.poc.relatorio;

import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.emocao.HeuristicaTwitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Ponto único para inferir o estado emocional inicial de uma rede a
 * partir de dados do Twitter, reproduzindo a mesma regra usada por
 * {@code AgenteControle}/{@code UsuarioAgent} (Etapa 4.5/4.6): quando a
 * rede vem do Twitter, o estado inicial de cada agente é o inferido pela
 * heurística a partir das suas features; agentes sem features
 * reconhecidas ficam neutros.
 *
 * <p>Extraído de {@code LancadorInterface} para ser reaproveitado também
 * por {@code AnaliseComparativa}, evitando duas implementações divergentes
 * da mesma regra.
 */
public final class EstadosIniciais {

    private EstadosIniciais() {}

    /** Infere o estado inicial de cada agente da rede via {@link HeuristicaTwitter}. */
    public static Map<Integer, EmotionState> inferirComHeuristica(RedeSocial rede) {
        HeuristicaTwitter heuristica = new HeuristicaTwitter();
        Map<Integer, EmotionState> estados = new HashMap<>();
        for (int i = 0; i < rede.getNumAgentes(); i++) {
            estados.put(i, heuristica.inferir(rede.getFeaturesTwitter(i), rede.getNomesFeaturesTwitter()));
        }
        return estados;
    }
}
