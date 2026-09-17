package com.indiratrading.repository;

import com.indiratrading.model.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM Case c")
    Long findMaxId();

    Page<Case> findByClientIdIn(List<String> clientIds, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE " +
           "(:clientIdsEmpty = true OR c.clientId IN :clientIds) AND " +
           "(:severity IS NULL OR c.severity = :severity) AND " +
           "(:state IS NULL OR c.state = :state)")
    Page<Case> findByFilters(@Param("clientIds") List<String> clientIds,
                             @Param("clientIdsEmpty") boolean clientIdsEmpty,
                             @Param("severity") String severity,
                             @Param("state") String state,
                             Pageable pageable);

    List<Case> findByClientIdAndIsin(String clientId, String isin);

    long countByState(String state);

    long countBySeverity(String severity);
}
