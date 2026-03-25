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
import java.nio.file.Paths;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Strings;
import com.sun.tools.javac.util.Context;

class FileManager {
  private static final ObjectMapper MAPPER = new ObjectMapper().registerModules(new GuavaModule(), new Jdk8Module());
  private static final String OVERWATCH_DIR_ENV_VAR = "MAVEN_PROJECTBASEDIR";
  private static final String BLAZAR_DIR_ENV_VAR = "VIEWABLE_BUILD_ARTIFACTS_DIR";

  private final Optional<String> vbaDirectory;
  private final Optional<String> buildDirectory;


  public static synchronized FileManager instance(Context context) {
    FileManager instance = context.get(FileManager.class);

    if (instance == null) {
      instance = new FileManager(
          HubSpotUtils.getPhase(context),
          HubSpotUtils.getVbaDirectory(context),
          HubSpotUtils.getBuildDirectory(context)
      );
      context.put(FileManager.class, instance);
    }

    return instance;
  }

  private final String phase;

  FileManager(String phase, Optional<String> vbaDirectory, Optional<String> buildDirectory) {
    this.phase = phase;
    this.vbaDirectory = vbaDirectory
        .or(() -> Optional.ofNullable(System.getenv(BLAZAR_DIR_ENV_VAR)));
    this.buildDirectory = buildDirectory
        .or(() -> Optional.ofNullable(System.getenv(OVERWATCH_DIR_ENV_VAR)));;
  }

  public String getPhase() {
    return phase;
  }

  Optional<Path> getErrorOutputPath() {
    return getVbaPath("error-prone")
        .map(o -> o.resolve("error-prone-exceptions.json"));
  }

  Optional<Path> getTimingsOutputPath() {
    return getVbaPath("error-prone")
        .map(o -> o.resolve("error-prone-timings.json"));
  }

  Optional<Path> getLifeCycleCanaryPath(String id) {
    return getMavenBuildPath( "target/overwatch-metadata")
        .map(o -> o.resolve(String.format("lifecycle-canary-%s.json", id)));
  }

  Optional<Path> getUncaughtExceptionPath() {
    return getVbaPath("error-prone")
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

  private Optional<Path> getMavenBuildPath(String pathToAppend) {
    return getDataDir(buildDirectory.orElse(""), pathToAppend);
  }

  private Optional<Path> getVbaPath(String pathToAppend) {
    return getDataDir(vbaDirectory.orElse(""), pathToAppend);
  }

  private Optional<Path> getDataDir(String dir, String pathToAppend) {
    if (Strings.isNullOrEmpty(dir)) {
      return Optional.empty();
    }

    Path res = Paths.get(dir).resolve(pathToAppend).resolve("by-phase").resolve(phase);
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
