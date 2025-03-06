package org.howietkl;

public class Utils {

  public static String bytesToHex(byte[] bytes) {
    StringBuilder hexStringBuilder = new StringBuilder();
    for (byte b : bytes) {
      hexStringBuilder.append(String.format("%02x", b));
    }
    return hexStringBuilder.toString();
  }

}
