package de.fdpg.dataportal_backend.terminology.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextualizedTermCodeRepository extends JpaRepository<ContextualizedTermCode, Long> {
}
