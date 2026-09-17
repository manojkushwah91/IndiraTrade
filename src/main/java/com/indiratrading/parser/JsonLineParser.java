package com.indiratrading.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JsonLineParser implements DataParser<Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> parse(byte[] rawData) {
        List<Map<String, Object>> records = new ArrayList<>();
        String content = new String(rawData);
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = objectMapper.readValue(line, Map.class);
                    records.add(record);
                } catch (Exception e) {
                    throw new RuntimeException("JSON line parsing failed: " + e.getMessage(), e);
                }
            }
        }
        return records;
    }

    @Override
    public String getSourceName() {
        return "JSON_LINES";
    }

    @Override
    public Map<String, String> getColumnMapping() {
        return Map.of(
            "event_id", "event_id",
            "client_id", "client_id",
            "event_type", "event_type",
            "amount_paise", "amount_paise",
            "state", "state",
            "effective_at", "effective_at",
            "description", "description"
        );
    }
}
