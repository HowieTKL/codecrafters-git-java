package org.howietkl.git.command;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.InflaterInputStream;

public class CatFileCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(CatFileCommand.class);

  @Override
  public void execute(String[] args) {
    if (args.length < 3 && !"-p".equals(args[1])) {
      return;
    }
    catFile(args[2]);
  }

  private void catFile(String blobSha) {
    String filePath = Utils.getPath(blobSha);
    LOG.info("cat-file hash={} path={}", blobSha, filePath);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(filePath))))) {
      String line = reader.readLine();
      System.out.print(line.substring(line.indexOf("\0") + 1));
      while ((line = reader.readLine()) != null) {
        System.out.print(line);
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

}