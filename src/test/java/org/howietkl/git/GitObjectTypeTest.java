package org.howietkl.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitObjectTypeTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getHeading() {
    assertEquals("blob", GitObjectType.BLOB.getHeading());
  }

  @Test
  void parse() {
    assertEquals(GitObjectType.BLOB, GitObjectType.parse("blob"));
    assertEquals(GitObjectType.COMMIT, GitObjectType.parse("commit"));
    assertEquals(GitObjectType.TREE, GitObjectType.parse("tree"));
    assertEquals(GitObjectType.TAG, GitObjectType.parse("tag"));
    assertEquals(GitObjectType.REF_DELTA, GitObjectType.parse("ref"));
    assertEquals(GitObjectType.BLOB, GitObjectType.parse("BLOB"));
  }

  @Test
  void misc() {
    assertEquals("BLOB", GitObjectType.BLOB.name());
    assertEquals(GitObjectType.BLOB, GitObjectType.valueOf(GitObjectType.class, "BLOB"));
  }

}