package tn.formation.calc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculatorTest {

    private final Calculator calc = new Calculator();

    @Test
    void ttcSimple() {
        assertEquals(119.0, calc.prixTtc(100, 0.19));
    }

    @Test
    void ttcArrondiAuCentime() {
        assertEquals(11.89, calc.prixTtc(9.99, 0.19));
    }

    @Test
    void ttcRefuseUnPrixNegatif() {
        assertThrows(IllegalArgumentException.class, () -> calc.prixTtc(-1, 0.19));
    }

    @Test
    void divisionNormale() {
        assertEquals(2.5, calc.diviser(5, 2));
    }

    @Test
    void divisionParZero() {
        assertThrows(ArithmeticException.class, () -> calc.diviser(1, 0));
    }
}
