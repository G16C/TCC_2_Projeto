package br.unb.poc.analise;

import br.unb.poc.modelo.emocao.Emotion;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serializa a lista de {@link AnaliseComparativa.Cenario} em um único
 * documento JSON para consumo pelo dashboard de comparação.
 *
 * <p>Formato:
 * <pre>
 * { "cenarios": [
 *   { "nome", "estrutura": {numNos, numArestas, grauMedio, grauMaximo, grauMinimo,
 *                            densidade, coeficienteAgrupamentoMedio, distanciaMediaAprox,
 *                            numComponentesConexos, fracaoMaiorComponente},
 *     "propagacao": {cicloConvergencia,
 *                     distribuicaoInicial: {rotulo: contagem, ...},
 *                     distribuicaoFinal:   {rotulo: contagem, ...},
 *                     serieMedia: [{ciclo, anger, disgust, joy, sadness}, ...]} },
 *   ... ] }
 * </pre>
 */
public final class AnaliseExport {

    private AnaliseExport() {}

    public static String paraJson(List<AnaliseComparativa.Cenario> cenarios) {
        StringBuilder json = new StringBuilder(8192);
        json.append("{\"cenarios\":[");
        for (int i = 0; i < cenarios.size(); i++) {
            if (i > 0) json.append(',');
            escreverCenario(json, cenarios.get(i));
        }
        json.append("]}");
        return json.toString();
    }

    private static void escreverCenario(StringBuilder json, AnaliseComparativa.Cenario cenario) {
        json.append("{\"nome\":").append(str(cenario.nome()));
        escreverEstrutura(json, cenario.estrutura());
        escreverPropagacao(json, cenario.propagacao());
        json.append('}');
    }

    private static void escreverEstrutura(StringBuilder json, MetricasEstruturais e) {
        json.append(",\"estrutura\":{")
            .append("\"numNos\":").append(e.numNos())
            .append(",\"numArestas\":").append(e.numArestas())
            .append(",\"grauMedio\":").append(num(e.grauMedio()))
            .append(",\"grauMaximo\":").append(e.grauMaximo())
            .append(",\"grauMinimo\":").append(e.grauMinimo())
            .append(",\"densidade\":").append(num(e.densidade()))
            .append(",\"coeficienteAgrupamentoMedio\":").append(num(e.coeficienteAgrupamentoMedio()))
            .append(",\"distanciaMediaAprox\":").append(num(e.distanciaMediaAprox()))
            .append(",\"numComponentesConexos\":").append(e.numComponentesConexos())
            .append(",\"fracaoMaiorComponente\":").append(num(e.fracaoMaiorComponente()))
            .append('}');
    }

    private static void escreverPropagacao(StringBuilder json, MetricasPropagacao p) {
        json.append(",\"propagacao\":{")
            .append("\"cicloConvergencia\":").append(p.cicloConvergencia())
            .append(",\"distribuicaoInicial\":");
        escreverContagem(json, p.distribuicaoInicial());
        json.append(",\"distribuicaoFinal\":");
        escreverContagem(json, p.distribuicaoFinal());
        json.append(",\"serieMedia\":[");
        List<Map<Emotion, Double>> serie = p.serieMediaPorCiclo();
        for (int c = 0; c < serie.size(); c++) {
            if (c > 0) json.append(',');
            Map<Emotion, Double> media = serie.get(c);
            json.append("{\"ciclo\":").append(c)
                .append(",\"anger\":").append(num(media.get(Emotion.ANGER)))
                .append(",\"disgust\":").append(num(media.get(Emotion.DISGUST)))
                .append(",\"joy\":").append(num(media.get(Emotion.JOY)))
                .append(",\"sadness\":").append(num(media.get(Emotion.SADNESS)))
                .append('}');
        }
        json.append("]}");
    }

    private static void escreverContagem(StringBuilder json, Map<String, Integer> contagem) {
        json.append('{');
        boolean primeiro = true;
        for (Map.Entry<String, Integer> entrada : contagem.entrySet()) {
            if (!primeiro) json.append(',');
            primeiro = false;
            json.append(str(entrada.getKey())).append(':').append(entrada.getValue());
        }
        json.append('}');
    }

    private static String str(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\');
            sb.append(c);
        }
        return sb.append('"').toString();
    }

    private static String num(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) return "0";
        return String.format(Locale.US, "%.6f", valor);
    }
}
