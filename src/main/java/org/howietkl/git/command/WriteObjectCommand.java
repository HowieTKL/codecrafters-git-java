package org.howietkl.git.command;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WriteObjectCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(WriteObjectCommand.class);

  @Override
  public void execute(String[] args) {
    if (args.length < 3 && !"-w".equals(args[1])) {
      return;
    }
    System.out.println(Utils.bytesToHex(writeObject(args[2])));
  }

  byte[] writeObject(String filePath) {
    try {
      LOG.info("writeObject path={}", filePath);
      byte[] data = Files.readAllBytes(Path.of(filePath));

      ByteArrayOutputStream buf = new ByteArrayOutputStream(data.length + 16);
      String header = String.format("blob %s\0", data.length);
      buf.write(header.getBytes(StandardCharsets.UTF_8));
      buf.write(data);

      return Utils.writeObjectFile(buf.toByteArray(), new File("."));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}