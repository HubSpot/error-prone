/*
 * Copyright 2021 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.hubspot;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sun.tools.javac.util.Context;

/**
 * Manages files within ${project.baseDir}/target/generated-(compile|test-compile)-metadata/**
 */
class FileManager {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static synchronized FileManager instance(Context context) {
    FileManager instance = context.get(FileManager.class);

    if (instance == null) {
      instance = new FileManager(HubSpotUtils.getPhase(context));
      context.put(FileManager.class, instance);
    }

    return instance;
  }

  private final Optional<Path> targetBaseDir;

  FileManager(String phase) {
    String targetDir = System.getenv("MAVEN_PROJECTBASEDIR");

    this.targetBaseDir = Strings.isNullOrEmpty(targetDir) ?
        Optional.empty() :
        Optional.of(Path.of(targetDir, "target",
            String.format("generated-%s-metadata", phase)));
  }

  Optional<Path> getErrorOutputPath() {
    return getDataDir("overwatch-metadata")
        .map(o -> o.resolve("error-prone-exceptions.json"));
  }

  Optional<Path> getTimingsOutputPath() {
    return getDataDir("error-prone")
        .map(o -> o.resolve("error-prone-timings.json"));
  }

  Optional<Path> getLifeCycleCanaryPath(String id) {
    return getDataDir("overwatch-metadata")
        .map(o -> o.resolve(String.format("lifecycle-canary-%s.json", id)));
  }

  Optional<Path> getUncaughtExceptionPath() {
    return getDataDir("error-prone")
        .map(o -> o.resolve("error-prone-exception.log"));
  }

  void write(Object data, Path path) {
    try (OutputStream stream = Files.newOutputStream(path)) {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(stream, data);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to write errorprone metadata to %s", path)
      );
    }
  }

  private Optional<Path> getDataDir(String pathToAppend) {
    if (targetBaseDir.isEmpty()) {
      return Optional.empty();
    }

    Path res = targetBaseDir.get().resolve(pathToAppend);
    if (!Files.exists(res)) {
      try {
        Files.createDirectories(res);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to create directory: %s", res),
            e
        );
      }
    }

    return Optional.of(res);
  }
}
