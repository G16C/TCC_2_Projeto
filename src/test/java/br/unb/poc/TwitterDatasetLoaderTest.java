package br.unb.poc;

import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TwitterDatasetLoaderTest {

    @Test
    void deveCarregarRedeTwitterDeDiretorio() throws Exception {
        Path raiz = Files.createTempDirectory("twitter-test");
        Path twitter = Files.createDirectories(raiz.resolve("twitter"));

        Files.writeString(twitter.resolve("42.edges"),
                "100 200\n200 300\n100 200\n");

        Files.writeString(twitter.resolve("42.feat"),
                "100 1 0 1\n200 0 1 0\n300 1 1 0\n");

        Files.writeString(twitter.resolve("42.featnames"),
                "0 #joy\n1 #sad\n2 @user\n");

        Files.writeString(twitter.resolve("42.egofeat"),
                "1 0 0\n");

        TwitterNetworkData dados =
                TwitterDatasetLoader.load(raiz, "42", 1.0);

        assertEquals("42", dados.getEgoId());
        assertEquals(3, dados.getNumAgentes());
        assertEquals(3, dados.getFeatures().size());
        assertEquals(3, dados.getNomesFeatures().size());

        int arestas = dados.getAdjacencia().stream()
                .mapToInt(java.util.Map::size)
                .sum() / 2;

        assertEquals(2, arestas);
        assertTrue(dados.getIdsExternos().contains("100"));
        assertTrue(dados.getIdsExternos().contains("200"));
        assertTrue(dados.getIdsExternos().contains("300"));
    }
}
