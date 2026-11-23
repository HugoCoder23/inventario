package com.portafolio.inventario.service;

/** Generador CSV mínimo (RFC 4180) con protección contra inyección de fórmulas en hojas de cálculo. */
final class CsvWriter {

    private final StringBuilder sb = new StringBuilder();

    CsvWriter(String... headers) {
        row((Object[]) headers);
    }

    CsvWriter row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values[i]));
        }
        sb.append("\r\n");
        return this;
    }

    static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // Evita que Excel interprete texto del usuario como fórmula (=, +, -, @).
        if (value instanceof CharSequence && !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
