/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.metrics.util;

import org.operaton.bpm.engine.management.Metrics;

public class MetricsUtil {

  /**
   * Resolves the internal name of the metric by the public name.
   *
   * @param publicName the public name
   * @return the internal name
   */
  public static String resolveInternalName(final String publicName) {
    if (publicName == null) return null;
    return switch (publicName) {
      case Metrics.TASK_USERS -> Metrics.UNIQUE_TASK_WORKERS;
      case Metrics.PROCESS_INSTANCES -> Metrics.ROOT_PROCESS_INSTANCE_START;
      case Metrics.DECISION_INSTANCES -> Metrics.EXECUTED_DECISION_INSTANCES;
      case Metrics.FLOW_NODE_INSTANCES -> Metrics.ACTIVTY_INSTANCE_START;
      default -> publicName;
    };
  }

  /**
   * Resolves the public name of the metric by the internal name.
   *
   * @param internalName the internal name
   * @return the public name
   */
  public static String resolvePublicName(final String internalName) {
    if (internalName == null) return null;
    return switch (internalName) {
    case Metrics.UNIQUE_TASK_WORKERS -> Metrics.TASK_USERS;
    case Metrics.ROOT_PROCESS_INSTANCE_START -> Metrics.PROCESS_INSTANCES;
    case Metrics.EXECUTED_DECISION_INSTANCES -> Metrics.DECISION_INSTANCES;
    case Metrics.ACTIVTY_INSTANCE_START -> Metrics.FLOW_NODE_INSTANCES;
    default -> internalName;
    };
  }

}
