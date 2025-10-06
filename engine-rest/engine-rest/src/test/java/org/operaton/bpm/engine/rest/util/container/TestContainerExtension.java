/*
 * Copyright 2025 the Operaton contributors.
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
 * 
 */
package org.operaton.bpm.engine.rest.util.container;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension counterpart of the {@code TestContainerRule}.
 * It looks up the {@link ContainerSpecifics} implementation via
 * {@link ServiceLoader} and delegates the lifecycle callbacks to
 * the container specific extension.
 */
public class TestContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOGGER = Logger.getLogger(TestContainerExtension.class.getSimpleName());

  protected ContainerSpecifics containerSpecifics;
  protected Extension delegate;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    lookUpContainerSpecifics();
    delegate = containerSpecifics.getExtension(context.getRequiredTestClass());
    if (delegate instanceof BeforeAllCallback callback) {
      callback.beforeAll(context);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (delegate instanceof AfterAllCallback callback) {
      callback.afterAll(context);
    }
  }

  protected void lookUpContainerSpecifics() {
    if (this.containerSpecifics == null) {
      ServiceLoader<ContainerSpecifics> serviceLoader = ServiceLoader.load(ContainerSpecifics.class);
      Iterator<ContainerSpecifics> it = serviceLoader.iterator();

      if (it.hasNext()) {
        this.containerSpecifics = it.next();

        if (it.hasNext()) {
          LOGGER.warning("There is more than one test runtime container implementation present on the classpath. "
              + "Using " + this.containerSpecifics.getClass().getName());
        }
      } else {
        throw new RuntimeException(
            "Could not find container provider SPI that implements " + ContainerSpecifics.class.getName());
      }
    }
  }
}
