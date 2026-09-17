package com.indiratrading.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_history")
public class ImportHistory {
    
    @Id
    private Long id;
    
    @Column(nullable = false)
    private String sourceName;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileHash;
    
    @Column(nullable = false)
    private LocalDateTime receivedAt;
    
    @Column(nullable = false)
    private LocalDateTime cutAt;
    
    @Column(nullable = false)
    private Integer rowCount;
    
    @Column(nullable = false)
    private Integer errorCount;
    
    @Column(nullable = false)
    private String status;
    
    @Column(length = 1000)
    private String schemaMapping;
    
    @Column(length = 2000)
    private String errors;
    
    @Column(nullable = false)
    private Integer version;
    
    public ImportHistory() {}
    
    public ImportHistory(String sourceName, String fileName, String fileHash, 
                        LocalDateTime receivedAt, LocalDateTime cutAt, 
                        Integer rowCount, Integer errorCount, String status,
                        String schemaMapping, String errors, Integer version) {
        this.sourceName = sourceName;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.receivedAt = receivedAt;
        this.cutAt = cutAt;
        this.rowCount = rowCount;
        this.errorCount = errorCount;
        this.status = status;
        this.schemaMapping = schemaMapping;
        this.errors = errors;
        this.version = version;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    
    public LocalDateTime getCutAt() { return cutAt; }
    public void setCutAt(LocalDateTime cutAt) { this.cutAt = cutAt; }
    
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSchemaMapping() { return schemaMapping; }
    public void setSchemaMapping(String schemaMapping) { this.schemaMapping = schemaMapping; }
    
    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
