package org.howietkl.git;

public class DeltifiedGitObject extends GitObject {
  private byte[] sha;

  DeltifiedGitObject(GitObjectInfo info) {
    super(info);
  }

  public byte[] getSha() {
    return sha;
  }

  public void setSha(byte[] sha) {
    this.sha = sha;
  }

}
