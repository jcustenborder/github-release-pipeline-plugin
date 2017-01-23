package com.github.jcustenborder.jenkins.github.release;

import com.google.common.io.Files;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GitHubReleaseStepTest {

  @Test
  public void foo() throws IOException {
    File tempDirectory = Files.createTempDir();
    File targetDirectory = new File(tempDirectory, "target");
    targetDirectory.mkdirs();
    List<String> inputFiles = Arrays.asList("kafka-connect-simulator-0.1.93.deb",
        "kafka-connect-simulator-0.1.93.jar", "kafka-connect-simulator-0.1.93.rpm",
        "kafka-connect-simulator-0.1.93.tar.gz");
    List<File> expected = new ArrayList<>(inputFiles.size());
    for (String inputFile : inputFiles) {
      File f = new File(targetDirectory, inputFile);

      if (!Files.getFileExtension(f.getName()).equals("jar")) {
        expected.add(f);
      }
      Files.touch(f);
    }

    final List<File> actual = new ArrayList<>(inputFiles.size());

    DirScanner scanner = new DirScanner.Glob(
        "target/kafka-connect-simulator-0.1.93.*",
        "**/*.jar"
    );

    scanner.scan(tempDirectory, new FileVisitor() {
      @Override
      public void visit(File file, String s) throws IOException {
        actual.add(file);
      }
    });

    Collections.sort(expected);
    Collections.sort(actual);

    assertEquals(expected, actual);
  }

}
