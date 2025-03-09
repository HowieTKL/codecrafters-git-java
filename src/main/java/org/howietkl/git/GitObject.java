package org.howietkl.git;

public class GitObject {
  private ObjectInfo info;
  private byte[] data;

  GitObject(ObjectInfo info) {
    this.info = info;
  }

  public ObjectInfo getInfo() {
    return info;
  }

  public void setInfo(ObjectInfo info) {
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
