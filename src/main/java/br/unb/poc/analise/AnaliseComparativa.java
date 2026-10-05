package br.unb.poc.analise;

import br.unb.poc.dados.TwitterNetworkData;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.topologia.TipoTopologia;
import br.unb.poc.dados.FonteDados;
import br.unb.poc.relatorio.EstadosIniciais;
import br.unb.poc.relatorio.SimuladorDireto;

import java.util.List;
import java.util.Map;

/**
 * Etapa de análise comparativa: executa a propagação emocional sobre a
 * rede real do Twitter e sobre topologias sintéticas (Small-World,
 * Scale-Free, Modular) do <b>mesmo tamanho</b>, com o <b>mesmo número de
 * nós-semente por emoção</b> (ver {@link SementesComparaveis}), variando
 * apenas a topologia. Calcula métricas estruturais e de propagação para
 * cada cenário, permitindo uma comparação direta entre dados sintéticos e
 * dados reais — o objetivo desta etapa.
 */
public final class AnaliseComparativa {

    private AnaliseComparativa() {}

    /** Um cenário executado (rede real ou sintética) com suas métricas já calculadas. */
    public record Cenario(
            String nome,
            RedeSocial rede,
            List<Map<Integer, EmotionState>> historico,
            MetricasEstruturais estrutura,
            MetricasPropagacao propagacao) {}

    /**
     * Executa a rede base fornecida (real do Twitter, ou a rede sintética
     * de demonstração — {@code nomeCenarioBase} identifica qual das duas
     * é, para a interface não rotular incorretamente) e três topologias
     * sintéticas comparáveis (mesmo número de nós, mesma quantidade de
     * sementes por emoção), retornando um cenário para cada uma. O
     * primeiro item da lista é sempre a rede base.
     */
    public static List<Cenario> executar(
            TwitterNetworkData dadosTwitter,
            double alpha,
            double delta,
            int numCiclos,
            long semente,
            String nomeCenarioBase) {

        SimulationConfiguration configReal = SimulationConfiguration.builder()
                .fonteDados(FonteDados.TWITTER_SOCIAL_CIRCLES)
                .tipoTopologia(TipoTopologia.FONTE_EXTERNA)
                .twitterDatasetPath("(carregado externamente)")
                .twitterEgoId(dadosTwitter.getEgoId())
                .alpha(alpha)
                .delta(delta)
                .numCiclos(numCiclos)
                .semente(semente)
                .build();

        RedeSocial redeReal = new RedeSocial(configReal, dadosTwitter);
        Map<Integer, EmotionState> estadosReal = EstadosIniciais.inferirComHeuristica(redeReal);
        List<Map<Integer, EmotionState>> historicoReal = SimuladorDireto.executar(redeReal, configReal, estadosReal);

        var contagemSementes = SementesComparaveis.contar(estadosReal);

        List<Cenario> cenarios = new java.util.ArrayList<>();
        cenarios.add(construirCenario(nomeCenarioBase, redeReal, configReal, historicoReal));

        int numAgentes = redeReal.getNumAgentes();
        for (TipoTopologia tipo : List.of(TipoTopologia.SMALL_WORLD, TipoTopologia.SCALE_FREE, TipoTopologia.MODULAR)) {
            SimulationConfiguration configSint = SimulationConfiguration.builder()
                    .numAgentes(numAgentes)
                    .fonteDados(FonteDados.SINTETICA)
                    .tipoTopologia(tipo)
                    .alpha(alpha)
                    .delta(delta)
                    .numCiclos(numCiclos)
                    .semente(semente + tipo.ordinal() + 1)
                    .build();

            RedeSocial redeSint = new RedeSocial(configSint);
            Map<Integer, EmotionState> estadosSint =
                    SementesComparaveis.atribuir(redeSint, contagemSementes, semente + tipo.ordinal() + 100);
            List<Map<Integer, EmotionState>> historicoSint = SimuladorDireto.executar(redeSint, configSint, estadosSint);

            cenarios.add(construirCenario(nomeAmigavel(tipo), redeSint, configSint, historicoSint));
        }

        return cenarios;
    }

    private static Cenario construirCenario(
            String nome, RedeSocial rede, SimulationConfiguration config, List<Map<Integer, EmotionState>> historico) {
        MetricasEstruturais estrutura = MetricasEstruturais.calcular(rede);
        MetricasPropagacao propagacao = MetricasPropagacao.calcular(historico);
        return new Cenario(nome, rede, historico, estrutura, propagacao);
    }

    private static String nomeAmigavel(TipoTopologia tipo) {
        return switch (tipo) {
            case SMALL_WORLD -> "Small-World (sintética)";
            case SCALE_FREE -> "Scale-Free (sintética)";
            case MODULAR -> "Modular (sintética)";
            case FONTE_EXTERNA -> "Fonte externa";
        };
    }
}
