package br.unb.poc.plataforma;

import br.unb.poc.analise.AnaliseComparativa;
import br.unb.poc.analise.AnaliseExport;
import br.unb.poc.dados.FonteDados;
import br.unb.poc.dados.TwitterDatasetLoader;
import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.demo.DatasetDemo;
import br.unb.poc.interfaces.DashboardServer;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.emocao.HeuristicaTwitter;
import br.unb.poc.modelo.topologia.TipoTopologia;
import br.unb.poc.relatorio.EstadosIniciais;
import br.unb.poc.relatorio.RelatorioExport;
import br.unb.poc.relatorio.SimuladorDireto;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Desktop;
import java.net.URI;

/**
 * Etapas 4.9 (preparar dados para a interface) e 5.0 (interface gráfica).
 *
 * <p>Sobe o {@link DashboardServer} alimentado por uma execução gerada em
 * memória, sem exigir JADE nem PostgreSQL: usa {@link DatasetDemo} para
 * gerar uma rede no formato Twitter Social Circles (via
 * {@code TwitterDatasetLoader}) e {@link SimuladorDireto} para propagar
 * as emoções, reaproveitando {@link HeuristicaTwitter} e
 * {@code FanEmotionModel} das etapas anteriores.
 *
 * <p>Para visualizar o resultado de uma execução real (JADE + PostgreSQL),
 * use a integração via {@code ColetorResultados}, que já acumula em
 * memória o mesmo formato de dados a partir de {@code UsuarioAgent}.
 *
 * <p>Uso (modo de demonstração, rede sintética):
 * <pre>
 *   java -cp target/classes:lib/jade.jar br.unb.poc.plataforma.LancadorInterface [porta] [numAgentes] [numCiclos] [alpha] [delta] [semente]
 * </pre>
 *
 * <p>Uso com o dataset real do Twitter Social Circles (mesmo `.tar.gz` ou
 * pasta extraída aceitos por {@code LancadorSimulacao --twitter}):
 * <pre>
 *   java ... br.unb.poc.plataforma.LancadorInterface --twitter caminho egoId [seedNodeId] [porta] [numCiclos] [alpha] [delta]
 * </pre>
 */
public final class LancadorInterface {

    private LancadorInterface() {}

    public static void main(String[] args) throws IOException {
        System.out.println("=".repeat(60));
        System.out.println("  Interface do PoC");
        System.out.println("  Etapas 4.9 (dados para a interface) e 5.0 (dashboard)");
        System.out.println("=".repeat(60));

        if (args.length > 0 && "--twitter".equalsIgnoreCase(args[0])) {
            iniciarComDatasetReal(args);
        } else {
            iniciarComDemo(args);
        }
    }

    private static void iniciarComDemo(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int numAgentes = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        int numCiclos = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        double alpha = args.length > 3 ? Double.parseDouble(args[3]) : 0.6;
        double delta = args.length > 4 ? Double.parseDouble(args[4]) : 0.02;
        long semente = args.length > 5 ? Long.parseLong(args[5]) : 42L;

        System.out.println("  Fonte: rede sintética de demonstração (DatasetDemo)");

        DashboardServer servidor = new DashboardServer(porta);
        servidor.definirReexecutor(parametros -> {
            try {
                return executarCenarioDemo(
                        parseInt(parametros, "numAgentes", numAgentes),
                        parseInt(parametros, "numCiclos", numCiclos),
                        parseDouble(parametros, "alpha", alpha),
                        parseDouble(parametros, "delta", delta),
                        parseLong(parametros, "semente", semente));
            } catch (IOException e) {
                throw new RuntimeException("Falha ao gerar o cenário de demonstração: " + e.getMessage(), e);
            }
        });

        String jsonInicial = executarCenarioDemo(numAgentes, numCiclos, alpha, delta, semente);
        servidor.atualizarResultado(jsonInicial);
        calcularComparacaoComSeguranca(servidor, DatasetDemo.gerar(semente, numAgentes), alpha, delta, numCiclos, semente,
                "Rede base (demonstração)");
        servidor.iniciar();
        abrirNavegador(porta);
    }

    private static void iniciarComDatasetReal(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Uso: --twitter <caminho> <egoId> [seedNodeId] [porta] [numCiclos] [alpha] [delta]");
        }
        String caminho = args[1];
        String egoId = args[2];
        String seedNodeId = args.length > 3 ? args[3] : "";
        int porta = args.length > 4 ? Integer.parseInt(args[4]) : 8080;
        int numCiclos = args.length > 5 ? Integer.parseInt(args[5]) : 20;
        double alpha = args.length > 6 ? Double.parseDouble(args[6]) : 0.6;
        double delta = args.length > 7 ? Double.parseDouble(args[7]) : 0.02;

        System.out.println("  Fonte: dataset real do Twitter Social Circles");
        System.out.printf("  Caminho: %s | ego=%s%n", caminho, egoId);

        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath(caminho)
                .twitterEgoId(egoId)
                .twitterSeedNodeId(seedNodeId)
                .alpha(alpha)
                .delta(delta)
                .numCiclos(numCiclos)
                .build();

        TwitterNetworkData dadosTwitter = TwitterDatasetLoader.load(
                java.nio.file.Paths.get(caminho), egoId, config.getPesoConexao());
        RedeSocial rede = new RedeSocial(config, dadosTwitter);

        int indiceSemente = 0;
        if (seedNodeId != null && !seedNodeId.isBlank()) {
            int encontrado = rede.getIdLocal(seedNodeId);
            if (encontrado >= 0) indiceSemente = encontrado;
            else System.err.println("[Interface] Nó-semente não encontrado: " + seedNodeId + ". Usando o primeiro nó.");
        }

        Map<Integer, EmotionState> estadosIniciais = EstadosIniciais.inferirComHeuristica(rede);
        List<Map<Integer, EmotionState>> historico = SimuladorDireto.executar(rede, config, estadosIniciais);
        String json = RelatorioExport.paraJson(rede, config, historico, indiceSemente);

        DashboardServer servidor = new DashboardServer(porta);
        servidor.atualizarResultado(json);
        // A rede real não é regenerada a cada reexecução — só a propagação
        // (ciclos/alpha/delta) é recalculada sobre a mesma topologia e os
        // mesmos estados iniciais já inferidos pela heurística.
        final int indiceSementeFinal = indiceSemente;
        servidor.definirReexecutor(parametros -> {
            SimulationConfiguration novoConfig = SimulationConfiguration.builder()
                    .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                    .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                    .twitterDatasetPath(caminho)
                    .twitterEgoId(egoId)
                    .twitterSeedNodeId(seedNodeId)
                    .alpha(parseDouble(parametros, "alpha", alpha))
                    .delta(parseDouble(parametros, "delta", delta))
                    .numCiclos(parseInt(parametros, "numCiclos", numCiclos))
                    .build();
            List<Map<Integer, EmotionState>> novoHistorico =
                    SimuladorDireto.executar(rede, novoConfig, estadosIniciais);
            return RelatorioExport.paraJson(rede, novoConfig, novoHistorico, indiceSementeFinal);
        });
        calcularComparacaoComSeguranca(servidor, dadosTwitter, alpha, delta, numCiclos, config.getSemente(),
                "Twitter (real)");
        servidor.iniciar();
        abrirNavegador(porta);
    }

    /**
     * Gera uma rede sintética no formato Twitter Social Circles, infere o
     * estado emocional inicial de cada nó (Etapa 4.5) e executa a
     * propagação (equivalente síncrono da Etapa 4.7) para alimentar a
     * interface (Etapa 4.9).
     */
    static String executarCenarioDemo(
            int numAgentes, int numCiclos, double alpha, double delta, long semente) throws IOException {

        SimulationConfiguration config = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(gerado em memória por DatasetDemo)")
                .twitterEgoId("demo")
                .alpha(alpha)
                .delta(delta)
                .numCiclos(numCiclos)
                .semente(semente)
                .build();

        TwitterNetworkData dadosTwitter = DatasetDemo.gerar(semente, numAgentes);
        RedeSocial rede = new RedeSocial(config, dadosTwitter);

        int indiceSemente = 0;
        Map<Integer, EmotionState> estadosIniciais = inferirEstadosIniciaisDemo(rede);

        List<Map<Integer, EmotionState>> historico =
                SimuladorDireto.executar(rede, config, estadosIniciais);

        return RelatorioExport.paraJson(rede, config, historico, indiceSemente);
    }

    /**
     * Estados iniciais para o modo de demonstração: usa a heurística real
     * (Etapa 4.5), mas força o nó 0 a um valor não-neutro quando nenhuma
     * feature foi reconhecida, para que a visualização de demonstração não
     * fique inteiramente monocromática. Esse ajuste é exclusivo do modo de
     * demonstração — não é aplicado ao dataset real nem à análise
     * comparativa, que usam {@link EstadosIniciais#inferirComHeuristica}
     * sem alterações.
     */
    private static Map<Integer, EmotionState> inferirEstadosIniciaisDemo(RedeSocial rede) {
        Map<Integer, EmotionState> estados = new HashMap<>(EstadosIniciais.inferirComHeuristica(rede));
        EmotionState doNo0 = estados.get(0);
        if (doNo0 != null && doNo0.getValor() == 0.0) {
            estados.put(0, EmotionState.semente(Emotion.ANGER, 0.8));
        }
        return estados;
    }

    /**
     * Calcula a análise comparativa (rede real/demonstração x topologias
     * sintéticas equivalentes — ver {@code AnaliseComparativa}) e alimenta
     * {@code /comparacao} no mesmo servidor. Roda de forma automática tanto
     * no modo de demonstração quanto com o dataset real, para que o link
     * "Comparar com redes sintéticas" do dashboard principal sempre tenha
     * dados. Qualquer falha aqui é apenas registrada — nunca derruba o
     * dashboard principal, que já está funcionando nesse ponto.
     */
    private static void calcularComparacaoComSeguranca(
            DashboardServer servidor, TwitterNetworkData dadosTwitter,
            double alpha, double delta, int numCiclos, long semente, String nomeCenarioBase) {
        try {
            System.out.println("  Calculando comparação com redes sintéticas equivalentes...");
            List<AnaliseComparativa.Cenario> cenarios =
                    AnaliseComparativa.executar(dadosTwitter, alpha, delta, numCiclos, semente, nomeCenarioBase);
            servidor.atualizarComparacao(AnaliseExport.paraJson(cenarios));
        } catch (Exception e) {
            System.err.println("  Aviso: não foi possível calcular a comparação com redes sintéticas: " + e.getMessage());
        }
    }

    /**
     * Tenta abrir o navegador padrão automaticamente apontando para o
     * dashboard. Se não conseguir (ambiente sem interface gráfica, sem
     * suporte a {@link Desktop}, execução via SSH sem X, etc.), apenas
     * informa a URL para o usuário abrir manualmente — nunca interrompe
     * a execução do servidor por causa disso.
     */
    private static void abrirNavegador(int porta) {
        String url = "http://localhost:" + porta + "/";
        System.out.println();
        System.out.println("  Dashboard disponível em: " + url);
        System.out.println();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("  (abrindo automaticamente no navegador padrão...)");
                return;
            }
        } catch (Exception e) {
            System.out.println("  Não foi possível abrir automaticamente via Desktop (" + e.getMessage() + ").");
        }

        // Fallback para ambientes onde java.awt.Desktop não está disponível
        // (comum em builds headless), tentando o comando do sistema operacional.
        String so = System.getProperty("os.name", "").toLowerCase();
        String[] comando;
        if (so.contains("win")) {
            comando = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (so.contains("mac")) {
            comando = new String[]{"open", url};
        } else {
            comando = new String[]{"xdg-open", url};
        }
        try {
            new ProcessBuilder(comando).start();
            System.out.println("  (abrindo automaticamente via '" + comando[0] + "'...)");
        } catch (IOException e) {
            System.out.println("  Abra manualmente no navegador: " + url);
        }
    }

    private static int parseInt(Map<String, String> params, String chave, int padrao) {
        String valor = params.get(chave);
        return (valor == null || valor.isBlank()) ? padrao : Integer.parseInt(valor);
    }

    private static double parseDouble(Map<String, String> params, String chave, double padrao) {
        String valor = params.get(chave);
        return (valor == null || valor.isBlank()) ? padrao : Double.parseDouble(valor);
    }

    private static long parseLong(Map<String, String> params, String chave, long padrao) {
        String valor = params.get(chave);
        return (valor == null || valor.isBlank()) ? padrao : Long.parseLong(valor);
    }
}
