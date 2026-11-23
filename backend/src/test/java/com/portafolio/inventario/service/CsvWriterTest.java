package com.portafolio.inventario.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void escapaComasComillasYSaltos() {
        assertThat(CsvWriter.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvWriter.escape("dice \"hola\"")).isEqualTo("\"dice \"\"hola\"\"\"");
        assertThat(CsvWriter.escape(null)).isEmpty();
    }

    @Test
    void neutralizaFormulasDeHojaDeCalculo() {
        assertThat(CsvWriter.escape("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        // Los números negativos no son texto del usuario y se conservan.
        assertThat(CsvWriter.escape(-5)).isEqualTo("-5");
    }
}
