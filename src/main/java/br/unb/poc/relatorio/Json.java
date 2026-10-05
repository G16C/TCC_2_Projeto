package br.unb.poc.relatorio;

import java.util.Locale;

/**
 * Utilitário mínimo para escrita de JSON, evitando adicionar uma
 * dependência externa (Jackson/Gson) apenas para a camada de interface.
 *
 * <p>Não é um serializador genérico: cobre apenas os tipos usados pelos
 * relatórios da Etapa 4.9 (números, texto, booleanos e composição manual
 * de objetos/arrays via {@link StringBuilder}).</p>
 */
final class Json {

    private Json() {}

    static String str(String valor) {
        if (valor == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static String num(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) return "0";
        return String.format(Locale.US, "%.6f", valor);
    }

    static String num(long valor) {
        return Long.toString(valor);
    }
}
