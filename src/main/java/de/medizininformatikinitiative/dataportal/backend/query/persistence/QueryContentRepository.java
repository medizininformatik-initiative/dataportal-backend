package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QueryContentRepository extends JpaRepository<QueryContentEntity, Long> {
  @Query("SELECT t FROM QueryContent t WHERE t.hash = ?1")
  Optional<QueryContentEntity> findByHash(String queryContentHash);

  @Query("SELECT t FROM QueryContent t LEFT JOIN Query q on t.id = q.queryContent.id WHERE q.id = ?1")
  Optional<QueryContentEntity> findByQueryId(Long queryId);
}
