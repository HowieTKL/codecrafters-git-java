import org.howietkl.git.CatFileCommand;
import org.howietkl.git.HashObjectCommand;
import org.howietkl.git.InitCommand;
import org.howietkl.git.LsTreeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    final String command = args[0];

    switch (command) {
      case "init" -> {
        new InitCommand().execute(args);
      }
      case "cat-file" -> {
        new CatFileCommand().execute(args);
      }
      case "hash-object" -> {
        new HashObjectCommand().execute(args);
      }
      case "ls-tree" -> {
        new LsTreeCommand().execute(args);
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }

}