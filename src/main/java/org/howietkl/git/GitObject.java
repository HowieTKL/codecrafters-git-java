package org.howietkl.git;

public class GitObject {
  private GitObjectInfo info;
  private byte[] data;

  GitObject(GitObjectInfo info) {
    this.info = info;
  }

  public GitObjectInfo getInfo() {
    return info;
  }

  public void setInfo(GitObjectInfo info) {
    this.info = info;
  }

  public byte[] getData() {
    // efficient to not copy since we don't modify, but less safe nonetheless
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void populate(String sha1) {

  }

}
