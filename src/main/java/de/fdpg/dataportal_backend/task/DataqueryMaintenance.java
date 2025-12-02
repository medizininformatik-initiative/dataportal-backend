package de.fdpg.dataportal_backend.task;

import de.fdpg.dataportal_backend.query.persistence.DataqueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataqueryMaintenance {

  private final DataqueryRepository dataqueryRepository;

  public DataqueryMaintenance(DataqueryRepository dataqueryRepository) {
    this.dataqueryRepository = dataqueryRepository;
  }

  @Scheduled(cron = "${app.purgeExpiredQueries}")
  public void purgeExpiredDataqueries() {
    int deletedQueryCount = dataqueryRepository.deleteExpired();
    log.debug("Deleted {} expired queries", deletedQueryCount);
  }

}
