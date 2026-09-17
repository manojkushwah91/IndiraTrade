package com.indiratrading.repository;

import com.indiratrading.model.SourceVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface SourceVersionRepository extends JpaRepository<SourceVersion, Long> {
    @Query("SELECT COALESCE(MAX(sv.id), 0) FROM SourceVersion sv")
    Long findMaxId();

    List<SourceVersion> findBySourceNameOrderByVersionDesc(String sourceName);
    Optional<SourceVersion> findBySourceNameAndVersion(String sourceName, Integer version);
    Optional<SourceVersion> findBySourceNameAndFileHash(String sourceName, String fileHash);
}
