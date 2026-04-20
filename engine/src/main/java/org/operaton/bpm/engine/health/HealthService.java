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
package org.operaton.bpm.engine.health;

/**
 * SPI for obtaining Operaton health information independent of the runtime.
 *
 * <p>Implementations are discovered via framework-specific dependency injection rather than
 * {@link java.util.ServiceLoader}. This is intentional: each supported runtime already provides a
 * lifecycle-aware DI mechanism that is preferable to a raw ServiceLoader:
 * <ul>
 *   <li><b>Spring Boot</b> – a default implementation is registered via
 *       {@code @ConditionalOnMissingBean}, allowing applications to supply a custom bean that
 *       replaces it.</li>
 *   <li><b>Quarkus</b> – a default implementation is registered as a CDI
 *       {@code @Produces} bean; CDI alternatives or decorators override it.</li>
 * </ul>
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 * @since 2.1
 */
public interface HealthService {

  /**
   * Perform a health check and return a {@link HealthResult}.
   */
  HealthResult check();
}