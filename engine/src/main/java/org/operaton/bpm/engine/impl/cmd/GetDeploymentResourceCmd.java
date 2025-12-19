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
package org.operaton.bpm.engine.impl.cmd;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;

import org.operaton.bpm.engine.exception.DeploymentResourceNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Joram Barrez
 */
public class GetDeploymentResourceCmd implements Command<InputStream>, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  protected String deploymentId;
  protected String resourceName;

  public GetDeploymentResourceCmd(String deploymentId, String resourceName) {
    this.deploymentId = deploymentId;
    this.resourceName = resourceName;
  }

  @Override
  public InputStream execute(CommandContext commandContext) {
    ensureNotNull("deploymentId", deploymentId);
    ensureNotNull("resourceName", resourceName);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadDeployment(deploymentId);
    }

    ResourceEntity resource = commandContext
      .getResourceManager()
      .findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    ensureNotNull(DeploymentResourceNotFoundException.class, "no resource found with name '%s' in deployment '%s'".formatted(resourceName, deploymentId), "resource", resource);
    return new ByteArrayInputStream(resource.getBytes());
  }

}
