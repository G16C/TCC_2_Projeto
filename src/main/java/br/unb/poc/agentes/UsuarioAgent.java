package br.unb.poc.agentes;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.EmotionPropagationModel;
import br.unb.poc.modelo.emocao.FanEmotionModel;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import br.unb.poc.relatorio.ColetorResultados;
import br.unb.poc.util.DatabaseUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Agente que representa um usuário comum de uma rede social digital.
 *
 * <p>Implementa a arquitetura reativa (comportamental): a cada ciclo
 * de simulação o agente:
 * <ol>
 *   <li>Envia seu estado emocional atual a todos os vizinhos via mensagem ACL;</li>
 *   <li>Recebe os estados emocionais dos vizinhos;</li>
 *   <li>Calcula a influência emocional agregada I_i(t);</li>
 *   <li>Atualiza seu estado emocional E_i(t+1) conforme a equação do modelo.</li>
 * </ol>
 *
 * <p>A regra de atualização é:
 * <pre>
 *   E_i(t+1) = α · E_i(t) + (1 − α) · I_i(t)
 * </pre>
 * seguida da aplicação do decaimento emocional δ em direção ao neutro.
 *
 * <p>Ao final de cada ciclo, o estado é persistido no PostgreSQL
 * por meio de {@link DatabaseUtil}.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 * @see br.unb.poc.modelo.ConfiguracaoPoc
 */
public class UsuarioAgent extends Agent {


    /** Índice numérico deste agente na rede (0 a NUM_AGENTES-1). */
    private int idAgente;

    /** Estado emocional atual do agente. */
    private EmotionState estadoEmocional;

    /** Taxa de persistência emocional (α). */
    private double alpha;

    /** Taxa de decaimento emocional (δ). */
    private double delta;

    /** Vizinhos deste agente na rede, mapeados ao peso w_ij da conexão. */
    private Map<Integer, Double> pesosVizinhos;

    /** Ciclo atual da simulação. */
    private int cicloAtual;

    /** ID da simulação corrente no banco de dados. */
    private int simulacaoId;

    /** Configuração do cenário corrente. */
    private SimulationConfiguration configuracao;

    /** Modelo de propagação emocional desacoplado do agente. */
    private EmotionPropagationModel modeloEmocional;
    private String idExterno;


    /**
     * Inicializa o agente com os parâmetros recebidos via {@code getArguments()}.
     *
     * <p>Argumentos esperados (na ordem):
     * <ol>
     *   <li>idAgente (Integer)</li>
     *   <li>estadoInicial (Double)</li>
     *   <li>redeSocial (RedeSocial)</li>
     *   <li>simulacaoId (Integer)</li>
     * </ol>
     */
    @Override
    protected void setup() {
        Object[] args = getArguments();

        this.idAgente    = (Integer) args[0];
        double estadoIni = (Double)  args[1];
        RedeSocial rede  = (RedeSocial) args[2];
        this.simulacaoId = (Integer) args[3];
        this.configuracao = args.length > 4
                ? (SimulationConfiguration) args[4]
                : SimulationConfiguration.padrao();

        EmotionState estadoInferido = args.length > 5 && args[5] instanceof EmotionState
                ? (EmotionState) args[5]
                : null;

        this.estadoEmocional = estadoInferido != null
                ? estadoInferido
                : criarEstadoInicial(estadoIni);
        this.alpha    = configuracao.getAlpha();
        this.delta    = configuracao.getDelta();
        this.modeloEmocional = new FanEmotionModel(alpha, delta);
        this.pesosVizinhos = rede.getVizinhosComPesos(idAgente);
        this.idExterno = rede.getIdExterno(idAgente);
        this.cicloAtual = 0;

        if (configuracao.isLogDetalhado()) {
            System.out.printf("[%s] Iniciado | Estado: %s | Vizinhos: %s%n",
                    getLocalName(), estadoEmocional, pesosVizinhos.keySet());
        }

        persistirEstado(0);

        addBehaviour(new EsperaInicio());
    }

    @Override
    protected void takeDown() {
        if (configuracao == null || configuracao.isLogDetalhado()) {
            System.out.printf("[%s] Encerrado após %d ciclos.%n",
                    getLocalName(), cicloAtual);
        }
    }


    /**
     * Comportamento cíclico que executa um ciclo completo de propagação
     * emocional a cada iteração:
     * <ol>
     *   <li>Envia o estado emocional atual a todos os vizinhos;</li>
     *   <li>Coleta os estados recebidos dos vizinhos;</li>
     *   <li>Aplica a equação de atualização emocional;</li>
     *   <li>Persiste o novo estado no banco de dados.</li>
     * </ol>
     */
    
    private class EsperaInicio extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage msg = receive();

            if (msg == null) {
                block();
                return;
            }

            if ("START".equals(msg.getContent())) {

                if (configuracao.isLogDetalhado()) {
                    System.out.println(getLocalName() + " iniciou.");
                }

                addBehaviour(new ComportamentoPropagacao());

                removeBehaviour(this);
            }
        }
    }

    private class ComportamentoPropagacao extends CyclicBehaviour {

        @Override
        public void action() {

            if (cicloAtual >= configuracao.getNumCiclos()) {
                block(configuracao.getIntervaloCicloMs());
                return;
            }

            for (int idVizinho : pesosVizinhos.keySet()) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(
                        configuracao.getPrefixoUsuario() + idVizinho,
                        AID.ISLOCALNAME));
                msg.setContent(estadoEmocional.serialize());
                msg.setConversationId("ciclo-" + cicloAtual);
                if (configuracao.isLogDetalhado()) {
                    System.out.printf(
                        "[%s] >>> INFORM para %s | ciclo=%d | estado=%s%n",
                        getLocalName(),
                        configuracao.getPrefixoUsuario() + idVizinho,
                        cicloAtual,
                        estadoEmocional
                    );
                }

                send(msg);
            }

            Map<Integer, EmotionState> influenciasRecebidas = new HashMap<>();
            MessageTemplate filtro = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("ciclo-" + cicloAtual)
            );

            long prazo = System.currentTimeMillis() + configuracao.getIntervaloCicloMs();
            while (influenciasRecebidas.size() < pesosVizinhos.size()
                    && System.currentTimeMillis() < prazo) {

                ACLMessage recebida = receive(filtro);
                if (recebida != null) {
                    if (configuracao.isLogDetalhado()) {
                        System.out.printf(
                            "[%s] <<< INFORM de %s | ciclo=%d | conteúdo=%s%n",
                            getLocalName(),
                            recebida.getSender().getLocalName(),
                            cicloAtual,
                            recebida.getContent()
                        );
                    }
                    try {
                        int idRemetente = extrairIdDoNome(recebida.getSender().getLocalName());
                        EmotionState estadoVizinho = EmotionState.deserialize(recebida.getContent());
                        influenciasRecebidas.put(idRemetente, estadoVizinho);
                    } catch (NumberFormatException e) {
                        System.err.printf("[%s] Mensagem inválida ignorada: %s%n",
                                getLocalName(), recebida.getContent());
                    }
                } else {
                    block(50);
                }
            }

            if (!influenciasRecebidas.isEmpty()) {
                EmotionState novoEstado = modeloEmocional.calcularProximoEstado(
                        estadoEmocional,
                        influenciasRecebidas,
                        pesosVizinhos);
                estadoEmocional = novoEstado;
            }

            cicloAtual++;

            if (configuracao.isLogDetalhado()) {
                System.out.printf("[%s] Ciclo %2d | Estado: %s%n",
                        getLocalName(), cicloAtual, estadoEmocional);
            }

            persistirEstado(cicloAtual);
        }
    }

    /**
     * Extrai o índice numérico do agente a partir do nome local JADE
     * (ex.: {@code "Usuario7"} → {@code 7}), assumindo o prefixo definido
     * em {@link ConfiguracaoPoc#PREFIXO_USUARIO}.
     *
     * @param nomeLocal nome local do agente remetente
     * @return índice numérico do agente
     */
    private int extrairIdDoNome(String nomeLocal) {
        return Integer.parseInt(nomeLocal.substring(configuracao.getPrefixoUsuario().length()));
    }

    /**
     * Persiste o estado emocional atual no banco de dados PostgreSQL.
     *
     * @param ciclo número do ciclo a ser registrado
     */
    private void persistirEstado(int ciclo) {
        try {
            DatabaseUtil.salvarEstado(simulacaoId, ciclo, idAgente, redeIdExterno(), estadoEmocional);
        } catch (SQLException e) {
            System.err.printf("[%s] Erro ao persistir estado no ciclo %d: %s%n",
                    getLocalName(), ciclo, e.getMessage());
        }
        // Também alimenta o coletor em memória usado pela interface (Etapa 4.9),
        // independente do resultado da persistência em banco.
        ColetorResultados.registrarEstado(simulacaoId, ciclo, idAgente, estadoEmocional);
    }

    /**
     * Compatibiliza a semente escalar da PoC com o novo estado
     * multidimensional. O valor negativo usado pela PoC representa
     * a introdução de raiva; valores positivos representam alegria.
     */
    private EmotionState criarEstadoInicial(double valor) {
        if (valor < 0) {
            return EmotionState.semente(configuracao.getEmocaoSemente(), Math.min(1.0, Math.abs(valor)));
        }
        if (valor > 0) {
            return EmotionState.semente(Emotion.JOY, Math.min(1.0, valor));
        }
        return EmotionState.neutro();
    }

    private String redeIdExterno() { return idExterno; }

    public EmotionState getEstadoEmocional() {
        return estadoEmocional;
    }

    public int getIdAgente() {
        return idAgente;
    }
}
