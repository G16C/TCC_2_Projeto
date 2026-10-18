package br.unb.poc.agentes;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.topologia.GeradorTopologia;
import br.unb.poc.modelo.topologia.GeradorTopologiaFactory;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.modelo.emocao.HeuristicaTwitter;
import br.unb.poc.relatorio.ColetorResultados;
import br.unb.poc.util.DatabaseUtil;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
// import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;


import java.sql.SQLException;

/**
 * Agente de controle da simulação.
 *
 * <p>Responsável por:
 * <ol>
 *   <li>Inicializar o esquema do banco de dados;</li>
 *   <li>Registrar a execução da simulação no PostgreSQL;</li>
 *   <li>Gerar a topologia da rede social ({@link RedeSocial});</li>
 *   <li>Instanciar dinamicamente todos os {@link UsuarioAgent}s na plataforma JADE;</li>
 *   <li>Monitorar o progresso dos ciclos;</li>
 *   <li>Encerrar todos os agentes ao término da simulação.</li>
 * </ol>
 *
 * <p>O agente de controle não participa da propagação emocional;
 * sua única responsabilidade é a orquestração do ambiente.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */

public class AgenteControle extends Agent {

    private int simulacaoId;
    private RedeSocial rede;
    private SimulationConfiguration configuracao;

    /**
     * Última rede/simulação criadas por este agente, expostas estaticamente
     * para que a interface (Etapas 4.9/5.0) possa montar o relatório em
     * memória a partir de {@link ColetorResultados} sem precisar reconsultar
     * o PostgreSQL (que não guarda a topologia da rede). Uso aceitável neste
     * PoC de processo único; não deve ser usado em execuções concorrentes.
     */
    public static volatile RedeSocial ultimaRedeCriada;
    public static volatile Integer ultimaSimulacaoId;

    @Override
    protected void setup() {
        System.out.println("[Controle] Iniciando simulação da PoC...");

        try {
            DatabaseUtil.inicializarEsquema();
            Object[] args = getArguments();
            this.configuracao = (args != null && args.length > 0
                    && args[0] instanceof SimulationConfiguration)
                    ? (SimulationConfiguration) args[0]
                    : SimulationConfiguration.padrao();

            GeradorTopologia gerador = GeradorTopologiaFactory.criar(configuracao.getTipoTopologia());
            this.rede = new RedeSocial(configuracao.getSemente(), gerador, configuracao);

            this.simulacaoId = DatabaseUtil.registrarSimulacao(configuracao, rede.getNumAgentes());
            System.out.println("[Controle] Simulação registrada no banco. ID = " + simulacaoId);

            ultimaRedeCriada = rede;
            ultimaSimulacaoId = simulacaoId;
            ColetorResultados.definirRede(simulacaoId, rede);
        } catch (SQLException | RuntimeException e) {
            System.err.println("[Controle] Falha no banco de dados: " + e.getMessage());
            doDelete();
            return;
        }

        System.out.println("[Controle] Fonte de dados: " + configuracao.getFonteDados());
        System.out.println("[Controle] Topologia: " + configuracao.getTipoTopologia());
        System.out.println("[Controle] Agentes carregados: " + rede.getNumAgentes());
        if (rede.usaDadosTwitter()) {
            System.out.println("[Controle] Twitter ego: " + rede.getDadosTwitter().getEgoId());
        }
        System.out.println(rede);

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                criarAgentesUsuario();
                doWait(5000);
                iniciarSimulacao();
            }
        });

        long duracaoTotal = (long) configuracao.getNumCiclos()
                          * configuracao.getIntervaloCicloMs()
                          + configuracao.getIntervaloCicloMs() * 2;

        addBehaviour(new TickerBehaviour(this, duracaoTotal) {
            @Override
            protected void onTick() {
                System.out.println("[Controle] Simulação concluída. Encerrando agentes...");
                // encerrarAgentesUsuario();
                DatabaseUtil.fecharConexao();
                // doDelete();
                stop(); // TickerBehaviour é cíclico por padrão; sem isso, repetiria a cada duracaoTotal ms.
            }
        });
    }


    private void iniciarSimulacao() {

        System.out.println("[Controle] Iniciando propagação emocional...");

        for (int i = 0; i < rede.getNumAgentes(); i++) {

            ACLMessage start = new ACLMessage(ACLMessage.INFORM);

            start.addReceiver(
                    new AID(
                            configuracao.getPrefixoUsuario() + i,
                            AID.ISLOCALNAME));

            start.setContent("START");

            send(start);
        }
    }

    /**
     * Instancia dinamicamente um {@link UsuarioAgent} para cada nó da rede.
     *
     * <p>O Agente 0 recebe o estado emocional inicial negativo (semente);
     * os demais são inicializados com o estado neutro padrão.
     */

    private void criarAgentesUsuario() {
        ContainerController cc = getContainerController();

        int indiceSemente = determinarIndiceSemente();

        for (int i = 0; i < rede.getNumAgentes(); i++) {
            double estadoInicial = (i == indiceSemente)
                    ? configuracao.getEstadoInicialSemente()
                    : configuracao.getEstadoInicialPadrao();

            EmotionState estadoInferido = rede.usaDadosTwitter()
                    ? new HeuristicaTwitter().inferir(
                            rede.getFeaturesTwitter(i),
                            rede.getNomesFeaturesTwitter())
                    : null;

            Object[] args = new Object[]{
                    i, estadoInicial, rede, simulacaoId, configuracao, estadoInferido
            };
            String nomeAgente = configuracao.getPrefixoUsuario() + i;

            try {
                AgentController ac = cc.createNewAgent(
                        nomeAgente,
                        UsuarioAgent.class.getName(),
                        args
                );
                ac.start();
                if (configuracao.isLogDetalhado()) {
                    System.out.printf("[Controle] Agente %s criado | estado escalar=%.2f | inferido=%s%n",
                            nomeAgente, estadoInicial,
                            estadoInferido == null ? "n/a" : estadoInferido);
                }
            } catch (StaleProxyException e) {
                System.err.printf("[Controle] Erro ao criar agente %s: %s%n",
                        nomeAgente, e.getMessage());
            }
        }
        if (!configuracao.isLogDetalhado()) {
            System.out.println("[Controle] " + rede.getNumAgentes()
                    + " agentes criados (use configuracao.logDetalhado(true) ou --verbose para ver ciclo a ciclo).");
        }
    }

    /**
     * Solicita o encerramento de todos os agentes usuário da simulação.
     */
    /**private void encerrarAgentesUsuario() {
        ContainerController cc = getContainerController();
        for (int i = 0; i < rede.getNumAgentes(); i++) {
            String nomeAgente = configuracao.getPrefixoUsuario() + i;
            try {
                AgentController ac = cc.getAgent(nomeAgente);
                ac.kill();
            } catch (ControllerException e) {
                System.err.printf("[Controle] Erro ao encerrar agente %s: %s%n",
                        nomeAgente, e.getMessage());
            }
        }
    }
    **/
   
    private int determinarIndiceSemente() {
        if (rede.usaDadosTwitter()
                && configuracao.getTwitterSeedNodeId() != null
                && !configuracao.getTwitterSeedNodeId().isBlank()) {
            int indice = rede.getIdLocal(configuracao.getTwitterSeedNodeId());
            if (indice >= 0) return indice;
            System.err.println("[Controle] Nó-semente Twitter não encontrado: "
                    + configuracao.getTwitterSeedNodeId() + ". Usando o primeiro nó.");
        }
        return 0;
    }

    @Override
    protected void takeDown() {
        System.out.println("[Controle] Agente de controle encerrado. Simulação finalizada.");
    }
}
