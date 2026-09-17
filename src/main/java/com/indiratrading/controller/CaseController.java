package com.indiratrading.controller;

import com.indiratrading.exception.AccessDeniedException;
import com.indiratrading.exception.NotFoundException;
import com.indiratrading.model.Case;
import com.indiratrading.model.CaseNote;
import com.indiratrading.model.User;
import com.indiratrading.service.CaseService;
import com.indiratrading.service.UserService;
import com.indiratrading.service.PerformanceMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    @Autowired private CaseService caseService;
    @Autowired private UserService userService;
    @Autowired private PerformanceMetricsService metricsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCases(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-User-Id", defaultValue = "USR001") String userId) {

        User user = userService.getUser(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        List<String> clientScope = userService.getClientScope(userId);
        Page<Case> cases = caseService.getCases(clientScope, severity, state, page, size);

        // Server-side masking for SUPPORT role
        List<Map<String, Object>> maskedCases = new ArrayList<>();
        boolean maskClientId = "SUPPORT".equals(user.getRole());

        for (Case c : cases.getContent()) {
            Map<String, Object> caseMap = new HashMap<>();
            caseMap.put("id", c.getId());
            caseMap.put("clientId", maskClientId ? maskClientId(c.getClientId()) : c.getClientId());
            caseMap.put("isin", c.getIsin());
            caseMap.put("caseType", c.getCaseType());
            caseMap.put("severity", c.getSeverity());
            caseMap.put("state", c.getState());
            caseMap.put("description", c.getDescription());
            caseMap.put("createdAt", c.getCreatedAt());
            caseMap.put("updatedAt", c.getUpdatedAt());
            caseMap.put("sourceName", c.getSourceName());
            caseMap.put("fileHash", c.getFileHash());
            maskedCases.add(caseMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cases", maskedCases);
        response.put("totalElements", cases.getTotalElements());
        response.put("totalPages", cases.getTotalPages());
        response.put("currentPage", cases.getNumber());
        response.put("pageSize", cases.getSize());
        response.put("userRole", user.getRole());
        response.put("userId", user.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCaseDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "USR001") String userId) {

        User user = userService.getUser(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Case c = caseService.getCaseById(id)
            .orElseThrow(() -> new NotFoundException("Case not found: " + id));

        // RBAC: check if user can access this client
        if (!"AUDITOR".equals(user.getRole()) &&
            !userService.canAccessClient(userId, c.getClientId())) {
            throw new AccessDeniedException(
                "User " + userId + " cannot access client " + c.getClientId());
        }

        List<CaseNote> notes = caseService.getCaseNotes(id);

        Map<String, Object> detail = new HashMap<>();
        detail.put("case", c);
        detail.put("notes", notes);
        detail.put("userRole", user.getRole());

        // Server-side masking for SUPPORT role - mask case object fields
        if ("SUPPORT".equals(user.getRole())) {
            Map<String, Object> maskedCase = new HashMap<>();
            maskedCase.put("id", c.getId());
            maskedCase.put("clientId", maskClientId(c.getClientId()));
            maskedCase.put("isin", c.getIsin());
            maskedCase.put("caseType", c.getCaseType());
            maskedCase.put("severity", c.getSeverity());
            maskedCase.put("state", c.getState());
            maskedCase.put("description", maskSensitiveInfo(c.getDescription()));
            maskedCase.put("createdAt", c.getCreatedAt());
            maskedCase.put("updatedAt", c.getUpdatedAt());
            maskedCase.put("sourceName", c.getSourceName());
            maskedCase.put("fileHash", c.getFileHash());
            maskedCase.put("reportCut", c.getReportCut());
            maskedCase.put("receivedAt", c.getReceivedAt());
            maskedCase.put("rawRowId", c.getRawRowId());
            maskedCase.put("mappingVersion", c.getMappingVersion());
            maskedCase.put("importHistoryId", c.getImportHistoryId());
            maskedCase.put("evidenceReferences", maskSensitiveInfo(c.getEvidenceReferences()));
            detail.put("case", maskedCase);

            // Also mask notes
            List<Map<String, Object>> maskedNotes = new ArrayList<>();
            for (CaseNote note : notes) {
                Map<String, Object> maskedNote = new HashMap<>();
                maskedNote.put("id", note.getId());
                maskedNote.put("caseId", note.getCaseId());
                maskedNote.put("userId", note.getUserId());
                maskedNote.put("noteContent", maskSensitiveInfo(note.getNoteContent()));
                maskedNote.put("evidenceVersion", note.getEvidenceVersion());
                maskedNote.put("createdAt", note.getCreatedAt());
                maskedNotes.add(maskedNote);
            }
            detail.put("notes", maskedNotes);
        }

        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Map<String, Object>> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "USR001") String userId) {

        User user = userService.getUser(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Case c = caseService.getCaseById(id)
            .orElseThrow(() -> new NotFoundException("Case not found: " + id));

        // Only investigator or ops_lead can add notes
        if (!"INVESTIGATOR".equals(user.getRole()) && !"OPS_LEAD".equals(user.getRole())) {
            throw new AccessDeniedException(
                "Role " + user.getRole() + " cannot add notes");
        }

        // Check client scope
        if (!userService.canAccessClient(userId, c.getClientId())) {
            throw new AccessDeniedException(
                "User " + userId + " cannot access client " + c.getClientId());
        }

        String noteContent = body.get("note");
        if (noteContent == null || noteContent.trim().isEmpty()) {
            throw new RuntimeException("Note content is required");
        }

        CaseNote note = caseService.addNote(id, userId,
            noteContent, body.getOrDefault("evidenceVersion", "1"));

        Map<String, Object> response = new HashMap<>();
        response.put("note", note);
        response.put("message", "Note added successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<Map<String, Object>> transitionCase(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "USR001") String userId) {

        User user = userService.getUser(userId)
            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Case c = caseService.getCaseById(id)
            .orElseThrow(() -> new NotFoundException("Case not found: " + id));

        // Check client scope
        if (!userService.canAccessClient(userId, c.getClientId())) {
            throw new AccessDeniedException(
                "User " + userId + " cannot access client " + c.getClientId());
        }

        String newState = body.get("newState");
        String reason = body.get("reason");
        String expectedPriorState = body.get("expectedPriorState");

        if (newState == null || newState.trim().isEmpty()) {
            throw new RuntimeException("newState is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("reason is required");
        }

        // Only ops_lead can resolve cases
        if ("RESOLVED".equals(newState) && !"OPS_LEAD".equals(user.getRole())) {
            throw new AccessDeniedException(
                "Only OPS_LEAD can resolve cases");
        }

        Case updated = caseService.transitionCase(id, userId,
            newState, reason, expectedPriorState,
            body.getOrDefault("expectedVersion", "1"));

        Map<String, Object> response = new HashMap<>();
        response.put("case", updated);
        response.put("message", "Case transitioned to " + newState);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = caseService.getMetrics();

        // Add metric definitions
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("totalCases", "Total number of exception cases in the system");
        definitions.put("openCases", "Cases awaiting investigation (newly created)");
        definitions.put("investigatingCases", "Cases currently being investigated by analysts");
        definitions.put("resolvedCases", "Cases that have been resolved and verified");
        definitions.put("criticalSeverity", "Wrong client scope, access leak, corrupted source");
        definitions.put("highSeverity", "Confirmed nonzero holding or cash mismatch at comparable cut");
        definitions.put("mediumSeverity", "Stale, missing, or ambiguous evidence");
        definitions.put("lowSeverity", "Informational item without financial discrepancy");

        metrics.put("definitions", definitions);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(metricsService.getHealthCheck());
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() < 4) return clientId;
        return clientId.substring(0, 3) + "***" + clientId.substring(clientId.length() - 3);
    }

    private String maskSensitiveInfo(String info) {
        if (info == null) return info;
        return info.replaceAll("(client\\s+\\w+\\d+)", "***")
                   .replaceAll("(CLI\\d+)", "CLI***")
                   .replaceAll("(UTR\\d+)", "UTR***");
    }
}
