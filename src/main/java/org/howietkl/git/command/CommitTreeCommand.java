package org.howietkl.git.command;

import org.howietkl.git.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.DeflaterOutputStream;

public class CommitTreeCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(CommitTreeCommand.class);

  @Override
  public void execute(String[] args) {
    if (args.length < 6 || !args[2].equals("-p") || !args[4].equals("-m")) {
      System.err.println("Expecting: commit-tree [treeSha] -p [commitSha] -m [message]");
      return;
    }
    System.out.println(Utils.bytesToHex(commitTree(args[1], args[3], args[5])));
  }

  byte[] commitTree(String treeSha, String parentCommitSha, String commitMessage) {
    LOG.info("commit-tree treeSha={} parentCommitSha={} msg={}", treeSha, parentCommitSha, commitMessage);
    Commit commit = new Commit();
    commit.treeSha = treeSha;
    commit.parentSha = parentCommitSha;
    commit.message = commitMessage;

    // generate commit
    byte[] everything = commit.getCommit().getBytes(StandardCharsets.UTF_8);

    // calculate SHA-1
    byte[] shaBytes = Utils.sha1(everything);
    String sha = Utils.bytesToHex(shaBytes);

    // ensure parent dir exists in .git/objects/
    File parentDir = new File(String.format(".git/objects/%s", sha.substring(0, 2)));
    parentDir.mkdirs();
    File commitFile = new File(parentDir, sha.substring(2));

    LOG.debug("commit-tree dir={} file={} contents={}",
        parentDir.getName(), commitFile.getName(), commit.getCommit());

    // create the commit
    try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(commitFile, false))) {
      out.write(everything);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    return shaBytes;
  }

  static class Commit {
    String treeSha;
    String parentSha;
    String authorName = "dev";
    String authorEmail = "dev@noreply.example.com";
    LocalDateTime authorTime = LocalDateTime.now();
    String committerName = "dev";
    String committerEmail = "dev@noreply.example.com";
    LocalDateTime commitTime = LocalDateTime.now();
    String message;

    private StringBuilder getTreeContent() {
      return new StringBuilder()
        .append("tree ").append(treeSha).append("\n")
        .append("parent ").append(parentSha).append("\n")
        .append("author ").append(authorName).append(" <").append(authorEmail).append("> ")
          .append(authorTime.toEpochSecond(ZoneOffset.UTC)).append(" +0000").append("\n")
        .append("committer ").append(committerName).append(" <").append(committerEmail).append("> ")
          .append(commitTime.toEpochSecond(ZoneOffset.UTC)).append(" +0000").append("\n\n")
        .append(message).append("\n");
    }

    String getCommit() {
      byte[] treeContent = getTreeContent().toString().getBytes(StandardCharsets.UTF_8);
      return new StringBuilder()
          .append("commit ").append(treeContent.length).append("\0").append(getTreeContent())
          .toString();
    }
  }

}
