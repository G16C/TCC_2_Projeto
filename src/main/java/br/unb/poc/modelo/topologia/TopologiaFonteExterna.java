package br.unb.poc.modelo.topologia;

import java.util.List;
import java.util.Map;
import java.util.Random;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;
import java.nio.file.Path;

/**
 * Topologia carregada a partir de uma fonte de dados externa
 * (arquivo de arestas, API ou banco de dados de uma rede social real).
 *
 * <p><b>Stub — implementação pendente.</b> O formato exato da fonte de
 * dados (arquivo local vs. serviço remoto; presença ou não de pesos e
 * estados emocionais iniciais; leitura única vs. atualização periódica)
 * ainda está em definição e será tratado na segunda etapa do trabalho
 * (TCC II), conforme discutido na seção 5.2.2 da monografia.
 *
 * <p>Quando o formato for definido, esta classe deve:
 * <ol>
 *   <li>Ler a fonte de dados (ex.: {@code Files.readAllLines} para um CSV
 *       de arestas no formato {@code origem,destino,peso}, ou uma chamada
 *       HTTP/JDBC para uma fonte remota);</li>
 *   <li>Validar que os índices de nó estão no intervalo {@code [0, numAgentes)};</li>
 *   <li>Popular um {@code Map<Integer, Double>} por agente com os pesos
 *       reais informados pela fonte (em vez do peso uniforme usado pelas
 *       topologias sintéticas); e</li>
 *   <li>Garantir a simetria das conexões, a menos que se decida modelar
 *       influência assimétrica explicitamente — o que exigiria também
 *       ajustes em {@code RedeSocial} e {@code UsuarioAgent}.</li>
 * </ol>
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public class TopologiaFonteExterna implements GeradorTopologia {

    private TwitterNetworkData dadosTwitter;

    @Override
    public List<Map<Integer, Double>> gerar(int numAgentes, Random random, SimulationConfiguration configuracao) {
        if (configuracao.getFonteDados() != br.unb.poc.dados.FonteDados.TWITTER_SOCIAL_CIRCLES) {
            throw new IllegalArgumentException(
                    "TopologiaFonteExterna requer FonteDados.TWITTER_SOCIAL_CIRCLES.");
        }

        try {
            this.dadosTwitter = TwitterDatasetLoader.load(
                    Path.of(configuracao.getTwitterDatasetPath()),
                    configuracao.getTwitterEgoId(),
                    configuracao.getPesoConexao());
            return dadosTwitter.getAdjacencia();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Não foi possível carregar o dataset Twitter: " + e.getMessage(), e);
        }
    }

    public TwitterNetworkData getDadosTwitter() {
        return dadosTwitter;
    }
}
