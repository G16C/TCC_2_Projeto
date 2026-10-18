package br.unb.poc.modelo.emocao;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Heurística determinística para estimar emoções a partir das features
 * disponibilizadas pelo Twitter Social Circles.
 *
 * <p>As features são indicadores binários de atributos associados aos
 * usuários. Elas não constituem rótulos emocionais. A heurística utiliza
 * apenas regras explicitamente cadastradas e deixa as demais features
 * neutras, evitando atribuições arbitrárias.</p>
 *
 * <p>As regras são representadas por pesos independentes para cada emoção.
 * O score bruto de cada emoção é a soma dos pesos das features presentes.
 * O resultado é normalizado por um limite configurável e restringido a
 * [0,1].</p>
 *
 * <h2>Referências</h2>
 * <ul>
 *   <li>As quatro emoções mapeadas (raiva, nojo, alegria, tristeza)
 *   seguem o mesmo conjunto usado pelo modelo de propagação, definido em
 *   Fan, Xu e Zhao (2018) — ver {@link FanEmotionModel}.</li>
 *   <li>O uso de hashtags como indicador (proxy) de emoção — em vez de
 *   um rótulo emocional explícito do dataset — segue a abordagem de
 *   "distant supervision"/rotulação fraca amplamente usada na literatura
 *   de análise de emoção em redes sociais:
 *   Mohammad, S. M. (2012). <i>#Emotional Tweets</i>. Proceedings of the
 *   First Joint Conference on Lexical and Computational Semantics
 *   (*SEM 2012); Wang, W., Chen, L., Thirunarayan, K., &amp; Sheth, A. P.
 *   (2012). <i>Harnessing Twitter "Big Data" for Automatic Emotion
 *   Identification</i>. IEEE SocialCom/PASSAT; Suttles, J., &amp; Ide, N.
 *   (2013). <i>Distant Supervision for Emotion Classification with
 *   Discrete Binary Values</i>. CICLing.</li>
 *   <li><b>O peso de 0,70 usado em {@link #regrasPadrao()} é uma
 *   constante conservadora escolhida manualmente</b> (não derivada
 *   estatisticamente de nenhuma das referências acima), para que uma
 *   único hashtag reconhecida já produza um sinal claro sem saturar a
 *   escala [0,1]. Para recalibrar com dados próprios, construa a
 *   heurística com um mapa de regras alternativo:
 *   {@code new HeuristicaTwitter(minhasRegras, normalizador)}.</li>
 * </ul>
 */
public final class HeuristicaTwitter implements HeuristicaEmocional {

    /** Regra de uma feature. */
    public record Regra(String feature, Map<Emotion, Double> pesos) {
        public Regra {
            Objects.requireNonNull(feature);
            Objects.requireNonNull(pesos);
            EnumMap<Emotion, Double> copia = new EnumMap<>(Emotion.class);
            copia.putAll(pesos);
            pesos = Collections.unmodifiableMap(copia);
        }
    }

    private final Map<String, Regra> regras;
    private final double normalizador;
    private final boolean correspondenciaParcial;

    public HeuristicaTwitter() {
        this(regrasPadrao(), 1.0, false);
    }

    public HeuristicaTwitter(Map<String, Regra> regras, double normalizador) {
        this(regras, normalizador, false);
    }

    public HeuristicaTwitter(Map<String, Regra> regras, double normalizador, boolean correspondenciaParcial) {
        if (normalizador <= 0) throw new IllegalArgumentException("normalizador deve ser > 0");
        this.regras = Collections.unmodifiableMap(new LinkedHashMap<>(regras));
        this.normalizador = normalizador;
        this.correspondenciaParcial = correspondenciaParcial;
    }

    @Override
    public EmotionState inferir(Map<String, Integer> features) {
        EnumMap<Emotion, Double> scores = new EnumMap<>(Emotion.class);
        for (Emotion e : Emotion.values()) scores.put(e, 0.0);

        if (features != null) {
            for (Map.Entry<String, Integer> entrada : features.entrySet()) {
                if (entrada.getValue() == null || entrada.getValue() <= 0) continue;
                Regra regra = localizarRegra(entrada.getKey());
                if (regra == null) continue;
                for (Emotion emotion : Emotion.values()) {
                    scores.merge(emotion, regra.pesos().getOrDefault(emotion, 0.0), Double::sum);
                }
            }
        }

        EnumMap<Emotion, Double> normalizados = new EnumMap<>(Emotion.class);
        for (Emotion emotion : Emotion.values()) {
            normalizados.put(emotion, clamp(scores.get(emotion) / normalizador));
        }
        return new EmotionState(normalizados);
    }

    /** Converte o vetor binário + nomes em um mapa legível por feature. */
    public EmotionState inferir(int[] vetor, List<String> nomes) {
        Map<String, Integer> ativas = new LinkedHashMap<>();
        if (vetor == null || nomes == null) return inferir(ativas);
        int limite = Math.min(vetor.length, nomes.size());
        for (int i = 0; i < limite; i++) {
            if (vetor[i] > 0) ativas.put(normalizarFeature(nomes.get(i)), vetor[i]);
        }
        return inferir(ativas);
    }

    public Map<String, Regra> getRegras() { return regras; }

    public Set<String> getFeaturesReconhecidas() {
        return regras.keySet();
    }

    private Regra localizarRegra(String feature) {
        if (feature == null) return null;
        String chave = normalizarFeature(feature);
        Regra direta = regras.get(chave);
        if (direta != null) return direta;
        if (!correspondenciaParcial) return null;
        for (Regra regra : regras.values()) {
            if (chave.contains(regra.feature()) || regra.feature().contains(chave)) return regra;
        }
        return null;
    }

    public static String normalizarFeature(String feature) {
        return feature == null ? "" : feature.trim().toLowerCase(Locale.ROOT);
    }

    private static double clamp(double x) { return Math.max(0.0, Math.min(1.0, x)); }

    /**
     * Conjunto inicial propositalmente conservador.
     *
     * <p>Somente padrões semanticamente claros são mapeados. O conjunto
     * pode ser calibrado posteriormente com literatura/dados rotulados.
     */
    public static Map<String, Regra> regrasPadrao() {
        Map<String, Regra> r = new LinkedHashMap<>();
        // Vocabulário inicial conservador. Estes termos são indicadores
        // lexicais das features e não rótulos emocionais do dataset.
        adicionar(r, Emotion.JOY, 0.70,
                "#love", "#lovely", "#happy", "#happiness", "#fun", "#winning",
                "#win", "#success", "#awesome", "#smile", "#beautiful", "#good");
        adicionar(r, Emotion.ANGER, 0.70,
                "#anger", "#angry", "#hate", "#hatred", "#mad", "#furious",
                "#annoyed", "#fail", "#failure", "#frustrated", "#frustration");
        adicionar(r, Emotion.SADNESS, 0.70,
                "#sad", "#sadness", "#sorry", "#cry", "#tears", "#loss",
                "#lost", "#alone", "#lonely", "#depressed");
        adicionar(r, Emotion.DISGUST, 0.70,
                "#disgust", "#disgusting", "#gross", "#nasty", "#sick",
                "#awful", "#horrible");
        return r;
    }

    private static void adicionar(Map<String, Regra> regras, Emotion emotion,
                                   double peso, String... features) {
        for (String feature : features) {
            regras.put(normalizarFeature(feature), regra(feature, emotion, peso));
        }
    }

    public static Regra regra(String feature, Emotion emotion, double peso) {
        if (peso < 0) throw new IllegalArgumentException("peso deve ser >= 0");
        return new Regra(normalizarFeature(feature), Map.of(emotion, peso));
    }
}
