package org.howietkl.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectTypeTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getHeading() {
    assertEquals("blob", ObjectType.BLOB.getHeading());
  }

  @Test
  void parse() {
    assertEquals(ObjectType.BLOB, ObjectType.parse("blob"));
    assertEquals(ObjectType.COMMIT, ObjectType.parse("commit"));
    assertEquals(ObjectType.TREE, ObjectType.parse("tree"));
    assertEquals(ObjectType.TAG, ObjectType.parse("tag"));
    assertEquals(ObjectType.REF_DELTA, ObjectType.parse("ref"));
    assertEquals(ObjectType.BLOB, ObjectType.parse("BLOB"));
  }

  @Test
  void misc() {
    assertEquals("BLOB", ObjectType.BLOB.name());
    assertEquals(ObjectType.BLOB, ObjectType.valueOf(ObjectType.class, "BLOB"));
  }

}