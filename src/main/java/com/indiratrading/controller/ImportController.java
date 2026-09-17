package com.indiratrading.controller;

import com.indiratrading.model.ImportHistory;
import com.indiratrading.model.Case;
import com.indiratrading.service.*;
import com.indiratrading.adapter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ImportController {

    @Autowired private ImportService importService;
    @Autowired private ConcurrentImportService concurrentImportService;
    @Autowired private PerformanceMetricsService metricsService;
    @Autowired private AdapterRegistry adapterRegistry;

    @Value("${app.data.path:src/main/resources/data}")
    private String dataPath;

    @PostMapping("/imports")
    public ResponseEntity<Map<String, Object>> importAll() {
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> importResults = new ArrayList<>();
        List<Case> allCases = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Path dataDir = Path.of(dataPath);
        LocalDateTime cutAt = LocalDateTime.now();

        // Map to store parsed data for case generation
        Map<String, List<Map<String, String>>> parsedData = new HashMap<>();

        String[] sources = {"INTERNAL_HOLDINGS", "DP_POSITION", "CASH_LEDGER",
                           "BANK_CONFIRMATION", "EXCHANGE_REFERENCE"};
        String[] files = {"internal_holdings_snapshot.csv", "dp_position_extract.html",
                         "cash_ledger.jsonl", "bank_confirmation.xlsx", "exchange_reference.csv"};
        String[] fallbacks = {"", "", "", "bank_confirmation.csv", ""};

        for (int i = 0; i < sources.length; i++) {
            try {
                Path filePath = dataDir.resolve(files[i]);
                // Fallback to CSV if XLSX not found (bank confirmation)
                if (!Files.exists(filePath) && !fallbacks[i].isEmpty()) {
                    filePath = dataDir.resolve(fallbacks[i]);
                }
                if (!Files.exists(filePath)) {
                    errors.add("File not found: " + files[i]);
                    continue;
                }

                byte[] raw = Files.readAllBytes(filePath);
                ImportHistory imp = concurrentImportService.importWithLocking(
                    sources[i], files[i], raw, cutAt);

                // Parse data for case generation
                try {
                    SourceAdapter adapter = adapterRegistry.getAdapter(sources[i]);
                    List<Map<String, String>> records = adapter.parse(raw);
                    parsedData.put(sources[i], records);
                } catch (Exception e) {
                    errors.add("Parse warning for " + sources[i] + ": " + e.getMessage());
                }

                Map<String, Object> importInfo = new HashMap<>();
                importInfo.put("sourceName", sources[i]);
                importInfo.put("fileName", files[i]);
                importInfo.put("importId", imp.getId());
                importInfo.put("version", imp.getVersion());
                importInfo.put("status", imp.getStatus());
                importResults.add(importInfo);
                metricsService.incrementImports();

            } catch (Exception e) {
                errors.add("Failed to import " + sources[i] + ": " + e.getMessage());
            }
        }

        // Generate cases from parsed data
        try {
            List<Map<String, String>> holdings = parsedData.getOrDefault("INTERNAL_HOLDINGS", Collections.emptyList());
            List<Map<String, String>> dpPositions = parsedData.getOrDefault("DP_POSITION", Collections.emptyList());
            List<Map<String, String>> bankConfirmations = parsedData.getOrDefault("BANK_CONFIRMATION", Collections.emptyList());

            // Compute file hashes for evidence
            String holdingsHash = computeHash(Files.readAllBytes(dataDir.resolve("internal_holdings_snapshot.csv")));
            String dpHash = computeHash(Files.readAllBytes(dataDir.resolve("dp_position_extract.html")));
            String combinedHash = holdingsHash + "|" + dpHash;

            if (!holdings.isEmpty() && !dpPositions.isEmpty()) {
                ImportHistory holdingsImp = concurrentImportService.importWithLocking(
                    "INTERNAL_HOLDINGS", "internal_holdings_snapshot.csv",
                    Files.readAllBytes(dataDir.resolve("internal_holdings_snapshot.csv")), cutAt);
                allCases.addAll(importService.generateHoldingCases(
                    "INTERNAL_HOLDINGS", holdingsImp.getId(), holdings, dpPositions, combinedHash));
            }

            if (!parsedData.containsKey("CASH_LEDGER") && !bankConfirmations.isEmpty()) {
                // Cash ledger is JSON lines - parse differently
                try {
                    byte[] cashRaw = Files.readAllBytes(dataDir.resolve("cash_ledger.jsonl"));
                    String cashHash = computeHash(cashRaw);
                    List<Map<String, String>> cashEvents = new ArrayList<>();
                    String content = new String(cashRaw);
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> raw = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(line, Map.class);
                            Map<String, String> record = new HashMap<>();
                            raw.forEach((k, v) -> record.put(k, v != null ? v.toString() : ""));
                            cashEvents.add(record);
                        }
                    }

                    ImportHistory cashImp = concurrentImportService.importWithLocking(
                        "CASH_LEDGER", "cash_ledger.jsonl", cashRaw, cutAt);
                    // Convert to Object maps for cash case generation
                    List<Map<String, Object>> cashEventsObj = new ArrayList<>();
                    for (Map<String, String> e : cashEvents) {
                        Map<String, Object> objMap = new HashMap<>(e);
                        // Convert amount_paise to long
                        String amt = e.get("amount_paise");
                        if (amt != null) {
                            objMap.put("amount_paise", Long.parseLong(amt));
                        }
                        cashEventsObj.add(objMap);
                    }
                    allCases.addAll(importService.generateCashCases(
                        "CASH_LEDGER", cashImp.getId(), cashEventsObj, bankConfirmations, cashHash));
                } catch (Exception e) {
                    errors.add("Cash case generation failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Case generation failed: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        metricsService.recordQueryTime("import_all", elapsed);

        Map<String, Object> result = new HashMap<>();
        result.put("importResults", importResults);
        result.put("totalImports", importResults.size());
        result.put("casesGenerated", allCases.size());
        result.put("cases", allCases);
        result.put("errors", errors);
        result.put("status", errors.isEmpty() ? "SUCCESS" : "PARTIAL");
        result.put("elapsedMs", elapsed);
        result.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/imports/{id}")
    public ResponseEntity<ImportHistory> getImport(@PathVariable Long id) {
        return importService.getImportById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/imports")
    public ResponseEntity<List<ImportHistory>> getAllImports() {
        return ResponseEntity.ok(importService.getAllImports());
    }

    @GetMapping("/imports/status")
    public ResponseEntity<Map<String, Object>> getImportStatus() {
        return ResponseEntity.ok(concurrentImportService.getImportStatus());
    }

    private String computeHash(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
