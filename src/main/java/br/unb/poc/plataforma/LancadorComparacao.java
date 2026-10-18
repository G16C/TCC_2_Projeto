package br.unb.poc.plataforma;

import br.unb.poc.analise.AnaliseComparativa;
import br.unb.poc.analise.AnaliseExport;
import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.interfaces.DashboardServer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

/**
 * Compara a rede real do Twitter com topologias sintéticas (Small-World,
 * Scale-Free, Modular) do mesmo tamanho e com a mesma quantidade de
 * nós-semente por emoção, apresentando o resultado em um dashboard web
 * dedicado — ver {@code docs/ETAPAS_EVOLUCAO.md}, seção "Análise
 * comparativa".
 *
 * <p>Uso:
 * <pre>
 *   java ... br.unb.poc.plataforma.LancadorComparacao &lt;caminho&gt; &lt;egoId&gt; [porta] [numCiclos] [alpha] [delta] [semente]
 * </pre>
 *
 * Ou via Maven:
 * <pre>
 *   mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorComparacao \
 *     -Dexec.args="/caminho/para/o/dataset 11681802 8081 20 0.6 0.02"
 * </pre>
 */
public final class LancadorComparacao {

    private LancadorComparacao() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: LancadorComparacao <caminho do dataset> <egoId> [porta] [numCiclos] [alpha] [delta] [semente]");
            System.exit(1);
            return;
        }
        String caminho = args[0];
        String egoId = args[1];
        int porta = args.length > 2 ? Integer.parseInt(args[2]) : 8081;
        int numCiclos = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        double alpha = args.length > 4 ? Double.parseDouble(args[4]) : 0.6;
        double delta = args.length > 5 ? Double.parseDouble(args[5]) : 0.02;
        long semente = args.length > 6 ? Long.parseLong(args[6]) : 42L;

        System.out.println("=".repeat(70));
        System.out.println("  Análise comparativa: rede real do Twitter x redes sintéticas");
        System.out.println("=".repeat(70));
        System.out.printf("  Dataset : %s | ego=%s%n", caminho, egoId);
        System.out.printf("  Ciclos  : %d | alpha=%.2f | delta=%.2f%n", numCiclos, alpha, delta);

        TwitterNetworkData dadosTwitter = TwitterDatasetLoader.load(Paths.get(caminho), egoId, 1.0);
        System.out.println("  Rede real carregada: " + dadosTwitter.getNumAgentes() + " agentes.");
        System.out.println("  Gerando topologias sintéticas comparáveis (Small-World, Scale-Free, Modular)...");

        List<AnaliseComparativa.Cenario> cenarios =
                AnaliseComparativa.executar(dadosTwitter, alpha, delta, numCiclos, semente, "Twitter (real)");

        for (AnaliseComparativa.Cenario c : cenarios) {
            System.out.printf("  - %-24s | nós=%-4d arestas=%-5d grau médio=%.2f agrupamento=%.3f dist.média=%.2f%n",
                    c.nome(), c.estrutura().numNos(), c.estrutura().numArestas(),
                    c.estrutura().grauMedio(), c.estrutura().coeficienteAgrupamentoMedio(),
                    c.estrutura().distanciaMediaAprox());
        }

        String json = AnaliseExport.paraJson(cenarios);

        DashboardServer servidor = new DashboardServer(porta);
        servidor.usarPaginaComparacaoComoInicial(true);
        servidor.atualizarComparacao(json);
        servidor.iniciar();
        abrirNavegador(porta);
    }

    private static void abrirNavegador(int porta) {
        String url = "http://localhost:" + porta + "/";
        System.out.println();
        System.out.println("  Comparação disponível em: " + url);
        System.out.println();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception e) {
            System.out.println("  Não foi possível abrir automaticamente via Desktop (" + e.getMessage() + ").");
        }
        String so = System.getProperty("os.name", "").toLowerCase();
        String[] comando = so.contains("win")
                ? new String[]{"rundll32", "url.dll,FileProtocolHandler", url}
                : so.contains("mac") ? new String[]{"open", url} : new String[]{"xdg-open", url};
        try {
            new ProcessBuilder(comando).start();
        } catch (IOException e) {
            System.out.println("  Abra manualmente no navegador: " + url);
        }
    }
}
