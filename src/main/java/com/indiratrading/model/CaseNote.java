package com.indiratrading.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_notes")
public class CaseNote {
    
    @Id
    private Long id;
    
    @Column(nullable = false)
    private Long caseId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String noteContent;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private String evidenceVersion;
    
    @Column(length = 100)
    private String previousState;
    
    @Column(length = 100)
    private String newState;
    
    public CaseNote() {}
    
    public CaseNote(Long caseId, String userId, String noteContent, 
                   LocalDateTime createdAt, String evidenceVersion,
                   String previousState, String newState) {
        this.caseId = caseId;
        this.userId = userId;
        this.noteContent = noteContent;
        this.createdAt = createdAt;
        this.evidenceVersion = evidenceVersion;
        this.previousState = previousState;
        this.newState = newState;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getNoteContent() { return noteContent; }
    public void setNoteContent(String noteContent) { this.noteContent = noteContent; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getEvidenceVersion() { return evidenceVersion; }
    public void setEvidenceVersion(String evidenceVersion) { this.evidenceVersion = evidenceVersion; }
    
    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }
    
    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }
}
