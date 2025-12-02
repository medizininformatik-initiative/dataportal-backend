package de.fdpg.dataportal_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface QueryRepository extends JpaRepository<Query, Long> {

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.createdBy = ?1")
  Optional<List<Query>> findByAuthor(String authorId);

  @org.springframework.data.jpa.repository.Query("SELECT t.createdBy FROM Query t WHERE t.id = ?1")
  Optional<String> getAuthor(Long queryId);

  @NativeQuery(value = "SELECT count (*) FROM query WHERE created_by = ?1 AND created_at > (current_timestamp - (?2 * interval '1 minute'))")
  Long countQueriesByAuthorInTheLastNMinutes(String authorId, long minutes);

  @NativeQuery(value = "SELECT EXTRACT (EPOCH from ( SELECT (current_timestamp - created_at) from query WHERE created_by = ?1 ORDER BY created_at desc LIMIT 1 OFFSET ?2))")
  Long getAgeOfNToLastQueryInSeconds(String authorId, int offset);

  @Modifying(clearAutomatically = true)
  @NativeQuery(value = "UPDATE query SET created_at = ?2 where id =?1;")
  void updateCreationDate(Long queryId, Timestamp timestamp);
}
