/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.impl;

import org.operaton.commons.logging.BaseLogger;

/**
 * The `RestLogger` class is a specialized logger for the REST module of Operaton.
 * It extends the {@link BaseLogger} class and serves as a base for other loggers in the REST module.
 *
 * <p>
 * This class is sealed, meaning only the specified permitted subclasses can extend it.
 * Currently, the only permitted subclass is {@link AuthLogger}.
 * </p>
 */
public sealed class RestLogger extends BaseLogger permits AuthLogger {

  /**
   * The project code used for logging purposes.
   * This helps in identifying logs related to the "REST" project.
   */
  public static final String PROJECT_CODE = "REST";

  /**
   * A static instance of the `AuthLogger` class, used for logging authentication-related events.
   * It is initialized with the project code, logger name, and a unique identifier.
   */
  public static final AuthLogger AUTH_LOGGER = BaseLogger.createLogger(AuthLogger.class, PROJECT_CODE,
      "org.operaton.bpm.engine.rest.security.auth", "01");

}


