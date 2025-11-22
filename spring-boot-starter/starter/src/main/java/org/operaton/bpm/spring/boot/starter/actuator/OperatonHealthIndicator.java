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
package org.operaton.bpm.spring.boot.starter.actuator;

import org.operaton.bpm.health.HealthResult;
import org.operaton.bpm.health.HealthService;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * Spring Boot Actuator adapter that delegates to Operaton's HealthService.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 */
public class OperatonHealthIndicator extends AbstractHealthIndicator {

  private final HealthService healthService;

  public OperatonHealthIndicator(HealthService healthService) {
    Assert.notNull(healthService, "healthService must not be null");
    this.healthService = healthService;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    HealthResult result = healthService.check();
    boolean up = result != null && "UP".equalsIgnoreCase(result.status());
    if (up) {
      builder.up();
    } else {
      builder.down();
    }
    if (result != null) {
      if (result.timestamp() != null) {
        builder.withDetail("timestamp", result.timestamp());
      }
      if (result.version() != null) {
        builder.withDetail("version", result.version());
      }
      for (Map.Entry<String, Object> e : result.details().entrySet()) {
        builder.withDetail(e.getKey(), e.getValue());
      }
    }
  }
}
