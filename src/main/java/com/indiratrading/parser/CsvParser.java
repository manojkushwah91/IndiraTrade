package com.indiratrading.parser;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class CsvParser implements DataParser<Map<String, String>> {

    @Override
    public List<Map<String, String>> parse(byte[] rawData) {
        List<Map<String, String>> records = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(rawData), StandardCharsets.UTF_8))
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return records;

            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    record.put(headers[i].trim().toLowerCase(), line[i].trim());
                }
                records.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV parsing failed: " + e.getMessage(), e);
        }
        return records;
    }

    @Override
    public String getSourceName() {
        return "CSV";
    }

    @Override
    public Map<String, String> getColumnMapping() {
        return Map.of(
            "client_id", "client_id",
            "isin", "isin",
            "symbol", "symbol",
            "series", "series",
            "exchange", "exchange",
            "quantity", "quantity",
            "cut_at", "cut_at"
        );
    }
}
