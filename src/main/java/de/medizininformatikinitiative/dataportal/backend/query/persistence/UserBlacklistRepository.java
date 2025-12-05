package de.medizininformatikinitiative.dataportal.backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, Long> {
  @org.springframework.data.jpa.repository.Query("SELECT t FROM UserBlacklist t WHERE t.userId = ?1")
  Optional<UserBlacklist> findByUserId(String userId);
}
