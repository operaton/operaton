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
 */
package org.operaton.bpm.integrationtest.util;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian SPI extension. Provides ability to subscribe to and respond to lifecycle events.
 * Current primary use: ability to start testcontainers before Arquillian containers,
 * so that platform subsystems can access them at startup time.
 */
public class ArquillianLifecycleObserverExtension implements LoadableExtension {
  @Override
  public void register(ExtensionBuilder extensionBuilder) {
    extensionBuilder.observer(ArquillianEventObserver.class);
  }
}
