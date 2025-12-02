package de.fdpg.dataportal_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QueryContentRepository extends JpaRepository<QueryContent, Long> {
    @Query("SELECT t FROM QueryContent t WHERE t.hash = ?1")
    Optional<QueryContent> findByHash(String queryContentHash);

    @Query("SELECT t FROM QueryContent t LEFT JOIN Query q on t.id = q.queryContent.id WHERE q.id = ?1")
    Optional<QueryContent> findByQueryId(Long queryId);
}
