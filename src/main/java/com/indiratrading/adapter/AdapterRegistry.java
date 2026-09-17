package com.indiratrading.adapter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdapterRegistry {

    private final Map<String, SourceAdapter> adapters = new HashMap<>();
    private final Map<String, SourceAdapter> byFileType = new HashMap<>();

    // Map specific source names to adapters
    private static final Map<String, String> SOURCE_TO_TYPE = Map.of(
        "INTERNAL_HOLDINGS", "csv",
        "EXCHANGE_REFERENCE", "csv",
        "DP_POSITION", "html",
        "CASH_LEDGER", "json_lines",
        "BANK_CONFIRMATION", "csv"
    );

    public AdapterRegistry(List<SourceAdapter> adapterList) {
        for (SourceAdapter adapter : adapterList) {
            adapters.put(adapter.getSourceName(), adapter);
            byFileType.put(adapter.getFileType().toLowerCase(), adapter);
        }
    }

    public SourceAdapter getAdapter(String sourceName) {
        // First try direct lookup
        SourceAdapter adapter = adapters.get(sourceName);
        if (adapter != null) return adapter;

        // Then try via source-to-type mapping
        String fileType = SOURCE_TO_TYPE.get(sourceName);
        if (fileType != null) {
            adapter = byFileType.get(fileType);
            if (adapter != null) return adapter;
        }

        throw new IllegalArgumentException("No adapter for source: " + sourceName);
    }

    public SourceAdapter getAdapterByFileType(String fileType) {
        SourceAdapter adapter = byFileType.get(fileType.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for file type: " + fileType);
        }
        return adapter;
    }

    public SourceAdapter detectAdapter(byte[] data, String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".csv")) return byFileType.get("csv");
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return byFileType.get("html");
        if (lower.endsWith(".jsonl") || lower.endsWith(".json")) return byFileType.get("json_lines");
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return byFileType.get("xlsx");
        throw new IllegalArgumentException("Cannot detect adapter for: " + fileName);
    }

    public Map<String, SourceAdapter> getAllAdapters() {
        return Map.copyOf(adapters);
    }
}
