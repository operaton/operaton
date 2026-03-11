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
package org.operaton.bpm.engine.health;

import java.util.Map;

/**
 * SPI to contribute frontend (webapps) related health information.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 * @since 1.1
 */
public interface FrontendHealthContributor {

  /**
   * Provide frontend-related health details. Implementations should at minimum
   * expose an "operational" flag indicating whether the frontend is available.
   * Example keys:
   * - operational: boolean
   * - path: String (application path if applicable)
   */
  Map<String, Object> frontendDetails();
}
