/*
 * Copyright 2024 the Operaton contributors.
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
package org.operaton.bpm.engine.rest.util.container;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.extension.ExtensionContext;

public class TomcatExtension extends BeforeAfterExtension {

  private File tempDir;
  private final TomcatServerBootstrap tomcatBootstrap;

  protected TomcatExtension(TomcatServerBootstrap bootstrap) {
    super(bootstrap);
    this.tomcatBootstrap = bootstrap;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    tempDir = Files.createTempDirectory("rest-container-").toFile();
    tomcatBootstrap.setWorkingDir(tempDir.getAbsolutePath());
    super.beforeAll(context);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    super.afterAll(context);
    deleteRecursively(tempDir);
  }

  private void deleteRecursively(File file) {
    if (file == null) {
      return;
    }
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File c : children) {
          deleteRecursively(c);
        }
      }
    }
    file.delete();
  }
}
