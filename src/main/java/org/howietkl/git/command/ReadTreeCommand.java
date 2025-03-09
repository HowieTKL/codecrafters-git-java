package org.howietkl.git.command;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class ReadTreeCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(ReadTreeCommand.class);

  public static class TreeObjectEntry {
    private String mode;
    private String name;
    private byte[] sha;

    public String getMode() {
      return mode;
    }

    public String getName() {
      return name;
    }

    public byte[] getSha() {
      return sha;
    }
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
    readTree(false, isNameOnly, treeSha, null);
  }

  /**
   * @param isQuiet    don't print anything
   * @param isNameOnly display only names
   * @param treeSha    SHA-1 of the tree (directory)
   * @param dir
   */
  public List<TreeObjectEntry> readTree(boolean isQuiet, boolean isNameOnly, String treeSha, File dir) {
    LOG.debug("readTree isNameOnly={} treeSha={}", isNameOnly, treeSha);
    if (dir == null) {
      dir = new File(".");
    }
    String filePath = Utils.getPath(treeSha);
    File file = new File(dir, filePath);
    LOG.debug("readTree file={}", filePath);

    /* File contents:
     * tree <size>\0
     * <mode> <name>\0<20_byte_sha>
     * <mode> <name>\0<20_byte_sha>
     */
    try (FileInputStream fis = new FileInputStream(file);
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
      LOG.debug("readTree preamble: {} {}", firstSplit[0], firstSplit[1]);
      buf.reset();

      // read subsequent entries, constructing TreeObjectEntry for each
      List<TreeObjectEntry> entries = new ArrayList<>();
      while ((b = in.read()) != -1) {
        if (b > 0) {
          buf.write(b);
        } else { // b == 0
          String segment = buf.toString(StandardCharsets.UTF_8);
          buf.reset();
          int indexOfFirstSpace = segment.indexOf(' ');
          TreeObjectEntry entry = new TreeObjectEntry();
          entry.mode = segment.substring(0, indexOfFirstSpace);
          entry.name = segment.substring(indexOfFirstSpace + 1);
          entry.sha = new byte[20];
          int shaBytesRead = in.read(entry.sha);
          assert shaBytesRead == 20;
          LOG.debug("ls-tree entry: {} {} {}", entry.mode, entry.name, Utils.bytesToHex(entry.sha));
          entries.add(entry);
        }
      }

      if (!isQuiet) {
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
      }
      return entries;
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

}