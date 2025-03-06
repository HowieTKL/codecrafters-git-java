package org.howietkl.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;

public class HashObjectCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(HashObjectCommand.class);

  @Override
  public void execute(String[] args) {
    if (args.length < 3 && !"-w".equals(args[1])) {
      return;
    }
    hashObject(args[2]);
  }

  private void hashObject(String filePath) {
    try {
      LOG.info("hash-object path={}", filePath);
      byte[] data = Files.readAllBytes(Path.of(filePath));
      MessageDigest msg = MessageDigest.getInstance("SHA-1");
      String preamble = String.format("blob %s\0", data.length);

      msg.update(preamble.getBytes(StandardCharsets.UTF_8));
      msg.update(data);
      byte[] digest = msg.digest();
      String blobSha = Utils.bytesToHex(digest);
      System.out.println(blobSha);

      File parentDir = new File(String.format(".git/objects/%s", blobSha.substring(0, 2)));
      parentDir.mkdirs();
      File file = new File(parentDir, blobSha.substring(2));
      try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(file, false))) {
        out.write(preamble.getBytes(StandardCharsets.UTF_8));
        out.write(data);
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

}