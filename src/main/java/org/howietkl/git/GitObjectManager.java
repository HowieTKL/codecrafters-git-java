package org.howietkl.git;

import org.howietkl.git.command.ReadTreeCommand;
import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class GitObjectManager {
  private static final Logger LOG = LoggerFactory.getLogger(GitObjectManager.class);

  public static void populateFromCommit(File dir, String sha) throws IOException {
    dir.mkdirs();
    LOG.info("created dir={}", dir.getAbsolutePath());

    String path = Utils.getPath(sha);
    File filePath = new File(dir, path);
    try (FileInputStream fis = new FileInputStream(filePath);
         InputStream in = new InflaterInputStream(fis);
         InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
         BufferedReader reader = new BufferedReader(isr);) {

      // read header
      int b;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      while ((b = reader.read()) != 0) {
        buf.write(b);
      }
      String[] header = buf.toString(StandardCharsets.UTF_8).split(" ");
      GitObjectInfo info = new GitObjectInfo();
      info.setType(GitObjectType.parse(header[0]));
      info.setSize(Integer.parseInt(header[1]));
      LOG.debug("populateFromCommit type={} sha={}", info.getType(), sha);
      assert info.getType() == GitObjectType.COMMIT: "Expecting COMMIT, but got " + info.getType();

      // look for TREE in commit
      String rootTreeSha = null;
      String line;
      while ((line = reader.readLine()) != null) {
        String[] lineParts = line.split(" ");
        if ("tree".equals(lineParts[0])) {
          rootTreeSha = lineParts[1];
          LOG.debug("populateFromCommit found rootTreeSha={}", rootTreeSha);
          break;
        }
      }
      if (rootTreeSha == null) {
        throw new IllegalStateException("Could not find root tree sha");
      }
      populateTree(dir, dir, rootTreeSha);
    }
  }

  private static void populateTree(File gitRoot, File dir, String sha) throws IOException {
    dir.mkdirs();
    LOG.info("created dir={}", dir.getAbsolutePath());
    List<ReadTreeCommand.TreeObjectEntry> treeEntries = new ReadTreeCommand()
        .readTree(true, false, sha, gitRoot);

    for (ReadTreeCommand.TreeObjectEntry entry : treeEntries) {
      String entrySha = Utils.bytesToHex(entry.getSha());
      LOG.debug("populateTree mode={} name={} sha={}", entry.getMode(), entry.getName(), entrySha);
      if ("40000".equals(entry.getMode())) { // tree/directory
        populateTree(gitRoot, new File(dir, entry.getName()), entrySha);
      } else if ("100644".equals(entry.getMode())) { // blob/file
        populateBlob(gitRoot, dir, entry);
      } else {
        throw new IllegalStateException("Unexpected mode " + entry.getMode());
      }
    }
  }

  private static void populateBlob(File gitRoot, File dir, ReadTreeCommand.TreeObjectEntry entry) throws IOException {
    File file = new File(dir, entry.getName());
    file.createNewFile();
    LOG.debug("populateBlob mode={} path={}", entry.getMode(), file.getAbsolutePath());
    String blob = Utils.getPath(Utils.bytesToHex(entry.getSha()));
    File blobFile = new File(gitRoot, blob);

    try (FileInputStream fis = new FileInputStream(blobFile);
         InputStream in = new InflaterInputStream(fis)) {

      // read preamble
      int b;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      while ((b = in.read()) != 0) {
        buf.write(b);
      }
      int size = Integer.parseInt(buf.toString(StandardCharsets.UTF_8).split(" ")[1]);

      LOG.debug("populateBlob name={} size={}", entry.getName(), size);
      // read data
      byte[] data = in.readNBytes(size);

      // write out to file
      try (FileOutputStream out = new FileOutputStream(file)) {
        out.write(data);
      }
    }
    LOG.info("created file={}", file.getAbsolutePath());
  }

}
