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
package org.operaton.bpm.engine.impl.context;

import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;


/**
 * An {@link ExecutionEntity} execution context. Provides access to the process instance and the deployment.
 *
 * @deprecated Use {@link BpmnExecutionContext} instead
 *
 * @author Tom Baeyens
 * @author Roman Smirnov
 * @author Daniel Meyer
 */
@Deprecated(forRemoval = true, since = "1.0")
public class ExecutionContext extends CoreExecutionContext<ExecutionEntity> {

  public ExecutionContext(ExecutionEntity execution) {
    super(execution);
  }

  public ExecutionEntity getProcessInstance() {
    return execution.getProcessInstance();
  }

  public ProcessDefinitionEntity getProcessDefinition() {
    return execution.getProcessDefinition();
  }

  @Override
  protected String getDeploymentId() {
    return getProcessDefinition().getDeploymentId();
  }
}
