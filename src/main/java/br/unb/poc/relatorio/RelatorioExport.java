package br.unb.poc.relatorio;

import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;

import java.util.List;
import java.util.Map;

/**
 * Etapa 4.9 — Preparar dados para a interface.
 *
 * <p>Converte a topologia da rede ({@link RedeSocial}) e o histórico de
 * estados emocionais por ciclo em um único documento JSON, desacoplado
 * de JADE, PostgreSQL e Swing/HTML. Qualquer camada de apresentação
 * (dashboard web, app desktop, exportação para o TCC) pode consumir o
 * mesmo contrato de dados.
 *
 * <p>Formato produzido:
 * <pre>
 * {
 *   "meta": { "numAgentes", "numCiclos", "alpha", "delta", "fonteDados",
 *             "twitterEgoId", "indiceSemente", "semente" },
 *   "nos": [ { "id", "idExterno", "grau" }, ... ],
 *   "arestas": [ { "origem", "destino", "peso" }, ... ],
 *   "ciclos": [ { "ciclo", "media": {anger,disgust,joy,sadness},
 *                 "estados": [ {"id","anger","disgust","joy","sadness","dominante"}, ... ] }, ... ]
 * }
 * </pre>
 */
public final class RelatorioExport {

    private RelatorioExport() {}

    public static String paraJson(
            RedeSocial rede,
            SimulationConfiguration config,
            List<Map<Integer, EmotionState>> historico,
            int indiceSemente) {

        int n = rede.getNumAgentes();
        StringBuilder json = new StringBuilder(4096);

        json.append("{");
        escreverMeta(json, rede, config, historico.size(), indiceSemente);
        json.append(",\"nos\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) json.append(',');
            json.append("{\"id\":").append(i)
                .append(",\"idExterno\":").append(Json.str(rede.getIdExterno(i)))
                .append(",\"grau\":").append(rede.getVizinhos(i).size())
                .append('}');
        }
        json.append("],\"arestas\":[");
        boolean primeiraAresta = true;
        for (int i = 0; i < n; i++) {
            for (Map.Entry<Integer, Double> vizinho : rede.getVizinhosComPesos(i).entrySet()) {
                int j = vizinho.getKey();
                if (j <= i) continue; // grafo não-dirigido: evita duplicar a aresta i-j/j-i
                if (!primeiraAresta) json.append(',');
                primeiraAresta = false;
                json.append("{\"origem\":").append(i)
                    .append(",\"destino\":").append(j)
                    .append(",\"peso\":").append(Json.num(vizinho.getValue()))
                    .append('}');
            }
        }
        json.append("],\"ciclos\":[");
        for (int c = 0; c < historico.size(); c++) {
            if (c > 0) json.append(',');
            escreverCiclo(json, c, historico.get(c), n);
        }
        json.append("]}");
        return json.toString();
    }

    private static void escreverMeta(
            StringBuilder json, RedeSocial rede, SimulationConfiguration config,
            int numCiclosGerados, int indiceSemente) {

        json.append("\"meta\":{")
            .append("\"numAgentes\":").append(rede.getNumAgentes())
            .append(",\"numCiclos\":").append(numCiclosGerados)
            .append(",\"alpha\":").append(Json.num(config.getAlpha()))
            .append(",\"delta\":").append(Json.num(config.getDelta()))
            .append(",\"fonteDados\":").append(Json.str(config.getFonteDados().name()))
            .append(",\"twitterEgoId\":").append(rede.usaDadosTwitter()
                    ? Json.str(rede.getDadosTwitter().getEgoId()) : "null")
            .append(",\"indiceSemente\":").append(indiceSemente)
            .append(",\"semente\":").append(config.getSemente())
            .append('}');
    }

    private static void escreverCiclo(
            StringBuilder json, int ciclo, Map<Integer, EmotionState> estados, int numAgentes) {

        double somaAnger = 0, somaDisgust = 0, somaJoy = 0, somaSadness = 0;
        int contados = 0;

        json.append("{\"ciclo\":").append(ciclo).append(",\"estados\":[");
        for (int i = 0; i < numAgentes; i++) {
            EmotionState estado = estados.get(i);
            if (estado == null) continue;
            if (contados > 0) json.append(',');
            contados++;

            double anger = estado.getIntensity(Emotion.ANGER);
            double disgust = estado.getIntensity(Emotion.DISGUST);
            double joy = estado.getIntensity(Emotion.JOY);
            double sadness = estado.getIntensity(Emotion.SADNESS);
            somaAnger += anger;
            somaDisgust += disgust;
            somaJoy += joy;
            somaSadness += sadness;

            json.append("{\"id\":").append(i)
                .append(",\"anger\":").append(Json.num(anger))
                .append(",\"disgust\":").append(Json.num(disgust))
                .append(",\"joy\":").append(Json.num(joy))
                .append(",\"sadness\":").append(Json.num(sadness))
                .append(",\"dominante\":").append(Json.str(estado.getRotuloDominante()))
                .append('}');
        }
        int base = Math.max(contados, 1);
        json.append("],\"media\":{")
            .append("\"anger\":").append(Json.num(somaAnger / base))
            .append(",\"disgust\":").append(Json.num(somaDisgust / base))
            .append(",\"joy\":").append(Json.num(somaJoy / base))
            .append(",\"sadness\":").append(Json.num(somaSadness / base))
            .append("}}");
    }
}
