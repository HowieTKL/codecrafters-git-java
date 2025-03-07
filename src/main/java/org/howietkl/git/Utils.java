package org.howietkl.git;

public class Utils {

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

}
