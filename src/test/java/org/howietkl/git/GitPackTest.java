package org.howietkl.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
class GitPackTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getType() {
    assertEquals(GitObjectType.COMMIT, GitPack.getType((byte) 0b00010000));
    assertEquals(GitObjectType.TREE, GitPack.getType((byte) 0b00100000));
    assertEquals(GitObjectType.BLOB, GitPack.getType((byte) 0b00110000));
    assertEquals(GitObjectType.TAG, GitPack.getType((byte) 0b01000000));
    assertEquals(GitObjectType.OFS_DELTA, GitPack.getType((byte) 0b01100000));
    assertEquals(GitObjectType.REF_DELTA, GitPack.getType((byte) 0b01110000));
  }

  @Test
  void getSizeFromVarint() {
    assertEquals(1, GitPack.getVarInt((byte) 0b01110001, null));
    assertEquals(7, GitPack.getVarInt((byte) 0b01110111, null));
    assertEquals(15, GitPack.getVarInt((byte) 0b01111111, null));

    byte[] bytes = new byte[]{(byte)0b00000001};
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    assertEquals(16, GitPack.getVarInt((byte) 0b11110000, buf));

    bytes = new byte[]{(byte) 0b11110000, (byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(16, anotherVarint(buf));

    bytes = new byte[]{(byte)0b10000000,(byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(2048, GitPack.getVarInt((byte) 0b11110000, buf));

    bytes = new byte[]{(byte) 0b11110000, (byte)0b10000000,(byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(2048, anotherVarint(buf));

    bytes = new byte[]{(byte)0b10000001, (byte)0b10000001, (byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(264208, GitPack.getVarInt((byte) 0b11110000, buf));

    bytes = new byte[]{(byte) 0b11110000, (byte)0b10000001, (byte)0b10000001, (byte)0b00000001};
    buf = ByteBuffer.wrap(bytes);
    assertEquals(264208, anotherVarint(buf));
  }

  private static long anotherVarint(ByteBuffer byteBuffer) {
    int firstByte = byteBuffer.get();

    // Extract type and initial size bits
    final int type = (firstByte >> 4) & 0x07; // Get bits 4-6 for type
    long size = firstByte & 0x0F; // Get bits 0-3 for initial size

    // Process additional size bytes if MSB is set
    int shift = 4; // Start shifting after our initial 4 bits
    while ((firstByte & 0x80) != 0) { // While MSB (bit 7) is set
      firstByte = byteBuffer.get();
      size |= (long) (firstByte & 0x7F) << shift; // Add 7 new bits to size
      shift += 7; // Next byte will shift 7 more positions
    }
    return size;
  }
}