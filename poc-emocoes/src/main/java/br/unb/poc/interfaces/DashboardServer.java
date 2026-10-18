package br.unb.poc.interfaces;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Etapa 5.0 — Interface gráfica.
 *
 * <p>Servidor HTTP embutido (apenas classes do próprio JDK, sem
 * dependência de um servlet container) que expõe:
 * <ul>
 *   <li>{@code GET /} — dashboard (HTML/CSS/JS autocontido, sem CDN);</li>
 *   <li>{@code GET /api/resultado.json} — último resultado disponível,
 *       no formato produzido por {@code RelatorioExport} (Etapa 4.9);</li>
 *   <li>{@code GET /api/executar?...} — opcional; se um reexecutor for
 *       fornecido, roda um novo cenário com os parâmetros da query string
 *       e retorna o novo JSON (usado pela página {@code /executar});</li>
 *   <li>{@code GET /executar} — página dedicada para configurar e rodar
 *       um novo cenário, separada do dashboard principal;</li>
 *   <li>{@code GET /comparacao} — dashboard de comparação entre a rede
 *       real do Twitter e topologias sintéticas equivalentes;</li>
 *   <li>{@code GET /api/comparacao.json} — dados da comparação, no
 *       formato produzido por {@code AnaliseExport}.</li>
 * </ul>
 *
 * <p>O servidor não sabe nada sobre JADE, PostgreSQL ou o dataset do
 * Twitter — ele apenas serve o contrato de dados já pronto, o que
 * permite reaproveitá-lo tanto para o modo de demonstração quanto para
 * uma execução real (bastando alimentar {@link #atualizarResultado}).
 */
public final class DashboardServer {

    private final int porta;
    private volatile String jsonAtual = "{}";
    private volatile String jsonComparacao = "{}";
    private volatile boolean paginaInicialComparacao = false;
    private Function<Map<String, String>, String> reexecutor;
    private HttpServer server;

    public DashboardServer(int porta) {
        this.porta = porta;
    }

    /** Define a função chamada por {@code GET /api/executar}; pode ser {@code null}. */
    public void definirReexecutor(Function<Map<String, String>, String> reexecutor) {
        this.reexecutor = reexecutor;
    }

    /** Atualiza o resultado servido por {@code /api/resultado.json}. */
    public void atualizarResultado(String json) {
        this.jsonAtual = (json == null) ? "{}" : json;
    }

    /** Atualiza os dados servidos por {@code /api/comparacao.json}. */
    public void atualizarComparacao(String json) {
        this.jsonComparacao = (json == null) ? "{}" : json;
    }

    /**
     * Quando {@code true}, {@code GET /} serve o dashboard de comparação
     * em vez do dashboard de rede única. Útil para um processo dedicado
     * apenas à comparação (ver {@code LancadorComparacao}).
     */
    public void usarPaginaComparacaoComoInicial(boolean valor) {
        this.paginaInicialComparacao = valor;
    }

    public void iniciar() throws IOException {
        try {
            server = HttpServer.create(new InetSocketAddress(porta), 0);
        } catch (java.net.BindException e) {
            throw new IOException(
                    "Não foi possível abrir a porta " + porta + " (já está em uso por outro processo?). "
                    + "Tente outra porta, ex.: informe " + (porta + 1) + " nos argumentos.", e);
        }
        server.createContext("/", this::servirPaginaPrincipal);
        server.createContext("/api/resultado.json", this::servirResultado);
        server.createContext("/api/executar", this::servirExecucao);
        server.createContext("/executar", this::servirPaginaExecutar);
        server.createContext("/comparacao", this::servirPaginaComparacao);
        server.createContext("/api/comparacao.json", this::servirComparacao);
        server.setExecutor(null);
        server.start();
        System.out.println("[Interface] Dashboard disponível em http://localhost:" + porta + "/");
    }

    public void parar() {
        if (server != null) server.stop(0);
    }

    private void servirPaginaPrincipal(HttpExchange exchange) throws IOException {
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        String pagina = paginaInicialComparacao ? ComparacaoHtml.PAGINA : DashboardHtml.PAGINA;
        responder(exchange, 200, "text/html; charset=utf-8", pagina);
    }

    private void servirPaginaComparacao(HttpExchange exchange) throws IOException {
        responder(exchange, 200, "text/html; charset=utf-8", ComparacaoHtml.PAGINA);
    }

    private void servirPaginaExecutar(HttpExchange exchange) throws IOException {
        responder(exchange, 200, "text/html; charset=utf-8", ExecutarHtml.PAGINA);
    }

    private void servirComparacao(HttpExchange exchange) throws IOException {
        cabecalhosCors(exchange);
        responder(exchange, 200, "application/json; charset=utf-8", jsonComparacao);
    }

    private void servirResultado(HttpExchange exchange) throws IOException {
        cabecalhosCors(exchange);
        responder(exchange, 200, "application/json; charset=utf-8", jsonAtual);
    }

    private void servirExecucao(HttpExchange exchange) throws IOException {
        cabecalhosCors(exchange);
        if (reexecutor == null) {
            responder(exchange, 501, "application/json; charset=utf-8",
                    "{\"erro\":\"Reexecução não disponível neste modo.\"}");
            return;
        }
        try {
            Map<String, String> parametros = parametrosDaQuery(exchange.getRequestURI().getRawQuery());
            String novoJson = reexecutor.apply(parametros);
            atualizarResultado(novoJson);
            responder(exchange, 200, "application/json; charset=utf-8", novoJson);
        } catch (RuntimeException e) {
            responder(exchange, 400, "application/json; charset=utf-8",
                    "{\"erro\":" + jsonStr(e.getMessage()) + "}");
        }
    }

    private static void cabecalhosCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }

    private static void responder(HttpExchange exchange, int status, String tipo, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", tipo);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> parametrosDaQuery(String query) {
        Map<String, String> parametros = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return parametros;
        for (String par : query.split("&")) {
            int pos = par.indexOf('=');
            String chave = pos < 0 ? par : par.substring(0, pos);
            String valor = pos < 0 ? "" : par.substring(pos + 1);
            parametros.put(
                    URLDecoder.decode(chave, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return parametros;
    }

    private static String jsonStr(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
