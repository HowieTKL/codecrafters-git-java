package org.howietkl.git;

public class GitObjectInfo {
  private GitObjectType type;
  private int size;

  public GitObjectType getType() {
    return type;
  }

  public void setType(GitObjectType type) {
    this.type = type;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }
}
