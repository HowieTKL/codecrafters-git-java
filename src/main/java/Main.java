import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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
        if (args.length < 2 && !"-p".equals(args[0])) {
          return;
        }
        String blobSha = args[1];
        String filePath = String.format(".git/objects/%s/%s", blobSha.substring(0, 2), blobSha.substring(2));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(filePath))))) {
          String line = reader.readLine();
          System.out.println(line.substring(line.indexOf("\0" + 1)));
          while ((line = reader.readLine()) != null) {
            System.out.print(line);
          }
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }

      default -> System.out.println("Unknown command: " + command);
    }
  }
}
