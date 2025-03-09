package org.howietkl.git.utils;

import org.howietkl.git.command.CloneCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public class GitHttpClient {
  private static final Logger LOG = LoggerFactory.getLogger(CloneCommand.class);

  /**
   * Technically required by protocol to discover server service=git-upload-pack
   * capabilities, but we can assume git protocol v2 support.
   */
  public static Set<String> discoverRefs(String url) throws IOException, InterruptedException {
    URI uri = URI.create(url + "/info/refs?service=git-upload-pack");
    LOG.debug("Discover refs from uri={}", uri);

    try (HttpClient client = HttpClient.newBuilder()
        .build()) {
      HttpRequest request = HttpRequest
          .newBuilder()
          .uri(uri)
          .version(HttpClient.Version.HTTP_2)
          .header("Content-Type", "application/x-git-upload-pack-request")
          .header("Cache-Control", "no-cache")
          .header("git-protocol", "version=2")
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to discover refs statusCode=" + response.statusCode());
      }
      LOG.debug("discoverRefs httpResponse={}", response.body());
    }
    return Collections.EMPTY_SET;
  }

  public static Set<String> fetchRefs(String url) throws IOException, InterruptedException {
    URI uri = URI.create(url + "/git-upload-pack");
    LOG.debug("fetchRefs uri={}", uri);
    StringBuilder postBody = new StringBuilder();
    postBody.append("0014command=ls-refs\n")
        .append("0001")
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
          .header("Accept", "application/x-git-upload-pack-result")
          .POST(HttpRequest.BodyPublishers.ofString(postBody.toString()))
          .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
          throw new RuntimeException("Failed to fetch hashes statusCode=" + response.statusCode());
        }
        /** Sample response:
         * 00500ed33a2a0b12f05eb1aba57c5ed4a5eac9c0162d HEAD symref-target:refs/heads/main
         * 003d0ed33a2a0b12f05eb1aba57c5ed4a5eac9c0162d refs/heads/main
         * 0000
         */
        LOG.debug("fetchRefs httpResponse={}", response.body());
        // extracting hashes...
        Set<String> refs = new HashSet<>();
        BufferedReader reader = new BufferedReader(new StringReader(response.body()));
        String line;
        while ((line = reader.readLine()) != null) {
          String[] augmentedHashName = line.split(" ");
          if (augmentedHashName.length == 2) {
            String ref = augmentedHashName[0].substring(4);
            assert ref.length() == 40: "Ref not SHA-1 (40 characters): " + ref;
            refs.add(ref);
          }
        }
        return refs;
    }
  }

  public static byte[] fetchPack(String url, Set<String> hashes) throws IOException, InterruptedException {
    URI uri = URI.create(url + "/git-upload-pack");
    // construct post body command
    StringBuilder postBody = new StringBuilder();
    postBody.append("0011command=fetch")
        .append("0014agent=git/2.00.0")
        .append("0016object-format=sha1")
        .append("0001000dthin-pack");
    // https://git-scm.com/docs/gitprotocol-http#_the_negotiation_algorithm
    hashes.stream().forEach(h -> postBody.append("0032want ").append(h).append("\n"));
    postBody.append("0009done\n").append("0000");
    LOG.debug("fetchPack uri={} postBody={}", uri, postBody);

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
      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to fetch Pack statusCode=" + response.statusCode());
      }
      LOG.debug("fetchPack retrieved {} bytes", response.body().length);

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
        LOG.error("Pack file missing PACK header");
      }
      throw new IllegalStateException("Pack could not be fetched");
    }
  }

}
