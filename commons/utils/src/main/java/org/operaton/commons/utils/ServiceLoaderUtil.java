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
package org.operaton.commons.utils;

import java.util.ServiceLoader;

/**
 * Utility class for loading services via ServiceLoader.
 *
 * @since 1.1
 */
public class ServiceLoaderUtil {
  private ServiceLoaderUtil() {
    // utility class
  }

  public static <T> T loadSingleService(Class<T> serviceClass) {
    return ServiceLoader.load(serviceClass)
            .findFirst()
            .orElseGet(() -> ServiceLoader.load(serviceClass, serviceClass.getClassLoader())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No %s found".formatted(serviceClass.getSimpleName()))));
  }
}
