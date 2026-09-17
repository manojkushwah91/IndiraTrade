package com.indiratrading.service;

import com.indiratrading.adapter.AdapterRegistry;
import com.indiratrading.adapter.SourceAdapter;
import com.indiratrading.model.ImportHistory;
import com.indiratrading.model.SourceVersion;
import com.indiratrading.repository.ImportHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ConcurrentImportService {

    @Autowired private ImportHistoryRepository importHistoryRepo;
    @Autowired private FileVersionService fileVersionService;
    @Autowired private AdapterRegistry adapterRegistry;

    private final ConcurrentHashMap<String, ReentrantLock> importLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImportHistory> inProgressImports = new ConcurrentHashMap<>();

    public ImportHistory importWithLocking(String sourceName, String fileName,
                                            byte[] rawData, LocalDateTime cutAt) {
        ReentrantLock lock = importLocks.computeIfAbsent(sourceName, k -> new ReentrantLock());
        lock.lock();
        try {
            String fileHash = computeHash(rawData);

            // Check if identical file already imported (idempotency)
            ImportHistory existing = importHistoryRepo.findBySourceNameAndFileHash(sourceName, fileHash);
            if (existing != null) {
                return existing;
            }

            // Check if same source is currently being imported
            ImportHistory inProgress = inProgressImports.get(sourceName);
            if (inProgress != null && lock.getHoldCount() > 1) {
                return inProgress;
            }

            // Create actual import record first (to get ID)
            List<ImportHistory> prevImports = importHistoryRepo.findBySourceNameOrderByVersionDesc(
                sourceName);
            int nextVersion = prevImports.isEmpty() ? 1 : prevImports.get(0).getVersion() + 1;

            ImportHistory importRecord = new ImportHistory(
                sourceName, fileName, fileHash, LocalDateTime.now(), cutAt,
                0, 0, "IMPORTING",
                adapterRegistry.getAdapter(sourceName).getColumnMapping().toString(),
                "", nextVersion
            );
            importRecord.setId(importHistoryRepo.findMaxId() + 1);
            importRecord = importHistoryRepo.save(importRecord);

            // Mark as in progress
            inProgressImports.put(sourceName, importRecord);

            try {
                // Store version for comparison (now we have the import ID)
                SourceVersion version = fileVersionService.storeVersion(
                    sourceName, fileName, rawData, importRecord.getId());

                // Update status to completed
                importRecord.setStatus("COMPLETED");
                importRecord = importHistoryRepo.save(importRecord);
                inProgressImports.remove(sourceName);
                return importRecord;

            } catch (Exception e) {
                importRecord.setStatus("FAILED");
                importRecord.setErrors(e.getMessage());
                importHistoryRepo.save(importRecord);
                inProgressImports.remove(sourceName);
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> getImportStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("inProgress", new ArrayList<>(inProgressImports.keySet()));
        status.put("lockQueueSizes", new HashMap<>());
        importLocks.forEach((source, lock) -> {
            Map<String, Object> lockInfo = new HashMap<>();
            lockInfo.put("queuedThreads", lock.getQueueLength());
            lockInfo.put("heldByCurrentThread", lock.isHeldByCurrentThread());
            ((Map<String, Object>) status.get("lockQueueSizes")).put(source, lockInfo);
        });
        return status;
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
