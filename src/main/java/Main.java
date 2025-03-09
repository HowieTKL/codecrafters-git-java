import org.howietkl.git.command.CatFileCommand;
import org.howietkl.git.command.CloneCommand;
import org.howietkl.git.command.CommitTreeCommand;
import org.howietkl.git.command.WriteObjectCommand;
import org.howietkl.git.command.InitCommand;
import org.howietkl.git.command.ReadTreeCommand;
import org.howietkl.git.command.WriteTreeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    final String command = args[0];

    switch (command) {
      case "init" -> new InitCommand().execute(args);
      case "cat-file" -> new CatFileCommand().execute(args);
      case "hash-object" -> new WriteObjectCommand().execute(args);
      case "ls-tree" -> new ReadTreeCommand().execute(args);
      case "write-tree" -> new WriteTreeCommand().execute(args);
      case "commit-tree" -> new CommitTreeCommand().execute(args);
      case "clone" -> new CloneCommand().execute(args);
      default -> System.out.println("Unknown command: " + command);
    }
  }

}