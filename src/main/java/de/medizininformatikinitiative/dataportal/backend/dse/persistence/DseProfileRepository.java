package de.medizininformatikinitiative.dataportal.backend.dse.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DseProfileRepository extends JpaRepository<DseProfileEntity, Long> {

  Optional<DseProfileEntity> findByUrl(String url);
}
