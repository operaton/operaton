/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.operaton.bpm.spring.boot.starter.actuator;

import java.util.Objects;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.health.HealthService;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health.Builder;
import org.springframework.util.Assert;

import static java.util.Objects.requireNonNull;

public class ProcessEngineHealthIndicator extends AbstractHealthIndicator {

  private final ProcessEngine processEngine;
  private final HealthService healthService;

  public ProcessEngineHealthIndicator(ProcessEngine processEngine, HealthService healthService) {
    this.processEngine = requireNonNull(processEngine, "processEngine must not be null");
    this.healthService = requireNonNull(healthService, "healthService must not be null");
  }

  @Override
  protected void doHealthCheck(Builder builder) {
    HealthResult result = healthService.check();

    if ("UP".equalsIgnoreCase(result.status())) {
      builder.up();
    } else if ("DOWN".equalsIgnoreCase(result.status())) {
      builder.down();
    } else {
      // Propagate UNKNOWN and any custom status strings as-is
      builder.status(result.status());
    }

    builder.withDetail("name", processEngine.getName());
    builder.withDetail("timestamp", result.timestamp());

    if (result.version() != null) {
      builder.withDetail("version", result.version());
    }
    result.details().forEach(builder::withDetail);
  }
}