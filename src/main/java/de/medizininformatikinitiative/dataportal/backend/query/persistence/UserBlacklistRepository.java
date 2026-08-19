package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklistEntity, Long> {
  @Query("SELECT t FROM UserBlacklist t WHERE t.userId = ?1")
  Optional<UserBlacklistEntity> findByUserId(String userId);
}
