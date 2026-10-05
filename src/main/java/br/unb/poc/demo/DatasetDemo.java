package br.unb.poc.demo;

import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Gera, em disco temporário, um pequeno dataset no mesmo formato do
 * Twitter Social Circles ({@code .edges}/{@code .feat}/{@code .featnames}),
 * organizado em comunidades com "hashtags" emocionalmente marcadas.
 *
 * <p>Não substitui o dataset real (ver {@code data/twitter/README.md});
 * serve para demonstrar e testar a interface (Etapas 4.9/5.0) e o pipeline
 * de inferência (Etapa 4.5, {@code HeuristicaTwitter}) sem exigir o
 * download do conjunto original.
 */
public final class DatasetDemo {

    private DatasetDemo() {}

    private static final String EGO_ID = "0";

    private static final String[][] VOCABULARIO_POR_COMUNIDADE = {
            {"#happy", "#love", "#win", "#good"},      // tende à alegria
            {"#angry", "#hate", "#frustrated"},        // tende à raiva
            {"#sad", "#lonely", "#cry"},                // tende à tristeza
            {"#disgusting", "#gross", "#awful"}         // tende ao nojo
    };

    private static final String[] NEUTRAS = {"@amigo1", "@amigo2", "device_iphone", "location_brasilia"};

    /**
     * Gera um dataset sintético com o número de agentes solicitado,
     * organizado em comunidades, e o carrega através do
     * {@link TwitterDatasetLoader} real (mesmo caminho de código usado
     * para o dataset original).
     */
    public static TwitterNetworkData gerar(long semente, int numAgentes) throws IOException {
        if (numAgentes < 4) numAgentes = 4;
        Random random = new Random(semente);
        int numComunidades = Math.max(2, Math.min(4, numAgentes / 6));

        // 1..numAgentes como identificadores externos (ego "0" não participa dos nós).
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= numAgentes; i++) ids.add(String.valueOf(i));

        int[] comunidadeDoNo = new int[numAgentes + 1];
        for (int i = 1; i <= numAgentes; i++) comunidadeDoNo[i] = (i - 1) % numComunidades;

        Set<Long> arestas = new LinkedHashSet<>();
        for (int i = 1; i <= numAgentes; i++) {
            int ligacoesInternas = 2 + random.nextInt(2);
            for (int k = 0; k < ligacoesInternas; k++) {
                int j = candidatoMesmaComunidade(random, numAgentes, comunidadeDoNo, comunidadeDoNo[i], i);
                adicionarAresta(arestas, i, j);
            }
            if (random.nextDouble() < 0.25) {
                int j = 1 + random.nextInt(numAgentes);
                if (j != i) adicionarAresta(arestas, i, j);
            }
        }
        // Garante que a rede é conexa: liga cada nó i ao nó i+1 num anel de baixo custo.
        for (int i = 1; i < numAgentes; i++) adicionarAresta(arestas, i, i + 1);
        adicionarAresta(arestas, numAgentes, 1);

        List<String> nomesFeatures = new ArrayList<>();
        Map<String, Integer> indiceFeature = new LinkedHashMap<>();
        for (String[] grupo : VOCABULARIO_POR_COMUNIDADE) {
            for (String tag : grupo) registrarFeature(nomesFeatures, indiceFeature, tag);
        }
        for (String tag : NEUTRAS) registrarFeature(nomesFeatures, indiceFeature, tag);

        StringBuilder edgesTxt = new StringBuilder();
        for (long par : arestas) {
            int a = (int) (par >> 32);
            int b = (int) (par & 0xffffffffL);
            edgesTxt.append(a).append(' ').append(b).append('\n');
        }

        StringBuilder featTxt = new StringBuilder();
        StringBuilder featnamesTxt = new StringBuilder();
        for (int i = 0; i < nomesFeatures.size(); i++) {
            featnamesTxt.append(i).append(' ').append(nomesFeatures.get(i)).append('\n');
        }
        for (int i = 1; i <= numAgentes; i++) {
            int[] vetor = new int[nomesFeatures.size()];
            String[] vocabComunidade = VOCABULARIO_POR_COMUNIDADE[comunidadeDoNo[i] % VOCABULARIO_POR_COMUNIDADE.length];

            int quantidadeMarcada = 1 + random.nextInt(2);
            for (int k = 0; k < quantidadeMarcada; k++) {
                String tag = vocabComunidade[random.nextInt(vocabComunidade.length)];
                vetor[indiceFeature.get(tag)] = 1;
            }
            String neutra = NEUTRAS[random.nextInt(NEUTRAS.length)];
            vetor[indiceFeature.get(neutra)] = 1;

            featTxt.append(i);
            for (int v : vetor) featTxt.append(' ').append(v);
            featTxt.append('\n');
        }

        Path raiz = Files.createTempDirectory("poc-emocoes-demo-twitter");
        Path pastaTwitter = Files.createDirectories(raiz.resolve("twitter"));
        Files.writeString(pastaTwitter.resolve(EGO_ID + ".edges"), edgesTxt.toString(), StandardCharsets.UTF_8);
        Files.writeString(pastaTwitter.resolve(EGO_ID + ".feat"), featTxt.toString(), StandardCharsets.UTF_8);
        Files.writeString(pastaTwitter.resolve(EGO_ID + ".featnames"), featnamesTxt.toString(), StandardCharsets.UTF_8);
        Files.writeString(pastaTwitter.resolve(EGO_ID + ".egofeat"), "0\n", StandardCharsets.UTF_8);

        return TwitterDatasetLoader.load(raiz, EGO_ID, 1.0);
    }

    private static int candidatoMesmaComunidade(
            Random random, int numAgentes, int[] comunidadeDoNo, int comunidade, int origem) {
        for (int tentativas = 0; tentativas < 20; tentativas++) {
            int j = 1 + random.nextInt(numAgentes);
            if (j != origem && comunidadeDoNo[j] == comunidade) return j;
        }
        int j = 1 + random.nextInt(numAgentes);
        return j == origem ? (j % numAgentes) + 1 : j;
    }

    private static void adicionarAresta(Set<Long> arestas, int a, int b) {
        if (a == b) return;
        int menor = Math.min(a, b), maior = Math.max(a, b);
        arestas.add((((long) menor) << 32) | (maior & 0xffffffffL));
    }

    private static void registrarFeature(List<String> nomes, Map<String, Integer> indice, String tag) {
        if (indice.containsKey(tag)) return;
        indice.put(tag, nomes.size());
        nomes.add(tag);
    }
}
