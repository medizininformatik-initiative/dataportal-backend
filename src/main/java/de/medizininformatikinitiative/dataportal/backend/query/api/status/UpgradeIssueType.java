package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import tools.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
@JsonSerialize(using = UpgradeIssueSerializer.class)
public enum UpgradeIssueType {
  FILTER_CHANGE(1000001, "Filter Change",
      "Changed filter \"{0}\" to \"{1}\" in attribute group \"{2}\""),
  FIELD_NO_LONGER_AVAILABLE(1000002, "Field removed as no longer available",
      "Removed field \"{0}\" from attribute group \"{1}\" as it is no longer available"),
  FIELD_CHANGED_TO_PARENT(1000003, "Field changed to parent",
      "Changed field \"{0}\" to parent field \"{1}\" in attribute group \"{2}\""),
  PROFILE_REMOVED(1000004, "Profile removed as it does not exist anymore",
      "Removed attribute group \"{0}\" as its profile \"{1}\" does not exist anymore"),
  REFERENCE_NO_LONGER_AVAILABLE(1000005, "Reference removed as no longer available",
      "Removed reference \"{0}\" from attribute group \"{1}\" as it is no longer available"),
  REFERENCE_CHANGED_TO_PARENT(1000006, "Reference changed to parent",
      "Changed reference \"{0}\" to parent reference \"{1}\" in attribute group \"{2}\""),
  LINKED_GROUPS_NO_LONGER_AVAILABLE(1000007, "One or more linked group references removed as no longer available",
      "Removed linked group(s) {0} from reference \"{1}\" in attribute group \"{2}\" as they are no longer available"),
  ALL_LINKED_GROUPS_NO_LONGER_AVAILABLE(1000008, "Attribute removed since all referenced groups are no longer available",
      "Removed reference \"{0}\" from attribute group \"{1}\" as none of its linked groups {2} are available anymore");

  private static final UpgradeIssueType[] VALUES;

  static {
    VALUES = values();
  }

  private final int code;
  private final String detail;
  private final String messageTemplate;

  UpgradeIssueType(int code, String detail, String messageTemplate) {
    this.code = code;
    this.detail = detail;
    this.messageTemplate = messageTemplate;
  }

  public static UpgradeIssueType valueOf(int upgradeIssueCode) {
    UpgradeIssueType upgradeIssueType = resolve(upgradeIssueCode);
    if (upgradeIssueType == null) {
      throw new IllegalArgumentException("No matching Upgrade issue for code " + upgradeIssueCode);
    }
    return upgradeIssueType;
  }

  @Nullable
  public static UpgradeIssueType resolve(int upgradeIssueCode) {
    for (UpgradeIssueType upgradeIssueType : VALUES) {
      if (upgradeIssueType.code == upgradeIssueCode) {
        return upgradeIssueType;
      }
    }
    return null;
  }

  public int code() {
    return this.code;
  }

  public String detail() {
    return this.detail;
  }

  /**
   * Builds a human-readable message describing the concrete change, e.g.
   * {@code Changed filter "onset-date" to "recorded-date" in attribute group "Diagnose"}.
   *
   * @param args the values to insert into the message template of this issue type
   * @return the formatted message
   */
  public String formatMessage(Object... args) {
    var stringArgs = Arrays.stream(args).map(String::valueOf).toArray();
    return new MessageFormat(messageTemplate, Locale.ROOT).format(stringArgs);
  }
}
