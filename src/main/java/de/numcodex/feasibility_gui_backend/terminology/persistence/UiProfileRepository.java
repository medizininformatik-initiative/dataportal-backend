package de.numcodex.feasibility_gui_backend.terminology.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UiProfileRepository extends JpaRepository<UiProfile, Long> {

  @Query("select up from ContextualizedTermCode ct left join UiProfile up on ct.uiProfileId = up.id where ct.contextTermcodeHash = :contextualizedTermcodeHash")
  Optional<UiProfile> findByContextualizedTermcodeHash(@Param("contextualizedTermcodeHash") String contextualizedTermcodeHash);

  @Query("select up.name from ContextualizedTermCode ct left join UiProfile up on ct.uiProfileId = up.id where ct.contextTermcodeHash = :contextualizedTermcodeHash")
  Optional<String> getUiProfileNameByContextualizedTermcodeHash(@Param("contextualizedTermcodeHash") String contextualizedTermcodeHash);
}
