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
package org.operaton.bpm.engine.impl.dmn.cmd;

import java.io.InputStream;
import java.io.Serializable;
import org.operaton.bpm.engine.impl.cmd.GetDeploymentResourceCmd;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.repository.DecisionDefinition;


/**
 * Gives access to a deployed decision model, e.g., a DMN 1.0 XML file, through a stream of bytes.
 */
public class GetDeploymentDecisionModelCmd implements Command<InputStream>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String decisionDefinitionId;

  public GetDeploymentDecisionModelCmd(String decisionDefinitionId) {
    this.decisionDefinitionId = decisionDefinitionId;
  }

  @Override
  public InputStream execute(final CommandContext commandContext) {
    DecisionDefinition decisionDefinition = new GetDeploymentDecisionDefinitionCmd(decisionDefinitionId).execute(commandContext);

    final String deploymentId = decisionDefinition.getDeploymentId();
    final String resourceName = decisionDefinition.getResourceName();

    return commandContext.runWithoutAuthorization(new GetDeploymentResourceCmd(deploymentId, resourceName));
  }

}
