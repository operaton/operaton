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
package org.operaton.bpm;

import java.util.Set;

import org.operaton.bpm.application.ProcessApplicationInfo;

/**
 * <p>The process application service provides access to all deployed process applications.</p>
 *
 * @author Daniel Meyer
 *
 */
public interface ProcessApplicationService {

  /**
   * @returns the names of all deployed process applications
   * */
  Set<String> getProcessApplicationNames();

  /**
   * <p>Provides information about a deployed process application</p>
   *
   * @param processApplicationName
   *
   * @return the {@link ProcessApplicationInfo} object or null if no such process application is deployed.
   */
  ProcessApplicationInfo getProcessApplicationInfo(String processApplicationName);

}
