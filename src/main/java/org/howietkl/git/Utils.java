package org.howietkl.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

  /*
  public static String getTreeMode(File file) throws IOException {
    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file.toPath(), LinkOption.NOFOLLOW_LINKS);
  }
   */

}
