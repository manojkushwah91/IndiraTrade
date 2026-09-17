package com.indiratrading;

import com.indiratrading.parser.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParserUnitTests {

    private final CsvParser csvParser = new CsvParser();
    private final HtmlParser htmlParser = new HtmlParser();
    private final JsonLineParser jsonLineParser = new JsonLineParser();

    @Test
    void testCsvParserHoldings() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/internal_holdings_snapshot.csv"));
        List<Map<String, String>> records = csvParser.parse(data);

        assertFalse(records.isEmpty(), "Should parse CSV records");
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("INE002A01018", records.get(0).get("isin"));
        assertEquals("100", records.get(0).get("quantity"));
        assertEquals("RELIANCE", records.get(0).get("symbol"));
    }

    @Test
    void testCsvParserExchangeReference() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/exchange_reference.csv"));
        List<Map<String, String>> records = csvParser.parse(data);

        assertFalse(records.isEmpty());
        assertEquals("RELIANCE", records.get(0).get("symbol"));
        assertEquals("INE002A01018", records.get(0).get("isin"));
    }

    @Test
    void testHtmlParserDpPositions() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/dp_position_extract.html"));
        List<Map<String, String>> records = htmlParser.parse(data);

        assertFalse(records.isEmpty(), "Should parse HTML table rows");
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("INE002A01018", records.get(0).get("isin"));
        assertEquals("SETTLED", records.get(0).get("movement_state"));
    }

    @Test
    void testHtmlParserPendingMovement() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/dp_position_extract.html"));
        List<Map<String, String>> records = htmlParser.parse(data);

        long pendingCount = records.stream()
            .filter(r -> "PENDING".equals(r.get("movement_state")))
            .count();

        assertEquals(1, pendingCount, "Should have exactly 1 PENDING movement");
    }

    @Test
    void testJsonLineParserCashLedger() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/cash_ledger.jsonl"));
        List<Map<String, Object>> records = jsonLineParser.parse(data);

        assertFalse(records.isEmpty(), "Should parse JSON lines");
        assertEquals("EVT001", records.get(0).get("event_id"));
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("POSTED", records.get(0).get("state"));
    }

    @Test
    void testJsonLineParserPendingEvents() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/cash_ledger.jsonl"));
        List<Map<String, Object>> records = jsonLineParser.parse(data);

        long pendingCount = records.stream()
            .filter(r -> "PENDING".equals(r.get("state")))
            .count();

        assertEquals(1, pendingCount, "Should have exactly 1 PENDING event");
    }

    @Test
    void testXlsxParserBankConfirmation() throws IOException {
        // Note: XLSX test would need actual XLSX file
        // For now, verify CSV bank confirmation works
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/bank_confirmation.csv"));
        List<Map<String, String>> records = csvParser.parse(data);

        assertFalse(records.isEmpty());
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("UTR001234567", records.get(0).get("bank_ref"));
        assertEquals("5000.00", records.get(0).get("amount_rupees"));
    }

    @Test
    void testIdempotentImport() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/internal_holdings_snapshot.csv"));

        List<Map<String, String>> first = csvParser.parse(data);
        List<Map<String, String>> second = csvParser.parse(data);

        assertEquals(first.size(), second.size(), "Same file should produce same results");
        assertEquals(first.get(0).get("client_id"), second.get(0).get("client_id"));
    }
}
