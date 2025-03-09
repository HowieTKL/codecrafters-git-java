package org.howietkl.git.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  public static String bytesToHex(byte[] bytes) {
    StringBuilder hexStringBuilder = new StringBuilder();
    for (byte b : bytes) {
      hexStringBuilder.append(String.format("%02x", b));
    }
    return hexStringBuilder.toString();
  }

  public static String getPath(String sha1) {
    return String.format(".git/objects/%s/%s", sha1.substring(0, 2), sha1.substring(2));
  }

  public static byte[] sha1(byte[] data) {
    try {
      return MessageDigest.getInstance("SHA-1").digest(data);
    } catch (NoSuchAlgorithmException e) {
      LOG.error("SHA-1 algorithm not found", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] getInflated(ByteBuffer buf, int size) throws DataFormatException {
    byte[] data = new byte[size];
    Inflater inflater = new Inflater();
    inflater.setInput(buf);
    inflater.inflate(data);
    inflater.end();
    return data;
  }

  /**
   * data consists of: [type] [data length]\0[contents]
   * @param data entire preamble + data to be written
   * @return sha-1
   * @throws IOException if error occurs during writing
   */
  public static byte[] writeObjectFile(byte[] data, File repoDir) throws IOException {
    byte[] sha1Bytes = sha1(data);
    String sha1 = bytesToHex(sha1Bytes);

    File dir = new File(repoDir, String.format(".git/objects/%s", sha1.substring(0, 2)));
    dir.mkdirs();

    File file = new File(dir, sha1.substring(2));
    LOG.trace("writeObjectFile file={}", file.getAbsolutePath());
    try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(file, false))) {
      out.write(data);
    }
    return sha1Bytes;
  }
}
