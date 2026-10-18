package br.unb.poc.modelo.topologia;

/**
 * Fábrica responsável por instanciar o {@link GeradorTopologia} adequado
 * a partir de um {@link TipoTopologia}, centralizando o ponto de decisão
 * sobre qual algoritmo de geração de rede será usado na simulação.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public final class GeradorTopologiaFactory {

    private GeradorTopologiaFactory() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Cria o gerador de topologia correspondente ao tipo informado.
     *
     * @param tipo tipo de topologia desejado
     * @return instância do {@link GeradorTopologia} correspondente
     */
    public static GeradorTopologia criar(TipoTopologia tipo) {
        switch (tipo) {
            case SMALL_WORLD:
                return new TopologiaSmallWorld();
            case SCALE_FREE:
                return new TopologiaScaleFree();
            case MODULAR:
                return new TopologiaModular();
            case FONTE_EXTERNA:
                return new TopologiaFonteExterna();
            default:
                throw new IllegalArgumentException("Tipo de topologia não suportado: " + tipo);
        }
    }
}
