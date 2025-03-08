package org.howietkl.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
    /* Tips from codecrafter comments to reverse engineer what's happening in a "git clone":
     * OPTION 1 (recommended)
     * - Install https://mitmproxy.org/
     * - git -c http.sslVerify=false -c http.proxy=localhost:8080 clone https://github.com/[repo]
     *
     * OPTION 2
     * - GIT_TRACE_CURL=1 git clone https://github.com/HowieTKL/hello hello
     *
     * References:
     * https://git-scm.com/docs/protocol-v2
     */
    LOG.info("Clone {} {}", httpPath, dir);

    try {
      File dirFile = new File(dir);
      dirFile.mkdirs();

      Set<String> hashes = fetchRootHashes(httpPath);
      byte[] pack = fetchPack(httpPath, hashes);
      LOG.debug("gitClone pack={}", Utils.bytesToHex(pack));

    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private Set<String> fetchRootHashes(String url) throws IOException, InterruptedException {
    URI uri = URI.create(url + "/git-upload-pack");
    LOG.debug("fetchRootHashes uri={}", uri.toString());
    StringBuilder postBody = new StringBuilder();
    postBody.append("0014command=ls-refs\n")
        .append("0014agent=git/2.00.0")
        .append("00010009peel\n")
        .append("001bref-prefix refs/heads/\n")
        .append("0000");
    try (HttpClient client = HttpClient.newBuilder()
        .build()) {
      HttpRequest request = HttpRequest
          .newBuilder()
          .uri(uri)
          .version(HttpClient.Version.HTTP_2)
          .header("Content-Type", "application/x-git-upload-pack-request")
          .header("Cache-Control", "no-cache")
          .header("git-protocol", "version=2")
          .POST(HttpRequest.BodyPublishers.ofString(postBody.toString()))
          .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        /** Sample response:
         * 00500ed33a2a0b12f05eb1aba57c5ed4a5eac9c0162d HEAD symref-target:refs/heads/main
         * 003d0ed33a2a0b12f05eb1aba57c5ed4a5eac9c0162d refs/heads/main
         * 0000
         */
        LOG.debug("fetchRootHashes httpResponse={}", response.body());
        // extracting hashes...
        Set<String> hashes = new HashSet<>();
        BufferedReader reader = new BufferedReader(new StringReader(response.body()));
        String line;
        while ((line = reader.readLine()) != null) {
          String[] augmentedHashName = line.split(" ");
          if (augmentedHashName.length == 2) {
            hashes.add(augmentedHashName[0].substring(4));
          }
        }
        LOG.debug(hashes.toString());
        return hashes;
    }
  }

  byte[] fetchPack(String url, Set<String> hashes) throws IOException, InterruptedException {
    URI uri = URI.create(url + "/git-upload-pack");
    LOG.debug("fetchPack uri={} hashes={}", uri, hashes.toString());
    // construct post body command
    StringBuilder postBody = new StringBuilder();
    postBody.append("0011command=fetch")
        .append("0014agent=git/2.00.0")
        .append("0016object-format=sha1")
        .append("0001000dthin-pack");
    // https://git-scm.com/docs/gitprotocol-http#_the_negotiation_algorithm
    hashes.stream().forEach(h -> postBody.append("0032want ").append(h).append("\n"));
    postBody.append("0009done\n").append("0000");
    LOG.debug("fetchPack postBody={}", postBody);

    try (HttpClient client = HttpClient.newBuilder()
        .build()) {
      HttpRequest request = HttpRequest
          .newBuilder()
          .uri(uri)
          .version(HttpClient.Version.HTTP_2)
          .header("Content-Type", "application/x-git-upload-pack-request")
          .header("Cache-Control", "no-cache")
          .header("git-protocol", "version=2")
          .POST(HttpRequest.BodyPublishers.ofString(postBody.toString()))
          .build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      LOG.debug("fetchPack - {} bytes returned response={}", response.body().length, Utils.bytesToHex(response.body()));

      // extract pack...
      ByteBuffer raw = ByteBuffer.wrap(response.body());
      // strip out "Compressing objects.." metadata
      while (raw.get() != 1) {
        // traverse bytes until we read 0x01
      }
      raw.mark();
      byte[] packHeader = new byte[4];
      raw.get(packHeader);
      // validate that we see a "PACK" header
      if ("PACK".equals(new String(packHeader, StandardCharsets.UTF_8))) {
        raw.reset();
        raw.compact();
        return raw.array();
      } else {
        LOG.error("Unexpected Pack file format");
      }
      throw new IllegalStateException("Pack could not be fetched");
    }
  }

}
