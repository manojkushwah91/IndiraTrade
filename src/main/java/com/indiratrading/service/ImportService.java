package com.indiratrading.service;

import com.indiratrading.model.*;
import com.indiratrading.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportService {

    @Autowired private ImportHistoryRepository importHistoryRepo;
    @Autowired private CaseRepository caseRepo;
    @Autowired private SourceVersionRepository sourceVersionRepo;

    @Transactional
    public ImportHistory importData(String sourceName, String fileName, byte[] rawData,
                                    LocalDateTime cutAt) {
        String fileHash = computeHash(rawData);

        ImportHistory existing = importHistoryRepo.findBySourceNameAndFileHash(sourceName, fileHash);
        if (existing != null) {
            return existing;
        }

        List<ImportHistory> prevImports = importHistoryRepo.findBySourceNameOrderByVersionDesc(sourceName);
        int nextVersion = prevImports.isEmpty() ? 1 : prevImports.get(0).getVersion() + 1;

        ImportHistory importRecord = new ImportHistory(
            sourceName, fileName, fileHash, LocalDateTime.now(), cutAt,
            0, 0, "COMPLETED", "", "", nextVersion
        );
        importRecord.setId(importHistoryRepo.findMaxId() + 1);
        importRecord = importHistoryRepo.save(importRecord);

        SourceVersion sv = new SourceVersion(sourceName, fileName, fileHash,
                                             nextVersion, rawData, importRecord.getId());
        sv.setId(sourceVersionRepo.findMaxId() + 1);
        sourceVersionRepo.save(sv);

        return importRecord;
    }

    @Transactional
    public List<Case> generateHoldingCases(String sourceName, Long importHistoryId,
                                            List<Map<String, String>> internalHoldings,
                                            List<Map<String, String>> dpPositions,
                                            String fileHash) {

        List<Case> cases = new ArrayList<>();
        Map<String, Map<String, String>> dpMap = new HashMap<>();

        for (Map<String, String> dp : dpPositions) {
            String key = dp.get("client_id") + "|" + dp.get("isin") + "|" + dp.get("cut_at");
            dpMap.put(key, dp);
        }

        for (Map<String, String> holding : internalHoldings) {
            String clientId = holding.get("client_id");
            String isin = holding.get("isin");
            String cutAt = holding.get("cut_at");
            String symbol = holding.getOrDefault("symbol", "UNKNOWN");
            String exchange = holding.getOrDefault("exchange", "UNKNOWN");

            int internalQty = Integer.parseInt(holding.getOrDefault("quantity", "0"));

            String key = clientId + "|" + isin + "|" + cutAt;
            Map<String, String> dpRow = dpMap.get(key);

            if (dpRow == null) {
                Case c = new Case(clientId, isin, "MISSING_SOURCE", "MEDIUM", "OPEN",
                    "No DP position found for " + isin + " at cut " + cutAt,
                    LocalDateTime.now(), LocalDateTime.now(), null,
                    "Internal: " + symbol + " (" + exchange + ") qty=" + internalQty,
                    importHistoryId);
                c.setSourceName("INTERNAL_HOLDINGS");
                c.setReportCut(cutAt);
                c.setReceivedAt(LocalDateTime.now().toString());
                c.setFileHash(fileHash);
                c.setRawRowId(clientId + "|" + isin + "|" + cutAt);
                c.setMappingVersion("1");
                c.setId(caseRepo.findMaxId() + 1);
                cases.add(caseRepo.save(c));
                continue;
            }

            int dpSettled = Integer.parseInt(dpRow.getOrDefault("settled_qty", "0"));
            int dpPending = Integer.parseInt(dpRow.getOrDefault("pending_qty", "0"));
            String movementState = dpRow.getOrDefault("movement_state", "UNKNOWN");

            if ("PENDING".equalsIgnoreCase(movementState)) {
                Case c = new Case(clientId, isin, "PENDING_DP_MOVEMENT", "MEDIUM", "OPEN",
                    "DP has pending movement of " + dpPending + " for " + isin,
                    LocalDateTime.now(), LocalDateTime.now(), null,
                    "Internal settled=" + internalQty + " DP settled=" + dpSettled +
                    " DP pending=" + dpPending,
                    importHistoryId);
                c.setSourceName("DP_POSITION");
                c.setReportCut(cutAt);
                c.setReceivedAt(LocalDateTime.now().toString());
                c.setFileHash(fileHash);
                c.setRawRowId(clientId + "|" + isin + "|" + cutAt);
                c.setMappingVersion("1");
                c.setId(caseRepo.findMaxId() + 1);
                cases.add(caseRepo.save(c));
            }

            int delta = internalQty - dpSettled;
            if (delta != 0) {
                // Domain rule: confirmed nonzero settled holding mismatch = HIGH
                Case c = new Case(clientId, isin, "HOLDING_MISMATCH", "HIGH", "OPEN",
                    "Holding mismatch for " + isin + ": internal=" + internalQty +
                    " DP=" + dpSettled + " delta=" + delta,
                    LocalDateTime.now(), LocalDateTime.now(), null,
                    "Internal: " + symbol + " (" + exchange + ") settled=" + internalQty +
                    " | DP settled=" + dpSettled + " pending=" + dpPending +
                    " movement=" + movementState,
                    importHistoryId);
                c.setSourceName("INTERNAL_HOLDINGS vs DP_POSITION");
                c.setReportCut(cutAt);
                c.setFileHash(fileHash);
                c.setReceivedAt(LocalDateTime.now().toString());
                c.setRawRowId(clientId + "|" + isin + "|" + cutAt);
                c.setMappingVersion("1");
                c.setId(caseRepo.findMaxId() + 1);
                cases.add(caseRepo.save(c));
            }
        }

        return cases;
    }

    @Transactional
    public List<Case> generateCashCases(String sourceName, Long importHistoryId,
                                         List<Map<String, Object>> cashEvents,
                                         List<Map<String, String>> bankConfirmations,
                                         String fileHash) {

        List<Case> cases = new ArrayList<>();
        Map<String, List<Map<String, String>>> bankByClient = bankConfirmations.stream()
            .collect(Collectors.groupingBy(b -> b.getOrDefault("client_id", "")));

        Map<String, List<Map<String, Object>>> eventsByClient = cashEvents.stream()
            .collect(Collectors.groupingBy(e -> (String) e.getOrDefault("client_id", "")));

        for (Map.Entry<String, List<Map<String, Object>>> entry : eventsByClient.entrySet()) {
            String clientId = entry.getKey();
            List<Map<String, Object>> events = entry.getValue();

            long postedSum = events.stream()
                .filter(e -> "POSTED".equalsIgnoreCase((String) e.get("state")))
                .mapToLong(e -> ((Number) e.get("amount_paise")).longValue())
                .sum();

            long pendingSum = events.stream()
                .filter(e -> "PENDING".equalsIgnoreCase((String) e.get("state")))
                .mapToLong(e -> ((Number) e.get("amount_paise")).longValue())
                .sum();

            List<Map<String, String>> bankEntries = bankByClient.getOrDefault(clientId, Collections.emptyList());

            if (bankEntries.isEmpty()) {
                Case c = new Case(clientId, "N/A", "MISSING_SOURCE", "MEDIUM", "OPEN",
                    "No bank confirmation found for client " + clientId,
                    LocalDateTime.now(), LocalDateTime.now(), null,
                    "Posted cash=" + postedSum + " paise pending=" + pendingSum + " paise",
                    importHistoryId);
                c.setFileHash(fileHash);
                c.setId(caseRepo.findMaxId() + 1);
                cases.add(caseRepo.save(c));
            } else {
                long bankCreditPaise = bankEntries.stream()
                    .filter(b -> "CREDIT".equalsIgnoreCase(b.get("direction")))
                    .mapToLong(b -> {
                        String amtStr = b.getOrDefault("amount_rupees", "0");
                        return new BigDecimal(amtStr).multiply(BigDecimal.valueOf(100))
                                .setScale(0, RoundingMode.HALF_UP).longValue();
                    })
                    .sum();

                if (postedSum != bankCreditPaise) {
                    Case c = new Case(clientId, "N/A", "CASH_MISMATCH", "HIGH", "OPEN",
                        "Cash mismatch: posted=" + postedSum + " paise bank_credit=" + bankCreditPaise + " paise",
                        LocalDateTime.now(), LocalDateTime.now(), null,
                        "Posted events sum=" + postedSum + " | Bank credits sum=" + bankCreditPaise,
                        importHistoryId);
                    c.setFileHash(fileHash);
                    c.setId(caseRepo.findMaxId() + 1);
                    cases.add(caseRepo.save(c));
                }
            }

            List<Map<String, String>> duplicateBankRefs = findDuplicateRefs(bankEntries);
            if (!duplicateBankRefs.isEmpty()) {
                Case c = new Case(clientId, "N/A", "DUPLICATE_BANK_REF", "HIGH", "OPEN",
                    "Duplicate bank references found for client " + clientId,
                    LocalDateTime.now(), LocalDateTime.now(), null,
                    "Duplicate refs: " + duplicateBankRefs.stream()
                        .map(b -> b.get("bank_ref"))
                        .collect(Collectors.joining(", ")),
                    importHistoryId);
                c.setFileHash(fileHash);
                c.setId(caseRepo.findMaxId() + 1);
                cases.add(caseRepo.save(c));
            }
        }

        return cases;
    }

    private List<Map<String, String>> findDuplicateRefs(List<Map<String, String>> bankEntries) {
        Map<String, Long> refCounts = bankEntries.stream()
            .collect(Collectors.groupingBy(b -> b.getOrDefault("bank_ref", ""), Collectors.counting()));
        return bankEntries.stream()
            .filter(b -> refCounts.getOrDefault(b.get("bank_ref"), 0L) > 1)
            .collect(Collectors.toList());
    }

    public Optional<ImportHistory> getImportById(Long id) {
        return importHistoryRepo.findById(id);
    }

    public List<ImportHistory> getAllImports() {
        return importHistoryRepo.findAll();
    }

    private String computeHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
}
