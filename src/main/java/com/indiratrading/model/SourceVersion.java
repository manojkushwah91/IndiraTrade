package com.indiratrading.model;

import jakarta.persistence.*;

@Entity
@Table(name = "source_versions")
public class SourceVersion {

    @Id
    private Long id;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileHash;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private byte[] rawBytes;

    @Column(nullable = false)
    private Long importHistoryId;

    public SourceVersion() {}

    public SourceVersion(String sourceName, String fileName, String fileHash,
                         Integer version, byte[] rawBytes, Long importHistoryId) {
        this.sourceName = sourceName;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.version = version;
        this.rawBytes = rawBytes;
        this.importHistoryId = importHistoryId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public byte[] getRawBytes() { return rawBytes; }
    public void setRawBytes(byte[] rawBytes) { this.rawBytes = rawBytes; }
    public Long getImportHistoryId() { return importHistoryId; }
    public void setImportHistoryId(Long importHistoryId) { this.importHistoryId = importHistoryId; }
}
