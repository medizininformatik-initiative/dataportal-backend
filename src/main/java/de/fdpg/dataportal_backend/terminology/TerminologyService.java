package de.fdpg.dataportal_backend.terminology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fdpg.dataportal_backend.terminology.api.*;
import de.fdpg.dataportal_backend.terminology.api.UiProfile;
import de.fdpg.dataportal_backend.terminology.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class TerminologyService {

  private final UiProfileRepository uiProfileRepository;

  private final TermCodeRepository termCodeRepository;

  @Getter
  private final List<TerminologySystemEntry> terminologySystems;

  @NonNull
  private ObjectMapper jsonUtil;


  public TerminologyService(@Value("${app.terminologySystemsFile}") String terminologySystemsFilename,
                            UiProfileRepository uiProfileRepository,
                            TermCodeRepository termCodeRepository,
                            ObjectMapper jsonUtil) throws IOException {
    this.uiProfileRepository = uiProfileRepository;
    this.termCodeRepository = termCodeRepository;
    this.jsonUtil = jsonUtil;
    this.terminologySystems = jsonUtil.readValue(new URL("file:" + terminologySystemsFilename), new TypeReference<>() {});
  }

  public String getUiProfileName(String contextualizedTermCodeHash) {
    Optional<String> uiProfileName = uiProfileRepository.getUiProfileNameByContextualizedTermcodeHash(contextualizedTermCodeHash);
    return uiProfileName.orElse("undefined");
  }

  public boolean isExistingTermCode(String system, String code) {
    return termCodeRepository.existsTermCode(system, code);
  }

  public static int min(int... numbers) {
    return Arrays.stream(numbers)
        .min().orElse(Integer.MAX_VALUE);
  }

  public List<CriteriaProfileData> getCriteriaProfileData(List<String> criteriaIds) {
    List<CriteriaProfileData> results = new ArrayList<>();

    for (String id : criteriaIds) {
      TermCode tc = termCodeRepository.findTermCodeByContextualizedTermcodeHash(id).orElse(null);
      Context c = termCodeRepository.findContextByContextualizedTermcodeHash(id).orElse(null);
      de.fdpg.dataportal_backend.common.api.TermCode context;
      List<de.fdpg.dataportal_backend.common.api.TermCode> termCodes = new ArrayList<>();

      if (c != null) {
        context = de.fdpg.dataportal_backend.common.api.TermCode.builder()
            .code(c.getCode())
            .display(c.getDisplay())
            .system(c.getSystem())
            .version(c.getVersion())
            .build();
      } else {
        context = null;
      }
      if (tc != null) {
        termCodes.add(
            de.fdpg.dataportal_backend.common.api.TermCode.builder()
                .code(tc.getCode())
                .display(tc.getDisplay())
                .system(tc.getSystem())
                .version(tc.getVersion())
                .build()
        );
      }
      results.add(
          CriteriaProfileData.builder()
              .id(id)
              .uiProfileId(getUiProfileName(id))
              .context(context)
              .termCodes(termCodes)
              .build()
      );
    }

    return results;
  }

  public List<CriteriaProfileData> addDisplayDataToCriteriaProfileData(List<CriteriaProfileData> criteriaProfileData, List<EsSearchResultEntry> displayData) {
    var result = new ArrayList<CriteriaProfileData>();
    for (CriteriaProfileData cpd : criteriaProfileData) {
      var searchResultEntry = displayData.stream().filter(dd -> cpd.id().equals(dd.id())).findFirst().orElse(null);
      if (searchResultEntry == null) {
        result.add(cpd);
      } else {
        result.add(cpd.addDisplay(searchResultEntry.display()));
      }
    }
    return result;
  }

  public List<UiProfileEntry> getUiProfiles() {
    var uiProfiles = uiProfileRepository.findAll();
    var convertedUiProfiles = uiProfiles.stream()
        .map(uip ->{
          try {
            return jsonUtil.readValue(uip.getUiProfile(), UiProfile.class);
          } catch (Exception e) {
            throw new RuntimeException("Failed to parse uiProfileId: " + uip.getUiProfile(), e);
          }
        })
        .toList();

    return convertedUiProfiles.stream()
        .map(UiProfileEntry::of)
        .toList();
  }
}
