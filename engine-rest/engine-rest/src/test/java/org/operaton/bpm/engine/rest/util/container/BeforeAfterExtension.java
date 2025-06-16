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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

public class BeforeAfterExtension implements BeforeAllCallback, AfterAllCallback, Extension {

  protected final AbstractServerBootstrap bootstrap;

  protected BeforeAfterExtension(AbstractServerBootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    bootstrap.start();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    bootstrap.stop();
  }
}
