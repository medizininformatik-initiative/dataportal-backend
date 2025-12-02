package de.fdpg.dataportal_backend.query.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, Long> {
  @org.springframework.data.jpa.repository.Query("SELECT t FROM UserBlacklist t WHERE t.userId = ?1")
  Optional<UserBlacklist> findByUserId(String userId);
}
