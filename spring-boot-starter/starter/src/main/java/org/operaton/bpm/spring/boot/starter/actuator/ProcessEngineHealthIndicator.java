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

import org.operaton.bpm.engine.ProcessEngine;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.util.Assert;

public class ProcessEngineHealthIndicator extends AbstractHealthIndicator {

  private final ProcessEngine processEngine;

  public ProcessEngineHealthIndicator(ProcessEngine processEngine) {
    Assert.notNull(processEngine, "processEngine must not be null");
    this.processEngine = processEngine;
  }

  @Override
  protected void doHealthCheck(Builder builder) {
    builder.up().withDetail("name", processEngine.getName());
  }

}
