package com.indiratrading.repository;

import com.indiratrading.model.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
    @Query("SELECT COALESCE(MAX(cn.id), 0) FROM CaseNote cn")
    Long findMaxId();

    List<CaseNote> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
