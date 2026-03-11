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

import org.operaton.bpm.engine.ManagementService;

/**
 * The engine collects information about multiple aspects of the installation.
 *
 * <p>
 * If telemetry is enabled this information is sent to Operaton. If telemetry is
 * disabled, the engine still collects this information and provides it through
 * {@link ManagementService#getTelemetryData()}.
 * </p>
 *
 * <p>
 * This class represents the data structure used to collect telemetry data.
 * </p>
 *
 * @see <a href=
 *      "https://docs.operaton.org/manual/latest/introduction/telemetry/#collected-data">Operaton
 *      Documentation: Collected Telemetry Data</a>
 */
public interface TelemetryData {

  /**
   * This method returns a String which is unique for each installation of
   * Operaton. It is stored once per database so all engines connected to the
   * same database will have the same installation ID. The ID is used to
   * identify a single installation of Operaton.
   */
  String getInstallation();

  /**
   * Returns a data object that stores information about the used Operaton
   * product.
   */
  Product getProduct();
}
