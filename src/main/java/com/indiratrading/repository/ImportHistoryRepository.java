package com.indiratrading.repository;

import com.indiratrading.model.ImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {
    @Query("SELECT COALESCE(MAX(ih.id), 0) FROM ImportHistory ih")
    Long findMaxId();

    List<ImportHistory> findBySourceNameOrderByVersionDesc(String sourceName);
    ImportHistory findBySourceNameAndFileHash(String sourceName, String fileHash);
}
