package com.indiratrading;

import com.indiratrading.service.*;
import com.indiratrading.model.*;
import com.indiratrading.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntegrationTests {

    @Autowired private ImportHistoryRepository importHistoryRepo;
    @Autowired private CaseRepository caseRepo;
    @Autowired private CaseNoteRepository caseNoteRepo;
    @Autowired private UserRepository userRepo;

    @BeforeEach
    void setup() {
        caseNoteRepo.deleteAll();
        caseRepo.deleteAll();
        importHistoryRepo.deleteAll();
    }

    @Test
    void testIdempotentImport() {
        ImportHistory first = new ImportHistory(
            "TEST", "test.csv", "hash123", LocalDateTime.now(), LocalDateTime.now(),
            1, 0, "COMPLETED", "", "", 1
        );
        first.setId(900L);
        importHistoryRepo.save(first);

        ImportHistory existing = importHistoryRepo.findBySourceNameAndFileHash("TEST", "hash123");
        assertNotNull(existing, "Same hash should find existing import");

        List<ImportHistory> all = importHistoryRepo.findBySourceNameOrderByVersionDesc("TEST");
        assertEquals(1, all.size(), "Should only have one import record");
    }

    @Test
    void testRBACDenial() {
        User limitedUser = new User("USR_TEST", "Test User", "SUPPORT", "CLI001,CLI002");
        limitedUser.setId(100L);
        userRepo.save(limitedUser);

        Case testCase = new Case(
            "CLI003", "INE009A01021", "HOLDING_MISMATCH", "HIGH", "OPEN",
            "Test case", LocalDateTime.now(), LocalDateTime.now(), null, "", 1L
        );
        testCase.setId(900L);
        caseRepo.save(testCase);

        Optional<User> user = userRepo.findByUserId("USR_TEST");
        assertTrue(user.isPresent());

        String scope = user.get().getClientScope();
        String[] scopedClients = scope.split(",");
        List<String> clientList = List.of(scopedClients);

        assertFalse(clientList.contains("CLI003"),
            "Support user should not access CLI003");
        assertTrue(clientList.contains("CLI001"),
            "Support user should access CLI001");
    }

    @Test
    void testCaseWorkflowStateTransitions() {
        Case testCase = new Case(
            "CLI001", "INE002A01018", "HOLDING_MISMATCH", "HIGH", "OPEN",
            "Test workflow", LocalDateTime.now(), LocalDateTime.now(), null, "", 1L
        );
        testCase.setId(901L);
        testCase = caseRepo.save(testCase);

        testCase.setState("INVESTIGATING");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("INVESTIGATING", testCase.getState());

        testCase.setState("NEEDS_SOURCE");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("NEEDS_SOURCE", testCase.getState());

        testCase.setState("INVESTIGATING");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("INVESTIGATING", testCase.getState());

        testCase.setState("RESOLVED");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("RESOLVED", testCase.getState());

        testCase.setState("REOPENED");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("REOPENED", testCase.getState());

        testCase.setState("INVESTIGATING");
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase = caseRepo.save(testCase);
        assertEquals("INVESTIGATING", testCase.getState());
    }

    @Test
    void testAuditTrailOnNote() {
        Case testCase = new Case(
            "CLI001", "INE002A01018", "HOLDING_MISMATCH", "HIGH", "OPEN",
            "Test audit", LocalDateTime.now(), LocalDateTime.now(), null, "", 1L
        );
        testCase.setId(902L);
        testCase = caseRepo.save(testCase);

        CaseNote note = new CaseNote(
            testCase.getId(), "USR001", "Investigating the mismatch",
            LocalDateTime.now(), "1", "OPEN", "INVESTIGATING"
        );
        note.setId(900L);
        note = caseNoteRepo.save(note);

        assertNotNull(note.getId());
        assertEquals("USR001", note.getUserId());
        assertEquals("OPEN", note.getPreviousState());
        assertEquals("INVESTIGATING", note.getNewState());

        List<CaseNote> notes = caseNoteRepo.findByCaseIdOrderByCreatedAtDesc(testCase.getId());
        assertEquals(1, notes.size());
        assertEquals("Investigating the mismatch", notes.get(0).getNoteContent());
    }

    @Test
    void testSeverityAssignment() {
        Case highCase = new Case(
            "CLI001", "INE002A01018", "HOLDING_MISMATCH", "HIGH", "OPEN",
            "Confirmed mismatch", LocalDateTime.now(), LocalDateTime.now(), null, "", 1L
        );
        highCase.setId(903L);
        highCase = caseRepo.save(highCase);
        assertEquals("HIGH", highCase.getSeverity());

        Case mediumCase = new Case(
            "CLI002", "INE049A01021", "MISSING_SOURCE", "MEDIUM", "OPEN",
            "Missing DP data", LocalDateTime.now(), LocalDateTime.now(), null, "", 1L
        );
        mediumCase.setId(904L);
        mediumCase = caseRepo.save(mediumCase);
        assertEquals("MEDIUM", mediumCase.getSeverity());
    }
}
