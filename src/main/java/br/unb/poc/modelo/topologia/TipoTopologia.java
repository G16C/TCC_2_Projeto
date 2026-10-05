package br.unb.poc.modelo.topologia;

/**
 * Tipos de topologia de rede social suportados pela simulação.
 *
 * @author Gabriel Campello Marques
 * @version 1.1.0
 */
public enum TipoTopologia {

    /** Modelo Small-World de Watts e Strogatz (1998). */
    SMALL_WORLD,

    /** Modelo Scale-Free (livre de escala) de Barabási e Albert (1999). */
    SCALE_FREE,

    /** Modelo de rede modular, com comunidades densamente conectadas. */
    MODULAR,

    /**
     * Topologia carregada a partir de uma fonte de dados externa
     * (arquivo, API ou banco de dados). Formato ainda em definição —
     * ver {@link TopologiaFonteExterna}.
     */
    FONTE_EXTERNA
}
