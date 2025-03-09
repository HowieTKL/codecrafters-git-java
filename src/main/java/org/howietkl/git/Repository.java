package org.howietkl.git;

import java.io.File;
import java.io.IOException;

public class Repository {

  /**
   * Populates a repository from git metadata.
   * @param rootSha root directory sha
   */
  public static void populate(File dir, String rootSha) throws IOException {
    dir.mkdirs();

    GitObjectManager.populateFromCommit(dir, rootSha);

  }
}
