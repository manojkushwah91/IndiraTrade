package com.indiratrading.service;

import com.indiratrading.model.Case;
import com.indiratrading.model.CaseNote;
import com.indiratrading.repository.CaseRepository;
import com.indiratrading.repository.CaseNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CaseService {

    private static final Set<String> VALID_TRANSITIONS = Set.of(
        "OPEN->INVESTIGATING",
        "INVESTIGATING->NEEDS_SOURCE",
        "INVESTIGATING->RESOLVED",
        "NEEDS_SOURCE->INVESTIGATING",
        "RESOLVED->REOPENED",
        "REOPENED->INVESTIGATING"
    );

    @Autowired private CaseRepository caseRepo;
    @Autowired private CaseNoteRepository caseNoteRepo;

    public Page<Case> getCases(List<String> clientIds, String severity, String state,
                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean emptyScope = clientIds == null || clientIds.isEmpty();
        return caseRepo.findByFilters(clientIds, emptyScope, severity, state, pageable);
    }

    public Optional<Case> getCaseById(Long id) {
        return caseRepo.findById(id);
    }

    public List<CaseNote> getCaseNotes(Long caseId) {
        return caseNoteRepo.findByCaseIdOrderByCreatedAtDesc(caseId);
    }

    @Transactional
    public CaseNote addNote(Long caseId, String userId, String noteContent,
                            String evidenceVersion) {
        Case caseEntity = caseRepo.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        CaseNote note = new CaseNote(
            caseId, userId, noteContent, LocalDateTime.now(),
            evidenceVersion, caseEntity.getState(), null
        );
        note.setId(caseNoteRepo.findMaxId() + 1);
        return caseNoteRepo.save(note);
    }

    @Transactional
    public Case transitionCase(Long caseId, String userId, String newState,
                               String reason, String expectedPriorState,
                               String expectedVersion) {
        Case caseEntity = caseRepo.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        String transitionKey = caseEntity.getState() + "->" + newState;
        if (!VALID_TRANSITIONS.contains(transitionKey)) {
            throw new RuntimeException("Invalid transition: " + transitionKey);
        }

        if (expectedPriorState != null && !expectedPriorState.equals(caseEntity.getState())) {
            throw new RuntimeException("Expected prior state " + expectedPriorState +
                " but found " + caseEntity.getState());
        }

        String previousState = caseEntity.getState();
        caseEntity.setState(newState);
        caseEntity.setUpdatedAt(LocalDateTime.now());
        caseRepo.save(caseEntity);

        CaseNote note = new CaseNote(
            caseId, userId, "State transition: " + previousState + " -> " + newState +
            "\nReason: " + reason,
            LocalDateTime.now(), expectedVersion, previousState, newState
        );
        note.setId(caseNoteRepo.findMaxId() + 1);
        caseNoteRepo.save(note);

        return caseEntity;
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalCases", caseRepo.count());
        metrics.put("openCases", caseRepo.countByState("OPEN"));
        metrics.put("investigatingCases", caseRepo.countByState("INVESTIGATING"));
        metrics.put("resolvedCases", caseRepo.countByState("RESOLVED"));
        metrics.put("criticalSeverity", caseRepo.countBySeverity("CRITICAL"));
        metrics.put("highSeverity", caseRepo.countBySeverity("HIGH"));
        metrics.put("mediumSeverity", caseRepo.countBySeverity("MEDIUM"));
        metrics.put("lowSeverity", caseRepo.countBySeverity("LOW"));
        return metrics;
    }
}
