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
package org.operaton.bpm.container.impl.jboss.config;

import java.util.Set;
import org.operaton.bpm.engine.impl.cfg.JakartaTransactionProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.operaton.bpm.engine.impl.diagnostics.OperatonIntegration;

/**
 *
 * @author Daniel Meyer
 *
 */
public class ManagedJtaProcessEngineConfiguration extends JakartaTransactionProcessEngineConfiguration {

  public ManagedJtaProcessEngineConfiguration() {
    // override job executor auto activate: set to true in shared engine scenario
    // if it is not specified (see #CAM-4817)
    setJobExecutorActivate(true);
  }

  protected void initIdGenerator() {
    if (idGenerator == null) {
      idGenerator = new StrongUuidGenerator();
    }
  }

  @Override
  protected void initTelemetryData() {
    super.initTelemetryData();
    Set<String> operatonIntegration = telemetryData.getProduct().getInternals().getOperatonIntegration();
    operatonIntegration.add(OperatonIntegration.WILDFLY_SUBSYSTEM);
  }

}
