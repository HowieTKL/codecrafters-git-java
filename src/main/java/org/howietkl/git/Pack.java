package org.howietkl.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

class Pack {
  private static final Logger LOG = LoggerFactory.getLogger(Pack.class);

  int version;
  int objectCount;
  List<PackObjectParser> pObjects = new ArrayList<>();

  static enum ObjectType {
    INVALID,    // 0
    COMMIT,     // 1
    TREE,       // 2
    BLOB,       // 3
    TAG,        // 4
    RESERVED,   // 5
    OFS_DELTA,  // 6
    REF_DELTA;  // 7
    public static final ObjectType[] values = values();
  }

  static class PackObjectParser {
    static final int VARINT_CONTINUE_MASK = 0b10000000;
    static final int VARINT_7BIT_MASK = 0b01111111;
    static final int OBJECT_TYPE_MASK = 0b01110000; // 3-bit type
    static final int VARINT_4BIT_MASK = 0b00001111;
    ObjectType type;
    byte[] data;

    static ObjectType getType(byte b) {
      return ObjectType.values[(b & OBJECT_TYPE_MASK) >> 4];
    }

    /**
     * Both type and size are represented by a variable length integer.
     * E.g. 0b1tttxxxx, 0b1yyyyyyy 0b0zzzzzzz => type=0bttt size=0bzzzzzzzzyyyyyyyxxxx
     * The MSB of 1 indicates whether us to continue using the next byte
     * to calculate the size, and concatenated in little-endian fashion.
     *
     * @param first byte extracted for type
     * @param buf remaining pack buffer
     * @return size from varint
     */
    static int getVarInt(byte first, ByteBuffer buf) {
      int objectSize = first & VARINT_4BIT_MASK;
      boolean continueParsing = (first & VARINT_CONTINUE_MASK) != 0;
      if (continueParsing) {
        objectSize += getVarInt(buf) << 4;
      }
      return objectSize;
    }

    static int getVarInt(ByteBuffer buf) {
      byte next = buf.get();
      int objectSize = next & VARINT_7BIT_MASK;
      boolean continueParsing = (next & VARINT_CONTINUE_MASK) != 0;
      if (continueParsing) {
        objectSize += getVarInt(buf) << 7;
      }
      return objectSize;
    }
  }

  static Pack process(byte[] pack) throws DataFormatException {
    ByteBuffer buf = ByteBuffer.wrap(pack);
    Pack p = new Pack();

    p.processHeader(buf);
    p.processObjects(buf);
    return p;
  }

  private void processHeader(ByteBuffer buf) {
    byte[] thePACK = new byte[4];
    // header: PACK + version + #objects
    buf.get(thePACK);
    if (!"PACK".equals(new String(thePACK, StandardCharsets.UTF_8))) {
      throw new IllegalStateException("Missing PACK header");
    }
    version = buf.getInt();
    objectCount = buf.getInt();
    LOG.debug("processHeader version={} objects={}", version, objectCount);
  }

  private void processObjects(ByteBuffer buf) throws DataFormatException {
    for (int i = 0; i < objectCount; i++) {
      processObject(buf, i);
    }
    assert pObjects.size() == objectCount;
  }

  private void processObject(ByteBuffer buf, int i) throws DataFormatException {
    byte b = buf.get();
    PackObjectParser pop = new PackObjectParser();
    pop.type = PackObjectParser.getType(b);
    int objectSize = PackObjectParser.getVarInt(b, buf);
    LOG.debug("processObjects object={} type={} size={}", i, pop.type, objectSize);
    switch (pop.type) {
      case COMMIT, TREE, BLOB, TAG -> {
        byte[] data = processUndeltified(buf, objectSize);
        LOG.trace("processObjects object={} type={} data={}",i, pop.type, new String(data, StandardCharsets.UTF_8));
      }
      case OFS_DELTA, REF_DELTA -> {
        processDeltified(buf, objectSize);
      }
      default -> {
        LOG.error("Unsupported pack object type={}", pop.type);
        throw new UnsupportedOperationException("Unsupported pack object type=" + pop.type);
      }
    }
    pObjects.add(pop);
  }

  private byte[] processUndeltified(ByteBuffer buf, int size) throws DataFormatException {
    return getInflated(buf, size);
  }

  private byte[] getInflated(ByteBuffer buf, int size) throws DataFormatException {
    byte[] data = new byte[size];
    Inflater inflater = new Inflater();
    inflater.setInput(buf);
    inflater.inflate(data);
    return data;
  }

  private byte[] processDeltified(ByteBuffer buf, int size) throws DataFormatException {
    byte[] sha1 = new byte[20];
    buf.get(sha1);
    String sha1String = Utils.bytesToHex(sha1);
    LOG.debug("processUndeltified sha1={}", sha1String);
    return getInflated(buf, size);
  }
}
