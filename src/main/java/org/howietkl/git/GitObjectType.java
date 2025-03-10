package org.howietkl.git;

public enum GitObjectType {
  INVALID(null),    // 0
  COMMIT("commit"), // 1
  TREE("tree"),     // 2
  BLOB("blob"),     // 3
  TAG("tag"),       // 4
  RESERVED(null),   // 5
  OFS_DELTA(null),  // 6
  REF_DELTA("ref"); // 7
  public static final GitObjectType[] values = values();
  private final String heading;

  GitObjectType(String heading) {
    this.heading = heading;
  }

  String getHeading() {
    return heading != null ? heading : name();
  }

  public static GitObjectType parse(String label) {
    if ("ref".equals(label)) {
      return REF_DELTA;
    }
    label = label.trim().toUpperCase();
    return valueOf(GitObjectType.class, label);
  }
}
