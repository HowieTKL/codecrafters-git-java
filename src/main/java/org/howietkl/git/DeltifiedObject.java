package org.howietkl.git;

public class DeltifiedObject extends GitObject {
  private byte[] sha;

  DeltifiedObject(ObjectInfo info) {
    super(info);
  }

  public byte[] getSha() {
    return sha;
  }

  public void setSha(byte[] sha) {
    this.sha = sha;
  }
}
