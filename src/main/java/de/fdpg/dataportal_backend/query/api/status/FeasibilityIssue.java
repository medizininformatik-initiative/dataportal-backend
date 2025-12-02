package de.fdpg.dataportal_backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize(using = FeasibilityIssueSerializer.class)
public enum FeasibilityIssue {

  USER_BLACKLISTED_NOT_POWER_USER(10001, "Too many requests - user is blacklisted", IssueType.FORBIDDEN, Severity.ERROR),
  QUOTA_EXCEEDED(10002, "Too many requests - quota reached", IssueType.BUSY, Severity.ERROR),
  POLLING_LIMIT_EXCEEDED(10003, "Too many requests - polling limit exceeded", IssueType.BUSY, Severity.ERROR),
  PRIVACY_RESTRICTION_RESULT_SIZE(10004, "Total number of results below threshold", IssueType.BUSY, Severity.ERROR),
  PRIVACY_RESTRICTION_RESULT_SITES(10005, "Too few responding sites", IssueType.BUSY, Severity.ERROR),
  SAVED_QUERY_STORAGE_FULL(10006, "Saved query limit reached. Please delete old saved queries before trying to create new ones.", IssueType.FORBIDDEN, Severity.ERROR);

  private static final FeasibilityIssue[] VALUES;

  static {
    VALUES = values();
  }

  private final int code;
  private final String message;
  private final IssueType type;
  private final Severity severity;


  FeasibilityIssue(int code, String message, IssueType type, Severity severity) {
    this.code = code;
    this.message = message;
    this.type = type;
    this.severity = severity;
  }

  public static FeasibilityIssue valueOf(int feasibilityIssueCode) {
    FeasibilityIssue feasibilityIssue = resolve(feasibilityIssueCode);
    if (feasibilityIssue == null) {
      throw new IllegalArgumentException("No matching Feasibility issue for code " + feasibilityIssueCode);
    }
    return feasibilityIssue;
  }

  @Nullable
  public static FeasibilityIssue resolve(int feasibilityIssueCode) {
    for (FeasibilityIssue feasibilityIssue : VALUES) {
      if (feasibilityIssue.code == feasibilityIssueCode) {
        return feasibilityIssue;
      }
    }
    return null;
  }

  public String message() {
    return this.message;
  }

  public IssueType type() {
    return this.type;
  }

  public int code() {
    return this.code;
  }

  public Severity severity() {
    return this.severity;
  }

  public enum IssueType {

    FORBIDDEN("forbidden"),
    BUSY("busy");
    private final String value;

    IssueType(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  }

  public enum Severity {

    ERROR("error");
    private final String value;

    Severity(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  }
}
