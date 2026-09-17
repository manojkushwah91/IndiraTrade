package com.indiratrading.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
public class Case {
    
    @Id
    private Long id;
    
    @Column(nullable = false)
    private String clientId;
    
    @Column(nullable = false)
    private String isin;
    
    @Column(nullable = false)
    private String caseType;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(nullable = false)
    private String state;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(length = 100)
    private String assignedTo;
    
    @Column(length = 2000)
    private String evidenceReferences;

    @Column(nullable = false)
    private Long importHistoryId;

    @Column(length = 100)
    private String sourceName;

    @Column(length = 100)
    private String reportCut;

    @Column(length = 100)
    private String receivedAt;

    @Column(length = 200)
    private String fileHash;

    @Column(length = 100)
    private String rawRowId;

    @Column(length = 50)
    private String mappingVersion;

    public Case() {}

    public Case(String clientId, String isin, String caseType, String severity,
                String state, String description, LocalDateTime createdAt,
                LocalDateTime updatedAt, String assignedTo, String evidenceReferences,
                Long importHistoryId) {
        this.clientId = clientId;
        this.isin = isin;
        this.caseType = caseType;
        this.severity = severity;
        this.state = state;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.assignedTo = assignedTo;
        this.evidenceReferences = evidenceReferences;
        this.importHistoryId = importHistoryId;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public String getEvidenceReferences() { return evidenceReferences; }
    public void setEvidenceReferences(String evidenceReferences) { this.evidenceReferences = evidenceReferences; }
    
    public Long getImportHistoryId() { return importHistoryId; }
    public void setImportHistoryId(Long importHistoryId) { this.importHistoryId = importHistoryId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getReportCut() { return reportCut; }
    public void setReportCut(String reportCut) { this.reportCut = reportCut; }

    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getRawRowId() { return rawRowId; }
    public void setRawRowId(String rawRowId) { this.rawRowId = rawRowId; }

    public String getMappingVersion() { return mappingVersion; }
    public void setMappingVersion(String mappingVersion) { this.mappingVersion = mappingVersion; }
}
