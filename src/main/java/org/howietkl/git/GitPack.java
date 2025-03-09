package org.howietkl.git;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class GitPack {
  private static final Logger LOG = LoggerFactory.getLogger(GitPack.class);

  static final int VARINT_CONTINUE_MASK = 0b10000000;
  static final int VARINT_7BIT_MASK = 0b01111111;
  static final int OBJECT_TYPE_MASK = 0b01110000; // 3-bit type
  static final int VARINT_4BIT_MASK = 0b00001111;

  File dir;
  int version;
  int objectCount;
  List<GitObject> objects = new ArrayList<>();

  /**
   * Both type and size are represented by a variable length integer.
   * E.g. 0b1tttxxxx, 0b1yyyyyyy 0b0zzzzzzz => type=0bttt size=0bzzzzzzzzyyyyyyyxxxx
   * The MSB of 1 indicates whether us to continue using the next byte
   * to calculate the size, and concatenated in little-endian fashion.
   *
   * @param buf remaining pack buffer
   * @return size from varint
   */
  static GitObjectInfo getObjectInfo(ByteBuffer buf) {
    GitObjectInfo info = new GitObjectInfo();
    byte b = buf.get();
    info.setType(getType(b));
    info.setSize(getVarInt(b, buf));
    return info;
  }

  static GitObjectType getType(byte b) {
    return GitObjectType.values[(b & OBJECT_TYPE_MASK) >> 4];
  }

  static int getVarInt(byte first, ByteBuffer buf) {
    int objectSize = first & VARINT_4BIT_MASK;
    if ((first & VARINT_CONTINUE_MASK) != 0) {
      objectSize |= getVarInt(buf) << 4;
    }
    return objectSize;
  }

  static int getVarInt(ByteBuffer buf) {
    byte next = buf.get();
    int objectSize = next & VARINT_7BIT_MASK;
    if ((next & VARINT_CONTINUE_MASK) != 0) {
      objectSize |= getVarInt(buf) << 7;
    }
    return objectSize;
  }

  public static GitPack process(byte[] pack, File dir) throws DataFormatException, IOException {
    ByteBuffer buf = ByteBuffer.wrap(pack);
    GitPack p = new GitPack();
    p.dir = dir;

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

  private void processObjects(ByteBuffer buf) throws DataFormatException, IOException {
    for (int i = 0; i < objectCount; i++) {
      processObject(buf, i);
    }
    assert objects.size() == objectCount;
  }

  private void processObject(ByteBuffer buf, int i) throws DataFormatException, IOException {
    GitObjectInfo info = getObjectInfo(buf);
    LOG.debug("processObjects objectIndex={} type={} size={}", i, info.getType(), info.getSize());
    GitObject gitObject;
    switch (info.getType()) {
      case COMMIT, TREE, BLOB, TAG -> {
        gitObject = new GitObject(info);
        processUndeltified(buf, gitObject);
        createObjectFile(gitObject);
      }
      case REF_DELTA -> {
        DeltifiedGitObject deltifiedGitObject = new DeltifiedGitObject(info);
        gitObject = deltifiedGitObject;
        processDeltified(buf, deltifiedGitObject);
      }
      case OFS_DELTA -> {
        DeltifiedGitObject deltifiedGitObject = new DeltifiedGitObject(info);
        gitObject = deltifiedGitObject;
        int offset = getVarInt(buf);
        int currentPosition = buf.position();
        int offsetPosition = currentPosition - offset;
        buf.position(offsetPosition);
        processDeltified(buf, deltifiedGitObject);
      }
      default -> throw new UnsupportedOperationException("Unsupported object type=" + info.getType());
    }
    objects.add(gitObject);
  }

  private void processDeltified(ByteBuffer buf, DeltifiedGitObject deltifiedGitObject) throws DataFormatException {
    byte[] sha1 = new byte[20];
    buf.get(sha1);
    String sha1String = Utils.bytesToHex(sha1);
    deltifiedGitObject.setSha(sha1);
    LOG.debug("processObject type={} sha1={}", deltifiedGitObject.getInfo().getType(), sha1String);
    deltifiedGitObject.setData(Utils.getInflated(buf, deltifiedGitObject.getInfo().getSize()));
  }

  private void processUndeltified(ByteBuffer buf, GitObject po) throws DataFormatException {
    byte[] inflatedData = Utils.getInflated(buf, po.getInfo().getSize());
    po.setData(inflatedData);
    if (po.getInfo().getType() == GitObjectType.BLOB) {
      LOG.trace("processUndeltified BLOB data={}", new String(inflatedData, StandardCharsets.UTF_8));
    }
  }

  void createObjectFile(GitObject gitObject) throws IOException {
    byte[] data = gitObject.getData();
    ByteArrayOutputStream buf = new ByteArrayOutputStream(data.length + 16);
    String header = String.format("%s %s\0", gitObject.getInfo().getType().getHeading(), data.length);
    buf.write(header.getBytes(StandardCharsets.UTF_8));
    buf.write(data);
    Utils.writeObjectFile(buf.toByteArray(), dir);
  }

}
