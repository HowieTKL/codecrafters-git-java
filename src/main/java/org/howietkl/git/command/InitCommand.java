package org.howietkl.git.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class InitCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(InitCommand.class);

  @Override
  public void execute(String[] args) {
    init(new File("."));
  }

  void init(File dir) {
    LOG.info("init");
    final File root = new File(dir,".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
    final File head = new File(root, "HEAD");

    try {
      head.createNewFile();
      Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
      System.out.println("Initialized git directory");
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }

  }

}