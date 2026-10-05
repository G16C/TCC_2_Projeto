package br.unb.poc.dados;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Carrega uma rede do conjunto Twitter Social Circles.
 *
 * <p>O conjunto é organizado em grupos de arquivos com o mesmo identificador
 * de ego: .edges, .feat, .featnames, .egofeat e .circles.
 *
 * <p>Esta implementação aceita tanto a pasta extraída quanto o arquivo
 * .tar.gz original, sem exigir uma biblioteca externa para leitura de TAR.
 *
 * <p>Importante: o conjunto fornece estrutura de rede e atributos binários,
 * mas não fornece diretamente rótulos emocionais das postagens. Portanto,
 * esta classe não atribui emoções aos usuários. A extração/calibração
 * emocional será uma etapa posterior.
 */
public final class TwitterDatasetLoader {

    private TwitterDatasetLoader() {}

    public static TwitterNetworkData load(Path fonte, String egoId, double pesoConexao)
            throws IOException {

        if (Files.isDirectory(fonte)) {
            return loadFromDirectory(fonte, egoId, pesoConexao);
        }

        String nome = fonte.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nome.endsWith(".tar.gz") || nome.endsWith(".tgz")) {
            return loadFromTarGz(fonte, egoId, pesoConexao);
        }

        throw new IOException("Fonte Twitter deve ser uma pasta extraída ou um arquivo .tar.gz: " + fonte);
    }

    private static TwitterNetworkData loadFromDirectory(
            Path diretorio, String egoId, double pesoConexao) throws IOException {

        Path raiz = diretorio.resolve("twitter");
        if (!Files.isDirectory(raiz)) {
            raiz = diretorio;
        }

        Path edges = raiz.resolve(egoId + ".edges");
        Path feat = raiz.resolve(egoId + ".feat");
        Path featnames = raiz.resolve(egoId + ".featnames");
        Path egofeat = raiz.resolve(egoId + ".egofeat");

        if (!Files.exists(edges)) {
            throw new FileNotFoundException("Arquivo de arestas não encontrado: " + edges);
        }

        List<String> edgeLines = Files.readAllLines(edges, StandardCharsets.UTF_8);
        List<String> featLines = Files.exists(feat)
                ? Files.readAllLines(feat, StandardCharsets.UTF_8)
                : List.of();
        List<String> featnameLines = Files.exists(featnames)
                ? Files.readAllLines(featnames, StandardCharsets.UTF_8)
                : List.of();
        List<String> egoFeatureLines = Files.exists(egofeat)
                ? Files.readAllLines(egofeat, StandardCharsets.UTF_8)
                : List.of();

        return build(egoId, edgeLines, featLines, featnameLines, egoFeatureLines, pesoConexao);
    }

    private static TwitterNetworkData loadFromTarGz(
            Path arquivo, String egoId, double pesoConexao) throws IOException {

        String prefixo = "twitter/" + egoId + ".";
        String edges = null;
        String feat = null;
        String featnames = null;
        String egofeat = null;

        try (InputStream fis = Files.newInputStream(arquivo);
             GZIPInputStream gzip = new GZIPInputStream(fis)) {

            byte[] header = new byte[512];

            while (true) {
                int lidos = readFully(gzip, header);
                if (lidos == 0) break;
                if (lidos != 512) throw new IOException("Entrada TAR truncada.");

                boolean vazio = true;
                for (byte b : header) {
                    if (b != 0) {
                        vazio = false;
                        break;
                    }
                }
                if (vazio) break;

                String nome = ascii(header, 0, 100);
                long tamanho = parseOctal(header, 124, 12);
                byte tipo = header[156];

                ByteArrayOutputStream conteudo = new ByteArrayOutputStream();
                if (tipo == 0 || tipo == '0') {
                    copyExactly(gzip, conteudo, tamanho);
                } else {
                    skipExactly(gzip, tamanho);
                }

                long padding = (512 - (tamanho % 512)) % 512;
                skipExactly(gzip, padding);

                if (nome.equals(prefixo + "edges")) edges = conteudo.toString(StandardCharsets.UTF_8);
                else if (nome.equals(prefixo + "feat")) feat = conteudo.toString(StandardCharsets.UTF_8);
                else if (nome.equals(prefixo + "featnames")) featnames = conteudo.toString(StandardCharsets.UTF_8);
                else if (nome.equals(prefixo + "egofeat")) egofeat = conteudo.toString(StandardCharsets.UTF_8);
            }
        }

        if (edges == null) {
            throw new FileNotFoundException(
                    "Ego '" + egoId + "' não encontrado no dataset Twitter: " + arquivo);
        }

        return build(
                egoId,
                lines(edges),
                lines(feat),
                lines(featnames),
                lines(egofeat),
                pesoConexao);
    }

    private static TwitterNetworkData build(
            String egoId,
            List<String> edgeLines,
            List<String> featLines,
            List<String> featnameLines,
            List<String> egoFeatureLines,
            double pesoConexao) {

        TreeSet<String> ids = new TreeSet<>();

        for (String linha : edgeLines) {
            String[] p = tokens(linha);
            if (p.length >= 2) {
                ids.add(p[0]);
                ids.add(p[1]);
            }
        }

        Map<String, int[]> features = new HashMap<>();
        for (String linha : featLines) {
            String[] p = tokens(linha);
            if (p.length < 1) continue;

            int[] vetor = new int[p.length - 1];
            for (int i = 1; i < p.length; i++) {
                vetor[i - 1] = Integer.parseInt(p[i]);
            }
            features.put(p[0], vetor);
            ids.add(p[0]);
        }

        List<String> nomesFeatures = new ArrayList<>();
        for (String linha : featnameLines) {
            String[] p = tokensPreserveFeatureName(linha);
            if (p.length >= 2) {
                nomesFeatures.add(p[1]);
            }
        }

        int[] featuresEgo = parseVector(egoFeatureLines);

        List<String> idsExternos = new ArrayList<>(ids);
        Map<String, Integer> local = new HashMap<>();
        for (int i = 0; i < idsExternos.size(); i++) {
            local.put(idsExternos.get(i), i);
        }

        List<Map<Integer, Double>> adj = new ArrayList<>();
        for (int i = 0; i < idsExternos.size(); i++) {
            adj.add(new HashMap<>());
        }

        for (String linha : edgeLines) {
            String[] p = tokens(linha);
            if (p.length < 2) continue;

            Integer a = local.get(p[0]);
            Integer b = local.get(p[1]);

            if (a == null || b == null || a.equals(b)) continue;

            // O arquivo .edges representa relações não direcionadas.
            adj.get(a).put(b, pesoConexao);
            adj.get(b).put(a, pesoConexao);
        }

        return new TwitterNetworkData(
                egoId,
                idsExternos,
                adj,
                features,
                nomesFeatures,
                featuresEgo);
    }

    private static String[] tokens(String linha) {
        if (linha == null) return new String[0];
        linha = linha.trim();
        return linha.isEmpty() ? new String[0] : linha.split("\\s+");
    }

    private static String[] tokensPreserveFeatureName(String linha) {
        if (linha == null) return new String[0];
        String trimmed = linha.trim();
        if (trimmed.isEmpty()) return new String[0];
        int pos = trimmed.indexOf(' ');
        if (pos < 0) return new String[] { trimmed };
        return new String[] {
                trimmed.substring(0, pos),
                trimmed.substring(pos + 1)
        };
    }

    private static int[] parseVector(List<String> linhas) {
        for (String linha : linhas) {
            String[] p = tokens(linha);
            int[] vetor = new int[p.length];
            for (int i = 0; i < p.length; i++) {
                vetor[i] = Integer.parseInt(p[i]);
            }
            return vetor;
        }
        return new int[0];
    }

    private static List<String> lines(String texto) {
        if (texto == null || texto.isEmpty()) return List.of();
        return Arrays.asList(texto.split("\\R"));
    }

    private static String ascii(byte[] b, int off, int len) {
        int fim = off;
        while (fim < off + len && b[fim] != 0) fim++;
        return new String(b, off, fim - off, StandardCharsets.US_ASCII);
    }

    private static long parseOctal(byte[] b, int off, int len) {
        long valor = 0;
        int i = off;
        int fim = off + len;
        while (i < fim && (b[i] == 0 || b[i] == ' ')) i++;
        while (i < fim && b[i] >= '0' && b[i] <= '7') {
            valor = (valor << 3) + (b[i] - '0');
            i++;
        }
        return valor;
    }

    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int n = in.read(buffer, total, buffer.length - total);
            if (n < 0) break;
            total += n;
        }
        return total;
    }

    private static void copyExactly(InputStream in, OutputStream out, long tamanho)
            throws IOException {
        byte[] buffer = new byte[8192];
        long restante = tamanho;
        while (restante > 0) {
            int n = in.read(buffer, 0, (int) Math.min(buffer.length, restante));
            if (n < 0) throw new EOFException("Entrada TAR truncada.");
            out.write(buffer, 0, n);
            restante -= n;
        }
    }

    private static void skipExactly(InputStream in, long tamanho) throws IOException {
        long restante = tamanho;
        while (restante > 0) {
            long n = in.skip(restante);
            if (n > 0) {
                restante -= n;
                continue;
            }
            if (in.read() < 0) throw new EOFException("Entrada TAR truncada.");
            restante--;
        }
    }
}
