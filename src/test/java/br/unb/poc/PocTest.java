package br.unb.poc;

import br.unb.poc.modelo.ConfiguracaoPoc;
import br.unb.poc.modelo.EstadoEmocional;
import br.unb.poc.modelo.RedeSocial;
import br.unb.poc.modelo.SimulationConfiguration;
import br.unb.poc.modelo.emocao.FanEmotionModel;
import br.unb.poc.modelo.emocao.Emotion;
import br.unb.poc.modelo.emocao.EmotionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários dos componentes centrais da Prova de Conceito.
 *
 * <p>Cobre os componentes {@link EstadoEmocional} e {@link RedeSocial},
 * verificando a corretude das regras de negócio independentemente
 * da plataforma JADE e do banco de dados.
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */
class PocTest {

    @Test
    @DisplayName("Estado emocional negativo deve ser categorizado como NEGATIVO")
    void testCategoriaNegativa() {
        EstadoEmocional estado = new EstadoEmocional(-0.8);
        assertEquals(EstadoEmocional.Categoria.NEGATIVO, estado.getCategoria());
    }

    @Test
    @DisplayName("Estado emocional zero deve ser categorizado como NEUTRO")
    void testCategoriaNeutra() {
        EstadoEmocional estado = new EstadoEmocional(0.0);
        assertEquals(EstadoEmocional.Categoria.NEUTRO, estado.getCategoria());
    }

    @Test
    @DisplayName("Estado emocional positivo deve ser categorizado como POSITIVO")
    void testCategoriaPositiva() {
        EstadoEmocional estado = new EstadoEmocional(0.9);
        assertEquals(EstadoEmocional.Categoria.POSITIVO, estado.getCategoria());
    }

    @Test
    @DisplayName("Clamping deve restringir valores acima de 1.0 para 1.0")
    void testClampingSuperior() {
        EstadoEmocional estado = new EstadoEmocional(2.5);
        assertEquals(1.0, estado.getValor(), 1e-9);
    }

    @Test
    @DisplayName("Clamping deve restringir valores abaixo de -1.0 para -1.0")
    void testClampingInferior() {
        EstadoEmocional estado = new EstadoEmocional(-3.0);
        assertEquals(-1.0, estado.getValor(), 1e-9);
    }

    @Test
    @DisplayName("Equação de atualização emocional deve produzir resultado correto")
    void testAtualizacaoEmocional() {
        // E_i(t) = -1.0, I_i(t) = 0.0, α = 0.6
        // E_i(t+1) = 0.6 * (-1.0) + 0.4 * 0.0 = -0.6
        // Após decaimento δ=0.05: -0.6 * (1 - 0.05) = -0.57
        double estadoAtual    = -1.0;
        double influencia     = 0.0;
        double alpha          = ConfiguracaoPoc.ALPHA;
        double delta          = ConfiguracaoPoc.DELTA;

        double novoValorSemDecaimento = alpha * estadoAtual + (1.0 - alpha) * influencia;
        double novoValorComDecaimento = novoValorSemDecaimento * (1.0 - delta);

        EstadoEmocional estado = new EstadoEmocional(novoValorComDecaimento);
        assertEquals(-0.57, estado.getValor(), 1e-9);
        assertEquals(EstadoEmocional.Categoria.NEGATIVO, estado.getCategoria());
    }

    @Test
    @DisplayName("Configuração padrão deve ser imutável e reproduzir os parâmetros da PoC")
    void testConfiguracaoPadrao() {
        SimulationConfiguration config = SimulationConfiguration.padrao();
        assertEquals(ConfiguracaoPoc.NUM_AGENTES, config.getNumAgentes());
        assertEquals(ConfiguracaoPoc.ALPHA, config.getAlpha(), 1e-9);
        assertEquals(ConfiguracaoPoc.DELTA, config.getDelta(), 1e-9);
        assertEquals(ConfiguracaoPoc.SEMENTE_SIMULACAO, config.getSemente());
    }

    @Test
    @DisplayName("Modelo multidimensional deve atualizar cada emoção separadamente")
    void testFanEmotionModelMultidimensional() {
        FanEmotionModel modelo = new FanEmotionModel(0.6, 0.05);

        EmotionState atual = EmotionState.neutro();
        EmotionState vizinho = EmotionState.semente(Emotion.ANGER, 1.0);

        EmotionState novo = modelo.calcularProximoEstado(
                atual,
                Map.of(0, vizinho),
                Map.of(0, 1.0));

        assertEquals(0.38, novo.getIntensity(Emotion.ANGER), 1e-9);
        assertEquals(0.0, novo.getIntensity(Emotion.DISGUST), 1e-9);
        assertEquals(0.0, novo.getIntensity(Emotion.JOY), 1e-9);
        assertEquals(0.0, novo.getIntensity(Emotion.SADNESS), 1e-9);
        assertEquals(Emotion.ANGER, novo.getDominantEmotion());
    }

    @Test
    @DisplayName("Estado emocional deve ser serializável para mensagens ACL")
    void testSerializacaoEstadoEmocional() {
        EmotionState original = new EmotionState(Map.of(
                Emotion.ANGER, 0.7,
                Emotion.DISGUST, 0.1,
                Emotion.JOY, 0.05,
                Emotion.SADNESS, 0.15));

        EmotionState reconstruido = EmotionState.deserialize(original.serialize());

        for (Emotion emotion : Emotion.values()) {
            assertEquals(original.getIntensity(emotion),
                    reconstruido.getIntensity(emotion), 1e-9);
        }
    }

    @Test
    @DisplayName("Configuração deve permitir selecionar a emoção da semente")
    void testEmocaoSemente() {
        SimulationConfiguration config = SimulationConfiguration.builder()
                .emocaoSemente(Emotion.SADNESS)
                .build();

        assertEquals(Emotion.SADNESS, config.getEmocaoSemente());
    }

    @Test
    @DisplayName("Semente deve iniciar com estado emocional negativo")
    void testEstadoInicialSemente() {
        EstadoEmocional semente = new EstadoEmocional(ConfiguracaoPoc.ESTADO_INICIAL_SEMENTE);
        assertEquals(-1.0, semente.getValor(), 1e-9);
        assertEquals(EstadoEmocional.Categoria.NEGATIVO, semente.getCategoria());
    }

    @Test
    @DisplayName("Demais agentes devem iniciar com estado emocional neutro")
    void testEstadoInicialPadrao() {
        EstadoEmocional padrao = new EstadoEmocional(ConfiguracaoPoc.ESTADO_INICIAL_PADRAO);
        assertEquals(0.0, padrao.getValor(), 1e-9);
        assertEquals(EstadoEmocional.Categoria.NEUTRO, padrao.getCategoria());
    }

    @Test
    @DisplayName("Rede deve conter o número correto de agentes")
    void testNumeroDeAgentes() {
        RedeSocial rede = new RedeSocial(42L);
        assertEquals(ConfiguracaoPoc.NUM_AGENTES, rede.getNumAgentes());
    }

    @Test
    @DisplayName("Cada agente deve ter ao menos um vizinho")
    void testCadaAgenteTemVizinhos() {
        RedeSocial rede = new RedeSocial(42L);
        for (int i = 0; i < ConfiguracaoPoc.NUM_AGENTES; i++) {
            Set<Integer> vizinhos = rede.getVizinhos(i);
            assertFalse(vizinhos.isEmpty(),
                    "Agente " + i + " não deve ter conjunto de vizinhos vazio");
        }
    }

    @Test
    @DisplayName("Nenhum agente deve ser vizinho de si mesmo")
    void testSemAutoConexao() {
        RedeSocial rede = new RedeSocial(42L);
        for (int i = 0; i < ConfiguracaoPoc.NUM_AGENTES; i++) {
            assertFalse(rede.getVizinhos(i).contains(i),
                    "Agente " + i + " não deve ser vizinho de si mesmo");
        }
    }

    @Test
    @DisplayName("A relação de vizinhança deve ser simétrica")
    void testSimetriaDeVizinhanca() {
        RedeSocial rede = new RedeSocial(42L);
        for (int i = 0; i < ConfiguracaoPoc.NUM_AGENTES; i++) {
            for (int j : rede.getVizinhos(i)) {
                assertTrue(rede.getVizinhos(j).contains(i),
                        String.format("Vizinhança assimétrica: %d → %d, mas não %d → %d", i, j, j, i));
            }
        }
    }

    @Test
    @DisplayName("Rede com mesma semente deve ser reprodutível")
    void testReprodutibilidade() {
        RedeSocial rede1 = new RedeSocial(42L);
        RedeSocial rede2 = new RedeSocial(42L);
        for (int i = 0; i < ConfiguracaoPoc.NUM_AGENTES; i++) {
            assertEquals(rede1.getVizinhos(i), rede2.getVizinhos(i),
                    "Topologias com mesma semente devem ser idênticas");
        }
    }
}
