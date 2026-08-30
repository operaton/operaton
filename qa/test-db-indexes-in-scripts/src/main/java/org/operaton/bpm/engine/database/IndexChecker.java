/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class IndexChecker {

  private static final Pattern CREATE_INDEX_PATTERN =
      Pattern.compile("^\\s*create\\s+(unique\\s+)?index\\s+(\\S+).*", Pattern.CASE_INSENSITIVE);
  private static final Pattern DROP_INDEX_PATTERN =
      Pattern.compile("^\\s*drop\\s+index\\s+([^.]+\\.)?([^ ;]+).*", Pattern.CASE_INSENSITIVE);

  private IndexChecker() {
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Must provide exactly one argument: <folder to SQL create scripts>");
    }

    Path createDir = Path.of(args[0]);
    Path dropDir = createDir.resolveSibling("drop");

    try (Stream<Path> createScripts = Files.walk(createDir)) {
      createScripts
          .filter(path -> path.toString().endsWith(".sql"))
          .forEach(createScript -> checkMatchingDropScript(createDir, dropDir, createScript));
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static void checkMatchingDropScript(Path createDir, Path dropDir, Path createScript) {
    Path dropScript = matchingDropScript(createDir, dropDir, createScript);

    try {
      Set<String> createdIndexes = extractIndexes(createScript, CREATE_INDEX_PATTERN, 2);
      Set<String> droppedIndexes = extractIndexes(dropScript, DROP_INDEX_PATTERN, 2);

      if (!createdIndexes.equals(droppedIndexes)) {
        throw new IllegalStateException("""
            Found index difference for:
            %s
            %s
            Created indexes: %s
            Dropped indexes: %s
            """.formatted(createScript, dropScript, createdIndexes, droppedIndexes));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Error processing SQL scripts: " + createScript + " and " + dropScript, e);
    }
  }

  private static Path matchingDropScript(Path createDir, Path dropDir, Path createScript) {
    Path relativeCreateScript = createDir.relativize(createScript);
    String dropFileName = relativeCreateScript.getFileName().toString().replace(".create.", ".drop.");
    return dropDir.resolve(relativeCreateScript).resolveSibling(dropFileName);
  }

  private static Set<String> extractIndexes(Path scriptPath, Pattern pattern, int valueIndex) throws IOException {
    if (!Files.exists(scriptPath)) {
      throw new IOException("SQL script does not exist: " + scriptPath);
    }

    Set<String> indexes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    try (BufferedReader reader = Files.newBufferedReader(scriptPath)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String indexName = extractIndexName(line, pattern, valueIndex);
        if (indexName != null) {
          indexes.add(indexName.toLowerCase(Locale.ROOT));
        }
      }
    }

    return indexes;
  }

  private static String extractIndexName(String input, Pattern pattern, int valueIndex) {
    Matcher matcher = pattern.matcher(input);
    if (matcher.matches()) {
      return matcher.group(valueIndex);
    }
    return null;
  }

}
