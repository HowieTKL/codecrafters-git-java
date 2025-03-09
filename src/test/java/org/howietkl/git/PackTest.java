package org.howietkl.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
class PackTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getType() {
    assertEquals(Pack.ObjectType.COMMIT, Pack.PackObjectParser.getType((byte) 0b00010000));
    assertEquals(Pack.ObjectType.TREE, Pack.PackObjectParser.getType((byte) 0b00100000));
    assertEquals(Pack.ObjectType.BLOB, Pack.PackObjectParser.getType((byte) 0b00110000));
    assertEquals(Pack.ObjectType.TAG, Pack.PackObjectParser.getType((byte) 0b01000000));
    assertEquals(Pack.ObjectType.OFS_DELTA, Pack.PackObjectParser.getType((byte) 0b01100000));
    assertEquals(Pack.ObjectType.REF_DELTA, Pack.PackObjectParser.getType((byte) 0b01110000));
  }

  @Test
  void getSizeFromVarint() {
    assertEquals(1, Pack.PackObjectParser.getVarInt((byte) 0b01110001, null));
    assertEquals(7, Pack.PackObjectParser.getVarInt((byte) 0b01110111, null));
    assertEquals(15, Pack.PackObjectParser.getVarInt((byte) 0b01111111, null));

    byte[] bytes = new byte[]{(byte)0b00000001};
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    assertEquals(16, Pack.PackObjectParser.getVarInt((byte) 0b11110000, buf));

    bytes = new byte[]{(byte)0b10000000,(byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(2048, Pack.PackObjectParser.getVarInt((byte) 0b11110000, buf));

    bytes = new byte[]{(byte)0b10000001,(byte)0b10000001, (byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(264208, Pack.PackObjectParser.getVarInt((byte) 0b11110000, buf));
  }
}