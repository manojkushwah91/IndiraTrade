package com.indiratrading.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JsonLineAdapter implements SourceAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSourceName() { return "JSON_LINES"; }

    @Override
    public String getFileType() { return "JSON_LINES"; }

    @Override
    public List<Map<String, String>> parse(byte[] rawData) {
        List<Map<String, String>> records = new ArrayList<>();
        String content = new String(rawData);
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> raw = objectMapper.readValue(line, Map.class);
                    Map<String, String> record = new HashMap<>();
                    raw.forEach((k, v) -> record.put(k, v != null ? v.toString() : ""));
                    records.add(record);
                } catch (Exception e) {
                    throw new RuntimeException("JSON line parsing failed: " + e.getMessage(), e);
                }
            }
        }
        return records;
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

    @Override
    public String getFileExtension() { return ".jsonl"; }

    @Override
    public boolean supports(byte[] rawData) {
        String content = new String(rawData).trim();
        return content.startsWith("{") || content.startsWith("[");
    }
}
