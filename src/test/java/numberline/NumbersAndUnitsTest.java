package numberline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumbersAndUnitsTest {
    @Test
    void parsesGroupedDecimalsAndRejectsNonfiniteValues() {
        assertEquals(new BigDecimal("1200"), Numbers.parse("1,200"));
        assertEquals(new BigDecimal("1200000"), Numbers.parse("1,200,000"));
        assertEquals(new BigDecimal("1.2"), Numbers.parse("1.2"));
        assertEquals(new BigDecimal("0.08"), Numbers.parse("0.08"));
        assertEquals("1200", Numbers.plain(new BigDecimal("1.2").multiply(new BigDecimal("1000"))));
        InvalidInputException nan = assertThrows(InvalidInputException.class, () -> Numbers.parse("NaN"));
        assertEquals("number", nan.category());
        assertThrows(InvalidInputException.class, () -> Numbers.parse("Infinity"));
        assertThrows(InvalidInputException.class, () -> Numbers.parse("1.2e3"));
        assertThrows(InvalidInputException.class, () -> Numbers.parse("1,20"));
    }

    @Test
    void allowlistFactorsMatchHandCalculation() {
        assertEquals("1200", Numbers.plain(Units.require("kg").toCanonical(new BigDecimal("1.2"))));
        assertEquals("1200", Numbers.plain(Units.require("g").toCanonical(new BigDecimal("1200"))));
        assertEquals("1200", Numbers.plain(Units.require("mg").toCanonical(new BigDecimal("1200000"))));
        assertEquals("1.2", Numbers.plain(Units.require("g").toCanonical(new BigDecimal("1.2"))));
        assertEquals("2000", Numbers.plain(Units.require("kg").toCanonical(new BigDecimal("2"))));
        assertEquals("0.08", Numbers.plain(Units.require("percent").toCanonical(new BigDecimal("8"))));
        assertEquals("0.08", Numbers.plain(Units.require("per_100").toCanonical(new BigDecimal("8"))));
        assertEquals("0.08", Numbers.plain(Units.require("per_1000").toCanonical(new BigDecimal("80"))));
        assertEquals("0.08", Numbers.plain(Units.require("proportion").toCanonical(new BigDecimal("0.08"))));
        assertEquals(Units.Dimension.MASS, Units.require("kg").dimension());
        assertEquals(Units.Dimension.RATE, Units.require("percent").dimension());
        assertEquals(Units.Dimension.COUNT, Units.require("count").dimension());
        InvalidInputException unknown = assertThrows(InvalidInputException.class, () -> Units.require("furlong"));
        assertEquals("unit", unknown.category());
        assertThrows(InvalidInputException.class, () -> Units.requireCurrency("GBP"));
    }
}
