package br.unb.poc.dados;

import java.util.*;

/**
 * Representa uma rede social real carregada do conjunto Twitter Social Circles.
 *
 * <p>Os identificadores originais do Twitter são preservados separadamente
 * dos índices locais usados pelo JADE. Isso permite que o motor de simulação
 * utilize índices compactos (0..N-1) sem perder a rastreabilidade da origem.
 */
public final class TwitterNetworkData {

    private final String egoId;
    private final List<String> idsExternos;
    private final List<Map<Integer, Double>> adjacencia;
    private final Map<String, int[]> features;
    private final List<String> nomesFeatures;
    private final int[] featuresEgo;

    public TwitterNetworkData(
            String egoId,
            List<String> idsExternos,
            List<Map<Integer, Double>> adjacencia,
            Map<String, int[]> features,
            List<String> nomesFeatures,
            int[] featuresEgo) {

        this.egoId = Objects.requireNonNull(egoId);
        this.idsExternos = List.copyOf(idsExternos);

        List<Map<Integer, Double>> copia = new ArrayList<>();
        for (Map<Integer, Double> mapa : adjacencia) {
            copia.add(Collections.unmodifiableMap(new HashMap<>(mapa)));
        }
        this.adjacencia = Collections.unmodifiableList(copia);
        this.features = Collections.unmodifiableMap(new HashMap<>(features));
        this.nomesFeatures = List.copyOf(nomesFeatures);
        this.featuresEgo = featuresEgo == null ? new int[0] : Arrays.copyOf(featuresEgo, featuresEgo.length);
    }

    public String getEgoId() {
        return egoId;
    }

    public List<String> getIdsExternos() {
        return idsExternos;
    }

    public String getIdExterno(int idLocal) {
        return idsExternos.get(idLocal);
    }

    public List<Map<Integer, Double>> getAdjacencia() {
        return adjacencia;
    }

    public Map<String, int[]> getFeatures() {
        return features;
    }

    public List<String> getNomesFeatures() {
        return nomesFeatures;
    }

    public int[] getFeaturesEgo() {
        return Arrays.copyOf(featuresEgo, featuresEgo.length);
    }

    public int getNumAgentes() {
        return idsExternos.size();
    }

    public int getIdLocal(String idExterno) {
        return idsExternos.indexOf(idExterno);
    }
}
