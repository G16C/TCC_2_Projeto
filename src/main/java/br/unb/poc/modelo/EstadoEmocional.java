package br.unb.poc.modelo;

/**
 * Representa o estado emocional de um agente na simulação.
 *
 * <p>O estado emocional é codificado como um valor numérico contínuo
 * no intervalo [-1.0, 1.0], onde:
 * <ul>
 *   <li>-1.0 corresponde ao estado totalmente <b>negativo</b>;</li>
 *   <li> 0.0 corresponde ao estado <b>neutro</b>;</li>
 *   <li>+1.0 corresponde ao estado totalmente <b>positivo</b>.</li>
 * </ul>
 *
 * <p>A categoria discreta (NEGATIVO, NEUTRO, POSITIVO) é derivada
 * do valor contínuo com base em limiares fixos, conforme o modelo
 * de Fan, Xu e Zhao (2018).
 *
 * @author Gabriel Campello Marques
 * @version 1.0.0
 */
public class EstadoEmocional {

    /** Limiar inferior: abaixo deste valor o estado é classificado como NEGATIVO. */
    public static final double LIMIAR_NEGATIVO = -0.2;

    /** Limiar superior: acima deste valor o estado é classificado como POSITIVO. */
    public static final double LIMIAR_POSITIVO = 0.2;

    /** Enumeração das categorias discretas de estado emocional. */
    public enum Categoria {
        NEGATIVO,
        NEUTRO,
        POSITIVO
    }

    private double valor;

    /**
     * Constrói um estado emocional com o valor informado.
     *
     * @param valor valor inicial no intervalo [-1.0, 1.0]
     * @throws IllegalArgumentException se o valor estiver fora do intervalo
     */
    public EstadoEmocional(double valor) {
        this.valor = clamp(valor);
    }

    /**
     * Retorna o valor numérico contínuo do estado emocional.
     *
     * @return valor no intervalo [-1.0, 1.0]
     */
    public double getValor() {
        return valor;
    }

    /**
     * Define o valor do estado emocional, aplicando clamping automático
     * para garantir que o resultado permaneça no intervalo [-1.0, 1.0].
     *
     * @param valor novo valor
     */
    public void setValor(double valor) {
        this.valor = clamp(valor);
    }

    /**
     * Retorna a categoria discreta correspondente ao valor atual.
     *
     * @return {@link Categoria} NEGATIVO, NEUTRO ou POSITIVO
     */
    public Categoria getCategoria() {
        if (valor < LIMIAR_NEGATIVO) return Categoria.NEGATIVO;
        if (valor > LIMIAR_POSITIVO) return Categoria.POSITIVO;
        return Categoria.NEUTRO;
    }

    /**
     * Restringe o valor ao intervalo [-1.0, 1.0].
     *
     * @param v valor a ser restringido
     * @return valor dentro do intervalo
     */
    private double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }

    @Override
    public String toString() {
        return String.format("%.4f (%s)", valor, getCategoria());
    }
}
