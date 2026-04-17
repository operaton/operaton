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
package org.operaton.bpm.quarkus.engine.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.health.HealthService;

@Readiness
@ApplicationScoped
public class OperatonHealthCheck implements HealthCheck {

  @Inject
  HealthService healthService;

  @Override
  public HealthCheckResponse call() {
    HealthResult result = healthService.check();

    HealthCheckResponseBuilder builder = HealthCheckResponse
            .named("operaton-engine")
            .status("UP".equalsIgnoreCase(result.status()));

    if (result.status() != null) {
      builder.withData("status", result.status());
    }

    flattenDetails("", result.details(), builder);

    if (result.version() != null) {
      builder.withData("version", result.version());
    }

    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private void flattenDetails(String prefix, java.util.Map<String, Object> details,
                              HealthCheckResponseBuilder builder) {
    details.forEach((k, v) -> {
      String key = prefix.isEmpty() ? k : prefix + "." + k;
      if (v instanceof java.util.Map) {
        flattenDetails(key, (java.util.Map<String, Object>) v, builder);
      } else {
        builder.withData(key, String.valueOf(v));
      }
    });
  }
}