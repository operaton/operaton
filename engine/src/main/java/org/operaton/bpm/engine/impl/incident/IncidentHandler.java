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
package org.operaton.bpm.engine.impl.incident;

import org.operaton.bpm.engine.runtime.Incident;

/**
 * The {@link IncidentHandler} interface may be implemented by components
 * that handle and resolve incidents of a specific type that occur during the
 * execution of a process instance.
 *
 * <p>
 *
 * Custom implementations of this interface may be wired through
 * {@link org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl#setCustomIncidentHandlers(java.util.List)}.
 *
 * @see FailedJobIncidentHandler
 * @see org.operaton.bpm.engine.runtime.Incident
 *
 * @author roman.smirnov
 */
public interface IncidentHandler {

  /**
   * Returns the incident type this handler activates for.
   */
  String getIncidentHandlerType();

  /**
   * Handle an incident that arose in the context of an execution.
   */
  Incident handleIncident(IncidentContext context, String message);

  /**
   * Called in situations in which an incident handler may wish to resolve existing incidents
   * The implementation receives this callback to enable it to resolve any open incidents that
   * may exist.
   */
  void resolveIncident(IncidentContext context);

  /**
   * Called in situations in which an incident handler may wish to delete existing incidents
   * Example: when a scope is ended or a job is deleted. The implementation receives
   * this callback to enable it to delete any open incidents that may exist.
   */
  void deleteIncident(IncidentContext context);

}

