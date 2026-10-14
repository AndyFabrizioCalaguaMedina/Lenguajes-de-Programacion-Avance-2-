package com.hcg.multiservicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hcg.multiservicios.validation.DniValidator;
import com.hcg.multiservicios.validation.RucValidator;
import org.junit.jupiter.api.Test;

class DocumentoValidatorsTest {

    @Test
    void rucValidosConDigitoVerificadorCorrecto() {
        assertTrue(RucValidator.esValido("20100070970"));
        assertTrue(RucValidator.esValido("20131312955"));
        assertTrue(RucValidator.esValido("20123456786"));
    }

    @Test
    void rucConDigitoVerificadorIncorrectoSeRechaza() {
        assertFalse(RucValidator.esValido("20100070971"));
        assertFalse(RucValidator.esValido("20512345678")); // el ejemplo del diseño no es un RUC real
    }

    @Test
    void rucConFormatoIncorrectoSeRechaza() {
        assertFalse(RucValidator.esValido(null));
        assertFalse(RucValidator.esValido(""));
        assertFalse(RucValidator.esValido("2010007097"));   // 10 digitos
        assertFalse(RucValidator.esValido("201000709700")); // 12 digitos
        assertFalse(RucValidator.esValido("2010007097A"));  // letra
        assertFalse(RucValidator.esValido("30100070970"));  // prefijo no valido
    }

    @Test
    void dni() {
        assertTrue(DniValidator.esValido("12345678"));
        assertFalse(DniValidator.esValido("1234567"));
        assertFalse(DniValidator.esValido("123456789"));
        assertFalse(DniValidator.esValido("1234567a"));
        assertFalse(DniValidator.esValido(null));
    }
}
