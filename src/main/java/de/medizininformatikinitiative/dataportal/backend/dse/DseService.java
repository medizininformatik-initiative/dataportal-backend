package de.medizininformatikinitiative.dataportal.backend.dse;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import de.medizininformatikinitiative.dataportal.backend.dse.api.DseProfile;
import de.medizininformatikinitiative.dataportal.backend.dse.persistence.DseProfileEntity;
import de.medizininformatikinitiative.dataportal.backend.dse.persistence.DseProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DseService {

  private final DseProfileRepository dseProfileRepository;

  private final ObjectMapper objectMapper;

  public DseService(DseProfileRepository dseProfileRepository, ObjectMapper objectMapper) {
    this.dseProfileRepository = dseProfileRepository;
    this.objectMapper = objectMapper;
  }

  public List<DseProfile> getProfileData(List<String> profileIds) {
    var results = new ArrayList<DseProfile>();

    for (String profileId : profileIds) {
      Optional<DseProfileEntity> dseProfile;
      try {
        dseProfile = dseProfileRepository.findByUrl(profileId);
      } catch (DataIntegrityViolationException e) {
        dseProfile = Optional.empty();
      }
      if (dseProfile.isPresent()) {
        try {
          results.add(objectMapper.readValue(dseProfile.get().getEntry(), DseProfile.class));
        } catch (JacksonException e) {
          throw new RuntimeException(e);
        }
      } else {
        results.add(DseProfile.builder()
            .url(profileId)
            .errorCode("TBD-00000")
            .errorCause("profile not found")
            .build());
      }
    }

    return results;
  }
}
