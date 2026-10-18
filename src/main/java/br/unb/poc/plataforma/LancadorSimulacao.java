package br.unb.poc.plataforma;

import br.unb.poc.agentes.AgenteControle;
import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.topologia.TipoTopologia;
import br.unb.poc.dados.FonteDados;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;

import java.nio.file.Paths;

/**
 * Ponto de entrada da Prova de Conceito.
 *
 * <p>Inicializa a plataforma JADE programaticamente, cria o
 * {@code Main Container} e instancia o {@link AgenteControle},
 * que é responsável por todo o ciclo da simulação.
 *
 * <p>Modo de uso:
 * <pre>
 *   mvn package
 *   java -jar target/poc-emocoes-1.0.0.jar
 * </pre>
 *
 * <p>Para visualizar a GUI do JADE durante a execução, adicione
 * o argumento {@code -gui} na configuração do perfil JADE.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */
public class LancadorSimulacao {

    private static SimulationConfiguration criarConfiguracao(String[] args) {
        boolean verboso = false;
        if (args != null) {
            for (String arg : args) {
                if ("--verbose".equalsIgnoreCase(arg)) verboso = true;
            }
        }
        // Remove flags do array antes de interpretar as posições --twitter/egoId/seed.
        java.util.List<String> posicionais = new java.util.ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (!"--verbose".equalsIgnoreCase(arg)) posicionais.add(arg);
            }
        }
        String[] restantes = posicionais.toArray(new String[0]);

        if (restantes.length >= 3 && "--twitter".equalsIgnoreCase(restantes[0])) {
            SimulationConfiguration.Builder builder = SimulationConfiguration.builder()
                    .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                    .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                    .twitterDatasetPath(restantes[1])
                    .twitterEgoId(restantes[2])
                    .logDetalhado(verboso);

            if (restantes.length >= 4) {
                builder.twitterSeedNodeId(restantes[3]);
            }

            return builder.build();
        }

        return SimulationConfiguration.builder()
                .logDetalhado(verboso)
                .build();
    }

    public static void main(String[] args) {

        SimulationConfiguration configuracao = criarConfiguracao(args);

        System.out.println("=".repeat(60));
        System.out.println("  PoC - Propagação de Emoções em Redes Sociais Digitais");
        System.out.println("  TCC - Engenharia de Software, UnB, 2026");
        System.out.println("  Autor: Gabriel Campello Marques");
        System.out.println("=".repeat(60));
        if (configuracao.getFonteDados() == FonteDados.TWITTER_SOCIAL_CIRCLES) {
            System.out.println("  Agentes   : (definido pelo dataset do Twitter, carregado a seguir)");
        } else {
            System.out.printf("  Agentes   : %d%n", configuracao.getNumAgentes());
        }
        System.out.printf("  Ciclos    : %d%n", configuracao.getNumCiclos());
        System.out.printf("  Alpha (α) : %.2f%n", configuracao.getAlpha());
        System.out.printf("  Delta (δ) : %.2f%n", configuracao.getDelta());
        System.out.printf("  Fonte     : %s%n", configuracao.getFonteDados());
        System.out.printf("  Log       : %s (use --verbose para ciclo a ciclo)%n",
                configuracao.isLogDetalhado() ? "detalhado" : "resumido");
        if (configuracao.getFonteDados() == FonteDados.TWITTER_SOCIAL_CIRCLES) {
            System.out.printf("  Twitter   : ego=%s%n", configuracao.getTwitterEgoId());
            System.out.printf("  Dataset   : %s%n", configuracao.getTwitterDatasetPath());
        }
        System.out.println("=".repeat(60));
        System.out.println();

        // ---- Inicializar plataforma JADE ----
        Runtime rt = Runtime.instance();

        // Encerra a JVM automaticamente quando todos os containers forem destruídos
        rt.setCloseVM(false);

        Profile perfil = new ProfileImpl();
        perfil.setParameter(Profile.MAIN_HOST, "localhost");
        perfil.setParameter(Profile.MAIN_PORT, "1099");
        // Desabilita o MTP HTTP: esta PoC roda em um único container local e
        // não precisa de mensageria entre plataformas JADE distintas. Evita
        // também os avisos "HTTPServer error: ... XMLCodec cannot access
        // com.sun.org.apache.xerces..." que aparecem em JDKs mais novos
        // (módulo java.xml não exporta essa classe interna por padrão).
        perfil.setParameter(Profile.NO_MTP, "true");
        // Desabilita GUI para execução headless (remova para ativar a interface gráfica)
        perfil.setParameter(Profile.GUI, "true");

        ContainerController mainContainer = rt.createMainContainer(perfil);

        // ---- Criar e iniciar o Agente de Controle ----
        try {

            AgentController controle = mainContainer.createNewAgent(
                    configuracao.getNomeAgenteControle(),
                    AgenteControle.class.getName(),
                    new Object[]{configuracao}
            );

            controle.start();


            // O Sniffer da PoC usa uma lista fixa de agentes. Para uma fonte
            // externa, o número de nós é conhecido somente após o carregamento.
            // A visualização da rede será substituída pela interface do TCC II.
            Thread.sleep(3000);

            int quantidadeAgentes = configuracao.getNumAgentes();

            if (configuracao.getFonteDados() == FonteDados.TWITTER_SOCIAL_CIRCLES) {

                try {
                    TwitterNetworkData dadosTwitter = TwitterDatasetLoader.load(
                            Paths.get(configuracao.getTwitterDatasetPath()),
                            configuracao.getTwitterEgoId(),
                            1.0
                    );

                    quantidadeAgentes = dadosTwitter.getNumAgentes();

                    System.out.printf(
                            "  Sniffer: rede Twitter carregada com %d agentes%n",
                            quantidadeAgentes
                    );

                } catch (Exception e) {
                    System.err.println(
                            "Não foi possível carregar a rede Twitter para configurar o Sniffer."
                    );
                    e.printStackTrace();
                }
            }

            Object[] snifferArgs = new Object[quantidadeAgentes + 1];

            for (int i = 0; i < quantidadeAgentes; i++) {
                snifferArgs[i] =
                        configuracao.getPrefixoUsuario() + i;
            }

            snifferArgs[quantidadeAgentes] =
                    configuracao.getNomeAgenteControle();

            AgentController sniffer = mainContainer.createNewAgent(
                    "Sniffer",
                    "jade.tools.sniffer.Sniffer",
                    snifferArgs
            );

            sniffer.start();

            System.out.println("Sniffer JADE iniciado.");
        } catch (Exception e) {
            System.err.println("Falha ao iniciar o Agente de Controle.");
            e.printStackTrace();
        }
    }
}
