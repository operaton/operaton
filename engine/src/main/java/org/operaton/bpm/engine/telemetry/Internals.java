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
package org.operaton.bpm.engine.telemetry;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.ManagementService;

/**
 * This class represents the structure of data describing Operaton internal
 * metrics and the technical environment in which Operaton is set-up.
 *
 * @see <a href=
 *      "https://docs.operaton.org/manual/latest/introduction/telemetry/#collected-data">Operaton
 *      Documentation: Collected Telemetry Data</a>
 */
public interface Internals {

  /**
   * Information about the connected database system.
   */
  Database getDatabase();

  /**
   * Information about the application server Operaton is running on.
   */
  ApplicationServer getApplicationServer();

  /**
   * The date when the engine started to collect dynamic data, such as command executions
   * and metrics.
   *
   * <p>
   * This method returns a date that represents when local diagnostics data
   * collection started.
   * </p>
   *
   * @return A date that represents the start of the time frame where the current telemetry
   * data set was collected.
   */
  Date getDataCollectionStartDate();

  /**
   * Information about the number of command executions performed by the Operaton
   * engine. Retrieving the data through {@link ManagementService#getTelemetryData()}
   * will not reset the count.
   */
  Map<String, Command> getCommands();

  /**
   * A selection of metrics collected by the engine. Metrics included are:
   * <ul>
   *   <li>The number of root process instance executions started.</li>
   *   <li>The number of activity instances started or also known as flow node
   * instances.</li>
   *   <li>The number of executed decision instances.</li>
   *   <li>The number of executed decision elements.</li>
   * </ul>
   * Retrieving the data through {@link ManagementService#getTelemetryData()} will
   * not reset the count.
   */
  Map<String, Metric> getMetrics();

  /**
   * Used Operaton integrations (e.g, Spring boot starter, Operaton Run,
   * WildFly/JBoss subsystem or Operaton EJB service).
   */
  Set<String> getOperatonIntegration();

  /**
   * Webapps enabled in the Operaton installation (e.g., cockpit, admin,
   * tasklist).
   */
  Set<String> getWebapps();

  /**
   * Information about the installed Java runtime environment.
   */
  Jdk getJdk();

}
