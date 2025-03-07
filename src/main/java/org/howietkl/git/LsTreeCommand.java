package org.howietkl.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class LsTreeCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(LsTreeCommand.class);

  private static class TreeObjectEntry {
    String mode;
    String name;
    byte[] sha;
  }

  @Override
  public void execute(String[] args) {
    if (args.length < 2) {
      return;
    }
    boolean isNameOnly = "--name-only".equals(args[1]);
    if (isNameOnly && args.length == 2) {
      throw new IllegalArgumentException("Missing tree-sha");
    }
    String treeSha = isNameOnly ? args[2] : args[1];
    lsTree(isNameOnly, treeSha);
  }

  private void lsTree(boolean isNameOnly, String treeSha) {
    LOG.info("lsTree isNameOnly={} treeSha={}", isNameOnly, treeSha);
    String filePath = Utils.getPath(treeSha);
    LOG.debug("lsTree file={}", filePath);

    /* File contents:
     * tree <size>\0
     * <mode> <name>\0<20_byte_sha>
     * <mode> <name>\0<20_byte_sha>
     */
    try (FileInputStream fis = new FileInputStream(filePath);
        InputStream in = new InflaterInputStream(fis)) {

      // read preamble
      int b;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      while ((b = in.read()) != -1 && b != 0) {
        buf.write(b);
      }
      String first = buf.toString(StandardCharsets.UTF_8);
      String[] firstSplit = first.split(" ");
      assert firstSplit.length == 2 && firstSplit[0].equals("tree");
      LOG.debug("ls-tree preamble: {} {}", firstSplit[0], firstSplit[1]);
      buf.reset();

      // read subsequent entries, constructing TreeObjectEntry for each
      List<TreeObjectEntry> entries = new ArrayList<>();
      while ((b = in.read()) != -1) {
        if (b > 0) {
          buf.write(b);
        } else { // b == 0
          String segment = buf.toString(StandardCharsets.UTF_8);
          buf.reset();
          String[] segmentSplit = segment.split(" ");
          assert segmentSplit.length == 2 && segmentSplit[0].length() == 6;
          TreeObjectEntry entry = new TreeObjectEntry();
          entry.mode = segmentSplit[0];
          entry.name = segmentSplit[1];
          entry.sha = new byte[20];
          int shaBytesRead = in.read(entry.sha);
          assert shaBytesRead == 20;
          LOG.debug("ls-tree entry: {} {} {}", entry.mode, entry.name, Utils.bytesToHex(entry.sha));
          entries.add(entry);
        }
      }

      // print out entries
      if (isNameOnly) {
        entries.forEach(entry -> System.out.println(entry.name));
      } else {
        entries.forEach(entry -> {
          System.out.print(entry.mode);
          System.out.print(" ");
          System.out.print(entry.name);
          System.out.print(" ");
          System.out.println(Utils.bytesToHex(entry.sha));
        });
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

}