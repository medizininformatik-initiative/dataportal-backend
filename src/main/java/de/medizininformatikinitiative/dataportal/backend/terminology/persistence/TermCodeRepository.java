package de.medizininformatikinitiative.dataportal.backend.terminology.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TermCodeRepository extends JpaRepository<TermCode, Long> {

  @Query("select case when count(t) > 0 then true else false end from TermCode t where t.code = :code and t.system = :system and t.version = :version")
  boolean existsTermCode(@Param("system") String system, @Param("code") String code, @Param("version") String version);

  @Query("select case when count(t) > 0 then true else false end from TermCode t where t.code = :code and t.system = :system")
  boolean existsTermCode(@Param("system") String system, @Param("code") String code);

  @Query("select tc from ContextualizedTermCode ctc left join TermCode tc on ctc.termCodeId = tc.id where ctc.contextTermcodeHash = :hash")
  Optional<TermCode> findTermCodeByContextualizedTermcodeHash(@Param("hash") String contextualizedTermcodeHash);

  @Query("select c from ContextualizedTermCode ctc left join Context c on ctc.contextId = c.id where ctc.contextTermcodeHash = :hash")
  Optional<Context> findContextByContextualizedTermcodeHash(@Param("hash") String contextualizedTermcodeHash);
}
