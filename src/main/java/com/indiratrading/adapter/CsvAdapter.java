package com.indiratrading.adapter;

import com.indiratrading.util.DataNormalizer;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CsvAdapter implements SourceAdapter {

    @Override
    public String getSourceName() { return "CSV"; }

    @Override
    public String getFileType() { return "CSV"; }

    @Override
    public List<Map<String, String>> parse(byte[] rawData) {
        List<Map<String, String>> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(rawData), StandardCharsets.UTF_8))
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return records;

            // Normalize headers
            String[] normalizedHeaders = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                normalizedHeaders[i] = headers[i].trim().toLowerCase()
                    .replace(" ", "_").replace("-", "_");
            }

            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    Map<String, String> record = new HashMap<>();
                    for (int i = 0; i < normalizedHeaders.length && i < line.length; i++) {
                        String value = line[i].trim();
                        // Handle empty strings as null
                        record.put(normalizedHeaders[i], value.isEmpty() ? null : value);
                    }
                    records.add(record);
                } catch (Exception e) {
                    errors.add(DataNormalizer.quarantineRow(
                        "Parse error: " + e.getMessage(), String.valueOf(rowNum)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV parsing failed: " + e.getMessage(), e);
        }
        return records;
    }

    @Override
    public Map<String, String> getColumnMapping() {
        return Map.of(
            "client_id", "client_id",
            "isin", "isin",
            "symbol", "symbol",
            "series", "series",
            "quantity", "quantity",
            "cut_at", "cut_at"
        );
    }

    @Override
    public String getFileExtension() { return ".csv"; }
}
