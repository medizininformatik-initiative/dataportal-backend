package de.medizininformatikinitiative.dataportal.backend.query.api.status;

import tools.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize(using = UpgradeIssueSerializer.class)
public enum UpgradeIssueType {
  FILTER_CHANGE(1000001, "Filter Change"),
  FIELD_NO_LONGER_AVAILABLE(1000002, "Field removed as no longer available"),
  FIELD_CHANGED_TO_PARENT(1000003, "Field changed to parent"),
  PROFILE_REMOVED(1000004, "Profile removed as it does not exist anymore"),
  REFERENCE_NO_LONGER_AVAILABLE(1000005, "Reference removed as no longer available"),
  REFERENCE_CHANGED_TO_PARENT(1000006, "Reference changed to parent"),
  LINKED_GROUPS_NO_LONGER_AVAILABLE(1000007, "One or more linked group references removed as no longer available"),
  ALL_LINKED_GROUPS_NO_LONGER_AVAILABLE(1000008, "Attribute removed since all referenced groups are no longer available");

  private static final UpgradeIssueType[] VALUES;

  static {
    VALUES = values();
  }

  private final int code;
  private final String detail;

  UpgradeIssueType(int code, String detail) {
    this.code = code;
    this.detail = detail;
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
}
