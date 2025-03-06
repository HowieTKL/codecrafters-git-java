import org.howietkl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  public static final String GIT_OBJECTS = ".git/objects/%s/%s";

  public static void main(String[] args) {
    final String command = args[0];

    switch (command) {
      case "init" -> {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      case "cat-file" -> {
        if (args.length < 3 && !"-p".equals(args[1])) {
          return;
        }
        String blobSha = args[2];
        String filePath = String.format(GIT_OBJECTS, blobSha.substring(0, 2), blobSha.substring(2));
        LOG.debug("cat-file path={}", filePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(filePath))))) {
          String line = reader.readLine();
          System.out.print(line.substring(line.indexOf("\0") + 1));
          while ((line = reader.readLine()) != null) {
            System.out.print(line);
          }
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }
      case "hash-object" -> {
        if (args.length < 3 && !"-w".equals(args[1])) {
          return;
        }
        try {
          byte[] data = Files.readAllBytes(Path.of(args[2]));
          MessageDigest msg = MessageDigest.getInstance("SHA-1");
          String preamble = String.format("blob %s\0", data.length);

          msg.update(preamble.getBytes(StandardCharsets.UTF_8));
          msg.update(data);
          byte[] digest = msg.digest();
          String blobSha = Utils.bytesToHex(digest);
          String dir = String.format(".git/objects/%s", blobSha.substring(0, 2));
          File parentDir = new File(dir);

          parentDir.mkdirs();
          try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(new File(parentDir, blobSha.substring(2)), false))) {
            out.write(preamble.getBytes(StandardCharsets.UTF_8));
            out.write(data);
          }
          System.out.println(blobSha);
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
