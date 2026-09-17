package com.indiratrading;

import com.indiratrading.parser.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ParserTests {

    @Autowired private CsvParser csvParser;
    @Autowired private HtmlParser htmlParser;
    @Autowired private JsonLineParser jsonLineParser;

    @Test
    void testCsvParserHoldings() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/internal_holdings_snapshot.csv"));
        List<Map<String, String>> records = csvParser.parse(data);

        assertFalse(records.isEmpty());
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("INE002A01018", records.get(0).get("isin"));
        assertEquals("100", records.get(0).get("quantity"));
    }

    @Test
    void testHtmlParserDpPositions() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/dp_position_extract.html"));
        List<Map<String, String>> records = htmlParser.parse(data);

        assertFalse(records.isEmpty());
        assertEquals("CLI001", records.get(0).get("client_id"));
        assertEquals("INE002A01018", records.get(0).get("isin"));
        assertEquals("SETTLED", records.get(0).get("movement_state"));
    }

    @Test
    void testJsonLineParserCashLedger() throws IOException {
        byte[] data = Files.readAllBytes(Path.of("src/main/resources/data/cash_ledger.jsonl"));
        List<Map<String, Object>> records = jsonLineParser.parse(data);

        assertFalse(records.isEmpty());
        assertEquals("EVT001", records.get(0).get("event_id"));
        assertEquals("CLI001", records.get(0).get("client_id"));
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
}
