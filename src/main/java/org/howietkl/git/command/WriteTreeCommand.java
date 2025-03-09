package org.howietkl.git.command;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;

public class WriteTreeCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(WriteTreeCommand.class);

  @Override
  public void execute(String[] args) {
    writeTree();
  }

  private void writeTree() {
    File currentDir = new File(".");
    LOG.info("write-tree root={}", currentDir.getAbsolutePath());
    System.out.println(Utils.bytesToHex(writeTree(currentDir)));
  }

  private byte[] writeTree(File current) {
    try {
      LOG.debug("write-tree file={}", current.getPath());
      File[] files = current.listFiles(f -> !(f.isDirectory() && f.getName().equals(".git")));
      Arrays.sort(files);

      // process listing
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      for (File file : files) {
        if (file.isDirectory()) {
          byte[] sha = writeTree(file);
          buf.write(("40000 " + file.getName() + "\0").getBytes(StandardCharsets.UTF_8));
          buf.write(sha);
        } else {
          byte[] sha = new WriteObjectCommand().writeObject(file.getAbsolutePath());
          buf.write(("100644 " + file.getName() + "\0").getBytes(StandardCharsets.UTF_8));
          buf.write(sha);
        }
      }
      byte[] listing = buf.toByteArray();

      // process current dir
      byte[] preamble = ("tree " + listing.length + "\0").getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream tree = new ByteArrayOutputStream(preamble.length + listing.length);
      tree.write(preamble);
      tree.write(listing);
      byte[] treeBytes = tree.toByteArray();
      byte[] sha = Utils.sha1(treeBytes);
      String path = Utils.getPath(Utils.bytesToHex(sha));
      File treeFile = new File(path);
      treeFile.getParentFile().mkdirs();
      try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(treeFile))) {
        out.write(treeBytes);
      }
      return sha;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

}
