package com.indiratrading;

import com.indiratrading.util.DataNormalizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseTests {

    @Test
    void testIsinNormalization() {
        assertEquals("INE002A01018", DataNormalizer.normalizeIsin("ine002a01018"));
        assertEquals("INE002A01018", DataNormalizer.normalizeIsin(" INE002A01018 "));
        assertEquals("INE002A01018", DataNormalizer.normalizeIsin("INE002A01018"));
        assertNull(DataNormalizer.normalizeIsin(null));
    }

    @Test
    void testSymbolNormalization() {
        assertEquals("RELIANCE", DataNormalizer.normalizeSymbol("reliance"));
        assertEquals("RELIANCE", DataNormalizer.normalizeSymbol(" Reliance "));
        assertNull(DataNormalizer.normalizeSymbol(null));
    }

    @Test
    void testClientIdNormalization() {
        assertEquals("CLI001", DataNormalizer.normalizeClientId("cli001"));
        assertEquals("CLI001", DataNormalizer.normalizeClientId(" CLI001 "));
        assertNull(DataNormalizer.normalizeClientId(null));
    }

    @Test
    void testParsePaise() {
        // Already in paise
        assertEquals(500000L, DataNormalizer.parsePaise("500000"));
        assertEquals(500000L, DataNormalizer.parsePaise(500000));

        // Rupees to paise
        assertEquals(500000L, DataNormalizer.parsePaise("5000.00"));
        assertEquals(500000L, DataNormalizer.parsePaise(5000.00));

        // Indian comma format
        assertEquals(100000L, DataNormalizer.parsePaise("1,00,000"));

        // With currency symbol
        assertEquals(500000L, DataNormalizer.parsePaise("₹5000"));
        assertEquals(500000L, DataNormalizer.parsePaise("Rs.5000"));
        assertEquals(500000L, DataNormalizer.parsePaise("Rs 5000"));

        // Zero
        assertEquals(0L, DataNormalizer.parsePaise("0"));
        assertEquals(0L, DataNormalizer.parsePaise(null));
    }

    @Test
    void testParseQuantity() {
        assertEquals(100, DataNormalizer.parseQuantity("100"));
        assertEquals(100, DataNormalizer.parseQuantity(100));
        assertEquals(100000, DataNormalizer.parseQuantity("1,00,000"));
        assertEquals(0, DataNormalizer.parseQuantity("0"));
        assertEquals(0, DataNormalizer.parseQuantity(null));
    }

    @Test
    void testParseFlexibleDate() {
        // ISO format
        LocalDateTime result = DataNormalizer.parseFlexibleDate("2026-09-15T15:30:00+05:30");
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(9, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());

        // Without timezone
        result = DataNormalizer.parseFlexibleDate("2026-09-15T15:30:00");
        assertNotNull(result);

        // Space separated
        result = DataNormalizer.parseFlexibleDate("2026-09-15 15:30:00");
        assertNotNull(result);

        // Date only
        result = DataNormalizer.parseFlexibleDate("2026-09-15");
        assertNotNull(result);

        // Indian format
        result = DataNormalizer.parseFlexibleDate("15/09/2026");
        assertNotNull(result);

        // Null/empty
        assertNull(DataNormalizer.parseFlexibleDate(null));
        assertNull(DataNormalizer.parseFlexibleDate(""));
    }

    @Test
    void testIsBlank() {
        assertTrue(DataNormalizer.isBlank(null));
        assertTrue(DataNormalizer.isBlank(""));
        assertTrue(DataNormalizer.isBlank("   "));
        assertFalse(DataNormalizer.isBlank("test"));
        assertFalse(DataNormalizer.isBlank(" test "));
    }

    @Test
    void testQuarantineRow() {
        String result = DataNormalizer.quarantineRow("Invalid data", "42");
        assertTrue(result.contains("42"));
        assertTrue(result.contains("Invalid data"));
        assertTrue(result.startsWith("ROW_ERROR"));
    }
}
