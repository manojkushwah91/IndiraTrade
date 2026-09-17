package com.indiratrading.service;

import com.indiratrading.model.SourceVersion;
import com.indiratrading.repository.SourceVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FileVersionService {

    @Autowired private SourceVersionRepository sourceVersionRepo;

    @Transactional
    public SourceVersion storeVersion(String sourceName, String fileName,
                                       byte[] rawData, Long importHistoryId) {
        String fileHash = computeHash(rawData);

        Optional<SourceVersion> existing = sourceVersionRepo.findBySourceNameAndFileHash(
            sourceName, fileHash);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<SourceVersion> prevVersions = sourceVersionRepo.findBySourceNameOrderByVersionDesc(
            sourceName);
        int nextVersion = prevVersions.isEmpty() ? 1 : prevVersions.get(0).getVersion() + 1;

        SourceVersion version = new SourceVersion(
            sourceName, fileName, fileHash, nextVersion, rawData, importHistoryId
        );
        version.setId(sourceVersionRepo.findMaxId() + 1);
        return sourceVersionRepo.save(version);
    }

    public Optional<SourceVersion> getLatestVersion(String sourceName) {
        List<SourceVersion> versions = sourceVersionRepo.findBySourceNameOrderByVersionDesc(sourceName);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    public Optional<SourceVersion> getVersion(String sourceName, Integer version) {
        return sourceVersionRepo.findBySourceNameAndVersion(sourceName, version);
    }

    public List<SourceVersion> getAllVersions(String sourceName) {
        return sourceVersionRepo.findBySourceNameOrderByVersionDesc(sourceName);
    }

    public Map<String, Object> compareVersions(String sourceName, Integer v1, Integer v2) {
        Optional<SourceVersion> ver1 = sourceVersionRepo.findBySourceNameAndVersion(sourceName, v1);
        Optional<SourceVersion> ver2 = sourceVersionRepo.findBySourceNameAndVersion(sourceName, v2);

        if (ver1.isEmpty() || ver2.isEmpty()) {
            throw new RuntimeException("Version not found");
        }

        SourceVersion version1 = ver1.get();
        SourceVersion version2 = ver2.get();

        Map<String, Object> diff = new HashMap<>();
        diff.put("sourceName", sourceName);
        diff.put("version1", v1);
        diff.put("version2", v2);
        diff.put("hash1", version1.getFileHash());
        diff.put("hash2", version2.getFileHash());
        diff.put("identical", version1.getFileHash().equals(version2.getFileHash()));

        String content1 = new String(version1.getRawBytes(), StandardCharsets.UTF_8);
        String content2 = new String(version2.getRawBytes(), StandardCharsets.UTF_8);
        String[] lines1 = content1.split("\n");
        String[] lines2 = content2.split("\n");

        diff.put("linesVersion1", lines1.length);
        diff.put("linesVersion2", lines2.length);

        List<String> changes = new ArrayList<>();
        int maxLines = Math.max(lines1.length, lines2.length);
        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.length ? lines1[i] : "<missing>";
            String line2 = i < lines2.length ? lines2[i] : "<missing>";
            if (!line1.equals(line2)) {
                changes.add("Line " + (i + 1) + ": [" + line1.substring(0, Math.min(50, line1.length())) +
                    "] -> [" + line2.substring(0, Math.min(50, line2.length())) + "]");
            }
        }
        diff.put("changes", changes);
        diff.put("changeCount", changes.size());

        return diff;
    }

    public boolean isIdempotent(String sourceName, byte[] rawData) {
        String fileHash = computeHash(rawData);
        return sourceVersionRepo.findBySourceNameAndFileHash(sourceName, fileHash).isPresent();
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
