package org.howietkl.git;

import java.io.File;

public class Repository {

  /**
   * Populates a repository from git metadata.
   * @param rootSha root directory sha
   */
  public static void populate(File dir, String rootSha) {
    dir.mkdirs();


  }
}
