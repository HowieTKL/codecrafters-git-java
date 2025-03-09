package org.howietkl.git.command;

import org.howietkl.git.utils.GitHttpClient;
import org.howietkl.git.Pack;
import org.howietkl.git.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

public class CloneCommand implements Command {
  private static final Logger LOG = LoggerFactory.getLogger(CloneCommand.class);

  @Override
  public void execute(String[] args) {
    if (args.length < 3) {
      return;
    }

    gitClone(args[1], args[2]);
  }

  private void gitClone(String httpPath, String dir) {
    LOG.info("Clone {} {}", httpPath, dir);

    try {
      // create dir, init
      File dirFile = new File(dir);
      dirFile.mkdirs();
      new InitCommand().init(dirFile);

      // discoverRefs(httpPath); // assume git v2 support
      Set<String> refs = GitHttpClient.fetchRefs(httpPath);
      byte[] pack = GitHttpClient.fetchPack(httpPath, refs);
      Pack.process(pack, dirFile);
      Repository.populate(dirFile, refs.stream().findFirst().get());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      // throw new RuntimeException(e);
    }
  }

}
