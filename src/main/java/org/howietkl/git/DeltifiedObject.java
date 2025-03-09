package org.howietkl.git;

public class DeltifiedObject extends GitObject {
  byte[] sha;

  DeltifiedObject(ObjectInfo info) {
    super(info);
  }
}
